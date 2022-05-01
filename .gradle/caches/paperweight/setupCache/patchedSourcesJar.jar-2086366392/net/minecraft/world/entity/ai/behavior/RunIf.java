package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class RunIf<E extends LivingEntity> extends Behavior<E> {
    private final Predicate<E> predicate;
    private final Behavior<? super E> wrappedBehavior;
    private final boolean checkWhileRunningAlso;

    public RunIf(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryStates, Predicate<E> condition, Behavior<? super E> delegate, boolean allowsContinuation) {
        super(mergeMaps(requiredMemoryStates, delegate.entryCondition));
        this.predicate = condition;
        this.wrappedBehavior = delegate;
        this.checkWhileRunningAlso = allowsContinuation;
    }

    private static Map<MemoryModuleType<?>, MemoryStatus> mergeMaps(Map<MemoryModuleType<?>, MemoryStatus> first, Map<MemoryModuleType<?>, MemoryStatus> second) {
        Map<MemoryModuleType<?>, MemoryStatus> map = Maps.newHashMap();
        map.putAll(first);
        map.putAll(second);
        return map;
    }

    public RunIf(Predicate<E> condition, Behavior<? super E> delegate, boolean allowsContinuation) {
        this(ImmutableMap.of(), condition, delegate, allowsContinuation);
    }

    public RunIf(Predicate<E> condition, Behavior<? super E> delegate) {
        this(ImmutableMap.of(), condition, delegate, false);
    }

    public RunIf(Map<MemoryModuleType<?>, MemoryStatus> memory, Behavior<? super E> delegate) {
        this(memory, (livingEntity) -> {
            return true;
        }, delegate, false);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        return this.predicate.test(entity) && this.wrappedBehavior.checkExtraStartConditions(world, entity);
    }

    @Override
    protected boolean canStillUse(ServerLevel world, E entity, long time) {
        return this.checkWhileRunningAlso && this.predicate.test(entity) && this.wrappedBehavior.canStillUse(world, entity, time);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        this.wrappedBehavior.start(world, entity, time);
    }

    @Override
    protected void tick(ServerLevel world, E entity, long time) {
        this.wrappedBehavior.tick(world, entity, time);
    }

    @Override
    protected void stop(ServerLevel world, E entity, long time) {
        this.wrappedBehavior.stop(world, entity, time);
    }

    @Override
    public String toString() {
        return "RunIf: " + this.wrappedBehavior;
    }
}
