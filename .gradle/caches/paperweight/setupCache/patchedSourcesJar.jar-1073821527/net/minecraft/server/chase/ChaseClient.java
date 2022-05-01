package net.minecraft.server.chase;

import com.google.common.base.Charsets;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ChaseCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class ChaseClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int RECONNECT_INTERVAL_SECONDS = 5;
    private final String serverHost;
    private final int serverPort;
    private final MinecraftServer server;
    private volatile boolean wantsToRun;
    @Nullable
    private Socket socket;
    @Nullable
    private Thread thread;

    public ChaseClient(String ip, int port, MinecraftServer minecraftServer) {
        this.serverHost = ip;
        this.serverPort = port;
        this.server = minecraftServer;
    }

    public void start() {
        if (this.thread != null && this.thread.isAlive()) {
            LOGGER.warn("Remote control client was asked to start, but it is already running. Will ignore.");
        }

        this.wantsToRun = true;
        this.thread = new Thread(this::run, "chase-client");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void stop() {
        this.wantsToRun = false;
        IOUtils.closeQuietly(this.socket);
        this.socket = null;
        this.thread = null;
    }

    public void run() {
        String string = this.serverHost + ":" + this.serverPort;

        while(this.wantsToRun) {
            try {
                LOGGER.info("Connecting to remote control server {}", (Object)string);
                this.socket = new Socket(this.serverHost, this.serverPort);
                LOGGER.info("Connected to remote control server! Will continuously execute the command broadcasted by that server.");

                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), Charsets.US_ASCII));

                    try {
                        while(this.wantsToRun) {
                            String string2 = bufferedReader.readLine();
                            if (string2 == null) {
                                LOGGER.warn("Lost connection to remote control server {}. Will retry in {}s.", string, 5);
                                break;
                            }

                            this.handleMessage(string2);
                        }
                    } catch (Throwable var7) {
                        try {
                            bufferedReader.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }

                        throw var7;
                    }

                    bufferedReader.close();
                } catch (IOException var8) {
                    LOGGER.warn("Lost connection to remote control server {}. Will retry in {}s.", string, 5);
                }
            } catch (IOException var9) {
                LOGGER.warn("Failed to connect to remote control server {}. Will retry in {}s.", string, 5);
            }

            if (this.wantsToRun) {
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException var5) {
                }
            }
        }

    }

    private void handleMessage(String message) {
        try {
            Scanner scanner = new Scanner(new StringReader(message));

            try {
                scanner.useLocale(Locale.ROOT);
                String string = scanner.next();
                if ("t".equals(string)) {
                    this.handleTeleport(scanner);
                } else {
                    LOGGER.warn("Unknown message type '{}'", (Object)string);
                }
            } catch (Throwable var6) {
                try {
                    scanner.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }

                throw var6;
            }

            scanner.close();
        } catch (NoSuchElementException var7) {
            LOGGER.warn("Could not parse message '{}', ignoring", (Object)message);
        }

    }

    private void handleTeleport(Scanner scanner) {
        this.parseTarget(scanner).ifPresent((pos) -> {
            this.executeCommand(String.format(Locale.ROOT, "/execute in %s run tp @s %.3f %.3f %.3f %.3f %.3f", pos.level.location(), pos.pos.x, pos.pos.y, pos.pos.z, pos.rot.y, pos.rot.x));
        });
    }

    private Optional<ChaseClient.TeleportTarget> parseTarget(Scanner scanner) {
        ResourceKey<Level> resourceKey = ChaseCommand.DIMENSION_NAMES.get(scanner.next());
        if (resourceKey == null) {
            return Optional.empty();
        } else {
            float f = scanner.nextFloat();
            float g = scanner.nextFloat();
            float h = scanner.nextFloat();
            float i = scanner.nextFloat();
            float j = scanner.nextFloat();
            return Optional.of(new ChaseClient.TeleportTarget(resourceKey, new Vec3((double)f, (double)g, (double)h), new Vec2(j, i)));
        }
    }

    private void executeCommand(String command) {
        this.server.execute(() -> {
            List<ServerPlayer> list = this.server.getPlayerList().getPlayers();
            if (!list.isEmpty()) {
                ServerPlayer serverPlayer = list.get(0);
                ServerLevel serverLevel = this.server.overworld();
                CommandSourceStack commandSourceStack = new CommandSourceStack(serverPlayer, Vec3.atLowerCornerOf(serverLevel.getSharedSpawnPos()), Vec2.ZERO, serverLevel, 4, "", TextComponent.EMPTY, this.server, serverPlayer);
                Commands commands = this.server.getCommands();
                commands.performCommand(commandSourceStack, command);
            }
        });
    }

    static record TeleportTarget(ResourceKey<Level> level, Vec3 pos, Vec2 rot) {
    }
}
