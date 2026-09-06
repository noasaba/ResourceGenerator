package com.noasaba.resourceGenerator;

import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Registry;
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
import java.util.Locale;

public final class ResourceGenerator extends JavaPlugin {

    // プラグインが想定する config.yml の最新バージョン
    private static final int CURRENT_CONFIG_VERSION = 9;

    private WorldManager worldManager;

    @Override
    public void onEnable() {
        // 1) config.yml が存在しなければ初期生成
        saveDefaultConfig();

        // 2) バージョンチェックして、古ければバックアップ + 新しい config.yml を再生成
        checkConfigVersionAndUpdate();

        // 3) Multiverse-Core, NethePortals の確認
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") instanceof MultiverseCore) {
            worldManager = MultiverseCoreApi.get().getWorldManager();
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
            File backupFile = nextAvailableBackupFile(fileVersion);
            boolean renameOk = configFile.renameTo(backupFile);
            if (renameOk) {
                getLogger().warning("旧バージョン(" + fileVersion + ") の config.yml をバックアップしました: " + backupFile.getName());
            } else {
                getLogger().warning("旧config.yml のバックアップに失敗しました。");
                return;
            }

            // リソースから新しい config.yml を再生成
            saveResource("config.yml", false);
            getLogger().info("新バージョン(" + CURRENT_CONFIG_VERSION + ")の config.yml を再生成しました。");
        }
    }

    private File nextAvailableBackupFile(int fileVersion) {
        String baseName = "config_old_ver" + fileVersion;
        File backupFile = new File(getDataFolder(), baseName + ".yml");
        int suffix = 1;
        while (backupFile.exists()) {
            backupFile = new File(getDataFolder(), baseName + "_" + suffix + ".yml");
            suffix++;
        }
        return backupFile;
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
        if (args.length != 1 && args.length != 3) {
            player.sendMessage("使用方法: /resource create [-s <seed>]");
            return true;
        }
        if (args.length == 3) {
            if (!args[1].equalsIgnoreCase("-s")) {
                player.sendMessage("使用方法: /resource create [-s <seed>]");
                return true;
            }
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
            if (!worldManager.isWorld(overworldName)
                    && !worldManager.isWorld(netherName)
                    && !worldManager.isWorld(endName)) {
                // 衝突なし
                break;
            }
            attempt++;
            if (attempt >= MAX_TRY) {
                player.sendMessage("ワールド名が連続で衝突しすぎです。config.yml の last_world_id を修正してください。");
                return;
            }
        }

        // ワールド作成
        boolean overworldCreated = createWorld(overworldName, World.Environment.NORMAL, seedLong);
        boolean netherCreated = createWorld(netherName, World.Environment.NETHER, seedLong);
        boolean endCreated = createWorld(endName, World.Environment.THE_END, seedLong);

        if (!overworldCreated || !netherCreated || !endCreated) {
            getLogger().severe("資源ワールドの作成に失敗しました: overworld=" + overworldCreated
                    + ", nether=" + netherCreated + ", end=" + endCreated);
            player.sendMessage("資源ワールドの作成に失敗しました。サーバーログを確認してください。");
            return;
        }

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

    private boolean createWorld(String worldName, World.Environment environment, Long seed) {
        CreateWorldOptions options = CreateWorldOptions.worldName(worldName)
                .environment(environment)
                .worldType(WorldType.NORMAL);
        if (seed != null) {
            options.seed(seed);
        }
        return worldManager.createWorld(options).isSuccess();
    }

    /**
     * master セクションを実行 ("execute in <world> run <cmd>" を付与)
     */
    private void applyMasterCommands(String actualWorldName, FileConfiguration config, Player player) {
        List<String> masterCommands = config.getStringList("master");
        if (masterCommands.isEmpty()) return;

        for (String cmd : masterCommands) {
            executeWorldCommand(actualWorldName, cmd);
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
            executeWorldCommand(actualWorldName, cmd);
        }
        player.sendMessage("[" + dimensionKey + "]コマンド適用 -> " + actualWorldName);
    }

    private void executeWorldCommand(String worldName, String command) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().warning("コマンド対象のワールドがロードされていません: " + worldName);
            return;
        }

        String[] parts = command.trim().split("\\s+");
        if (parts.length == 3 && parts[0].equalsIgnoreCase("gamerule")) {
            GameRule<?> gameRule = findGameRule(parts[1]);
            if (gameRule == null || !setGameRule(world, gameRule, parts[2])) {
                getLogger().warning("ゲームルールの適用に失敗しました (" + worldName + "): " + command);
            }
            return;
        }
        if (parts.length == 2 && parts[0].equalsIgnoreCase("difficulty")) {
            try {
                world.setDifficulty(Difficulty.valueOf(parts[1].toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                getLogger().warning("難易度の適用に失敗しました (" + worldName + "): " + command);
            }
            return;
        }

        String finalCommand = command.replace("{world}", worldName);
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand)) {
            getLogger().warning("コマンドの実行に失敗しました (" + worldName + "): " + finalCommand);
        }
    }

    private <T> boolean setGameRule(World world, GameRule<T> gameRule, String rawValue) {
        try {
            Object parsedValue;
            if (gameRule.getType() == Boolean.class) {
                if (!rawValue.equalsIgnoreCase("true") && !rawValue.equalsIgnoreCase("false")) {
                    return false;
                }
                parsedValue = Boolean.parseBoolean(rawValue);
            } else if (gameRule.getType() == Integer.class) {
                parsedValue = Integer.parseInt(rawValue);
            } else {
                return false;
            }
            return world.setGameRule(gameRule, gameRule.getType().cast(parsedValue));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private GameRule<?> findGameRule(String name) {
        for (GameRule<?> gameRule : Registry.GAME_RULE) {
            if (gameRule.getName().equalsIgnoreCase(name)
                    || gameRule.getKey().toString().equalsIgnoreCase(name)) {
                return gameRule;
            }
        }
        return null;
    }
}
