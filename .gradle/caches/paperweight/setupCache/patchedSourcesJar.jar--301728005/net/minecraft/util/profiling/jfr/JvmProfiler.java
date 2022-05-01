package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.net.SocketAddress;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public interface JvmProfiler {
    JvmProfiler INSTANCE = (JvmProfiler)(Runtime.class.getModule().getLayer().findModule("jdk.jfr").isPresent() ? JfrProfiler.getInstance() : new JvmProfiler.NoOpProfiler());

    boolean start(Environment instanceType);

    Path stop();

    boolean isRunning();

    boolean isAvailable();

    void onServerTick(float tickTime);

    void onPacketReceived(int protocolId, int packetId, SocketAddress remoteAddress, int bytes);

    void onPacketSent(int protocolId, int packetId, SocketAddress remoteAddress, int bytes);

    @Nullable
    ProfiledDuration onWorldLoadedStarted();

    @Nullable
    ProfiledDuration onChunkGenerate(ChunkPos chunkPos, ResourceKey<Level> world, String targetStatus);

    public static class NoOpProfiler implements JvmProfiler {
        private static final Logger LOGGER = LogUtils.getLogger();
        static final ProfiledDuration noOpCommit = () -> {
        };

        @Override
        public boolean start(Environment instanceType) {
            LOGGER.warn("Attempted to start Flight Recorder, but it's not supported on this JVM");
            return false;
        }

        @Override
        public Path stop() {
            throw new IllegalStateException("Attempted to stop Flight Recorder, but it's not supported on this JVM");
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void onPacketReceived(int protocolId, int packetId, SocketAddress remoteAddress, int bytes) {
        }

        @Override
        public void onPacketSent(int protocolId, int packetId, SocketAddress remoteAddress, int bytes) {
        }

        @Override
        public void onServerTick(float tickTime) {
        }

        @Override
        public ProfiledDuration onWorldLoadedStarted() {
            return noOpCommit;
        }

        @Nullable
        @Override
        public ProfiledDuration onChunkGenerate(ChunkPos chunkPos, ResourceKey<Level> world, String targetStatus) {
            return null;
        }
    }
}
