package net.minecraft.util.profiling.jfr.stats;

import com.google.common.base.MoreObjects;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public record ThreadAllocationStat(Instant timestamp, String threadName, long totalBytes) {
    private static final String UNKNOWN_THREAD = "unknown";

    public static ThreadAllocationStat from(RecordedEvent event) {
        RecordedThread recordedThread = event.getThread("thread");
        String string = recordedThread == null ? "unknown" : MoreObjects.firstNonNull(recordedThread.getJavaName(), "unknown");
        return new ThreadAllocationStat(event.getStartTime(), string, event.getLong("allocated"));
    }

    public static ThreadAllocationStat.Summary summary(List<ThreadAllocationStat> samples) {
        Map<String, Double> map = new TreeMap<>();
        Map<String, List<ThreadAllocationStat>> map2 = samples.stream().collect(Collectors.groupingBy((sample) -> {
            return sample.threadName;
        }));
        map2.forEach((threadName, groupedSamples) -> {
            if (groupedSamples.size() >= 2) {
                ThreadAllocationStat threadAllocationStat = groupedSamples.get(0);
                ThreadAllocationStat threadAllocationStat2 = groupedSamples.get(groupedSamples.size() - 1);
                long l = Duration.between(threadAllocationStat.timestamp, threadAllocationStat2.timestamp).getSeconds();
                long m = threadAllocationStat2.totalBytes - threadAllocationStat.totalBytes;
                map.put(threadName, (double)m / (double)l);
            }
        });
        return new ThreadAllocationStat.Summary(map);
    }

    public static record Summary(Map<String, Double> allocationsPerSecondByThread) {
    }
}
