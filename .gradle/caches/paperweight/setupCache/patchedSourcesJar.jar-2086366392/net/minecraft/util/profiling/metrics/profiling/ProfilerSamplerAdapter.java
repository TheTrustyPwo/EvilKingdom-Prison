package net.minecraft.util.profiling.metrics.profiling;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;

public class ProfilerSamplerAdapter {
    private final Set<String> previouslyFoundSamplerNames = new ObjectOpenHashSet<>();

    public Set<MetricSampler> newSamplersFoundInProfiler(Supplier<ProfileCollector> profilerSupplier) {
        Set<MetricSampler> set = profilerSupplier.get().getChartedPaths().stream().filter((target) -> {
            return !this.previouslyFoundSamplerNames.contains(target.getLeft());
        }).map((target) -> {
            return samplerForProfilingPath(profilerSupplier, target.getLeft(), target.getRight());
        }).collect(Collectors.toSet());

        for(MetricSampler metricSampler : set) {
            this.previouslyFoundSamplerNames.add(metricSampler.getName());
        }

        return set;
    }

    private static MetricSampler samplerForProfilingPath(Supplier<ProfileCollector> profilerSupplier, String id, MetricCategory type) {
        return MetricSampler.create(id, type, () -> {
            ActiveProfiler.PathEntry pathEntry = profilerSupplier.get().getEntry(id);
            return pathEntry == null ? 0.0D : (double)pathEntry.getMaxDuration() / (double)TimeUtil.NANOSECONDS_PER_MILLISECOND;
        });
    }
}
