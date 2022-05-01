package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class StartAttacking<E extends Mob> extends Behavior<E> {
    private final Predicate<E> canAttackPredicate;
    private final Function<E, Optional<? extends LivingEntity>> targetFinderFunction;

    public StartAttacking(Predicate<E> startCondition, Function<E, Optional<? extends LivingEntity>> targetGetter) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED));
        this.canAttackPredicate = startCondition;
        this.targetFinderFunction = targetGetter;
    }

    public StartAttacking(Function<E, Optional<? extends LivingEntity>> targetGetter) {
        this((mob) -> {
            return true;
        }, targetGetter);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        if (!this.canAttackPredicate.test(entity)) {
            return false;
        } else {
            Optional<? extends LivingEntity> optional = this.targetFinderFunction.apply(entity);
            return optional.isPresent() ? entity.canAttack(optional.get()) : false;
        }
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        this.targetFinderFunction.apply(entity).ifPresent((livingEntity) -> {
            this.setAttackTarget(entity, livingEntity);
        });
    }

    private void setAttackTarget(E entity, LivingEntity target) {
        entity.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        entity.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }
}
