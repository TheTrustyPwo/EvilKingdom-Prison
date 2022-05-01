package net.minecraft.world.entity.animal.horse;

import com.google.common.collect.UnmodifiableIterator;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason; // CraftBukkit

public abstract class AbstractHorse extends Animal implements ContainerListener, PlayerRideableJumping, Saddleable {

    public static final int EQUIPMENT_SLOT_OFFSET = 400;
    public static final int CHEST_SLOT_OFFSET = 499;
    public static final int INVENTORY_SLOT_OFFSET = 500;
    private static final Predicate<LivingEntity> PARENT_HORSE_SELECTOR = (entityliving) -> {
        return entityliving instanceof AbstractHorse && ((AbstractHorse) entityliving).isBred();
    };
    private static final TargetingConditions MOMMY_TARGETING = TargetingConditions.forNonCombat().range(16.0D).ignoreLineOfSight().selector(AbstractHorse.PARENT_HORSE_SELECTOR);
    private static final Ingredient FOOD_ITEMS = Ingredient.of(Items.WHEAT, Items.SUGAR, Blocks.HAY_BLOCK.asItem(), Items.APPLE, Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> DATA_ID_OWNER_UUID = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int FLAG_TAME = 2;
    private static final int FLAG_SADDLE = 4;
    private static final int FLAG_BRED = 8;
    private static final int FLAG_EATING = 16;
    private static final int FLAG_STANDING = 32;
    private static final int FLAG_OPEN_MOUTH = 64;
    public static final int INV_SLOT_SADDLE = 0;
    public static final int INV_SLOT_ARMOR = 1;
    public static final int INV_BASE_COUNT = 2;
    private int eatingCounter;
    private int mouthCounter;
    private int standCounter;
    public int tailCounter;
    public int sprintCounter;
    protected boolean isJumping;
    public SimpleContainer inventory;
    protected int temper;
    protected float playerJumpPendingScale;
    private boolean allowStandSliding;
    private float eatAnim;
    private float eatAnimO;
    private float standAnim;
    private float standAnimO;
    private float mouthAnim;
    private float mouthAnimO;
    protected boolean canGallop = true;
    protected int gallopSoundCounter;
    public int maxDomestication = 100; // CraftBukkit - store max domestication value

    protected AbstractHorse(EntityType<? extends AbstractHorse> type, Level world) {
        super(type, world);
        this.maxUpStep = 1.0F;
        this.createInventory();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D, AbstractHorse.class));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.of(Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE), false));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AbstractHorse.DATA_ID_FLAGS, (byte) 0);
        this.entityData.define(AbstractHorse.DATA_ID_OWNER_UUID, Optional.empty());
    }

    protected boolean getFlag(int bitmask) {
        return ((Byte) this.entityData.get(AbstractHorse.DATA_ID_FLAGS) & bitmask) != 0;
    }

    protected void setFlag(int bitmask, boolean flag) {
        byte b0 = (Byte) this.entityData.get(AbstractHorse.DATA_ID_FLAGS);

        if (flag) {
            this.entityData.set(AbstractHorse.DATA_ID_FLAGS, (byte) (b0 | bitmask));
        } else {
            this.entityData.set(AbstractHorse.DATA_ID_FLAGS, (byte) (b0 & ~bitmask));
        }

    }

    public boolean isTamed() {
        return this.getFlag(2);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return (UUID) ((Optional) this.entityData.get(AbstractHorse.DATA_ID_OWNER_UUID)).orElse((Object) null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(AbstractHorse.DATA_ID_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public boolean isJumping() {
        return this.isJumping;
    }

    public void setTamed(boolean tame) {
        this.setFlag(2, tame);
    }

    public void setIsJumping(boolean inAir) {
        this.isJumping = inAir;
    }

    @Override
    protected void onLeashDistance(float leashLength) {
        if (leashLength > 6.0F && this.isEating()) {
            this.setEating(false);
        }

    }

    public boolean isEating() {
        return this.getFlag(16);
    }

    public boolean isStanding() {
        return this.getFlag(32);
    }

    public boolean isBred() {
        return this.getFlag(8);
    }

    public void setBred(boolean bred) {
        this.setFlag(8, bred);
    }

    @Override
    public boolean isSaddleable() {
        return this.isAlive() && !this.isBaby() && this.isTamed();
    }

    @Override
    public void equipSaddle(@Nullable SoundSource sound) {
        this.inventory.setItem(0, new ItemStack(Items.SADDLE));
        if (sound != null) {
            this.level.playSound((Player) null, (Entity) this, SoundEvents.HORSE_SADDLE, sound, 0.5F, 1.0F);
        }

    }

    @Override
    public boolean isSaddled() {
        return this.getFlag(4);
    }

    public int getTemper() {
        return this.temper;
    }

    public void setTemper(int temper) {
        this.temper = temper;
    }

    public int modifyTemper(int difference) {
        int j = Mth.clamp(this.getTemper() + difference, (int) 0, this.getMaxTemper());

        this.setTemper(j);
        return j;
    }

    @Override
    public boolean isCollidable(boolean ignoreClimbing) { // Paper
        return !this.isVehicle();
    }

    private void eating() {
        this.openMouth();
        if (!this.isSilent()) {
            SoundEvent soundeffect = this.getEatingSound();

            if (soundeffect != null) {
                this.level.playSound((Player) null, this.getX(), this.getY(), this.getZ(), soundeffect, this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
        }

    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (fallDistance > 1.0F) {
            this.playSound(SoundEvents.HORSE_LAND, 0.4F, 1.0F);
        }

        int i = this.calculateFallDamage(fallDistance, damageMultiplier);

        if (i <= 0) {
            return false;
        } else {
            this.hurt(damageSource, (float) i);
            if (this.isVehicle()) {
                Iterator iterator = this.getIndirectPassengers().iterator();

                while (iterator.hasNext()) {
                    Entity entity = (Entity) iterator.next();

                    entity.hurt(damageSource, (float) i);
                }
            }

            this.playBlockFallSound();
            return true;
        }
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return Mth.ceil((fallDistance * 0.5F - 3.0F) * damageMultiplier);
    }

    protected int getInventorySize() {
        return 2;
    }

    public void createInventory() {
        SimpleContainer inventorysubcontainer = this.inventory;

        this.inventory = new SimpleContainer(this.getInventorySize(), (org.bukkit.entity.AbstractHorse) this.getBukkitEntity()); // CraftBukkit
        if (inventorysubcontainer != null) {
            inventorysubcontainer.removeListener(this);
            int i = Math.min(inventorysubcontainer.getContainerSize(), this.inventory.getContainerSize());

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = inventorysubcontainer.getItem(j);

                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.inventory.addListener(this);
        this.updateContainerEquipment();
    }

    protected void updateContainerEquipment() {
        if (!this.level.isClientSide) {
            this.setFlag(4, !this.inventory.getItem(0).isEmpty());
        }
    }

    @Override
    public void containerChanged(Container sender) {
        boolean flag = this.isSaddled();

        this.updateContainerEquipment();
        if (this.tickCount > 20 && !flag && this.isSaddled()) {
            this.playSound(SoundEvents.HORSE_SADDLE, 0.5F, 1.0F);
        }

    }

    public double getCustomJump() {
        return this.getAttributeValue(Attributes.JUMP_STRENGTH);
    }

    @Nullable
    protected SoundEvent getEatingSound() {
        return null;
    }

    @Nullable
    @Override
    public SoundEvent getDeathSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.random.nextInt(3) == 0) {
            this.stand();
        }

        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.random.nextInt(10) == 0 && !this.isImmobile()) {
            this.stand();
        }

        return null;
    }

    @Nullable
    protected SoundEvent getAngrySound() {
        this.stand();
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.getMaterial().isLiquid()) {
            BlockState iblockdata1 = this.level.getBlockState(pos.above());
            SoundType soundeffecttype = state.getSoundType();

            if (iblockdata1.is(Blocks.SNOW)) {
                soundeffecttype = iblockdata1.getSoundType();
            }

            if (this.isVehicle() && this.canGallop) {
                ++this.gallopSoundCounter;
                if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
                    this.playGallopSound(soundeffecttype);
                } else if (this.gallopSoundCounter <= 5) {
                    this.playSound(SoundEvents.HORSE_STEP_WOOD, soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
                }
            } else if (soundeffecttype == SoundType.WOOD) {
                this.playSound(SoundEvents.HORSE_STEP_WOOD, soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
            } else {
                this.playSound(SoundEvents.HORSE_STEP, soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
            }

        }
    }

    protected void playGallopSound(SoundType group) {
        this.playSound(SoundEvents.HORSE_GALLOP, group.getVolume() * 0.15F, group.getPitch());
    }

    public static AttributeSupplier.Builder createBaseHorseAttributes() {
        return Mob.createMobAttributes().add(Attributes.JUMP_STRENGTH).add(Attributes.MAX_HEALTH, 53.0D).add(Attributes.MOVEMENT_SPEED, 0.22499999403953552D);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 6;
    }

    public int getMaxTemper() {
        return this.maxDomestication; // CraftBukkit - return stored max domestication instead of 100
    }

    @Override
    public float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    public void openInventory(Player player) {
        if (!this.level.isClientSide && (!this.isVehicle() || this.hasPassenger((Entity) player)) && this.isTamed()) {
            player.openHorseInventory(this, this.inventory);
        }

    }

    public InteractionResult fedFood(Player player, ItemStack stack) {
        boolean flag = this.handleEating(player, stack);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return this.level.isClientSide ? InteractionResult.CONSUME : (flag ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }

    protected boolean handleEating(Player player, ItemStack item) {
        boolean flag = false;
        float f = 0.0F;
        short short0 = 0;
        byte b0 = 0;

        if (item.is(Items.WHEAT)) {
            f = 2.0F;
            short0 = 20;
            b0 = 3;
        } else if (item.is(Items.SUGAR)) {
            f = 1.0F;
            short0 = 30;
            b0 = 3;
        } else if (item.is(Blocks.HAY_BLOCK.asItem())) {
            f = 20.0F;
            short0 = 180;
        } else if (item.is(Items.APPLE)) {
            f = 3.0F;
            short0 = 60;
            b0 = 3;
        } else if (item.is(Items.GOLDEN_CARROT)) {
            f = 4.0F;
            short0 = 60;
            b0 = 5;
            if (!this.level.isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                flag = true;
                this.setInLove(player);
            }
        } else if (item.is(Items.GOLDEN_APPLE) || item.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            f = 10.0F;
            short0 = 240;
            b0 = 10;
            if (!this.level.isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                flag = true;
                this.setInLove(player);
            }
        }

        if (this.getHealth() < this.getMaxHealth() && f > 0.0F) {
            this.heal(f, RegainReason.EATING); // CraftBukkit
            flag = true;
        }

        if (this.isBaby() && short0 > 0) {
            this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);
            if (!this.level.isClientSide) {
                this.ageUp(short0);
            }

            flag = true;
        }

        if (b0 > 0 && (flag || !this.isTamed()) && this.getTemper() < this.getMaxTemper()) {
            flag = true;
            if (!this.level.isClientSide) {
                this.modifyTemper(b0);
            }
        }

        if (flag) {
            this.eating();
            this.gameEvent(GameEvent.EAT, this.eyeBlockPosition());
        }

        return flag;
    }

    protected void doPlayerRide(Player player) {
        this.setEating(false);
        this.setStanding(false);
        if (!this.level.isClientSide) {
            player.setYRot(this.getYRot());
            player.setXRot(this.getXRot());
            player.startRiding(this);
        }

    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() && this.isVehicle() && this.isSaddled() || this.isEating() || this.isStanding();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return AbstractHorse.FOOD_ITEMS.test(stack);
    }

    private void moveTail() {
        this.tailCounter = 1;
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (this.inventory != null) {
            for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.inventory.getItem(i);

                if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack)) {
                    this.spawnAtLocation(itemstack);
                }
            }

        }
    }

    @Override
    public void aiStep() {
        if (this.random.nextInt(200) == 0) {
            this.moveTail();
        }

        super.aiStep();
        if (!this.level.isClientSide && this.isAlive()) {
            if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
                this.heal(1.0F, RegainReason.REGEN); // CraftBukkit
            }

            if (this.canEatGrass()) {
                if (!this.isEating() && !this.isVehicle() && this.random.nextInt(300) == 0 && this.level.getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
                    this.setEating(true);
                }

                if (this.isEating() && ++this.eatingCounter > 50) {
                    this.eatingCounter = 0;
                    this.setEating(false);
                }
            }

            this.followMommy();
        }
    }

    protected void followMommy() {
        if (this.isBred() && this.isBaby() && !this.isEating()) {
            LivingEntity entityliving = this.level.getNearestEntity(AbstractHorse.class, AbstractHorse.MOMMY_TARGETING, this, this.getX(), this.getY(), this.getZ(), this.getBoundingBox().inflate(16.0D));

            if (entityliving != null && this.distanceToSqr((Entity) entityliving) > 4.0D) {
                this.navigation.createPath((Entity) entityliving, 0);
            }
        }

    }

    public boolean canEatGrass() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.mouthCounter > 0 && ++this.mouthCounter > 30) {
            this.mouthCounter = 0;
            this.setFlag(64, false);
        }

        if ((this.isControlledByLocalInstance() || this.isEffectiveAi()) && this.standCounter > 0 && ++this.standCounter > 20) {
            this.standCounter = 0;
            this.setStanding(false);
        }

        if (this.tailCounter > 0 && ++this.tailCounter > 8) {
            this.tailCounter = 0;
        }

        if (this.sprintCounter > 0) {
            ++this.sprintCounter;
            if (this.sprintCounter > 300) {
                this.sprintCounter = 0;
            }
        }

        this.eatAnimO = this.eatAnim;
        if (this.isEating()) {
            this.eatAnim += (1.0F - this.eatAnim) * 0.4F + 0.05F;
            if (this.eatAnim > 1.0F) {
                this.eatAnim = 1.0F;
            }
        } else {
            this.eatAnim += (0.0F - this.eatAnim) * 0.4F - 0.05F;
            if (this.eatAnim < 0.0F) {
                this.eatAnim = 0.0F;
            }
        }

        this.standAnimO = this.standAnim;
        if (this.isStanding()) {
            this.eatAnim = 0.0F;
            this.eatAnimO = this.eatAnim;
            this.standAnim += (1.0F - this.standAnim) * 0.4F + 0.05F;
            if (this.standAnim > 1.0F) {
                this.standAnim = 1.0F;
            }
        } else {
            this.allowStandSliding = false;
            this.standAnim += (0.8F * this.standAnim * this.standAnim * this.standAnim - this.standAnim) * 0.6F - 0.05F;
            if (this.standAnim < 0.0F) {
                this.standAnim = 0.0F;
            }
        }

        this.mouthAnimO = this.mouthAnim;
        if (this.getFlag(64)) {
            this.mouthAnim += (1.0F - this.mouthAnim) * 0.7F + 0.05F;
            if (this.mouthAnim > 1.0F) {
                this.mouthAnim = 1.0F;
            }
        } else {
            this.mouthAnim += (0.0F - this.mouthAnim) * 0.7F - 0.05F;
            if (this.mouthAnim < 0.0F) {
                this.mouthAnim = 0.0F;
            }
        }

    }

    // Paper Start - Horse API
    public void setMouthOpen(boolean open) {
        this.setFlag(FLAG_OPEN_MOUTH, open);
    }
    public boolean isMouthOpen() {
        return this.getFlag(FLAG_OPEN_MOUTH);
    }
    // Paper End - Horse API
    private void openMouth() {
        if (!this.level.isClientSide) {
            this.mouthCounter = 1;
            this.setFlag(64, true);
        }

    }

    public void setEating(boolean eatingGrass) {
        this.setFlag(16, eatingGrass);
    }

    // Paper Start - Horse API
    public void setForceStanding(boolean standing) {
        this.setFlag(FLAG_STANDING, standing);
    }
    // Paper End - Horse API
    public void setStanding(boolean angry) {
        if (angry) {
            this.setEating(false);
        }

        this.setFlag(32, angry);
    }

    private void stand() {
        if (this.isControlledByLocalInstance() || this.isEffectiveAi()) {
            this.standCounter = 1;
            this.setStanding(true);
        }

    }

    public void makeMad() {
        if (!this.isStanding()) {
            this.stand();
            SoundEvent soundeffect = this.getAngrySound();

            if (soundeffect != null) {
                this.playSound(soundeffect, this.getSoundVolume(), this.getVoicePitch());
            }
        }

    }

    public boolean tameWithName(Player player) {
        this.setOwnerUUID(player.getUUID());
        this.setTamed(true);
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayer) player, (Animal) this);
        }

        this.level.broadcastEntityEvent(this, (byte) 7);
        return true;
    }

    @Override
    public void travel(Vec3 movementInput) {
        if (this.isAlive()) {
            if (this.isVehicle() && this.canBeControlledByRider() && this.isSaddled()) {
                LivingEntity entityliving = (LivingEntity) this.getControllingPassenger();

                this.setYRot(entityliving.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(entityliving.getXRot() * 0.5F);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.yBodyRot;
                float f = entityliving.xxa * 0.5F;
                float f1 = entityliving.zza;

                if (f1 <= 0.0F) {
                    f1 *= 0.25F;
                    this.gallopSoundCounter = 0;
                }

                if (this.onGround && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
                    f = 0.0F;
                    f1 = 0.0F;
                }

                if (this.playerJumpPendingScale > 0.0F && !this.isJumping() && this.onGround) {
                    double d0 = this.getCustomJump() * (double) this.playerJumpPendingScale * (double) this.getBlockJumpFactor();
                    double d1 = d0 + this.getJumpBoostPower();
                    Vec3 vec3d1 = this.getDeltaMovement();

                    this.setDeltaMovement(vec3d1.x, d1, vec3d1.z);
                    this.setIsJumping(true);
                    this.hasImpulse = true;
                    if (f1 > 0.0F) {
                        float f2 = Mth.sin(this.getYRot() * 0.017453292F);
                        float f3 = Mth.cos(this.getYRot() * 0.017453292F);

                        this.setDeltaMovement(this.getDeltaMovement().add((double) (-0.4F * f2 * this.playerJumpPendingScale), 0.0D, (double) (0.4F * f3 * this.playerJumpPendingScale)));
                    }

                    this.playerJumpPendingScale = 0.0F;
                }

                this.flyingSpeed = this.getSpeed() * 0.1F;
                if (this.isControlledByLocalInstance()) {
                    this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    super.travel(new Vec3((double) f, movementInput.y, (double) f1));
                } else if (entityliving instanceof Player) {
                    this.setDeltaMovement(Vec3.ZERO);
                }

                if (this.onGround) {
                    this.playerJumpPendingScale = 0.0F;
                    this.setIsJumping(false);
                }

                this.calculateEntityAnimation(this, false);
                this.tryCheckInsideBlocks();
            } else {
                this.flyingSpeed = 0.02F;
                super.travel(movementInput);
            }
        }
    }

    protected void playJumpSound() {
        this.playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("EatingHaystack", this.isEating());
        nbt.putBoolean("Bred", this.isBred());
        nbt.putInt("Temper", this.getTemper());
        nbt.putBoolean("Tame", this.isTamed());
        if (this.getOwnerUUID() != null) {
            nbt.putUUID("Owner", this.getOwnerUUID());
        }
        nbt.putInt("Bukkit.MaxDomestication", this.maxDomestication); // CraftBukkit

        if (!this.inventory.getItem(0).isEmpty()) {
            nbt.put("SaddleItem", this.inventory.getItem(0).save(new CompoundTag()));
        }

    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setEating(nbt.getBoolean("EatingHaystack"));
        this.setBred(nbt.getBoolean("Bred"));
        this.setTemper(nbt.getInt("Temper"));
        this.setTamed(nbt.getBoolean("Tame"));
        UUID uuid;

        if (nbt.hasUUID("Owner")) {
            uuid = nbt.getUUID("Owner");
        } else {
            String s = nbt.getString("Owner");

            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
        }

        if (uuid != null) {
            this.setOwnerUUID(uuid);
        }
        // CraftBukkit start
        if (nbt.contains("Bukkit.MaxDomestication")) {
            this.maxDomestication = nbt.getInt("Bukkit.MaxDomestication");
        }
        // CraftBukkit end

        if (nbt.contains("SaddleItem", 10)) {
            ItemStack itemstack = ItemStack.of(nbt.getCompound("SaddleItem"));

            if (itemstack.is(Items.SADDLE)) {
                this.inventory.setItem(0, itemstack);
            }
        }

        this.updateContainerEquipment();
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    protected boolean canParent() {
        return !this.isVehicle() && !this.isPassenger() && this.isTamed() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob entity) {
        return null;
    }

    protected void setOffspringAttributes(AgeableMob mate, AbstractHorse child) {
        double d0 = this.getAttributeBaseValue(Attributes.MAX_HEALTH) + mate.getAttributeBaseValue(Attributes.MAX_HEALTH) + (double) this.generateRandomMaxHealth();

        child.getAttribute(Attributes.MAX_HEALTH).setBaseValue(d0 / 3.0D);
        double d1 = this.getAttributeBaseValue(Attributes.JUMP_STRENGTH) + mate.getAttributeBaseValue(Attributes.JUMP_STRENGTH) + this.generateRandomJumpStrength();

        child.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(d1 / 3.0D);
        double d2 = this.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) + mate.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) + this.generateRandomSpeed();

        child.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(d2 / 3.0D);
    }

    @Override
    public boolean canBeControlledByRider() {
        return this.getControllingPassenger() instanceof LivingEntity;
    }

    public float getEatAnim(float tickDelta) {
        return Mth.lerp(tickDelta, this.eatAnimO, this.eatAnim);
    }

    public float getStandAnim(float tickDelta) {
        return Mth.lerp(tickDelta, this.standAnimO, this.standAnim);
    }

    public float getMouthAnim(float tickDelta) {
        return Mth.lerp(tickDelta, this.mouthAnimO, this.mouthAnim);
    }

    @Override
    public void onPlayerJump(int strength) {
        if (this.isSaddled()) {
            if (strength < 0) {
                strength = 0;
            } else {
                this.allowStandSliding = true;
                this.stand();
            }

            if (strength >= 90) {
                this.playerJumpPendingScale = 1.0F;
            } else {
                this.playerJumpPendingScale = 0.4F + 0.4F * (float) strength / 90.0F;
            }

        }
    }

    @Override
    public boolean canJump() {
        return this.isSaddled();
    }

    @Override
    public void handleStartJump(int height) {
        // CraftBukkit start
        float power;
        if (height >= 90) {
            power = 1.0F;
        } else {
            power = 0.4F + 0.4F * (float) height / 90.0F;
        }
        org.bukkit.event.entity.HorseJumpEvent event = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callHorseJumpEvent(this, power);
        if (event.isCancelled()) {
            return;
        }
        // CraftBukkit end
        this.allowStandSliding = true;
        this.stand();
        this.playJumpSound();
    }

    @Override
    public void handleStopJump() {}

    protected void spawnTamingParticles(boolean positive) {
        SimpleParticleType particletype = positive ? ParticleTypes.HEART : ParticleTypes.SMOKE;

        for (int i = 0; i < 7; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;

            this.level.addParticle(particletype, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 7) {
            this.spawnTamingParticles(true);
        } else if (status == 6) {
            this.spawnTamingParticles(false);
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    public void positionRider(Entity passenger) {
        super.positionRider(passenger);
        if (passenger instanceof Mob) {
            Mob entityinsentient = (Mob) passenger;

            this.yBodyRot = entityinsentient.yBodyRot;
        }

        if (this.standAnimO > 0.0F) {
            float f = Mth.sin(this.yBodyRot * 0.017453292F);
            float f1 = Mth.cos(this.yBodyRot * 0.017453292F);
            float f2 = 0.7F * this.standAnimO;
            float f3 = 0.15F * this.standAnimO;

            passenger.setPos(this.getX() + (double) (f2 * f), this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset() + (double) f3, this.getZ() - (double) (f2 * f1));
            if (passenger instanceof LivingEntity) {
                ((LivingEntity) passenger).yBodyRot = this.yBodyRot;
            }
        }

    }

    protected float generateRandomMaxHealth() {
        return 15.0F + (float) this.random.nextInt(8) + (float) this.random.nextInt(9);
    }

    protected double generateRandomJumpStrength() {
        return 0.4000000059604645D + this.random.nextDouble() * 0.2D + this.random.nextDouble() * 0.2D + this.random.nextDouble() * 0.2D;
    }

    protected double generateRandomSpeed() {
        return (0.44999998807907104D + this.random.nextDouble() * 0.3D + this.random.nextDouble() * 0.3D + this.random.nextDouble() * 0.3D) * 0.25D;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.95F;
    }

    public boolean canWearArmor() {
        return false;
    }

    public boolean isWearingArmor() {
        return !this.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
    }

    public boolean isArmor(ItemStack item) {
        return false;
    }

    private SlotAccess createEquipmentSlotAccess(final int slot, final Predicate<ItemStack> predicate) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return AbstractHorse.this.inventory.getItem(slot);
            }

            @Override
            public boolean set(ItemStack stack) {
                if (!predicate.test(stack)) {
                    return false;
                } else {
                    AbstractHorse.this.inventory.setItem(slot, stack);
                    AbstractHorse.this.updateContainerEquipment();
                    return true;
                }
            }
        };
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        int j = mappedIndex - 400;

        if (j >= 0 && j < 2 && j < this.inventory.getContainerSize()) {
            if (j == 0) {
                return this.createEquipmentSlotAccess(j, (itemstack) -> {
                    return itemstack.isEmpty() || itemstack.is(Items.SADDLE);
                });
            }

            if (j == 1) {
                if (!this.canWearArmor()) {
                    return SlotAccess.NULL;
                }

                return this.createEquipmentSlotAccess(j, (itemstack) -> {
                    return itemstack.isEmpty() || this.isArmor(itemstack);
                });
            }
        }

        int k = mappedIndex - 500 + 2;

        return k >= 2 && k < this.inventory.getContainerSize() ? SlotAccess.forContainer(this.inventory, k) : super.getSlot(mappedIndex);
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        return this.getFirstPassenger();
    }

    @Nullable
    private Vec3 getDismountLocationInDirection(Vec3 offset, LivingEntity passenger) {
        double d0 = this.getX() + offset.x;
        double d1 = this.getBoundingBox().minY;
        double d2 = this.getZ() + offset.z;
        BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
        UnmodifiableIterator unmodifiableiterator = passenger.getDismountPoses().iterator();

        while (unmodifiableiterator.hasNext()) {
            Pose entitypose = (Pose) unmodifiableiterator.next();

            blockposition_mutableblockposition.set(d0, d1, d2);
            double d3 = this.getBoundingBox().maxY + 0.75D;

            while (true) {
                double d4 = this.level.getBlockFloorHeight(blockposition_mutableblockposition);

                if ((double) blockposition_mutableblockposition.getY() + d4 > d3) {
                    break;
                }

                if (DismountHelper.isBlockFloorValid(d4)) {
                    AABB axisalignedbb = passenger.getLocalBoundsForPose(entitypose);
                    Vec3 vec3d1 = new Vec3(d0, (double) blockposition_mutableblockposition.getY() + d4, d2);

                    if (DismountHelper.canDismountTo(this.level, passenger, axisalignedbb.move(vec3d1))) {
                        passenger.setPose(entitypose);
                        return vec3d1;
                    }
                }

                blockposition_mutableblockposition.move(Direction.UP);
                if ((double) blockposition_mutableblockposition.getY() >= d3) {
                    break;
                }
            }
        }

        return null;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 vec3d = getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) passenger.getBbWidth(), this.getYRot() + (passenger.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F));
        Vec3 vec3d1 = this.getDismountLocationInDirection(vec3d, passenger);

        if (vec3d1 != null) {
            return vec3d1;
        } else {
            Vec3 vec3d2 = getCollisionHorizontalEscapeVector((double) this.getBbWidth(), (double) passenger.getBbWidth(), this.getYRot() + (passenger.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F));
            Vec3 vec3d3 = this.getDismountLocationInDirection(vec3d2, passenger);

            return vec3d3 != null ? vec3d3 : this.position();
        }
    }

    protected void randomizeAttributes() {}

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        if (entityData == null) {
            entityData = new AgeableMob.AgeableMobGroupData(0.2F);
        }

        this.randomizeAttributes();
        return super.finalizeSpawn(world, difficulty, spawnReason, (SpawnGroupData) entityData, entityNbt);
    }

    public boolean hasInventoryChanged(Container inventory) {
        return this.inventory != inventory;
    }
}
