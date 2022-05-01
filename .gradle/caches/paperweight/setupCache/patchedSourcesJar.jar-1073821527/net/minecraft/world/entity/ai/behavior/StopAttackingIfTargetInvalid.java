package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class StopAttackingIfTargetInvalid<E extends Mob> extends Behavior<E> {
    private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;
    private final Predicate<LivingEntity> stopAttackingWhen;
    private final Consumer<E> onTargetErased;

    public StopAttackingIfTargetInvalid(Predicate<LivingEntity> condition, Consumer<E> forgetCallback) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED));
        this.stopAttackingWhen = condition;
        this.onTargetErased = forgetCallback;
    }

    public StopAttackingIfTargetInvalid(Predicate<LivingEntity> alternativeCondition) {
        this(alternativeCondition, (mob) -> {
        });
    }

    public StopAttackingIfTargetInvalid(Consumer<E> forgetCallback) {
        this((target) -> {
            return false;
        }, forgetCallback);
    }

    public StopAttackingIfTargetInvalid() {
        this((target) -> {
            return false;
        }, (mob) -> {
        });
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        LivingEntity livingEntity = this.getAttackTarget(entity);
        if (!entity.canAttack(livingEntity)) {
            this.clearAttackTarget(entity);
        } else if (isTiredOfTryingToReachTarget(entity)) {
            this.clearAttackTarget(entity);
        } else if (this.isCurrentTargetDeadOrRemoved(entity)) {
            this.clearAttackTarget(entity);
        } else if (this.isCurrentTargetInDifferentLevel(entity)) {
            this.clearAttackTarget(entity);
        } else if (this.stopAttackingWhen.test(this.getAttackTarget(entity))) {
            this.clearAttackTarget(entity);
        }
    }

    private boolean isCurrentTargetInDifferentLevel(E entity) {
        return this.getAttackTarget(entity).level != entity.level;
    }

    private LivingEntity getAttackTarget(E entity) {
        return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }

    private static <E extends LivingEntity> boolean isTiredOfTryingToReachTarget(E entity) {
        Optional<Long> optional = entity.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        return optional.isPresent() && entity.level.getGameTime() - optional.get() > 200L;
    }

    private boolean isCurrentTargetDeadOrRemoved(E entity) {
        Optional<LivingEntity> optional = entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return optional.isPresent() && !optional.get().isAlive();
    }

    protected void clearAttackTarget(E entity) {
        this.onTargetErased.accept(entity);
        entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
