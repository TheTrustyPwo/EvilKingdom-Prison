package net.minecraft.util.profiling.jfr.parse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.NetworkPacketSummary;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;

public class JfrStatsParser {
    private Instant recordingStarted = Instant.EPOCH;
    private Instant recordingEnded = Instant.EPOCH;
    private final List<ChunkGenStat> chunkGenStats = Lists.newArrayList();
    private final List<CpuLoadStat> cpuLoadStat = Lists.newArrayList();
    private final Map<NetworkPacketSummary.PacketIdentification, JfrStatsParser.MutableCountAndSize> receivedPackets = Maps.newHashMap();
    private final Map<NetworkPacketSummary.PacketIdentification, JfrStatsParser.MutableCountAndSize> sentPackets = Maps.newHashMap();
    private final List<FileIOStat> fileWrites = Lists.newArrayList();
    private final List<FileIOStat> fileReads = Lists.newArrayList();
    private int garbageCollections;
    private Duration gcTotalDuration = Duration.ZERO;
    private final List<GcHeapStat> gcHeapStats = Lists.newArrayList();
    private final List<ThreadAllocationStat> threadAllocationStats = Lists.newArrayList();
    private final List<TickTimeStat> tickTimes = Lists.newArrayList();
    @Nullable
    private Duration worldCreationDuration = null;

    private JfrStatsParser(Stream<RecordedEvent> events) {
        this.capture(events);
    }

    public static JfrStatsResult parse(Path path) {
        try {
            final RecordingFile recordingFile = new RecordingFile(path);

            JfrStatsResult var4;
            try {
                Iterator<RecordedEvent> iterator = new Iterator<RecordedEvent>() {
                    @Override
                    public boolean hasNext() {
                        return recordingFile.hasMoreEvents();
                    }

                    @Override
                    public RecordedEvent next() {
                        if (!this.hasNext()) {
                            throw new NoSuchElementException();
                        } else {
                            try {
                                return recordingFile.readEvent();
                            } catch (IOException var2) {
                                throw new UncheckedIOException(var2);
                            }
                        }
                    }
                };
                Stream<RecordedEvent> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 1297), false);
                var4 = (new JfrStatsParser(stream)).results();
            } catch (Throwable var6) {
                try {
                    recordingFile.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }

                throw var6;
            }

            recordingFile.close();
            return var4;
        } catch (IOException var7) {
            throw new UncheckedIOException(var7);
        }
    }

    private JfrStatsResult results() {
        Duration duration = Duration.between(this.recordingStarted, this.recordingEnded);
        return new JfrStatsResult(this.recordingStarted, this.recordingEnded, duration, this.worldCreationDuration, this.tickTimes, this.cpuLoadStat, GcHeapStat.summary(duration, this.gcHeapStats, this.gcTotalDuration, this.garbageCollections), ThreadAllocationStat.summary(this.threadAllocationStats), collectPacketStats(duration, this.receivedPackets), collectPacketStats(duration, this.sentPackets), FileIOStat.summary(duration, this.fileWrites), FileIOStat.summary(duration, this.fileReads), this.chunkGenStats);
    }

    private void capture(Stream<RecordedEvent> events) {
        events.forEach((event) -> {
            if (event.getEndTime().isAfter(this.recordingEnded) || this.recordingEnded.equals(Instant.EPOCH)) {
                this.recordingEnded = event.getEndTime();
            }

            if (event.getStartTime().isBefore(this.recordingStarted) || this.recordingStarted.equals(Instant.EPOCH)) {
                this.recordingStarted = event.getStartTime();
            }

            String var2 = event.getEventType().getName();
            switch(var2) {
            case "minecraft.ChunkGeneration":
                this.chunkGenStats.add(ChunkGenStat.from(event));
                break;
            case "minecraft.LoadWorld":
                this.worldCreationDuration = event.getDuration();
                break;
            case "minecraft.ServerTickTime":
                this.tickTimes.add(TickTimeStat.from(event));
                break;
            case "minecraft.PacketReceived":
                this.incrementPacket(event, event.getInt("bytes"), this.receivedPackets);
                break;
            case "minecraft.PacketSent":
                this.incrementPacket(event, event.getInt("bytes"), this.sentPackets);
                break;
            case "jdk.ThreadAllocationStatistics":
                this.threadAllocationStats.add(ThreadAllocationStat.from(event));
                break;
            case "jdk.GCHeapSummary":
                this.gcHeapStats.add(GcHeapStat.from(event));
                break;
            case "jdk.CPULoad":
                this.cpuLoadStat.add(CpuLoadStat.from(event));
                break;
            case "jdk.FileWrite":
                this.appendFileIO(event, this.fileWrites, "bytesWritten");
                break;
            case "jdk.FileRead":
                this.appendFileIO(event, this.fileReads, "bytesRead");
                break;
            case "jdk.GarbageCollection":
                ++this.garbageCollections;
                this.gcTotalDuration = this.gcTotalDuration.plus(event.getDuration());
            }

        });
    }

    private void incrementPacket(RecordedEvent event, int bytes, Map<NetworkPacketSummary.PacketIdentification, JfrStatsParser.MutableCountAndSize> packetsToCounter) {
        packetsToCounter.computeIfAbsent(NetworkPacketSummary.PacketIdentification.from(event), (packet) -> {
            return new JfrStatsParser.MutableCountAndSize();
        }).increment(bytes);
    }

    private void appendFileIO(RecordedEvent event, List<FileIOStat> samples, String bytesKey) {
        samples.add(new FileIOStat(event.getDuration(), event.getString("path"), event.getLong(bytesKey)));
    }

    private static NetworkPacketSummary collectPacketStats(Duration duration, Map<NetworkPacketSummary.PacketIdentification, JfrStatsParser.MutableCountAndSize> packetsToCounter) {
        List<Pair<NetworkPacketSummary.PacketIdentification, NetworkPacketSummary.PacketCountAndSize>> list = packetsToCounter.entrySet().stream().map((entry) -> {
            return Pair.of(entry.getKey(), entry.getValue().toCountAndSize());
        }).toList();
        return new NetworkPacketSummary(duration, list);
    }

    public static final class MutableCountAndSize {
        private long count;
        private long totalSize;

        public void increment(int bytes) {
            this.totalSize += (long)bytes;
            ++this.count;
        }

        public NetworkPacketSummary.PacketCountAndSize toCountAndSize() {
            return new NetworkPacketSummary.PacketCountAndSize(this.count, this.totalSize);
        }
    }
}
