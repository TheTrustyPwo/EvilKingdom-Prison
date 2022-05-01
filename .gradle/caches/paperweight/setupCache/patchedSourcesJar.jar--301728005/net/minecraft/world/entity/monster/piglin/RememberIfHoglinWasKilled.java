package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class RememberIfHoglinWasKilled<E extends Piglin> extends Behavior<E> {
    public RememberIfHoglinWasKilled() {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.HUNTED_RECENTLY, MemoryStatus.REGISTERED));
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        if (this.isAttackTargetDeadHoglin(entity)) {
            PiglinAi.dontKillAnyMoreHoglinsForAWhile(entity);
        }

    }

    private boolean isAttackTargetDeadHoglin(E piglin) {
        LivingEntity livingEntity = piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        return livingEntity.getType() == EntityType.HOGLIN && livingEntity.isDeadOrDying();
    }
}
