package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class CountDownCooldownTicks extends Behavior<LivingEntity> {
    private final MemoryModuleType<Integer> cooldownTicks;

    public CountDownCooldownTicks(MemoryModuleType<Integer> moduleType) {
        super(ImmutableMap.of(moduleType, MemoryStatus.VALUE_PRESENT));
        this.cooldownTicks = moduleType;
    }

    private Optional<Integer> getCooldownTickMemory(LivingEntity entity) {
        return entity.getBrain().getMemory(this.cooldownTicks);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel world, LivingEntity entity, long time) {
        Optional<Integer> optional = this.getCooldownTickMemory(entity);
        return optional.isPresent() && optional.get() > 0;
    }

    @Override
    protected void tick(ServerLevel world, LivingEntity entity, long time) {
        Optional<Integer> optional = this.getCooldownTickMemory(entity);
        entity.getBrain().setMemory(this.cooldownTicks, optional.get() - 1);
    }

    @Override
    protected void stop(ServerLevel world, LivingEntity entity, long time) {
        entity.getBrain().eraseMemory(this.cooldownTicks);
    }
}
