package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CrossbowAttack<E extends Mob & CrossbowAttackMob, T extends LivingEntity> extends Behavior<E> {
    private static final int TIMEOUT = 1200;
    private int attackDelay;
    private CrossbowAttack.CrossbowState crossbowState = CrossbowAttack.CrossbowState.UNCHARGED;

    public CrossbowAttack() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        LivingEntity livingEntity = getAttackTarget(entity);
        return entity.isHolding(Items.CROSSBOW) && BehaviorUtils.canSee(entity, livingEntity) && BehaviorUtils.isWithinAttackRange(entity, livingEntity, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, E mob, long l) {
        return mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && this.checkExtraStartConditions(serverLevel, mob);
    }

    @Override
    protected void tick(ServerLevel serverLevel, E mob, long l) {
        LivingEntity livingEntity = getAttackTarget(mob);
        this.lookAtTarget(mob, livingEntity);
        this.crossbowAttack(mob, livingEntity);
    }

    @Override
    protected void stop(ServerLevel world, E entity, long time) {
        if (entity.isUsingItem()) {
            entity.stopUsingItem();
        }

        if (entity.isHolding(Items.CROSSBOW)) {
            entity.setChargingCrossbow(false);
            CrossbowItem.setCharged(entity.getUseItem(), false);
        }

    }

    private void crossbowAttack(E entity, LivingEntity target) {
        if (this.crossbowState == CrossbowAttack.CrossbowState.UNCHARGED) {
            entity.startUsingItem(ProjectileUtil.getWeaponHoldingHand(entity, Items.CROSSBOW));
            this.crossbowState = CrossbowAttack.CrossbowState.CHARGING;
            entity.setChargingCrossbow(true);
        } else if (this.crossbowState == CrossbowAttack.CrossbowState.CHARGING) {
            if (!entity.isUsingItem()) {
                this.crossbowState = CrossbowAttack.CrossbowState.UNCHARGED;
            }

            int i = entity.getTicksUsingItem();
            ItemStack itemStack = entity.getUseItem();
            if (i >= CrossbowItem.getChargeDuration(itemStack)) {
                entity.releaseUsingItem();
                this.crossbowState = CrossbowAttack.CrossbowState.CHARGED;
                this.attackDelay = 20 + entity.getRandom().nextInt(20);
                entity.setChargingCrossbow(false);
            }
        } else if (this.crossbowState == CrossbowAttack.CrossbowState.CHARGED) {
            --this.attackDelay;
            if (this.attackDelay == 0) {
                this.crossbowState = CrossbowAttack.CrossbowState.READY_TO_ATTACK;
            }
        } else if (this.crossbowState == CrossbowAttack.CrossbowState.READY_TO_ATTACK) {
            entity.performRangedAttack(target, 1.0F);
            ItemStack itemStack2 = entity.getItemInHand(ProjectileUtil.getWeaponHoldingHand(entity, Items.CROSSBOW));
            CrossbowItem.setCharged(itemStack2, false);
            this.crossbowState = CrossbowAttack.CrossbowState.UNCHARGED;
        }

    }

    private void lookAtTarget(Mob entity, LivingEntity target) {
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
    }

    private static LivingEntity getAttackTarget(LivingEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }

    static enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK;
    }
}
