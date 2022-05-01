package net.minecraft.util.profiling;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.LongSupplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public class SingleTickProfiler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final LongSupplier realTime;
    private final long saveThreshold;
    private int tick;
    private final File location;
    private ProfileCollector profiler = InactiveProfiler.INSTANCE;

    public SingleTickProfiler(LongSupplier timeGetter, String filename, long overtime) {
        this.realTime = timeGetter;
        this.location = new File("debug", filename);
        this.saveThreshold = overtime;
    }

    public ProfilerFiller startTick() {
        this.profiler = new ActiveProfiler(this.realTime, () -> {
            return this.tick;
        }, false);
        ++this.tick;
        return this.profiler;
    }

    public void endTick() {
        if (this.profiler != InactiveProfiler.INSTANCE) {
            ProfileResults profileResults = this.profiler.getResults();
            this.profiler = InactiveProfiler.INSTANCE;
            if (profileResults.getNanoDuration() >= this.saveThreshold) {
                File file = new File(this.location, "tick-results-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt");
                profileResults.saveResults(file.toPath());
                LOGGER.info("Recorded long tick -- wrote info to: {}", (Object)file.getAbsolutePath());
            }

        }
    }

    @Nullable
    public static SingleTickProfiler createTickProfiler(String name) {
        return null;
    }

    public static ProfilerFiller decorateFiller(ProfilerFiller profiler, @Nullable SingleTickProfiler monitor) {
        return monitor != null ? ProfilerFiller.tee(monitor.startTick(), profiler) : profiler;
    }
}
