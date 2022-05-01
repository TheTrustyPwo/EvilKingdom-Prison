package net.minecraft.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import io.papermc.paper.world.ThreadedWorldUpgrader;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;

// CraftBukkit start
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.SharedConstants;

public class Main {

    private static final Logger LOGGER = LogUtils.getLogger();

    public Main() {}

    @DontObfuscate
    public static void main(final OptionSet optionset) { // CraftBukkit - replaces main(String[] astring)
        SharedConstants.tryDetectVersion();
        /* CraftBukkit start - Replace everything
        OptionParser optionparser = new OptionParser();
        OptionSpec<Void> optionspec = optionparser.accepts("nogui");
        OptionSpec<Void> optionspec1 = optionparser.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
        OptionSpec<Void> optionspec2 = optionparser.accepts("demo");
        OptionSpec<Void> optionspec3 = optionparser.accepts("bonusChest");
        OptionSpec<Void> optionspec4 = optionparser.accepts("forceUpgrade");
        OptionSpec<Void> optionspec5 = optionparser.accepts("eraseCache");
        OptionSpec<Void> optionspec6 = optionparser.accepts("safeMode", "Loads level with vanilla datapack only");
        OptionSpec<Void> optionspec7 = optionparser.accepts("help").forHelp();
        OptionSpec<String> optionspec8 = optionparser.accepts("singleplayer").withRequiredArg();
        OptionSpec<String> optionspec9 = optionparser.accepts("universe").withRequiredArg().defaultsTo(".", new String[0]);
        OptionSpec<String> optionspec10 = optionparser.accepts("world").withRequiredArg();
        OptionSpec<Integer> optionspec11 = optionparser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1, new Integer[0]);
        OptionSpec<String> optionspec12 = optionparser.accepts("serverId").withRequiredArg();
        OptionSpec<Void> optionspec13 = optionparser.accepts("jfrProfile");
        NonOptionArgumentSpec nonoptionargumentspec = optionparser.nonOptions();

        try {
            OptionSet optionset = optionparser.parse(astring);

            if (optionset.has(optionspec7)) {
                optionparser.printHelpOn(System.err);
                return;
            }
            */ // CraftBukkit end

        try {

            CrashReport.preload();
            if (optionset.has("jfrProfile")) { // CraftBukkit
                JvmProfiler.INSTANCE.start(Environment.SERVER);
            }

            Bootstrap.bootStrap();
            Bootstrap.validate();
            Util.startTimerHackThread();
            Path path = Paths.get("server.properties");
            DedicatedServerSettings dedicatedserversettings = new DedicatedServerSettings(optionset); // CraftBukkit - CLI argument support

            dedicatedserversettings.forceSave();
            // Paper start - load config files for access below if needed
            org.bukkit.configuration.file.YamlConfiguration bukkitConfiguration = loadConfigFile((File) optionset.valueOf("bukkit-settings"));
            org.bukkit.configuration.file.YamlConfiguration spigotConfiguration = loadConfigFile((File) optionset.valueOf("spigot-settings"));
            org.bukkit.configuration.file.YamlConfiguration paperConfiguration = loadConfigFile((File) optionset.valueOf("paper-settings"));
            // Paper end

            Path path1 = Paths.get("eula.txt");
            Eula eula = new Eula(path1);

            if (optionset.has("initSettings")) { // CraftBukkit
                Main.LOGGER.info("Initialized '{}' and '{}'", path.toAbsolutePath(), path1.toAbsolutePath());
                return;
            }

            // Spigot Start
            boolean eulaAgreed = Boolean.getBoolean( "com.mojang.eula.agree" );
            if ( eulaAgreed )
            {
                System.err.println( "You have used the Spigot command line EULA agreement flag." );
                System.err.println( "By using this setting you are indicating your agreement to Mojang's EULA (https://account.mojang.com/documents/minecraft_eula)." );
                System.err.println( "If you do not agree to the above EULA please stop your server and remove this flag immediately." );
            }
            // Spigot End
            if (!eula.hasAgreedToEULA() && !eulaAgreed) { // Spigot
                Main.LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
                return;
            }

            org.spigotmc.SpigotConfig.disabledAdvancements = spigotConfiguration.getStringList("advancements.disabled"); // Paper - fix SPIGOT-5885, must be set early in init
            // Paper start - fix SPIGOT-5824
            File file;
            File userCacheFile = new File("usercache.json");
            if (optionset.has("universe")) {
                file = (File) optionset.valueOf("universe"); // CraftBukkit
                userCacheFile = new File(file, "usercache.json");
            } else {
                file = new File(bukkitConfiguration.getString("settings.world-container", "."));
            }
            // Paper end - fix SPIGOT-5824
            YggdrasilAuthenticationService yggdrasilauthenticationservice = new com.destroystokyo.paper.profile.PaperAuthenticationService(Proxy.NO_PROXY); // Paper
            MinecraftSessionService minecraftsessionservice = yggdrasilauthenticationservice.createMinecraftSessionService();
            GameProfileRepository gameprofilerepository = yggdrasilauthenticationservice.createProfileRepository();
            GameProfileCache usercache = new GameProfileCache(gameprofilerepository, userCacheFile); // Paper - only move usercache.json into folder if --universe is used, not world-container
            // CraftBukkit start
            String s = (String) Optional.ofNullable((String) optionset.valueOf("world")).orElse(dedicatedserversettings.getProperties().levelName);
            LevelStorageSource convertable = LevelStorageSource.createDefault(file.toPath());
            LevelStorageSource.LevelStorageAccess convertable_conversionsession = convertable.createAccess(s, LevelStem.OVERWORLD);
            LevelSummary worldinfo = convertable_conversionsession.getSummary();

            if (worldinfo != null) {
                if (worldinfo.requiresManualConversion()) {
                    Main.LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
                    return;
                }

                if (!worldinfo.isCompatible()) {
                    Main.LOGGER.info("This world was created by an incompatible version.");
                    return;
                }
            }

            boolean flag = optionset.has("safeMode");

            if (flag) {
                Main.LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
            }

            PackRepository resourcepackrepository = new PackRepository(PackType.SERVER_DATA, new RepositorySource[]{new ServerPacksSource(), new FolderRepositorySource(convertable_conversionsession.getLevelPath(LevelResource.DATAPACK_DIR).toFile(), PackSource.WORLD)});
            // CraftBukkit start
            File bukkitDataPackFolder = new File(convertable_conversionsession.getLevelPath(LevelResource.DATAPACK_DIR).toFile(), "bukkit");
            if (!bukkitDataPackFolder.exists()) {
                bukkitDataPackFolder.mkdirs();
            }
            File mcMeta = new File(bukkitDataPackFolder, "pack.mcmeta");
            try {
                com.google.common.io.Files.write("{\n"
                        + "    \"pack\": {\n"
                        + "        \"description\": \"Data pack for resources provided by Bukkit plugins\",\n"
                        + "        \"pack_format\": " + SharedConstants.getCurrentVersion().getPackVersion() + "\n"
                        + "    }\n"
                        + "}\n", mcMeta, com.google.common.base.Charsets.UTF_8);
            } catch (java.io.IOException ex) {
                throw new RuntimeException("Could not initialize Bukkit datapack", ex);
            }
            AtomicReference<DataPackConfig> config = new AtomicReference<>();
            AtomicReference<DynamicOps<Tag>> ops = new AtomicReference<>();
            // CraftBukkit end

            WorldStem worldstem;

            try {
                WorldStem.InitConfig worldstem_b = new WorldStem.InitConfig(resourcepackrepository, Commands.CommandSelection.DEDICATED, dedicatedserversettings.getProperties().functionPermissionLevel, flag);

                worldstem = (WorldStem) WorldStem.load(worldstem_b, () -> {
                    DataPackConfig datapackconfiguration = convertable_conversionsession.getDataPacks();

                    return datapackconfiguration == null ? DataPackConfig.DEFAULT : datapackconfiguration;
                }, (iresourcemanager, datapackconfiguration) -> {
                    RegistryAccess.Writable iregistrycustom_e = RegistryAccess.builtinCopy();
                    DynamicOps<Tag> dynamicops = RegistryOps.createAndLoad(NbtOps.INSTANCE, iregistrycustom_e, iresourcemanager);
                    // CraftBukkit start
                    config.set(datapackconfiguration);
                    ops.set(dynamicops);
                    return Pair.of(null, iregistrycustom_e.freeze());
                    // CraftBukkit end
                    /*
                    SaveData savedata = convertable_conversionsession.getDataTag(dynamicops, datapackconfiguration, iregistrycustom_e.allElementsLifecycle());

                    if (savedata != null) {
                        return Pair.of(savedata, iregistrycustom_e.freeze());
                    } else {
                        WorldSettings worldsettings;
                        GeneratorSettings generatorsettings;

                        if (optionset.has(optionspec2)) {
                            worldsettings = MinecraftServer.DEMO_SETTINGS;
                            generatorsettings = GeneratorSettings.demoSettings(iregistrycustom_e);
                        } else {
                            DedicatedServerProperties dedicatedserverproperties = dedicatedserversettings.getProperties();

                            worldsettings = new WorldSettings(dedicatedserverproperties.levelName, dedicatedserverproperties.gamemode, dedicatedserverproperties.hardcore, dedicatedserverproperties.difficulty, false, new GameRules(), datapackconfiguration);
                            generatorsettings = optionset.has(optionspec3) ? dedicatedserverproperties.getWorldGenSettings(iregistrycustom_e).withBonusChest() : dedicatedserverproperties.getWorldGenSettings(iregistrycustom_e);
                        }

                        WorldDataServer worlddataserver = new WorldDataServer(worldsettings, generatorsettings, Lifecycle.stable());

                        return Pair.of(worlddataserver, iregistrycustom_e.freeze());
                    }
                     */
                }, Util.backgroundExecutor(), Runnable::run).get();
            } catch (Exception exception) {
                Main.LOGGER.warn("Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", exception);
                resourcepackrepository.close();
                return;
            }

            worldstem.updateGlobals();
            /*
            IRegistryCustom.Dimension iregistrycustom_dimension = worldstem.registryAccess();

            dedicatedserversettings.getProperties().getWorldGenSettings(iregistrycustom_dimension);
            SaveData savedata = worldstem.worldData();

            if (optionset.has(optionspec4)) {
                forceUpgrade(convertable_conversionsession, DataConverterRegistry.getDataFixer(), optionset.has(optionspec5), () -> {
                    return true;
                }, savedata.worldGenSettings());
            }

            convertable_conversionsession.saveDataTag(iregistrycustom_dimension, savedata);
            */
            Class.forName(net.minecraft.world.entity.npc.VillagerTrades.class.getName());// Paper - load this sync so it won't fail later async
            final DedicatedServer dedicatedserver = (DedicatedServer) MinecraftServer.spin((thread) -> {
                DedicatedServer dedicatedserver1 = new DedicatedServer(optionset, config.get(), ops.get(), thread, convertable_conversionsession, resourcepackrepository, worldstem, dedicatedserversettings, DataFixers.getDataFixer(), minecraftsessionservice, gameprofilerepository, usercache, LoggerChunkProgressListener::new);

                /*
                dedicatedserver1.setSingleplayerName((String) optionset.valueOf(optionspec8));
                dedicatedserver1.setPort((Integer) optionset.valueOf(optionspec11));
                dedicatedserver1.setDemo(optionset.has(optionspec2));
                dedicatedserver1.setId((String) optionset.valueOf(optionspec12));
                */
                boolean flag1 = !optionset.has("nogui") && !optionset.nonOptionArguments().contains("nogui");

                if(!Boolean.parseBoolean(System.getenv().getOrDefault("PAPER_DISABLE_SERVER_GUI", String.valueOf(false)))) // Paper
                if (flag1 && !GraphicsEnvironment.isHeadless()) {
                    dedicatedserver1.showGui();
                }

                if (optionset.has("port")) {
                    int port = (Integer) optionset.valueOf("port");
                    if (port > 0) {
                        dedicatedserver1.setPort(port);
                    }
                }

                return dedicatedserver1;
            });
            /* CraftBukkit start
            Thread thread = new Thread("Server Shutdown Thread") {
                public void run() {
                    dedicatedserver.halt(true);
                }
            };

            thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(Main.LOGGER));
            Runtime.getRuntime().addShutdownHook(thread);
            */ // CraftBukkit end
        } catch (Exception exception1) {
            Main.LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", exception1);
        }

    }

    // Paper start - load config files
    private static org.bukkit.configuration.file.YamlConfiguration loadConfigFile(File configFile) throws Exception {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        if (configFile.exists()) {
            try {
                config.load(configFile);
            } catch (Exception ex) {
                throw new Exception("Failed to load configuration file: " + configFile.getName(), ex);
            }
        }
        return config;
    }
    // Paper end

    // Paper start - fix and optimise world upgrading
    public static void convertWorldButItWorks(net.minecraft.resources.ResourceKey<net.minecraft.world.level.dimension.LevelStem> dimensionType, net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess worldSession,
                                              DataFixer dataFixer, Optional<net.minecraft.resources.ResourceKey<com.mojang.serialization.Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator>>> generatorKey, boolean removeCaches) {
        int threads = Runtime.getRuntime().availableProcessors() * 3 / 8;
        final ThreadedWorldUpgrader worldUpgrader = new ThreadedWorldUpgrader(dimensionType, worldSession.getLevelId(), worldSession.levelPath.toFile(), threads, dataFixer, generatorKey, removeCaches);
        worldUpgrader.convert();
    }
    // Paper end - fix and optimise world upgrading

    public static void forceUpgrade(LevelStorageSource.LevelStorageAccess session, DataFixer dataFixer, boolean eraseCache, BooleanSupplier continueCheck, WorldGenSettings generatorOptions) {
        Main.LOGGER.info("Forcing world upgrade! {}", session.getLevelId()); // CraftBukkit
        WorldUpgrader worldupgrader = new WorldUpgrader(session, dataFixer, generatorOptions, eraseCache);
        Component ichatbasecomponent = null;

        while (!worldupgrader.isFinished()) {
            Component ichatbasecomponent1 = worldupgrader.getStatus();

            if (ichatbasecomponent != ichatbasecomponent1) {
                ichatbasecomponent = ichatbasecomponent1;
                Main.LOGGER.info(worldupgrader.getStatus().getString());
            }

            int i = worldupgrader.getTotalChunks();

            if (i > 0) {
                int j = worldupgrader.getConverted() + worldupgrader.getSkipped();

                Main.LOGGER.info("{}% completed ({} / {} chunks)...", new Object[]{Mth.floor((float) j / (float) i * 100.0F), j, i});
            }

            if (!continueCheck.getAsBoolean()) {
                worldupgrader.cancel();
            } else {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException interruptedexception) {
                    ;
                }
            }
        }

    }
}
