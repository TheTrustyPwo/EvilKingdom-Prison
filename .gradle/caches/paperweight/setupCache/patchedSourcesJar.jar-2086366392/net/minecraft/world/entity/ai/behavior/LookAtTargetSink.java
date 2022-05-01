package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class LookAtTargetSink extends Behavior<Mob> {
    public LookAtTargetSink(int minRunTime, int maxRunTime) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT), minRunTime, maxRunTime);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Mob mob, long l) {
        return mob.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).filter((lookTarget) -> {
            return lookTarget.isVisibleBy(mob);
        }).isPresent();
    }

    @Override
    protected void stop(ServerLevel serverLevel, Mob mob, long l) {
        mob.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(ServerLevel serverLevel, Mob mob, long l) {
        mob.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).ifPresent((lookTarget) -> {
            mob.getLookControl().setLookAt(lookTarget.currentPosition());
        });
    }
}
