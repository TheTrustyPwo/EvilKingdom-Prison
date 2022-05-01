package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;

public class MeleeAttack extends Behavior<Mob> {
    private final int cooldownBetweenAttacks;

    public MeleeAttack(int interval) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT));
        this.cooldownBetweenAttacks = interval;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Mob entity) {
        LivingEntity livingEntity = this.getAttackTarget(entity);
        return !this.isHoldingUsableProjectileWeapon(entity) && BehaviorUtils.canSee(entity, livingEntity) && BehaviorUtils.isWithinMeleeAttackRange(entity, livingEntity);
    }

    private boolean isHoldingUsableProjectileWeapon(Mob entity) {
        return entity.isHolding((stack) -> {
            Item item = stack.getItem();
            return item instanceof ProjectileWeaponItem && entity.canFireProjectileWeapon((ProjectileWeaponItem)item);
        });
    }

    @Override
    protected void start(ServerLevel world, Mob entity, long time) {
        LivingEntity livingEntity = this.getAttackTarget(entity);
        BehaviorUtils.lookAtEntity(entity, livingEntity);
        entity.swing(InteractionHand.MAIN_HAND);
        entity.doHurtTarget(livingEntity);
        entity.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, (long)this.cooldownBetweenAttacks);
    }

    private LivingEntity getAttackTarget(Mob entity) {
        return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }
}
