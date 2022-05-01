package net.minecraft.util.profiling.metrics;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class MetricsRegistry {
    public static final MetricsRegistry INSTANCE = new MetricsRegistry();
    private final WeakHashMap<ProfilerMeasured, Void> measuredInstances = new WeakHashMap<>();

    private MetricsRegistry() {
    }

    public void add(ProfilerMeasured executor) {
        this.measuredInstances.put(executor, (Void)null);
    }

    public List<MetricSampler> getRegisteredSamplers() {
        Map<String, List<MetricSampler>> map = this.measuredInstances.keySet().stream().flatMap((executor) -> {
            return executor.profiledMetrics().stream();
        }).collect(Collectors.groupingBy(MetricSampler::getName));
        return aggregateDuplicates(map);
    }

    private static List<MetricSampler> aggregateDuplicates(Map<String, List<MetricSampler>> samplers) {
        return samplers.entrySet().stream().map((entry) -> {
            String string = entry.getKey();
            List<MetricSampler> list = entry.getValue();
            return (MetricSampler)(list.size() > 1 ? new MetricsRegistry.AggregatedMetricSampler(string, list) : list.get(0));
        }).collect(Collectors.toList());
    }

    static class AggregatedMetricSampler extends MetricSampler {
        private final List<MetricSampler> delegates;

        AggregatedMetricSampler(String id, List<MetricSampler> delegates) {
            super(id, delegates.get(0).getCategory(), () -> {
                return averageValueFromDelegates(delegates);
            }, () -> {
                beforeTick(delegates);
            }, thresholdTest(delegates));
            this.delegates = delegates;
        }

        private static MetricSampler.ThresholdTest thresholdTest(List<MetricSampler> delegates) {
            return (value) -> {
                return delegates.stream().anyMatch((sampler) -> {
                    return sampler.thresholdTest != null ? sampler.thresholdTest.test(value) : false;
                });
            };
        }

        private static void beforeTick(List<MetricSampler> samplers) {
            for(MetricSampler metricSampler : samplers) {
                metricSampler.onStartTick();
            }

        }

        private static double averageValueFromDelegates(List<MetricSampler> samplers) {
            double d = 0.0D;

            for(MetricSampler metricSampler : samplers) {
                d += metricSampler.getSampler().getAsDouble();
            }

            return d / (double)samplers.size();
        }

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                if (!super.equals(object)) {
                    return false;
                } else {
                    MetricsRegistry.AggregatedMetricSampler aggregatedMetricSampler = (MetricsRegistry.AggregatedMetricSampler)object;
                    return this.delegates.equals(aggregatedMetricSampler.delegates);
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.delegates);
        }
    }
}
