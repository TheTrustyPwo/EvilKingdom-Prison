package net.minecraft.util.profiling;

import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.MetricCategory;

public interface ProfilerFiller {
    String ROOT = "root";

    void startTick();

    void endTick();

    void push(String location);

    void push(Supplier<String> locationGetter);

    void pop();

    void popPush(String location);

    void popPush(Supplier<String> locationGetter);

    void markForCharting(MetricCategory type);

    default void incrementCounter(String marker) {
        this.incrementCounter(marker, 1);
    }

    void incrementCounter(String marker, int i);

    default void incrementCounter(Supplier<String> markerGetter) {
        this.incrementCounter(markerGetter, 1);
    }

    void incrementCounter(Supplier<String> markerGetter, int i);

    static ProfilerFiller tee(ProfilerFiller a, ProfilerFiller b) {
        if (a == InactiveProfiler.INSTANCE) {
            return b;
        } else {
            return b == InactiveProfiler.INSTANCE ? a : new ProfilerFiller() {
                @Override
                public void startTick() {
                    a.startTick();
                    b.startTick();
                }

                @Override
                public void endTick() {
                    a.endTick();
                    b.endTick();
                }

                @Override
                public void push(String location) {
                    a.push(location);
                    b.push(location);
                }

                @Override
                public void push(Supplier<String> locationGetter) {
                    a.push(locationGetter);
                    b.push(locationGetter);
                }

                @Override
                public void markForCharting(MetricCategory type) {
                    a.markForCharting(type);
                    b.markForCharting(type);
                }

                @Override
                public void pop() {
                    a.pop();
                    b.pop();
                }

                @Override
                public void popPush(String location) {
                    a.popPush(location);
                    b.popPush(location);
                }

                @Override
                public void popPush(Supplier<String> locationGetter) {
                    a.popPush(locationGetter);
                    b.popPush(locationGetter);
                }

                @Override
                public void incrementCounter(String marker, int i) {
                    a.incrementCounter(marker, i);
                    b.incrementCounter(marker, i);
                }

                @Override
                public void incrementCounter(Supplier<String> markerGetter, int i) {
                    a.incrementCounter(markerGetter, i);
                    b.incrementCounter(markerGetter, i);
                }
            };
        }
    }
}
