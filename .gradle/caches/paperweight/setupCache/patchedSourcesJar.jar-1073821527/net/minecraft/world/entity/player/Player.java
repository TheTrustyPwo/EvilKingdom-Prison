package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public abstract class Player extends LivingEntity {
    public static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";
    public static final int MAX_NAME_LENGTH = 16;
    public static final int MAX_HEALTH = 20;
    public static final int SLEEP_DURATION = 100;
    public static final int WAKE_UP_DURATION = 10;
    public static final int ENDER_SLOT_OFFSET = 200;
    public static final float CROUCH_BB_HEIGHT = 1.5F;
    public static final float SWIMMING_BB_WIDTH = 0.6F;
    public static final float SWIMMING_BB_HEIGHT = 0.6F;
    public static final float DEFAULT_EYE_HEIGHT = 1.62F;
    public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F);
    private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.STANDING, STANDING_DIMENSIONS).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.5F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
    private static final int FLY_ACHIEVEMENT_SPEED = 25;
    private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> DATA_PLAYER_MAIN_HAND = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_LEFT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
    protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_RIGHT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
    private long timeEntitySatOnShoulder;
    private final Inventory inventory = new Inventory(this);
    protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
    public final InventoryMenu inventoryMenu;
    public AbstractContainerMenu containerMenu;
    protected FoodData foodData = new FoodData();
    protected int jumpTriggerTime;
    public float oBob;
    public float bob;
    public int takeXpDelay;
    public double xCloakO;
    public double yCloakO;
    public double zCloakO;
    public double xCloak;
    public double yCloak;
    public double zCloak;
    public int sleepCounter;
    protected boolean wasUnderwater;
    private final Abilities abilities = new Abilities();
    public int experienceLevel;
    public int totalExperience;
    public float experienceProgress;
    protected int enchantmentSeed;
    protected final float defaultFlySpeed = 0.02F;
    private int lastLevelUpTime;
    public GameProfile gameProfile;
    private boolean reducedDebugInfo;
    private ItemStack lastItemInMainHand = ItemStack.EMPTY;
    private final ItemCooldowns cooldowns = this.createItemCooldowns();
    @Nullable
    public FishingHook fishing;

    public Player(Level world, BlockPos pos, float yaw, GameProfile profile) {
        super(EntityType.PLAYER, world);
        this.setUUID(createPlayerUUID(profile));
        this.gameProfile = profile;
        this.inventoryMenu = new InventoryMenu(this.inventory, !world.isClientSide, this);
        this.containerMenu = this.inventoryMenu;
        this.moveTo((double)pos.getX() + 0.5D, (double)(pos.getY() + 1), (double)pos.getZ() + 0.5D, yaw, 0.0F);
        this.rotOffs = 180.0F;
    }

    public boolean blockActionRestricted(Level world, BlockPos pos, GameType gameMode) {
        if (!gameMode.isBlockPlacingRestricted()) {
            return false;
        } else if (gameMode == GameType.SPECTATOR) {
            return true;
        } else if (this.mayBuild()) {
            return false;
        } else {
            ItemStack itemStack = this.getMainHandItem();
            return itemStack.isEmpty() || !itemStack.hasAdventureModeBreakTagForBlock(world.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY), new BlockInWorld(world, pos, false));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.MOVEMENT_SPEED, (double)0.1F).add(Attributes.ATTACK_SPEED).add(Attributes.LUCK);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
        this.entityData.define(DATA_SCORE_ID, 0);
        this.entityData.define(DATA_PLAYER_MODE_CUSTOMISATION, (byte)0);
        this.entityData.define(DATA_PLAYER_MAIN_HAND, (byte)1);
        this.entityData.define(DATA_SHOULDER_LEFT, new CompoundTag());
        this.entityData.define(DATA_SHOULDER_RIGHT, new CompoundTag());
    }

    @Override
    public void tick() {
        this.noPhysics = this.isSpectator();
        if (this.isSpectator()) {
            this.onGround = false;
        }

        if (this.takeXpDelay > 0) {
            --this.takeXpDelay;
        }

        if (this.isSleeping()) {
            ++this.sleepCounter;
            if (this.sleepCounter > 100) {
                this.sleepCounter = 100;
            }

            if (!this.level.isClientSide && this.level.isDay()) {
                this.stopSleepInBed(false, true);
            }
        } else if (this.sleepCounter > 0) {
            ++this.sleepCounter;
            if (this.sleepCounter >= 110) {
                this.sleepCounter = 0;
            }
        }

        this.updateIsUnderwater();
        super.tick();
        if (!this.level.isClientSide && this.containerMenu != null && !this.containerMenu.stillValid(this)) {
            this.closeContainer();
            this.containerMenu = this.inventoryMenu;
        }

        this.moveCloak();
        if (!this.level.isClientSide) {
            this.foodData.tick(this);
            this.awardStat(Stats.PLAY_TIME);
            this.awardStat(Stats.TOTAL_WORLD_TIME);
            if (this.isAlive()) {
                this.awardStat(Stats.TIME_SINCE_DEATH);
            }

            if (this.isDiscrete()) {
                this.awardStat(Stats.CROUCH_TIME);
            }

            if (!this.isSleeping()) {
                this.awardStat(Stats.TIME_SINCE_REST);
            }
        }

        int i = 29999999;
        double d = Mth.clamp(this.getX(), -2.9999999E7D, 2.9999999E7D);
        double e = Mth.clamp(this.getZ(), -2.9999999E7D, 2.9999999E7D);
        if (d != this.getX() || e != this.getZ()) {
            this.setPos(d, this.getY(), e);
        }

        ++this.attackStrengthTicker;
        ItemStack itemStack = this.getMainHandItem();
        if (!ItemStack.matches(this.lastItemInMainHand, itemStack)) {
            if (!ItemStack.isSameIgnoreDurability(this.lastItemInMainHand, itemStack)) {
                this.resetAttackStrengthTicker();
            }

            this.lastItemInMainHand = itemStack.copy();
        }

        this.turtleHelmetTick();
        this.cooldowns.tick();
        this.updatePlayerPose();
    }

    public boolean isSecondaryUseActive() {
        return this.isShiftKeyDown();
    }

    protected boolean wantsToStopRiding() {
        return this.isShiftKeyDown();
    }

    protected boolean isStayingOnGroundSurface() {
        return this.isShiftKeyDown();
    }

    protected boolean updateIsUnderwater() {
        this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
        return this.wasUnderwater;
    }

    private void turtleHelmetTick() {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.HEAD);
        if (itemStack.is(Items.TURTLE_HELMET) && !this.isEyeInFluid(FluidTags.WATER)) {
            this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
        }

    }

    protected ItemCooldowns createItemCooldowns() {
        return new ItemCooldowns();
    }

    private void moveCloak() {
        this.xCloakO = this.xCloak;
        this.yCloakO = this.yCloak;
        this.zCloakO = this.zCloak;
        double d = this.getX() - this.xCloak;
        double e = this.getY() - this.yCloak;
        double f = this.getZ() - this.zCloak;
        double g = 10.0D;
        if (d > 10.0D) {
            this.xCloak = this.getX();
            this.xCloakO = this.xCloak;
        }

        if (f > 10.0D) {
            this.zCloak = this.getZ();
            this.zCloakO = this.zCloak;
        }

        if (e > 10.0D) {
            this.yCloak = this.getY();
            this.yCloakO = this.yCloak;
        }

        if (d < -10.0D) {
            this.xCloak = this.getX();
            this.xCloakO = this.xCloak;
        }

        if (f < -10.0D) {
            this.zCloak = this.getZ();
            this.zCloakO = this.zCloak;
        }

        if (e < -10.0D) {
            this.yCloak = this.getY();
            this.yCloakO = this.yCloak;
        }

        this.xCloak += d * 0.25D;
        this.zCloak += f * 0.25D;
        this.yCloak += e * 0.25D;
    }

    protected void updatePlayerPose() {
        if (this.canEnterPose(Pose.SWIMMING)) {
            Pose pose;
            if (this.isFallFlying()) {
                pose = Pose.FALL_FLYING;
            } else if (this.isSleeping()) {
                pose = Pose.SLEEPING;
            } else if (this.isSwimming()) {
                pose = Pose.SWIMMING;
            } else if (this.isAutoSpinAttack()) {
                pose = Pose.SPIN_ATTACK;
            } else if (this.isShiftKeyDown() && !this.abilities.flying) {
                pose = Pose.CROUCHING;
            } else {
                pose = Pose.STANDING;
            }

            Pose pose8;
            if (!this.isSpectator() && !this.isPassenger() && !this.canEnterPose(pose)) {
                if (this.canEnterPose(Pose.CROUCHING)) {
                    pose8 = Pose.CROUCHING;
                } else {
                    pose8 = Pose.SWIMMING;
                }
            } else {
                pose8 = pose;
            }

            this.setPose(pose8);
        }
    }

    @Override
    public int getPortalWaitTime() {
        return this.abilities.invulnerable ? 1 : 80;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.PLAYER_SWIM;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    @Override
    protected SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
    }

    @Override
    public int getDimensionChangingDelay() {
        return 10;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        this.level.playSound(this, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch);
    }

    public void playNotifySound(SoundEvent event, SoundSource category, float volume, float pitch) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.PLAYERS;
    }

    @Override
    public int getFireImmuneTicks() {
        return 20;
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 9) {
            this.completeUsingItem();
        } else if (status == 23) {
            this.reducedDebugInfo = false;
        } else if (status == 22) {
            this.reducedDebugInfo = true;
        } else if (status == 43) {
            this.addParticlesAroundSelf(ParticleTypes.CLOUD);
        } else {
            super.handleEntityEvent(status);
        }

    }

    private void addParticlesAroundSelf(ParticleOptions parameters) {
        for(int i = 0; i < 5; ++i) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(parameters, this.getRandomX(1.0D), this.getRandomY() + 1.0D, this.getRandomZ(1.0D), d, e, f);
        }

    }

    public void closeContainer() {
        this.containerMenu = this.inventoryMenu;
    }

    @Override
    public void rideTick() {
        if (!this.level.isClientSide && this.wantsToStopRiding() && this.isPassenger()) {
            this.stopRiding();
            this.setShiftKeyDown(false);
        } else {
            double d = this.getX();
            double e = this.getY();
            double f = this.getZ();
            super.rideTick();
            this.oBob = this.bob;
            this.bob = 0.0F;
            this.checkRidingStatistics(this.getX() - d, this.getY() - e, this.getZ() - f);
        }
    }

    @Override
    protected void serverAiStep() {
        super.serverAiStep();
        this.updateSwingTime();
        this.yHeadRot = this.getYRot();
    }

    @Override
    public void aiStep() {
        if (this.jumpTriggerTime > 0) {
            --this.jumpTriggerTime;
        }

        if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
            if (this.getHealth() < this.getMaxHealth() && this.tickCount % 20 == 0) {
                this.heal(1.0F);
            }

            if (this.foodData.needsFood() && this.tickCount % 10 == 0) {
                this.foodData.setFoodLevel(this.foodData.getFoodLevel() + 1);
            }
        }

        this.inventory.tick();
        this.oBob = this.bob;
        super.aiStep();
        this.flyingSpeed = 0.02F;
        if (this.isSprinting()) {
            this.flyingSpeed += 0.006F;
        }

        this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        float g;
        if (this.onGround && !this.isDeadOrDying() && !this.isSwimming()) {
            g = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
        } else {
            g = 0.0F;
        }

        this.bob += (g - this.bob) * 0.4F;
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            AABB aABB;
            if (this.isPassenger() && !this.getVehicle().isRemoved()) {
                aABB = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0D, 0.0D, 1.0D);
            } else {
                aABB = this.getBoundingBox().inflate(1.0D, 0.5D, 1.0D);
            }

            List<Entity> list = this.level.getEntities(this, aABB);
            List<Entity> list2 = Lists.newArrayList();

            for(int i = 0; i < list.size(); ++i) {
                Entity entity = list.get(i);
                if (entity.getType() == EntityType.EXPERIENCE_ORB) {
                    list2.add(entity);
                } else if (!entity.isRemoved()) {
                    this.touch(entity);
                }
            }

            if (!list2.isEmpty()) {
                this.touch(Util.getRandom(list2, this.random));
            }
        }

        this.playShoulderEntityAmbientSound(this.getShoulderEntityLeft());
        this.playShoulderEntityAmbientSound(this.getShoulderEntityRight());
        if (!this.level.isClientSide && (this.fallDistance > 0.5F || this.isInWater()) || this.abilities.flying || this.isSleeping() || this.isInPowderSnow) {
            this.removeEntitiesOnShoulder();
        }

    }

    private void playShoulderEntityAmbientSound(@Nullable CompoundTag entityNbt) {
        if (entityNbt != null && (!entityNbt.contains("Silent") || !entityNbt.getBoolean("Silent")) && this.level.random.nextInt(200) == 0) {
            String string = entityNbt.getString("id");
            EntityType.byString(string).filter((entityType) -> {
                return entityType == EntityType.PARROT;
            }).ifPresent((entityType) -> {
                if (!Parrot.imitateNearbyMobs(this.level, this)) {
                    this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), Parrot.getAmbient(this.level, this.level.random), this.getSoundSource(), 1.0F, Parrot.getPitch(this.level.random));
                }

            });
        }

    }

    private void touch(Entity entity) {
        entity.playerTouch(this);
    }

    public int getScore() {
        return this.entityData.get(DATA_SCORE_ID);
    }

    public void setScore(int score) {
        this.entityData.set(DATA_SCORE_ID, score);
    }

    public void increaseScore(int score) {
        int i = this.getScore();
        this.entityData.set(DATA_SCORE_ID, i + score);
    }

    public void startAutoSpinAttack(int riptideTicks) {
        this.autoSpinAttackTicks = riptideTicks;
        if (!this.level.isClientSide) {
            this.removeEntitiesOnShoulder();
            this.setLivingEntityFlag(4, true);
        }

    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.reapplyPosition();
        if (!this.isSpectator()) {
            this.dropAllDeathLoot(source);
        }

        if (source != null) {
            this.setDeltaMovement((double)(-Mth.cos((this.hurtDir + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F), (double)0.1F, (double)(-Mth.sin((this.hurtDir + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F));
        } else {
            this.setDeltaMovement(0.0D, 0.1D, 0.0D);
        }

        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setSharedFlagOnFire(false);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (!this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            this.destroyVanishingCursedItems();
            this.inventory.dropAll();
        }

    }

    protected void destroyVanishingCursedItems() {
        for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (!itemStack.isEmpty() && EnchantmentHelper.hasVanishingCurse(itemStack)) {
                this.inventory.removeItemNoUpdate(i);
            }
        }

    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (source == DamageSource.ON_FIRE) {
            return SoundEvents.PLAYER_HURT_ON_FIRE;
        } else if (source == DamageSource.DROWN) {
            return SoundEvents.PLAYER_HURT_DROWN;
        } else if (source == DamageSource.SWEET_BERRY_BUSH) {
            return SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH;
        } else {
            return source == DamageSource.FREEZE ? SoundEvents.PLAYER_HURT_FREEZE : SoundEvents.PLAYER_HURT;
        }
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Nullable
    public ItemEntity drop(ItemStack stack, boolean retainOwnership) {
        return this.drop(stack, false, retainOwnership);
    }

    @Nullable
    public ItemEntity drop(ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        if (stack.isEmpty()) {
            return null;
        } else {
            if (this.level.isClientSide) {
                this.swing(InteractionHand.MAIN_HAND);
            }

            double d = this.getEyeY() - (double)0.3F;
            ItemEntity itemEntity = new ItemEntity(this.level, this.getX(), d, this.getZ(), stack);
            itemEntity.setPickUpDelay(40);
            if (retainOwnership) {
                itemEntity.setThrower(this.getUUID());
            }

            if (throwRandomly) {
                float f = this.random.nextFloat() * 0.5F;
                float g = this.random.nextFloat() * ((float)Math.PI * 2F);
                itemEntity.setDeltaMovement((double)(-Mth.sin(g) * f), (double)0.2F, (double)(Mth.cos(g) * f));
            } else {
                float h = 0.3F;
                float i = Mth.sin(this.getXRot() * ((float)Math.PI / 180F));
                float j = Mth.cos(this.getXRot() * ((float)Math.PI / 180F));
                float k = Mth.sin(this.getYRot() * ((float)Math.PI / 180F));
                float l = Mth.cos(this.getYRot() * ((float)Math.PI / 180F));
                float m = this.random.nextFloat() * ((float)Math.PI * 2F);
                float n = 0.02F * this.random.nextFloat();
                itemEntity.setDeltaMovement((double)(-k * j * 0.3F) + Math.cos((double)m) * (double)n, (double)(-i * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(l * j * 0.3F) + Math.sin((double)m) * (double)n);
            }

            return itemEntity;
        }
    }

    public float getDestroySpeed(BlockState block) {
        float f = this.inventory.getDestroySpeed(block);
        if (f > 1.0F) {
            int i = EnchantmentHelper.getBlockEfficiency(this);
            ItemStack itemStack = this.getMainHandItem();
            if (i > 0 && !itemStack.isEmpty()) {
                f += (float)(i * i + 1);
            }
        }

        if (MobEffectUtil.hasDigSpeed(this)) {
            f *= 1.0F + (float)(MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
        }

        if (this.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            float g;
            switch(this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
            case 0:
                g = 0.3F;
                break;
            case 1:
                g = 0.09F;
                break;
            case 2:
                g = 0.0027F;
                break;
            case 3:
            default:
                g = 8.1E-4F;
            }

            f *= g;
        }

        if (this.isEyeInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this)) {
            f /= 5.0F;
        }

        if (!this.onGround) {
            f /= 5.0F;
        }

        return f;
    }

    public boolean hasCorrectToolForDrops(BlockState state) {
        return !state.requiresCorrectToolForDrops() || this.inventory.getSelected().isCorrectToolForDrops(state);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setUUID(createPlayerUUID(this.gameProfile));
        ListTag listTag = nbt.getList("Inventory", 10);
        this.inventory.load(listTag);
        this.inventory.selected = nbt.getInt("SelectedItemSlot");
        this.sleepCounter = nbt.getShort("SleepTimer");
        this.experienceProgress = nbt.getFloat("XpP");
        this.experienceLevel = nbt.getInt("XpLevel");
        this.totalExperience = nbt.getInt("XpTotal");
        this.enchantmentSeed = nbt.getInt("XpSeed");
        if (this.enchantmentSeed == 0) {
            this.enchantmentSeed = this.random.nextInt();
        }

        this.setScore(nbt.getInt("Score"));
        this.foodData.readAdditionalSaveData(nbt);
        this.abilities.loadSaveData(nbt);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double)this.abilities.getWalkingSpeed());
        if (nbt.contains("EnderItems", 9)) {
            this.enderChestInventory.fromTag(nbt.getList("EnderItems", 10));
        }

        if (nbt.contains("ShoulderEntityLeft", 10)) {
            this.setShoulderEntityLeft(nbt.getCompound("ShoulderEntityLeft"));
        }

        if (nbt.contains("ShoulderEntityRight", 10)) {
            this.setShoulderEntityRight(nbt.getCompound("ShoulderEntityRight"));
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        nbt.put("Inventory", this.inventory.save(new ListTag()));
        nbt.putInt("SelectedItemSlot", this.inventory.selected);
        nbt.putShort("SleepTimer", (short)this.sleepCounter);
        nbt.putFloat("XpP", this.experienceProgress);
        nbt.putInt("XpLevel", this.experienceLevel);
        nbt.putInt("XpTotal", this.totalExperience);
        nbt.putInt("XpSeed", this.enchantmentSeed);
        nbt.putInt("Score", this.getScore());
        this.foodData.addAdditionalSaveData(nbt);
        this.abilities.addSaveData(nbt);
        nbt.put("EnderItems", this.enderChestInventory.createTag());
        if (!this.getShoulderEntityLeft().isEmpty()) {
            nbt.put("ShoulderEntityLeft", this.getShoulderEntityLeft());
        }

        if (!this.getShoulderEntityRight().isEmpty()) {
            nbt.put("ShoulderEntityRight", this.getShoulderEntityRight());
        }

    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (super.isInvulnerableTo(damageSource)) {
            return true;
        } else if (damageSource == DamageSource.DROWN) {
            return !this.level.getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
        } else if (damageSource.isFall()) {
            return !this.level.getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
        } else if (damageSource.isFire()) {
            return !this.level.getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
        } else if (damageSource == DamageSource.FREEZE) {
            return !this.level.getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE);
        } else {
            return false;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.abilities.invulnerable && !source.isBypassInvul()) {
            return false;
        } else {
            this.noActionTime = 0;
            if (this.isDeadOrDying()) {
                return false;
            } else {
                if (!this.level.isClientSide) {
                    this.removeEntitiesOnShoulder();
                }

                if (source.scalesWithDifficulty()) {
                    if (this.level.getDifficulty() == Difficulty.PEACEFUL) {
                        amount = 0.0F;
                    }

                    if (this.level.getDifficulty() == Difficulty.EASY) {
                        amount = Math.min(amount / 2.0F + 1.0F, amount);
                    }

                    if (this.level.getDifficulty() == Difficulty.HARD) {
                        amount = amount * 3.0F / 2.0F;
                    }
                }

                return amount == 0.0F ? false : super.hurt(source, amount);
            }
        }
    }

    @Override
    protected void blockUsingShield(LivingEntity attacker) {
        super.blockUsingShield(attacker);
        if (attacker.getMainHandItem().getItem() instanceof AxeItem) {
            this.disableShield(true);
        }

    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
    }

    public boolean canHarmPlayer(Player player) {
        Team team = this.getTeam();
        Team team2 = player.getTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlliedTo(team2) ? true : team.isAllowFriendlyFire();
        }
    }

    @Override
    protected void hurtArmor(DamageSource source, float amount) {
        this.inventory.hurtArmor(source, amount, Inventory.ALL_ARMOR_SLOTS);
    }

    @Override
    protected void hurtHelmet(DamageSource source, float amount) {
        this.inventory.hurtArmor(source, amount, Inventory.HELMET_SLOT_ONLY);
    }

    @Override
    protected void hurtCurrentlyUsedShield(float amount) {
        if (this.useItem.is(Items.SHIELD)) {
            if (!this.level.isClientSide) {
                this.awardStat(Stats.ITEM_USED.get(this.useItem.getItem()));
            }

            if (amount >= 3.0F) {
                int i = 1 + Mth.floor(amount);
                InteractionHand interactionHand = this.getUsedItemHand();
                this.useItem.hurtAndBreak(i, this, (player) -> {
                    player.broadcastBreakEvent(interactionHand);
                });
                if (this.useItem.isEmpty()) {
                    if (interactionHand == InteractionHand.MAIN_HAND) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }

                    this.useItem = ItemStack.EMPTY;
                    this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
                }
            }

        }
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        if (!this.isInvulnerableTo(source)) {
            amount = this.getDamageAfterArmorAbsorb(source, amount);
            amount = this.getDamageAfterMagicAbsorb(source, amount);
            float var8 = Math.max(amount - this.getAbsorptionAmount(), 0.0F);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - (amount - var8));
            float g = amount - var8;
            if (g > 0.0F && g < 3.4028235E37F) {
                this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(g * 10.0F));
            }

            if (var8 != 0.0F) {
                this.causeFoodExhaustion(source.getFoodExhaustion());
                float h = this.getHealth();
                this.setHealth(this.getHealth() - var8);
                this.getCombatTracker().recordDamage(source, h, var8);
                if (var8 < 3.4028235E37F) {
                    this.awardStat(Stats.DAMAGE_TAKEN, Math.round(var8 * 10.0F));
                }

            }
        }
    }

    @Override
    protected boolean onSoulSpeedBlock() {
        return !this.abilities.flying && super.onSoulSpeedBlock();
    }

    public void openTextEdit(SignBlockEntity sign) {
    }

    public void openMinecartCommandBlock(BaseCommandBlock commandBlockExecutor) {
    }

    public void openCommandBlock(CommandBlockEntity commandBlock) {
    }

    public void openStructureBlock(StructureBlockEntity structureBlock) {
    }

    public void openJigsawBlock(JigsawBlockEntity jigsaw) {
    }

    public void openHorseInventory(AbstractHorse horse, Container inventory) {
    }

    public OptionalInt openMenu(@Nullable MenuProvider factory) {
        return OptionalInt.empty();
    }

    public void sendMerchantOffers(int syncId, MerchantOffers offers, int levelProgress, int experience, boolean leveled, boolean refreshable) {
    }

    public void openItemGui(ItemStack book, InteractionHand hand) {
    }

    public InteractionResult interactOn(Entity entity, InteractionHand hand) {
        if (this.isSpectator()) {
            if (entity instanceof MenuProvider) {
                this.openMenu((MenuProvider)entity);
            }

            return InteractionResult.PASS;
        } else {
            ItemStack itemStack = this.getItemInHand(hand);
            ItemStack itemStack2 = itemStack.copy();
            InteractionResult interactionResult = entity.interact(this, hand);
            if (interactionResult.consumesAction()) {
                if (this.abilities.instabuild && itemStack == this.getItemInHand(hand) && itemStack.getCount() < itemStack2.getCount()) {
                    itemStack.setCount(itemStack2.getCount());
                }

                return interactionResult;
            } else {
                if (!itemStack.isEmpty() && entity instanceof LivingEntity) {
                    if (this.abilities.instabuild) {
                        itemStack = itemStack2;
                    }

                    InteractionResult interactionResult2 = itemStack.interactLivingEntity(this, (LivingEntity)entity, hand);
                    if (interactionResult2.consumesAction()) {
                        if (itemStack.isEmpty() && !this.abilities.instabuild) {
                            this.setItemInHand(hand, ItemStack.EMPTY);
                        }

                        return interactionResult2;
                    }
                }

                return InteractionResult.PASS;
            }
        }
    }

    @Override
    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public void removeVehicle() {
        super.removeVehicle();
        this.boardingCooldown = 0;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isSleeping();
    }

    @Override
    public boolean isAffectedByFluids() {
        return !this.abilities.flying;
    }

    @Override
    protected Vec3 maybeBackOffFromEdge(Vec3 movement, MoverType type) {
        if (!this.abilities.flying && (type == MoverType.SELF || type == MoverType.PLAYER) && this.isStayingOnGroundSurface() && this.isAboveGround()) {
            double d = movement.x;
            double e = movement.z;
            double f = 0.05D;

            while(d != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(d, (double)(-this.maxUpStep), 0.0D))) {
                if (d < 0.05D && d >= -0.05D) {
                    d = 0.0D;
                } else if (d > 0.0D) {
                    d -= 0.05D;
                } else {
                    d += 0.05D;
                }
            }

            while(e != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(0.0D, (double)(-this.maxUpStep), e))) {
                if (e < 0.05D && e >= -0.05D) {
                    e = 0.0D;
                } else if (e > 0.0D) {
                    e -= 0.05D;
                } else {
                    e += 0.05D;
                }
            }

            while(d != 0.0D && e != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(d, (double)(-this.maxUpStep), e))) {
                if (d < 0.05D && d >= -0.05D) {
                    d = 0.0D;
                } else if (d > 0.0D) {
                    d -= 0.05D;
                } else {
                    d += 0.05D;
                }

                if (e < 0.05D && e >= -0.05D) {
                    e = 0.0D;
                } else if (e > 0.0D) {
                    e -= 0.05D;
                } else {
                    e += 0.05D;
                }
            }

            movement = new Vec3(d, movement.y, e);
        }

        return movement;
    }

    private boolean isAboveGround() {
        return this.onGround || this.fallDistance < this.maxUpStep && !this.level.noCollision(this, this.getBoundingBox().move(0.0D, (double)(this.fallDistance - this.maxUpStep), 0.0D));
    }

    public void attack(Entity target) {
        if (target.isAttackable()) {
            if (!target.skipAttackInteraction(this)) {
                float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float g;
                if (target instanceof LivingEntity) {
                    g = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)target).getMobType());
                } else {
                    g = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), MobType.UNDEFINED);
                }

                float i = this.getAttackStrengthScale(0.5F);
                f *= 0.2F + i * i * 0.8F;
                g *= i;
                this.resetAttackStrengthTicker();
                if (f > 0.0F || g > 0.0F) {
                    boolean bl = i > 0.9F;
                    boolean bl2 = false;
                    int j = 0;
                    j += EnchantmentHelper.getKnockbackBonus(this);
                    if (this.isSprinting() && bl) {
                        this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                        ++j;
                        bl2 = true;
                    }

                    boolean bl3 = bl && this.fallDistance > 0.0F && !this.onGround && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && target instanceof LivingEntity;
                    bl3 = bl3 && !this.isSprinting();
                    if (bl3) {
                        f *= 1.5F;
                    }

                    f += g;
                    boolean bl4 = false;
                    double d = (double)(this.walkDist - this.walkDistO);
                    if (bl && !bl3 && !bl2 && this.onGround && d < (double)this.getSpeed()) {
                        ItemStack itemStack = this.getItemInHand(InteractionHand.MAIN_HAND);
                        if (itemStack.getItem() instanceof SwordItem) {
                            bl4 = true;
                        }
                    }

                    float k = 0.0F;
                    boolean bl5 = false;
                    int l = EnchantmentHelper.getFireAspect(this);
                    if (target instanceof LivingEntity) {
                        k = ((LivingEntity)target).getHealth();
                        if (l > 0 && !target.isOnFire()) {
                            bl5 = true;
                            target.setSecondsOnFire(1);
                        }
                    }

                    Vec3 vec3 = target.getDeltaMovement();
                    boolean bl6 = target.hurt(DamageSource.playerAttack(this), f);
                    if (bl6) {
                        if (j > 0) {
                            if (target instanceof LivingEntity) {
                                ((LivingEntity)target).knockback((double)((float)j * 0.5F), (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
                            } else {
                                target.push((double)(-Mth.sin(this.getYRot() * ((float)Math.PI / 180F)) * (float)j * 0.5F), 0.1D, (double)(Mth.cos(this.getYRot() * ((float)Math.PI / 180F)) * (float)j * 0.5F));
                            }

                            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
                            this.setSprinting(false);
                        }

                        if (bl4) {
                            float m = 1.0F + EnchantmentHelper.getSweepingDamageRatio(this) * f;

                            for(LivingEntity livingEntity : this.level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D))) {
                                if (livingEntity != this && livingEntity != target && !this.isAlliedTo(livingEntity) && (!(livingEntity instanceof ArmorStand) || !((ArmorStand)livingEntity).isMarker()) && this.distanceToSqr(livingEntity) < 9.0D) {
                                    livingEntity.knockback((double)0.4F, (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
                                    livingEntity.hurt(DamageSource.playerAttack(this), m);
                                }
                            }

                            this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                            this.sweepAttack();
                        }

                        if (target instanceof ServerPlayer && target.hurtMarked) {
                            ((ServerPlayer)target).connection.send(new ClientboundSetEntityMotionPacket(target));
                            target.hurtMarked = false;
                            target.setDeltaMovement(vec3);
                        }

                        if (bl3) {
                            this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                            this.crit(target);
                        }

                        if (!bl3 && !bl4) {
                            if (bl) {
                                this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F);
                            } else {
                                this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F);
                            }
                        }

                        if (g > 0.0F) {
                            this.magicCrit(target);
                        }

                        this.setLastHurtMob(target);
                        if (target instanceof LivingEntity) {
                            EnchantmentHelper.doPostHurtEffects((LivingEntity)target, this);
                        }

                        EnchantmentHelper.doPostDamageEffects(this, target);
                        ItemStack itemStack2 = this.getMainHandItem();
                        Entity entity = target;
                        if (target instanceof EnderDragonPart) {
                            entity = ((EnderDragonPart)target).parentMob;
                        }

                        if (!this.level.isClientSide && !itemStack2.isEmpty() && entity instanceof LivingEntity) {
                            itemStack2.hurtEnemy((LivingEntity)entity, this);
                            if (itemStack2.isEmpty()) {
                                this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            }
                        }

                        if (target instanceof LivingEntity) {
                            float n = k - ((LivingEntity)target).getHealth();
                            this.awardStat(Stats.DAMAGE_DEALT, Math.round(n * 10.0F));
                            if (l > 0) {
                                target.setSecondsOnFire(l * 4);
                            }

                            if (this.level instanceof ServerLevel && n > 2.0F) {
                                int o = (int)((double)n * 0.5D);
                                ((ServerLevel)this.level).sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5D), target.getZ(), o, 0.1D, 0.0D, 0.1D, 0.2D);
                            }
                        }

                        this.causeFoodExhaustion(0.1F);
                    } else {
                        this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                        if (bl5) {
                            target.clearFire();
                        }
                    }
                }

            }
        }
    }

    @Override
    protected void doAutoAttackOnTouch(LivingEntity target) {
        this.attack(target);
    }

    public void disableShield(boolean sprinting) {
        float f = 0.25F + (float)EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
        if (sprinting) {
            f += 0.75F;
        }

        if (this.random.nextFloat() < f) {
            this.getCooldowns().addCooldown(Items.SHIELD, 100);
            this.stopUsingItem();
            this.level.broadcastEntityEvent(this, (byte)30);
        }

    }

    public void crit(Entity target) {
    }

    public void magicCrit(Entity target) {
    }

    public void sweepAttack() {
        double d = (double)(-Mth.sin(this.getYRot() * ((float)Math.PI / 180F)));
        double e = (double)Mth.cos(this.getYRot() * ((float)Math.PI / 180F));
        if (this.level instanceof ServerLevel) {
            ((ServerLevel)this.level).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d, this.getY(0.5D), this.getZ() + e, 0, d, 0.0D, e, 0.0D);
        }

    }

    public void respawn() {
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.inventoryMenu.removed(this);
        if (this.containerMenu != null) {
            this.containerMenu.removed(this);
        }

    }

    public boolean isLocalPlayer() {
        return false;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Abilities getAbilities() {
        return this.abilities;
    }

    public void updateTutorialInventoryAction(ItemStack cursorStack, ItemStack slotStack, ClickAction clickType) {
    }

    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos pos) {
        this.startSleeping(pos);
        this.sleepCounter = 0;
        return Either.right(Unit.INSTANCE);
    }

    public void stopSleepInBed(boolean skipSleepTimer, boolean updateSleepingPlayers) {
        super.stopSleeping();
        if (this.level instanceof ServerLevel && updateSleepingPlayers) {
            ((ServerLevel)this.level).updateSleepingPlayerList();
        }

        this.sleepCounter = skipSleepTimer ? 0 : 100;
    }

    @Override
    public void stopSleeping() {
        this.stopSleepInBed(true, true);
    }

    public static Optional<Vec3> findRespawnPositionAndUseSpawnBlock(ServerLevel world, BlockPos pos, float angle, boolean forced, boolean alive) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if (block instanceof RespawnAnchorBlock && blockState.getValue(RespawnAnchorBlock.CHARGE) > 0 && RespawnAnchorBlock.canSetSpawn(world)) {
            Optional<Vec3> optional = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, world, pos);
            if (!alive && optional.isPresent()) {
                world.setBlock(pos, blockState.setValue(RespawnAnchorBlock.CHARGE, Integer.valueOf(blockState.getValue(RespawnAnchorBlock.CHARGE) - 1)), 3);
            }

            return optional;
        } else if (block instanceof BedBlock && BedBlock.canSetSpawn(world)) {
            return BedBlock.findStandUpPosition(EntityType.PLAYER, world, pos, angle);
        } else if (!forced) {
            return Optional.empty();
        } else {
            boolean bl = block.isPossibleToRespawnInThis();
            boolean bl2 = world.getBlockState(pos.above()).getBlock().isPossibleToRespawnInThis();
            return bl && bl2 ? Optional.of(new Vec3((double)pos.getX() + 0.5D, (double)pos.getY() + 0.1D, (double)pos.getZ() + 0.5D)) : Optional.empty();
        }
    }

    public boolean isSleepingLongEnough() {
        return this.isSleeping() && this.sleepCounter >= 100;
    }

    public int getSleepTimer() {
        return this.sleepCounter;
    }

    public void displayClientMessage(Component message, boolean actionBar) {
    }

    public void awardStat(ResourceLocation stat) {
        this.awardStat(Stats.CUSTOM.get(stat));
    }

    public void awardStat(ResourceLocation stat, int amount) {
        this.awardStat(Stats.CUSTOM.get(stat), amount);
    }

    public void awardStat(Stat<?> stat) {
        this.awardStat(stat, 1);
    }

    public void awardStat(Stat<?> stat, int amount) {
    }

    public void resetStat(Stat<?> stat) {
    }

    public int awardRecipes(Collection<Recipe<?>> recipes) {
        return 0;
    }

    public void awardRecipesByKey(ResourceLocation[] ids) {
    }

    public int resetRecipes(Collection<Recipe<?>> recipes) {
        return 0;
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        this.awardStat(Stats.JUMP);
        if (this.isSprinting()) {
            this.causeFoodExhaustion(0.2F);
        } else {
            this.causeFoodExhaustion(0.05F);
        }

    }

    @Override
    public void travel(Vec3 movementInput) {
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        if (this.isSwimming() && !this.isPassenger()) {
            double g = this.getLookAngle().y;
            double h = g < -0.2D ? 0.085D : 0.06D;
            if (g <= 0.0D || this.jumping || !this.level.getBlockState(new BlockPos(this.getX(), this.getY() + 1.0D - 0.1D, this.getZ())).getFluidState().isEmpty()) {
                Vec3 vec3 = this.getDeltaMovement();
                this.setDeltaMovement(vec3.add(0.0D, (g - vec3.y) * h, 0.0D));
            }
        }

        if (this.abilities.flying && !this.isPassenger()) {
            double i = this.getDeltaMovement().y;
            float j = this.flyingSpeed;
            this.flyingSpeed = this.abilities.getFlyingSpeed() * (float)(this.isSprinting() ? 2 : 1);
            super.travel(movementInput);
            Vec3 vec32 = this.getDeltaMovement();
            this.setDeltaMovement(vec32.x, i * 0.6D, vec32.z);
            this.flyingSpeed = j;
            this.resetFallDistance();
            this.setSharedFlag(7, false);
        } else {
            super.travel(movementInput);
        }

        this.checkMovementStatistics(this.getX() - d, this.getY() - e, this.getZ() - f);
    }

    @Override
    public void updateSwimming() {
        if (this.abilities.flying) {
            this.setSwimming(false);
        } else {
            super.updateSwimming();
        }

    }

    protected boolean freeAt(BlockPos pos) {
        return !this.level.getBlockState(pos).isSuffocating(this.level, pos);
    }

    @Override
    public float getSpeed() {
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    public void checkMovementStatistics(double dx, double dy, double dz) {
        if (!this.isPassenger()) {
            if (this.isSwimming()) {
                int i = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                if (i > 0) {
                    this.awardStat(Stats.SWIM_ONE_CM, i);
                    this.causeFoodExhaustion(0.01F * (float)i * 0.01F);
                }
            } else if (this.isEyeInFluid(FluidTags.WATER)) {
                int j = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                if (j > 0) {
                    this.awardStat(Stats.WALK_UNDER_WATER_ONE_CM, j);
                    this.causeFoodExhaustion(0.01F * (float)j * 0.01F);
                }
            } else if (this.isInWater()) {
                int k = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (k > 0) {
                    this.awardStat(Stats.WALK_ON_WATER_ONE_CM, k);
                    this.causeFoodExhaustion(0.01F * (float)k * 0.01F);
                }
            } else if (this.onClimbable()) {
                if (dy > 0.0D) {
                    this.awardStat(Stats.CLIMB_ONE_CM, (int)Math.round(dy * 100.0D));
                }
            } else if (this.onGround) {
                int l = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (l > 0) {
                    if (this.isSprinting()) {
                        this.awardStat(Stats.SPRINT_ONE_CM, l);
                        this.causeFoodExhaustion(0.1F * (float)l * 0.01F);
                    } else if (this.isCrouching()) {
                        this.awardStat(Stats.CROUCH_ONE_CM, l);
                        this.causeFoodExhaustion(0.0F * (float)l * 0.01F);
                    } else {
                        this.awardStat(Stats.WALK_ONE_CM, l);
                        this.causeFoodExhaustion(0.0F * (float)l * 0.01F);
                    }
                }
            } else if (this.isFallFlying()) {
                int m = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                this.awardStat(Stats.AVIATE_ONE_CM, m);
            } else {
                int n = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (n > 25) {
                    this.awardStat(Stats.FLY_ONE_CM, n);
                }
            }

        }
    }

    public void checkRidingStatistics(double dx, double dy, double dz) {
        if (this.isPassenger()) {
            int i = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
            if (i > 0) {
                Entity entity = this.getVehicle();
                if (entity instanceof AbstractMinecart) {
                    this.awardStat(Stats.MINECART_ONE_CM, i);
                } else if (entity instanceof Boat) {
                    this.awardStat(Stats.BOAT_ONE_CM, i);
                } else if (entity instanceof Pig) {
                    this.awardStat(Stats.PIG_ONE_CM, i);
                } else if (entity instanceof AbstractHorse) {
                    this.awardStat(Stats.HORSE_ONE_CM, i);
                } else if (entity instanceof Strider) {
                    this.awardStat(Stats.STRIDER_ONE_CM, i);
                }
            }
        }

    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (this.abilities.mayfly) {
            return false;
        } else {
            if (fallDistance >= 2.0F) {
                this.awardStat(Stats.FALL_ONE_CM, (int)Math.round((double)fallDistance * 100.0D));
            }

            return super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
        }
    }

    public boolean tryToStartFallFlying() {
        if (!this.onGround && !this.isFallFlying() && !this.isInWater() && !this.hasEffect(MobEffects.LEVITATION)) {
            ItemStack itemStack = this.getItemBySlot(EquipmentSlot.CHEST);
            if (itemStack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemStack)) {
                this.startFallFlying();
                return true;
            }
        }

        return false;
    }

    public void startFallFlying() {
        this.setSharedFlag(7, true);
    }

    public void stopFallFlying() {
        this.setSharedFlag(7, true);
        this.setSharedFlag(7, false);
    }

    @Override
    protected void doWaterSplashEffect() {
        if (!this.isSpectator()) {
            super.doWaterSplashEffect();
        }

    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
    }

    @Override
    public void killed(ServerLevel world, LivingEntity other) {
        this.awardStat(Stats.ENTITY_KILLED.get(other.getType()));
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
        if (!this.abilities.flying) {
            super.makeStuckInBlock(state, multiplier);
        }

    }

    public void giveExperiencePoints(int experience) {
        this.increaseScore(experience);
        this.experienceProgress += (float)experience / (float)this.getXpNeededForNextLevel();
        this.totalExperience = Mth.clamp(this.totalExperience + experience, 0, Integer.MAX_VALUE);

        while(this.experienceProgress < 0.0F) {
            float f = this.experienceProgress * (float)this.getXpNeededForNextLevel();
            if (this.experienceLevel > 0) {
                this.giveExperienceLevels(-1);
                this.experienceProgress = 1.0F + f / (float)this.getXpNeededForNextLevel();
            } else {
                this.giveExperienceLevels(-1);
                this.experienceProgress = 0.0F;
            }
        }

        while(this.experienceProgress >= 1.0F) {
            this.experienceProgress = (this.experienceProgress - 1.0F) * (float)this.getXpNeededForNextLevel();
            this.giveExperienceLevels(1);
            this.experienceProgress /= (float)this.getXpNeededForNextLevel();
        }

    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed;
    }

    public void onEnchantmentPerformed(ItemStack enchantedItem, int experienceLevels) {
        this.experienceLevel -= experienceLevels;
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        this.enchantmentSeed = this.random.nextInt();
    }

    public void giveExperienceLevels(int levels) {
        this.experienceLevel += levels;
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        if (levels > 0 && this.experienceLevel % 5 == 0 && (float)this.lastLevelUpTime < (float)this.tickCount - 100.0F) {
            float f = this.experienceLevel > 30 ? 1.0F : (float)this.experienceLevel / 30.0F;
            this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), f * 0.75F, 1.0F);
            this.lastLevelUpTime = this.tickCount;
        }

    }

    public int getXpNeededForNextLevel() {
        if (this.experienceLevel >= 30) {
            return 112 + (this.experienceLevel - 30) * 9;
        } else {
            return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
        }
    }

    public void causeFoodExhaustion(float exhaustion) {
        if (!this.abilities.invulnerable) {
            if (!this.level.isClientSide) {
                this.foodData.addExhaustion(exhaustion);
            }

        }
    }

    public FoodData getFoodData() {
        return this.foodData;
    }

    public boolean canEat(boolean ignoreHunger) {
        return this.abilities.invulnerable || ignoreHunger || this.foodData.needsFood();
    }

    public boolean isHurt() {
        return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
    }

    public boolean mayBuild() {
        return this.abilities.mayBuild;
    }

    public boolean mayUseItemAt(BlockPos pos, Direction facing, ItemStack stack) {
        if (this.abilities.mayBuild) {
            return true;
        } else {
            BlockPos blockPos = pos.relative(facing.getOpposite());
            BlockInWorld blockInWorld = new BlockInWorld(this.level, blockPos, false);
            return stack.hasAdventureModePlaceTagForBlock(this.level.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY), blockInWorld);
        }
    }

    @Override
    protected int getExperienceReward(Player player) {
        if (!this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !this.isSpectator()) {
            int i = this.experienceLevel * 7;
            return i > 100 ? 100 : i;
        } else {
            return 0;
        }
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return this.abilities.flying || this.onGround && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
    }

    public void onUpdateAbilities() {
    }

    @Override
    public Component getName() {
        return new TextComponent(this.gameProfile.getName());
    }

    public PlayerEnderChestContainer getEnderChestInventory() {
        return this.enderChestInventory;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.inventory.getSelected();
        } else if (slot == EquipmentSlot.OFFHAND) {
            return this.inventory.offhand.get(0);
        } else {
            return slot.getType() == EquipmentSlot.Type.ARMOR ? this.inventory.armor.get(slot.getIndex()) : ItemStack.EMPTY;
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        this.verifyEquippedItem(stack);
        if (slot == EquipmentSlot.MAINHAND) {
            this.equipEventAndSound(stack);
            this.inventory.items.set(this.inventory.selected, stack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            this.equipEventAndSound(stack);
            this.inventory.offhand.set(0, stack);
        } else if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            this.equipEventAndSound(stack);
            this.inventory.armor.set(slot.getIndex(), stack);
        }

    }

    public boolean addItem(ItemStack stack) {
        this.equipEventAndSound(stack);
        return this.inventory.add(stack);
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return Lists.newArrayList(this.getMainHandItem(), this.getOffhandItem());
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.inventory.armor;
    }

    public boolean setEntityOnShoulder(CompoundTag entityNbt) {
        if (!this.isPassenger() && this.onGround && !this.isInWater() && !this.isInPowderSnow) {
            if (this.getShoulderEntityLeft().isEmpty()) {
                this.setShoulderEntityLeft(entityNbt);
                this.timeEntitySatOnShoulder = this.level.getGameTime();
                return true;
            } else if (this.getShoulderEntityRight().isEmpty()) {
                this.setShoulderEntityRight(entityNbt);
                this.timeEntitySatOnShoulder = this.level.getGameTime();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void removeEntitiesOnShoulder() {
        if (this.timeEntitySatOnShoulder + 20L < this.level.getGameTime()) {
            this.respawnEntityOnShoulder(this.getShoulderEntityLeft());
            this.setShoulderEntityLeft(new CompoundTag());
            this.respawnEntityOnShoulder(this.getShoulderEntityRight());
            this.setShoulderEntityRight(new CompoundTag());
        }

    }

    private void respawnEntityOnShoulder(CompoundTag entityNbt) {
        if (!this.level.isClientSide && !entityNbt.isEmpty()) {
            EntityType.create(entityNbt, this.level).ifPresent((entity) -> {
                if (entity instanceof TamableAnimal) {
                    ((TamableAnimal)entity).setOwnerUUID(this.uuid);
                }

                entity.setPos(this.getX(), this.getY() + (double)0.7F, this.getZ());
                ((ServerLevel)this.level).addWithUUID(entity);
            });
        }

    }

    @Override
    public abstract boolean isSpectator();

    @Override
    public boolean isSwimming() {
        return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
    }

    public abstract boolean isCreative();

    @Override
    public boolean isPushedByFluid() {
        return !this.abilities.flying;
    }

    public Scoreboard getScoreboard() {
        return this.level.getScoreboard();
    }

    @Override
    public Component getDisplayName() {
        MutableComponent mutableComponent = PlayerTeam.formatNameForTeam(this.getTeam(), this.getName());
        return this.decorateDisplayNameComponent(mutableComponent);
    }

    private MutableComponent decorateDisplayNameComponent(MutableComponent component) {
        String string = this.getGameProfile().getName();
        return component.withStyle((style) -> {
            return style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + string + " ")).withHoverEvent(this.createHoverEvent()).withInsertion(string);
        });
    }

    @Override
    public String getScoreboardName() {
        return this.getGameProfile().getName();
    }

    @Override
    public float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        switch(pose) {
        case SWIMMING:
        case FALL_FLYING:
        case SPIN_ATTACK:
            return 0.4F;
        case CROUCHING:
            return 1.27F;
        default:
            return 1.62F;
        }
    }

    @Override
    public void setAbsorptionAmount(float amount) {
        if (amount < 0.0F) {
            amount = 0.0F;
        }

        this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, amount);
    }

    @Override
    public float getAbsorptionAmount() {
        return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
    }

    public static UUID createPlayerUUID(GameProfile profile) {
        UUID uUID = profile.getId();
        if (uUID == null) {
            uUID = createPlayerUUID(profile.getName());
        }

        return uUID;
    }

    public static UUID createPlayerUUID(String nickname) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8));
    }

    public boolean isModelPartShown(PlayerModelPart modelPart) {
        return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & modelPart.getMask()) == modelPart.getMask();
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        if (mappedIndex >= 0 && mappedIndex < this.inventory.items.size()) {
            return SlotAccess.forContainer(this.inventory, mappedIndex);
        } else {
            int i = mappedIndex - 200;
            return i >= 0 && i < this.enderChestInventory.getContainerSize() ? SlotAccess.forContainer(this.enderChestInventory, i) : super.getSlot(mappedIndex);
        }
    }

    public boolean isReducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public void setReducedDebugInfo(boolean reducedDebugInfo) {
        this.reducedDebugInfo = reducedDebugInfo;
    }

    @Override
    public void setRemainingFireTicks(int fireTicks) {
        super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(fireTicks, 1) : fireTicks);
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.entityData.get(DATA_PLAYER_MAIN_HAND) == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public void setMainArm(HumanoidArm arm) {
        this.entityData.set(DATA_PLAYER_MAIN_HAND, (byte)(arm == HumanoidArm.LEFT ? 0 : 1));
    }

    public CompoundTag getShoulderEntityLeft() {
        return this.entityData.get(DATA_SHOULDER_LEFT);
    }

    public void setShoulderEntityLeft(CompoundTag entityNbt) {
        this.entityData.set(DATA_SHOULDER_LEFT, entityNbt);
    }

    public CompoundTag getShoulderEntityRight() {
        return this.entityData.get(DATA_SHOULDER_RIGHT);
    }

    public void setShoulderEntityRight(CompoundTag entityNbt) {
        this.entityData.set(DATA_SHOULDER_RIGHT, entityNbt);
    }

    public float getCurrentItemAttackStrengthDelay() {
        return (float)(1.0D / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0D);
    }

    public float getAttackStrengthScale(float baseTime) {
        return Mth.clamp(((float)this.attackStrengthTicker + baseTime) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
    }

    public void resetAttackStrengthTicker() {
        this.attackStrengthTicker = 0;
    }

    public ItemCooldowns getCooldowns() {
        return this.cooldowns;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
    }

    public float getLuck() {
        return (float)this.getAttributeValue(Attributes.LUCK);
    }

    public boolean canUseGameMasterBlocks() {
        return this.abilities.instabuild && this.getPermissionLevel() >= 2;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EquipmentSlot equipmentSlot = Mob.getEquipmentSlotForItem(stack);
        return this.getItemBySlot(equipmentSlot).isEmpty();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return POSES.getOrDefault(pose, STANDING_DIMENSIONS);
    }

    @Override
    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
    }

    @Override
    public ItemStack getProjectile(ItemStack stack) {
        if (!(stack.getItem() instanceof ProjectileWeaponItem)) {
            return ItemStack.EMPTY;
        } else {
            Predicate<ItemStack> predicate = ((ProjectileWeaponItem)stack.getItem()).getSupportedHeldProjectiles();
            ItemStack itemStack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            if (!itemStack.isEmpty()) {
                return itemStack;
            } else {
                predicate = ((ProjectileWeaponItem)stack.getItem()).getAllSupportedProjectiles();

                for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
                    ItemStack itemStack2 = this.inventory.getItem(i);
                    if (predicate.test(itemStack2)) {
                        return itemStack2;
                    }
                }

                return this.abilities.instabuild ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
            }
        }
    }

    @Override
    public ItemStack eat(Level world, ItemStack stack) {
        this.getFoodData().eat(stack.getItem(), stack);
        this.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        world.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        if (this instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)this, stack);
        }

        return super.eat(world, stack);
    }

    @Override
    protected boolean shouldRemoveSoulSpeed(BlockState landingState) {
        return this.abilities.flying || super.shouldRemoveSoulSpeed(landingState);
    }

    @Override
    public Vec3 getRopeHoldPosition(float delta) {
        double d = 0.22D * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0D : 1.0D);
        float f = Mth.lerp(delta * 0.5F, this.getXRot(), this.xRotO) * ((float)Math.PI / 180F);
        float g = Mth.lerp(delta, this.yBodyRotO, this.yBodyRot) * ((float)Math.PI / 180F);
        if (!this.isFallFlying() && !this.isAutoSpinAttack()) {
            if (this.isVisuallySwimming()) {
                return this.getPosition(delta).add((new Vec3(d, 0.2D, -0.15D)).xRot(-f).yRot(-g));
            } else {
                double m = this.getBoundingBox().getYsize() - 1.0D;
                double n = this.isCrouching() ? -0.2D : 0.07D;
                return this.getPosition(delta).add((new Vec3(d, m, n)).yRot(-g));
            }
        } else {
            Vec3 vec3 = this.getViewVector(delta);
            Vec3 vec32 = this.getDeltaMovement();
            double e = vec32.horizontalDistanceSqr();
            double h = vec3.horizontalDistanceSqr();
            float k;
            if (e > 0.0D && h > 0.0D) {
                double i = (vec32.x * vec3.x + vec32.z * vec3.z) / Math.sqrt(e * h);
                double j = vec32.x * vec3.z - vec32.z * vec3.x;
                k = (float)(Math.signum(j) * Math.acos(i));
            } else {
                k = 0.0F;
            }

            return this.getPosition(delta).add((new Vec3(d, -0.11D, 0.85D)).zRot(-k).xRot(-f).yRot(-g));
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    public boolean isScoping() {
        return this.isUsingItem() && this.getUseItem().is(Items.SPYGLASS);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public static enum BedSleepingProblem {
        NOT_POSSIBLE_HERE,
        NOT_POSSIBLE_NOW(new TranslatableComponent("block.minecraft.bed.no_sleep")),
        TOO_FAR_AWAY(new TranslatableComponent("block.minecraft.bed.too_far_away")),
        OBSTRUCTED(new TranslatableComponent("block.minecraft.bed.obstructed")),
        OTHER_PROBLEM,
        NOT_SAFE(new TranslatableComponent("block.minecraft.bed.not_safe"));

        @Nullable
        private final Component message;

        private BedSleepingProblem() {
            this.message = null;
        }

        private BedSleepingProblem(Component message) {
            this.message = message;
        }

        @Nullable
        public Component getMessage() {
            return this.message;
        }
    }
}
