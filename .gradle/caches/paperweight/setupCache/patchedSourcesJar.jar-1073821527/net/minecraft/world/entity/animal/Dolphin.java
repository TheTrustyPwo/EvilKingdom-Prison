package net.minecraft.world.entity.animal;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ConfiguredStructureTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.DolphinJumpGoal;
import net.minecraft.world.entity.ai.goal.FollowBoatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class Dolphin extends WaterAnimal {
    private static final EntityDataAccessor<BlockPos> TREASURE_POS = SynchedEntityData.defineId(Dolphin.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> GOT_FISH = SynchedEntityData.defineId(Dolphin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOISTNESS_LEVEL = SynchedEntityData.defineId(Dolphin.class, EntityDataSerializers.INT);
    static final TargetingConditions SWIM_WITH_PLAYER_TARGETING = TargetingConditions.forNonCombat().range(10.0D).ignoreLineOfSight();
    public static final int TOTAL_AIR_SUPPLY = 4800;
    private static final int TOTAL_MOISTNESS_LEVEL = 2400;
    public static final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> {
        return !item.hasPickUpDelay() && item.isAlive() && item.isInWater();
    };

    public Dolphin(EntityType<? extends Dolphin> type, Level world) {
        super(type, world);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setCanPickUpLoot(true);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        this.setAirSupply(this.getMaxAirSupply());
        this.setXRot(0.0F);
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return false;
    }

    @Override
    protected void handleAirSupply(int air) {
    }

    public void setTreasurePos(BlockPos treasurePos) {
        this.entityData.set(TREASURE_POS, treasurePos);
    }

    public BlockPos getTreasurePos() {
        return this.entityData.get(TREASURE_POS);
    }

    public boolean gotFish() {
        return this.entityData.get(GOT_FISH);
    }

    public void setGotFish(boolean hasFish) {
        this.entityData.set(GOT_FISH, hasFish);
    }

    public int getMoistnessLevel() {
        return this.entityData.get(MOISTNESS_LEVEL);
    }

    public void setMoisntessLevel(int moistness) {
        this.entityData.set(MOISTNESS_LEVEL, moistness);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TREASURE_POS, BlockPos.ZERO);
        this.entityData.define(GOT_FISH, false);
        this.entityData.define(MOISTNESS_LEVEL, 2400);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("TreasurePosX", this.getTreasurePos().getX());
        nbt.putInt("TreasurePosY", this.getTreasurePos().getY());
        nbt.putInt("TreasurePosZ", this.getTreasurePos().getZ());
        nbt.putBoolean("GotFish", this.gotFish());
        nbt.putInt("Moistness", this.getMoistnessLevel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        int i = nbt.getInt("TreasurePosX");
        int j = nbt.getInt("TreasurePosY");
        int k = nbt.getInt("TreasurePosZ");
        this.setTreasurePos(new BlockPos(i, j, k));
        super.readAdditionalSaveData(nbt);
        this.setGotFish(nbt.getBoolean("GotFish"));
        this.setMoisntessLevel(nbt.getInt("Moistness"));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreathAirGoal(this));
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(1, new Dolphin.DolphinSwimToTreasureGoal(this));
        this.goalSelector.addGoal(2, new Dolphin.DolphinSwimWithPlayerGoal(this, 4.0D));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new DolphinJumpGoal(this, 10));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, (double)1.2F, true));
        this.goalSelector.addGoal(8, new Dolphin.PlayWithItemsGoal());
        this.goalSelector.addGoal(8, new FollowBoatGoal(this));
        this.goalSelector.addGoal(9, new AvoidEntityGoal<>(this, Guardian.class, 8.0F, 1.0D, 1.0D));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, Guardian.class)).setAlertOthers());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, (double)1.2F).add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new WaterBoundPathNavigation(this, world);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean bl = target.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (bl) {
            this.doEnchantDamageEffects(this, target);
            this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.0F, 1.0F);
        }

        return bl;
    }

    @Override
    public int getMaxAirSupply() {
        return 4800;
    }

    @Override
    protected int increaseAirSupply(int air) {
        return this.getMaxAirSupply();
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.3F;
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return true;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EquipmentSlot equipmentSlot = Mob.getEquipmentSlotForItem(stack);
        if (!this.getItemBySlot(equipmentSlot).isEmpty()) {
            return false;
        } else {
            return equipmentSlot == EquipmentSlot.MAINHAND && super.canTakeItem(stack);
        }
    }

    @Override
    protected void pickUpItem(ItemEntity item) {
        if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            ItemStack itemStack = item.getItem();
            if (this.canHoldItem(itemStack)) {
                this.onItemPickup(item);
                this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
                this.handDropChances[EquipmentSlot.MAINHAND.getIndex()] = 2.0F;
                this.take(item, itemStack.getCount());
                item.discard();
            }
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.isNoAi()) {
            this.setAirSupply(this.getMaxAirSupply());
        } else {
            if (this.isInWaterRainOrBubble()) {
                this.setMoisntessLevel(2400);
            } else {
                this.setMoisntessLevel(this.getMoistnessLevel() - 1);
                if (this.getMoistnessLevel() <= 0) {
                    this.hurt(DamageSource.DRY_OUT, 1.0F);
                }

                if (this.onGround) {
                    this.setDeltaMovement(this.getDeltaMovement().add((double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F), 0.5D, (double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F)));
                    this.setYRot(this.random.nextFloat() * 360.0F);
                    this.onGround = false;
                    this.hasImpulse = true;
                }
            }

            if (this.level.isClientSide && this.isInWater() && this.getDeltaMovement().lengthSqr() > 0.03D) {
                Vec3 vec3 = this.getViewVector(0.0F);
                float f = Mth.cos(this.getYRot() * ((float)Math.PI / 180F)) * 0.3F;
                float g = Mth.sin(this.getYRot() * ((float)Math.PI / 180F)) * 0.3F;
                float h = 1.2F - this.random.nextFloat() * 0.7F;

                for(int i = 0; i < 2; ++i) {
                    this.level.addParticle(ParticleTypes.DOLPHIN, this.getX() - vec3.x * (double)h + (double)f, this.getY() - vec3.y, this.getZ() - vec3.z * (double)h + (double)g, 0.0D, 0.0D, 0.0D);
                    this.level.addParticle(ParticleTypes.DOLPHIN, this.getX() - vec3.x * (double)h - (double)f, this.getY() - vec3.y, this.getZ() - vec3.z * (double)h - (double)g, 0.0D, 0.0D, 0.0D);
                }
            }

        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 38) {
            this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
        } else {
            super.handleEntityEvent(status);
        }

    }

    private void addParticlesAroundSelf(ParticleOptions parameters) {
        for(int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.01D;
            double e = this.random.nextGaussian() * 0.01D;
            double f = this.random.nextGaussian() * 0.01D;
            this.level.addParticle(parameters, this.getRandomX(1.0D), this.getRandomY() + 0.2D, this.getRandomZ(1.0D), d, e, f);
        }

    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.isEmpty() && itemStack.is(ItemTags.FISHES)) {
            if (!this.level.isClientSide) {
                this.playSound(SoundEvents.DOLPHIN_EAT, 1.0F, 1.0F);
            }

            this.setGotFish(true);
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Nullable
    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.DOLPHIN_AMBIENT_WATER : SoundEvents.DOLPHIN_AMBIENT;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.DOLPHIN_SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    protected boolean closeToNextPos() {
        BlockPos blockPos = this.getNavigation().getTargetPos();
        return blockPos != null ? blockPos.closerToCenterThan(this.position(), 12.0D) : false;
    }

    @Override
    public void travel(Vec3 movementInput) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), movementInput);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(movementInput);
        }

    }

    @Override
    public boolean canBeLeashed(Player player) {
        return true;
    }

    static class DolphinSwimToTreasureGoal extends Goal {
        private final Dolphin dolphin;
        private boolean stuck;

        DolphinSwimToTreasureGoal(Dolphin dolphin) {
            this.dolphin = dolphin;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public boolean canUse() {
            return this.dolphin.gotFish() && this.dolphin.getAirSupply() >= 100;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos blockPos = this.dolphin.getTreasurePos();
            return !(new BlockPos((double)blockPos.getX(), this.dolphin.getY(), (double)blockPos.getZ())).closerToCenterThan(this.dolphin.position(), 4.0D) && !this.stuck && this.dolphin.getAirSupply() >= 100;
        }

        @Override
        public void start() {
            if (this.dolphin.level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)this.dolphin.level;
                this.stuck = false;
                this.dolphin.getNavigation().stop();
                BlockPos blockPos = this.dolphin.blockPosition();
                BlockPos blockPos2 = serverLevel.findNearestMapFeature(ConfiguredStructureTags.DOLPHIN_LOCATED, blockPos, 50, false);
                if (blockPos2 != null) {
                    this.dolphin.setTreasurePos(blockPos2);
                    serverLevel.broadcastEntityEvent(this.dolphin, (byte)38);
                } else {
                    this.stuck = true;
                }
            }
        }

        @Override
        public void stop() {
            BlockPos blockPos = this.dolphin.getTreasurePos();
            if ((new BlockPos((double)blockPos.getX(), this.dolphin.getY(), (double)blockPos.getZ())).closerToCenterThan(this.dolphin.position(), 4.0D) || this.stuck) {
                this.dolphin.setGotFish(false);
            }

        }

        @Override
        public void tick() {
            Level level = this.dolphin.level;
            if (this.dolphin.closeToNextPos() || this.dolphin.getNavigation().isDone()) {
                Vec3 vec3 = Vec3.atCenterOf(this.dolphin.getTreasurePos());
                Vec3 vec32 = DefaultRandomPos.getPosTowards(this.dolphin, 16, 1, vec3, (double)((float)Math.PI / 8F));
                if (vec32 == null) {
                    vec32 = DefaultRandomPos.getPosTowards(this.dolphin, 8, 4, vec3, (double)((float)Math.PI / 2F));
                }

                if (vec32 != null) {
                    BlockPos blockPos = new BlockPos(vec32);
                    if (!level.getFluidState(blockPos).is(FluidTags.WATER) || !level.getBlockState(blockPos).isPathfindable(level, blockPos, PathComputationType.WATER)) {
                        vec32 = DefaultRandomPos.getPosTowards(this.dolphin, 8, 5, vec3, (double)((float)Math.PI / 2F));
                    }
                }

                if (vec32 == null) {
                    this.stuck = true;
                    return;
                }

                this.dolphin.getLookControl().setLookAt(vec32.x, vec32.y, vec32.z, (float)(this.dolphin.getMaxHeadYRot() + 20), (float)this.dolphin.getMaxHeadXRot());
                this.dolphin.getNavigation().moveTo(vec32.x, vec32.y, vec32.z, 1.3D);
                if (level.random.nextInt(this.adjustedTickDelay(80)) == 0) {
                    level.broadcastEntityEvent(this.dolphin, (byte)38);
                }
            }

        }
    }

    static class DolphinSwimWithPlayerGoal extends Goal {
        private final Dolphin dolphin;
        private final double speedModifier;
        @Nullable
        private Player player;

        DolphinSwimWithPlayerGoal(Dolphin dolphin, double speed) {
            this.dolphin = dolphin;
            this.speedModifier = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.player = this.dolphin.level.getNearestPlayer(Dolphin.SWIM_WITH_PLAYER_TARGETING, this.dolphin);
            if (this.player == null) {
                return false;
            } else {
                return this.player.isSwimming() && this.dolphin.getTarget() != this.player;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.player != null && this.player.isSwimming() && this.dolphin.distanceToSqr(this.player) < 256.0D;
        }

        @Override
        public void start() {
            this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
        }

        @Override
        public void stop() {
            this.player = null;
            this.dolphin.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.dolphin.getLookControl().setLookAt(this.player, (float)(this.dolphin.getMaxHeadYRot() + 20), (float)this.dolphin.getMaxHeadXRot());
            if (this.dolphin.distanceToSqr(this.player) < 6.25D) {
                this.dolphin.getNavigation().stop();
            } else {
                this.dolphin.getNavigation().moveTo(this.player, this.speedModifier);
            }

            if (this.player.isSwimming() && this.player.level.random.nextInt(6) == 0) {
                this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
            }

        }
    }

    class PlayWithItemsGoal extends Goal {
        private int cooldown;

        @Override
        public boolean canUse() {
            if (this.cooldown > Dolphin.this.tickCount) {
                return false;
            } else {
                List<ItemEntity> list = Dolphin.this.level.getEntitiesOfClass(ItemEntity.class, Dolphin.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), Dolphin.ALLOWED_ITEMS);
                return !list.isEmpty() || !Dolphin.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
            }
        }

        @Override
        public void start() {
            List<ItemEntity> list = Dolphin.this.level.getEntitiesOfClass(ItemEntity.class, Dolphin.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), Dolphin.ALLOWED_ITEMS);
            if (!list.isEmpty()) {
                Dolphin.this.getNavigation().moveTo(list.get(0), (double)1.2F);
                Dolphin.this.playSound(SoundEvents.DOLPHIN_PLAY, 1.0F, 1.0F);
            }

            this.cooldown = 0;
        }

        @Override
        public void stop() {
            ItemStack itemStack = Dolphin.this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                this.drop(itemStack);
                Dolphin.this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.cooldown = Dolphin.this.tickCount + Dolphin.this.random.nextInt(100);
            }

        }

        @Override
        public void tick() {
            List<ItemEntity> list = Dolphin.this.level.getEntitiesOfClass(ItemEntity.class, Dolphin.this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), Dolphin.ALLOWED_ITEMS);
            ItemStack itemStack = Dolphin.this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                this.drop(itemStack);
                Dolphin.this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            } else if (!list.isEmpty()) {
                Dolphin.this.getNavigation().moveTo(list.get(0), (double)1.2F);
            }

        }

        private void drop(ItemStack stack) {
            if (!stack.isEmpty()) {
                double d = Dolphin.this.getEyeY() - (double)0.3F;
                ItemEntity itemEntity = new ItemEntity(Dolphin.this.level, Dolphin.this.getX(), d, Dolphin.this.getZ(), stack);
                itemEntity.setPickUpDelay(40);
                itemEntity.setThrower(Dolphin.this.getUUID());
                float f = 0.3F;
                float g = Dolphin.this.random.nextFloat() * ((float)Math.PI * 2F);
                float h = 0.02F * Dolphin.this.random.nextFloat();
                itemEntity.setDeltaMovement((double)(0.3F * -Mth.sin(Dolphin.this.getYRot() * ((float)Math.PI / 180F)) * Mth.cos(Dolphin.this.getXRot() * ((float)Math.PI / 180F)) + Mth.cos(g) * h), (double)(0.3F * Mth.sin(Dolphin.this.getXRot() * ((float)Math.PI / 180F)) * 1.5F), (double)(0.3F * Mth.cos(Dolphin.this.getYRot() * ((float)Math.PI / 180F)) * Mth.cos(Dolphin.this.getXRot() * ((float)Math.PI / 180F)) + Mth.sin(g) * h));
                Dolphin.this.level.addFreshEntity(itemEntity);
            }
        }
    }
}
