package net.minecraft.world.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class MinecartTNT extends AbstractMinecart {
    private static final byte EVENT_PRIME = 10;
    private int fuse = -1;

    public MinecartTNT(EntityType<? extends MinecartTNT> type, Level world) {
        super(type, world);
    }

    public MinecartTNT(Level world, double x, double y, double z) {
        super(EntityType.TNT_MINECART, world, x, y, z);
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.TNT;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.TNT.defaultBlockState();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fuse > 0) {
            --this.fuse;
            this.level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
        } else if (this.fuse == 0) {
            this.explode(this.getDeltaMovement().horizontalDistanceSqr());
        }

        if (this.horizontalCollision) {
            double d = this.getDeltaMovement().horizontalDistanceSqr();
            if (d >= (double)0.01F) {
                this.explode(d);
            }
        }

    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity entity = source.getDirectEntity();
        if (entity instanceof AbstractArrow) {
            AbstractArrow abstractArrow = (AbstractArrow)entity;
            if (abstractArrow.isOnFire()) {
                this.explode(abstractArrow.getDeltaMovement().lengthSqr());
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void destroy(DamageSource damageSource) {
        double d = this.getDeltaMovement().horizontalDistanceSqr();
        if (!damageSource.isFire() && !damageSource.isExplosion() && !(d >= (double)0.01F)) {
            super.destroy(damageSource);
            if (!damageSource.isExplosion() && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                this.spawnAtLocation(Blocks.TNT);
            }

        } else {
            if (this.fuse < 0) {
                this.primeFuse();
                this.fuse = this.random.nextInt(20) + this.random.nextInt(20);
            }

        }
    }

    protected void explode(double velocity) {
        if (!this.level.isClientSide) {
            double d = Math.sqrt(velocity);
            if (d > 5.0D) {
                d = 5.0D;
            }

            this.level.explode(this, this.getX(), this.getY(), this.getZ(), (float)(4.0D + this.random.nextDouble() * 1.5D * d), Explosion.BlockInteraction.BREAK);
            this.discard();
        }

    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (fallDistance >= 3.0F) {
            float f = fallDistance / 10.0F;
            this.explode((double)(f * f));
        }

        return super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
    }

    @Override
    public void activateMinecart(int x, int y, int z, boolean powered) {
        if (powered && this.fuse < 0) {
            this.primeFuse();
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 10) {
            this.primeFuse();
        } else {
            super.handleEntityEvent(status);
        }

    }

    public void primeFuse() {
        this.fuse = 80;
        if (!this.level.isClientSide) {
            this.level.broadcastEntityEvent(this, (byte)10);
            if (!this.isSilent()) {
                this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.fuse > -1;
    }

    @Override
    public float getBlockExplosionResistance(Explosion explosion, BlockGetter world, BlockPos pos, BlockState blockState, FluidState fluidState, float max) {
        return !this.isPrimed() || !blockState.is(BlockTags.RAILS) && !world.getBlockState(pos.above()).is(BlockTags.RAILS) ? super.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState, max) : 0.0F;
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter world, BlockPos pos, BlockState state, float explosionPower) {
        return !this.isPrimed() || !state.is(BlockTags.RAILS) && !world.getBlockState(pos.above()).is(BlockTags.RAILS) ? super.shouldBlockExplode(explosion, world, pos, state, explosionPower) : false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("TNTFuse", 99)) {
            this.fuse = nbt.getInt("TNTFuse");
        }

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("TNTFuse", this.fuse);
    }
}
