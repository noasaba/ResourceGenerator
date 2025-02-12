package com.noasaba.resourceGenerator;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class ResourceGenerator extends JavaPlugin {

    // プラグインが想定する config.yml の最新バージョン
    private static final int CURRENT_CONFIG_VERSION = 9;

    private MultiverseCore mvCore;
    private MVWorldManager worldManager;

    @Override
    public void onEnable() {
        // 1) config.yml が存在しなければ初期生成
        saveDefaultConfig();

        // 2) バージョンチェックして、古ければバックアップ + 新しい config.yml を再生成
        checkConfigVersionAndUpdate();

        // 3) Multiverse-Core, NethePortals の確認
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") instanceof MultiverseCore) {
            mvCore = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
            if (mvCore != null) {
                worldManager = mvCore.getMVWorldManager();
            }
        } else {
            getLogger().severe("Multiverse-Core が見つからないため、ワールド作成機能を使用できません。");
        }

        if (getServer().getPluginManager().getPlugin("Multiverse-NetherPortals") == null) {
            getLogger().severe("Multiverse-NetherPortals が見つからないため、ポータルリンク機能を使用できません。");
        }

        getLogger().info("ResourceGenerator が有効になりました。");
    }

    /**
     * config.yml のバージョンをチェックし、古かったらバックアップ＆再生成する
     */
    private void checkConfigVersionAndUpdate() {
        // configファイルの場所
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            // まだ存在しないなら、saveDefaultConfig() で生成されたはずなので何もしない
            return;
        }

        // ローカルの config を一時読み込み
        FileConfiguration localConfig = YamlConfiguration.loadConfiguration(configFile);
        int fileVersion = localConfig.getInt("configVersion", 0);

        // 比較
        if (fileVersion < CURRENT_CONFIG_VERSION) {
            // バージョンが古い → バックアップ
            File backupFile = new File(getDataFolder(), "config_old_ver" + fileVersion + ".yml");
            boolean renameOk = configFile.renameTo(backupFile);
            if (renameOk) {
                getLogger().warning("旧バージョン(" + fileVersion + ") の config.yml をバックアップしました: " + backupFile.getName());
            } else {
                getLogger().warning("旧config.yml のバックアップに失敗しました。");
            }

            // リソースから新しい config.yml を再生成
            saveResource("config.yml", false);
            getLogger().info("新バージョン(" + CURRENT_CONFIG_VERSION + ")の config.yml を再生成しました。");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ResourceGenerator が無効になりました。");
    }

    /**
     * /resource create [-s <seed>] コマンドの処理
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
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

        // サブコマンドが "create" かどうか
        if (args.length < 1 || !args[0].equalsIgnoreCase("create")) {
            player.sendMessage("使用方法: /resource create [-s <seed>]");
            return true;
        }

        // /resource create -s <seed> の引数チェック
        Long seed = null;
        if (args.length >= 3 && args[1].equalsIgnoreCase("-s")) {
            try {
                seed = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("シード値は数値で指定してください。例: /resource create -s 123456789");
                return true;
            }
        }

        // configを再ロードし、最新を使う
        reloadConfig();
        FileConfiguration config = getConfig();

        // ワールド作成
        createResourceWorld(player, config, seed);
        return true;
    }

    /**
     * 資源ワールド(オーバーワールド/ネザー/エンド)を作成する。
     * シード値 seed がnullならランダム、指定があればその値に。
     * master & dimension_commands を適用。
     */
    private void createResourceWorld(Player player, FileConfiguration config, Long seedLong) {
        if (worldManager == null) {
            player.sendMessage("Multiverse-Core が見つからないため、ワールドを作成できません。");
            return;
        }

        // last_world_id を読み込み
        int baseId = config.getInt("last_world_id", 0);
        int attempt = 0;
        final int MAX_TRY = 100;

        String overworldName, netherName, endName;
        while (true) {
            baseId++;
            overworldName = "re_world_" + baseId;
            netherName    = "re_world_nether_" + baseId;
            endName       = "re_world_the_end_" + baseId;

            // 既に同名ワールドが存在するかチェック
            if (!worldManager.isMVWorld(overworldName)
                    && !worldManager.isMVWorld(netherName)
                    && !worldManager.isMVWorld(endName)) {
                // 衝突なし
                break;
            }
            attempt++;
            if (attempt >= MAX_TRY) {
                player.sendMessage("ワールド名が連続で衝突しすぎです。config.yml の last_world_id を修正してください。");
                return;
            }
        }

        // シードを String に変換 (nullならランダム扱い)
        String seedString = (seedLong == null) ? null : seedLong.toString();

        // ワールド作成
        worldManager.addWorld(overworldName, World.Environment.NORMAL, seedString, WorldType.NORMAL, false, null);
        worldManager.addWorld(netherName, World.Environment.NETHER, seedString, WorldType.NORMAL, false, null);
        worldManager.addWorld(endName, World.Environment.THE_END, seedString, WorldType.NORMAL, false, null);

        // ネザー & エンド ポータルリンク (Multiverse-NetherPortals)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvnp link nether " + overworldName + " " + netherName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvnp link nether " + netherName + " " + overworldName);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvnp link end " + overworldName + " " + endName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvnp link end " + endName + " " + overworldName);

        // last_world_id を更新
        config.set("last_world_id", baseId);
        saveConfig();

        // master(共通)コマンド
        applyMasterCommands(overworldName, config, player);
        applyMasterCommands(netherName, config, player);
        applyMasterCommands(endName, config, player);

        // ディメンション別コマンド
        applyDimensionCommands("overworld", overworldName, config, player);
        applyDimensionCommands("nether", netherName, config, player);
        applyDimensionCommands("end", endName, config, player);

        // 完了メッセージ
        String seedMsg = (seedLong == null) ? "ランダム" : seedLong.toString();
        player.sendMessage("資源ワールド " + overworldName + " を作成しました。 (シード: " + seedMsg + ")");
    }

    /**
     * master セクションを実行 ("execute in <world> run <cmd>" を付与)
     */
    private void applyMasterCommands(String actualWorldName, FileConfiguration config, Player player) {
        List<String> masterCommands = config.getStringList("master");
        if (masterCommands.isEmpty()) return;

        for (String cmd : masterCommands) {
            String finalCmd = "execute in " + actualWorldName + " run " + cmd;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }
        player.sendMessage("[master]コマンド適用 -> " + actualWorldName);
    }

    /**
     * dimension_commands.overworld / nether / end を実行
     */
    private void applyDimensionCommands(String dimensionKey, String actualWorldName,
                                        FileConfiguration config, Player player) {
        List<String> commands = config.getStringList("dimension_commands." + dimensionKey);
        if (commands.isEmpty()) {
            getLogger().info("[" + dimensionKey + "] 用のコマンドは未設定。");
            return;
        }
        for (String cmd : commands) {
            String finalCmd = "execute in " + actualWorldName + " run " + cmd;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }
        player.sendMessage("[" + dimensionKey + "]コマンド適用 -> " + actualWorldName);
    }
}
