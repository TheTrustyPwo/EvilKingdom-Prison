package net.minecraft.util.profiling.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;

public class MetricSampler {
    private final String name;
    private final MetricCategory category;
    private final DoubleSupplier sampler;
    private final ByteBuf ticks;
    private final ByteBuf values;
    private volatile boolean isRunning;
    @Nullable
    private final Runnable beforeTick;
    @Nullable
    final MetricSampler.ThresholdTest thresholdTest;
    private double currentValue;

    protected MetricSampler(String name, MetricCategory type, DoubleSupplier retriever, @Nullable Runnable startAction, @Nullable MetricSampler.ThresholdTest deviationChecker) {
        this.name = name;
        this.category = type;
        this.beforeTick = startAction;
        this.sampler = retriever;
        this.thresholdTest = deviationChecker;
        this.values = ByteBufAllocator.DEFAULT.buffer();
        this.ticks = ByteBufAllocator.DEFAULT.buffer();
        this.isRunning = true;
    }

    public static MetricSampler create(String name, MetricCategory type, DoubleSupplier retriever) {
        return new MetricSampler(name, type, retriever, (Runnable)null, (MetricSampler.ThresholdTest)null);
    }

    public static <T> MetricSampler create(String name, MetricCategory type, T context, ToDoubleFunction<T> retriever) {
        return builder(name, type, retriever, context).build();
    }

    public static <T> MetricSampler.MetricSamplerBuilder<T> builder(String name, MetricCategory type, ToDoubleFunction<T> retriever, T context) {
        return new MetricSampler.MetricSamplerBuilder<>(name, type, retriever, context);
    }

    public void onStartTick() {
        if (!this.isRunning) {
            throw new IllegalStateException("Not running");
        } else {
            if (this.beforeTick != null) {
                this.beforeTick.run();
            }

        }
    }

    public void onEndTick(int tick) {
        this.verifyRunning();
        this.currentValue = this.sampler.getAsDouble();
        this.values.writeDouble(this.currentValue);
        this.ticks.writeInt(tick);
    }

    public void onFinished() {
        this.verifyRunning();
        this.values.release();
        this.ticks.release();
        this.isRunning = false;
    }

    private void verifyRunning() {
        if (!this.isRunning) {
            throw new IllegalStateException(String.format("Sampler for metric %s not started!", this.name));
        }
    }

    DoubleSupplier getSampler() {
        return this.sampler;
    }

    public String getName() {
        return this.name;
    }

    public MetricCategory getCategory() {
        return this.category;
    }

    public MetricSampler.SamplerResult result() {
        Int2DoubleMap int2DoubleMap = new Int2DoubleOpenHashMap();
        int i = Integer.MIN_VALUE;

        int j;
        int k;
        for(j = Integer.MIN_VALUE; this.values.isReadable(8); j = k) {
            k = this.ticks.readInt();
            if (i == Integer.MIN_VALUE) {
                i = k;
            }

            int2DoubleMap.put(k, this.values.readDouble());
        }

        return new MetricSampler.SamplerResult(i, j, int2DoubleMap);
    }

    public boolean triggersThreshold() {
        return this.thresholdTest != null && this.thresholdTest.test(this.currentValue);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            MetricSampler metricSampler = (MetricSampler)object;
            return this.name.equals(metricSampler.name) && this.category.equals(metricSampler.category);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    public static class MetricSamplerBuilder<T> {
        private final String name;
        private final MetricCategory category;
        private final DoubleSupplier sampler;
        private final T context;
        @Nullable
        private Runnable beforeTick;
        @Nullable
        private MetricSampler.ThresholdTest thresholdTest;

        public MetricSamplerBuilder(String name, MetricCategory type, ToDoubleFunction<T> timeFunction, T context) {
            this.name = name;
            this.category = type;
            this.sampler = () -> {
                return timeFunction.applyAsDouble(context);
            };
            this.context = context;
        }

        public MetricSampler.MetricSamplerBuilder<T> withBeforeTick(Consumer<T> action) {
            this.beforeTick = () -> {
                action.accept(this.context);
            };
            return this;
        }

        public MetricSampler.MetricSamplerBuilder<T> withThresholdAlert(MetricSampler.ThresholdTest deviationChecker) {
            this.thresholdTest = deviationChecker;
            return this;
        }

        public MetricSampler build() {
            return new MetricSampler(this.name, this.category, this.sampler, this.beforeTick, this.thresholdTest);
        }
    }

    public static class SamplerResult {
        private final Int2DoubleMap recording;
        private final int firstTick;
        private final int lastTick;

        public SamplerResult(int startTick, int endTick, Int2DoubleMap values) {
            this.firstTick = startTick;
            this.lastTick = endTick;
            this.recording = values;
        }

        public double valueAtTick(int tick) {
            return this.recording.get(tick);
        }

        public int getFirstTick() {
            return this.firstTick;
        }

        public int getLastTick() {
            return this.lastTick;
        }
    }

    public interface ThresholdTest {
        boolean test(double value);
    }

    public static class ValueIncreasedByPercentage implements MetricSampler.ThresholdTest {
        private final float percentageIncreaseThreshold;
        private double previousValue = Double.MIN_VALUE;

        public ValueIncreasedByPercentage(float threshold) {
            this.percentageIncreaseThreshold = threshold;
        }

        @Override
        public boolean test(double value) {
            boolean bl2;
            if (this.previousValue != Double.MIN_VALUE && !(value <= this.previousValue)) {
                bl2 = (value - this.previousValue) / this.previousValue >= (double)this.percentageIncreaseThreshold;
            } else {
                bl2 = false;
            }

            this.previousValue = value;
            return bl2;
        }
    }
}
