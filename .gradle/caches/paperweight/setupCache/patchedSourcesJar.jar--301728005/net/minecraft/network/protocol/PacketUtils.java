package net.minecraft.network.protocol;

import com.mojang.logging.LogUtils;
import net.minecraft.network.PacketListener;
import org.slf4j.Logger;

// CraftBukkit start
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;

public class PacketUtils {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Paper start - detailed watchdog information
    public static final java.util.concurrent.ConcurrentLinkedDeque<PacketListener> packetProcessing = new java.util.concurrent.ConcurrentLinkedDeque<>();
    static final java.util.concurrent.atomic.AtomicLong totalMainThreadPacketsProcessed = new java.util.concurrent.atomic.AtomicLong();

    public static long getTotalProcessedPackets() {
        return totalMainThreadPacketsProcessed.get();
    }

    public static java.util.List<PacketListener> getCurrentPacketProcessors() {
        java.util.List<PacketListener> ret = new java.util.ArrayList<>(4);
        for (PacketListener listener : packetProcessing) {
            ret.add(listener);
        }

        return ret;
    }
    // Paper end - detailed watchdog information

    public PacketUtils() {}

    public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> packet, T listener, ServerLevel world) throws RunningOnDifferentThreadException {
        PacketUtils.ensureRunningOnSameThread(packet, listener, (BlockableEventLoop) world.getServer());
    }

    public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> packet, T listener, BlockableEventLoop<?> engine) throws RunningOnDifferentThreadException {
        if (!engine.isSameThread()) {
            engine.executeIfPossible(() -> {
                packetProcessing.push(listener); // Paper - detailed watchdog information
                try { // Paper - detailed watchdog information
                if (MinecraftServer.getServer().hasStopped() || (listener instanceof ServerGamePacketListenerImpl && ((ServerGamePacketListenerImpl) listener).processedDisconnect)) return; // CraftBukkit, MC-142590
                if (listener.getConnection().isConnected()) {
                    co.aikar.timings.Timing timing = co.aikar.timings.MinecraftTimings.getPacketTiming(packet); // Paper - timings
                    try (co.aikar.timings.Timing ignored = timing.startTiming()) { // Paper - timings
                        packet.handle(listener);
                    } catch (Exception exception) {
                        net.minecraft.network.Connection networkmanager = listener.getConnection();
                        String playerIP = com.destroystokyo.paper.PaperConfig.logPlayerIpAddresses ? String.valueOf(networkmanager.getRemoteAddress()) : "<ip address withheld>"; // Paper
                        if (networkmanager.getPlayer() != null) {
                            LOGGER.error("Error whilst processing packet {} for {}[{}]", packet, networkmanager.getPlayer().getScoreboardName(), playerIP, exception); // Paper
                        } else {
                            LOGGER.error("Error whilst processing packet {} for connection from {}", packet, playerIP, exception); // Paper
                        }
                        net.minecraft.network.chat.TextComponent error = new net.minecraft.network.chat.TextComponent("Packet processing error");
                        networkmanager.send(new net.minecraft.network.protocol.game.ClientboundDisconnectPacket(error), (future) -> {
                            networkmanager.disconnect(error);
                        });
                        networkmanager.setReadOnly();
                    }
                } else {
                    PacketUtils.LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
                }
                // Paper start - detailed watchdog information
                } finally {
                    totalMainThreadPacketsProcessed.getAndIncrement();
                    packetProcessing.pop();
                }
                // Paper end - detailed watchdog information

            });
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
            // CraftBukkit start - SPIGOT-5477, MC-142590
        } else if (MinecraftServer.getServer().hasStopped() || (listener instanceof ServerGamePacketListenerImpl && ((ServerGamePacketListenerImpl) listener).processedDisconnect)) {
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
            // CraftBukkit end
        }
    }
}
