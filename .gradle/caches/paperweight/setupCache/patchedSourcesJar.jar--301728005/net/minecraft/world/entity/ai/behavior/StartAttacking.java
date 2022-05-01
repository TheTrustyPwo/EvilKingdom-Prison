package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
// CraftBukkit end

public class StartAttacking<E extends Mob> extends Behavior<E> {

    private final Predicate<E> canAttackPredicate;
    private final Function<E, Optional<? extends LivingEntity>> targetFinderFunction;

    public StartAttacking(Predicate<E> startCondition, Function<E, Optional<? extends LivingEntity>> targetGetter) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED));
        this.canAttackPredicate = startCondition;
        this.targetFinderFunction = targetGetter;
    }

    public StartAttacking(Function<E, Optional<? extends LivingEntity>> targetGetter) {
        this((entityinsentient) -> {
            return true;
        }, targetGetter);
    }

    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        if (!this.canAttackPredicate.test(entity)) {
            return false;
        } else {
            Optional<? extends LivingEntity> optional = (Optional) this.targetFinderFunction.apply(entity);

            return optional.isPresent() ? entity.canAttack((LivingEntity) optional.get()) : false;
        }
    }

    protected void start(ServerLevel world, E entity, long time) {
        (this.targetFinderFunction.apply(entity)).ifPresent((entityliving) -> { // CraftBukkit - decompile error
            this.setAttackTarget(entity, entityliving);
        });
    }

    private void setAttackTarget(E entity, LivingEntity target) {
        // CraftBukkit start
        EntityTargetEvent event = CraftEventFactory.callEntityTargetLivingEvent(entity, target, (target instanceof ServerPlayer) ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.CLOSEST_ENTITY);
        if (event.isCancelled()) {
            return;
        }
        target = (event.getTarget() != null) ? ((CraftLivingEntity) event.getTarget()).getHandle() : null;
        // CraftBukkit end

        entity.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target); // CraftBukkit - decompile error
        entity.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }
}
