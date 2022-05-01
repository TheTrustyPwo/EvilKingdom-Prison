package net.minecraft.world.entity.projectile;

import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FireworkRocketEntity extends Projectile implements ItemSupplier {
    public static final EntityDataAccessor<ItemStack> DATA_ID_FIREWORKS_ITEM = SynchedEntityData.defineId(FireworkRocketEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<OptionalInt> DATA_ATTACHED_TO_TARGET = SynchedEntityData.defineId(FireworkRocketEntity.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT);
    public static final EntityDataAccessor<Boolean> DATA_SHOT_AT_ANGLE = SynchedEntityData.defineId(FireworkRocketEntity.class, EntityDataSerializers.BOOLEAN);
    public int life;
    public int lifetime;
    @Nullable
    public LivingEntity attachedToEntity;

    public FireworkRocketEntity(EntityType<? extends FireworkRocketEntity> type, Level world) {
        super(type, world);
    }

    public FireworkRocketEntity(Level world, double x, double y, double z, ItemStack stack) {
        super(EntityType.FIREWORK_ROCKET, world);
        this.life = 0;
        this.setPos(x, y, z);
        int i = 1;
        if (!stack.isEmpty() && stack.hasTag()) {
            this.entityData.set(DATA_ID_FIREWORKS_ITEM, stack.copy());
            i += stack.getOrCreateTagElement("Fireworks").getByte("Flight");
        }

        this.setDeltaMovement(this.random.nextGaussian() * 0.001D, 0.05D, this.random.nextGaussian() * 0.001D);
        this.lifetime = 10 * i + this.random.nextInt(6) + this.random.nextInt(7);
    }

    public FireworkRocketEntity(Level world, @Nullable Entity entity, double x, double y, double z, ItemStack stack) {
        this(world, x, y, z, stack);
        this.setOwner(entity);
    }

    public FireworkRocketEntity(Level world, ItemStack stack, LivingEntity shooter) {
        this(world, shooter, shooter.getX(), shooter.getY(), shooter.getZ(), stack);
        this.entityData.set(DATA_ATTACHED_TO_TARGET, OptionalInt.of(shooter.getId()));
        this.attachedToEntity = shooter;
    }

    public FireworkRocketEntity(Level world, ItemStack stack, double x, double y, double z, boolean shotAtAngle) {
        this(world, x, y, z, stack);
        this.entityData.set(DATA_SHOT_AT_ANGLE, shotAtAngle);
    }

    public FireworkRocketEntity(Level world, ItemStack stack, Entity entity, double x, double y, double z, boolean shotAtAngle) {
        this(world, stack, x, y, z, shotAtAngle);
        this.setOwner(entity);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ID_FIREWORKS_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_ATTACHED_TO_TARGET, OptionalInt.empty());
        this.entityData.define(DATA_SHOT_AT_ANGLE, false);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D && !this.isAttachedToEntity();
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return super.shouldRender(cameraX, cameraY, cameraZ) && !this.isAttachedToEntity();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAttachedToEntity()) {
            if (this.attachedToEntity == null) {
                this.entityData.get(DATA_ATTACHED_TO_TARGET).ifPresent((id) -> {
                    Entity entity = this.level.getEntity(id);
                    if (entity instanceof LivingEntity) {
                        this.attachedToEntity = (LivingEntity)entity;
                    }

                });
            }

            if (this.attachedToEntity != null) {
                Vec3 vec33;
                if (this.attachedToEntity.isFallFlying()) {
                    Vec3 vec3 = this.attachedToEntity.getLookAngle();
                    double d = 1.5D;
                    double e = 0.1D;
                    Vec3 vec32 = this.attachedToEntity.getDeltaMovement();
                    this.attachedToEntity.setDeltaMovement(vec32.add(vec3.x * 0.1D + (vec3.x * 1.5D - vec32.x) * 0.5D, vec3.y * 0.1D + (vec3.y * 1.5D - vec32.y) * 0.5D, vec3.z * 0.1D + (vec3.z * 1.5D - vec32.z) * 0.5D));
                    vec33 = this.attachedToEntity.getHandHoldingItemAngle(Items.FIREWORK_ROCKET);
                } else {
                    vec33 = Vec3.ZERO;
                }

                this.setPos(this.attachedToEntity.getX() + vec33.x, this.attachedToEntity.getY() + vec33.y, this.attachedToEntity.getZ() + vec33.z);
                this.setDeltaMovement(this.attachedToEntity.getDeltaMovement());
            }
        } else {
            if (!this.isShotAtAngle()) {
                double f = this.horizontalCollision ? 1.0D : 1.15D;
                this.setDeltaMovement(this.getDeltaMovement().multiply(f, 1.0D, f).add(0.0D, 0.04D, 0.0D));
            }

            Vec3 vec35 = this.getDeltaMovement();
            this.move(MoverType.SELF, vec35);
            this.setDeltaMovement(vec35);
        }

        HitResult hitResult = ProjectileUtil.getHitResult(this, this::canHitEntity);
        if (!this.noPhysics) {
            this.onHit(hitResult);
            this.hasImpulse = true;
        }

        this.updateRotation();
        if (this.life == 0 && !this.isSilent()) {
            this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0F, 1.0F);
        }

        ++this.life;
        if (this.level.isClientSide && this.life % 2 < 2) {
            this.level.addParticle(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.05D, -this.getDeltaMovement().y * 0.5D, this.random.nextGaussian() * 0.05D);
        }

        if (!this.level.isClientSide && this.life > this.lifetime) {
            this.explode();
        }

    }

    private void explode() {
        this.level.broadcastEntityEvent(this, (byte)17);
        this.gameEvent(GameEvent.EXPLODE, this.getOwner());
        this.dealExplosionDamage();
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level.isClientSide) {
            this.explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        BlockPos blockPos = new BlockPos(blockHitResult.getBlockPos());
        this.level.getBlockState(blockPos).entityInside(this.level, blockPos, this);
        if (!this.level.isClientSide() && this.hasExplosion()) {
            this.explode();
        }

        super.onHitBlock(blockHitResult);
    }

    private boolean hasExplosion() {
        ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        CompoundTag compoundTag = itemStack.isEmpty() ? null : itemStack.getTagElement("Fireworks");
        ListTag listTag = compoundTag != null ? compoundTag.getList("Explosions", 10) : null;
        return listTag != null && !listTag.isEmpty();
    }

    private void dealExplosionDamage() {
        float f = 0.0F;
        ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        CompoundTag compoundTag = itemStack.isEmpty() ? null : itemStack.getTagElement("Fireworks");
        ListTag listTag = compoundTag != null ? compoundTag.getList("Explosions", 10) : null;
        if (listTag != null && !listTag.isEmpty()) {
            f = 5.0F + (float)(listTag.size() * 2);
        }

        if (f > 0.0F) {
            if (this.attachedToEntity != null) {
                this.attachedToEntity.hurt(DamageSource.fireworks(this, this.getOwner()), 5.0F + (float)(listTag.size() * 2));
            }

            double d = 5.0D;
            Vec3 vec3 = this.position();

            for(LivingEntity livingEntity : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0D))) {
                if (livingEntity != this.attachedToEntity && !(this.distanceToSqr(livingEntity) > 25.0D)) {
                    boolean bl = false;

                    for(int i = 0; i < 2; ++i) {
                        Vec3 vec32 = new Vec3(livingEntity.getX(), livingEntity.getY(0.5D * (double)i), livingEntity.getZ());
                        HitResult hitResult = this.level.clip(new ClipContext(vec3, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                        if (hitResult.getType() == HitResult.Type.MISS) {
                            bl = true;
                            break;
                        }
                    }

                    if (bl) {
                        float g = f * (float)Math.sqrt((5.0D - (double)this.distanceTo(livingEntity)) / 5.0D);
                        livingEntity.hurt(DamageSource.fireworks(this, this.getOwner()), g);
                    }
                }
            }
        }

    }

    private boolean isAttachedToEntity() {
        return this.entityData.get(DATA_ATTACHED_TO_TARGET).isPresent();
    }

    public boolean isShotAtAngle() {
        return this.entityData.get(DATA_SHOT_AT_ANGLE);
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 17 && this.level.isClientSide) {
            if (!this.hasExplosion()) {
                for(int i = 0; i < this.random.nextInt(3) + 2; ++i) {
                    this.level.addParticle(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.05D, 0.005D, this.random.nextGaussian() * 0.05D);
                }
            } else {
                ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
                CompoundTag compoundTag = itemStack.isEmpty() ? null : itemStack.getTagElement("Fireworks");
                Vec3 vec3 = this.getDeltaMovement();
                this.level.createFireworks(this.getX(), this.getY(), this.getZ(), vec3.x, vec3.y, vec3.z, compoundTag);
            }
        }

        super.handleEntityEvent(status);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Life", this.life);
        nbt.putInt("LifeTime", this.lifetime);
        ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        if (!itemStack.isEmpty()) {
            nbt.put("FireworksItem", itemStack.save(new CompoundTag()));
        }

        nbt.putBoolean("ShotAtAngle", this.entityData.get(DATA_SHOT_AT_ANGLE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.life = nbt.getInt("Life");
        this.lifetime = nbt.getInt("LifeTime");
        ItemStack itemStack = ItemStack.of(nbt.getCompound("FireworksItem"));
        if (!itemStack.isEmpty()) {
            this.entityData.set(DATA_ID_FIREWORKS_ITEM, itemStack);
        }

        if (nbt.contains("ShotAtAngle")) {
            this.entityData.set(DATA_SHOT_AT_ANGLE, nbt.getBoolean("ShotAtAngle"));
        }

    }

    @Override
    public ItemStack getItem() {
        ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        return itemStack.isEmpty() ? new ItemStack(Items.FIREWORK_ROCKET) : itemStack;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
