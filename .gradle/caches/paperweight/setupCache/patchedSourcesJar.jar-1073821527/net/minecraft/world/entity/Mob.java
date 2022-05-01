package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class Mob extends LivingEntity {
    private static final EntityDataAccessor<Byte> DATA_MOB_FLAGS_ID = SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);
    private static final int MOB_FLAG_NO_AI = 1;
    private static final int MOB_FLAG_LEFTHANDED = 2;
    private static final int MOB_FLAG_AGGRESSIVE = 4;
    public static final float MAX_WEARING_ARMOR_CHANCE = 0.15F;
    public static final float MAX_PICKUP_LOOT_CHANCE = 0.55F;
    public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5F;
    public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25F;
    public static final String LEASH_TAG = "Leash";
    private static final int PICKUP_REACH = 1;
    public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
    public static final int UPDATE_GOAL_SELECTOR_EVERY_N_TICKS = 2;
    public int ambientSoundTime;
    protected int xpReward;
    protected LookControl lookControl;
    protected MoveControl moveControl;
    protected JumpControl jumpControl;
    private final BodyRotationControl bodyRotationControl;
    protected PathNavigation navigation;
    public GoalSelector goalSelector;
    public GoalSelector targetSelector;
    @Nullable
    private LivingEntity target;
    private final Sensing sensing;
    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    public final float[] handDropChances = new float[2];
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
    public final float[] armorDropChances = new float[4];
    private boolean canPickUpLoot;
    private boolean persistenceRequired;
    private final Map<BlockPathTypes, Float> pathfindingMalus = Maps.newEnumMap(BlockPathTypes.class);
    @Nullable
    public ResourceLocation lootTable;
    public long lootTableSeed;
    @Nullable
    public Entity leashHolder;
    private int delayedLeashHolderId;
    @Nullable
    private CompoundTag leashInfoTag;
    private BlockPos restrictCenter = BlockPos.ZERO;
    private float restrictRadius = -1.0F;

    protected Mob(EntityType<? extends Mob> type, Level world) {
        super(type, world);
        this.goalSelector = new GoalSelector(world.getProfilerSupplier());
        this.targetSelector = new GoalSelector(world.getProfilerSupplier());
        this.lookControl = new LookControl(this);
        this.moveControl = new MoveControl(this);
        this.jumpControl = new JumpControl(this);
        this.bodyRotationControl = this.createBodyControl();
        this.navigation = this.createNavigation(world);
        this.sensing = new Sensing(this);
        Arrays.fill(this.armorDropChances, 0.085F);
        Arrays.fill(this.handDropChances, 0.085F);
        if (world != null && !world.isClientSide) {
            this.registerGoals();
        }

    }

    protected void registerGoals() {
    }

    public static AttributeSupplier.Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.ATTACK_KNOCKBACK);
    }

    protected PathNavigation createNavigation(Level world) {
        return new GroundPathNavigation(this, world);
    }

    protected boolean shouldPassengersInheritMalus() {
        return false;
    }

    public float getPathfindingMalus(BlockPathTypes nodeType) {
        Mob mob;
        if (this.getVehicle() instanceof Mob && ((Mob)this.getVehicle()).shouldPassengersInheritMalus()) {
            mob = (Mob)this.getVehicle();
        } else {
            mob = this;
        }

        Float float_ = mob.pathfindingMalus.get(nodeType);
        return float_ == null ? nodeType.getMalus() : float_;
    }

    public void setPathfindingMalus(BlockPathTypes nodeType, float penalty) {
        this.pathfindingMalus.put(nodeType, penalty);
    }

    public boolean canCutCorner(BlockPathTypes type) {
        return type != BlockPathTypes.DANGER_FIRE && type != BlockPathTypes.DANGER_CACTUS && type != BlockPathTypes.DANGER_OTHER && type != BlockPathTypes.WALKABLE_DOOR;
    }

    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this);
    }

    public LookControl getLookControl() {
        return this.lookControl;
    }

    public MoveControl getMoveControl() {
        if (this.isPassenger() && this.getVehicle() instanceof Mob) {
            Mob mob = (Mob)this.getVehicle();
            return mob.getMoveControl();
        } else {
            return this.moveControl;
        }
    }

    public JumpControl getJumpControl() {
        return this.jumpControl;
    }

    public PathNavigation getNavigation() {
        if (this.isPassenger() && this.getVehicle() instanceof Mob) {
            Mob mob = (Mob)this.getVehicle();
            return mob.getNavigation();
        } else {
            return this.navigation;
        }
    }

    public Sensing getSensing() {
        return this.sensing;
    }

    @Nullable
    public LivingEntity getTarget() {
        return this.target;
    }

    public void setTarget(@Nullable LivingEntity target) {
        this.target = target;
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != EntityType.GHAST;
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) {
        return false;
    }

    public void ate() {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MOB_FLAGS_ID, (byte)0);
    }

    public int getAmbientSoundInterval() {
        return 80;
    }

    public void playAmbientSound() {
        SoundEvent soundEvent = this.getAmbientSound();
        if (soundEvent != null) {
            this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.level.getProfiler().push("mobBaseTick");
        if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
            this.resetAmbientSoundTime();
            this.playAmbientSound();
        }

        this.level.getProfiler().pop();
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        this.resetAmbientSoundTime();
        super.playHurtSound(source);
    }

    private void resetAmbientSoundTime() {
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    @Override
    protected int getExperienceReward(Player player) {
        if (this.xpReward > 0) {
            int i = this.xpReward;

            for(int j = 0; j < this.armorItems.size(); ++j) {
                if (!this.armorItems.get(j).isEmpty() && this.armorDropChances[j] <= 1.0F) {
                    i += 1 + this.random.nextInt(3);
                }
            }

            for(int k = 0; k < this.handItems.size(); ++k) {
                if (!this.handItems.get(k).isEmpty() && this.handDropChances[k] <= 1.0F) {
                    i += 1 + this.random.nextInt(3);
                }
            }

            return i;
        } else {
            return this.xpReward;
        }
    }

    public void spawnAnim() {
        if (this.level.isClientSide) {
            for(int i = 0; i < 20; ++i) {
                double d = this.random.nextGaussian() * 0.02D;
                double e = this.random.nextGaussian() * 0.02D;
                double f = this.random.nextGaussian() * 0.02D;
                double g = 10.0D;
                this.level.addParticle(ParticleTypes.POOF, this.getX(1.0D) - d * 10.0D, this.getRandomY() - e * 10.0D, this.getRandomZ(1.0D) - f * 10.0D, d, e, f);
            }
        } else {
            this.level.broadcastEntityEvent(this, (byte)20);
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 20) {
            this.spawnAnim();
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            this.tickLeash();
            if (this.tickCount % 5 == 0) {
                this.updateControlFlags();
            }
        }

    }

    protected void updateControlFlags() {
        boolean bl = !(this.getControllingPassenger() instanceof Mob);
        boolean bl2 = !(this.getVehicle() instanceof Boat);
        this.goalSelector.setControlFlag(Goal.Flag.MOVE, bl);
        this.goalSelector.setControlFlag(Goal.Flag.JUMP, bl && bl2);
        this.goalSelector.setControlFlag(Goal.Flag.LOOK, bl);
    }

    @Override
    protected float tickHeadTurn(float bodyRotation, float headRotation) {
        this.bodyRotationControl.clientTick();
        return headRotation;
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("CanPickUpLoot", this.canPickUpLoot());
        nbt.putBoolean("PersistenceRequired", this.persistenceRequired);
        ListTag listTag = new ListTag();

        for(ItemStack itemStack : this.armorItems) {
            CompoundTag compoundTag = new CompoundTag();
            if (!itemStack.isEmpty()) {
                itemStack.save(compoundTag);
            }

            listTag.add(compoundTag);
        }

        nbt.put("ArmorItems", listTag);
        ListTag listTag2 = new ListTag();

        for(ItemStack itemStack2 : this.handItems) {
            CompoundTag compoundTag2 = new CompoundTag();
            if (!itemStack2.isEmpty()) {
                itemStack2.save(compoundTag2);
            }

            listTag2.add(compoundTag2);
        }

        nbt.put("HandItems", listTag2);
        ListTag listTag3 = new ListTag();

        for(float f : this.armorDropChances) {
            listTag3.add(FloatTag.valueOf(f));
        }

        nbt.put("ArmorDropChances", listTag3);
        ListTag listTag4 = new ListTag();

        for(float g : this.handDropChances) {
            listTag4.add(FloatTag.valueOf(g));
        }

        nbt.put("HandDropChances", listTag4);
        if (this.leashHolder != null) {
            CompoundTag compoundTag3 = new CompoundTag();
            if (this.leashHolder instanceof LivingEntity) {
                UUID uUID = this.leashHolder.getUUID();
                compoundTag3.putUUID("UUID", uUID);
            } else if (this.leashHolder instanceof HangingEntity) {
                BlockPos blockPos = ((HangingEntity)this.leashHolder).getPos();
                compoundTag3.putInt("X", blockPos.getX());
                compoundTag3.putInt("Y", blockPos.getY());
                compoundTag3.putInt("Z", blockPos.getZ());
            }

            nbt.put("Leash", compoundTag3);
        } else if (this.leashInfoTag != null) {
            nbt.put("Leash", this.leashInfoTag.copy());
        }

        nbt.putBoolean("LeftHanded", this.isLeftHanded());
        if (this.lootTable != null) {
            nbt.putString("DeathLootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                nbt.putLong("DeathLootTableSeed", this.lootTableSeed);
            }
        }

        if (this.isNoAi()) {
            nbt.putBoolean("NoAI", this.isNoAi());
        }

    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("CanPickUpLoot", 1)) {
            this.setCanPickUpLoot(nbt.getBoolean("CanPickUpLoot"));
        }

        this.persistenceRequired = nbt.getBoolean("PersistenceRequired");
        if (nbt.contains("ArmorItems", 9)) {
            ListTag listTag = nbt.getList("ArmorItems", 10);

            for(int i = 0; i < this.armorItems.size(); ++i) {
                this.armorItems.set(i, ItemStack.of(listTag.getCompound(i)));
            }
        }

        if (nbt.contains("HandItems", 9)) {
            ListTag listTag2 = nbt.getList("HandItems", 10);

            for(int j = 0; j < this.handItems.size(); ++j) {
                this.handItems.set(j, ItemStack.of(listTag2.getCompound(j)));
            }
        }

        if (nbt.contains("ArmorDropChances", 9)) {
            ListTag listTag3 = nbt.getList("ArmorDropChances", 5);

            for(int k = 0; k < listTag3.size(); ++k) {
                this.armorDropChances[k] = listTag3.getFloat(k);
            }
        }

        if (nbt.contains("HandDropChances", 9)) {
            ListTag listTag4 = nbt.getList("HandDropChances", 5);

            for(int l = 0; l < listTag4.size(); ++l) {
                this.handDropChances[l] = listTag4.getFloat(l);
            }
        }

        if (nbt.contains("Leash", 10)) {
            this.leashInfoTag = nbt.getCompound("Leash");
        }

        this.setLeftHanded(nbt.getBoolean("LeftHanded"));
        if (nbt.contains("DeathLootTable", 8)) {
            this.lootTable = new ResourceLocation(nbt.getString("DeathLootTable"));
            this.lootTableSeed = nbt.getLong("DeathLootTableSeed");
        }

        this.setNoAi(nbt.getBoolean("NoAI"));
    }

    @Override
    protected void dropFromLootTable(DamageSource source, boolean causedByPlayer) {
        super.dropFromLootTable(source, causedByPlayer);
        this.lootTable = null;
    }

    @Override
    protected LootContext.Builder createLootContext(boolean causedByPlayer, DamageSource source) {
        return super.createLootContext(causedByPlayer, source).withOptionalRandomSeed(this.lootTableSeed, this.random);
    }

    @Override
    public final ResourceLocation getLootTable() {
        return this.lootTable == null ? this.getDefaultLootTable() : this.lootTable;
    }

    protected ResourceLocation getDefaultLootTable() {
        return super.getLootTable();
    }

    public void setZza(float forwardSpeed) {
        this.zza = forwardSpeed;
    }

    public void setYya(float upwardSpeed) {
        this.yya = upwardSpeed;
    }

    public void setXxa(float sidewaysSpeed) {
        this.xxa = sidewaysSpeed;
    }

    @Override
    public void setSpeed(float movementSpeed) {
        super.setSpeed(movementSpeed);
        this.setZza(movementSpeed);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.level.getProfiler().push("looting");
        if (!this.level.isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            for(ItemEntity itemEntity : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0D, 0.0D, 1.0D))) {
                if (!itemEntity.isRemoved() && !itemEntity.getItem().isEmpty() && !itemEntity.hasPickUpDelay() && this.wantsToPickUp(itemEntity.getItem())) {
                    this.pickUpItem(itemEntity);
                }
            }
        }

        this.level.getProfiler().pop();
    }

    protected void pickUpItem(ItemEntity item) {
        ItemStack itemStack = item.getItem();
        if (this.equipItemIfPossible(itemStack)) {
            this.onItemPickup(item);
            this.take(item, itemStack.getCount());
            item.discard();
        }

    }

    public boolean equipItemIfPossible(ItemStack equipment) {
        EquipmentSlot equipmentSlot = getEquipmentSlotForItem(equipment);
        ItemStack itemStack = this.getItemBySlot(equipmentSlot);
        boolean bl = this.canReplaceCurrentItem(equipment, itemStack);
        if (bl && this.canHoldItem(equipment)) {
            double d = (double)this.getEquipmentDropChance(equipmentSlot);
            if (!itemStack.isEmpty() && (double)Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d) {
                this.spawnAtLocation(itemStack);
            }

            this.setItemSlotAndDropWhenKilled(equipmentSlot, equipment);
            this.equipEventAndSound(equipment);
            return true;
        } else {
            return false;
        }
    }

    protected void setItemSlotAndDropWhenKilled(EquipmentSlot slot, ItemStack stack) {
        this.setItemSlot(slot, stack);
        this.setGuaranteedDrop(slot);
        this.persistenceRequired = true;
    }

    public void setGuaranteedDrop(EquipmentSlot slot) {
        switch(slot.getType()) {
        case HAND:
            this.handDropChances[slot.getIndex()] = 2.0F;
            break;
        case ARMOR:
            this.armorDropChances[slot.getIndex()] = 2.0F;
        }

    }

    protected boolean canReplaceCurrentItem(ItemStack newStack, ItemStack oldStack) {
        if (oldStack.isEmpty()) {
            return true;
        } else if (newStack.getItem() instanceof SwordItem) {
            if (!(oldStack.getItem() instanceof SwordItem)) {
                return true;
            } else {
                SwordItem swordItem = (SwordItem)newStack.getItem();
                SwordItem swordItem2 = (SwordItem)oldStack.getItem();
                if (swordItem.getDamage() != swordItem2.getDamage()) {
                    return swordItem.getDamage() > swordItem2.getDamage();
                } else {
                    return this.canReplaceEqualItem(newStack, oldStack);
                }
            }
        } else if (newStack.getItem() instanceof BowItem && oldStack.getItem() instanceof BowItem) {
            return this.canReplaceEqualItem(newStack, oldStack);
        } else if (newStack.getItem() instanceof CrossbowItem && oldStack.getItem() instanceof CrossbowItem) {
            return this.canReplaceEqualItem(newStack, oldStack);
        } else if (newStack.getItem() instanceof ArmorItem) {
            if (EnchantmentHelper.hasBindingCurse(oldStack)) {
                return false;
            } else if (!(oldStack.getItem() instanceof ArmorItem)) {
                return true;
            } else {
                ArmorItem armorItem = (ArmorItem)newStack.getItem();
                ArmorItem armorItem2 = (ArmorItem)oldStack.getItem();
                if (armorItem.getDefense() != armorItem2.getDefense()) {
                    return armorItem.getDefense() > armorItem2.getDefense();
                } else if (armorItem.getToughness() != armorItem2.getToughness()) {
                    return armorItem.getToughness() > armorItem2.getToughness();
                } else {
                    return this.canReplaceEqualItem(newStack, oldStack);
                }
            }
        } else {
            if (newStack.getItem() instanceof DiggerItem) {
                if (oldStack.getItem() instanceof BlockItem) {
                    return true;
                }

                if (oldStack.getItem() instanceof DiggerItem) {
                    DiggerItem diggerItem = (DiggerItem)newStack.getItem();
                    DiggerItem diggerItem2 = (DiggerItem)oldStack.getItem();
                    if (diggerItem.getAttackDamage() != diggerItem2.getAttackDamage()) {
                        return diggerItem.getAttackDamage() > diggerItem2.getAttackDamage();
                    }

                    return this.canReplaceEqualItem(newStack, oldStack);
                }
            }

            return false;
        }
    }

    public boolean canReplaceEqualItem(ItemStack newStack, ItemStack oldStack) {
        if (newStack.getDamageValue() >= oldStack.getDamageValue() && (!newStack.hasTag() || oldStack.hasTag())) {
            if (newStack.hasTag() && oldStack.hasTag()) {
                return newStack.getTag().getAllKeys().stream().anyMatch((string) -> {
                    return !string.equals("Damage");
                }) && !oldStack.getTag().getAllKeys().stream().anyMatch((string) -> {
                    return !string.equals("Damage");
                });
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean canHoldItem(ItemStack stack) {
        return true;
    }

    public boolean wantsToPickUp(ItemStack stack) {
        return this.canHoldItem(stack);
    }

    public boolean removeWhenFarAway(double distanceSquared) {
        return true;
    }

    public boolean requiresCustomPersistence() {
        return this.isPassenger();
    }

    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void checkDespawn() {
        if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            this.discard();
        } else if (!this.isPersistenceRequired() && !this.requiresCustomPersistence()) {
            Entity entity = this.level.getNearestPlayer(this, -1.0D);
            if (entity != null) {
                double d = entity.distanceToSqr(this);
                int i = this.getType().getCategory().getDespawnDistance();
                int j = i * i;
                if (d > (double)j && this.removeWhenFarAway(d)) {
                    this.discard();
                }

                int k = this.getType().getCategory().getNoDespawnDistance();
                int l = k * k;
                if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && d > (double)l && this.removeWhenFarAway(d)) {
                    this.discard();
                } else if (d < (double)l) {
                    this.noActionTime = 0;
                }
            }

        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    protected final void serverAiStep() {
        ++this.noActionTime;
        this.level.getProfiler().push("sensing");
        this.sensing.tick();
        this.level.getProfiler().pop();
        int i = this.level.getServer().getTickCount() + this.getId();
        if (i % 2 != 0 && this.tickCount > 1) {
            this.level.getProfiler().push("targetSelector");
            this.targetSelector.tickRunningGoals(false);
            this.level.getProfiler().pop();
            this.level.getProfiler().push("goalSelector");
            this.goalSelector.tickRunningGoals(false);
            this.level.getProfiler().pop();
        } else {
            this.level.getProfiler().push("targetSelector");
            this.targetSelector.tick();
            this.level.getProfiler().pop();
            this.level.getProfiler().push("goalSelector");
            this.goalSelector.tick();
            this.level.getProfiler().pop();
        }

        this.level.getProfiler().push("navigation");
        this.navigation.tick();
        this.level.getProfiler().pop();
        this.level.getProfiler().push("mob tick");
        this.customServerAiStep();
        this.level.getProfiler().pop();
        this.level.getProfiler().push("controls");
        this.level.getProfiler().push("move");
        this.moveControl.tick();
        this.level.getProfiler().popPush("look");
        this.lookControl.tick();
        this.level.getProfiler().popPush("jump");
        this.jumpControl.tick();
        this.level.getProfiler().pop();
        this.level.getProfiler().pop();
        this.sendDebugPackets();
    }

    protected void sendDebugPackets() {
        DebugPackets.sendGoalSelector(this.level, this, this.goalSelector);
    }

    protected void customServerAiStep() {
    }

    public int getMaxHeadXRot() {
        return 40;
    }

    public int getMaxHeadYRot() {
        return 75;
    }

    public int getHeadRotSpeed() {
        return 10;
    }

    public void lookAt(Entity targetEntity, float maxYawChange, float maxPitchChange) {
        double d = targetEntity.getX() - this.getX();
        double e = targetEntity.getZ() - this.getZ();
        double f;
        if (targetEntity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)targetEntity;
            f = livingEntity.getEyeY() - this.getEyeY();
        } else {
            f = (targetEntity.getBoundingBox().minY + targetEntity.getBoundingBox().maxY) / 2.0D - this.getEyeY();
        }

        double h = Math.sqrt(d * d + e * e);
        float i = (float)(Mth.atan2(e, d) * (double)(180F / (float)Math.PI)) - 90.0F;
        float j = (float)(-(Mth.atan2(f, h) * (double)(180F / (float)Math.PI)));
        this.setXRot(this.rotlerp(this.getXRot(), j, maxPitchChange));
        this.setYRot(this.rotlerp(this.getYRot(), i, maxYawChange));
    }

    private float rotlerp(float from, float to, float max) {
        float f = Mth.wrapDegrees(to - from);
        if (f > max) {
            f = max;
        }

        if (f < -max) {
            f = -max;
        }

        return from + f;
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> type, LevelAccessor world, MobSpawnType spawnReason, BlockPos pos, Random random) {
        BlockPos blockPos = pos.below();
        return spawnReason == MobSpawnType.SPAWNER || world.getBlockState(blockPos).isValidSpawn(world, blockPos, type);
    }

    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType spawnReason) {
        return true;
    }

    public boolean checkSpawnObstruction(LevelReader world) {
        return !world.containsAnyLiquid(this.getBoundingBox()) && world.isUnobstructed(this);
    }

    public int getMaxSpawnClusterSize() {
        return 4;
    }

    public boolean isMaxGroupSizeReached(int count) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        if (this.getTarget() == null) {
            return 3;
        } else {
            int i = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
            i -= (3 - this.level.getDifficulty().getId()) * 4;
            if (i < 0) {
                i = 0;
            }

            return i + 3;
        }
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return this.handItems;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.armorItems;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        switch(slot.getType()) {
        case HAND:
            return this.handItems.get(slot.getIndex());
        case ARMOR:
            return this.armorItems.get(slot.getIndex());
        default:
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        this.verifyEquippedItem(stack);
        switch(slot.getType()) {
        case HAND:
            this.handItems.set(slot.getIndex(), stack);
            break;
        case ARMOR:
            this.armorItems.set(slot.getIndex(), stack);
        }

    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropCustomDeathLoot(source, lootingMultiplier, allowDrops);

        for(EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            ItemStack itemStack = this.getItemBySlot(equipmentSlot);
            float f = this.getEquipmentDropChance(equipmentSlot);
            boolean bl = f > 1.0F;
            if (!itemStack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemStack) && (allowDrops || bl) && Math.max(this.random.nextFloat() - (float)lootingMultiplier * 0.01F, 0.0F) < f) {
                if (!bl && itemStack.isDamageableItem()) {
                    itemStack.setDamageValue(itemStack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemStack.getMaxDamage() - 3, 1))));
                }

                this.spawnAtLocation(itemStack);
                this.setItemSlot(equipmentSlot, ItemStack.EMPTY);
            }
        }

    }

    protected float getEquipmentDropChance(EquipmentSlot slot) {
        float f;
        switch(slot.getType()) {
        case HAND:
            f = this.handDropChances[slot.getIndex()];
            break;
        case ARMOR:
            f = this.armorDropChances[slot.getIndex()];
            break;
        default:
            f = 0.0F;
        }

        return f;
    }

    protected void populateDefaultEquipmentSlots(DifficultyInstance difficulty) {
        if (this.random.nextFloat() < 0.15F * difficulty.getSpecialMultiplier()) {
            int i = this.random.nextInt(2);
            float f = this.level.getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
            if (this.random.nextFloat() < 0.095F) {
                ++i;
            }

            if (this.random.nextFloat() < 0.095F) {
                ++i;
            }

            if (this.random.nextFloat() < 0.095F) {
                ++i;
            }

            boolean bl = true;

            for(EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                if (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR) {
                    ItemStack itemStack = this.getItemBySlot(equipmentSlot);
                    if (!bl && this.random.nextFloat() < f) {
                        break;
                    }

                    bl = false;
                    if (itemStack.isEmpty()) {
                        Item item = getEquipmentForSlot(equipmentSlot, i);
                        if (item != null) {
                            this.setItemSlot(equipmentSlot, new ItemStack(item));
                        }
                    }
                }
            }
        }

    }

    @Nullable
    public static Item getEquipmentForSlot(EquipmentSlot equipmentSlot, int equipmentLevel) {
        switch(equipmentSlot) {
        case HEAD:
            if (equipmentLevel == 0) {
                return Items.LEATHER_HELMET;
            } else if (equipmentLevel == 1) {
                return Items.GOLDEN_HELMET;
            } else if (equipmentLevel == 2) {
                return Items.CHAINMAIL_HELMET;
            } else if (equipmentLevel == 3) {
                return Items.IRON_HELMET;
            } else if (equipmentLevel == 4) {
                return Items.DIAMOND_HELMET;
            }
        case CHEST:
            if (equipmentLevel == 0) {
                return Items.LEATHER_CHESTPLATE;
            } else if (equipmentLevel == 1) {
                return Items.GOLDEN_CHESTPLATE;
            } else if (equipmentLevel == 2) {
                return Items.CHAINMAIL_CHESTPLATE;
            } else if (equipmentLevel == 3) {
                return Items.IRON_CHESTPLATE;
            } else if (equipmentLevel == 4) {
                return Items.DIAMOND_CHESTPLATE;
            }
        case LEGS:
            if (equipmentLevel == 0) {
                return Items.LEATHER_LEGGINGS;
            } else if (equipmentLevel == 1) {
                return Items.GOLDEN_LEGGINGS;
            } else if (equipmentLevel == 2) {
                return Items.CHAINMAIL_LEGGINGS;
            } else if (equipmentLevel == 3) {
                return Items.IRON_LEGGINGS;
            } else if (equipmentLevel == 4) {
                return Items.DIAMOND_LEGGINGS;
            }
        case FEET:
            if (equipmentLevel == 0) {
                return Items.LEATHER_BOOTS;
            } else if (equipmentLevel == 1) {
                return Items.GOLDEN_BOOTS;
            } else if (equipmentLevel == 2) {
                return Items.CHAINMAIL_BOOTS;
            } else if (equipmentLevel == 3) {
                return Items.IRON_BOOTS;
            } else if (equipmentLevel == 4) {
                return Items.DIAMOND_BOOTS;
            }
        default:
            return null;
        }
    }

    protected void populateDefaultEquipmentEnchantments(DifficultyInstance difficulty) {
        float f = difficulty.getSpecialMultiplier();
        this.enchantSpawnedWeapon(f);

        for(EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR) {
                this.enchantSpawnedArmor(f, equipmentSlot);
            }
        }

    }

    protected void enchantSpawnedWeapon(float power) {
        if (!this.getMainHandItem().isEmpty() && this.random.nextFloat() < 0.25F * power) {
            this.setItemSlot(EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(this.random, this.getMainHandItem(), (int)(5.0F + power * (float)this.random.nextInt(18)), false));
        }

    }

    protected void enchantSpawnedArmor(float power, EquipmentSlot slot) {
        ItemStack itemStack = this.getItemBySlot(slot);
        if (!itemStack.isEmpty() && this.random.nextFloat() < 0.5F * power) {
            this.setItemSlot(slot, EnchantmentHelper.enchantItem(this.random, itemStack, (int)(5.0F + power * (float)this.random.nextInt(18)), false));
        }

    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        this.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus", this.random.nextGaussian() * 0.05D, AttributeModifier.Operation.MULTIPLY_BASE));
        if (this.random.nextFloat() < 0.05F) {
            this.setLeftHanded(true);
        } else {
            this.setLeftHanded(false);
        }

        return entityData;
    }

    public boolean canBeControlledByRider() {
        return false;
    }

    public void setPersistenceRequired() {
        this.persistenceRequired = true;
    }

    public void setDropChance(EquipmentSlot slot, float chance) {
        switch(slot.getType()) {
        case HAND:
            this.handDropChances[slot.getIndex()] = chance;
            break;
        case ARMOR:
            this.armorDropChances[slot.getIndex()] = chance;
        }

    }

    public boolean canPickUpLoot() {
        return this.canPickUpLoot;
    }

    public void setCanPickUpLoot(boolean canPickUpLoot) {
        this.canPickUpLoot = canPickUpLoot;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EquipmentSlot equipmentSlot = getEquipmentSlotForItem(stack);
        return this.getItemBySlot(equipmentSlot).isEmpty() && this.canPickUpLoot();
    }

    public boolean isPersistenceRequired() {
        return this.persistenceRequired;
    }

    @Override
    public final InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.isAlive()) {
            return InteractionResult.PASS;
        } else if (this.getLeashHolder() == player) {
            this.dropLeash(true, !player.getAbilities().instabuild);
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            InteractionResult interactionResult = this.checkAndHandleImportantInteractions(player, hand);
            if (interactionResult.consumesAction()) {
                return interactionResult;
            } else {
                interactionResult = this.mobInteract(player, hand);
                return interactionResult.consumesAction() ? interactionResult : super.interact(player, hand);
            }
        }
    }

    private InteractionResult checkAndHandleImportantInteractions(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.LEAD) && this.canBeLeashed(player)) {
            this.setLeashedTo(player, true);
            itemStack.shrink(1);
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            if (itemStack.is(Items.NAME_TAG)) {
                InteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
                if (interactionResult.consumesAction()) {
                    return interactionResult;
                }
            }

            if (itemStack.getItem() instanceof SpawnEggItem) {
                if (this.level instanceof ServerLevel) {
                    SpawnEggItem spawnEggItem = (SpawnEggItem)itemStack.getItem();
                    Optional<Mob> optional = spawnEggItem.spawnOffspringFromSpawnEgg(player, this, this.getType(), (ServerLevel)this.level, this.position(), itemStack);
                    optional.ifPresent((entity) -> {
                        this.onOffspringSpawnedFromEgg(player, entity);
                    });
                    return optional.isPresent() ? InteractionResult.SUCCESS : InteractionResult.PASS;
                } else {
                    return InteractionResult.CONSUME;
                }
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    protected void onOffspringSpawnedFromEgg(Player player, Mob child) {
    }

    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public boolean isWithinRestriction() {
        return this.isWithinRestriction(this.blockPosition());
    }

    public boolean isWithinRestriction(BlockPos pos) {
        if (this.restrictRadius == -1.0F) {
            return true;
        } else {
            return this.restrictCenter.distSqr(pos) < (double)(this.restrictRadius * this.restrictRadius);
        }
    }

    public void restrictTo(BlockPos target, int range) {
        this.restrictCenter = target;
        this.restrictRadius = (float)range;
    }

    public BlockPos getRestrictCenter() {
        return this.restrictCenter;
    }

    public float getRestrictRadius() {
        return this.restrictRadius;
    }

    public void clearRestriction() {
        this.restrictRadius = -1.0F;
    }

    public boolean hasRestriction() {
        return this.restrictRadius != -1.0F;
    }

    @Nullable
    public <T extends Mob> T convertTo(EntityType<T> entityType, boolean keepEquipment) {
        if (this.isRemoved()) {
            return (T)null;
        } else {
            T mob = entityType.create(this.level);
            mob.copyPosition(this);
            mob.setBaby(this.isBaby());
            mob.setNoAi(this.isNoAi());
            if (this.hasCustomName()) {
                mob.setCustomName(this.getCustomName());
                mob.setCustomNameVisible(this.isCustomNameVisible());
            }

            if (this.isPersistenceRequired()) {
                mob.setPersistenceRequired();
            }

            mob.setInvulnerable(this.isInvulnerable());
            if (keepEquipment) {
                mob.setCanPickUpLoot(this.canPickUpLoot());

                for(EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                    ItemStack itemStack = this.getItemBySlot(equipmentSlot);
                    if (!itemStack.isEmpty()) {
                        mob.setItemSlot(equipmentSlot, itemStack.copy());
                        mob.setDropChance(equipmentSlot, this.getEquipmentDropChance(equipmentSlot));
                        itemStack.setCount(0);
                    }
                }
            }

            this.level.addFreshEntity(mob);
            if (this.isPassenger()) {
                Entity entity = this.getVehicle();
                this.stopRiding();
                mob.startRiding(entity, true);
            }

            this.discard();
            return mob;
        }
    }

    protected void tickLeash() {
        if (this.leashInfoTag != null) {
            this.restoreLeashFromSave();
        }

        if (this.leashHolder != null) {
            if (!this.isAlive() || !this.leashHolder.isAlive()) {
                this.dropLeash(true, true);
            }

        }
    }

    public void dropLeash(boolean sendPacket, boolean dropItem) {
        if (this.leashHolder != null) {
            this.leashHolder = null;
            this.leashInfoTag = null;
            if (!this.level.isClientSide && dropItem) {
                this.spawnAtLocation(Items.LEAD);
            }

            if (!this.level.isClientSide && sendPacket && this.level instanceof ServerLevel) {
                ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, (Entity)null));
            }
        }

    }

    public boolean canBeLeashed(Player player) {
        return !this.isLeashed() && !(this instanceof Enemy);
    }

    public boolean isLeashed() {
        return this.leashHolder != null;
    }

    @Nullable
    public Entity getLeashHolder() {
        if (this.leashHolder == null && this.delayedLeashHolderId != 0 && this.level.isClientSide) {
            this.leashHolder = this.level.getEntity(this.delayedLeashHolderId);
        }

        return this.leashHolder;
    }

    public void setLeashedTo(Entity entity, boolean sendPacket) {
        this.leashHolder = entity;
        this.leashInfoTag = null;
        if (!this.level.isClientSide && sendPacket && this.level instanceof ServerLevel) {
            ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, this.leashHolder));
        }

        if (this.isPassenger()) {
            this.stopRiding();
        }

    }

    public void setDelayedLeashHolderId(int id) {
        this.delayedLeashHolderId = id;
        this.dropLeash(false, false);
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        boolean bl = super.startRiding(entity, force);
        if (bl && this.isLeashed()) {
            this.dropLeash(true, true);
        }

        return bl;
    }

    private void restoreLeashFromSave() {
        if (this.leashInfoTag != null && this.level instanceof ServerLevel) {
            if (this.leashInfoTag.hasUUID("UUID")) {
                UUID uUID = this.leashInfoTag.getUUID("UUID");
                Entity entity = ((ServerLevel)this.level).getEntity(uUID);
                if (entity != null) {
                    this.setLeashedTo(entity, true);
                    return;
                }
            } else if (this.leashInfoTag.contains("X", 99) && this.leashInfoTag.contains("Y", 99) && this.leashInfoTag.contains("Z", 99)) {
                BlockPos blockPos = NbtUtils.readBlockPos(this.leashInfoTag);
                this.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(this.level, blockPos), true);
                return;
            }

            if (this.tickCount > 100) {
                this.spawnAtLocation(Items.LEAD);
                this.leashInfoTag = null;
            }
        }

    }

    @Override
    public boolean isControlledByLocalInstance() {
        return this.canBeControlledByRider() && super.isControlledByLocalInstance();
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && !this.isNoAi();
    }

    public void setNoAi(boolean aiDisabled) {
        byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, aiDisabled ? (byte)(b | 1) : (byte)(b & -2));
    }

    public void setLeftHanded(boolean leftHanded) {
        byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, leftHanded ? (byte)(b | 2) : (byte)(b & -3));
    }

    public void setAggressive(boolean attacking) {
        byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, attacking ? (byte)(b | 4) : (byte)(b & -5));
    }

    public boolean isNoAi() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
    }

    public boolean isLeftHanded() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
    }

    public boolean isAggressive() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
    }

    public void setBaby(boolean baby) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public double getMeleeAttackRangeSqr(LivingEntity target) {
        return (double)(this.getBbWidth() * 2.0F * this.getBbWidth() * 2.0F + target.getBbWidth());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float g = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        if (target instanceof LivingEntity) {
            f += EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)target).getMobType());
            g += (float)EnchantmentHelper.getKnockbackBonus(this);
        }

        int i = EnchantmentHelper.getFireAspect(this);
        if (i > 0) {
            target.setSecondsOnFire(i * 4);
        }

        boolean bl = target.hurt(DamageSource.mobAttack(this), f);
        if (bl) {
            if (g > 0.0F && target instanceof LivingEntity) {
                ((LivingEntity)target).knockback((double)(g * 0.5F), (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }

            if (target instanceof Player) {
                Player player = (Player)target;
                this.maybeDisableShield(player, this.getMainHandItem(), player.isUsingItem() ? player.getUseItem() : ItemStack.EMPTY);
            }

            this.doEnchantDamageEffects(this, target);
            this.setLastHurtMob(target);
        }

        return bl;
    }

    private void maybeDisableShield(Player player, ItemStack mobStack, ItemStack playerStack) {
        if (!mobStack.isEmpty() && !playerStack.isEmpty() && mobStack.getItem() instanceof AxeItem && playerStack.is(Items.SHIELD)) {
            float f = 0.25F + (float)EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
            if (this.random.nextFloat() < f) {
                player.getCooldowns().addCooldown(Items.SHIELD, 100);
                this.level.broadcastEntityEvent(player, (byte)30);
            }
        }

    }

    public boolean isSunBurnTick() {
        if (this.level.isDay() && !this.level.isClientSide) {
            float f = this.getBrightness();
            BlockPos blockPos = new BlockPos(this.getX(), this.getEyeY(), this.getZ());
            boolean bl = this.isInWaterRainOrBubble() || this.isInPowderSnow || this.wasInPowderSnow;
            if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !bl && this.level.canSeeSky(blockPos)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void jumpInLiquid(TagKey<Fluid> fluid) {
        if (this.getNavigation().canFloat()) {
            super.jumpInLiquid(fluid);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.3D, 0.0D));
        }

    }

    public void removeFreeWill() {
        this.goalSelector.removeAllGoals();
        this.getBrain().removeAllBehaviors();
    }

    @Override
    protected void removeAfterChangingDimensions() {
        super.removeAfterChangingDimensions();
        this.dropLeash(true, false);
        this.getAllSlots().forEach((stack) -> {
            stack.setCount(0);
        });
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        SpawnEggItem spawnEggItem = SpawnEggItem.byId(this.getType());
        return spawnEggItem == null ? null : new ItemStack(spawnEggItem);
    }
}
