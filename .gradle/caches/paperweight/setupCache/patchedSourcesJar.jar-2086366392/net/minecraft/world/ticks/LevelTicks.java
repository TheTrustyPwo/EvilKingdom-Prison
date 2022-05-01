package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class LevelTicks<T> implements LevelTickAccess<T> {
    private static final Comparator<LevelChunkTicks<?>> CONTAINER_DRAIN_ORDER = (a, b) -> {
        return ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare(a.peek(), b.peek());
    };
    private final LongPredicate tickCheck;
    private final Supplier<ProfilerFiller> profiler;
    private final Long2ObjectMap<LevelChunkTicks<T>> allContainers = new Long2ObjectOpenHashMap<>();
    private final Long2LongMap nextTickForContainer = Util.make(new Long2LongOpenHashMap(), (map) -> {
        map.defaultReturnValue(Long.MAX_VALUE);
    });
    private final Queue<LevelChunkTicks<T>> containersToTick = new PriorityQueue<>(CONTAINER_DRAIN_ORDER);
    private final Queue<ScheduledTick<T>> toRunThisTick = new ArrayDeque<>();
    private final List<ScheduledTick<T>> alreadyRunThisTick = new ArrayList<>();
    private final Set<ScheduledTick<?>> toRunThisTickSet = new ObjectOpenCustomHashSet<>(ScheduledTick.UNIQUE_TICK_HASH);
    private final BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> chunkScheduleUpdater = (chunkTickScheduler, tick) -> {
        if (tick.equals(chunkTickScheduler.peek())) {
            this.updateContainerScheduling(tick);
        }

    };

    public LevelTicks(LongPredicate tickingFutureReadyPredicate, Supplier<ProfilerFiller> profilerGetter) {
        this.tickCheck = tickingFutureReadyPredicate;
        this.profiler = profilerGetter;
    }

    public void addContainer(ChunkPos pos, LevelChunkTicks<T> scheduler) {
        long l = pos.toLong();
        this.allContainers.put(l, scheduler);
        ScheduledTick<T> scheduledTick = scheduler.peek();
        if (scheduledTick != null) {
            this.nextTickForContainer.put(l, scheduledTick.triggerTick());
        }

        scheduler.setOnTickAdded(this.chunkScheduleUpdater);
    }

    public void removeContainer(ChunkPos pos) {
        long l = pos.toLong();
        LevelChunkTicks<T> levelChunkTicks = this.allContainers.remove(l);
        this.nextTickForContainer.remove(l);
        if (levelChunkTicks != null) {
            levelChunkTicks.setOnTickAdded((BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>>)null);
        }

    }

    @Override
    public void schedule(ScheduledTick<T> orderedTick) {
        long l = ChunkPos.asLong(orderedTick.pos());
        LevelChunkTicks<T> levelChunkTicks = this.allContainers.get(l);
        if (levelChunkTicks == null) {
            Util.pauseInIde(new IllegalStateException("Trying to schedule tick in not loaded position " + orderedTick.pos()));
        } else {
            levelChunkTicks.schedule(orderedTick);
        }
    }

    public void tick(long time, int maxTicks, BiConsumer<BlockPos, T> ticker) {
        ProfilerFiller profilerFiller = this.profiler.get();
        profilerFiller.push("collect");
        this.collectTicks(time, maxTicks, profilerFiller);
        profilerFiller.popPush("run");
        profilerFiller.incrementCounter("ticksToRun", this.toRunThisTick.size());
        this.runCollectedTicks(ticker);
        profilerFiller.popPush("cleanup");
        this.cleanupAfterTick();
        profilerFiller.pop();
    }

    private void collectTicks(long time, int maxTicks, ProfilerFiller profiler) {
        this.sortContainersToTick(time);
        profiler.incrementCounter("containersToTick", this.containersToTick.size());
        this.drainContainers(time, maxTicks);
        this.rescheduleLeftoverContainers();
    }

    private void sortContainersToTick(long time) {
        ObjectIterator<Entry> objectIterator = Long2LongMaps.fastIterator(this.nextTickForContainer);

        while(objectIterator.hasNext()) {
            Entry entry = objectIterator.next();
            long l = entry.getLongKey();
            long m = entry.getLongValue();
            if (m <= time) {
                LevelChunkTicks<T> levelChunkTicks = this.allContainers.get(l);
                if (levelChunkTicks == null) {
                    objectIterator.remove();
                } else {
                    ScheduledTick<T> scheduledTick = levelChunkTicks.peek();
                    if (scheduledTick == null) {
                        objectIterator.remove();
                    } else if (scheduledTick.triggerTick() > time) {
                        entry.setValue(scheduledTick.triggerTick());
                    } else if (this.tickCheck.test(l)) {
                        objectIterator.remove();
                        this.containersToTick.add(levelChunkTicks);
                    }
                }
            }
        }

    }

    private void drainContainers(long time, int maxTicks) {
        LevelChunkTicks<T> levelChunkTicks;
        while(this.canScheduleMoreTicks(maxTicks) && (levelChunkTicks = this.containersToTick.poll()) != null) {
            ScheduledTick<T> scheduledTick = levelChunkTicks.poll();
            this.scheduleForThisTick(scheduledTick);
            this.drainFromCurrentContainer(this.containersToTick, levelChunkTicks, time, maxTicks);
            ScheduledTick<T> scheduledTick2 = levelChunkTicks.peek();
            if (scheduledTick2 != null) {
                if (scheduledTick2.triggerTick() <= time && this.canScheduleMoreTicks(maxTicks)) {
                    this.containersToTick.add(levelChunkTicks);
                } else {
                    this.updateContainerScheduling(scheduledTick2);
                }
            }
        }

    }

    private void rescheduleLeftoverContainers() {
        for(LevelChunkTicks<T> levelChunkTicks : this.containersToTick) {
            this.updateContainerScheduling(levelChunkTicks.peek());
        }

    }

    private void updateContainerScheduling(ScheduledTick<T> tick) {
        this.nextTickForContainer.put(ChunkPos.asLong(tick.pos()), tick.triggerTick());
    }

    private void drainFromCurrentContainer(Queue<LevelChunkTicks<T>> tickableChunkTickSchedulers, LevelChunkTicks<T> chunkTickScheduler, long tick, int maxTicks) {
        if (this.canScheduleMoreTicks(maxTicks)) {
            LevelChunkTicks<T> levelChunkTicks = tickableChunkTickSchedulers.peek();
            ScheduledTick<T> scheduledTick = levelChunkTicks != null ? levelChunkTicks.peek() : null;

            while(this.canScheduleMoreTicks(maxTicks)) {
                ScheduledTick<T> scheduledTick2 = chunkTickScheduler.peek();
                if (scheduledTick2 == null || scheduledTick2.triggerTick() > tick || scheduledTick != null && ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare(scheduledTick2, scheduledTick) > 0) {
                    break;
                }

                chunkTickScheduler.poll();
                this.scheduleForThisTick(scheduledTick2);
            }

        }
    }

    private void scheduleForThisTick(ScheduledTick<T> tick) {
        this.toRunThisTick.add(tick);
    }

    private boolean canScheduleMoreTicks(int maxTicks) {
        return this.toRunThisTick.size() < maxTicks;
    }

    private void runCollectedTicks(BiConsumer<BlockPos, T> ticker) {
        while(!this.toRunThisTick.isEmpty()) {
            ScheduledTick<T> scheduledTick = this.toRunThisTick.poll();
            if (!this.toRunThisTickSet.isEmpty()) {
                this.toRunThisTickSet.remove(scheduledTick);
            }

            this.alreadyRunThisTick.add(scheduledTick);
            ticker.accept(scheduledTick.pos(), scheduledTick.type());
        }

    }

    private void cleanupAfterTick() {
        this.toRunThisTick.clear();
        this.containersToTick.clear();
        this.alreadyRunThisTick.clear();
        this.toRunThisTickSet.clear();
    }

    @Override
    public boolean hasScheduledTick(BlockPos pos, T type) {
        LevelChunkTicks<T> levelChunkTicks = this.allContainers.get(ChunkPos.asLong(pos));
        return levelChunkTicks != null && levelChunkTicks.hasScheduledTick(pos, type);
    }

    @Override
    public boolean willTickThisTick(BlockPos pos, T type) {
        this.calculateTickSetIfNeeded();
        return this.toRunThisTickSet.contains(ScheduledTick.probe(type, pos));
    }

    private void calculateTickSetIfNeeded() {
        if (this.toRunThisTickSet.isEmpty() && !this.toRunThisTick.isEmpty()) {
            this.toRunThisTickSet.addAll(this.toRunThisTick);
        }

    }

    private void forContainersInArea(BoundingBox box, LevelTicks.PosAndContainerConsumer<T> visitor) {
        int i = SectionPos.posToSectionCoord((double)box.minX());
        int j = SectionPos.posToSectionCoord((double)box.minZ());
        int k = SectionPos.posToSectionCoord((double)box.maxX());
        int l = SectionPos.posToSectionCoord((double)box.maxZ());

        for(int m = i; m <= k; ++m) {
            for(int n = j; n <= l; ++n) {
                long o = ChunkPos.asLong(m, n);
                LevelChunkTicks<T> levelChunkTicks = this.allContainers.get(o);
                if (levelChunkTicks != null) {
                    visitor.accept(o, levelChunkTicks);
                }
            }
        }

    }

    public void clearArea(BoundingBox box) {
        Predicate<ScheduledTick<T>> predicate = (tick) -> {
            return box.isInside(tick.pos());
        };
        this.forContainersInArea(box, (chunkPos, chunkTickScheduler) -> {
            ScheduledTick<T> scheduledTick = chunkTickScheduler.peek();
            chunkTickScheduler.removeIf(predicate);
            ScheduledTick<T> scheduledTick2 = chunkTickScheduler.peek();
            if (scheduledTick2 != scheduledTick) {
                if (scheduledTick2 != null) {
                    this.updateContainerScheduling(scheduledTick2);
                } else {
                    this.nextTickForContainer.remove(chunkPos);
                }
            }

        });
        this.alreadyRunThisTick.removeIf(predicate);
        this.toRunThisTick.removeIf(predicate);
    }

    public void copyArea(BoundingBox box, Vec3i offset) {
        List<ScheduledTick<T>> list = new ArrayList<>();
        Predicate<ScheduledTick<T>> predicate = (tick) -> {
            return box.isInside(tick.pos());
        };
        this.alreadyRunThisTick.stream().filter(predicate).forEach(list::add);
        this.toRunThisTick.stream().filter(predicate).forEach(list::add);
        this.forContainersInArea(box, (chunkPos, chunkTickScheduler) -> {
            chunkTickScheduler.getAll().filter(predicate).forEach(list::add);
        });
        LongSummaryStatistics longSummaryStatistics = list.stream().mapToLong(ScheduledTick::subTickOrder).summaryStatistics();
        long l = longSummaryStatistics.getMin();
        long m = longSummaryStatistics.getMax();
        list.forEach((tick) -> {
            this.schedule(new ScheduledTick<>(tick.type(), tick.pos().offset(offset), tick.triggerTick(), tick.priority(), tick.subTickOrder() - l + m + 1L));
        });
    }

    @Override
    public int count() {
        return this.allContainers.values().stream().mapToInt(TickAccess::count).sum();
    }

    @FunctionalInterface
    interface PosAndContainerConsumer<T> {
        void accept(long chunkPos, LevelChunkTicks<T> chunkTickScheduler);
    }
}
