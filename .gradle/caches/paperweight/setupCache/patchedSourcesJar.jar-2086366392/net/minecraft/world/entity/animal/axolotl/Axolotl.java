package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.math.Vector3f;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class Axolotl extends Animal implements LerpingModel, Bucketable {

    // CraftBukkit start - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    @Override
    public int getDefaultMaxAirSupply() {
        return Axolotl.AXOLOTL_TOTAL_AIR_SUPPLY;
    }
    // CraftBukkit end
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int TOTAL_PLAYDEAD_TIME = 200;
    protected static final ImmutableList<? extends SensorType<? extends Sensor<? super Axolotl>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_ADULT, SensorType.HURT_BY, SensorType.AXOLOTL_ATTACKABLES, SensorType.AXOLOTL_TEMPTATIONS);
    // CraftBukkit - decompile error
    protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.<MemoryModuleType<?>>of(MemoryModuleType.BREED_TARGET, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.NEAREST_VISIBLE_ADULT, new MemoryModuleType[]{MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.PLAY_DEAD_TICKS, MemoryModuleType.NEAREST_ATTACKABLE, MemoryModuleType.TEMPTING_PLAYER, MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryModuleType.IS_TEMPTED, MemoryModuleType.HAS_HUNTING_COOLDOWN});
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_DEAD = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.BOOLEAN);
    public static final double PLAYER_REGEN_DETECTION_RANGE = 20.0D;
    public static final int RARE_VARIANT_CHANCE = 1200;
    private static final int AXOLOTL_TOTAL_AIR_SUPPLY = 6000;
    public static final String VARIANT_TAG = "Variant";
    private static final int REHYDRATE_AIR_SUPPLY = 1800;
    private static final int REGEN_BUFF_MAX_DURATION = 2400;
    private final Map<String, Vector3f> modelRotationValues = Maps.newHashMap();
    private static final int REGEN_BUFF_BASE_DURATION = 100;

    public Axolotl(EntityType<? extends Axolotl> type, Level world) {
        super(type, world);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.moveControl = new Axolotl.AxolotlMoveControl(this);
        this.lookControl = new Axolotl.AxolotlLookControl(this, 20);
        this.maxUpStep = 1.0F;
    }

    @Override
    public Map<String, Vector3f> getModelRotationValues() {
        return this.modelRotationValues;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return 0.0F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(Axolotl.DATA_VARIANT, 0);
        this.entityData.define(Axolotl.DATA_PLAYING_DEAD, false);
        this.entityData.define(Axolotl.FROM_BUCKET, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Variant", this.getVariant().getId());
        nbt.putBoolean("FromBucket", this.fromBucket());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setVariant(Axolotl.Variant.BY_ID[nbt.getInt("Variant")]);
        this.setFromBucket(nbt.getBoolean("FromBucket"));
    }

    @Override
    public void playAmbientSound() {
        if (!this.isPlayingDead()) {
            super.playAmbientSound();
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        boolean flag = false;

        if (spawnReason == MobSpawnType.BUCKET) {
            return (SpawnGroupData) entityData;
        } else {
            if (entityData instanceof Axolotl.AxolotlGroupData) {
                if (((Axolotl.AxolotlGroupData) entityData).getGroupSize() >= 2) {
                    flag = true;
                }
            } else {
                entityData = new Axolotl.AxolotlGroupData(new Axolotl.Variant[]{Axolotl.Variant.getCommonSpawnVariant(this.level.random), Axolotl.Variant.getCommonSpawnVariant(this.level.random)});
            }

            this.setVariant(((Axolotl.AxolotlGroupData) entityData).getVariant(this.level.random));
            if (flag) {
                this.setAge(-24000);
            }

            return super.finalizeSpawn(world, difficulty, spawnReason, (SpawnGroupData) entityData, entityNbt);
        }
    }

    @Override
    public void baseTick() {
        int i = this.getAirSupply();

        super.baseTick();
        if (!this.isNoAi()) {
            this.handleAirSupply(i);
        }

    }

    protected void handleAirSupply(int air) {
        if (this.isAlive() && !this.isInWaterRainOrBubble()) {
            this.setAirSupply(air - 1);
            if (this.getAirSupply() == -20) {
                this.setAirSupply(0);
                this.hurt(DamageSource.DRY_OUT, 2.0F);
            }
        } else {
            this.setAirSupply(this.getMaxAirSupply());
        }

    }

    public void rehydrate() {
        int i = this.getAirSupply() + 1800;

        this.setAirSupply(Math.min(i, this.getMaxAirSupply()));
    }

    @Override
    public int getMaxAirSupply() {
        return maxAirTicks; // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    }

    public Axolotl.Variant getVariant() {
        return Axolotl.Variant.BY_ID[(Integer) this.entityData.get(Axolotl.DATA_VARIANT)];
    }

    public void setVariant(Axolotl.Variant variant) {
        this.entityData.set(Axolotl.DATA_VARIANT, variant.getId());
    }

    private static boolean useRareVariant(Random random) {
        return random.nextInt(1200) == 0;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader world) {
        return world.isUnobstructed(this);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public MobType getMobType() {
        return MobType.WATER;
    }

    public void setPlayingDead(boolean playingDead) {
        this.entityData.set(Axolotl.DATA_PLAYING_DEAD, playingDead);
    }

    public boolean isPlayingDead() {
        return (Boolean) this.entityData.get(Axolotl.DATA_PLAYING_DEAD);
    }

    @Override
    public boolean fromBucket() {
        return (Boolean) this.entityData.get(Axolotl.FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(Axolotl.FROM_BUCKET, fromBucket);
        this.setPersistenceRequired(fromBucket || this.isPersistenceRequired()); // CraftBukkit - SPIGOT-4106 update persistence // Paper - actually set as persistent
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob entity) {
        Axolotl axolotl = (Axolotl) EntityType.AXOLOTL.create(world);

        if (axolotl != null) {
            Axolotl.Variant axolotl_variant;

            if (Axolotl.useRareVariant(this.random)) {
                axolotl_variant = Axolotl.Variant.getRareSpawnVariant(this.random);
            } else {
                axolotl_variant = this.random.nextBoolean() ? this.getVariant() : ((Axolotl) entity).getVariant();
            }

            axolotl.setVariant(axolotl_variant);
            axolotl.setPersistenceRequired();
        }

        return axolotl;
    }

    @Override
    public double getMeleeAttackRangeSqr(LivingEntity target) {
        return 1.5D + (double) target.getBbWidth() * 2.0D;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.AXOLOTL_TEMPT_ITEMS);
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        this.level.getProfiler().push("axolotlBrain");
        this.getBrain().tick((ServerLevel) this.level, this);
        this.level.getProfiler().pop();
        this.level.getProfiler().push("axolotlActivityUpdate");
        AxolotlAi.updateActivity(this);
        this.level.getProfiler().pop();
        if (!this.isNoAi()) {
            Optional<Integer> optional = this.getBrain().getMemory(MemoryModuleType.PLAY_DEAD_TICKS);

            this.setPlayingDead(optional.isPresent() && (Integer) optional.get() > 0);
        }

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0D).add(Attributes.MOVEMENT_SPEED, 1.0D).add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new Axolotl.AxolotlPathNavigation(this, world);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean flag = target.hurt(DamageSource.mobAttack(this), (float) ((int) this.getAttributeValue(Attributes.ATTACK_DAMAGE)));

        if (flag) {
            this.doEnchantDamageEffects(this, target);
            this.playSound(SoundEvents.AXOLOTL_ATTACK, 1.0F, 1.0F);
        }

        return flag;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float f1 = this.getHealth();

        if (!this.level.isClientSide && !this.isNoAi() && this.level.random.nextInt(3) == 0 && ((float) this.level.random.nextInt(3) < amount || f1 / this.getMaxHealth() < 0.5F) && amount < f1 && this.isInWater() && (source.getEntity() != null || source.getDirectEntity() != null) && !this.isPlayingDead()) {
            this.brain.setMemory(MemoryModuleType.PLAY_DEAD_TICKS, (int) 200);
        }

        return super.hurt(source, amount);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.655F;
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
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return (InteractionResult) Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        Bucketable.saveDefaultDataToBucketTag(this, stack);
        CompoundTag nbttagcompound = stack.getOrCreateTag();

        nbttagcompound.putInt("Variant", this.getVariant().getId());
        nbttagcompound.putInt("Age", this.getAge());
        Brain<?> behaviorcontroller = this.getBrain();

        if (behaviorcontroller.hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN)) {
            nbttagcompound.putLong("HuntingCooldown", behaviorcontroller.getTimeUntilExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN));
        }

    }

    @Override
    public void loadFromBucketTag(CompoundTag nbt) {
        Bucketable.loadDefaultDataFromBucketTag(this, nbt);
        int i = nbt.getInt("Variant");

        if (i >= 0 && i < Axolotl.Variant.BY_ID.length) {
            this.setVariant(Axolotl.Variant.BY_ID[i]);
        } else {
            Axolotl.LOGGER.error("Invalid variant: {}", i);
        }

        if (nbt.contains("Age")) {
            this.setAge(nbt.getInt("Age"));
        }

        if (nbt.contains("HuntingCooldown")) {
            this.getBrain().setMemoryWithExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN, true, nbt.getLong("HuntingCooldown"));
        }

    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.AXOLOTL_BUCKET);
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_AXOLOTL;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.isPlayingDead() && super.canBeSeenAsEnemy();
    }

    public static void onStopAttacking(Axolotl axolotl) {
        Optional<LivingEntity> optional = axolotl.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);

        if (optional.isPresent()) {
            Level world = axolotl.level;
            LivingEntity entityliving = (LivingEntity) optional.get();

            if (entityliving.isDeadOrDying()) {
                DamageSource damagesource = entityliving.getLastDamageSource();

                if (damagesource != null) {
                    Entity entity = damagesource.getEntity();

                    if (entity != null && entity.getType() == EntityType.PLAYER) {
                        Player entityhuman = (Player) entity;
                        List<Player> list = world.getEntitiesOfClass(Player.class, axolotl.getBoundingBox().inflate(20.0D));

                        if (list.contains(entityhuman)) {
                            axolotl.applySupportingEffects(entityhuman);
                        }
                    }
                }
            }

        }
    }

    public void applySupportingEffects(Player player) {
        MobEffectInstance mobeffect = player.getEffect(MobEffects.REGENERATION);
        int i = mobeffect != null ? mobeffect.getDuration() : 0;

        if (i < 2400) {
            i = Math.min(2400, 100 + i);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, i, 0), this, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.AXOLOTL); // CraftBukkit
        }

        player.removeEffect(MobEffects.DIG_SLOWDOWN);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.fromBucket();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Nullable
    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.AXOLOTL_IDLE_WATER : SoundEvents.AXOLOTL_IDLE_AIR;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.AXOLOTL_SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }

    @Override
    protected Brain.Provider<Axolotl> brainProvider() {
        return Brain.provider(Axolotl.MEMORY_TYPES, Axolotl.SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return AxolotlAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public Brain<Axolotl> getBrain() {
        return (Brain<Axolotl>) super.getBrain(); // CraftBukkit - decompile error
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    public void travel(Vec3 movementInput) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), movementInput);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(movementInput);
        }

    }

    @Override
    protected void usePlayerItem(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.TROPICAL_FISH_BUCKET)) {
            player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
        } else {
            super.usePlayerItem(player, hand, stack);
        }

    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return true; // CraftBukkit
    }

    public static boolean checkAxolotlSpawnRules(EntityType<? extends LivingEntity> type, ServerLevelAccessor world, MobSpawnType reason, BlockPos pos, Random random) {
        return world.getBlockState(pos.below()).is(BlockTags.AXOLOTLS_SPAWNABLE_ON);
    }

    private static class AxolotlMoveControl extends SmoothSwimmingMoveControl {

        private final Axolotl axolotl;

        public AxolotlMoveControl(Axolotl axolotl) {
            super(axolotl, 85, 10, 0.1F, 0.5F, false);
            this.axolotl = axolotl;
        }

        @Override
        public void tick() {
            if (!this.axolotl.isPlayingDead()) {
                super.tick();
            }

        }
    }

    private class AxolotlLookControl extends SmoothSwimmingLookControl {

        public AxolotlLookControl(Axolotl axolotl, int i) {
            super(axolotl, i);
        }

        @Override
        public void tick() {
            if (!Axolotl.this.isPlayingDead()) {
                super.tick();
            }

        }
    }

    public static enum Variant {

        LUCY(0, "lucy", true), WILD(1, "wild", true), GOLD(2, "gold", true), CYAN(3, "cyan", true), BLUE(4, "blue", false);

        public static final Axolotl.Variant[] BY_ID = (Axolotl.Variant[]) Arrays.stream(values()).sorted(Comparator.comparingInt(Axolotl.Variant::getId)).toArray((i) -> {
            return new Axolotl.Variant[i];
        });
        private final int id;
        private final String name;
        private final boolean common;

        private Variant(int i, String s, boolean flag) {
            this.id = i;
            this.name = s;
            this.common = flag;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public static Axolotl.Variant getCommonSpawnVariant(Random random) {
            return getSpawnVariant(random, true);
        }

        public static Axolotl.Variant getRareSpawnVariant(Random random) {
            return getSpawnVariant(random, false);
        }

        private static Axolotl.Variant getSpawnVariant(Random random, boolean natural) {
            Axolotl.Variant[] aaxolotl_variant = (Axolotl.Variant[]) Arrays.stream(Axolotl.Variant.BY_ID).filter((axolotl_variant) -> {
                return axolotl_variant.common == natural;
            }).toArray((i) -> {
                return new Axolotl.Variant[i];
            });

            return (Axolotl.Variant) Util.getRandom((Object[]) aaxolotl_variant, random);
        }
    }

    public static class AxolotlGroupData extends AgeableMob.AgeableMobGroupData {

        public final Axolotl.Variant[] types;

        public AxolotlGroupData(Axolotl.Variant... variants) {
            super(false);
            this.types = variants;
        }

        public Axolotl.Variant getVariant(Random random) {
            return this.types[random.nextInt(this.types.length)];
        }
    }

    private static class AxolotlPathNavigation extends WaterBoundPathNavigation {

        AxolotlPathNavigation(Axolotl axolotl, Level world) {
            super(axolotl, world);
        }

        @Override
        protected boolean canUpdatePath() {
            return true;
        }

        @Override
        protected PathFinder createPathFinder(int range) {
            this.nodeEvaluator = new AmphibiousNodeEvaluator(false);
            return new PathFinder(this.nodeEvaluator, range);
        }

        @Override
        public boolean isStableDestination(BlockPos pos) {
            return !this.level.getBlockState(pos.below()).isAir();
        }
    }
}
