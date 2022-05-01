package net.minecraft.util.profiling.metrics.storage;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class MetricsPersister {
    public static final Path PROFILING_RESULTS_DIR = Paths.get("debug/profiling");
    public static final String METRICS_DIR_NAME = "metrics";
    public static final String DEVIATIONS_DIR_NAME = "deviations";
    public static final String PROFILING_RESULT_FILENAME = "profiling.txt";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String rootFolderName;

    public MetricsPersister(String type) {
        this.rootFolderName = type;
    }

    public Path saveReports(Set<MetricSampler> samplers, Map<MetricSampler, List<RecordedDeviation>> deviations, ProfileResults result) {
        try {
            Files.createDirectories(PROFILING_RESULTS_DIR);
        } catch (IOException var8) {
            throw new UncheckedIOException(var8);
        }

        try {
            Path path = Files.createTempDirectory("minecraft-profiling");
            path.toFile().deleteOnExit();
            Files.createDirectories(PROFILING_RESULTS_DIR);
            Path path2 = path.resolve(this.rootFolderName);
            Path path3 = path2.resolve("metrics");
            this.saveMetrics(samplers, path3);
            if (!deviations.isEmpty()) {
                this.saveDeviations(deviations, path2.resolve("deviations"));
            }

            this.saveProfilingTaskExecutionResult(result, path2);
            return path;
        } catch (IOException var7) {
            throw new UncheckedIOException(var7);
        }
    }

    private void saveMetrics(Set<MetricSampler> samplers, Path directory) {
        if (samplers.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one sampler to persist");
        } else {
            Map<MetricCategory, List<MetricSampler>> map = samplers.stream().collect(Collectors.groupingBy(MetricSampler::getCategory));
            map.forEach((type, sampler) -> {
                this.saveCategory(type, sampler, directory);
            });
        }
    }

    private void saveCategory(MetricCategory type, List<MetricSampler> samplers, Path directory) {
        Path path = directory.resolve(Util.sanitizeName(type.getDescription(), ResourceLocation::validPathChar) + ".csv");
        Writer writer = null;

        try {
            Files.createDirectories(path.getParent());
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            CsvOutput.Builder builder = CsvOutput.builder();
            builder.addColumn("@tick");

            for(MetricSampler metricSampler : samplers) {
                builder.addColumn(metricSampler.getName());
            }

            CsvOutput csvOutput = builder.build(writer);
            List<MetricSampler.SamplerResult> list = samplers.stream().map(MetricSampler::result).collect(Collectors.toList());
            int i = list.stream().mapToInt(MetricSampler.SamplerResult::getFirstTick).summaryStatistics().getMin();
            int j = list.stream().mapToInt(MetricSampler.SamplerResult::getLastTick).summaryStatistics().getMax();

            for(int k = i; k <= j; ++k) {
                int l = k;
                Stream<String> stream = list.stream().map((data) -> {
                    return String.valueOf(data.valueAtTick(l));
                });
                Object[] objects = Stream.concat(Stream.of(String.valueOf(k)), stream).toArray((ix) -> {
                    return new String[ix];
                });
                csvOutput.writeRow(objects);
            }

            LOGGER.info("Flushed metrics to {}", (Object)path);
        } catch (Exception var18) {
            LOGGER.error("Could not save profiler results to {}", path, var18);
        } finally {
            IOUtils.closeQuietly(writer);
        }

    }

    private void saveDeviations(Map<MetricSampler, List<RecordedDeviation>> deviations, Path deviationsDirectory) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS", Locale.UK).withZone(ZoneId.systemDefault());
        deviations.forEach((sampler, sampleDeviations) -> {
            sampleDeviations.forEach((deviation) -> {
                String string = dateTimeFormatter.format(deviation.timestamp);
                Path path2 = deviationsDirectory.resolve(Util.sanitizeName(sampler.getName(), ResourceLocation::validPathChar)).resolve(String.format(Locale.ROOT, "%d@%s.txt", deviation.tick, string));
                deviation.profilerResultAtTick.saveResults(path2);
            });
        });
    }

    private void saveProfilingTaskExecutionResult(ProfileResults result, Path directory) {
        result.saveResults(directory.resolve("profiling.txt"));
    }
}
