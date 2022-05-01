package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.GameRules;

public class StartCelebratingIfTargetDead extends Behavior<LivingEntity> {
    private final int celebrateDuration;
    private final BiPredicate<LivingEntity, LivingEntity> dancePredicate;

    public StartCelebratingIfTargetDead(int duration, BiPredicate<LivingEntity, LivingEntity> predicate) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED, MemoryModuleType.CELEBRATE_LOCATION, MemoryStatus.VALUE_ABSENT, MemoryModuleType.DANCING, MemoryStatus.REGISTERED));
        this.celebrateDuration = duration;
        this.dancePredicate = predicate;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        return this.getAttackTarget(entity).isDeadOrDying();
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        LivingEntity livingEntity = this.getAttackTarget(entity);
        if (this.dancePredicate.test(entity, livingEntity)) {
            entity.getBrain().setMemoryWithExpiry(MemoryModuleType.DANCING, true, (long)this.celebrateDuration);
        }

        entity.getBrain().setMemoryWithExpiry(MemoryModuleType.CELEBRATE_LOCATION, livingEntity.blockPosition(), (long)this.celebrateDuration);
        if (livingEntity.getType() != EntityType.PLAYER || world.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            entity.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        }

    }

    private LivingEntity getAttackTarget(LivingEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }
}
