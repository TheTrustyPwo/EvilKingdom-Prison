package net.minecraft.world.entity;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.slf4j.Logger;

public abstract class LivingEntity extends Entity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID SPEED_MODIFIER_SPRINTING_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    private static final UUID SPEED_MODIFIER_SOUL_SPEED_UUID = UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038");
    private static final UUID SPEED_MODIFIER_POWDER_SNOW_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
    private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(SPEED_MODIFIER_SPRINTING_UUID, "Sprinting speed boost", (double)0.3F, AttributeModifier.Operation.MULTIPLY_TOTAL);
    public static final int HAND_SLOTS = 2;
    public static final int ARMOR_SLOTS = 4;
    public static final int EQUIPMENT_SLOT_OFFSET = 98;
    public static final int ARMOR_SLOT_OFFSET = 100;
    public static final int SWING_DURATION = 6;
    public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
    private static final int DAMAGE_SOURCE_TIMEOUT = 40;
    public static final double MIN_MOVEMENT_DISTANCE = 0.003D;
    public static final double DEFAULT_BASE_GRAVITY = 0.08D;
    public static final int DEATH_DURATION = 20;
    private static final int WAIT_TICKS_BEFORE_ITEM_USE_EFFECTS = 7;
    private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
    private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
    public static final int USE_ITEM_INTERVAL = 4;
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0D;
    protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
    protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
    protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
    protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_EFFECT_COLOR_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    protected static final float DEFAULT_EYE_HEIGHT = 1.74F;
    protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F);
    public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
    private final AttributeMap attributes;
    public CombatTracker combatTracker = new CombatTracker(this);
    public final Map<MobEffect, MobEffectInstance> activeEffects = Maps.newHashMap();
    private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);
    public boolean swinging;
    private boolean discardFriction = false;
    public InteractionHand swingingArm;
    public int swingTime;
    public int removeArrowTime;
    public int removeStingerTime;
    public int hurtTime;
    public int hurtDuration;
    public float hurtDir;
    public int deathTime;
    public float oAttackAnim;
    public float attackAnim;
    protected int attackStrengthTicker;
    public float animationSpeedOld;
    public float animationSpeed;
    public float animationPosition;
    public int invulnerableDuration = 20;
    public final float timeOffs;
    public final float rotA;
    public float yBodyRot;
    public float yBodyRotO;
    public float yHeadRot;
    public float yHeadRotO;
    public float flyingSpeed = 0.02F;
    @Nullable
    public Player lastHurtByPlayer;
    public int lastHurtByPlayerTime;
    protected boolean dead;
    protected int noActionTime;
    protected float oRun;
    protected float run;
    protected float animStep;
    protected float animStepO;
    protected float rotOffs;
    protected int deathScore;
    public float lastHurt;
    public boolean jumping;
    public float xxa;
    public float yya;
    public float zza;
    protected int lerpSteps;
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected double lerpYRot;
    protected double lerpXRot;
    protected double lyHeadRot;
    protected int lerpHeadSteps;
    public boolean effectsDirty = true;
    @Nullable
    public LivingEntity lastHurtByMob;
    public int lastHurtByMobTimestamp;
    private LivingEntity lastHurtMob;
    private int lastHurtMobTimestamp;
    private float speed;
    private int noJumpDelay;
    private float absorptionAmount;
    protected ItemStack useItem = ItemStack.EMPTY;
    protected int useItemRemaining;
    protected int fallFlyTicks;
    private BlockPos lastPos;
    private Optional<BlockPos> lastClimbablePos = Optional.empty();
    @Nullable
    private DamageSource lastDamageSource;
    private long lastDamageStamp;
    protected int autoSpinAttackTicks;
    private float swimAmount;
    private float swimAmountO;
    protected Brain<?> brain;

    protected LivingEntity(EntityType<? extends LivingEntity> type, Level world) {
        super(type, world);
        this.attributes = new AttributeMap(DefaultAttributes.getSupplier(type));
        this.setHealth(this.getMaxHealth());
        this.blocksBuilding = true;
        this.rotA = (float)((Math.random() + 1.0D) * (double)0.01F);
        this.reapplyPosition();
        this.timeOffs = (float)Math.random() * 12398.0F;
        this.setYRot((float)(Math.random() * (double)((float)Math.PI * 2F)));
        this.yHeadRot = this.getYRot();
        this.maxUpStep = 0.6F;
        NbtOps nbtOps = NbtOps.INSTANCE;
        this.brain = this.makeBrain(new Dynamic<>(nbtOps, nbtOps.createMap(ImmutableMap.of(nbtOps.createString("memories"), nbtOps.emptyMap()))));
    }

    public Brain<?> getBrain() {
        return this.brain;
    }

    protected Brain.Provider<?> brainProvider() {
        return Brain.provider(ImmutableList.of(), ImmutableList.of());
    }

    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return this.brainProvider().makeBrain(dynamic);
    }

    @Override
    public void kill() {
        this.hurt(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
    }

    public boolean canAttackType(EntityType<?> type) {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
        this.entityData.define(DATA_EFFECT_COLOR_ID, 0);
        this.entityData.define(DATA_EFFECT_AMBIENCE_ID, false);
        this.entityData.define(DATA_ARROW_COUNT_ID, 0);
        this.entityData.define(DATA_STINGER_COUNT_ID, 0);
        this.entityData.define(DATA_HEALTH_ID, 1.0F);
        this.entityData.define(SLEEPING_POS_ID, Optional.empty());
    }

    public static AttributeSupplier.Builder createLivingAttributes() {
        return AttributeSupplier.builder().add(Attributes.MAX_HEALTH).add(Attributes.KNOCKBACK_RESISTANCE).add(Attributes.MOVEMENT_SPEED).add(Attributes.ARMOR).add(Attributes.ARMOR_TOUGHNESS);
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
        if (!this.isInWater()) {
            this.updateInWaterStateAndDoWaterCurrentPushing();
        }

        if (!this.level.isClientSide && onGround && this.fallDistance > 0.0F) {
            this.removeSoulSpeed();
            this.tryAddSoulSpeed();
        }

        if (!this.level.isClientSide && this.fallDistance > 3.0F && onGround) {
            float f = (float)Mth.ceil(this.fallDistance - 3.0F);
            if (!landedState.isAir()) {
                double d = Math.min((double)(0.2F + f / 15.0F), 2.5D);
                int i = (int)(150.0D * d);
                ((ServerLevel)this.level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, landedState), this.getX(), this.getY(), this.getZ(), i, 0.0D, 0.0D, 0.0D, (double)0.15F);
            }
        }

        super.checkFallDamage(heightDifference, onGround, landedState, landedPosition);
    }

    public boolean canBreatheUnderwater() {
        return this.getMobType() == MobType.UNDEAD;
    }

    public float getSwimAmount(float tickDelta) {
        return Mth.lerp(tickDelta, this.swimAmountO, this.swimAmount);
    }

    @Override
    public void baseTick() {
        this.oAttackAnim = this.attackAnim;
        if (this.firstTick) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
        }

        if (this.canSpawnSoulSpeedParticle()) {
            this.spawnSoulSpeedParticle();
        }

        super.baseTick();
        this.level.getProfiler().push("livingEntityBaseTick");
        if (this.fireImmune() || this.level.isClientSide) {
            this.clearFire();
        }

        if (this.isAlive()) {
            boolean bl = this instanceof Player;
            if (this.isInWall()) {
                this.hurt(DamageSource.IN_WALL, 1.0F);
            } else if (bl && !this.level.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
                double d = this.level.getWorldBorder().getDistanceToBorder(this) + this.level.getWorldBorder().getDamageSafeZone();
                if (d < 0.0D) {
                    double e = this.level.getWorldBorder().getDamagePerBlock();
                    if (e > 0.0D) {
                        this.hurt(DamageSource.IN_WALL, (float)Math.max(1, Mth.floor(-d * e)));
                    }
                }
            }

            if (this.isEyeInFluid(FluidTags.WATER) && !this.level.getBlockState(new BlockPos(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
                boolean bl2 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!bl || !((Player)this).getAbilities().invulnerable);
                if (bl2) {
                    this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
                    if (this.getAirSupply() == -20) {
                        this.setAirSupply(0);
                        Vec3 vec3 = this.getDeltaMovement();

                        for(int i = 0; i < 8; ++i) {
                            double f = this.random.nextDouble() - this.random.nextDouble();
                            double g = this.random.nextDouble() - this.random.nextDouble();
                            double h = this.random.nextDouble() - this.random.nextDouble();
                            this.level.addParticle(ParticleTypes.BUBBLE, this.getX() + f, this.getY() + g, this.getZ() + h, vec3.x, vec3.y, vec3.z);
                        }

                        this.hurt(DamageSource.DROWN, 2.0F);
                    }
                }

                if (!this.level.isClientSide && this.isPassenger() && this.getVehicle() != null && !this.getVehicle().rideableUnderWater()) {
                    this.stopRiding();
                }
            } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }

            if (!this.level.isClientSide) {
                BlockPos blockPos = this.blockPosition();
                if (!Objects.equal(this.lastPos, blockPos)) {
                    this.lastPos = blockPos;
                    this.onChangedBlock(blockPos);
                }
            }
        }

        if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow)) {
            if (!this.level.isClientSide && this.wasOnFire) {
                this.playEntityOnFireExtinguishedSound();
            }

            this.clearFire();
        }

        if (this.hurtTime > 0) {
            --this.hurtTime;
        }

        if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
            --this.invulnerableTime;
        }

        if (this.isDeadOrDying() && this.level.shouldTickDeath(this)) {
            this.tickDeath();
        }

        if (this.lastHurtByPlayerTime > 0) {
            --this.lastHurtByPlayerTime;
        } else {
            this.lastHurtByPlayer = null;
        }

        if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
            this.lastHurtMob = null;
        }

        if (this.lastHurtByMob != null) {
            if (!this.lastHurtByMob.isAlive()) {
                this.setLastHurtByMob((LivingEntity)null);
            } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
                this.setLastHurtByMob((LivingEntity)null);
            }
        }

        this.tickEffects();
        this.animStepO = this.animStep;
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.level.getProfiler().pop();
    }

    public boolean canSpawnSoulSpeedParticle() {
        return this.tickCount % 5 == 0 && this.getDeltaMovement().x != 0.0D && this.getDeltaMovement().z != 0.0D && !this.isSpectator() && EnchantmentHelper.hasSoulSpeed(this) && this.onSoulSpeedBlock();
    }

    protected void spawnSoulSpeedParticle() {
        Vec3 vec3 = this.getDeltaMovement();
        this.level.addParticle(ParticleTypes.SOUL, this.getX() + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth(), this.getY() + 0.1D, this.getZ() + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth(), vec3.x * -0.2D, 0.1D, vec3.z * -0.2D);
        float f = this.random.nextFloat() * 0.4F + this.random.nextFloat() > 0.9F ? 0.6F : 0.0F;
        this.playSound(SoundEvents.SOUL_ESCAPE, f, 0.6F + this.random.nextFloat() * 0.4F);
    }

    protected boolean onSoulSpeedBlock() {
        return this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).is(BlockTags.SOUL_SPEED_BLOCKS);
    }

    @Override
    protected float getBlockSpeedFactor() {
        return this.onSoulSpeedBlock() && EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, this) > 0 ? 1.0F : super.getBlockSpeedFactor();
    }

    protected boolean shouldRemoveSoulSpeed(BlockState landingState) {
        return !landingState.isAir() || this.isFallFlying();
    }

    protected void removeSoulSpeed() {
        AttributeInstance attributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeInstance != null) {
            if (attributeInstance.getModifier(SPEED_MODIFIER_SOUL_SPEED_UUID) != null) {
                attributeInstance.removeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID);
            }

        }
    }

    protected void tryAddSoulSpeed() {
        if (!this.getBlockStateOn().isAir()) {
            int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, this);
            if (i > 0 && this.onSoulSpeedBlock()) {
                AttributeInstance attributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attributeInstance == null) {
                    return;
                }

                attributeInstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID, "Soul speed boost", (double)(0.03F * (1.0F + (float)i * 0.35F)), AttributeModifier.Operation.ADDITION));
                if (this.getRandom().nextFloat() < 0.04F) {
                    ItemStack itemStack = this.getItemBySlot(EquipmentSlot.FEET);
                    itemStack.hurtAndBreak(1, this, (player) -> {
                        player.broadcastBreakEvent(EquipmentSlot.FEET);
                    });
                }
            }
        }

    }

    protected void removeFrost() {
        AttributeInstance attributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeInstance != null) {
            if (attributeInstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_UUID) != null) {
                attributeInstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID);
            }

        }
    }

    protected void tryAddFrost() {
        if (!this.getBlockStateOn().isAir()) {
            int i = this.getTicksFrozen();
            if (i > 0) {
                AttributeInstance attributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attributeInstance == null) {
                    return;
                }

                float f = -0.05F * this.getPercentFrozen();
                attributeInstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID, "Powder snow slow", (double)f, AttributeModifier.Operation.ADDITION));
            }
        }

    }

    protected void onChangedBlock(BlockPos pos) {
        int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, this);
        if (i > 0) {
            FrostWalkerEnchantment.onEntityMoved(this, this.level, pos, i);
        }

        if (this.shouldRemoveSoulSpeed(this.getBlockStateOn())) {
            this.removeSoulSpeed();
        }

        this.tryAddSoulSpeed();
    }

    public boolean isBaby() {
        return false;
    }

    public float getScale() {
        return this.isBaby() ? 0.5F : 1.0F;
    }

    protected boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean rideableUnderWater() {
        return false;
    }

    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20 && !this.level.isClientSide()) {
            this.level.broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }

    }

    protected boolean shouldDropExperience() {
        return !this.isBaby();
    }

    protected boolean shouldDropLoot() {
        return !this.isBaby();
    }

    protected int decreaseAirSupply(int air) {
        int i = EnchantmentHelper.getRespiration(this);
        return i > 0 && this.random.nextInt(i + 1) > 0 ? air : air - 1;
    }

    protected int increaseAirSupply(int air) {
        return Math.min(air + 4, this.getMaxAirSupply());
    }

    protected int getExperienceReward(Player player) {
        return 0;
    }

    protected boolean isAlwaysExperienceDropper() {
        return false;
    }

    public Random getRandom() {
        return this.random;
    }

    @Nullable
    public LivingEntity getLastHurtByMob() {
        return this.lastHurtByMob;
    }

    public int getLastHurtByMobTimestamp() {
        return this.lastHurtByMobTimestamp;
    }

    public void setLastHurtByPlayer(@Nullable Player attacking) {
        this.lastHurtByPlayer = attacking;
        this.lastHurtByPlayerTime = this.tickCount;
    }

    public void setLastHurtByMob(@Nullable LivingEntity attacker) {
        this.lastHurtByMob = attacker;
        this.lastHurtByMobTimestamp = this.tickCount;
    }

    @Nullable
    public LivingEntity getLastHurtMob() {
        return this.lastHurtMob;
    }

    public int getLastHurtMobTimestamp() {
        return this.lastHurtMobTimestamp;
    }

    public void setLastHurtMob(Entity target) {
        if (target instanceof LivingEntity) {
            this.lastHurtMob = (LivingEntity)target;
        } else {
            this.lastHurtMob = null;
        }

        this.lastHurtMobTimestamp = this.tickCount;
    }

    public int getNoActionTime() {
        return this.noActionTime;
    }

    public void setNoActionTime(int despawnCounter) {
        this.noActionTime = despawnCounter;
    }

    public boolean shouldDiscardFriction() {
        return this.discardFriction;
    }

    public void setDiscardFriction(boolean noDrag) {
        this.discardFriction = noDrag;
    }

    protected void equipEventAndSound(ItemStack stack) {
        SoundEvent soundEvent = stack.getEquipSound();
        if (!stack.isEmpty() && soundEvent != null && !this.isSpectator()) {
            this.gameEvent(GameEvent.EQUIP);
            this.playSound(soundEvent, 1.0F, 1.0F);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putFloat("Health", this.getHealth());
        nbt.putShort("HurtTime", (short)this.hurtTime);
        nbt.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
        nbt.putShort("DeathTime", (short)this.deathTime);
        nbt.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
        nbt.put("Attributes", this.getAttributes().save());
        if (!this.activeEffects.isEmpty()) {
            ListTag listTag = new ListTag();

            for(MobEffectInstance mobEffectInstance : this.activeEffects.values()) {
                listTag.add(mobEffectInstance.save(new CompoundTag()));
            }

            nbt.put("ActiveEffects", listTag);
        }

        nbt.putBoolean("FallFlying", this.isFallFlying());
        this.getSleepingPos().ifPresent((pos) -> {
            nbt.putInt("SleepingX", pos.getX());
            nbt.putInt("SleepingY", pos.getY());
            nbt.putInt("SleepingZ", pos.getZ());
        });
        DataResult<Tag> dataResult = this.brain.serializeStart(NbtOps.INSTANCE);
        dataResult.resultOrPartial(LOGGER::error).ifPresent((brain) -> {
            nbt.put("Brain", brain);
        });
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        this.setAbsorptionAmount(nbt.getFloat("AbsorptionAmount"));
        if (nbt.contains("Attributes", 9) && this.level != null && !this.level.isClientSide) {
            this.getAttributes().load(nbt.getList("Attributes", 10));
        }

        if (nbt.contains("ActiveEffects", 9)) {
            ListTag listTag = nbt.getList("ActiveEffects", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                CompoundTag compoundTag = listTag.getCompound(i);
                MobEffectInstance mobEffectInstance = MobEffectInstance.load(compoundTag);
                if (mobEffectInstance != null) {
                    this.activeEffects.put(mobEffectInstance.getEffect(), mobEffectInstance);
                }
            }
        }

        if (nbt.contains("Health", 99)) {
            this.setHealth(nbt.getFloat("Health"));
        }

        this.hurtTime = nbt.getShort("HurtTime");
        this.deathTime = nbt.getShort("DeathTime");
        this.lastHurtByMobTimestamp = nbt.getInt("HurtByTimestamp");
        if (nbt.contains("Team", 8)) {
            String string = nbt.getString("Team");
            PlayerTeam playerTeam = this.level.getScoreboard().getPlayerTeam(string);
            boolean bl = playerTeam != null && this.level.getScoreboard().addPlayerToTeam(this.getStringUUID(), playerTeam);
            if (!bl) {
                LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", (Object)string);
            }
        }

        if (nbt.getBoolean("FallFlying")) {
            this.setSharedFlag(7, true);
        }

        if (nbt.contains("SleepingX", 99) && nbt.contains("SleepingY", 99) && nbt.contains("SleepingZ", 99)) {
            BlockPos blockPos = new BlockPos(nbt.getInt("SleepingX"), nbt.getInt("SleepingY"), nbt.getInt("SleepingZ"));
            this.setSleepingPos(blockPos);
            this.entityData.set(DATA_POSE, Pose.SLEEPING);
            if (!this.firstTick) {
                this.setPosToBed(blockPos);
            }
        }

        if (nbt.contains("Brain", 10)) {
            this.brain = this.makeBrain(new Dynamic<>(NbtOps.INSTANCE, nbt.get("Brain")));
        }

    }

    protected void tickEffects() {
        Iterator<MobEffect> iterator = this.activeEffects.keySet().iterator();

        try {
            while(iterator.hasNext()) {
                MobEffect mobEffect = iterator.next();
                MobEffectInstance mobEffectInstance = this.activeEffects.get(mobEffect);
                if (!mobEffectInstance.tick(this, () -> {
                    this.onEffectUpdated(mobEffectInstance, true, (Entity)null);
                })) {
                    if (!this.level.isClientSide) {
                        iterator.remove();
                        this.onEffectRemoved(mobEffectInstance);
                    }
                } else if (mobEffectInstance.getDuration() % 600 == 0) {
                    this.onEffectUpdated(mobEffectInstance, false, (Entity)null);
                }
            }
        } catch (ConcurrentModificationException var11) {
        }

        if (this.effectsDirty) {
            if (!this.level.isClientSide) {
                this.updateInvisibilityStatus();
                this.updateGlowingStatus();
            }

            this.effectsDirty = false;
        }

        int i = this.entityData.get(DATA_EFFECT_COLOR_ID);
        boolean bl = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
        if (i > 0) {
            boolean bl2;
            if (this.isInvisible()) {
                bl2 = this.random.nextInt(15) == 0;
            } else {
                bl2 = this.random.nextBoolean();
            }

            if (bl) {
                bl2 &= this.random.nextInt(5) == 0;
            }

            if (bl2 && i > 0) {
                double d = (double)(i >> 16 & 255) / 255.0D;
                double e = (double)(i >> 8 & 255) / 255.0D;
                double f = (double)(i >> 0 & 255) / 255.0D;
                this.level.addParticle(bl ? ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), d, e, f);
            }
        }

    }

    protected void updateInvisibilityStatus() {
        if (this.activeEffects.isEmpty()) {
            this.removeEffectParticles();
            this.setInvisible(false);
        } else {
            Collection<MobEffectInstance> collection = this.activeEffects.values();
            this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(collection));
            this.entityData.set(DATA_EFFECT_COLOR_ID, PotionUtils.getColor(collection));
            this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
        }

    }

    private void updateGlowingStatus() {
        boolean bl = this.isCurrentlyGlowing();
        if (this.getSharedFlag(6) != bl) {
            this.setSharedFlag(6, bl);
        }

    }

    public double getVisibilityPercent(@Nullable Entity entity) {
        double d = 1.0D;
        if (this.isDiscrete()) {
            d *= 0.8D;
        }

        if (this.isInvisible()) {
            float f = this.getArmorCoverPercentage();
            if (f < 0.1F) {
                f = 0.1F;
            }

            d *= 0.7D * (double)f;
        }

        if (entity != null) {
            ItemStack itemStack = this.getItemBySlot(EquipmentSlot.HEAD);
            EntityType<?> entityType = entity.getType();
            if (entityType == EntityType.SKELETON && itemStack.is(Items.SKELETON_SKULL) || entityType == EntityType.ZOMBIE && itemStack.is(Items.ZOMBIE_HEAD) || entityType == EntityType.CREEPER && itemStack.is(Items.CREEPER_HEAD)) {
                d *= 0.5D;
            }
        }

        return d;
    }

    public boolean canAttack(LivingEntity target) {
        return target instanceof Player && this.level.getDifficulty() == Difficulty.PEACEFUL ? false : target.canBeSeenAsEnemy();
    }

    public boolean canAttack(LivingEntity entity, TargetingConditions predicate) {
        return predicate.test(this, entity);
    }

    public boolean canBeSeenAsEnemy() {
        return !this.isInvulnerable() && this.canBeSeenByAnyone();
    }

    public boolean canBeSeenByAnyone() {
        return !this.isSpectator() && this.isAlive();
    }

    public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> effects) {
        for(MobEffectInstance mobEffectInstance : effects) {
            if (mobEffectInstance.isVisible() && !mobEffectInstance.isAmbient()) {
                return false;
            }
        }

        return true;
    }

    protected void removeEffectParticles() {
        this.entityData.set(DATA_EFFECT_AMBIENCE_ID, false);
        this.entityData.set(DATA_EFFECT_COLOR_ID, 0);
    }

    public boolean removeAllEffects() {
        if (this.level.isClientSide) {
            return false;
        } else {
            Iterator<MobEffectInstance> iterator = this.activeEffects.values().iterator();

            boolean bl;
            for(bl = false; iterator.hasNext(); bl = true) {
                this.onEffectRemoved(iterator.next());
                iterator.remove();
            }

            return bl;
        }
    }

    public Collection<MobEffectInstance> getActiveEffects() {
        return this.activeEffects.values();
    }

    public Map<MobEffect, MobEffectInstance> getActiveEffectsMap() {
        return this.activeEffects;
    }

    public boolean hasEffect(MobEffect effect) {
        return this.activeEffects.containsKey(effect);
    }

    @Nullable
    public MobEffectInstance getEffect(MobEffect effect) {
        return this.activeEffects.get(effect);
    }

    public final boolean addEffect(MobEffectInstance effect) {
        return this.addEffect(effect, (Entity)null);
    }

    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        if (!this.canBeAffected(effect)) {
            return false;
        } else {
            MobEffectInstance mobEffectInstance = this.activeEffects.get(effect.getEffect());
            if (mobEffectInstance == null) {
                this.activeEffects.put(effect.getEffect(), effect);
                this.onEffectAdded(effect, source);
                return true;
            } else if (mobEffectInstance.update(effect)) {
                this.onEffectUpdated(mobEffectInstance, true, source);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean canBeAffected(MobEffectInstance effect) {
        if (this.getMobType() == MobType.UNDEAD) {
            MobEffect mobEffect = effect.getEffect();
            if (mobEffect == MobEffects.REGENERATION || mobEffect == MobEffects.POISON) {
                return false;
            }
        }

        return true;
    }

    public void forceAddEffect(MobEffectInstance effect, @Nullable Entity source) {
        if (this.canBeAffected(effect)) {
            MobEffectInstance mobEffectInstance = this.activeEffects.put(effect.getEffect(), effect);
            if (mobEffectInstance == null) {
                this.onEffectAdded(effect, source);
            } else {
                this.onEffectUpdated(effect, true, source);
            }

        }
    }

    public boolean isInvertedHealAndHarm() {
        return this.getMobType() == MobType.UNDEAD;
    }

    @Nullable
    public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect type) {
        return this.activeEffects.remove(type);
    }

    public boolean removeEffect(MobEffect type) {
        MobEffectInstance mobEffectInstance = this.removeEffectNoUpdate(type);
        if (mobEffectInstance != null) {
            this.onEffectRemoved(mobEffectInstance);
            return true;
        } else {
            return false;
        }
    }

    protected void onEffectAdded(MobEffectInstance effect, @Nullable Entity source) {
        this.effectsDirty = true;
        if (!this.level.isClientSide) {
            effect.getEffect().addAttributeModifiers(this, this.getAttributes(), effect.getAmplifier());
        }

    }

    protected void onEffectUpdated(MobEffectInstance effect, boolean reapplyEffect, @Nullable Entity source) {
        this.effectsDirty = true;
        if (reapplyEffect && !this.level.isClientSide) {
            MobEffect mobEffect = effect.getEffect();
            mobEffect.removeAttributeModifiers(this, this.getAttributes(), effect.getAmplifier());
            mobEffect.addAttributeModifiers(this, this.getAttributes(), effect.getAmplifier());
        }

    }

    protected void onEffectRemoved(MobEffectInstance effect) {
        this.effectsDirty = true;
        if (!this.level.isClientSide) {
            effect.getEffect().removeAttributeModifiers(this, this.getAttributes(), effect.getAmplifier());
        }

    }

    public void heal(float amount) {
        float f = this.getHealth();
        if (f > 0.0F) {
            this.setHealth(f + amount);
        }

    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH_ID);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH_ID, Mth.clamp(health, 0.0F, this.getMaxHealth()));
    }

    public boolean isDeadOrDying() {
        return this.getHealth() <= 0.0F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.level.isClientSide) {
            return false;
        } else if (this.isDeadOrDying()) {
            return false;
        } else if (source.isFire() && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        } else {
            if (this.isSleeping() && !this.level.isClientSide) {
                this.stopSleeping();
            }

            this.noActionTime = 0;
            float f = amount;
            boolean bl = false;
            float g = 0.0F;
            if (amount > 0.0F && this.isDamageSourceBlocked(source)) {
                this.hurtCurrentlyUsedShield(amount);
                g = amount;
                amount = 0.0F;
                if (!source.isProjectile()) {
                    Entity entity = source.getDirectEntity();
                    if (entity instanceof LivingEntity) {
                        this.blockUsingShield((LivingEntity)entity);
                    }
                }

                bl = true;
            }

            this.animationSpeed = 1.5F;
            boolean bl2 = true;
            if ((float)this.invulnerableTime > 10.0F) {
                if (amount <= this.lastHurt) {
                    return false;
                }

                this.actuallyHurt(source, amount - this.lastHurt);
                this.lastHurt = amount;
                bl2 = false;
            } else {
                this.lastHurt = amount;
                this.invulnerableTime = 20;
                this.actuallyHurt(source, amount);
                this.hurtDuration = 10;
                this.hurtTime = this.hurtDuration;
            }

            if (source.isDamageHelmet() && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                this.hurtHelmet(source, amount);
                amount *= 0.75F;
            }

            this.hurtDir = 0.0F;
            Entity entity2 = source.getEntity();
            if (entity2 != null) {
                if (entity2 instanceof LivingEntity && !source.isNoAggro()) {
                    this.setLastHurtByMob((LivingEntity)entity2);
                }

                if (entity2 instanceof Player) {
                    this.lastHurtByPlayerTime = 100;
                    this.lastHurtByPlayer = (Player)entity2;
                } else if (entity2 instanceof Wolf) {
                    Wolf wolf = (Wolf)entity2;
                    if (wolf.isTame()) {
                        this.lastHurtByPlayerTime = 100;
                        LivingEntity livingEntity = wolf.getOwner();
                        if (livingEntity != null && livingEntity.getType() == EntityType.PLAYER) {
                            this.lastHurtByPlayer = (Player)livingEntity;
                        } else {
                            this.lastHurtByPlayer = null;
                        }
                    }
                }
            }

            if (bl2) {
                if (bl) {
                    this.level.broadcastEntityEvent(this, (byte)29);
                } else if (source instanceof EntityDamageSource && ((EntityDamageSource)source).isThorns()) {
                    this.level.broadcastEntityEvent(this, (byte)33);
                } else {
                    byte b;
                    if (source == DamageSource.DROWN) {
                        b = 36;
                    } else if (source.isFire()) {
                        b = 37;
                    } else if (source == DamageSource.SWEET_BERRY_BUSH) {
                        b = 44;
                    } else if (source == DamageSource.FREEZE) {
                        b = 57;
                    } else {
                        b = 2;
                    }

                    this.level.broadcastEntityEvent(this, b);
                }

                if (source != DamageSource.DROWN && (!bl || amount > 0.0F)) {
                    this.markHurt();
                }

                if (entity2 != null) {
                    double i = entity2.getX() - this.getX();

                    double j;
                    for(j = entity2.getZ() - this.getZ(); i * i + j * j < 1.0E-4D; j = (Math.random() - Math.random()) * 0.01D) {
                        i = (Math.random() - Math.random()) * 0.01D;
                    }

                    this.hurtDir = (float)(Mth.atan2(j, i) * (double)(180F / (float)Math.PI) - (double)this.getYRot());
                    this.knockback((double)0.4F, i, j);
                } else {
                    this.hurtDir = (float)((int)(Math.random() * 2.0D) * 180);
                }
            }

            if (this.isDeadOrDying()) {
                if (!this.checkTotemDeathProtection(source)) {
                    SoundEvent soundEvent = this.getDeathSound();
                    if (bl2 && soundEvent != null) {
                        this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
                    }

                    this.die(source);
                }
            } else if (bl2) {
                this.playHurtSound(source);
            }

            boolean bl3 = !bl || amount > 0.0F;
            if (bl3) {
                this.lastDamageSource = source;
                this.lastDamageStamp = this.level.getGameTime();
            }

            if (this instanceof ServerPlayer) {
                CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer)this, source, f, amount, bl);
                if (g > 0.0F && g < 3.4028235E37F) {
                    ((ServerPlayer)this).awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(g * 10.0F));
                }
            }

            if (entity2 instanceof ServerPlayer) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer)entity2, this, source, f, amount, bl);
            }

            return bl3;
        }
    }

    protected void blockUsingShield(LivingEntity attacker) {
        attacker.blockedByShield(this);
    }

    protected void blockedByShield(LivingEntity target) {
        target.knockback(0.5D, target.getX() - this.getX(), target.getZ() - this.getZ());
    }

    private boolean checkTotemDeathProtection(DamageSource source) {
        if (source.isBypassInvul()) {
            return false;
        } else {
            ItemStack itemStack = null;

            for(InteractionHand interactionHand : InteractionHand.values()) {
                ItemStack itemStack2 = this.getItemInHand(interactionHand);
                if (itemStack2.is(Items.TOTEM_OF_UNDYING)) {
                    itemStack = itemStack2.copy();
                    itemStack2.shrink(1);
                    break;
                }
            }

            if (itemStack != null) {
                if (this instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer)this;
                    serverPlayer.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                    CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, itemStack);
                }

                this.setHealth(1.0F);
                this.removeAllEffects();
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                this.level.broadcastEntityEvent(this, (byte)35);
            }

            return itemStack != null;
        }
    }

    @Nullable
    public DamageSource getLastDamageSource() {
        if (this.level.getGameTime() - this.lastDamageStamp > 40L) {
            this.lastDamageSource = null;
        }

        return this.lastDamageSource;
    }

    protected void playHurtSound(DamageSource source) {
        SoundEvent soundEvent = this.getHurtSound(source);
        if (soundEvent != null) {
            this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    public boolean isDamageSourceBlocked(DamageSource source) {
        Entity entity = source.getDirectEntity();
        boolean bl = false;
        if (entity instanceof AbstractArrow) {
            AbstractArrow abstractArrow = (AbstractArrow)entity;
            if (abstractArrow.getPierceLevel() > 0) {
                bl = true;
            }
        }

        if (!source.isBypassArmor() && this.isBlocking() && !bl) {
            Vec3 vec3 = source.getSourcePosition();
            if (vec3 != null) {
                Vec3 vec32 = this.getViewVector(1.0F);
                Vec3 vec33 = vec3.vectorTo(this.position()).normalize();
                vec33 = new Vec3(vec33.x, 0.0D, vec33.z);
                if (vec33.dot(vec32) < 0.0D) {
                    return true;
                }
            }
        }

        return false;
    }

    private void breakItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            if (!this.isSilent()) {
                this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_BREAK, this.getSoundSource(), 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F, false);
            }

            this.spawnItemParticles(stack, 5);
        }

    }

    public void die(DamageSource source) {
        if (!this.isRemoved() && !this.dead) {
            Entity entity = source.getEntity();
            LivingEntity livingEntity = this.getKillCredit();
            if (this.deathScore >= 0 && livingEntity != null) {
                livingEntity.awardKillScore(this, this.deathScore, source);
            }

            if (this.isSleeping()) {
                this.stopSleeping();
            }

            if (!this.level.isClientSide && this.hasCustomName()) {
                LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
            }

            this.dead = true;
            this.getCombatTracker().recheckStatus();
            if (this.level instanceof ServerLevel) {
                if (entity != null) {
                    entity.killed((ServerLevel)this.level, this);
                }

                this.dropAllDeathLoot(source);
                this.createWitherRose(livingEntity);
            }

            this.level.broadcastEntityEvent(this, (byte)3);
            this.setPose(Pose.DYING);
        }
    }

    protected void createWitherRose(@Nullable LivingEntity adversary) {
        if (!this.level.isClientSide) {
            boolean bl = false;
            if (adversary instanceof WitherBoss) {
                if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    BlockPos blockPos = this.blockPosition();
                    BlockState blockState = Blocks.WITHER_ROSE.defaultBlockState();
                    if (this.level.getBlockState(blockPos).isAir() && blockState.canSurvive(this.level, blockPos)) {
                        this.level.setBlock(blockPos, blockState, 3);
                        bl = true;
                    }
                }

                if (!bl) {
                    ItemEntity itemEntity = new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
                    this.level.addFreshEntity(itemEntity);
                }
            }

        }
    }

    protected void dropAllDeathLoot(DamageSource source) {
        Entity entity = source.getEntity();
        int i;
        if (entity instanceof Player) {
            i = EnchantmentHelper.getMobLooting((LivingEntity)entity);
        } else {
            i = 0;
        }

        boolean bl = this.lastHurtByPlayerTime > 0;
        if (this.shouldDropLoot() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.dropFromLootTable(source, bl);
            this.dropCustomDeathLoot(source, i, bl);
        }

        this.dropEquipment();
        this.dropExperience();
    }

    protected void dropEquipment() {
    }

    protected void dropExperience() {
        if (this.level instanceof ServerLevel && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerTime > 0 && this.shouldDropExperience() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
            ExperienceOrb.award((ServerLevel)this.level, this.position(), this.getExperienceReward(this.lastHurtByPlayer));
        }

    }

    protected void dropCustomDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
    }

    public ResourceLocation getLootTable() {
        return this.getType().getDefaultLootTable();
    }

    protected void dropFromLootTable(DamageSource source, boolean causedByPlayer) {
        ResourceLocation resourceLocation = this.getLootTable();
        LootTable lootTable = this.level.getServer().getLootTables().get(resourceLocation);
        LootContext.Builder builder = this.createLootContext(causedByPlayer, source);
        lootTable.getRandomItems(builder.create(LootContextParamSets.ENTITY), this::spawnAtLocation);
    }

    protected LootContext.Builder createLootContext(boolean causedByPlayer, DamageSource source) {
        LootContext.Builder builder = (new LootContext.Builder((ServerLevel)this.level)).withRandom(this.random).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.DAMAGE_SOURCE, source).withOptionalParameter(LootContextParams.KILLER_ENTITY, source.getEntity()).withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, source.getDirectEntity());
        if (causedByPlayer && this.lastHurtByPlayer != null) {
            builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer).withLuck(this.lastHurtByPlayer.getLuck());
        }

        return builder;
    }

    public void knockback(double strength, double x, double z) {
        strength *= 1.0D - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (!(strength <= 0.0D)) {
            this.hasImpulse = true;
            Vec3 vec3 = this.getDeltaMovement();
            Vec3 vec32 = (new Vec3(x, 0.0D, z)).normalize().scale(strength);
            this.setDeltaMovement(vec3.x / 2.0D - vec32.x, this.onGround ? Math.min(0.4D, vec3.y / 2.0D + strength) : vec3.y, vec3.z / 2.0D - vec32.z);
        }
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GENERIC_HURT;
    }

    @Nullable
    public SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    private SoundEvent getFallDamageSound(int distance) {
        return distance > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }

    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
    }

    protected SoundEvent getDrinkingSound(ItemStack stack) {
        return stack.getDrinkingSound();
    }

    public SoundEvent getEatingSound(ItemStack stack) {
        return stack.getEatingSound();
    }

    @Override
    public void setOnGround(boolean onGround) {
        super.setOnGround(onGround);
        if (onGround) {
            this.lastClimbablePos = Optional.empty();
        }

    }

    public Optional<BlockPos> getLastClimbablePos() {
        return this.lastClimbablePos;
    }

    public boolean onClimbable() {
        if (this.isSpectator()) {
            return false;
        } else {
            BlockPos blockPos = this.blockPosition();
            BlockState blockState = this.getFeetBlockState();
            if (blockState.is(BlockTags.CLIMBABLE)) {
                this.lastClimbablePos = Optional.of(blockPos);
                return true;
            } else if (blockState.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(blockPos, blockState)) {
                this.lastClimbablePos = Optional.of(blockPos);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean trapdoorUsableAsLadder(BlockPos pos, BlockState state) {
        if (state.getValue(TrapDoorBlock.OPEN)) {
            BlockState blockState = this.level.getBlockState(pos.below());
            if (blockState.is(Blocks.LADDER) && blockState.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isAlive() {
        return !this.isRemoved() && this.getHealth() > 0.0F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        boolean bl = super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
        int i = this.calculateFallDamage(fallDistance, damageMultiplier);
        if (i > 0) {
            this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
            this.playBlockFallSound();
            this.hurt(damageSource, (float)i);
            return true;
        } else {
            return bl;
        }
    }

    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        MobEffectInstance mobEffectInstance = this.getEffect(MobEffects.JUMP);
        float f = mobEffectInstance == null ? 0.0F : (float)(mobEffectInstance.getAmplifier() + 1);
        return Mth.ceil((fallDistance - 3.0F - f) * damageMultiplier);
    }

    protected void playBlockFallSound() {
        if (!this.isSilent()) {
            int i = Mth.floor(this.getX());
            int j = Mth.floor(this.getY() - (double)0.2F);
            int k = Mth.floor(this.getZ());
            BlockState blockState = this.level.getBlockState(new BlockPos(i, j, k));
            if (!blockState.isAir()) {
                SoundType soundType = blockState.getSoundType();
                this.playSound(soundType.getFallSound(), soundType.getVolume() * 0.5F, soundType.getPitch() * 0.75F);
            }

        }
    }

    @Override
    public void animateHurt() {
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
        this.hurtDir = 0.0F;
    }

    public int getArmorValue() {
        return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
    }

    protected void hurtArmor(DamageSource source, float amount) {
    }

    protected void hurtHelmet(DamageSource source, float amount) {
    }

    protected void hurtCurrentlyUsedShield(float amount) {
    }

    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        if (!source.isBypassArmor()) {
            this.hurtArmor(source, amount);
            amount = CombatRules.getDamageAfterAbsorb(amount, (float)this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        }

        return amount;
    }

    protected float getDamageAfterMagicAbsorb(DamageSource source, float amount) {
        if (source.isBypassMagic()) {
            return amount;
        } else {
            if (this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && source != DamageSource.OUT_OF_WORLD) {
                int i = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f = amount * (float)j;
                float g = amount;
                amount = Math.max(f / 25.0F, 0.0F);
                float h = g - amount;
                if (h > 0.0F && h < 3.4028235E37F) {
                    if (this instanceof ServerPlayer) {
                        ((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(h * 10.0F));
                    } else if (source.getEntity() instanceof ServerPlayer) {
                        ((ServerPlayer)source.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(h * 10.0F));
                    }
                }
            }

            if (amount <= 0.0F) {
                return 0.0F;
            } else {
                int k = EnchantmentHelper.getDamageProtection(this.getArmorSlots(), source);
                if (k > 0) {
                    amount = CombatRules.getDamageAfterMagicAbsorb(amount, (float)k);
                }

                return amount;
            }
        }
    }

    protected void actuallyHurt(DamageSource source, float amount) {
        if (!this.isInvulnerableTo(source)) {
            amount = this.getDamageAfterArmorAbsorb(source, amount);
            amount = this.getDamageAfterMagicAbsorb(source, amount);
            float var8 = Math.max(amount - this.getAbsorptionAmount(), 0.0F);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - (amount - var8));
            float g = amount - var8;
            if (g > 0.0F && g < 3.4028235E37F && source.getEntity() instanceof ServerPlayer) {
                ((ServerPlayer)source.getEntity()).awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(g * 10.0F));
            }

            if (var8 != 0.0F) {
                float h = this.getHealth();
                this.setHealth(h - var8);
                this.getCombatTracker().recordDamage(source, h, var8);
                this.setAbsorptionAmount(this.getAbsorptionAmount() - var8);
                this.gameEvent(GameEvent.ENTITY_DAMAGED, source.getEntity());
            }
        }
    }

    public CombatTracker getCombatTracker() {
        return this.combatTracker;
    }

    @Nullable
    public LivingEntity getKillCredit() {
        if (this.combatTracker.getKiller() != null) {
            return this.combatTracker.getKiller();
        } else if (this.lastHurtByPlayer != null) {
            return this.lastHurtByPlayer;
        } else {
            return this.lastHurtByMob != null ? this.lastHurtByMob : null;
        }
    }

    public final float getMaxHealth() {
        return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
    }

    public final int getArrowCount() {
        return this.entityData.get(DATA_ARROW_COUNT_ID);
    }

    public final void setArrowCount(int stuckArrowCount) {
        this.entityData.set(DATA_ARROW_COUNT_ID, stuckArrowCount);
    }

    public final int getStingerCount() {
        return this.entityData.get(DATA_STINGER_COUNT_ID);
    }

    public final void setStingerCount(int stingerCount) {
        this.entityData.set(DATA_STINGER_COUNT_ID, stingerCount);
    }

    private int getCurrentSwingDuration() {
        if (MobEffectUtil.hasDigSpeed(this)) {
            return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
        } else {
            return this.hasEffect(MobEffects.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
        }
    }

    public void swing(InteractionHand hand) {
        this.swing(hand, false);
    }

    public void swing(InteractionHand hand, boolean fromServerPlayer) {
        if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
            this.swingingArm = hand;
            if (this.level instanceof ServerLevel) {
                ClientboundAnimatePacket clientboundAnimatePacket = new ClientboundAnimatePacket(this, hand == InteractionHand.MAIN_HAND ? 0 : 3);
                ServerChunkCache serverChunkCache = ((ServerLevel)this.level).getChunkSource();
                if (fromServerPlayer) {
                    serverChunkCache.broadcastAndSend(this, clientboundAnimatePacket);
                } else {
                    serverChunkCache.broadcast(this, clientboundAnimatePacket);
                }
            }
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        switch(status) {
        case 2:
        case 33:
        case 36:
        case 37:
        case 44:
        case 57:
            this.animationSpeed = 1.5F;
            this.invulnerableTime = 20;
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
            this.hurtDir = 0.0F;
            if (status == 33) {
                this.playSound(SoundEvents.THORNS_HIT, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            DamageSource damageSource;
            if (status == 37) {
                damageSource = DamageSource.ON_FIRE;
            } else if (status == 36) {
                damageSource = DamageSource.DROWN;
            } else if (status == 44) {
                damageSource = DamageSource.SWEET_BERRY_BUSH;
            } else if (status == 57) {
                damageSource = DamageSource.FREEZE;
            } else {
                damageSource = DamageSource.GENERIC;
            }

            SoundEvent soundEvent = this.getHurtSound(damageSource);
            if (soundEvent != null) {
                this.playSound(soundEvent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            this.hurt(DamageSource.GENERIC, 0.0F);
            this.lastDamageSource = damageSource;
            this.lastDamageStamp = this.level.getGameTime();
            break;
        case 3:
            SoundEvent soundEvent2 = this.getDeathSound();
            if (soundEvent2 != null) {
                this.playSound(soundEvent2, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            if (!(this instanceof Player)) {
                this.setHealth(0.0F);
                this.die(DamageSource.GENERIC);
            }
            break;
        case 4:
        case 5:
        case 6:
        case 7:
        case 8:
        case 9:
        case 10:
        case 11:
        case 12:
        case 13:
        case 14:
        case 15:
        case 16:
        case 17:
        case 18:
        case 19:
        case 20:
        case 21:
        case 22:
        case 23:
        case 24:
        case 25:
        case 26:
        case 27:
        case 28:
        case 31:
        case 32:
        case 34:
        case 35:
        case 38:
        case 39:
        case 40:
        case 41:
        case 42:
        case 43:
        case 45:
        case 53:
        case 56:
        case 58:
        case 59:
        default:
            super.handleEntityEvent(status);
            break;
        case 29:
            this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + this.level.random.nextFloat() * 0.4F);
            break;
        case 30:
            this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            break;
        case 46:
            int i = 128;

            for(int j = 0; j < 128; ++j) {
                double d = (double)j / 127.0D;
                float f = (this.random.nextFloat() - 0.5F) * 0.2F;
                float g = (this.random.nextFloat() - 0.5F) * 0.2F;
                float h = (this.random.nextFloat() - 0.5F) * 0.2F;
                double e = Mth.lerp(d, this.xo, this.getX()) + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth() * 2.0D;
                double k = Mth.lerp(d, this.yo, this.getY()) + this.random.nextDouble() * (double)this.getBbHeight();
                double l = Mth.lerp(d, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth() * 2.0D;
                this.level.addParticle(ParticleTypes.PORTAL, e, k, l, (double)f, (double)g, (double)h);
            }
            break;
        case 47:
            this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
            break;
        case 48:
            this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
            break;
        case 49:
            this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
            break;
        case 50:
            this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
            break;
        case 51:
            this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
            break;
        case 52:
            this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
            break;
        case 54:
            HoneyBlock.showJumpParticles(this);
            break;
        case 55:
            this.swapHandItems();
            break;
        case 60:
            this.makePoofParticles();
        }

    }

    private void makePoofParticles() {
        for(int i = 0; i < 20; ++i) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(ParticleTypes.POOF, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), d, e, f);
        }

    }

    private void swapHandItems() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.OFFHAND);
        this.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
        this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
    }

    @Override
    protected void outOfWorld() {
        this.hurt(DamageSource.OUT_OF_WORLD, 4.0F);
    }

    protected void updateSwingTime() {
        int i = this.getCurrentSwingDuration();
        if (this.swinging) {
            ++this.swingTime;
            if (this.swingTime >= i) {
                this.swingTime = 0;
                this.swinging = false;
            }
        } else {
            this.swingTime = 0;
        }

        this.attackAnim = (float)this.swingTime / (float)i;
    }

    @Nullable
    public AttributeInstance getAttribute(Attribute attribute) {
        return this.getAttributes().getInstance(attribute);
    }

    public double getAttributeValue(Attribute attribute) {
        return this.getAttributes().getValue(attribute);
    }

    public double getAttributeBaseValue(Attribute attribute) {
        return this.getAttributes().getBaseValue(attribute);
    }

    public AttributeMap getAttributes() {
        return this.attributes;
    }

    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    public ItemStack getMainHandItem() {
        return this.getItemBySlot(EquipmentSlot.MAINHAND);
    }

    public ItemStack getOffhandItem() {
        return this.getItemBySlot(EquipmentSlot.OFFHAND);
    }

    public boolean isHolding(Item item) {
        return this.isHolding((stack) -> {
            return stack.is(item);
        });
    }

    public boolean isHolding(Predicate<ItemStack> predicate) {
        return predicate.test(this.getMainHandItem()) || predicate.test(this.getOffhandItem());
    }

    public ItemStack getItemInHand(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return this.getItemBySlot(EquipmentSlot.MAINHAND);
        } else if (hand == InteractionHand.OFF_HAND) {
            return this.getItemBySlot(EquipmentSlot.OFFHAND);
        } else {
            throw new IllegalArgumentException("Invalid hand " + hand);
        }
    }

    public void setItemInHand(InteractionHand hand, ItemStack stack) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.setItemSlot(EquipmentSlot.MAINHAND, stack);
        } else {
            if (hand != InteractionHand.OFF_HAND) {
                throw new IllegalArgumentException("Invalid hand " + hand);
            }

            this.setItemSlot(EquipmentSlot.OFFHAND, stack);
        }

    }

    public boolean hasItemInSlot(EquipmentSlot slot) {
        return !this.getItemBySlot(slot).isEmpty();
    }

    @Override
    public abstract Iterable<ItemStack> getArmorSlots();

    public abstract ItemStack getItemBySlot(EquipmentSlot slot);

    @Override
    public abstract void setItemSlot(EquipmentSlot slot, ItemStack stack);

    protected void verifyEquippedItem(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        if (compoundTag != null) {
            stack.getItem().verifyTagAfterLoad(compoundTag);
        }

    }

    public float getArmorCoverPercentage() {
        Iterable<ItemStack> iterable = this.getArmorSlots();
        int i = 0;
        int j = 0;

        for(ItemStack itemStack : iterable) {
            if (!itemStack.isEmpty()) {
                ++j;
            }

            ++i;
        }

        return i > 0 ? (float)j / (float)i : 0.0F;
    }

    @Override
    public void setSprinting(boolean sprinting) {
        super.setSprinting(sprinting);
        AttributeInstance attributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeInstance.getModifier(SPEED_MODIFIER_SPRINTING_UUID) != null) {
            attributeInstance.removeModifier(SPEED_MODIFIER_SPRINTING);
        }

        if (sprinting) {
            attributeInstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
        }

    }

    public float getSoundVolume() {
        return 1.0F;
    }

    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    @Override
    public void push(Entity entity) {
        if (!this.isSleeping()) {
            super.push(entity);
        }

    }

    private void dismountVehicle(Entity vehicle) {
        Vec3 vec3;
        if (this.isRemoved()) {
            vec3 = this.position();
        } else if (!vehicle.isRemoved() && !this.level.getBlockState(vehicle.blockPosition()).is(BlockTags.PORTALS)) {
            vec3 = vehicle.getDismountLocationForPassenger(this);
        } else {
            double d = Math.max(this.getY(), vehicle.getY());
            vec3 = new Vec3(this.getX(), d, this.getZ());
        }

        this.dismountTo(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    protected float getJumpPower() {
        return 0.42F * this.getBlockJumpFactor();
    }

    public double getJumpBoostPower() {
        return this.hasEffect(MobEffects.JUMP) ? (double)(0.1F * (float)(this.getEffect(MobEffects.JUMP).getAmplifier() + 1)) : 0.0D;
    }

    protected void jumpFromGround() {
        double d = (double)this.getJumpPower() + this.getJumpBoostPower();
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x, d, vec3.z);
        if (this.isSprinting()) {
            float f = this.getYRot() * ((float)Math.PI / 180F);
            this.setDeltaMovement(this.getDeltaMovement().add((double)(-Mth.sin(f) * 0.2F), 0.0D, (double)(Mth.cos(f) * 0.2F)));
        }

        this.hasImpulse = true;
    }

    protected void goDownInWater() {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)-0.04F, 0.0D));
    }

    protected void jumpInLiquid(TagKey<Fluid> fluid) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)0.04F, 0.0D));
    }

    protected float getWaterSlowDown() {
        return 0.8F;
    }

    public boolean canStandOnFluid(FluidState fluidState) {
        return false;
    }

    public void travel(Vec3 movementInput) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            double d = 0.08D;
            boolean bl = this.getDeltaMovement().y <= 0.0D;
            if (bl && this.hasEffect(MobEffects.SLOW_FALLING)) {
                d = 0.01D;
                this.resetFallDistance();
            }

            FluidState fluidState = this.level.getFluidState(this.blockPosition());
            if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
                double e = this.getY();
                float f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
                float g = 0.02F;
                float h = (float)EnchantmentHelper.getDepthStrider(this);
                if (h > 3.0F) {
                    h = 3.0F;
                }

                if (!this.onGround) {
                    h *= 0.5F;
                }

                if (h > 0.0F) {
                    f += (0.54600006F - f) * h / 3.0F;
                    g += (this.getSpeed() - g) * h / 3.0F;
                }

                if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                    f = 0.96F;
                }

                this.moveRelative(g, movementInput);
                this.move(MoverType.SELF, this.getDeltaMovement());
                Vec3 vec3 = this.getDeltaMovement();
                if (this.horizontalCollision && this.onClimbable()) {
                    vec3 = new Vec3(vec3.x, 0.2D, vec3.z);
                }

                this.setDeltaMovement(vec3.multiply((double)f, (double)0.8F, (double)f));
                Vec3 vec32 = this.getFluidFallingAdjustedMovement(d, bl, this.getDeltaMovement());
                this.setDeltaMovement(vec32);
                if (this.horizontalCollision && this.isFree(vec32.x, vec32.y + (double)0.6F - this.getY() + e, vec32.z)) {
                    this.setDeltaMovement(vec32.x, (double)0.3F, vec32.z);
                }
            } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
                double i = this.getY();
                this.moveRelative(0.02F, movementInput);
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, (double)0.8F, 0.5D));
                    Vec3 vec33 = this.getFluidFallingAdjustedMovement(d, bl, this.getDeltaMovement());
                    this.setDeltaMovement(vec33);
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
                }

                if (!this.isNoGravity()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -d / 4.0D, 0.0D));
                }

                Vec3 vec34 = this.getDeltaMovement();
                if (this.horizontalCollision && this.isFree(vec34.x, vec34.y + (double)0.6F - this.getY() + i, vec34.z)) {
                    this.setDeltaMovement(vec34.x, (double)0.3F, vec34.z);
                }
            } else if (this.isFallFlying()) {
                Vec3 vec35 = this.getDeltaMovement();
                if (vec35.y > -0.5D) {
                    this.fallDistance = 1.0F;
                }

                Vec3 vec36 = this.getLookAngle();
                float j = this.getXRot() * ((float)Math.PI / 180F);
                double k = Math.sqrt(vec36.x * vec36.x + vec36.z * vec36.z);
                double l = vec35.horizontalDistance();
                double m = vec36.length();
                double n = Math.cos((double)j);
                n = n * n * Math.min(1.0D, m / 0.4D);
                vec35 = this.getDeltaMovement().add(0.0D, d * (-1.0D + n * 0.75D), 0.0D);
                if (vec35.y < 0.0D && k > 0.0D) {
                    double o = vec35.y * -0.1D * n;
                    vec35 = vec35.add(vec36.x * o / k, o, vec36.z * o / k);
                }

                if (j < 0.0F && k > 0.0D) {
                    double p = l * (double)(-Mth.sin(j)) * 0.04D;
                    vec35 = vec35.add(-vec36.x * p / k, p * 3.2D, -vec36.z * p / k);
                }

                if (k > 0.0D) {
                    vec35 = vec35.add((vec36.x / k * l - vec35.x) * 0.1D, 0.0D, (vec36.z / k * l - vec35.z) * 0.1D);
                }

                this.setDeltaMovement(vec35.multiply((double)0.99F, (double)0.98F, (double)0.99F));
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.horizontalCollision && !this.level.isClientSide) {
                    double q = this.getDeltaMovement().horizontalDistance();
                    double r = l - q;
                    float s = (float)(r * 10.0D - 3.0D);
                    if (s > 0.0F) {
                        this.playSound(this.getFallDamageSound((int)s), 1.0F, 1.0F);
                        this.hurt(DamageSource.FLY_INTO_WALL, s);
                    }
                }

                if (this.onGround && !this.level.isClientSide) {
                    this.setSharedFlag(7, false);
                }
            } else {
                BlockPos blockPos = this.getBlockPosBelowThatAffectsMyMovement();
                float t = this.level.getBlockState(blockPos).getBlock().getFriction();
                float u = this.onGround ? t * 0.91F : 0.91F;
                Vec3 vec37 = this.handleRelativeFrictionAndCalculateMovement(movementInput, t);
                double v = vec37.y;
                if (this.hasEffect(MobEffects.LEVITATION)) {
                    v += (0.05D * (double)(this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - vec37.y) * 0.2D;
                    this.resetFallDistance();
                } else if (this.level.isClientSide && !this.level.hasChunkAt(blockPos)) {
                    if (this.getY() > (double)this.level.getMinBuildHeight()) {
                        v = -0.1D;
                    } else {
                        v = 0.0D;
                    }
                } else if (!this.isNoGravity()) {
                    v -= d;
                }

                if (this.shouldDiscardFriction()) {
                    this.setDeltaMovement(vec37.x, v, vec37.z);
                } else {
                    this.setDeltaMovement(vec37.x * (double)u, v * (double)0.98F, vec37.z * (double)u);
                }
            }
        }

        this.calculateEntityAnimation(this, this instanceof FlyingAnimal);
    }

    public void calculateEntityAnimation(LivingEntity entity, boolean flutter) {
        entity.animationSpeedOld = entity.animationSpeed;
        double d = entity.getX() - entity.xo;
        double e = flutter ? entity.getY() - entity.yo : 0.0D;
        double f = entity.getZ() - entity.zo;
        float g = (float)Math.sqrt(d * d + e * e + f * f) * 4.0F;
        if (g > 1.0F) {
            g = 1.0F;
        }

        entity.animationSpeed += (g - entity.animationSpeed) * 0.4F;
        entity.animationPosition += entity.animationSpeed;
    }

    public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 movementInput, float slipperiness) {
        this.moveRelative(this.getFrictionInfluencedSpeed(slipperiness), movementInput);
        this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 vec3 = this.getDeltaMovement();
        if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.getFeetBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
            vec3 = new Vec3(vec3.x, 0.2D, vec3.z);
        }

        return vec3;
    }

    public Vec3 getFluidFallingAdjustedMovement(double d, boolean bl, Vec3 vec3) {
        if (!this.isNoGravity() && !this.isSprinting()) {
            double e;
            if (bl && Math.abs(vec3.y - 0.005D) >= 0.003D && Math.abs(vec3.y - d / 16.0D) < 0.003D) {
                e = -0.003D;
            } else {
                e = vec3.y - d / 16.0D;
            }

            return new Vec3(vec3.x, e, vec3.z);
        } else {
            return vec3;
        }
    }

    private Vec3 handleOnClimbable(Vec3 motion) {
        if (this.onClimbable()) {
            this.resetFallDistance();
            float f = 0.15F;
            double d = Mth.clamp(motion.x, (double)-0.15F, (double)0.15F);
            double e = Mth.clamp(motion.z, (double)-0.15F, (double)0.15F);
            double g = Math.max(motion.y, (double)-0.15F);
            if (g < 0.0D && !this.getFeetBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
                g = 0.0D;
            }

            motion = new Vec3(d, g, e);
        }

        return motion;
    }

    private float getFrictionInfluencedSpeed(float slipperiness) {
        return this.onGround ? this.getSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.flyingSpeed;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float movementSpeed) {
        this.speed = movementSpeed;
    }

    public boolean doHurtTarget(Entity target) {
        this.setLastHurtMob(target);
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.updatingUsingItem();
        this.updateSwimAmount();
        if (!this.level.isClientSide) {
            int i = this.getArrowCount();
            if (i > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - i);
                }

                --this.removeArrowTime;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(i - 1);
                }
            }

            int j = this.getStingerCount();
            if (j > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - j);
                }

                --this.removeStingerTime;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(j - 1);
                }
            }

            this.detectEquipmentUpdates();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }

            if (this.isSleeping() && !this.checkBedExists()) {
                this.stopSleeping();
            }
        }

        this.aiStep();
        double d = this.getX() - this.xo;
        double e = this.getZ() - this.zo;
        float f = (float)(d * d + e * e);
        float g = this.yBodyRot;
        float h = 0.0F;
        this.oRun = this.run;
        float k = 0.0F;
        if (f > 0.0025000002F) {
            k = 1.0F;
            h = (float)Math.sqrt((double)f) * 3.0F;
            float l = (float)Mth.atan2(e, d) * (180F / (float)Math.PI) - 90.0F;
            float m = Mth.abs(Mth.wrapDegrees(this.getYRot()) - l);
            if (95.0F < m && m < 265.0F) {
                g = l - 180.0F;
            } else {
                g = l;
            }
        }

        if (this.attackAnim > 0.0F) {
            g = this.getYRot();
        }

        if (!this.onGround) {
            k = 0.0F;
        }

        this.run += (k - this.run) * 0.3F;
        this.level.getProfiler().push("headTurn");
        h = this.tickHeadTurn(g, h);
        this.level.getProfiler().pop();
        this.level.getProfiler().push("rangeChecks");

        while(this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }

        while(this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }

        while(this.yBodyRot - this.yBodyRotO < -180.0F) {
            this.yBodyRotO -= 360.0F;
        }

        while(this.yBodyRot - this.yBodyRotO >= 180.0F) {
            this.yBodyRotO += 360.0F;
        }

        while(this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }

        while(this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }

        while(this.yHeadRot - this.yHeadRotO < -180.0F) {
            this.yHeadRotO -= 360.0F;
        }

        while(this.yHeadRot - this.yHeadRotO >= 180.0F) {
            this.yHeadRotO += 360.0F;
        }

        this.level.getProfiler().pop();
        this.animStep += h;
        if (this.isFallFlying()) {
            ++this.fallFlyTicks;
        } else {
            this.fallFlyTicks = 0;
        }

        if (this.isSleeping()) {
            this.setXRot(0.0F);
        }

    }

    public void detectEquipmentUpdates() {
        Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
        if (map != null) {
            this.handleHandSwap(map);
            if (!map.isEmpty()) {
                this.handleEquipmentChanges(map);
            }
        }

    }

    @Nullable
    private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
        Map<EquipmentSlot, ItemStack> map = null;

        for(EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            ItemStack itemStack;
            switch(equipmentSlot.getType()) {
            case HAND:
                itemStack = this.getLastHandItem(equipmentSlot);
                break;
            case ARMOR:
                itemStack = this.getLastArmorItem(equipmentSlot);
                break;
            default:
                continue;
            }

            ItemStack itemStack4 = this.getItemBySlot(equipmentSlot);
            if (!ItemStack.matches(itemStack4, itemStack)) {
                if (map == null) {
                    map = Maps.newEnumMap(EquipmentSlot.class);
                }

                map.put(equipmentSlot, itemStack4);
                if (!itemStack.isEmpty()) {
                    this.getAttributes().removeAttributeModifiers(itemStack.getAttributeModifiers(equipmentSlot));
                }

                if (!itemStack4.isEmpty()) {
                    this.getAttributes().addTransientAttributeModifiers(itemStack4.getAttributeModifiers(equipmentSlot));
                }
            }
        }

        return map;
    }

    private void handleHandSwap(Map<EquipmentSlot, ItemStack> equipmentChanges) {
        ItemStack itemStack = equipmentChanges.get(EquipmentSlot.MAINHAND);
        ItemStack itemStack2 = equipmentChanges.get(EquipmentSlot.OFFHAND);
        if (itemStack != null && itemStack2 != null && ItemStack.matches(itemStack, this.getLastHandItem(EquipmentSlot.OFFHAND)) && ItemStack.matches(itemStack2, this.getLastHandItem(EquipmentSlot.MAINHAND))) {
            ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundEntityEventPacket(this, (byte)55));
            equipmentChanges.remove(EquipmentSlot.MAINHAND);
            equipmentChanges.remove(EquipmentSlot.OFFHAND);
            this.setLastHandItem(EquipmentSlot.MAINHAND, itemStack.copy());
            this.setLastHandItem(EquipmentSlot.OFFHAND, itemStack2.copy());
        }

    }

    private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> equipmentChanges) {
        List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(equipmentChanges.size());
        equipmentChanges.forEach((slot, stack) -> {
            ItemStack itemStack = stack.copy();
            list.add(Pair.of(slot, itemStack));
            switch(slot.getType()) {
            case HAND:
                this.setLastHandItem(slot, itemStack);
                break;
            case ARMOR:
                this.setLastArmorItem(slot, itemStack);
            }

        });
        ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEquipmentPacket(this.getId(), list));
    }

    private ItemStack getLastArmorItem(EquipmentSlot slot) {
        return this.lastArmorItemStacks.get(slot.getIndex());
    }

    private void setLastArmorItem(EquipmentSlot slot, ItemStack armor) {
        this.lastArmorItemStacks.set(slot.getIndex(), armor);
    }

    private ItemStack getLastHandItem(EquipmentSlot slot) {
        return this.lastHandItemStacks.get(slot.getIndex());
    }

    private void setLastHandItem(EquipmentSlot slot, ItemStack stack) {
        this.lastHandItemStacks.set(slot.getIndex(), stack);
    }

    protected float tickHeadTurn(float bodyRotation, float headRotation) {
        float f = Mth.wrapDegrees(bodyRotation - this.yBodyRot);
        this.yBodyRot += f * 0.3F;
        float g = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
        boolean bl = g < -90.0F || g >= 90.0F;
        if (g < -75.0F) {
            g = -75.0F;
        }

        if (g >= 75.0F) {
            g = 75.0F;
        }

        this.yBodyRot = this.getYRot() - g;
        if (g * g > 2500.0F) {
            this.yBodyRot += g * 0.2F;
        }

        if (bl) {
            headRotation *= -1.0F;
        }

        return headRotation;
    }

    public void aiStep() {
        if (this.noJumpDelay > 0) {
            --this.noJumpDelay;
        }

        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.setPacketCoordinates(this.getX(), this.getY(), this.getZ());
        }

        if (this.lerpSteps > 0) {
            double d = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
            double e = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
            double f = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
            double g = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
            this.setYRot(this.getYRot() + (float)g / (float)this.lerpSteps);
            this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
            --this.lerpSteps;
            this.setPos(d, e, f);
            this.setRot(this.getYRot(), this.getXRot());
        } else if (!this.isEffectiveAi()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
        }

        if (this.lerpHeadSteps > 0) {
            this.yHeadRot += (float)Mth.wrapDegrees(this.lyHeadRot - (double)this.yHeadRot) / (float)this.lerpHeadSteps;
            --this.lerpHeadSteps;
        }

        Vec3 vec3 = this.getDeltaMovement();
        double h = vec3.x;
        double i = vec3.y;
        double j = vec3.z;
        if (Math.abs(vec3.x) < 0.003D) {
            h = 0.0D;
        }

        if (Math.abs(vec3.y) < 0.003D) {
            i = 0.0D;
        }

        if (Math.abs(vec3.z) < 0.003D) {
            j = 0.0D;
        }

        this.setDeltaMovement(h, i, j);
        this.level.getProfiler().push("ai");
        if (this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.isEffectiveAi()) {
            this.level.getProfiler().push("newAi");
            this.serverAiStep();
            this.level.getProfiler().pop();
        }

        this.level.getProfiler().pop();
        this.level.getProfiler().push("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            double k;
            if (this.isInLava()) {
                k = this.getFluidHeight(FluidTags.LAVA);
            } else {
                k = this.getFluidHeight(FluidTags.WATER);
            }

            boolean bl = this.isInWater() && k > 0.0D;
            double m = this.getFluidJumpThreshold();
            if (!bl || this.onGround && !(k > m)) {
                if (!this.isInLava() || this.onGround && !(k > m)) {
                    if ((this.onGround || bl && k <= m) && this.noJumpDelay == 0) {
                        this.jumpFromGround();
                        this.noJumpDelay = 10;
                    }
                } else {
                    this.jumpInLiquid(FluidTags.LAVA);
                }
            } else {
                this.jumpInLiquid(FluidTags.WATER);
            }
        } else {
            this.noJumpDelay = 0;
        }

        this.level.getProfiler().pop();
        this.level.getProfiler().push("travel");
        this.xxa *= 0.98F;
        this.zza *= 0.98F;
        this.updateFallFlying();
        AABB aABB = this.getBoundingBox();
        this.travel(new Vec3((double)this.xxa, (double)this.yya, (double)this.zza));
        this.level.getProfiler().pop();
        this.level.getProfiler().push("freezing");
        boolean bl2 = this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES);
        if (!this.level.isClientSide && !this.isDeadOrDying()) {
            int n = this.getTicksFrozen();
            if (this.isInPowderSnow && this.canFreeze()) {
                this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), n + 1));
            } else {
                this.setTicksFrozen(Math.max(0, n - 2));
            }
        }

        this.removeFrost();
        this.tryAddFrost();
        if (!this.level.isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
            int o = bl2 ? 5 : 1;
            this.hurt(DamageSource.FREEZE, (float)o);
        }

        this.level.getProfiler().pop();
        this.level.getProfiler().push("push");
        if (this.autoSpinAttackTicks > 0) {
            --this.autoSpinAttackTicks;
            this.checkAutoSpinAttack(aABB, this.getBoundingBox());
        }

        this.pushEntities();
        this.level.getProfiler().pop();
        if (!this.level.isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
            this.hurt(DamageSource.DROWN, 1.0F);
        }

    }

    public boolean isSensitiveToWater() {
        return false;
    }

    private void updateFallFlying() {
        boolean bl = this.getSharedFlag(7);
        if (bl && !this.onGround && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
            ItemStack itemStack = this.getItemBySlot(EquipmentSlot.CHEST);
            if (itemStack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemStack)) {
                bl = true;
                int i = this.fallFlyTicks + 1;
                if (!this.level.isClientSide && i % 10 == 0) {
                    int j = i / 10;
                    if (j % 2 == 0) {
                        itemStack.hurtAndBreak(1, this, (player) -> {
                            player.broadcastBreakEvent(EquipmentSlot.CHEST);
                        });
                    }

                    this.gameEvent(GameEvent.ELYTRA_FREE_FALL);
                }
            } else {
                bl = false;
            }
        } else {
            bl = false;
        }

        if (!this.level.isClientSide) {
            this.setSharedFlag(7, bl);
        }

    }

    protected void serverAiStep() {
    }

    protected void pushEntities() {
        List<Entity> list = this.level.getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this));
        if (!list.isEmpty()) {
            int i = this.level.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
                int j = 0;

                for(int k = 0; k < list.size(); ++k) {
                    if (!list.get(k).isPassenger()) {
                        ++j;
                    }
                }

                if (j > i - 1) {
                    this.hurt(DamageSource.CRAMMING, 6.0F);
                }
            }

            for(int l = 0; l < list.size(); ++l) {
                Entity entity = list.get(l);
                this.doPush(entity);
            }
        }

    }

    protected void checkAutoSpinAttack(AABB a, AABB b) {
        AABB aABB = a.minmax(b);
        List<Entity> list = this.level.getEntities(this, aABB);
        if (!list.isEmpty()) {
            for(int i = 0; i < list.size(); ++i) {
                Entity entity = list.get(i);
                if (entity instanceof LivingEntity) {
                    this.doAutoAttackOnTouch((LivingEntity)entity);
                    this.autoSpinAttackTicks = 0;
                    this.setDeltaMovement(this.getDeltaMovement().scale(-0.2D));
                    break;
                }
            }
        } else if (this.horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }

        if (!this.level.isClientSide && this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
        }

    }

    protected void doPush(Entity entity) {
        entity.push(this);
    }

    protected void doAutoAttackOnTouch(LivingEntity target) {
    }

    public boolean isAutoSpinAttack() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        super.stopRiding();
        if (entity != null && entity != this.getVehicle() && !this.level.isClientSide) {
            this.dismountVehicle(entity);
        }

    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.oRun = this.run;
        this.run = 0.0F;
        this.resetFallDistance();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = (double)yaw;
        this.lerpXRot = (double)pitch;
        this.lerpSteps = interpolationSteps;
    }

    @Override
    public void lerpHeadTo(float yaw, int interpolationSteps) {
        this.lyHeadRot = (double)yaw;
        this.lerpHeadSteps = interpolationSteps;
    }

    public void setJumping(boolean jumping) {
        this.jumping = jumping;
    }

    public void onItemPickup(ItemEntity item) {
        Player player = item.getThrower() != null ? this.level.getPlayerByUUID(item.getThrower()) : null;
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)player, item.getItem(), this);
        }

    }

    public void take(Entity item, int count) {
        if (!item.isRemoved() && !this.level.isClientSide && (item instanceof ItemEntity || item instanceof AbstractArrow || item instanceof ExperienceOrb)) {
            ((ServerLevel)this.level).getChunkSource().broadcast(item, new ClientboundTakeItemEntityPacket(item.getId(), this.getId(), count));
        }

    }

    public boolean hasLineOfSight(Entity entity) {
        if (entity.level != this.level) {
            return false;
        } else {
            Vec3 vec3 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
            Vec3 vec32 = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            if (vec32.distanceTo(vec3) > 128.0D) {
                return false;
            } else {
                return this.level.clip(new ClipContext(vec3, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
            }
        }
    }

    @Override
    public float getViewYRot(float tickDelta) {
        return tickDelta == 1.0F ? this.yHeadRot : Mth.lerp(tickDelta, this.yHeadRotO, this.yHeadRot);
    }

    public float getAttackAnim(float tickDelta) {
        float f = this.attackAnim - this.oAttackAnim;
        if (f < 0.0F) {
            ++f;
        }

        return this.oAttackAnim + f * tickDelta;
    }

    public boolean isEffectiveAi() {
        return !this.level.isClientSide;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && !this.isSpectator() && !this.onClimbable();
    }

    @Override
    public float getYHeadRot() {
        return this.yHeadRot;
    }

    @Override
    public void setYHeadRot(float headYaw) {
        this.yHeadRot = headYaw;
    }

    @Override
    public void setYBodyRot(float bodyYaw) {
        this.yBodyRot = bodyYaw;
    }

    @Override
    protected Vec3 getRelativePortalPosition(Direction.Axis portalAxis, BlockUtil.FoundRectangle portalRect) {
        return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(portalAxis, portalRect));
    }

    public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 pos) {
        return new Vec3(pos.x, pos.y, 0.0D);
    }

    public float getAbsorptionAmount() {
        return this.absorptionAmount;
    }

    public void setAbsorptionAmount(float amount) {
        if (amount < 0.0F) {
            amount = 0.0F;
        }

        this.absorptionAmount = amount;
    }

    public void onEnterCombat() {
    }

    public void onLeaveCombat() {
    }

    protected void updateEffectVisibility() {
        this.effectsDirty = true;
    }

    public abstract HumanoidArm getMainArm();

    public boolean isUsingItem() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
    }

    public InteractionHand getUsedItemHand() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private void updatingUsingItem() {
        if (this.isUsingItem()) {
            if (ItemStack.isSameIgnoreDurability(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                this.updateUsingItem(this.useItem);
            } else {
                this.stopUsingItem();
            }
        }

    }

    protected void updateUsingItem(ItemStack stack) {
        stack.onUseTick(this.level, this, this.getUseItemRemainingTicks());
        if (this.shouldTriggerItemUseEffects()) {
            this.triggerItemUseEffects(stack, 5);
        }

        if (--this.useItemRemaining == 0 && !this.level.isClientSide && !stack.useOnRelease()) {
            this.completeUsingItem();
        }

    }

    private boolean shouldTriggerItemUseEffects() {
        int i = this.getUseItemRemainingTicks();
        FoodProperties foodProperties = this.useItem.getItem().getFoodProperties();
        boolean bl = foodProperties != null && foodProperties.isFastFood();
        bl |= i <= this.useItem.getUseDuration() - 7;
        return bl && i % 4 == 0;
    }

    private void updateSwimAmount() {
        this.swimAmountO = this.swimAmount;
        if (this.isVisuallySwimming()) {
            this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
        } else {
            this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
        }

    }

    protected void setLivingEntityFlag(int mask, boolean value) {
        int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
        if (value) {
            i |= mask;
        } else {
            i &= ~mask;
        }

        this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)i);
    }

    public void startUsingItem(InteractionHand hand) {
        ItemStack itemStack = this.getItemInHand(hand);
        if (!itemStack.isEmpty() && !this.isUsingItem()) {
            this.useItem = itemStack;
            this.useItemRemaining = itemStack.getUseDuration();
            if (!this.level.isClientSide) {
                this.setLivingEntityFlag(1, true);
                this.setLivingEntityFlag(2, hand == InteractionHand.OFF_HAND);
            }

        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (SLEEPING_POS_ID.equals(data)) {
            if (this.level.isClientSide) {
                this.getSleepingPos().ifPresent(this::setPosToBed);
            }
        } else if (DATA_LIVING_ENTITY_FLAGS.equals(data) && this.level.isClientSide) {
            if (this.isUsingItem() && this.useItem.isEmpty()) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                if (!this.useItem.isEmpty()) {
                    this.useItemRemaining = this.useItem.getUseDuration();
                }
            } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
                this.useItem = ItemStack.EMPTY;
                this.useItemRemaining = 0;
            }
        }

    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor anchorPoint, Vec3 target) {
        super.lookAt(anchorPoint, target);
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRot = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
    }

    protected void triggerItemUseEffects(ItemStack stack, int particleCount) {
        if (!stack.isEmpty() && this.isUsingItem()) {
            if (stack.getUseAnimation() == UseAnim.DRINK) {
                this.playSound(this.getDrinkingSound(stack), 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
            }

            if (stack.getUseAnimation() == UseAnim.EAT) {
                this.spawnItemParticles(stack, particleCount);
                this.playSound(this.getEatingSound(stack), 0.5F + 0.5F * (float)this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

        }
    }

    private void spawnItemParticles(ItemStack stack, int count) {
        for(int i = 0; i < count; ++i) {
            Vec3 vec3 = new Vec3(((double)this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
            vec3 = vec3.xRot(-this.getXRot() * ((float)Math.PI / 180F));
            vec3 = vec3.yRot(-this.getYRot() * ((float)Math.PI / 180F));
            double d = (double)(-this.random.nextFloat()) * 0.6D - 0.3D;
            Vec3 vec32 = new Vec3(((double)this.random.nextFloat() - 0.5D) * 0.3D, d, 0.6D);
            vec32 = vec32.xRot(-this.getXRot() * ((float)Math.PI / 180F));
            vec32 = vec32.yRot(-this.getYRot() * ((float)Math.PI / 180F));
            vec32 = vec32.add(this.getX(), this.getEyeY(), this.getZ());
            this.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), vec32.x, vec32.y, vec32.z, vec3.x, vec3.y + 0.05D, vec3.z);
        }

    }

    protected void completeUsingItem() {
        if (!this.level.isClientSide || this.isUsingItem()) {
            InteractionHand interactionHand = this.getUsedItemHand();
            if (!this.useItem.equals(this.getItemInHand(interactionHand))) {
                this.releaseUsingItem();
            } else {
                if (!this.useItem.isEmpty() && this.isUsingItem()) {
                    this.triggerItemUseEffects(this.useItem, 16);
                    ItemStack itemStack = this.useItem.finishUsingItem(this.level, this);
                    if (itemStack != this.useItem) {
                        this.setItemInHand(interactionHand, itemStack);
                    }

                    this.stopUsingItem();
                }

            }
        }
    }

    public ItemStack getUseItem() {
        return this.useItem;
    }

    public int getUseItemRemainingTicks() {
        return this.useItemRemaining;
    }

    public int getTicksUsingItem() {
        return this.isUsingItem() ? this.useItem.getUseDuration() - this.getUseItemRemainingTicks() : 0;
    }

    public void releaseUsingItem() {
        if (!this.useItem.isEmpty()) {
            this.useItem.releaseUsing(this.level, this, this.getUseItemRemainingTicks());
            if (this.useItem.useOnRelease()) {
                this.updatingUsingItem();
            }
        }

        this.stopUsingItem();
    }

    public void stopUsingItem() {
        if (!this.level.isClientSide) {
            this.setLivingEntityFlag(1, false);
        }

        this.useItem = ItemStack.EMPTY;
        this.useItemRemaining = 0;
    }

    public boolean isBlocking() {
        if (this.isUsingItem() && !this.useItem.isEmpty()) {
            Item item = this.useItem.getItem();
            if (item.getUseAnimation(this.useItem) != UseAnim.BLOCK) {
                return false;
            } else {
                return item.getUseDuration(this.useItem) - this.useItemRemaining >= 5;
            }
        } else {
            return false;
        }
    }

    public boolean isSuppressingSlidingDownLadder() {
        return this.isShiftKeyDown();
    }

    public boolean isFallFlying() {
        return this.getSharedFlag(7);
    }

    @Override
    public boolean isVisuallySwimming() {
        return super.isVisuallySwimming() || !this.isFallFlying() && this.getPose() == Pose.FALL_FLYING;
    }

    public int getFallFlyingTicks() {
        return this.fallFlyTicks;
    }

    public boolean randomTeleport(double x, double y, double z, boolean particleEffects) {
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        double g = y;
        boolean bl = false;
        BlockPos blockPos = new BlockPos(x, y, z);
        Level level = this.level;
        if (level.hasChunkAt(blockPos)) {
            boolean bl2 = false;

            while(!bl2 && blockPos.getY() > level.getMinBuildHeight()) {
                BlockPos blockPos2 = blockPos.below();
                BlockState blockState = level.getBlockState(blockPos2);
                if (blockState.getMaterial().blocksMotion()) {
                    bl2 = true;
                } else {
                    --g;
                    blockPos = blockPos2;
                }
            }

            if (bl2) {
                this.teleportTo(x, g, z);
                if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
                    bl = true;
                }
            }
        }

        if (!bl) {
            this.teleportTo(d, e, f);
            return false;
        } else {
            if (particleEffects) {
                level.broadcastEntityEvent(this, (byte)46);
            }

            if (this instanceof PathfinderMob) {
                ((PathfinderMob)this).getNavigation().stop();
            }

            return true;
        }
    }

    public boolean isAffectedByPotions() {
        return true;
    }

    public boolean attackable() {
        return true;
    }

    public void setRecordPlayingNearby(BlockPos songPosition, boolean playing) {
    }

    public boolean canTakeItem(ItemStack stack) {
        return false;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddMobPacket(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return pose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : super.getDimensions(pose).scale(this.getScale());
    }

    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of(Pose.STANDING);
    }

    public AABB getLocalBoundsForPose(Pose pose) {
        EntityDimensions entityDimensions = this.getDimensions(pose);
        return new AABB((double)(-entityDimensions.width / 2.0F), 0.0D, (double)(-entityDimensions.width / 2.0F), (double)(entityDimensions.width / 2.0F), (double)entityDimensions.height, (double)(entityDimensions.width / 2.0F));
    }

    public Optional<BlockPos> getSleepingPos() {
        return this.entityData.get(SLEEPING_POS_ID);
    }

    public void setSleepingPos(BlockPos pos) {
        this.entityData.set(SLEEPING_POS_ID, Optional.of(pos));
    }

    public void clearSleepingPos() {
        this.entityData.set(SLEEPING_POS_ID, Optional.empty());
    }

    public boolean isSleeping() {
        return this.getSleepingPos().isPresent();
    }

    public void startSleeping(BlockPos pos) {
        if (this.isPassenger()) {
            this.stopRiding();
        }

        BlockState blockState = this.level.getBlockState(pos);
        if (blockState.getBlock() instanceof BedBlock) {
            this.level.setBlock(pos, blockState.setValue(BedBlock.OCCUPIED, Boolean.valueOf(true)), 3);
        }

        this.setPose(Pose.SLEEPING);
        this.setPosToBed(pos);
        this.setSleepingPos(pos);
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
    }

    private void setPosToBed(BlockPos pos) {
        this.setPos((double)pos.getX() + 0.5D, (double)pos.getY() + 0.6875D, (double)pos.getZ() + 0.5D);
    }

    private boolean checkBedExists() {
        return this.getSleepingPos().map((pos) -> {
            return this.level.getBlockState(pos).getBlock() instanceof BedBlock;
        }).orElse(false);
    }

    public void stopSleeping() {
        this.getSleepingPos().filter(this.level::hasChunkAt).ifPresent((pos) -> {
            BlockState blockState = this.level.getBlockState(pos);
            if (blockState.getBlock() instanceof BedBlock) {
                this.level.setBlock(pos, blockState.setValue(BedBlock.OCCUPIED, Boolean.valueOf(false)), 3);
                Vec3 vec3 = BedBlock.findStandUpPosition(this.getType(), this.level, pos, this.getYRot()).orElseGet(() -> {
                    BlockPos blockPos2 = pos.above();
                    return new Vec3((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.1D, (double)blockPos2.getZ() + 0.5D);
                });
                Vec3 vec32 = Vec3.atBottomCenterOf(pos).subtract(vec3).normalize();
                float f = (float)Mth.wrapDegrees(Mth.atan2(vec32.z, vec32.x) * (double)(180F / (float)Math.PI) - 90.0D);
                this.setPos(vec3.x, vec3.y, vec3.z);
                this.setYRot(f);
                this.setXRot(0.0F);
            }

        });
        Vec3 vec3 = this.position();
        this.setPose(Pose.STANDING);
        this.setPos(vec3.x, vec3.y, vec3.z);
        this.clearSleepingPos();
    }

    @Nullable
    public Direction getBedOrientation() {
        BlockPos blockPos = this.getSleepingPos().orElse((BlockPos)null);
        return blockPos != null ? BedBlock.getBedOrientation(this.level, blockPos) : null;
    }

    @Override
    public boolean isInWall() {
        return !this.isSleeping() && super.isInWall();
    }

    @Override
    protected final float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return pose == Pose.SLEEPING ? 0.2F : this.getStandingEyeHeight(pose, dimensions);
    }

    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return super.getEyeHeight(pose, dimensions);
    }

    public ItemStack getProjectile(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    public ItemStack eat(Level world, ItemStack stack) {
        if (stack.isEdible()) {
            world.gameEvent(this, GameEvent.EAT, this.eyeBlockPosition());
            world.playSound((Player)null, this.getX(), this.getY(), this.getZ(), this.getEatingSound(stack), SoundSource.NEUTRAL, 1.0F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
            this.addEatEffect(stack, world, this);
            if (!(this instanceof Player) || !((Player)this).getAbilities().instabuild) {
                stack.shrink(1);
            }

            this.gameEvent(GameEvent.EAT);
        }

        return stack;
    }

    private void addEatEffect(ItemStack stack, Level world, LivingEntity targetEntity) {
        Item item = stack.getItem();
        if (item.isEdible()) {
            for(Pair<MobEffectInstance, Float> pair : item.getFoodProperties().getEffects()) {
                if (!world.isClientSide && pair.getFirst() != null && world.random.nextFloat() < pair.getSecond()) {
                    targetEntity.addEffect(new MobEffectInstance(pair.getFirst()));
                }
            }
        }

    }

    private static byte entityEventForEquipmentBreak(EquipmentSlot slot) {
        switch(slot) {
        case MAINHAND:
            return 47;
        case OFFHAND:
            return 48;
        case HEAD:
            return 49;
        case CHEST:
            return 50;
        case FEET:
            return 52;
        case LEGS:
            return 51;
        default:
            return 47;
        }
    }

    public void broadcastBreakEvent(EquipmentSlot slot) {
        this.level.broadcastEntityEvent(this, entityEventForEquipmentBreak(slot));
    }

    public void broadcastBreakEvent(InteractionHand hand) {
        this.broadcastBreakEvent(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (this.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
            float f = 0.5F;
            return this.getBoundingBox().inflate(0.5D, 0.5D, 0.5D);
        } else {
            return super.getBoundingBoxForCulling();
        }
    }

    public static EquipmentSlot getEquipmentSlotForItem(ItemStack stack) {
        Item item = stack.getItem();
        if (!stack.is(Items.CARVED_PUMPKIN) && (!(item instanceof BlockItem) || !(((BlockItem)item).getBlock() instanceof AbstractSkullBlock))) {
            if (item instanceof ArmorItem) {
                return ((ArmorItem)item).getSlot();
            } else if (stack.is(Items.ELYTRA)) {
                return EquipmentSlot.CHEST;
            } else {
                return stack.is(Items.SHIELD) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            }
        } else {
            return EquipmentSlot.HEAD;
        }
    }

    private static SlotAccess createEquipmentSlotAccess(LivingEntity entity, EquipmentSlot slot) {
        return slot != EquipmentSlot.HEAD && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND ? SlotAccess.forEquipmentSlot(entity, slot, (stack) -> {
            return stack.isEmpty() || Mob.getEquipmentSlotForItem(stack) == slot;
        }) : SlotAccess.forEquipmentSlot(entity, slot);
    }

    @Nullable
    private static EquipmentSlot getEquipmentSlot(int slotId) {
        if (slotId == 100 + EquipmentSlot.HEAD.getIndex()) {
            return EquipmentSlot.HEAD;
        } else if (slotId == 100 + EquipmentSlot.CHEST.getIndex()) {
            return EquipmentSlot.CHEST;
        } else if (slotId == 100 + EquipmentSlot.LEGS.getIndex()) {
            return EquipmentSlot.LEGS;
        } else if (slotId == 100 + EquipmentSlot.FEET.getIndex()) {
            return EquipmentSlot.FEET;
        } else if (slotId == 98) {
            return EquipmentSlot.MAINHAND;
        } else {
            return slotId == 99 ? EquipmentSlot.OFFHAND : null;
        }
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        EquipmentSlot equipmentSlot = getEquipmentSlot(mappedIndex);
        return equipmentSlot != null ? createEquipmentSlotAccess(this, equipmentSlot) : super.getSlot(mappedIndex);
    }

    @Override
    public boolean canFreeze() {
        if (this.isSpectator()) {
            return false;
        } else {
            boolean bl = !this.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.LEGS).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FREEZE_IMMUNE_WEARABLES);
            return bl && super.canFreeze();
        }
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return !this.level.isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
    }

    public void recreateFromPacket(ClientboundAddMobPacket packet) {
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        float g = (float)(packet.getyRot() * 360) / 256.0F;
        float h = (float)(packet.getxRot() * 360) / 256.0F;
        this.setPacketCoordinates(d, e, f);
        this.yBodyRot = (float)(packet.getyHeadRot() * 360) / 256.0F;
        this.yHeadRot = (float)(packet.getyHeadRot() * 360) / 256.0F;
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.setId(packet.getId());
        this.setUUID(packet.getUUID());
        this.absMoveTo(d, e, f, g, h);
        this.setDeltaMovement((double)((float)packet.getXd() / 8000.0F), (double)((float)packet.getYd() / 8000.0F), (double)((float)packet.getZd() / 8000.0F));
    }

    public static record Fallsounds(SoundEvent small, SoundEvent big) {
    }
}
