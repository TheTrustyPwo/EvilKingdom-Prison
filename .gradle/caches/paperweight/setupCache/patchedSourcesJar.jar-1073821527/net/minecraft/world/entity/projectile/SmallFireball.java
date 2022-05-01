package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SmallFireball extends Fireball {
    public SmallFireball(EntityType<? extends SmallFireball> type, Level world) {
        super(type, world);
    }

    public SmallFireball(Level world, LivingEntity owner, double velocityX, double velocityY, double velocityZ) {
        super(EntityType.SMALL_FIREBALL, owner, velocityX, velocityY, velocityZ, world);
    }

    public SmallFireball(Level world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(EntityType.SMALL_FIREBALL, x, y, z, velocityX, velocityY, velocityZ, world);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level.isClientSide) {
            Entity entity = entityHitResult.getEntity();
            if (!entity.fireImmune()) {
                Entity entity2 = this.getOwner();
                int i = entity.getRemainingFireTicks();
                entity.setSecondsOnFire(5);
                boolean bl = entity.hurt(DamageSource.fireball(this, entity2), 5.0F);
                if (!bl) {
                    entity.setRemainingFireTicks(i);
                } else if (entity2 instanceof LivingEntity) {
                    this.doEnchantDamageEffects((LivingEntity)entity2, entity);
                }
            }

        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (!this.level.isClientSide) {
            Entity entity = this.getOwner();
            if (!(entity instanceof Mob) || this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                BlockPos blockPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                if (this.level.isEmptyBlock(blockPos)) {
                    this.level.setBlockAndUpdate(blockPos, BaseFireBlock.getState(this.level, blockPos));
                }
            }

        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
            this.discard();
        }

    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
