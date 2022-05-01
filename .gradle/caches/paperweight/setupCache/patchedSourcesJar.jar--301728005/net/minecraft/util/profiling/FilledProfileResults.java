package net.minecraft.util.profiling;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

public class FilledProfileResults implements ProfileResults {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ProfilerPathEntry EMPTY = new ProfilerPathEntry() {
        @Override
        public long getDuration() {
            return 0L;
        }

        @Override
        public long getMaxDuration() {
            return 0L;
        }

        @Override
        public long getCount() {
            return 0L;
        }

        @Override
        public Object2LongMap<String> getCounters() {
            return Object2LongMaps.emptyMap();
        }
    };
    private static final Splitter SPLITTER = Splitter.on('\u001e');
    private static final Comparator<Entry<String, FilledProfileResults.CounterCollector>> COUNTER_ENTRY_COMPARATOR = Entry.<String, FilledProfileResults.CounterCollector>comparingByValue(Comparator.comparingLong((counterCollector) -> {
        return counterCollector.totalValue;
    })).reversed();
    private final Map<String, ? extends ProfilerPathEntry> entries;
    private final long startTimeNano;
    private final int startTimeTicks;
    private final long endTimeNano;
    private final int endTimeTicks;
    private final int tickDuration;

    public FilledProfileResults(Map<String, ? extends ProfilerPathEntry> locationInfos, long startTime, int startTick, long endTime, int endTick) {
        this.entries = locationInfos;
        this.startTimeNano = startTime;
        this.startTimeTicks = startTick;
        this.endTimeNano = endTime;
        this.endTimeTicks = endTick;
        this.tickDuration = endTick - startTick;
    }

    private ProfilerPathEntry getEntry(String path) {
        ProfilerPathEntry profilerPathEntry = this.entries.get(path);
        return profilerPathEntry != null ? profilerPathEntry : EMPTY;
    }

    @Override
    public List<ResultField> getTimes(String parentPath) {
        String string = parentPath;
        ProfilerPathEntry profilerPathEntry = this.getEntry("root");
        long l = profilerPathEntry.getDuration();
        ProfilerPathEntry profilerPathEntry2 = this.getEntry(parentPath);
        long m = profilerPathEntry2.getDuration();
        long n = profilerPathEntry2.getCount();
        List<ResultField> list = Lists.newArrayList();
        if (!parentPath.isEmpty()) {
            parentPath = parentPath + "\u001e";
        }

        long o = 0L;

        for(String string2 : this.entries.keySet()) {
            if (isDirectChild(parentPath, string2)) {
                o += this.getEntry(string2).getDuration();
            }
        }

        float f = (float)o;
        if (o < m) {
            o = m;
        }

        if (l < o) {
            l = o;
        }

        for(String string3 : this.entries.keySet()) {
            if (isDirectChild(parentPath, string3)) {
                ProfilerPathEntry profilerPathEntry3 = this.getEntry(string3);
                long p = profilerPathEntry3.getDuration();
                double d = (double)p * 100.0D / (double)o;
                double e = (double)p * 100.0D / (double)l;
                String string4 = string3.substring(parentPath.length());
                list.add(new ResultField(string4, d, e, profilerPathEntry3.getCount()));
            }
        }

        if ((float)o > f) {
            list.add(new ResultField("unspecified", (double)((float)o - f) * 100.0D / (double)o, (double)((float)o - f) * 100.0D / (double)l, n));
        }

        Collections.sort(list);
        list.add(0, new ResultField(string, 100.0D, (double)o * 100.0D / (double)l, n));
        return list;
    }

    private static boolean isDirectChild(String parent, String path) {
        return path.length() > parent.length() && path.startsWith(parent) && path.indexOf(30, parent.length() + 1) < 0;
    }

    private Map<String, FilledProfileResults.CounterCollector> getCounterValues() {
        Map<String, FilledProfileResults.CounterCollector> map = Maps.newTreeMap();
        this.entries.forEach((location, info) -> {
            Object2LongMap<String> object2LongMap = info.getCounters();
            if (!object2LongMap.isEmpty()) {
                List<String> list = SPLITTER.splitToList(location);
                object2LongMap.forEach((marker, count) -> {
                    map.computeIfAbsent(marker, (k) -> {
                        return new FilledProfileResults.CounterCollector();
                    }).addValue(list.iterator(), count);
                });
            }

        });
        return map;
    }

    @Override
    public long getStartTimeNano() {
        return this.startTimeNano;
    }

    @Override
    public int getStartTimeTicks() {
        return this.startTimeTicks;
    }

    @Override
    public long getEndTimeNano() {
        return this.endTimeNano;
    }

    @Override
    public int getEndTimeTicks() {
        return this.endTimeTicks;
    }

    @Override
    public boolean saveResults(Path path) {
        Writer writer = null;

        boolean var4;
        try {
            Files.createDirectories(path.getParent());
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            writer.write(this.getProfilerResults(this.getNanoDuration(), this.getTickDuration()));
            return true;
        } catch (Throwable var8) {
            LOGGER.error("Could not save profiler results to {}", path, var8);
            var4 = false;
        } finally {
            IOUtils.closeQuietly(writer);
        }

        return var4;
    }

    protected String getProfilerResults(long timeSpan, int tickSpan) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("---- Minecraft Profiler Results ----\n");
        stringBuilder.append("// ");
        stringBuilder.append(getComment());
        stringBuilder.append("\n\n");
        stringBuilder.append("Version: ").append(SharedConstants.getCurrentVersion().getId()).append('\n');
        stringBuilder.append("Time span: ").append(timeSpan / 1000000L).append(" ms\n");
        stringBuilder.append("Tick span: ").append(tickSpan).append(" ticks\n");
        stringBuilder.append("// This is approximately ").append(String.format(Locale.ROOT, "%.2f", (float)tickSpan / ((float)timeSpan / 1.0E9F))).append(" ticks per second. It should be ").append((int)20).append(" ticks per second\n\n");
        stringBuilder.append("--- BEGIN PROFILE DUMP ---\n\n");
        this.appendProfilerResults(0, "root", stringBuilder);
        stringBuilder.append("--- END PROFILE DUMP ---\n\n");
        Map<String, FilledProfileResults.CounterCollector> map = this.getCounterValues();
        if (!map.isEmpty()) {
            stringBuilder.append("--- BEGIN COUNTER DUMP ---\n\n");
            this.appendCounters(map, stringBuilder, tickSpan);
            stringBuilder.append("--- END COUNTER DUMP ---\n\n");
        }

        return stringBuilder.toString();
    }

    @Override
    public String getProfilerResults() {
        StringBuilder stringBuilder = new StringBuilder();
        this.appendProfilerResults(0, "root", stringBuilder);
        return stringBuilder.toString();
    }

    private static StringBuilder indentLine(StringBuilder sb, int size) {
        sb.append(String.format("[%02d] ", size));

        for(int i = 0; i < size; ++i) {
            sb.append("|   ");
        }

        return sb;
    }

    private void appendProfilerResults(int level, String name, StringBuilder sb) {
        List<ResultField> list = this.getTimes(name);
        Object2LongMap<String> object2LongMap = ObjectUtils.firstNonNull(this.entries.get(name), EMPTY).getCounters();
        object2LongMap.forEach((marker, count) -> {
            indentLine(sb, level).append('#').append(marker).append(' ').append((Object)count).append('/').append(count / (long)this.tickDuration).append('\n');
        });
        if (list.size() >= 3) {
            for(int i = 1; i < list.size(); ++i) {
                ResultField resultField = list.get(i);
                indentLine(sb, level).append(resultField.name).append('(').append(resultField.count).append('/').append(String.format(Locale.ROOT, "%.0f", (float)resultField.count / (float)this.tickDuration)).append(')').append(" - ").append(String.format(Locale.ROOT, "%.2f", resultField.percentage)).append("%/").append(String.format(Locale.ROOT, "%.2f", resultField.globalPercentage)).append("%\n");
                if (!"unspecified".equals(resultField.name)) {
                    try {
                        this.appendProfilerResults(level + 1, name + "\u001e" + resultField.name, sb);
                    } catch (Exception var9) {
                        sb.append("[[ EXCEPTION ").append((Object)var9).append(" ]]");
                    }
                }
            }

        }
    }

    private void appendCounterResults(int depth, String name, FilledProfileResults.CounterCollector info, int tickSpan, StringBuilder sb) {
        indentLine(sb, depth).append(name).append(" total:").append(info.selfValue).append('/').append(info.totalValue).append(" average: ").append(info.selfValue / (long)tickSpan).append('/').append(info.totalValue / (long)tickSpan).append('\n');
        info.children.entrySet().stream().sorted(COUNTER_ENTRY_COMPARATOR).forEach((entry) -> {
            this.appendCounterResults(depth + 1, entry.getKey(), entry.getValue(), tickSpan, sb);
        });
    }

    private void appendCounters(Map<String, FilledProfileResults.CounterCollector> counters, StringBuilder sb, int tickSpan) {
        counters.forEach((name, info) -> {
            sb.append("-- Counter: ").append(name).append(" --\n");
            this.appendCounterResults(0, "root", info.children.get("root"), tickSpan, sb);
            sb.append("\n\n");
        });
    }

    private static String getComment() {
        String[] strings = new String[]{"I'd Rather Be Surfing", "Shiny numbers!", "Am I not running fast enough? :(", "I'm working as hard as I can!", "Will I ever be good enough for you? :(", "Speedy. Zoooooom!", "Hello world", "40% better than a crash report.", "Now with extra numbers", "Now with less numbers", "Now with the same numbers", "You should add flames to things, it makes them go faster!", "Do you feel the need for... optimization?", "*cracks redstone whip*", "Maybe if you treated it better then it'll have more motivation to work faster! Poor server."};

        try {
            return strings[(int)(Util.getNanos() % (long)strings.length)];
        } catch (Throwable var2) {
            return "Witty comment unavailable :(";
        }
    }

    @Override
    public int getTickDuration() {
        return this.tickDuration;
    }

    static class CounterCollector {
        long selfValue;
        long totalValue;
        final Map<String, FilledProfileResults.CounterCollector> children = Maps.newHashMap();

        public void addValue(Iterator<String> pathIterator, long time) {
            this.totalValue += time;
            if (!pathIterator.hasNext()) {
                this.selfValue += time;
            } else {
                this.children.computeIfAbsent(pathIterator.next(), (k) -> {
                    return new FilledProfileResults.CounterCollector();
                }).addValue(pathIterator, time);
            }

        }
    }
}
