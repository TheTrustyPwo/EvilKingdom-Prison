package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class Vex extends Monster {
    public static final float FLAP_DEGREES_PER_TICK = 45.836624F;
    public static final int TICKS_PER_FLAP = Mth.ceil(3.9269907F);
    protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Vex.class, EntityDataSerializers.BYTE);
    private static final int FLAG_IS_CHARGING = 1;
    @Nullable
    Mob owner;
    @Nullable
    private BlockPos boundOrigin;
    public boolean hasLimitedLife;
    public int limitedLifeTicks;

    public Vex(EntityType<? extends Vex> type, Level world) {
        super(type, world);
        this.moveControl = new Vex.VexMoveControl(this);
        this.xpReward = 3;
    }

    @Override
    public boolean isFlapping() {
        return this.tickCount % TICKS_PER_FLAP == 0;
    }

    @Override
    public void move(MoverType movementType, Vec3 movement) {
        super.move(movementType, movement);
        this.checkInsideBlocks();
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;
        this.setNoGravity(true);
        if (this.hasLimitedLife && --this.limitedLifeTicks <= 0) {
            this.limitedLifeTicks = 20;
            this.hurt(DamageSource.STARVE, 1.0F);
        }

    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new Vex.VexChargeAttackGoal());
        this.goalSelector.addGoal(8, new Vex.VexRandomMoveGoal());
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, Raider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, new Vex.VexCopyOwnerTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14.0D).add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLAGS_ID, (byte)0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("BoundX")) {
            this.boundOrigin = new BlockPos(nbt.getInt("BoundX"), nbt.getInt("BoundY"), nbt.getInt("BoundZ"));
        }

        if (nbt.contains("LifeTicks")) {
            this.setLimitedLife(nbt.getInt("LifeTicks"));
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.boundOrigin != null) {
            nbt.putInt("BoundX", this.boundOrigin.getX());
            nbt.putInt("BoundY", this.boundOrigin.getY());
            nbt.putInt("BoundZ", this.boundOrigin.getZ());
        }

        if (this.hasLimitedLife) {
            nbt.putInt("LifeTicks", this.limitedLifeTicks);
        }

    }

    @Nullable
    public Mob getOwner() {
        return this.owner;
    }

    @Nullable
    public BlockPos getBoundOrigin() {
        return this.boundOrigin;
    }

    public void setBoundOrigin(@Nullable BlockPos bounds) {
        this.boundOrigin = bounds;
    }

    private boolean getVexFlag(int mask) {
        int i = this.entityData.get(DATA_FLAGS_ID);
        return (i & mask) != 0;
    }

    private void setVexFlag(int mask, boolean value) {
        int i = this.entityData.get(DATA_FLAGS_ID);
        if (value) {
            i |= mask;
        } else {
            i &= ~mask;
        }

        this.entityData.set(DATA_FLAGS_ID, (byte)(i & 255));
    }

    public boolean isCharging() {
        return this.getVexFlag(1);
    }

    public void setIsCharging(boolean charging) {
        this.setVexFlag(1, charging);
    }

    public void setOwner(Mob owner) {
        this.owner = owner;
    }

    public void setLimitedLife(int lifeTicks) {
        this.hasLimitedLife = true;
        this.limitedLifeTicks = lifeTicks;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        this.populateDefaultEquipmentSlots(difficulty);
        this.populateDefaultEquipmentEnchantments(difficulty);
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    class VexChargeAttackGoal extends Goal {
        public VexChargeAttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Vex.this.getTarget() != null && !Vex.this.getMoveControl().hasWanted() && Vex.this.random.nextInt(reducedTickDelay(7)) == 0) {
                return Vex.this.distanceToSqr(Vex.this.getTarget()) > 4.0D;
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Vex.this.getMoveControl().hasWanted() && Vex.this.isCharging() && Vex.this.getTarget() != null && Vex.this.getTarget().isAlive();
        }

        @Override
        public void start() {
            LivingEntity livingEntity = Vex.this.getTarget();
            if (livingEntity != null) {
                Vec3 vec3 = livingEntity.getEyePosition();
                Vex.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, 1.0D);
            }

            Vex.this.setIsCharging(true);
            Vex.this.playSound(SoundEvents.VEX_CHARGE, 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            Vex.this.setIsCharging(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity livingEntity = Vex.this.getTarget();
            if (livingEntity != null) {
                if (Vex.this.getBoundingBox().intersects(livingEntity.getBoundingBox())) {
                    Vex.this.doHurtTarget(livingEntity);
                    Vex.this.setIsCharging(false);
                } else {
                    double d = Vex.this.distanceToSqr(livingEntity);
                    if (d < 9.0D) {
                        Vec3 vec3 = livingEntity.getEyePosition();
                        Vex.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, 1.0D);
                    }
                }

            }
        }
    }

    class VexCopyOwnerTargetGoal extends TargetGoal {
        private final TargetingConditions copyOwnerTargeting = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

        public VexCopyOwnerTargetGoal(PathfinderMob mob) {
            super(mob, false);
        }

        @Override
        public boolean canUse() {
            return Vex.this.owner != null && Vex.this.owner.getTarget() != null && this.canAttack(Vex.this.owner.getTarget(), this.copyOwnerTargeting);
        }

        @Override
        public void start() {
            Vex.this.setTarget(Vex.this.owner.getTarget());
            super.start();
        }
    }

    class VexMoveControl extends MoveControl {
        public VexMoveControl(Vex owner) {
            super(owner);
        }

        @Override
        public void tick() {
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                Vec3 vec3 = new Vec3(this.wantedX - Vex.this.getX(), this.wantedY - Vex.this.getY(), this.wantedZ - Vex.this.getZ());
                double d = vec3.length();
                if (d < Vex.this.getBoundingBox().getSize()) {
                    this.operation = MoveControl.Operation.WAIT;
                    Vex.this.setDeltaMovement(Vex.this.getDeltaMovement().scale(0.5D));
                } else {
                    Vex.this.setDeltaMovement(Vex.this.getDeltaMovement().add(vec3.scale(this.speedModifier * 0.05D / d)));
                    if (Vex.this.getTarget() == null) {
                        Vec3 vec32 = Vex.this.getDeltaMovement();
                        Vex.this.setYRot(-((float)Mth.atan2(vec32.x, vec32.z)) * (180F / (float)Math.PI));
                        Vex.this.yBodyRot = Vex.this.getYRot();
                    } else {
                        double e = Vex.this.getTarget().getX() - Vex.this.getX();
                        double f = Vex.this.getTarget().getZ() - Vex.this.getZ();
                        Vex.this.setYRot(-((float)Mth.atan2(e, f)) * (180F / (float)Math.PI));
                        Vex.this.yBodyRot = Vex.this.getYRot();
                    }
                }

            }
        }
    }

    class VexRandomMoveGoal extends Goal {
        public VexRandomMoveGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !Vex.this.getMoveControl().hasWanted() && Vex.this.random.nextInt(reducedTickDelay(7)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
            BlockPos blockPos = Vex.this.getBoundOrigin();
            if (blockPos == null) {
                blockPos = Vex.this.blockPosition();
            }

            for(int i = 0; i < 3; ++i) {
                BlockPos blockPos2 = blockPos.offset(Vex.this.random.nextInt(15) - 7, Vex.this.random.nextInt(11) - 5, Vex.this.random.nextInt(15) - 7);
                if (Vex.this.level.isEmptyBlock(blockPos2)) {
                    Vex.this.moveControl.setWantedPosition((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.5D, (double)blockPos2.getZ() + 0.5D, 0.25D);
                    if (Vex.this.getTarget() == null) {
                        Vex.this.getLookControl().setLookAt((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.5D, (double)blockPos2.getZ() + 0.5D, 180.0F, 20.0F);
                    }
                    break;
                }
            }

        }
    }
}
