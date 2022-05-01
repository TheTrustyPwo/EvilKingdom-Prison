package net.minecraft.world.entity.monster;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class Strider extends Animal implements ItemSteerable, Saddleable {

    private static final float SUFFOCATE_STEERING_MODIFIER = 0.23F;
    private static final float SUFFOCATE_SPEED_MODIFIER = 0.66F;
    private static final float STEERING_MODIFIER = 0.55F;
    private static final Ingredient FOOD_ITEMS = Ingredient.of(Items.WARPED_FUNGUS);
    private static final Ingredient TEMPT_ITEMS = Ingredient.of(Items.WARPED_FUNGUS, Items.WARPED_FUNGUS_ON_A_STICK);
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SUFFOCATING = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SADDLE_ID = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.BOOLEAN);
    public final ItemBasedSteering steering;
    @Nullable
    private TemptGoal temptGoal;
    @Nullable
    private PanicGoal panicGoal;

    public Strider(EntityType<? extends Strider> type, Level world) {
        super(type, world);
        this.steering = new ItemBasedSteering(this.entityData, Strider.DATA_BOOST_TIME, Strider.DATA_SADDLE_ID);
        this.blocksBuilding = true;
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
    }

    public static boolean checkStriderSpawnRules(EntityType<Strider> type, LevelAccessor world, MobSpawnType spawnReason, BlockPos pos, Random random) {
        BlockPos.MutableBlockPos blockposition_mutableblockposition = pos.mutable();

        do {
            blockposition_mutableblockposition.move(Direction.UP);
        } while (world.getFluidState(blockposition_mutableblockposition).is(FluidTags.LAVA));

        return world.getBlockState(blockposition_mutableblockposition).isAir();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (Strider.DATA_BOOST_TIME.equals(data) && this.level.isClientSide) {
            this.steering.onSynced();
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(Strider.DATA_BOOST_TIME, 0);
        this.entityData.define(Strider.DATA_SUFFOCATING, false);
        this.entityData.define(Strider.DATA_SADDLE_ID, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        this.steering.addAdditionalSaveData(nbt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.steering.readAdditionalSaveData(nbt);
    }

    @Override
    public boolean isSaddled() {
        return this.steering.hasSaddle();
    }

    @Override
    public boolean isSaddleable() {
        return this.isAlive() && !this.isBaby();
    }

    @Override
    public void equipSaddle(@Nullable SoundSource sound) {
        this.steering.setSaddle(true);
        if (sound != null) {
            this.level.playSound((Player) null, (Entity) this, SoundEvents.STRIDER_SADDLE, sound, 0.5F, 1.0F);
        }

    }

    @Override
    protected void registerGoals() {
        this.panicGoal = new PanicGoal(this, 1.65D);
        this.goalSelector.addGoal(1, this.panicGoal);
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.temptGoal = new TemptGoal(this, 1.4D, Strider.TEMPT_ITEMS, false);
        this.goalSelector.addGoal(3, this.temptGoal);
        this.goalSelector.addGoal(4, new Strider.StriderGoToLavaGoal(this, 1.5D));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D, 60));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Strider.class, 8.0F));
    }

    public void setSuffocating(boolean cold) {
        this.entityData.set(Strider.DATA_SUFFOCATING, cold);
    }

    public boolean isSuffocating() {
        return this.getVehicle() instanceof Strider ? ((Strider) this.getVehicle()).isSuffocating() : (Boolean) this.entityData.get(Strider.DATA_SUFFOCATING);
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    @Override
    public double getPassengersRidingOffset() {
        float f = Math.min(0.25F, this.animationSpeed);
        float f1 = this.animationPosition;

        return (double) this.getBbHeight() - 0.19D + (double) (0.12F * Mth.cos(f1 * 1.5F) * 2.0F * f);
    }

    @Override
    public boolean canBeControlledByRider() {
        Entity entity = this.getControllingPassenger();

        if (!(entity instanceof Player)) {
            return false;
        } else {
            Player entityhuman = (Player) entity;

            return entityhuman.getMainHandItem().is(Items.WARPED_FUNGUS_ON_A_STICK) || entityhuman.getOffhandItem().is(Items.WARPED_FUNGUS_ON_A_STICK);
        }
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader world) {
        return world.isUnobstructed(this);
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        return this.getFirstPassenger();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3[] avec3d = new Vec3[]{getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) passenger.getBbWidth(), passenger.getYRot()), getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) passenger.getBbWidth(), passenger.getYRot() - 22.5F), getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) passenger.getBbWidth(), passenger.getYRot() + 22.5F), getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) passenger.getBbWidth(), passenger.getYRot() - 45.0F), getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) passenger.getBbWidth(), passenger.getYRot() + 45.0F)};
        Set<BlockPos> set = Sets.newLinkedHashSet();
        double d0 = this.getBoundingBox().maxY;
        double d1 = this.getBoundingBox().minY - 0.5D;
        BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
        Vec3[] avec3d1 = avec3d;
        int i = avec3d.length;

        for (int j = 0; j < i; ++j) {
            Vec3 vec3d = avec3d1[j];

            blockposition_mutableblockposition.set(this.getX() + vec3d.x, d0, this.getZ() + vec3d.z);

            for (double d2 = d0; d2 > d1; --d2) {
                set.add(blockposition_mutableblockposition.immutable());
                blockposition_mutableblockposition.move(Direction.DOWN);
            }
        }

        Iterator iterator = set.iterator();

        while (iterator.hasNext()) {
            BlockPos blockposition = (BlockPos) iterator.next();

            if (!this.level.getFluidState(blockposition).is(FluidTags.LAVA)) {
                double d3 = this.level.getBlockFloorHeight(blockposition);

                if (DismountHelper.isBlockFloorValid(d3)) {
                    Vec3 vec3d1 = Vec3.upFromBottomCenterOf(blockposition, d3);
                    UnmodifiableIterator unmodifiableiterator = passenger.getDismountPoses().iterator();

                    while (unmodifiableiterator.hasNext()) {
                        Pose entitypose = (Pose) unmodifiableiterator.next();
                        AABB axisalignedbb = passenger.getLocalBoundsForPose(entitypose);

                        if (DismountHelper.canDismountTo(this.level, passenger, axisalignedbb.move(vec3d1))) {
                            passenger.setPose(entitypose);
                            return vec3d1;
                        }
                    }
                }
            }
        }

        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    @Override
    public void travel(Vec3 movementInput) {
        this.setSpeed(this.getMoveSpeed());
        this.travel(this, this.steering, movementInput);
    }

    public float getMoveSpeed() {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.isSuffocating() ? 0.66F : 1.0F);
    }

    @Override
    public float getSteeringSpeed() {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.isSuffocating() ? 0.23F : 0.55F);
    }

    @Override
    public void travelWithInput(Vec3 movementInput) {
        super.travel(movementInput);
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.6F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(this.isInLava() ? SoundEvents.STRIDER_STEP_LAVA : SoundEvents.STRIDER_STEP, 1.0F, 1.0F);
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
        this.checkInsideBlocks();
        if (this.isInLava()) {
            this.resetFallDistance();
        } else {
            super.checkFallDamage(heightDifference, onGround, landedState, landedPosition);
        }
    }

    @Override
    public void tick() {
        if (this.isBeingTempted() && this.random.nextInt(140) == 0) {
            this.playSound(SoundEvents.STRIDER_HAPPY, 1.0F, this.getVoicePitch());
        } else if (this.isPanicking() && this.random.nextInt(60) == 0) {
            this.playSound(SoundEvents.STRIDER_RETREAT, 1.0F, this.getVoicePitch());
        }

        BlockState iblockdata = this.level.getBlockState(this.blockPosition());
        BlockState iblockdata1 = this.getBlockStateOn();
        boolean flag = iblockdata.is(BlockTags.STRIDER_WARM_BLOCKS) || iblockdata1.is(BlockTags.STRIDER_WARM_BLOCKS) || this.getFluidHeight(FluidTags.LAVA) > 0.0D;

        // CraftBukkit start
        if (!flag ^ this.isSuffocating()) {
            if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callStriderTemperatureChangeEvent(this, !flag)) {
                this.setSuffocating(!flag);
            }
        }
        // CraftBukkit end
        super.tick();
        this.floatStrider();
        this.checkInsideBlocks();
    }

    private boolean isPanicking() {
        return this.panicGoal != null && this.panicGoal.isRunning();
    }

    private boolean isBeingTempted() {
        return this.temptGoal != null && this.temptGoal.isRunning();
    }

    @Override
    protected boolean shouldPassengersInheritMalus() {
        return true;
    }

    private void floatStrider() {
        if (this.isInLava()) {
            CollisionContext voxelshapecollision = CollisionContext.of(this);

            if (voxelshapecollision.isAbove(LiquidBlock.STABLE_SHAPE, this.blockPosition(), true) && !this.level.getFluidState(this.blockPosition().above()).is(FluidTags.LAVA)) {
                this.onGround = true;
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(0.0D, 0.05D, 0.0D));
            }
        }

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.17499999701976776D).add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return !this.isPanicking() && !this.isBeingTempted() ? SoundEvents.STRIDER_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isVehicle() && !this.isEyeInFluid(FluidTags.LAVA);
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new Strider.StriderPathNavigation(this, world);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return world.getBlockState(pos).getFluidState().is(FluidTags.LAVA) ? 10.0F : (this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F);
    }

    @Override
    public Strider getBreedOffspring(ServerLevel world, AgeableMob entity) {
        return (Strider) EntityType.STRIDER.create(world);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return Strider.FOOD_ITEMS.test(stack);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (this.isSaddled()) {
            this.spawnAtLocation((ItemLike) Items.SADDLE);
        }

    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean flag = this.isFood(player.getItemInHand(hand));

        if (!flag && this.isSaddled() && !this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level.isClientSide) {
                player.startRiding(this);
            }

            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            InteractionResult enuminteractionresult = super.mobInteract(player, hand);

            if (!enuminteractionresult.consumesAction()) {
                ItemStack itemstack = player.getItemInHand(hand);

                return itemstack.is(Items.SADDLE) ? itemstack.interactLivingEntity(player, this, hand) : InteractionResult.PASS;
            } else {
                if (flag && !this.isSilent()) {
                    this.level.playSound((Player) null, this.getX(), this.getY(), this.getZ(), SoundEvents.STRIDER_EAT, this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
                }

                return enuminteractionresult;
            }
        }
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0D, (double) (0.6F * this.getEyeHeight()), (double) (this.getBbWidth() * 0.4F));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        if (this.isBaby()) {
            return super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
        } else {
            Object object;

            if (this.random.nextInt(30) == 0) {
                Mob entityinsentient = (Mob) EntityType.ZOMBIFIED_PIGLIN.create(world.getLevel());

                object = this.spawnJockey(world, difficulty, entityinsentient, new Zombie.ZombieGroupData(Zombie.getSpawnAsBabyOdds(this.random), false));
                entityinsentient.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));
                this.equipSaddle((SoundSource) null);
            } else if (this.random.nextInt(10) == 0) {
                AgeableMob entityageable = (AgeableMob) EntityType.STRIDER.create(world.getLevel());

                entityageable.setAge(-24000);
                object = this.spawnJockey(world, difficulty, entityageable, (SpawnGroupData) null);
            } else {
                object = new AgeableMob.AgeableMobGroupData(0.5F);
            }

            return super.finalizeSpawn(world, difficulty, spawnReason, (SpawnGroupData) object, entityNbt);
        }
    }

    private SpawnGroupData spawnJockey(ServerLevelAccessor world, DifficultyInstance difficulty, Mob rider, @Nullable SpawnGroupData entityData) {
        rider.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
        rider.finalizeSpawn(world, difficulty, MobSpawnType.JOCKEY, entityData, (CompoundTag) null);
        rider.startRiding(this, true);
        return new AgeableMob.AgeableMobGroupData(0.0F);
    }

    private static class StriderGoToLavaGoal extends MoveToBlockGoal {

        private final Strider strider;

        StriderGoToLavaGoal(Strider strider, double speed) {
            super(strider, speed, 8, 2);
            this.strider = strider;
        }

        @Override
        public BlockPos getMoveToTarget() {
            return this.blockPos;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.strider.isInLava() && this.isValidTarget(this.strider.level, this.blockPos);
        }

        @Override
        public boolean canUse() {
            return !this.strider.isInLava() && super.canUse();
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 20 == 0;
        }

        @Override
        protected boolean isValidTarget(LevelReader world, BlockPos pos) {
            return world.getBlockState(pos).is(Blocks.LAVA) && world.getBlockState(pos.above()).isPathfindable(world, pos, PathComputationType.LAND);
        }
    }

    private static class StriderPathNavigation extends GroundPathNavigation {

        StriderPathNavigation(Strider entity, Level world) {
            super(entity, world);
        }

        @Override
        protected PathFinder createPathFinder(int range) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            return new PathFinder(this.nodeEvaluator, range);
        }

        @Override
        protected boolean hasValidPathType(BlockPathTypes pathType) {
            return pathType != BlockPathTypes.LAVA && pathType != BlockPathTypes.DAMAGE_FIRE && pathType != BlockPathTypes.DANGER_FIRE ? super.hasValidPathType(pathType) : true;
        }

        @Override
        public boolean isStableDestination(BlockPos pos) {
            return this.level.getBlockState(pos).is(Blocks.LAVA) || super.isStableDestination(pos);
        }
    }
}
