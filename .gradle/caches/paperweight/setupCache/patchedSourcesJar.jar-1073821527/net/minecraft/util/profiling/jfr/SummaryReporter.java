package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.profiling.jfr.parse.JfrStatsParser;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class SummaryReporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Runnable onDeregistration;

    protected SummaryReporter(Runnable stopCallback) {
        this.onDeregistration = stopCallback;
    }

    public void recordingStopped(@Nullable Path dumpPath) {
        if (dumpPath != null) {
            this.onDeregistration.run();
            infoWithFallback(() -> {
                return "Dumped flight recorder profiling to " + dumpPath;
            });

            JfrStatsResult jfrStatsResult;
            try {
                jfrStatsResult = JfrStatsParser.parse(dumpPath);
            } catch (Throwable var5) {
                warnWithFallback(() -> {
                    return "Failed to parse JFR recording";
                }, var5);
                return;
            }

            try {
                infoWithFallback(jfrStatsResult::asJson);
                Path path = dumpPath.resolveSibling("jfr-report-" + StringUtils.substringBefore(dumpPath.getFileName().toString(), ".jfr") + ".json");
                Files.writeString(path, jfrStatsResult.asJson(), StandardOpenOption.CREATE);
                infoWithFallback(() -> {
                    return "Dumped recording summary to " + path;
                });
            } catch (Throwable var4) {
                warnWithFallback(() -> {
                    return "Failed to output JFR report";
                }, var4);
            }

        }
    }

    private static void infoWithFallback(Supplier<String> supplier) {
        if (LogUtils.isLoggerActive()) {
            LOGGER.info(supplier.get());
        } else {
            Bootstrap.realStdoutPrintln(supplier.get());
        }

    }

    private static void warnWithFallback(Supplier<String> supplier, Throwable throwable) {
        if (LogUtils.isLoggerActive()) {
            LOGGER.warn(supplier.get(), throwable);
        } else {
            Bootstrap.realStdoutPrintln(supplier.get());
            throwable.printStackTrace(Bootstrap.STDOUT);
        }

    }
}
