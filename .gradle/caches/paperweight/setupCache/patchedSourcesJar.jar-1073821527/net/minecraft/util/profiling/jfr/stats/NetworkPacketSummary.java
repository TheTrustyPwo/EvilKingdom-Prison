package net.minecraft.util.profiling.jfr.stats;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public final class NetworkPacketSummary {
    private final NetworkPacketSummary.PacketCountAndSize totalPacketCountAndSize;
    private final List<Pair<NetworkPacketSummary.PacketIdentification, NetworkPacketSummary.PacketCountAndSize>> largestSizeContributors;
    private final Duration recordingDuration;

    public NetworkPacketSummary(Duration duration, List<Pair<NetworkPacketSummary.PacketIdentification, NetworkPacketSummary.PacketCountAndSize>> packetsToStatistics) {
        this.recordingDuration = duration;
        this.totalPacketCountAndSize = packetsToStatistics.stream().map(Pair::getSecond).reduce(NetworkPacketSummary.PacketCountAndSize::add).orElseGet(() -> {
            return new NetworkPacketSummary.PacketCountAndSize(0L, 0L);
        });
        this.largestSizeContributors = packetsToStatistics.stream().sorted(Comparator.comparing(Pair::getSecond, NetworkPacketSummary.PacketCountAndSize.SIZE_THEN_COUNT)).limit(10L).toList();
    }

    public double getCountsPerSecond() {
        return (double)this.totalPacketCountAndSize.totalCount / (double)this.recordingDuration.getSeconds();
    }

    public double getSizePerSecond() {
        return (double)this.totalPacketCountAndSize.totalSize / (double)this.recordingDuration.getSeconds();
    }

    public long getTotalCount() {
        return this.totalPacketCountAndSize.totalCount;
    }

    public long getTotalSize() {
        return this.totalPacketCountAndSize.totalSize;
    }

    public List<Pair<NetworkPacketSummary.PacketIdentification, NetworkPacketSummary.PacketCountAndSize>> largestSizeContributors() {
        return this.largestSizeContributors;
    }

    public static record PacketCountAndSize(long totalCount, long totalSize) {
        static final Comparator<NetworkPacketSummary.PacketCountAndSize> SIZE_THEN_COUNT = Comparator.comparing(NetworkPacketSummary.PacketCountAndSize::totalSize).thenComparing(NetworkPacketSummary.PacketCountAndSize::totalCount).reversed();

        NetworkPacketSummary.PacketCountAndSize add(NetworkPacketSummary.PacketCountAndSize statistics) {
            return new NetworkPacketSummary.PacketCountAndSize(this.totalCount + statistics.totalCount, this.totalSize + statistics.totalSize);
        }
    }

    public static record PacketIdentification(PacketFlow direction, int protocolId, int packetId) {
        private static final Map<NetworkPacketSummary.PacketIdentification, String> PACKET_NAME_BY_ID;

        public String packetName() {
            return PACKET_NAME_BY_ID.getOrDefault(this, "unknown");
        }

        public static NetworkPacketSummary.PacketIdentification from(RecordedEvent event) {
            return new NetworkPacketSummary.PacketIdentification(event.getEventType().getName().equals("minecraft.PacketSent") ? PacketFlow.CLIENTBOUND : PacketFlow.SERVERBOUND, event.getInt("protocolId"), event.getInt("packetId"));
        }

        static {
            Builder<NetworkPacketSummary.PacketIdentification, String> builder = ImmutableMap.builder();

            for(ConnectionProtocol connectionProtocol : ConnectionProtocol.values()) {
                for(PacketFlow packetFlow : PacketFlow.values()) {
                    Int2ObjectMap<Class<? extends Packet<?>>> int2ObjectMap = connectionProtocol.getPacketsByIds(packetFlow);
                    int2ObjectMap.forEach((packetId, clazz) -> {
                        builder.put(new NetworkPacketSummary.PacketIdentification(packetFlow, connectionProtocol.getId(), packetId), clazz.getSimpleName());
                    });
                }
            }

            PACKET_NAME_BY_ID = builder.build();
        }
    }
}
