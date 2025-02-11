package com.noasaba.resourceGenerator;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ResourceGenerator extends JavaPlugin {
    private MultiverseCore mvCore;
    private MVWorldManager worldManager;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // プラグインのバージョンを取得
        String pluginVersion = getDescription().getVersion();

        // コンソールに起動メッセージを表示
        getLogger().info("== === ==");
        getLogger().info("ResourceGenerator v" + pluginVersion + " - Developed by NOASABA (by nanosize)");
        getLogger().info("== === ==");

        // config.yml を生成・読み込み
        saveDefaultConfig();
        config = getConfig();

        // Multiverse-Core の存在確認 & 取得
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") instanceof MultiverseCore) {
            mvCore = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
            if (mvCore != null) {
                worldManager = mvCore.getMVWorldManager();
                getLogger().info("Multiverse-Core を検出しました。");
            }
        } else {
            getLogger().severe("Multiverse-Core が見つからないため、ワールド作成機能を使用できません。");
        }

        // Multiverse-NetherPortals の存在確認
        if (getServer().getPluginManager().getPlugin("Multiverse-NetherPortals") != null) {
            getLogger().info("Multiverse-NetherPortals を検出しました。");
        } else {
            getLogger().severe("Multiverse-NetherPortals が見つからないため、ポータルリンク機能を使用できません。");
        }

        getLogger().info("ResourceGeneratorが有効になりました。");
    }


    @Override
    public void onDisable() {
        getLogger().info("ResourceGeneratorが無効になりました。");
    }

    /**
     * /resource create コマンドのみを処理
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // プレイヤー以外の場合は使用不可
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }
        Player player = (Player) sender;

        // パーミッションチェック
        if (!player.hasPermission("resourcegenerator.admin")) {
            player.sendMessage("権限がありません。");
            return true;
        }

        // サブコマンドが "create" かどうか判定
        if (args.length < 1 || !args[0].equalsIgnoreCase("create")) {
            player.sendMessage("使用方法: /resource create");
            return true;
        }

        // 資源ワールドの作成処理
        createResourceWorld(player);
        return true;
    }

    /**
     * 資源ワールド(オーバーワールド/ネザー/エンド)を作成し、双方向ポータルリンク
     * さらにエンドからオーバーワールドに戻れるよう、自動で respawnWorld を設定
     */
    private void createResourceWorld(Player player) {
        if (worldManager == null) {
            player.sendMessage("Multiverse-Core が見つからないため、ワールドを作成できません。");
            return;
        }

        // config.yml の last_world_id を +1
        int lastWorldId = config.getInt("last_world_id", 0) + 1;

        // ワールド名を決定
        String overworldName = "re_world_" + lastWorldId;
        String netherName    = "re_world_nether_" + lastWorldId;
        String endName       = "re_world_the_end_" + lastWorldId;

        // 既に存在しないかチェック
        if (worldManager.isMVWorld(overworldName)) {
            player.sendMessage("ワールド " + overworldName + " は既に存在します。");
            return;
        }

        // オーバーワールド作成
        worldManager.addWorld(
                overworldName,
                World.Environment.NORMAL,
                null,
                WorldType.NORMAL,
                false,
                null
        );

        // ネザー作成
        worldManager.addWorld(
                netherName,
                World.Environment.NETHER,
                null,
                WorldType.NORMAL,
                false,
                null
        );

        // エンド作成
        worldManager.addWorld(
                endName,
                World.Environment.THE_END,
                null,
                WorldType.NORMAL,
                false,
                null
        );

        // ここからポータルリンク等のコマンド自動実行
        if (getServer().getPluginManager().getPlugin("Multiverse-NetherPortals") != null) {
            // ★ ネザー: 双方向リンク
            // (1) Overworld -> Nether
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "mvnp link nether " + overworldName + " " + netherName
            );
            // (2) Nether -> Overworld
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "mvnp link nether " + netherName + " " + overworldName
            );

            // ★ エンド: 双方向リンク
            // (1) Overworld -> End
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "mvnp link end " + overworldName + " " + endName
            );
            // (2) End -> Overworld
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "mvnp link end " + endName + " " + overworldName
            );

            // ★ エンド中央ポータルから Overworld に戻るよう設定 (Multiverse-Coreのコマンド)
            // コマンド形式: /mv modify set respawnWorld <value> <world>
            //   PROPERTY = respawnWorld
            //   VALUE    = overworldName
            //   WORLD    = endName
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "mv modify set respawnWorld " + overworldName + " " + endName
            );

            player.sendMessage("ネザー/エンドを双方向リンクし、エンド中央ポータルから " + overworldName + " に戻れるよう設定しました。");
        } else {
            player.sendMessage("Multiverse-NetherPortals が見つからないため、ポータルリンクは設定されません。");
        }

        // 新しいIDを保存
        config.set("last_world_id", lastWorldId);
        saveConfig();

        player.sendMessage("資源ワールド " + overworldName + " を作成しました。");

        // config.yml に書かれた初期コマンドを実行
        List<String> commands = config.getStringList("create_commands");
        for (String cmd : commands) {
            String replaced = cmd.replace("{world}", overworldName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced);
        }
        player.sendMessage("初期設定コマンドを適用しました。");
    }
}
