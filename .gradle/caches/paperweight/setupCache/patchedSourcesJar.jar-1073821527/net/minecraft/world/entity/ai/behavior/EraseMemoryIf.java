package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class EraseMemoryIf<E extends LivingEntity> extends Behavior<E> {
    private final Predicate<E> predicate;
    private final MemoryModuleType<?> memoryType;

    public EraseMemoryIf(Predicate<E> condition, MemoryModuleType<?> memory) {
        super(ImmutableMap.of(memory, MemoryStatus.VALUE_PRESENT));
        this.predicate = condition;
        this.memoryType = memory;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        return this.predicate.test(entity);
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        entity.getBrain().eraseMemory(this.memoryType);
    }
}
