package net.minecraft.world.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class Brain<E extends LivingEntity> {
    static final Logger LOGGER = LogUtils.getLogger();
    private final Supplier<Codec<Brain<E>>> codec;
    private static final int SCHEDULE_UPDATE_DELAY = 20;
    private final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = Maps.newHashMap();
    private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.newLinkedHashMap();
    private final Map<Integer, Map<Activity, Set<Behavior<? super E>>>> availableBehaviorsByPriority = Maps.newTreeMap();
    private Schedule schedule = Schedule.EMPTY;
    private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements = Maps.newHashMap();
    private final Map<Activity, Set<MemoryModuleType<?>>> activityMemoriesToEraseWhenStopped = Maps.newHashMap();
    private Set<Activity> coreActivities = Sets.newHashSet();
    private final Set<Activity> activeActivities = Sets.newHashSet();
    private Activity defaultActivity = Activity.IDLE;
    private long lastScheduleUpdate = -9999L;

    public static <E extends LivingEntity> Brain.Provider<E> provider(Collection<? extends MemoryModuleType<?>> memoryModules, Collection<? extends SensorType<? extends Sensor<? super E>>> sensors) {
        return new Brain.Provider<>(memoryModules, sensors);
    }

    public static <E extends LivingEntity> Codec<Brain<E>> codec(Collection<? extends MemoryModuleType<?>> memoryModules, Collection<? extends SensorType<? extends Sensor<? super E>>> sensors) {
        final MutableObject<Codec<Brain<E>>> mutableObject = new MutableObject<>();
        mutableObject.setValue((new MapCodec<Brain<E>>() {
            public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
                return memoryModules.stream().flatMap((memoryModuleType) -> {
                    return memoryModuleType.getCodec().map((codec) -> {
                        return Registry.MEMORY_MODULE_TYPE.getKey(memoryModuleType);
                    }).stream();
                }).map((resourceLocation) -> {
                    return dynamicOps.createString(resourceLocation.toString());
                });
            }

            public <T> DataResult<Brain<E>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
                MutableObject<DataResult<Builder<Brain.MemoryValue<?>>>> mutableObject = new MutableObject<>(DataResult.success(ImmutableList.builder()));
                mapLike.entries().forEach((pair) -> {
                    DataResult<MemoryModuleType<?>> dataResult = Registry.MEMORY_MODULE_TYPE.byNameCodec().parse(dynamicOps, pair.getFirst());
                    DataResult<? extends Brain.MemoryValue<?>> dataResult2 = dataResult.flatMap((memoryModuleType) -> {
                        return this.captureRead(memoryModuleType, dynamicOps, (T)pair.getSecond());
                    });
                    mutableObject.setValue(mutableObject.getValue().apply2(Builder::add, dataResult2));
                });
                ImmutableList<Brain.MemoryValue<?>> immutableList = mutableObject.getValue().resultOrPartial(Brain.LOGGER::error).map(Builder::build).orElseGet(ImmutableList::of);
                return DataResult.success(new Brain<>(memoryModules, sensors, immutableList, mutableObject::getValue));
            }

            private <T, U> DataResult<Brain.MemoryValue<U>> captureRead(MemoryModuleType<U> memoryModuleType, DynamicOps<T> dynamicOps, T object) {
                return memoryModuleType.getCodec().map(DataResult::success).orElseGet(() -> {
                    return DataResult.error("No codec for memory: " + memoryModuleType);
                }).flatMap((codec) -> {
                    return codec.parse(dynamicOps, object);
                }).map((expirableValue) -> {
                    return new Brain.MemoryValue<>(memoryModuleType, Optional.of(expirableValue));
                });
            }

            public <T> RecordBuilder<T> encode(Brain<E> brain, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
                brain.memories().forEach((memoryValue) -> {
                    memoryValue.serialize(dynamicOps, recordBuilder);
                });
                return recordBuilder;
            }
        }).fieldOf("memories").codec());
        return mutableObject.getValue();
    }

    public Brain(Collection<? extends MemoryModuleType<?>> memories, Collection<? extends SensorType<? extends Sensor<? super E>>> sensors, ImmutableList<Brain.MemoryValue<?>> memoryEntries, Supplier<Codec<Brain<E>>> codecSupplier) {
        this.codec = codecSupplier;

        for(MemoryModuleType<?> memoryModuleType : memories) {
            this.memories.put(memoryModuleType, Optional.empty());
        }

        for(SensorType<? extends Sensor<? super E>> sensorType : sensors) {
            this.sensors.put(sensorType, sensorType.create());
        }

        for(Sensor<? super E> sensor : this.sensors.values()) {
            for(MemoryModuleType<?> memoryModuleType2 : sensor.requires()) {
                this.memories.put(memoryModuleType2, Optional.empty());
            }
        }

        for(Brain.MemoryValue<?> memoryValue : memoryEntries) {
            memoryValue.setMemoryInternal(this);
        }

    }

    public <T> DataResult<T> serializeStart(DynamicOps<T> ops) {
        return this.codec.get().encodeStart(ops, this);
    }

    Stream<Brain.MemoryValue<?>> memories() {
        return this.memories.entrySet().stream().map((entry) -> {
            return Brain.MemoryValue.createUnchecked(entry.getKey(), entry.getValue());
        });
    }

    public boolean hasMemoryValue(MemoryModuleType<?> type) {
        return this.checkMemory(type, MemoryStatus.VALUE_PRESENT);
    }

    public <U> void eraseMemory(MemoryModuleType<U> type) {
        this.setMemory(type, Optional.empty());
    }

    public <U> void setMemory(MemoryModuleType<U> type, @Nullable U value) {
        this.setMemory(type, Optional.ofNullable(value));
    }

    public <U> void setMemoryWithExpiry(MemoryModuleType<U> type, U value, long startTime) {
        this.setMemoryInternal(type, Optional.of(ExpirableValue.of(value, startTime)));
    }

    public <U> void setMemory(MemoryModuleType<U> type, Optional<? extends U> value) {
        this.setMemoryInternal(type, value.map(ExpirableValue::of));
    }

    <U> void setMemoryInternal(MemoryModuleType<U> type, Optional<? extends ExpirableValue<?>> memory) {
        if (this.memories.containsKey(type)) {
            if (memory.isPresent() && this.isEmptyCollection(memory.get().getValue())) {
                this.eraseMemory(type);
            } else {
                this.memories.put(type, memory);
            }
        }

    }

    public <U> Optional<U> getMemory(MemoryModuleType<U> type) {
        return this.memories.get(type).map(ExpirableValue::getValue);
    }

    public <U> long getTimeUntilExpiry(MemoryModuleType<U> type) {
        Optional<? extends ExpirableValue<?>> optional = this.memories.get(type);
        return optional.map(ExpirableValue::getTimeToLive).orElse(0L);
    }

    /** @deprecated */
    @Deprecated
    @VisibleForDebug
    public Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> getMemories() {
        return this.memories;
    }

    public <U> boolean isMemoryValue(MemoryModuleType<U> type, U value) {
        return !this.hasMemoryValue(type) ? false : this.getMemory(type).filter((object2) -> {
            return object2.equals(value);
        }).isPresent();
    }

    public boolean checkMemory(MemoryModuleType<?> type, MemoryStatus state) {
        Optional<? extends ExpirableValue<?>> optional = this.memories.get(type);
        if (optional == null) {
            return false;
        } else {
            return state == MemoryStatus.REGISTERED || state == MemoryStatus.VALUE_PRESENT && optional.isPresent() || state == MemoryStatus.VALUE_ABSENT && !optional.isPresent();
        }
    }

    public Schedule getSchedule() {
        return this.schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public void setCoreActivities(Set<Activity> coreActivities) {
        this.coreActivities = coreActivities;
    }

    /** @deprecated */
    @Deprecated
    @VisibleForDebug
    public Set<Activity> getActiveActivities() {
        return this.activeActivities;
    }

    /** @deprecated */
    @Deprecated
    @VisibleForDebug
    public List<Behavior<? super E>> getRunningBehaviors() {
        List<Behavior<? super E>> list = new ObjectArrayList<>();

        for(Map<Activity, Set<Behavior<? super E>>> map : this.availableBehaviorsByPriority.values()) {
            for(Set<Behavior<? super E>> set : map.values()) {
                for(Behavior<? super E> behavior : set) {
                    if (behavior.getStatus() == Behavior.Status.RUNNING) {
                        list.add(behavior);
                    }
                }
            }
        }

        return list;
    }

    public void useDefaultActivity() {
        this.setActiveActivity(this.defaultActivity);
    }

    public Optional<Activity> getActiveNonCoreActivity() {
        for(Activity activity : this.activeActivities) {
            if (!this.coreActivities.contains(activity)) {
                return Optional.of(activity);
            }
        }

        return Optional.empty();
    }

    public void setActiveActivityIfPossible(Activity activity) {
        if (this.activityRequirementsAreMet(activity)) {
            this.setActiveActivity(activity);
        } else {
            this.useDefaultActivity();
        }

    }

    private void setActiveActivity(Activity except) {
        if (!this.isActive(except)) {
            this.eraseMemoriesForOtherActivitesThan(except);
            this.activeActivities.clear();
            this.activeActivities.addAll(this.coreActivities);
            this.activeActivities.add(except);
        }
    }

    private void eraseMemoriesForOtherActivitesThan(Activity except) {
        for(Activity activity : this.activeActivities) {
            if (activity != except) {
                Set<MemoryModuleType<?>> set = this.activityMemoriesToEraseWhenStopped.get(activity);
                if (set != null) {
                    for(MemoryModuleType<?> memoryModuleType : set) {
                        this.eraseMemory(memoryModuleType);
                    }
                }
            }
        }

    }

    public void updateActivityFromSchedule(long timeOfDay, long time) {
        if (time - this.lastScheduleUpdate > 20L) {
            this.lastScheduleUpdate = time;
            Activity activity = this.getSchedule().getActivityAt((int)(timeOfDay % 24000L));
            if (!this.activeActivities.contains(activity)) {
                this.setActiveActivityIfPossible(activity);
            }
        }

    }

    public void setActiveActivityToFirstValid(List<Activity> activities) {
        for(Activity activity : activities) {
            if (this.activityRequirementsAreMet(activity)) {
                this.setActiveActivity(activity);
                break;
            }
        }

    }

    public void setDefaultActivity(Activity activity) {
        this.defaultActivity = activity;
    }

    public void addActivity(Activity activity, int begin, ImmutableList<? extends Behavior<? super E>> list) {
        this.addActivity(activity, this.createPriorityPairs(begin, list));
    }

    public void addActivityAndRemoveMemoryWhenStopped(Activity activity, int begin, ImmutableList<? extends Behavior<? super E>> tasks, MemoryModuleType<?> memoryType) {
        Set<Pair<MemoryModuleType<?>, MemoryStatus>> set = ImmutableSet.of(Pair.of(memoryType, MemoryStatus.VALUE_PRESENT));
        Set<MemoryModuleType<?>> set2 = ImmutableSet.of(memoryType);
        this.addActivityAndRemoveMemoriesWhenStopped(activity, this.createPriorityPairs(begin, tasks), set, set2);
    }

    public void addActivity(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> indexedTasks) {
        this.addActivityAndRemoveMemoriesWhenStopped(activity, indexedTasks, ImmutableSet.of(), Sets.newHashSet());
    }

    public void addActivityWithConditions(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> indexedTasks, Set<Pair<MemoryModuleType<?>, MemoryStatus>> requiredMemories) {
        this.addActivityAndRemoveMemoriesWhenStopped(activity, indexedTasks, requiredMemories, Sets.newHashSet());
    }

    public void addActivityAndRemoveMemoriesWhenStopped(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> indexedTasks, Set<Pair<MemoryModuleType<?>, MemoryStatus>> requiredMemories, Set<MemoryModuleType<?>> forgettingMemories) {
        this.activityRequirements.put(activity, requiredMemories);
        if (!forgettingMemories.isEmpty()) {
            this.activityMemoriesToEraseWhenStopped.put(activity, forgettingMemories);
        }

        for(Pair<Integer, ? extends Behavior<? super E>> pair : indexedTasks) {
            this.availableBehaviorsByPriority.computeIfAbsent(pair.getFirst(), (integer) -> {
                return Maps.newHashMap();
            }).computeIfAbsent(activity, (activityx) -> {
                return Sets.newLinkedHashSet();
            }).add(pair.getSecond());
        }

    }

    @VisibleForTesting
    public void removeAllBehaviors() {
        this.availableBehaviorsByPriority.clear();
    }

    public boolean isActive(Activity activity) {
        return this.activeActivities.contains(activity);
    }

    public Brain<E> copyWithoutBehaviors() {
        Brain<E> brain = new Brain<>(this.memories.keySet(), this.sensors.keySet(), ImmutableList.of(), this.codec);

        for(Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : this.memories.entrySet()) {
            MemoryModuleType<?> memoryModuleType = entry.getKey();
            if (entry.getValue().isPresent()) {
                brain.memories.put(memoryModuleType, entry.getValue());
            }
        }

        return brain;
    }

    public void tick(ServerLevel world, E entity) {
        this.forgetOutdatedMemories();
        this.tickSensors(world, entity);
        this.startEachNonRunningBehavior(world, entity);
        this.tickEachRunningBehavior(world, entity);
    }

    private void tickSensors(ServerLevel world, E entity) {
        for(Sensor<? super E> sensor : this.sensors.values()) {
            sensor.tick(world, entity);
        }

    }

    private void forgetOutdatedMemories() {
        for(Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : this.memories.entrySet()) {
            if (entry.getValue().isPresent()) {
                ExpirableValue<?> expirableValue = entry.getValue().get();
                expirableValue.tick();
                if (expirableValue.hasExpired()) {
                    this.eraseMemory(entry.getKey());
                }
            }
        }

    }

    public void stopAll(ServerLevel world, E entity) {
        long l = entity.level.getGameTime();

        for(Behavior<? super E> behavior : this.getRunningBehaviors()) {
            behavior.doStop(world, entity, l);
        }

    }

    private void startEachNonRunningBehavior(ServerLevel world, E entity) {
        long l = world.getGameTime();

        for(Map<Activity, Set<Behavior<? super E>>> map : this.availableBehaviorsByPriority.values()) {
            for(Entry<Activity, Set<Behavior<? super E>>> entry : map.entrySet()) {
                Activity activity = entry.getKey();
                if (this.activeActivities.contains(activity)) {
                    for(Behavior<? super E> behavior : entry.getValue()) {
                        if (behavior.getStatus() == Behavior.Status.STOPPED) {
                            behavior.tryStart(world, entity, l);
                        }
                    }
                }
            }
        }

    }

    private void tickEachRunningBehavior(ServerLevel world, E entity) {
        long l = world.getGameTime();

        for(Behavior<? super E> behavior : this.getRunningBehaviors()) {
            behavior.tickOrStop(world, entity, l);
        }

    }

    private boolean activityRequirementsAreMet(Activity activity) {
        if (!this.activityRequirements.containsKey(activity)) {
            return false;
        } else {
            for(Pair<MemoryModuleType<?>, MemoryStatus> pair : this.activityRequirements.get(activity)) {
                MemoryModuleType<?> memoryModuleType = pair.getFirst();
                MemoryStatus memoryStatus = pair.getSecond();
                if (!this.checkMemory(memoryModuleType, memoryStatus)) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean isEmptyCollection(Object value) {
        return value instanceof Collection && ((Collection)value).isEmpty();
    }

    ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> createPriorityPairs(int begin, ImmutableList<? extends Behavior<? super E>> tasks) {
        int i = begin;
        Builder<Pair<Integer, ? extends Behavior<? super E>>> builder = ImmutableList.builder();

        for(Behavior<? super E> behavior : tasks) {
            builder.add(Pair.of(i++, behavior));
        }

        return builder.build();
    }

    static final class MemoryValue<U> {
        private final MemoryModuleType<U> type;
        private final Optional<? extends ExpirableValue<U>> value;

        static <U> Brain.MemoryValue<U> createUnchecked(MemoryModuleType<U> type, Optional<? extends ExpirableValue<?>> data) {
            return new Brain.MemoryValue<>(type, data);
        }

        MemoryValue(MemoryModuleType<U> type, Optional<? extends ExpirableValue<U>> data) {
            this.type = type;
            this.value = data;
        }

        void setMemoryInternal(Brain<?> brain) {
            brain.setMemoryInternal(this.type, this.value);
        }

        public <T> void serialize(DynamicOps<T> ops, RecordBuilder<T> builder) {
            this.type.getCodec().ifPresent((codec) -> {
                this.value.ifPresent((expirableValue) -> {
                    builder.add(Registry.MEMORY_MODULE_TYPE.byNameCodec().encodeStart(ops, this.type), codec.encodeStart(ops, expirableValue));
                });
            });
        }
    }

    public static final class Provider<E extends LivingEntity> {
        private final Collection<? extends MemoryModuleType<?>> memoryTypes;
        private final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes;
        private final Codec<Brain<E>> codec;

        Provider(Collection<? extends MemoryModuleType<?>> memoryModules, Collection<? extends SensorType<? extends Sensor<? super E>>> sensors) {
            this.memoryTypes = memoryModules;
            this.sensorTypes = sensors;
            this.codec = Brain.codec(memoryModules, sensors);
        }

        public Brain<E> makeBrain(Dynamic<?> data) {
            return this.codec.parse(data).resultOrPartial(Brain.LOGGER::error).orElseGet(() -> {
                return new Brain<>(this.memoryTypes, this.sensorTypes, ImmutableList.of(), () -> {
                    return this.codec;
                });
            });
        }
    }
}
