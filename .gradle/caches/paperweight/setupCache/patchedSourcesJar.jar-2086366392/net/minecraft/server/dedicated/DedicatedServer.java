package net.minecraft.server.dedicated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.DefaultUncaughtExceptionHandlerWithName;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ConsoleInput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.WorldStem;
import net.minecraft.server.gui.MinecraftServerGui;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.network.TextFilterClient;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import net.minecraft.server.rcon.thread.RconThread;
import net.minecraft.util.Mth;
import net.minecraft.util.monitoring.jmx.MinecraftServerStatistics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

// CraftBukkit start
import com.mojang.serialization.DynamicOps;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.io.IoBuilder;
import org.bukkit.command.CommandSender;
import co.aikar.timings.MinecraftTimings; // Paper
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.craftbukkit.v1_18_R2.util.Waitable;
import org.bukkit.event.server.RemoteServerCommandEvent;
// CraftBukkit end

public class DedicatedServer extends MinecraftServer implements ServerInterface {

    static final Logger LOGGER = LogUtils.getLogger();
    private static final int CONVERSION_RETRY_DELAY_MS = 5000;
    private static final int CONVERSION_RETRIES = 2;
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    private final java.util.Queue<ConsoleInput> serverCommandQueue = new java.util.concurrent.ConcurrentLinkedQueue<>(); // Paper - use a proper queuemmands
    @Nullable
    private QueryThreadGs4 queryThreadGs4;
    public final RconConsoleSource rconConsoleSource;
    @Nullable
    private RconThread rconThread;
    public DedicatedServerSettings settings;
    @Nullable
    private MinecraftServerGui gui;
    @Nullable
    private final TextFilterClient textFilterClient;
    @Nullable
    private final Component resourcePackPrompt;

    // CraftBukkit start - Signature changed
    public DedicatedServer(joptsimple.OptionSet options, DataPackConfig datapackconfiguration, DynamicOps<Tag> registryreadops, Thread thread, LevelStorageSource.LevelStorageAccess convertable_conversionsession, PackRepository resourcepackrepository, WorldStem worldstem, DedicatedServerSettings dedicatedserversettings, DataFixer datafixer, MinecraftSessionService minecraftsessionservice, GameProfileRepository gameprofilerepository, GameProfileCache usercache, ChunkProgressListenerFactory worldloadlistenerfactory) {
        super(options, datapackconfiguration, registryreadops, thread, convertable_conversionsession, resourcepackrepository, worldstem, Proxy.NO_PROXY, datafixer, minecraftsessionservice, gameprofilerepository, usercache, worldloadlistenerfactory);
        // CraftBukkit end
        this.settings = dedicatedserversettings;
        this.rconConsoleSource = new RconConsoleSource(this);
        this.textFilterClient = TextFilterClient.createFromConfig(dedicatedserversettings.getProperties().textFilteringConfig);
        this.resourcePackPrompt = DedicatedServer.parseResourcePackPrompt(dedicatedserversettings);
    }

    @Override
    public boolean initServer() throws IOException {
        Thread thread = new Thread("Server console handler") {
            public void run() {
                // CraftBukkit start
                if (!org.bukkit.craftbukkit.v1_18_R2.Main.useConsole) {
                    return;
                }
                // Paper start - Use TerminalConsoleAppender
                new com.destroystokyo.paper.console.PaperConsole(DedicatedServer.this).start();
                /*
                jline.console.ConsoleReader bufferedreader = reader;

                // MC-33041, SPIGOT-5538: if System.in is not valid due to javaw, then return
                try {
                    System.in.available();
                } catch (IOException ex) {
                    return;
                }
                // CraftBukkit end

                String s;

                try {
                    // CraftBukkit start - JLine disabling compatibility
                    while (!DedicatedServer.this.isStopped() && DedicatedServer.this.isRunning()) {
                        if (org.bukkit.craftbukkit.v1_18_R2.Main.useJline) {
                            s = bufferedreader.readLine(">", null);
                        } else {
                            s = bufferedreader.readLine();
                        }

                        // SPIGOT-5220: Throttle if EOF (ctrl^d) or stdin is /dev/null
                        if (s == null) {
                            try {
                                Thread.sleep(50L);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                            continue;
                        }
                        if (s.trim().length() > 0) { // Trim to filter lines which are just spaces
                            DedicatedServer.this.issueCommand(s, DedicatedServer.this.getServerCommandListener());
                        }
                        // CraftBukkit end
                    }
                } catch (IOException ioexception) {
                    DedicatedServer.LOGGER.error("Exception handling console input", ioexception);
                }

                */
                // Paper end
            }
        };

        // CraftBukkit start - TODO: handle command-line logging arguments
        java.util.logging.Logger global = java.util.logging.Logger.getLogger("");
        global.setUseParentHandlers(false);
        for (java.util.logging.Handler handler : global.getHandlers()) {
            global.removeHandler(handler);
        }
        global.addHandler(new org.bukkit.craftbukkit.v1_18_R2.util.ForwardLogHandler());

        // Paper start - Not needed with TerminalConsoleAppender
        final org.apache.logging.log4j.Logger logger = LogManager.getRootLogger();
        /*
        final org.apache.logging.log4j.core.Logger logger = ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger());
        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
            if (appender instanceof org.apache.logging.log4j.core.appender.ConsoleAppender) {
                logger.removeAppender(appender);
            }
        }

        new org.bukkit.craftbukkit.v1_18_R2.util.TerminalConsoleWriterThread(System.out, this.reader).start();
        */
        // Paper end

        System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(logger).setLevel(Level.WARN).buildPrintStream());
        // CraftBukkit end

        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(DedicatedServer.LOGGER));
        // thread.start(); // Paper - moved down
        DedicatedServer.LOGGER.info("Starting minecraft server version {}", SharedConstants.getCurrentVersion().getName());
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            DedicatedServer.LOGGER.warn("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        // Paper start - detect running as root
        if (io.papermc.paper.util.ServerEnvironment.userIsRootOrAdmin()) {
            DedicatedServer.LOGGER.warn("****************************");
            DedicatedServer.LOGGER.warn("YOU ARE RUNNING THIS SERVER AS AN ADMINISTRATIVE OR ROOT USER. THIS IS NOT ADVISED.");
            DedicatedServer.LOGGER.warn("YOU ARE OPENING YOURSELF UP TO POTENTIAL RISKS WHEN DOING THIS.");
            DedicatedServer.LOGGER.warn("FOR MORE INFORMATION, SEE https://madelinemiller.dev/blog/root-minecraft-server/");
            DedicatedServer.LOGGER.warn("****************************");
        }
        // Paper end

        DedicatedServer.LOGGER.info("Loading properties");
        DedicatedServerProperties dedicatedserverproperties = this.settings.getProperties();

        if (this.isSingleplayer()) {
            this.setLocalIp("127.0.0.1");
        } else {
            this.setUsesAuthentication(dedicatedserverproperties.onlineMode);
            this.setPreventProxyConnections(dedicatedserverproperties.preventProxyConnections);
            this.setLocalIp(dedicatedserverproperties.serverIp);
        }
        // Spigot start
        this.setPlayerList(new DedicatedPlayerList(this, this.registryHolder, this.playerDataStorage));
        org.spigotmc.SpigotConfig.init((java.io.File) options.valueOf("spigot-settings"));
        org.spigotmc.SpigotConfig.registerCommands();
        // Spigot end
        // Paper start - moved up to right after PlayerList creation but before file load/save
        if (this.convertOldUsers()) {
            this.getProfileCache().save(false); // Paper
        }
        this.getPlayerList().loadAndSaveFiles(); // Must be after convertNames
        // Paper end
        // Paper start
        try {
            com.destroystokyo.paper.PaperConfig.init((java.io.File) options.valueOf("paper-settings"));
        } catch (Exception e) {
            DedicatedServer.LOGGER.error("Unable to load server configuration", e);
            return false;
        }
        thread.start(); // Paper - start console thread after MinecraftServer.console & PaperConfig are initialized
        com.destroystokyo.paper.PaperConfig.registerCommands();
        com.destroystokyo.paper.VersionHistoryManager.INSTANCE.getClass(); // load version history now
        io.papermc.paper.util.ObfHelper.INSTANCE.getClass(); // load mappings for stacktrace deobf and etc.
        io.papermc.paper.brigadier.PaperBrigadierProviderImpl.INSTANCE.getClass(); // init PaperBrigadierProvider
        // Paper end

        this.setPvpAllowed(dedicatedserverproperties.pvp);
        this.setFlightAllowed(dedicatedserverproperties.allowFlight);
        this.setResourcePack(dedicatedserverproperties.resourcePack, this.getPackHash());
        this.setMotd(dedicatedserverproperties.motd);
        super.setPlayerIdleTimeout((Integer) dedicatedserverproperties.playerIdleTimeout.get());
        this.setEnforceWhitelist(dedicatedserverproperties.enforceWhitelist);
        // this.worldData.setGameType(dedicatedserverproperties.gamemode); // CraftBukkit - moved to world loading
        DedicatedServer.LOGGER.info("Default game type: {}", dedicatedserverproperties.gamemode);
        // Paper start - Unix domain socket support
        java.net.SocketAddress bindAddress;
        if (this.getLocalIp().startsWith("unix:")) {
            if (!io.netty.channel.epoll.Epoll.isAvailable()) {
                DedicatedServer.LOGGER.error("**** INVALID CONFIGURATION!");
                DedicatedServer.LOGGER.error("You are trying to use a Unix domain socket but you're not on a supported OS.");
                return false;
            } else if (!com.destroystokyo.paper.PaperConfig.velocitySupport && !org.spigotmc.SpigotConfig.bungee) {
                DedicatedServer.LOGGER.error("**** INVALID CONFIGURATION!");
                DedicatedServer.LOGGER.error("Unix domain sockets require IPs to be forwarded from a proxy.");
                return false;
            }
            bindAddress = new io.netty.channel.unix.DomainSocketAddress(this.getLocalIp().substring("unix:".length()));
        } else {
        InetAddress inetaddress = null;

        if (!this.getLocalIp().isEmpty()) {
            inetaddress = InetAddress.getByName(this.getLocalIp());
        }

        if (this.getPort() < 0) {
            this.setPort(dedicatedserverproperties.serverPort);
        }
        bindAddress = new java.net.InetSocketAddress(inetaddress, this.getPort());
        }
        // Paper end

        this.initializeKeyPair();
        DedicatedServer.LOGGER.info("Starting Minecraft server on {}:{}", this.getLocalIp().isEmpty() ? "*" : this.getLocalIp(), this.getPort());

        try {
            this.getConnection().bind(bindAddress); // Paper - Unix domain socket support
        } catch (IOException ioexception) {
            DedicatedServer.LOGGER.warn("**** FAILED TO BIND TO PORT!");
            DedicatedServer.LOGGER.warn("The exception was: {}", ioexception.toString());
            DedicatedServer.LOGGER.warn("Perhaps a server is already running on that port?");
            return false;
        }

        // CraftBukkit start
        // this.setPlayerList(new DedicatedPlayerList(this, this.registryHolder, this.playerDataStorage)); // Spigot - moved up
        server.loadPlugins();
        server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.STARTUP);
        // CraftBukkit end

        if (!this.usesAuthentication()) {
            DedicatedServer.LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            DedicatedServer.LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
            // Spigot start
            if (org.spigotmc.SpigotConfig.bungee) {
                DedicatedServer.LOGGER.warn("Whilst this makes it possible to use BungeeCord, unless access to your server is properly restricted, it also opens up the ability for hackers to connect with any username they choose.");
                DedicatedServer.LOGGER.warn("Please see http://www.spigotmc.org/wiki/firewall-guide/ for further information.");
            } else {
                DedicatedServer.LOGGER.warn("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            }
            // Spigot end
            DedicatedServer.LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
        }


        if (!OldUsersConverter.serverReadyAfterUserconversion(this)) {
            return false;
        } else {
            // this.setPlayerList(new DedicatedPlayerList(this, this.registryAccess(), this.playerDataStorage)); // CraftBukkit - moved up
            long i = Util.getNanos();

            SkullBlockEntity.setup(this.getProfileCache(), this.getSessionService(), this);
            GameProfileCache.setUsesAuthentication(this.usesAuthentication());
            DedicatedServer.LOGGER.info("Preparing level \"{}\"", this.getLevelIdName());
            this.loadLevel(storageSource.getLevelId()); // CraftBukkit
            long j = Util.getNanos() - i;
            String s = String.format(Locale.ROOT, "%.3fs", (double) j / 1.0E9D);

            //DedicatedServer.LOGGER.info("Done ({})! For help, type \"help\"", s); // Paper moved to after init
            if (dedicatedserverproperties.announcePlayerAchievements != null) {
                ((GameRules.BooleanValue) this.getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)).set(dedicatedserverproperties.announcePlayerAchievements, null); // Paper
            }

            if (dedicatedserverproperties.enableQuery) {
                DedicatedServer.LOGGER.info("Starting GS4 status listener");
                this.queryThreadGs4 = QueryThreadGs4.create(this);
            }

            if (dedicatedserverproperties.enableRcon) {
                DedicatedServer.LOGGER.info("Starting remote control listener");
                this.rconThread = RconThread.create(this);
                this.remoteConsole = new org.bukkit.craftbukkit.v1_18_R2.command.CraftRemoteConsoleCommandSender(this.rconConsoleSource); // CraftBukkit
            }

            if (false && this.getMaxTickLength() > 0L) {  // Spigot - disable
                Thread thread1 = new Thread(new ServerWatchdog(this));

                thread1.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandlerWithName(DedicatedServer.LOGGER));
                thread1.setName("Server Watchdog");
                thread1.setDaemon(true);
                thread1.start();
            }

            Items.AIR.fillItemCategory(CreativeModeTab.TAB_SEARCH, NonNullList.create());
            if (dedicatedserverproperties.enableJmxMonitoring) {
                MinecraftServerStatistics.registerJmxMonitoring(this);
                DedicatedServer.LOGGER.info("JMX monitoring enabled");
            }

            return true;
        }
    }

    @Override
    public boolean isSpawningAnimals() {
        return this.getProperties().spawnAnimals && super.isSpawningAnimals();
    }

    @Override
    public boolean isSpawningMonsters() {
        return this.settings.getProperties().spawnMonsters && super.isSpawningMonsters();
    }

    @Override
    public boolean areNpcsEnabled() {
        return this.settings.getProperties().spawnNpcs && super.areNpcsEnabled();
    }

    public String getPackHash() {
        DedicatedServerProperties dedicatedserverproperties = this.settings.getProperties();
        String s;

        if (!dedicatedserverproperties.resourcePackSha1.isEmpty()) {
            s = dedicatedserverproperties.resourcePackSha1;
            if (!Strings.isNullOrEmpty(dedicatedserverproperties.resourcePackHash)) {
                DedicatedServer.LOGGER.warn("resource-pack-hash is deprecated and found along side resource-pack-sha1. resource-pack-hash will be ignored.");
            }
        } else if (!Strings.isNullOrEmpty(dedicatedserverproperties.resourcePackHash)) {
            DedicatedServer.LOGGER.warn("resource-pack-hash is deprecated. Please use resource-pack-sha1 instead.");
            s = dedicatedserverproperties.resourcePackHash;
        } else {
            s = "";
        }

        if (!s.isEmpty() && !DedicatedServer.SHA1.matcher(s).matches()) {
            DedicatedServer.LOGGER.warn("Invalid sha1 for ressource-pack-sha1");
        }

        if (!dedicatedserverproperties.resourcePack.isEmpty() && s.isEmpty()) {
            DedicatedServer.LOGGER.warn("You specified a resource pack without providing a sha1 hash. Pack will be updated on the client only if you change the name of the pack.");
        }

        return s;
    }

    @Override
    public DedicatedServerProperties getProperties() {
        return this.settings.getProperties();
    }

    @Override
    public void forceDifficulty() {
        //this.a(this.getDedicatedServerProperties().difficulty, true); // Paper - Don't overwrite level.dat's difficulty, keep current
    }

    @Override
    public boolean isHardcore() {
        return this.getProperties().hardcore;
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport details) {
        details.setDetail("Is Modded", () -> {
            return this.getModdedStatus().fullDescription();
        });
        details.setDetail("Type", () -> {
            return "Dedicated Server (map_server.txt)";
        });
        return details;
    }

    @Override
    public void dumpServerProperties(Path file) throws IOException {
        DedicatedServerProperties dedicatedserverproperties = this.getProperties();
        BufferedWriter bufferedwriter = Files.newBufferedWriter(file);

        try {
            bufferedwriter.write(String.format("sync-chunk-writes=%s%n", dedicatedserverproperties.syncChunkWrites));
            bufferedwriter.write(String.format("gamemode=%s%n", dedicatedserverproperties.gamemode));
            bufferedwriter.write(String.format("spawn-monsters=%s%n", dedicatedserverproperties.spawnMonsters));
            bufferedwriter.write(String.format("entity-broadcast-range-percentage=%d%n", dedicatedserverproperties.entityBroadcastRangePercentage));
            bufferedwriter.write(String.format("max-world-size=%d%n", dedicatedserverproperties.maxWorldSize));
            bufferedwriter.write(String.format("spawn-npcs=%s%n", dedicatedserverproperties.spawnNpcs));
            bufferedwriter.write(String.format("view-distance=%d%n", dedicatedserverproperties.viewDistance));
            bufferedwriter.write(String.format("simulation-distance=%d%n", dedicatedserverproperties.simulationDistance));
            bufferedwriter.write(String.format("spawn-animals=%s%n", dedicatedserverproperties.spawnAnimals));
            bufferedwriter.write(String.format("generate-structures=%s%n", dedicatedserverproperties.getWorldGenSettings(this.registryAccess()).generateFeatures()));
            bufferedwriter.write(String.format("use-native=%s%n", dedicatedserverproperties.useNativeTransport));
            bufferedwriter.write(String.format("rate-limit=%d%n", dedicatedserverproperties.rateLimitPacketsPerSecond));
        } catch (Throwable throwable) {
            if (bufferedwriter != null) {
                try {
                    bufferedwriter.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            }

            throw throwable;
        }

        if (bufferedwriter != null) {
            bufferedwriter.close();
        }

    }

    @Override
    public void onServerExit() {
        if (this.textFilterClient != null) {
            this.textFilterClient.close();
        }

        if (this.gui != null) {
            this.gui.close();
        }

        if (this.rconThread != null) {
            //this.remoteControlListener.b(); // Paper - don't wait for remote connections
        }

        if (this.queryThreadGs4 != null) {
            //this.remoteStatusListener.b(); // Paper - don't wait for remote connections
        }

        hasFullyShutdown = true; // Paper
        System.exit(this.abnormalExit ? 70 : 0); // CraftBukkit // Paper
    }

    @Override
    public void tickChildren(BooleanSupplier shouldKeepTicking) {
        super.tickChildren(shouldKeepTicking);
        this.handleConsoleInputs();
    }

    @Override
    public boolean isNetherEnabled() {
        return this.getProperties().allowNether;
    }

    public void handleConsoleInput(String command, CommandSourceStack commandSource) {
        this.serverCommandQueue.add(new ConsoleInput(command, commandSource));
    }

    public void handleConsoleInputs() {
        MinecraftTimings.serverCommandTimer.startTiming(); // Spigot
        // Paper start - use proper queue
        ConsoleInput servercommand;
        while ((servercommand = this.serverCommandQueue.poll()) != null) {
            // Paper end

            // CraftBukkit start - ServerCommand for preprocessing
            ServerCommandEvent event = new ServerCommandEvent(console, servercommand.msg);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) continue;
            servercommand = new ConsoleInput(event.getCommand(), servercommand.source);

            // this.getCommands().performCommand(servercommand.source, servercommand.msg); // Called in dispatchServerCommand
            server.dispatchServerCommand(console, servercommand);
            // CraftBukkit end
        }

        MinecraftTimings.serverCommandTimer.stopTiming(); // Spigot
    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return this.getProperties().rateLimitPacketsPerSecond;
    }

    @Override
    public boolean isEpollEnabled() {
        return this.getProperties().useNativeTransport;
    }

    @Override
    public DedicatedPlayerList getPlayerList() {
        return (DedicatedPlayerList) super.getPlayerList();
    }

    @Override
    public boolean isPublished() {
        return true;
    }

    @Override
    public String getServerIp() {
        return this.getLocalIp();
    }

    @Override
    public int getServerPort() {
        return this.getPort();
    }

    @Override
    public String getServerName() {
        return this.getMotd();
    }

    public void showGui() {
        if (this.gui == null) {
            this.gui = MinecraftServerGui.showFrameFor(this);
        }

    }

    @Override
    public boolean hasGui() {
        return this.gui != null;
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return this.getProperties().enableCommandBlock;
    }

    @Override
    public int getSpawnProtectionRadius() {
        return this.getProperties().spawnProtection;
    }

    @Override
    public boolean isUnderSpawnProtection(ServerLevel world, BlockPos pos, Player player) {
        if (world.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return false;
        } else if (this.getPlayerList().getOps().isEmpty()) {
            return false;
        } else if (this.getPlayerList().isOp(player.getGameProfile())) {
            return false;
        } else if (this.getSpawnProtectionRadius() <= 0) {
            return false;
        } else {
            BlockPos blockposition1 = world.getSharedSpawnPos();
            int i = Mth.abs(pos.getX() - blockposition1.getX());
            int j = Mth.abs(pos.getZ() - blockposition1.getZ());
            int k = Math.max(i, j);

            return k <= this.getSpawnProtectionRadius();
        }
    }

    @Override
    public boolean repliesToStatus() {
        return this.getProperties().enableStatus;
    }

    @Override
    public boolean hidesOnlinePlayers() {
        return this.getProperties().hideOnlinePlayers;
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return this.getProperties().opPermissionLevel;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return this.getProperties().functionPermissionLevel;
    }

    @Override
    public void setPlayerIdleTimeout(int playerIdleTimeout) {
        super.setPlayerIdleTimeout(playerIdleTimeout);
        this.settings.update((dedicatedserverproperties) -> {
            return (DedicatedServerProperties) dedicatedserverproperties.playerIdleTimeout.update(this.registryAccess(), playerIdleTimeout);
        });
    }

    @Override
    public boolean shouldRconBroadcast() {
        return this.getProperties().broadcastRconToOps;
    }

    @Override
    public boolean shouldInformAdmins() {
        return this.getProperties().broadcastConsoleToOps;
    }

    @Override
    public int getAbsoluteMaxWorldSize() {
        return this.getProperties().maxWorldSize;
    }

    @Override
    public int getCompressionThreshold() {
        return this.getProperties().networkCompressionThreshold;
    }

    protected boolean convertOldUsers() {
        boolean flag = false;

        int i;

        for (i = 0; !flag && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
                this.waitForRetry();
            }

            flag = OldUsersConverter.convertUserBanlist(this);
        }

        boolean flag1 = false;

        for (i = 0; !flag1 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
                this.waitForRetry();
            }

            flag1 = OldUsersConverter.convertIpBanlist(this);
        }

        boolean flag2 = false;

        for (i = 0; !flag2 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
                this.waitForRetry();
            }

            flag2 = OldUsersConverter.convertOpsList(this);
        }

        boolean flag3 = false;

        for (i = 0; !flag3 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
                this.waitForRetry();
            }

            flag3 = OldUsersConverter.convertWhiteList(this);
        }

        boolean flag4 = false;

        for (i = 0; !flag4 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
                this.waitForRetry();
            }

            flag4 = OldUsersConverter.convertPlayers(this);
        }

        return flag || flag1 || flag2 || flag3 || flag4;
    }

    private void waitForRetry() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException interruptedexception) {
            ;
        }
    }

    public long getMaxTickLength() {
        return this.getProperties().maxTickTime;
    }

    @Override
    public String getPluginNames() {
        // CraftBukkit start - Whole method
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = server.getPluginManager().getPlugins();

        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0 && server.getQueryPlugins()) {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++) {
                if (i > 0) {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
        // CraftBukkit end
    }

    @Override
    public String runCommand(String command) {
        Waitable[] waitableArray = new Waitable[1];
        this.rconConsoleSource.prepareForCommand();
        this.executeBlocking(() -> {
            // CraftBukkit start - fire RemoteServerCommandEvent
            RemoteServerCommandEvent event = new RemoteServerCommandEvent(remoteConsole, command);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            // Paper start
            if (command.toLowerCase().startsWith("timings") && command.toLowerCase().matches("timings (report|paste|get|merged|seperate)")) {
                org.bukkit.command.BufferedCommandSender sender = new org.bukkit.command.BufferedCommandSender();
                Waitable<String> waitable = new Waitable<String>() {
                    @Override
                    protected String evaluate() {
                        return sender.getBuffer();
                    }
                };
                waitableArray[0] = waitable;
                co.aikar.timings.Timings.generateReport(new co.aikar.timings.TimingsReportListener(sender, waitable));
            } else {
            // Paper end
            ConsoleInput serverCommand = new ConsoleInput(event.getCommand(), this.rconConsoleSource.createCommandSourceStack());
            server.dispatchServerCommand(remoteConsole, serverCommand);
            } // Paper
            // CraftBukkit end
        });
        // Paper start
        if (waitableArray[0] != null) {
            //noinspection unchecked
            Waitable<String> waitable = waitableArray[0];
            try {
                return waitable.get();
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Exception processing rcon command " + command, e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Maintain interrupted state
                throw new RuntimeException("Interrupted processing rcon command " + command, e);
            }

        }
        // Paper end
        return this.rconConsoleSource.getCommandResponse();
    }

    public void storeUsingWhiteList(boolean useWhitelist) {
        this.settings.update((dedicatedserverproperties) -> {
            return (DedicatedServerProperties) dedicatedserverproperties.whiteList.update(this.registryAccess(), useWhitelist);
        });
    }

    @Override
    public void stopServer() {
        super.stopServer();
        //Util.shutdownExecutors(); // Paper - moved into super
        SkullBlockEntity.clear();
    }

    @Override
    public boolean isSingleplayerOwner(GameProfile profile) {
        return false;
    }

    @Override
    public int getScaledTrackingDistance(int initialDistance) {
        return this.getProperties().entityBroadcastRangePercentage * initialDistance / 100;
    }

    @Override
    public String getLevelIdName() {
        return this.storageSource.getLevelId();
    }

    @Override
    public boolean forceSynchronousWrites() {
        return this.settings.getProperties().syncChunkWrites;
    }

    @Override
    public TextFilter createTextFilterForPlayer(ServerPlayer player) {
        return this.textFilterClient != null ? this.textFilterClient.createContext(player.getGameProfile()) : TextFilter.DUMMY;
    }

    @Override
    public boolean isResourcePackRequired() {
        return this.settings.getProperties().requireResourcePack;
    }

    @Nullable
    @Override
    public GameType getForcedGameType() {
        return this.settings.getProperties().forceGameMode ? this.worldData.getGameType() : null;
    }

    @Nullable
    private static Component parseResourcePackPrompt(DedicatedServerSettings propertiesLoader) {
        String s = propertiesLoader.getProperties().resourcePackPrompt;

        if (!Strings.isNullOrEmpty(s)) {
            try {
                return Component.Serializer.fromJson(s);
            } catch (Exception exception) {
                DedicatedServer.LOGGER.warn("Failed to parse resource pack prompt '{}'", s, exception);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Component getResourcePackPrompt() {
        return this.resourcePackPrompt;
    }

    // CraftBukkit start
    public boolean isDebugging() {
        return this.getProperties().debug;
    }

    @Override
    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return console;
    }
    // CraftBukkit end
}
