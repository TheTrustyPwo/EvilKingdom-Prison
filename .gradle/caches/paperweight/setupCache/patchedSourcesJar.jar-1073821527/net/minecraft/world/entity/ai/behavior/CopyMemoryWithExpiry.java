package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class CopyMemoryWithExpiry<E extends Mob, T> extends Behavior<E> {
    private final Predicate<E> predicate;
    private final MemoryModuleType<? extends T> sourceMemory;
    private final MemoryModuleType<T> targetMemory;
    private final UniformInt durationOfCopy;

    public CopyMemoryWithExpiry(Predicate<E> runPredicate, MemoryModuleType<? extends T> sourceType, MemoryModuleType<T> targetType, UniformInt duration) {
        super(ImmutableMap.of(sourceType, MemoryStatus.VALUE_PRESENT, targetType, MemoryStatus.VALUE_ABSENT));
        this.predicate = runPredicate;
        this.sourceMemory = sourceType;
        this.targetMemory = targetType;
        this.durationOfCopy = duration;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        return this.predicate.test(entity);
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        Brain<?> brain = entity.getBrain();
        brain.setMemoryWithExpiry(this.targetMemory, brain.getMemory(this.sourceMemory).get(), (long)this.durationOfCopy.sample(world.random));
    }
}
