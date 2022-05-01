package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BecomePassiveIfMemoryPresent extends Behavior<LivingEntity> {
    private final int pacifyDuration;

    public BecomePassiveIfMemoryPresent(MemoryModuleType<?> requiredMemoryModuleType, int duration) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.PACIFIED, MemoryStatus.VALUE_ABSENT, requiredMemoryModuleType, MemoryStatus.VALUE_PRESENT));
        this.pacifyDuration = duration;
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        entity.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, (long)this.pacifyDuration);
        entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
