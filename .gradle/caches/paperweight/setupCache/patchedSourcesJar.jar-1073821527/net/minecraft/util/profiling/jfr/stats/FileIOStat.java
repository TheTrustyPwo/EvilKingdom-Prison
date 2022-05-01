package net.minecraft.util.profiling.jfr.stats;

import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public record FileIOStat(Duration duration, @Nullable String path, long bytes) {
    public static FileIOStat.Summary summary(Duration duration, List<FileIOStat> samples) {
        long l = samples.stream().mapToLong((sample) -> {
            return sample.bytes;
        }).sum();
        return new FileIOStat.Summary(l, (double)l / (double)duration.getSeconds(), (long)samples.size(), (double)samples.size() / (double)duration.getSeconds(), samples.stream().map(FileIOStat::duration).reduce(Duration.ZERO, Duration::plus), samples.stream().filter((sample) -> {
            return sample.path != null;
        }).collect(Collectors.groupingBy((sample) -> {
            return sample.path;
        }, Collectors.summingLong((sample) -> {
            return sample.bytes;
        }))).entrySet().stream().sorted(Entry.<String, Long>comparingByValue().reversed()).map((entry) -> {
            return Pair.of(entry.getKey(), entry.getValue());
        }).limit(10L).toList());
    }

    public static record Summary(long totalBytes, double bytesPerSecond, long counts, double countsPerSecond, Duration timeSpentInIO, List<Pair<String, Long>> topTenContributorsByTotalBytes) {
    }
}
