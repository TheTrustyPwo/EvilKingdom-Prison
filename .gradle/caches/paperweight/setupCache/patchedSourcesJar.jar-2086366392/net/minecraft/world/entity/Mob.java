package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.LootContext;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.EntityUnleashEvent.UnleashReason;
// CraftBukkit end

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
    @Nullable public net.minecraft.world.entity.ai.goal.FloatGoal goalFloat; // Paper
    public GoalSelector targetSelector;
    @Nullable
    private LivingEntity target;
    private final Sensing sensing;
    private final NonNullList<ItemStack> handItems;
    public final float[] handDropChances;
    private final NonNullList<ItemStack> armorItems;
    public final float[] armorDropChances;
    private boolean canPickUpLoot;
    private boolean persistenceRequired;
    private final Map<BlockPathTypes, Float> pathfindingMalus;
    @Nullable
    public ResourceLocation lootTable;
    public long lootTableSeed;
    @Nullable
    public Entity leashHolder;
    private int delayedLeashHolderId;
    @Nullable
    private CompoundTag leashInfoTag;
    private BlockPos restrictCenter;
    private float restrictRadius;

    public boolean aware = true; // CraftBukkit

    protected Mob(EntityType<? extends Mob> type, Level world) {
        super(type, world);
        this.handItems = NonNullList.withSize(2, ItemStack.EMPTY);
        this.handDropChances = new float[2];
        this.armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
        this.armorDropChances = new float[4];
        this.pathfindingMalus = Maps.newEnumMap(BlockPathTypes.class);
        this.restrictCenter = BlockPos.ZERO;
        this.restrictRadius = -1.0F;
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

        // CraftBukkit start - default persistance to type's persistance value
        this.persistenceRequired = !this.removeWhenFarAway(0);
    }

    public void setPersistenceRequired(boolean persistenceRequired) {
        this.persistenceRequired = persistenceRequired;
    }
    // CraftBukkit end

    protected void registerGoals() {}

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
        Mob entityinsentient;

        if (this.getVehicle() instanceof Mob && ((Mob) this.getVehicle()).shouldPassengersInheritMalus()) {
            entityinsentient = (Mob) this.getVehicle();
        } else {
            entityinsentient = this;
        }

        Float ofloat = (Float) entityinsentient.pathfindingMalus.get(nodeType);

        return ofloat == null ? nodeType.getMalus() : ofloat;
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

    // Paper start
    @Override
    public void inactiveTick() {
        super.inactiveTick();
        if (this.goalSelector.inactiveTick()) {
            this.goalSelector.tick();
        }
        if (this.targetSelector.inactiveTick()) {
            this.targetSelector.tick();
        }
    }
    // Paper end

    public MoveControl getMoveControl() {
        if (this.isPassenger() && this.getVehicle() instanceof Mob) {
            Mob entityinsentient = (Mob) this.getVehicle();

            return entityinsentient.getMoveControl();
        } else {
            return this.moveControl;
        }
    }

    public JumpControl getJumpControl() {
        return this.jumpControl;
    }

    public PathNavigation getNavigation() {
        if (this.isPassenger() && this.getVehicle() instanceof Mob) {
            Mob entityinsentient = (Mob) this.getVehicle();

            return entityinsentient.getNavigation();
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

    public org.bukkit.craftbukkit.v1_18_R2.entity.CraftMob getBukkitMob() { return (org.bukkit.craftbukkit.v1_18_R2.entity.CraftMob) super.getBukkitEntity(); } // Paper
    public void setTarget(@Nullable LivingEntity target) {
        // CraftBukkit start - fire event
        this.setTarget(target, EntityTargetEvent.TargetReason.UNKNOWN, true);
    }

    public boolean setTarget(LivingEntity entityliving, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (this.getTarget() == entityliving) return false;
        if (fireEvent) {
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && this.getTarget() != null && entityliving == null) {
                reason = this.getTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED;
            }
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN) {
                level.getCraftServer().getLogger().log(java.util.logging.Level.WARNING, "Unknown target reason, please report on the issue tracker", new Exception());
            }
            CraftLivingEntity ctarget = null;
            if (entityliving != null) {
                ctarget = (CraftLivingEntity) entityliving.getBukkitEntity();
            }
            EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(this.getBukkitEntity(), ctarget, reason);
            level.getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }

            if (event.getTarget() != null) {
                entityliving = ((CraftLivingEntity) event.getTarget()).getHandle();
            } else {
                entityliving = null;
            }
        }
        this.target = entityliving;
        return true;
        // CraftBukkit end
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != EntityType.GHAST;
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) {
        return false;
    }

    public void ate() {}

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(Mob.DATA_MOB_FLAGS_ID, (byte) 0);
    }

    public int getAmbientSoundInterval() {
        return 80;
    }

    public void playAmbientSound() {
        SoundEvent soundeffect = this.getAmbientSound();

        if (soundeffect != null) {
            this.playSound(soundeffect, this.getSoundVolume(), this.getVoicePitch());
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

            int j;

            for (j = 0; j < this.armorItems.size(); ++j) {
                if (!((ItemStack) this.armorItems.get(j)).isEmpty() && this.armorDropChances[j] <= 1.0F) {
                    i += 1 + this.random.nextInt(3);
                }
            }

            for (j = 0; j < this.handItems.size(); ++j) {
                if (!((ItemStack) this.handItems.get(j)).isEmpty() && this.handDropChances[j] <= 1.0F) {
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
            for (int i = 0; i < 20; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                double d3 = 10.0D;

                this.level.addParticle(ParticleTypes.POOF, this.getX(1.0D) - d0 * 10.0D, this.getRandomY() - d1 * 10.0D, this.getRandomZ(1.0D) - d2 * 10.0D, d0, d1, d2);
            }
        } else {
            this.level.broadcastEntityEvent(this, (byte) 20);
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
        boolean flag = !(this.getControllingPassenger() instanceof Mob);
        boolean flag1 = !(this.getVehicle() instanceof Boat);

        this.goalSelector.setControlFlag(Goal.Flag.MOVE, flag);
        this.goalSelector.setControlFlag(Goal.Flag.JUMP, flag && flag1);
        this.goalSelector.setControlFlag(Goal.Flag.LOOK, flag);
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
        ListTag nbttaglist = new ListTag();

        CompoundTag nbttagcompound1;

        for (Iterator iterator = this.armorItems.iterator(); iterator.hasNext(); nbttaglist.add(nbttagcompound1)) {
            ItemStack itemstack = (ItemStack) iterator.next();

            nbttagcompound1 = new CompoundTag();
            if (!itemstack.isEmpty()) {
                itemstack.save(nbttagcompound1);
            }
        }

        nbt.put("ArmorItems", nbttaglist);
        ListTag nbttaglist1 = new ListTag();

        CompoundTag nbttagcompound2;

        for (Iterator iterator1 = this.handItems.iterator(); iterator1.hasNext(); nbttaglist1.add(nbttagcompound2)) {
            ItemStack itemstack1 = (ItemStack) iterator1.next();

            nbttagcompound2 = new CompoundTag();
            if (!itemstack1.isEmpty()) {
                itemstack1.save(nbttagcompound2);
            }
        }

        nbt.put("HandItems", nbttaglist1);
        ListTag nbttaglist2 = new ListTag();
        float[] afloat = this.armorDropChances;
        int i = afloat.length;

        int j;

        for (j = 0; j < i; ++j) {
            float f = afloat[j];

            nbttaglist2.add(FloatTag.valueOf(f));
        }

        nbt.put("ArmorDropChances", nbttaglist2);
        ListTag nbttaglist3 = new ListTag();
        float[] afloat1 = this.handDropChances;

        j = afloat1.length;

        for (int k = 0; k < j; ++k) {
            float f1 = afloat1[k];

            nbttaglist3.add(FloatTag.valueOf(f1));
        }

        nbt.put("HandDropChances", nbttaglist3);
        if (this.leashHolder != null) {
            nbttagcompound2 = new CompoundTag();
            if (this.leashHolder instanceof LivingEntity) {
                UUID uuid = this.leashHolder.getUUID();

                nbttagcompound2.putUUID("UUID", uuid);
            } else if (this.leashHolder instanceof HangingEntity) {
                BlockPos blockposition = ((HangingEntity) this.leashHolder).getPos();

                nbttagcompound2.putInt("X", blockposition.getX());
                nbttagcompound2.putInt("Y", blockposition.getY());
                nbttagcompound2.putInt("Z", blockposition.getZ());
            }

            nbt.put("Leash", nbttagcompound2);
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

        nbt.putBoolean("Bukkit.Aware", this.aware); // CraftBukkit
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        // CraftBukkit start - If looting or persistence is false only use it if it was set after we started using it
        if (nbt.contains("CanPickUpLoot", 1)) {
            boolean data = nbt.getBoolean("CanPickUpLoot");
            if (isLevelAtLeast(nbt, 1) || data) {
                this.setCanPickUpLoot(data);
            }
        }

        boolean data = nbt.getBoolean("PersistenceRequired");
        if (isLevelAtLeast(nbt, 1) || data) {
            this.persistenceRequired = data;
        }
        // CraftBukkit end
        ListTag nbttaglist;
        int i;

        if (nbt.contains("ArmorItems", 9)) {
            nbttaglist = nbt.getList("ArmorItems", 10);

            for (i = 0; i < this.armorItems.size(); ++i) {
                this.armorItems.set(i, ItemStack.of(nbttaglist.getCompound(i)));
            }
        }

        if (nbt.contains("HandItems", 9)) {
            nbttaglist = nbt.getList("HandItems", 10);

            for (i = 0; i < this.handItems.size(); ++i) {
                this.handItems.set(i, ItemStack.of(nbttaglist.getCompound(i)));
            }
        }

        if (nbt.contains("ArmorDropChances", 9)) {
            nbttaglist = nbt.getList("ArmorDropChances", 5);

            for (i = 0; i < nbttaglist.size(); ++i) {
                this.armorDropChances[i] = nbttaglist.getFloat(i);
            }
        }

        if (nbt.contains("HandDropChances", 9)) {
            nbttaglist = nbt.getList("HandDropChances", 5);

            for (i = 0; i < nbttaglist.size(); ++i) {
                this.handDropChances[i] = nbttaglist.getFloat(i);
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
        // CraftBukkit start
        if (nbt.contains("Bukkit.Aware")) {
            this.aware = nbt.getBoolean("Bukkit.Aware");
        }
        // CraftBukkit end
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

    public ResourceLocation getDefaultLootTable() {
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
            List<ItemEntity> list = this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0D, 0.0D, 1.0D));
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                ItemEntity entityitem = (ItemEntity) iterator.next();

                if (!entityitem.isRemoved() && !entityitem.getItem().isEmpty() && !entityitem.hasPickUpDelay() && this.wantsToPickUp(entityitem.getItem())) {
                    // Paper Start
                    if (!entityitem.canMobPickup) {
                        continue;
                    }
                    // Paper End
                    this.pickUpItem(entityitem);
                }
            }
        }

        this.level.getProfiler().pop();
    }

    protected void pickUpItem(ItemEntity item) {
        ItemStack itemstack = item.getItem();

        if (this.equipItemIfPossible(itemstack, item)) { // CraftBukkit - add item
            this.onItemPickup(item);
            this.take(item, itemstack.getCount());
            item.discard();
        }

    }

    public boolean equipItemIfPossible(ItemStack equipment) {
        // CraftBukkit start - add item
        return this.equipItemIfPossible(equipment, null);
    }

    public boolean equipItemIfPossible(ItemStack itemstack, ItemEntity entityitem) {
        // CraftBukkit end
        EquipmentSlot enumitemslot = getEquipmentSlotForItem(itemstack);
        ItemStack itemstack1 = this.getItemBySlot(enumitemslot);
        boolean flag = this.canReplaceCurrentItem(itemstack, itemstack1);

        // CraftBukkit start
        boolean canPickup = flag && this.canHoldItem(itemstack);
        if (entityitem != null) {
            canPickup = !org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityPickupItemEvent(this, entityitem, 0, !canPickup).isCancelled();
        }
        if (canPickup) {
            // CraftBukkit end
            double d0 = (double) this.getEquipmentDropChance(enumitemslot);

            if (!itemstack1.isEmpty() && (double) Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
                this.forceDrops = true; // CraftBukkit
                this.spawnAtLocation(itemstack1);
                this.forceDrops = false; // CraftBukkit
            }

            this.setItemSlotAndDropWhenKilled(enumitemslot, itemstack);
            this.equipEventAndSound(itemstack);
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
        switch (slot.getType()) {
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
                SwordItem itemsword = (SwordItem) newStack.getItem();
                SwordItem itemsword1 = (SwordItem) oldStack.getItem();

                return itemsword.getDamage() != itemsword1.getDamage() ? itemsword.getDamage() > itemsword1.getDamage() : this.canReplaceEqualItem(newStack, oldStack);
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
                ArmorItem itemarmor = (ArmorItem) newStack.getItem();
                ArmorItem itemarmor1 = (ArmorItem) oldStack.getItem();

                return itemarmor.getDefense() != itemarmor1.getDefense() ? itemarmor.getDefense() > itemarmor1.getDefense() : (itemarmor.getToughness() != itemarmor1.getToughness() ? itemarmor.getToughness() > itemarmor1.getToughness() : this.canReplaceEqualItem(newStack, oldStack));
            }
        } else {
            if (newStack.getItem() instanceof DiggerItem) {
                if (oldStack.getItem() instanceof BlockItem) {
                    return true;
                }

                if (oldStack.getItem() instanceof DiggerItem) {
                    DiggerItem itemtool = (DiggerItem) newStack.getItem();
                    DiggerItem itemtool1 = (DiggerItem) oldStack.getItem();

                    if (itemtool.getAttackDamage() != itemtool1.getAttackDamage()) {
                        return itemtool.getAttackDamage() > itemtool1.getAttackDamage();
                    }

                    return this.canReplaceEqualItem(newStack, oldStack);
                }
            }

            return false;
        }
    }

    public boolean canReplaceEqualItem(ItemStack newStack, ItemStack oldStack) {
        return newStack.getDamageValue() >= oldStack.getDamageValue() && (!newStack.hasTag() || oldStack.hasTag()) ? (newStack.hasTag() && oldStack.hasTag() ? newStack.getTag().getAllKeys().stream().anyMatch((s) -> {
            return !s.equals("Damage");
        }) && !oldStack.getTag().getAllKeys().stream().anyMatch((s) -> {
            return !s.equals("Damage");
        }) : false) : true;
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
            // Paper start - optimise checkDespawn
            Player entityhuman = this.level.findNearbyPlayer(this, level.paperConfig.hardDespawnDistances.getInt(this.getType().getCategory()) + 1, EntitySelector.affectsSpawning); // Paper
            if (entityhuman == null) {
                entityhuman = ((ServerLevel)this.level).playersAffectingSpawning.isEmpty() ? null : ((ServerLevel)this.level).playersAffectingSpawning.get(0);
            }
            // Paper end - optimise checkDespawn

            if (entityhuman != null) {
                double d0 = entityhuman.distanceToSqr((Entity) this);
                int i = this.level.paperConfig.hardDespawnDistances.getInt(this.getType().getCategory()); // Paper - custom despawn distances
                int j = i * i;

                if (d0 > (double) j) { // CraftBukkit - remove isTypeNotPersistent() check
                    this.discard();
                }

                int k = this.level.paperConfig.softDespawnDistances.getInt(this.getType().getCategory()); // Paper - custom despawn distances
                int l = k * k;

                if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && d0 > (double) l) { // CraftBukkit - remove isTypeNotPersistent() check
                    this.discard();
                } else if (d0 < (double) l) {
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
        if (!this.aware) { // Paper start - Allow nerfed mobs to jump, float and take water damage
            if (goalFloat != null) {
                if (goalFloat.canUse()) goalFloat.tick();
                this.getJumpControl().tick();
            }
            if (this.isSensitiveToWater() && isInWaterRainOrBubble()) {
                hurt(DamageSource.DROWN, 1.0F);
            }
            return;
        }
        // Paper end
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

    protected void customServerAiStep() {}

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
        double d0 = targetEntity.getX() - this.getX();
        double d1 = targetEntity.getZ() - this.getZ();
        double d2;

        if (targetEntity instanceof LivingEntity) {
            LivingEntity entityliving = (LivingEntity) targetEntity;

            d2 = entityliving.getEyeY() - this.getEyeY();
        } else {
            d2 = (targetEntity.getBoundingBox().minY + targetEntity.getBoundingBox().maxY) / 2.0D - this.getEyeY();
        }

        double d3 = Math.sqrt(d0 * d0 + d1 * d1);
        float f2 = (float) (Mth.atan2(d1, d0) * 57.2957763671875D) - 90.0F;
        float f3 = (float) (-(Mth.atan2(d2, d3) * 57.2957763671875D));

        this.setXRot(this.rotlerp(this.getXRot(), f3, maxPitchChange));
        this.setYRot(this.rotlerp(this.getYRot(), f2, maxYawChange));
    }

    private float rotlerp(float from, float to, float max) {
        float f3 = Mth.wrapDegrees(to - from);

        if (f3 > max) {
            f3 = max;
        }

        if (f3 < -max) {
            f3 = -max;
        }

        return from + f3;
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> type, LevelAccessor world, MobSpawnType spawnReason, BlockPos pos, Random random) {
        BlockPos blockposition1 = pos.below();

        return spawnReason == MobSpawnType.SPAWNER || world.getBlockState(blockposition1).isValidSpawn(world, blockposition1, type);
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
            int i = (int) (this.getHealth() - this.getMaxHealth() * 0.33F);

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
        switch (slot.getType()) {
            case HAND:
                return (ItemStack) this.handItems.get(slot.getIndex());
            case ARMOR:
                return (ItemStack) this.armorItems.get(slot.getIndex());
            default:
                return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        this.verifyEquippedItem(stack);
        switch (slot.getType()) {
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
        EquipmentSlot[] aenumitemslot = EquipmentSlot.values();
        int j = aenumitemslot.length;

        for (int k = 0; k < j; ++k) {
            EquipmentSlot enumitemslot = aenumitemslot[k];
            ItemStack itemstack = this.getItemBySlot(enumitemslot);
            float f = this.getEquipmentDropChance(enumitemslot);
            boolean flag1 = f > 1.0F;

            if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack) && (allowDrops || flag1) && Math.max(this.random.nextFloat() - (float) lootingMultiplier * 0.01F, 0.0F) < f) {
                if (!flag1 && itemstack.isDamageableItem()) {
                    itemstack.setDamageValue(itemstack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemstack.getMaxDamage() - 3, 1))));
                }

                this.spawnAtLocation(itemstack);
                if (this.clearEquipmentSlots) { // Paper
                this.setItemSlot(enumitemslot, ItemStack.EMPTY);
                // Paper start
                } else {
                    this.clearedEquipmentSlots.add(enumitemslot);
                }
                // Paper end
            }
        }

    }

    protected float getEquipmentDropChance(EquipmentSlot slot) {
        float f;

        switch (slot.getType()) {
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

            boolean flag = true;
            EquipmentSlot[] aenumitemslot = EquipmentSlot.values();
            int j = aenumitemslot.length;

            for (int k = 0; k < j; ++k) {
                EquipmentSlot enumitemslot = aenumitemslot[k];

                if (enumitemslot.getType() == EquipmentSlot.Type.ARMOR) {
                    ItemStack itemstack = this.getItemBySlot(enumitemslot);

                    if (!flag && this.random.nextFloat() < f) {
                        break;
                    }

                    flag = false;
                    if (itemstack.isEmpty()) {
                        Item item = Mob.getEquipmentForSlot(enumitemslot, i);

                        if (item != null) {
                            this.setItemSlot(enumitemslot, new ItemStack(item));
                        }
                    }
                }
            }
        }

    }

    @Nullable
    public static Item getEquipmentForSlot(EquipmentSlot equipmentSlot, int equipmentLevel) {
        switch (equipmentSlot) {
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
        EquipmentSlot[] aenumitemslot = EquipmentSlot.values();
        int i = aenumitemslot.length;

        for (int j = 0; j < i; ++j) {
            EquipmentSlot enumitemslot = aenumitemslot[j];

            if (enumitemslot.getType() == EquipmentSlot.Type.ARMOR) {
                this.enchantSpawnedArmor(f, enumitemslot);
            }
        }

    }

    protected void enchantSpawnedWeapon(float power) {
        if (!this.getMainHandItem().isEmpty() && this.random.nextFloat() < 0.25F * power) {
            this.setItemSlot(EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(this.random, this.getMainHandItem(), (int) (5.0F + power * (float) this.random.nextInt(18)), false));
        }

    }

    protected void enchantSpawnedArmor(float power, EquipmentSlot slot) {
        ItemStack itemstack = this.getItemBySlot(slot);

        if (!itemstack.isEmpty() && this.random.nextFloat() < 0.5F * power) {
            this.setItemSlot(slot, EnchantmentHelper.enchantItem(this.random, itemstack, (int) (5.0F + power * (float) this.random.nextInt(18)), false));
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
        switch (slot.getType()) {
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
        EquipmentSlot enumitemslot = getEquipmentSlotForItem(stack);

        return this.getItemBySlot(enumitemslot).isEmpty() && this.canPickUpLoot();
    }

    public boolean isPersistenceRequired() {
        return this.persistenceRequired;
    }

    @Override
    public final InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.isAlive()) {
            return InteractionResult.PASS;
        } else if (this.getLeashHolder() == player) {
            // CraftBukkit start - fire PlayerUnleashEntityEvent
            // Paper start - drop leash variable
            org.bukkit.event.player.PlayerUnleashEntityEvent event = CraftEventFactory.callPlayerUnleashEntityEvent(this, player, !player.getAbilities().instabuild);
            if (event.isCancelled()) {
                // Paper end
                ((ServerPlayer) player).connection.send(new ClientboundSetEntityLinkPacket(this, this.getLeashHolder()));
                return InteractionResult.PASS;
            }
            // CraftBukkit end
            this.dropLeash(true, event.isDropLeash()); // Paper - drop leash variable
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            InteractionResult enuminteractionresult = this.checkAndHandleImportantInteractions(player, hand);

            if (enuminteractionresult.consumesAction()) {
                return enuminteractionresult;
            } else {
                enuminteractionresult = this.mobInteract(player, hand);
                return enuminteractionresult.consumesAction() ? enuminteractionresult : super.interact(player, hand);
            }
        }
    }

    private InteractionResult checkAndHandleImportantInteractions(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.is(Items.LEAD) && this.canBeLeashed(player)) {
            // CraftBukkit start - fire PlayerLeashEntityEvent
            if (CraftEventFactory.callPlayerLeashEntityEvent(this, player, player).isCancelled()) {
                ((ServerPlayer) player).connection.send(new ClientboundSetEntityLinkPacket(this, this.getLeashHolder()));
                return InteractionResult.PASS;
            }
            // CraftBukkit end
            this.setLeashedTo(player, true);
            itemstack.shrink(1);
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            if (itemstack.is(Items.NAME_TAG)) {
                InteractionResult enuminteractionresult = itemstack.interactLivingEntity(player, this, hand);

                if (enuminteractionresult.consumesAction()) {
                    return enuminteractionresult;
                }
            }

            if (itemstack.getItem() instanceof SpawnEggItem) {
                if (this.level instanceof ServerLevel) {
                    SpawnEggItem itemmonsteregg = (SpawnEggItem) itemstack.getItem();
                    Optional<Mob> optional = itemmonsteregg.spawnOffspringFromSpawnEgg(player, this, (EntityType<? extends Mob>) this.getType(), (ServerLevel) this.level, this.position(), itemstack); // CraftBukkit - decompile error

                    optional.ifPresent((entityinsentient) -> {
                        this.onOffspringSpawnedFromEgg(player, entityinsentient);
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

    protected void onOffspringSpawnedFromEgg(Player player, Mob child) {}

    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public boolean isWithinRestriction() {
        return this.isWithinRestriction(this.blockPosition());
    }

    public boolean isWithinRestriction(BlockPos pos) {
        return this.restrictRadius == -1.0F ? true : this.restrictCenter.distSqr(pos) < (double) (this.restrictRadius * this.restrictRadius);
    }

    public void restrictTo(BlockPos target, int range) {
        this.restrictCenter = target;
        this.restrictRadius = (float) range;
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

    // CraftBukkit start
    @Nullable
    public <T extends Mob> T convertTo(EntityType<T> entityType, boolean keepEquipment) {
        return this.convertTo(entityType, keepEquipment, EntityTransformEvent.TransformReason.UNKNOWN, CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    @Nullable
    public <T extends Mob> T convertTo(EntityType<T> entitytypes, boolean flag, EntityTransformEvent.TransformReason transformReason, CreatureSpawnEvent.SpawnReason spawnReason) {
        // CraftBukkit end
        if (this.isRemoved()) {
            return null;
        } else {
            T t0 = entitytypes.create(this.level); // CraftBukkit - decompile error

            t0.copyPosition(this);
            t0.setBaby(this.isBaby());
            t0.setNoAi(this.isNoAi());
            if (this.hasCustomName()) {
                t0.setCustomName(this.getCustomName());
                t0.setCustomNameVisible(this.isCustomNameVisible());
            }

            if (this.isPersistenceRequired()) {
                t0.setPersistenceRequired();
            }

            t0.setInvulnerable(this.isInvulnerable());
            if (flag) {
                t0.setCanPickUpLoot(this.canPickUpLoot());
                EquipmentSlot[] aenumitemslot = EquipmentSlot.values();
                int i = aenumitemslot.length;

                for (int j = 0; j < i; ++j) {
                    EquipmentSlot enumitemslot = aenumitemslot[j];
                    ItemStack itemstack = this.getItemBySlot(enumitemslot);

                    if (!itemstack.isEmpty()) {
                        t0.setItemSlot(enumitemslot, itemstack.copy());
                        t0.setDropChance(enumitemslot, this.getEquipmentDropChance(enumitemslot));
                        itemstack.setCount(0);
                    }
                }
            }

            // CraftBukkit start
            if (CraftEventFactory.callEntityTransformEvent(this, t0, transformReason).isCancelled()) {
                return null;
            }
            this.level.addFreshEntity(t0, spawnReason);
            // CraftBukkit end
            if (this.isPassenger()) {
                Entity entity = this.getVehicle();

                this.stopRiding();
                t0.startRiding(entity, true);
            }

            this.discard();
            return t0;
        }
    }

    protected void tickLeash() {
        if (this.leashInfoTag != null) {
            this.restoreLeashFromSave();
        }

        if (this.leashHolder != null) {
            if (!this.isAlive() || !this.leashHolder.isAlive()) {
                // Paper start - drop leash variable
                EntityUnleashEvent event = new EntityUnleashEvent(this.getBukkitEntity(), (!this.isAlive()) ? UnleashReason.PLAYER_UNLEASH : UnleashReason.HOLDER_GONE, true);
                this.level.getCraftServer().getPluginManager().callEvent(event); // CraftBukkit
                this.dropLeash(true, event.isDropLeash());
                // Paper end
            }

        }
    }

    public void dropLeash(boolean sendPacket, boolean dropItem) {
        if (this.leashHolder != null) {
            this.leashHolder = null;
            this.leashInfoTag = null;
            if (!this.level.isClientSide && dropItem) {
                this.forceDrops = true; // CraftBukkit
                this.spawnAtLocation((ItemLike) Items.LEAD);
                this.forceDrops = false; // CraftBukkit
            }

            if (!this.level.isClientSide && sendPacket && this.level instanceof ServerLevel) {
                ((ServerLevel) this.level).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, (Entity) null));
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
            ((ServerLevel) this.level).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, this.leashHolder));
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
        boolean flag1 = super.startRiding(entity, force);

        if (flag1 && this.isLeashed()) {
            // Paper start - drop leash variable
            EntityUnleashEvent event = new EntityUnleashEvent(this.getBukkitEntity(), UnleashReason.UNKNOWN, true);
            if (!event.callEvent()) { return flag1; }
            this.dropLeash(true, event.isDropLeash());
            // Paper end
        }

        return flag1;
    }

    private void restoreLeashFromSave() {
        if (this.leashInfoTag != null && this.level instanceof ServerLevel) {
            if (this.leashInfoTag.hasUUID("UUID")) {
                UUID uuid = this.leashInfoTag.getUUID("UUID");
                Entity entity = ((ServerLevel) this.level).getEntity(uuid);

                if (entity != null) {
                    this.setLeashedTo(entity, true);
                    return;
                }
            } else if (this.leashInfoTag.contains("X", 99) && this.leashInfoTag.contains("Y", 99) && this.leashInfoTag.contains("Z", 99)) {
                BlockPos blockposition = NbtUtils.readBlockPos(this.leashInfoTag);

                this.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(this.level, blockposition), true);
                return;
            }

            if (this.tickCount > 100) {
                this.forceDrops = true; // CraftBukkit
                this.spawnAtLocation((ItemLike) Items.LEAD);
                this.forceDrops = false; // CraftBukkit
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
        byte b0 = (Byte) this.entityData.get(Mob.DATA_MOB_FLAGS_ID);

        this.entityData.set(Mob.DATA_MOB_FLAGS_ID, aiDisabled ? (byte) (b0 | 1) : (byte) (b0 & -2));
    }

    public void setLeftHanded(boolean leftHanded) {
        byte b0 = (Byte) this.entityData.get(Mob.DATA_MOB_FLAGS_ID);

        this.entityData.set(Mob.DATA_MOB_FLAGS_ID, leftHanded ? (byte) (b0 | 2) : (byte) (b0 & -3));
    }

    public void setAggressive(boolean attacking) {
        byte b0 = (Byte) this.entityData.get(Mob.DATA_MOB_FLAGS_ID);

        this.entityData.set(Mob.DATA_MOB_FLAGS_ID, attacking ? (byte) (b0 | 4) : (byte) (b0 & -5));
    }

    public boolean isNoAi() {
        return ((Byte) this.entityData.get(Mob.DATA_MOB_FLAGS_ID) & 1) != 0;
    }

    public boolean isLeftHanded() {
        return ((Byte) this.entityData.get(Mob.DATA_MOB_FLAGS_ID) & 2) != 0;
    }

    public boolean isAggressive() {
        return ((Byte) this.entityData.get(Mob.DATA_MOB_FLAGS_ID) & 4) != 0;
    }

    public void setBaby(boolean baby) {}

    @Override
    public HumanoidArm getMainArm() {
        return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public double getMeleeAttackRangeSqr(LivingEntity target) {
        return (double) (this.getBbWidth() * 2.0F * this.getBbWidth() * 2.0F + target.getBbWidth());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float f1 = (float) this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);

        if (target instanceof LivingEntity) {
            f += EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity) target).getMobType());
            f1 += (float) EnchantmentHelper.getKnockbackBonus(this);
        }

        int i = EnchantmentHelper.getFireAspect(this);

        if (i > 0) {
            // CraftBukkit start - Call a combust event when somebody hits with a fire enchanted item
            EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), target.getBukkitEntity(), i * 4);
            org.bukkit.Bukkit.getPluginManager().callEvent(combustEvent);

            if (!combustEvent.isCancelled()) {
                target.setSecondsOnFire(combustEvent.getDuration(), false);
            }
            // CraftBukkit end
        }

        boolean flag = target.hurt(DamageSource.mobAttack(this), f);

        if (flag) {
            if (f1 > 0.0F && target instanceof LivingEntity) {
                ((LivingEntity) target).knockback((double) (f1 * 0.5F), (double) Mth.sin(this.getYRot() * 0.017453292F), (double) (-Mth.cos(this.getYRot() * 0.017453292F)), this); // Paper
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }

            if (target instanceof Player) {
                Player entityhuman = (Player) target;

                this.maybeDisableShield(entityhuman, this.getMainHandItem(), entityhuman.isUsingItem() ? entityhuman.getUseItem() : ItemStack.EMPTY);
            }

            this.doEnchantDamageEffects(this, target);
            this.setLastHurtMob(target);
        }

        return flag;
    }

    private void maybeDisableShield(Player player, ItemStack mobStack, ItemStack playerStack) {
        if (!mobStack.isEmpty() && !playerStack.isEmpty() && mobStack.getItem() instanceof AxeItem && playerStack.is(Items.SHIELD)) {
            float f = 0.25F + (float) EnchantmentHelper.getBlockEfficiency(this) * 0.05F;

            if (this.random.nextFloat() < f) {
                player.getCooldowns().addCooldown(Items.SHIELD, 100);
                this.level.broadcastEntityEvent(player, (byte) 30);
            }
        }

    }

    public boolean isSunBurnTick() {
        if (this.level.isDay() && !this.level.isClientSide) {
            float f = this.getBrightness();
            BlockPos blockposition = new BlockPos(this.getX(), this.getEyeY(), this.getZ());
            boolean flag = this.isInWaterRainOrBubble() || this.isInPowderSnow || this.wasInPowderSnow;

            if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !flag && this.level.canSeeSky(blockposition)) {
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
        // Paper start - drop leash variable
        EntityUnleashEvent event = new EntityUnleashEvent(this.getBukkitEntity(), UnleashReason.UNKNOWN, false);
        this.level.getCraftServer().getPluginManager().callEvent(event); // CraftBukkit
        this.dropLeash(true, event.isDropLeash());
        // Paper end
        this.getAllSlots().forEach((itemstack) -> {
            if (!itemstack.isEmpty()) itemstack.setCount(0); // CraftBukkit
        });
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        SpawnEggItem itemmonsteregg = SpawnEggItem.byId(this.getType());

        return itemmonsteregg == null ? null : new ItemStack(itemmonsteregg);
    }
}
