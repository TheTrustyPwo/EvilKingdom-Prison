package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;

public record GcHeapStat(Instant timestamp, long heapUsed, GcHeapStat.Timing timing) {
    public static GcHeapStat from(RecordedEvent event) {
        return new GcHeapStat(event.getStartTime(), event.getLong("heapUsed"), event.getString("when").equalsIgnoreCase("before gc") ? GcHeapStat.Timing.BEFORE_GC : GcHeapStat.Timing.AFTER_GC);
    }

    public static GcHeapStat.Summary summary(Duration duration, List<GcHeapStat> samples, Duration gcDuration, int count) {
        return new GcHeapStat.Summary(duration, gcDuration, count, calculateAllocationRatePerSecond(samples));
    }

    private static double calculateAllocationRatePerSecond(List<GcHeapStat> samples) {
        long l = 0L;
        Map<GcHeapStat.Timing, List<GcHeapStat>> map = samples.stream().collect(Collectors.groupingBy((gcHeapStatx) -> {
            return gcHeapStatx.timing;
        }));
        List<GcHeapStat> list = map.get(GcHeapStat.Timing.BEFORE_GC);
        List<GcHeapStat> list2 = map.get(GcHeapStat.Timing.AFTER_GC);

        for(int i = 1; i < list.size(); ++i) {
            GcHeapStat gcHeapStat = list.get(i);
            GcHeapStat gcHeapStat2 = list2.get(i - 1);
            l += gcHeapStat.heapUsed - gcHeapStat2.heapUsed;
        }

        Duration duration = Duration.between((samples.get(1)).timestamp, (samples.get(samples.size() - 1)).timestamp);
        return (double)l / (double)duration.getSeconds();
    }

    public static record Summary(Duration duration, Duration gcTotalDuration, int totalGCs, double allocationRateBytesPerSecond) {
        public float gcOverHead() {
            return (float)this.gcTotalDuration.toMillis() / (float)this.duration.toMillis();
        }
    }

    static enum Timing {
        BEFORE_GC,
        AFTER_GC;
    }
}
