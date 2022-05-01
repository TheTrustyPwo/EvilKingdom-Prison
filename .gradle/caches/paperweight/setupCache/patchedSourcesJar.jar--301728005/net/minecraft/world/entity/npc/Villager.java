package net.minecraft.world.entity.npc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
// CraftBukkit end

public class Villager extends AbstractVillager implements ReputationEventHandler, VillagerDataHolder {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<VillagerData> DATA_VILLAGER_DATA = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.VILLAGER_DATA);
    public static final int BREEDING_FOOD_THRESHOLD = 12;
    public static final Map<Item, Integer> FOOD_POINTS = ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
    private static final int TRADES_PER_LEVEL = 2;
    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(Items.BREAD, Items.POTATO, Items.CARROT, Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT, new Item[]{Items.BEETROOT_SEEDS});
    private static final int MAX_GOSSIP_TOPICS = 10;
    private static final int GOSSIP_COOLDOWN = 1200;
    private static final int GOSSIP_DECAY_INTERVAL = 24000;
    private static final int REPUTATION_CHANGE_PER_EVENT = 25;
    private static final int HOW_FAR_AWAY_TO_TALK_TO_OTHER_VILLAGERS_ABOUT_GOLEMS = 10;
    private static final int HOW_MANY_VILLAGERS_NEED_TO_AGREE_TO_SPAWN_A_GOLEM = 5;
    private static final long TIME_SINCE_SLEEPING_FOR_GOLEM_SPAWNING = 24000L;
    @VisibleForTesting
    public static final float SPEED_MODIFIER = 0.5F;
    private int updateMerchantTimer;
    private boolean increaseProfessionLevelOnUpdate;
    @Nullable
    private Player lastTradedPlayer;
    private boolean chasing;
    private int foodLevel;
    private final GossipContainer gossips;
    private long lastGossipTime;
    private long lastGossipDecayTime;
    private int villagerXp;
    private long lastRestockGameTime;
    public int numberOfRestocksToday;
    private long lastRestockCheckDayTime;
    private boolean assignProfessionWhenSpawned;
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.WALK_TARGET, new MemoryModuleType[]{MemoryModuleType.LOOK_TARGET, MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.BREED_TARGET, MemoryModuleType.PATH, MemoryModuleType.DOORS_TO_CLOSE, MemoryModuleType.NEAREST_BED, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_HOSTILE, MemoryModuleType.SECONDARY_JOB_SITE, MemoryModuleType.HIDING_PLACE, MemoryModuleType.HEARD_BELL_TIME, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.LAST_SLEPT, MemoryModuleType.LAST_WOKEN, MemoryModuleType.LAST_WORKED_AT_POI, MemoryModuleType.GOLEM_DETECTED_RECENTLY});
    private static final ImmutableList<SensorType<? extends Sensor<? super Villager>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_BED, SensorType.HURT_BY, SensorType.VILLAGER_HOSTILES, SensorType.VILLAGER_BABIES, SensorType.SECONDARY_POIS, SensorType.GOLEM_DETECTED);
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<Villager, PoiType>> POI_MEMORIES = ImmutableMap.of(MemoryModuleType.HOME, (entityvillager, villageplacetype) -> {
        return villageplacetype == PoiType.HOME;
    }, MemoryModuleType.JOB_SITE, (entityvillager, villageplacetype) -> {
        return entityvillager.getVillagerData().getProfession().getJobPoiType() == villageplacetype;
    }, MemoryModuleType.POTENTIAL_JOB_SITE, (entityvillager, villageplacetype) -> {
        return PoiType.ALL_JOBS.test(villageplacetype);
    }, MemoryModuleType.MEETING_POINT, (entityvillager, villageplacetype) -> {
        return villageplacetype == PoiType.MEETING;
    });

    public Villager(EntityType<? extends Villager> entityType, Level world) {
        this(entityType, world, VillagerType.PLAINS);
    }

    public Villager(EntityType<? extends Villager> entityType, Level world, VillagerType type) {
        super(entityType, world);
        this.gossips = new GossipContainer();
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.setCanPickUpLoot(true);
        this.setVillagerData(this.getVillagerData().setType(type).setProfession(VillagerProfession.NONE));
    }

    @Override
    public Brain<Villager> getBrain() {
        return (Brain<Villager>) super.getBrain(); // CraftBukkit - decompile error
    }

    @Override
    protected Brain.Provider<Villager> brainProvider() {
        return Brain.provider(Villager.MEMORY_TYPES, Villager.SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<Villager> behaviorcontroller = this.brainProvider().makeBrain(dynamic);

        this.registerBrainGoals(behaviorcontroller);
        return behaviorcontroller;
    }

    public void refreshBrain(ServerLevel world) {
        Brain<Villager> behaviorcontroller = this.getBrain();

        behaviorcontroller.stopAll(world, this);
        this.brain = behaviorcontroller.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    private void registerBrainGoals(Brain<Villager> brain) {
        VillagerProfession villagerprofession = this.getVillagerData().getProfession();

        if (this.isBaby()) {
            brain.setSchedule(Schedule.VILLAGER_BABY);
            brain.addActivity(Activity.PLAY, VillagerGoalPackages.getPlayPackage(0.5F));
        } else {
            brain.setSchedule(Schedule.VILLAGER_DEFAULT);
            brain.addActivityWithConditions(Activity.WORK, VillagerGoalPackages.getWorkPackage(villagerprofession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        }

        brain.addActivity(Activity.CORE, VillagerGoalPackages.getCorePackage(villagerprofession, 0.5F));
        brain.addActivityWithConditions(Activity.MEET, VillagerGoalPackages.getMeetPackage(villagerprofession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        brain.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(villagerprofession, 0.5F));
        brain.addActivity(Activity.IDLE, VillagerGoalPackages.getIdlePackage(villagerprofession, 0.5F));
        brain.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(villagerprofession, 0.5F));
        brain.addActivity(Activity.PRE_RAID, VillagerGoalPackages.getPreRaidPackage(villagerprofession, 0.5F));
        brain.addActivity(Activity.RAID, VillagerGoalPackages.getRaidPackage(villagerprofession, 0.5F));
        brain.addActivity(Activity.HIDE, VillagerGoalPackages.getHidePackage(villagerprofession, 0.5F));
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(this.level.getDayTime(), this.level.getGameTime());
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (this.level instanceof ServerLevel) {
            this.refreshBrain((ServerLevel) this.level);
        }

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5D).add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    public boolean assignProfessionWhenSpawned() {
        return this.assignProfessionWhenSpawned;
    }

    // Spigot Start
    @Override
    public void inactiveTick() {
        // SPIGOT-3874, SPIGOT-3894, SPIGOT-3846, SPIGOT-5286 :(
        // Paper start
        if (this.getUnhappyCounter() > 0) {
            this.setUnhappyCounter(this.getUnhappyCounter() - 1);
        }
        if (this.isEffectiveAi()) {
            if (level.spigotConfig.tickInactiveVillagers) {
                this.customServerAiStep();
            } else {
                this.mobTick(true);
            }
        }
        maybeDecayGossip();
        // Paper end

        super.inactiveTick();
    }
    // Spigot End

    @Override
    protected void customServerAiStep() { mobTick(false); }
    protected void mobTick(boolean inactive) {
        this.level.getProfiler().push("villagerBrain");
        if (!inactive) this.getBrain().tick((ServerLevel) this.level, this); // Paper
        this.level.getProfiler().pop();
        if (this.assignProfessionWhenSpawned) {
            this.assignProfessionWhenSpawned = false;
        }

        if (!this.isTrading() && this.updateMerchantTimer > 0) {
            --this.updateMerchantTimer;
            if (this.updateMerchantTimer <= 0) {
                if (this.increaseProfessionLevelOnUpdate) {
                    this.increaseMerchantCareer();
                    this.increaseProfessionLevelOnUpdate = false;
                }

                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.VILLAGER_TRADE); // CraftBukkit
            }
        }

        if (this.lastTradedPlayer != null && this.level instanceof ServerLevel) {
            ((ServerLevel) this.level).onReputationEvent(ReputationEventType.TRADE, this.lastTradedPlayer, this);
            this.level.broadcastEntityEvent(this, (byte) 14);
            this.lastTradedPlayer = null;
        }

        if (!inactive && !this.isNoAi() && this.random.nextInt(100) == 0) { // Paper
            Raid raid = ((ServerLevel) this.level).getRaidAt(this.blockPosition());

            if (raid != null && raid.isActive() && !raid.isOver()) {
                this.level.broadcastEntityEvent(this, (byte) 42);
            }
        }

        if (this.getVillagerData().getProfession() == VillagerProfession.NONE && this.isTrading()) {
            this.stopTrading();
        }
        if (inactive) return; // Paper

        super.customServerAiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getUnhappyCounter() > 0) {
            this.setUnhappyCounter(this.getUnhappyCounter() - 1);
        }

        this.maybeDecayGossip();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (!itemstack.is(Items.VILLAGER_SPAWN_EGG) && this.isAlive() && !this.isTrading() && !this.isSleeping()) {
            if (this.isBaby()) {
                this.setUnhappy();
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            } else {
                boolean flag = this.getOffers().isEmpty();

                if (hand == InteractionHand.MAIN_HAND) {
                    if (flag && !this.level.isClientSide) {
                        this.setUnhappy();
                    }

                    player.awardStat(Stats.TALKED_TO_VILLAGER);
                }

                if (flag) {
                    return InteractionResult.sidedSuccess(this.level.isClientSide);
                } else {
                    if (!this.level.isClientSide && !this.offers.isEmpty()) {
                        this.startTrading(player);
                    }

                    return InteractionResult.sidedSuccess(this.level.isClientSide);
                }
            }
        } else {
            return super.mobInteract(player, hand);
        }
    }

    public void setUnhappy() {
        this.setUnhappyCounter(40);
        if (!this.level.isClientSide()) {
            this.playSound(SoundEvents.VILLAGER_NO, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    private void startTrading(Player customer) {
        this.updateSpecialPrices(customer);
        this.setTradingPlayer(customer);
        this.openTradingScreen(customer, this.getDisplayName(), this.getVillagerData().getLevel());
    }

    @Override
    public void setTradingPlayer(@Nullable Player customer) {
        boolean flag = this.getTradingPlayer() != null && customer == null;

        super.setTradingPlayer(customer);
        if (flag) {
            this.stopTrading();
        }

    }

    @Override
    protected void stopTrading() {
        super.stopTrading();
        this.resetSpecialPrices();
    }

    private void resetSpecialPrices() {
        Iterator iterator = this.getOffers().iterator();

        while (iterator.hasNext()) {
            MerchantOffer merchantrecipe = (MerchantOffer) iterator.next();

            merchantrecipe.resetSpecialPriceDiff();
        }

    }

    @Override
    public boolean canRestock() {
        return true;
    }

    @Override
    public boolean isClientSide() {
        return this.getLevel().isClientSide;
    }

    public void restock() {
        this.updateDemand();
        Iterator iterator = this.getOffers().iterator();

        while (iterator.hasNext()) {
            MerchantOffer merchantrecipe = (MerchantOffer) iterator.next();

            // CraftBukkit start
            VillagerReplenishTradeEvent event = new VillagerReplenishTradeEvent((org.bukkit.entity.Villager) this.getBukkitEntity(), merchantrecipe.asBukkit());
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                merchantrecipe.resetUses();
            }
            // CraftBukkit end
        }

        this.lastRestockGameTime = this.level.getGameTime();
        ++this.numberOfRestocksToday;
    }

    private boolean needsToRestock() {
        Iterator iterator = this.getOffers().iterator();

        MerchantOffer merchantrecipe;

        do {
            if (!iterator.hasNext()) {
                return false;
            }

            merchantrecipe = (MerchantOffer) iterator.next();
        } while (!merchantrecipe.needsRestock());

        return true;
    }

    private boolean allowedToRestock() {
        return this.numberOfRestocksToday == 0 || this.numberOfRestocksToday < 2 && this.level.getGameTime() > this.lastRestockGameTime + 2400L;
    }

    public boolean shouldRestock() {
        long i = this.lastRestockGameTime + 12000L;
        long j = this.level.getGameTime();
        boolean flag = j > i;
        long k = this.level.getDayTime();

        if (this.lastRestockCheckDayTime > 0L) {
            long l = this.lastRestockCheckDayTime / 24000L;
            long i1 = k / 24000L;

            flag |= i1 > l;
        }

        this.lastRestockCheckDayTime = k;
        if (flag) {
            this.lastRestockGameTime = j;
            this.resetNumberOfRestocks();
        }

        return this.allowedToRestock() && this.needsToRestock();
    }

    private void catchUpDemand() {
        int i = 2 - this.numberOfRestocksToday;

        if (i > 0) {
            Iterator iterator = this.getOffers().iterator();

            while (iterator.hasNext()) {
                MerchantOffer merchantrecipe = (MerchantOffer) iterator.next();

                // CraftBukkit start
                VillagerReplenishTradeEvent event = new VillagerReplenishTradeEvent((org.bukkit.entity.Villager) this.getBukkitEntity(), merchantrecipe.asBukkit());
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    merchantrecipe.resetUses();
                }
                // CraftBukkit end
            }
        }

        for (int j = 0; j < i; ++j) {
            this.updateDemand();
        }

    }

    private void updateDemand() {
        Iterator iterator = this.getOffers().iterator();

        while (iterator.hasNext()) {
            MerchantOffer merchantrecipe = (MerchantOffer) iterator.next();

            merchantrecipe.updateDemand();
        }

    }

    private void updateSpecialPrices(Player player) {
        int i = this.getPlayerReputation(player);

        if (i != 0) {
            Iterator iterator = this.getOffers().iterator();

            while (iterator.hasNext()) {
                MerchantOffer merchantrecipe = (MerchantOffer) iterator.next();
                if (merchantrecipe.ignoreDiscounts) continue; // Paper

                merchantrecipe.addToSpecialPriceDiff(-Mth.floor((float) i * merchantrecipe.getPriceMultiplier()));
            }
        }

        if (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            MobEffectInstance mobeffect = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
            int j = mobeffect.getAmplifier();
            Iterator iterator1 = this.getOffers().iterator();

            while (iterator1.hasNext()) {
                MerchantOffer merchantrecipe1 = (MerchantOffer) iterator1.next();
                if (merchantrecipe1.ignoreDiscounts) continue; // Paper
                double d0 = 0.3D + 0.0625D * (double) j;
                int k = (int) Math.floor(d0 * (double) merchantrecipe1.getBaseCostA().getCount());

                merchantrecipe1.addToSpecialPriceDiff(-Math.max(k, 1));
            }
        }

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(Villager.DATA_VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        DataResult<Tag> dataresult = VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, this.getVillagerData()); // CraftBukkit - decompile error
        Logger logger = Villager.LOGGER;

        Objects.requireNonNull(logger);
        dataresult.resultOrPartial(logger::error).ifPresent((nbtbase) -> {
            nbt.put("VillagerData", nbtbase);
        });
        nbt.putByte("FoodLevel", (byte) this.foodLevel);
        nbt.put("Gossips", (Tag) this.gossips.store(NbtOps.INSTANCE).getValue());
        nbt.putInt("Xp", this.villagerXp);
        nbt.putLong("LastRestock", this.lastRestockGameTime);
        nbt.putLong("LastGossipDecay", this.lastGossipDecayTime);
        nbt.putInt("RestocksToday", this.numberOfRestocksToday);
        if (this.assignProfessionWhenSpawned) {
            nbt.putBoolean("AssignProfessionWhenSpawned", true);
        }

    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("VillagerData", 10)) {
            DataResult<VillagerData> dataresult = VillagerData.CODEC.parse(new Dynamic(NbtOps.INSTANCE, nbt.get("VillagerData")));
            Logger logger = Villager.LOGGER;

            Objects.requireNonNull(logger);
            dataresult.resultOrPartial(logger::error).ifPresent(this::setVillagerData);
        }

        if (nbt.contains("Offers", 10)) {
            this.offers = new MerchantOffers(nbt.getCompound("Offers"));
        }

        if (nbt.contains("FoodLevel", 1)) {
            this.foodLevel = nbt.getByte("FoodLevel");
        }

        ListTag nbttaglist = nbt.getList("Gossips", 10);

        this.gossips.update(new Dynamic(NbtOps.INSTANCE, nbttaglist));
        if (nbt.contains("Xp", 3)) {
            this.villagerXp = nbt.getInt("Xp");
        }

        this.lastRestockGameTime = nbt.getLong("LastRestock");
        this.lastGossipDecayTime = nbt.getLong("LastGossipDecay");
        this.setCanPickUpLoot(true);
        if (this.level instanceof ServerLevel) {
            this.refreshBrain((ServerLevel) this.level);
        }

        this.numberOfRestocksToday = nbt.getInt("RestocksToday");
        if (nbt.contains("AssignProfessionWhenSpawned")) {
            this.assignProfessionWhenSpawned = nbt.getBoolean("AssignProfessionWhenSpawned");
        }

    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isSleeping() ? null : (this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    public void playWorkSound() {
        SoundEvent soundeffect = this.getVillagerData().getProfession().getWorkSound();

        if (soundeffect != null) {
            this.playSound(soundeffect, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    @Override
    public void setVillagerData(VillagerData villagerData) {
        VillagerData villagerdata1 = this.getVillagerData();

        if (villagerdata1.getProfession() != villagerData.getProfession()) {
            this.offers = null;
        }

        this.entityData.set(Villager.DATA_VILLAGER_DATA, villagerData);
    }

    @Override
    public VillagerData getVillagerData() {
        return (VillagerData) this.entityData.get(Villager.DATA_VILLAGER_DATA);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        int i = 3 + this.random.nextInt(4);

        this.villagerXp += offer.getXp();
        this.lastTradedPlayer = this.getTradingPlayer();
        if (this.shouldIncreaseLevel()) {
            this.updateMerchantTimer = 40;
            this.increaseProfessionLevelOnUpdate = true;
            i += 5;
        }

        if (offer.shouldRewardExp()) {
            this.level.addFreshEntity(new ExperienceOrb(this.level, this.getX(), this.getY() + 0.5D, this.getZ(), i, org.bukkit.entity.ExperienceOrb.SpawnReason.VILLAGER_TRADE, this.getTradingPlayer(), this)); // Paper
        }

    }

    public void setChasing(boolean flag) {
        this.chasing = flag;
    }

    public boolean isChasing() {
        return this.chasing;
    }

    @Override
    public void setLastHurtByMob(@Nullable LivingEntity attacker) {
        if (attacker != null && this.level instanceof ServerLevel) {
            ((ServerLevel) this.level).onReputationEvent(ReputationEventType.VILLAGER_HURT, attacker, this);
            if (this.isAlive() && attacker instanceof Player) {
                this.level.broadcastEntityEvent(this, (byte) 13);
            }
        }

        super.setLastHurtByMob(attacker);
    }

    @Override
    public void die(DamageSource source) {
        if (org.spigotmc.SpigotConfig.logVillagerDeaths) Villager.LOGGER.info("Villager {} died, message: '{}'", this, source.getLocalizedDeathMessage(this).getString()); // Spigot
        Entity entity = source.getEntity();

        if (entity != null) {
            this.tellWitnessesThatIWasMurdered(entity);
        }

        this.releaseAllPois();
        super.die(source);
    }

    public void releaseAllPois() {
        this.releasePoi(MemoryModuleType.HOME);
        this.releasePoi(MemoryModuleType.JOB_SITE);
        this.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
        this.releasePoi(MemoryModuleType.MEETING_POINT);
    }

    private void tellWitnessesThatIWasMurdered(Entity killer) {
        Level world = this.level;

        if (world instanceof ServerLevel) {
            ServerLevel worldserver = (ServerLevel) world;
            Optional optional = this.brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);

            if (!optional.isEmpty()) {
                NearestVisibleLivingEntities nearestvisiblelivingentities = (NearestVisibleLivingEntities) optional.get();

                Objects.requireNonNull(ReputationEventHandler.class);
                nearestvisiblelivingentities.findAll(ReputationEventHandler.class::isInstance).forEach((entityliving) -> {
                    worldserver.onReputationEvent(ReputationEventType.VILLAGER_KILLED, killer, (ReputationEventHandler) entityliving);
                });
            }
        }
    }

    public void releasePoi(MemoryModuleType<GlobalPos> memorymoduletype) {
        if (this.level instanceof ServerLevel) {
            MinecraftServer minecraftserver = ((ServerLevel) this.level).getServer();

            this.brain.getMemory(memorymoduletype).ifPresent((globalpos) -> {
                ServerLevel worldserver = minecraftserver.getLevel(globalpos.dimension());

                if (worldserver != null) {
                    PoiManager villageplace = worldserver.getPoiManager();
                    Optional<PoiType> optional = villageplace.getType(globalpos.pos());
                    BiPredicate<Villager, PoiType> bipredicate = (BiPredicate) Villager.POI_MEMORIES.get(memorymoduletype);

                    if (optional.isPresent() && bipredicate.test(this, (PoiType) optional.get())) {
                        villageplace.release(globalpos.pos());
                        DebugPackets.sendPoiTicketCountPacket(worldserver, globalpos.pos());
                    }

                }
            });
        }
    }

    @Override
    public boolean canBreed() {
        return this.foodLevel + this.countFoodPointsInInventory() >= 12 && this.getAge() == 0;
    }

    private boolean hungry() {
        return this.foodLevel < 12;
    }

    private void eatUntilFull() {
        if (this.hungry() && this.countFoodPointsInInventory() != 0) {
            for (int i = 0; i < this.getInventory().getContainerSize(); ++i) {
                ItemStack itemstack = this.getInventory().getItem(i);

                if (!itemstack.isEmpty()) {
                    Integer integer = (Integer) Villager.FOOD_POINTS.get(itemstack.getItem());

                    if (integer != null) {
                        int j = itemstack.getCount();

                        for (int k = j; k > 0; --k) {
                            this.foodLevel += integer;
                            this.getInventory().removeItem(i, 1);
                            if (!this.hungry()) {
                                return;
                            }
                        }
                    }
                }
            }

        }
    }

    public int getPlayerReputation(Player player) {
        return this.gossips.getReputation(player.getUUID(), (reputationtype) -> {
            return true;
        });
    }

    private void digestFood(int amount) {
        this.foodLevel -= amount;
    }

    public void eatAndDigestFood() {
        this.eatUntilFull();
        this.digestFood(12);
    }

    public void setOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    private boolean shouldIncreaseLevel() {
        int i = this.getVillagerData().getLevel();

        return VillagerData.canLevelUp(i) && this.villagerXp >= VillagerData.getMaxXpPerLevel(i);
    }

    public void increaseMerchantCareer() {
        this.setVillagerData(this.getVillagerData().setLevel(this.getVillagerData().getLevel() + 1));
        this.updateTrades();
    }

    @Override
    protected Component getTypeName() {
        String s = this.getType().getDescriptionId();

        return new TranslatableComponent(s + "." + Registry.VILLAGER_PROFESSION.getKey(this.getVillagerData().getProfession()).getPath());
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 12) {
            this.addParticlesAroundSelf(ParticleTypes.HEART);
        } else if (status == 13) {
            this.addParticlesAroundSelf(ParticleTypes.ANGRY_VILLAGER);
        } else if (status == 14) {
            this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
        } else if (status == 42) {
            this.addParticlesAroundSelf(ParticleTypes.SPLASH);
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        if (spawnReason == MobSpawnType.BREEDING) {
            this.setVillagerData(this.getVillagerData().setProfession(VillagerProfession.NONE));
        }

        if (spawnReason == MobSpawnType.COMMAND || spawnReason == MobSpawnType.SPAWN_EGG || spawnReason == MobSpawnType.SPAWNER || spawnReason == MobSpawnType.DISPENSER) {
            this.setVillagerData(this.getVillagerData().setType(VillagerType.byBiome(world.getBiome(this.blockPosition()))));
        }

        if (spawnReason == MobSpawnType.STRUCTURE) {
            this.assignProfessionWhenSpawned = true;
        }

        return super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public Villager getBreedOffspring(ServerLevel world, AgeableMob entity) {
        double d0 = this.random.nextDouble();
        VillagerType villagertype;

        if (d0 < 0.5D) {
            villagertype = VillagerType.byBiome(world.getBiome(this.blockPosition()));
        } else if (d0 < 0.75D) {
            villagertype = this.getVillagerData().getType();
        } else {
            villagertype = ((Villager) entity).getVillagerData().getType();
        }

        Villager entityvillager = new Villager(EntityType.VILLAGER, world, villagertype);

        entityvillager.finalizeSpawn(world, world.getCurrentDifficultyAt(entityvillager.blockPosition()), MobSpawnType.BREEDING, (SpawnGroupData) null, (CompoundTag) null);
        return entityvillager;
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) {
            // Paper - move log down, event can cancel
            Witch entitywitch = (Witch) EntityType.WITCH.create(world);

            // Paper start
            if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityZapEvent(this, lightning, entitywitch).isCancelled()) {
                return;
            }
            // Paper end

            if (org.spigotmc.SpigotConfig.logVillagerDeaths) Villager.LOGGER.info("Villager {} was struck by lightning {}.", this, lightning); // Paper - move log down, event can cancel

            entitywitch.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            entitywitch.finalizeSpawn(world, world.getCurrentDifficultyAt(entitywitch.blockPosition()), MobSpawnType.CONVERSION, (SpawnGroupData) null, (CompoundTag) null);
            entitywitch.setNoAi(this.isNoAi());
            if (this.hasCustomName()) {
                entitywitch.setCustomName(this.getCustomName());
                entitywitch.setCustomNameVisible(this.isCustomNameVisible());
            }

            entitywitch.setPersistenceRequired();
            // CraftBukkit start
            if (CraftEventFactory.callEntityTransformEvent(this, entitywitch, EntityTransformEvent.TransformReason.LIGHTNING).isCancelled()) {
                return;
            }
            world.addFreshEntityWithPassengers(entitywitch, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.LIGHTNING);
            // CraftBukkit end
            this.releaseAllPois();
            this.discard();
        } else {
            super.thunderHit(world, lightning);
        }

    }

    @Override
    protected void pickUpItem(ItemEntity item) {
        ItemStack itemstack = item.getItem();

        if (this.wantsToPickUp(itemstack)) {
            SimpleContainer inventorysubcontainer = this.getInventory();
            boolean flag = inventorysubcontainer.canAddItem(itemstack);

            if (!flag) {
                return;
            }

            // CraftBukkit start
            ItemStack remaining = new SimpleContainer(inventorysubcontainer).addItem(itemstack);
            if (CraftEventFactory.callEntityPickupItemEvent(this, item, remaining.getCount(), false).isCancelled()) {
                return;
            }
            // CraftBukkit end

            this.onItemPickup(item);
            this.take(item, itemstack.getCount());
            ItemStack itemstack1 = inventorysubcontainer.addItem(itemstack);

            if (itemstack1.isEmpty()) {
                item.discard();
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }

    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        Item item = stack.getItem();

        return (Villager.WANTED_ITEMS.contains(item) || this.getVillagerData().getProfession().getRequestedItems().contains(item)) && this.getInventory().canAddItem(stack);
    }

    public boolean hasExcessFood() {
        return this.countFoodPointsInInventory() >= 24;
    }

    public boolean wantsMoreFood() {
        return this.countFoodPointsInInventory() < 12;
    }

    private int countFoodPointsInInventory() {
        SimpleContainer inventorysubcontainer = this.getInventory();

        return Villager.FOOD_POINTS.entrySet().stream().mapToInt((entry) -> {
            return inventorysubcontainer.countItem((Item) entry.getKey()) * (Integer) entry.getValue();
        }).sum();
    }

    public boolean hasFarmSeeds() {
        return this.getInventory().hasAnyOf(ImmutableSet.of(Items.WHEAT_SEEDS, Items.POTATO, Items.CARROT, Items.BEETROOT_SEEDS));
    }

    @Override
    protected void updateTrades() {
        VillagerData villagerdata = this.getVillagerData();
        Int2ObjectMap<VillagerTrades.ItemListing[]> int2objectmap = (Int2ObjectMap) VillagerTrades.TRADES.get(villagerdata.getProfession());

        if (int2objectmap != null && !int2objectmap.isEmpty()) {
            VillagerTrades.ItemListing[] avillagertrades_imerchantrecipeoption = (VillagerTrades.ItemListing[]) int2objectmap.get(villagerdata.getLevel());

            if (avillagertrades_imerchantrecipeoption != null) {
                MerchantOffers merchantrecipelist = this.getOffers();

                this.addOffersFromItemListings(merchantrecipelist, avillagertrades_imerchantrecipeoption, 2);
            }
        }
    }

    public void gossip(ServerLevel world, Villager villager, long time) {
        if ((time < this.lastGossipTime || time >= this.lastGossipTime + 1200L) && (time < villager.lastGossipTime || time >= villager.lastGossipTime + 1200L)) {
            this.gossips.transferFrom(villager.gossips, this.random, 10);
            this.lastGossipTime = time;
            villager.lastGossipTime = time;
            this.spawnGolemIfNeeded(world, time, 5);
        }
    }

    private void maybeDecayGossip() {
        long i = this.level.getGameTime();

        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = i;
        } else if (i >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = i;
        }
    }

    public void spawnGolemIfNeeded(ServerLevel world, long time, int requiredCount) {
        if (this.wantsToSpawnGolem(time)) {
            AABB axisalignedbb = this.getBoundingBox().inflate(10.0D, 10.0D, 10.0D);
            List<Villager> list = world.getEntitiesOfClass(Villager.class, axisalignedbb);
            List<Villager> list1 = (List) list.stream().filter((entityvillager) -> {
                return entityvillager.wantsToSpawnGolem(time);
            }).limit(5L).collect(Collectors.toList());

            if (list1.size() >= requiredCount) {
                IronGolem entityirongolem = this.trySpawnGolem(world);

                if (entityirongolem != null) {
                    list.forEach(GolemSensor::golemDetected);
                }
            }
        }
    }

    public boolean wantsToSpawnGolem(long time) {
        return !this.golemSpawnConditionsMet(this.level.getGameTime()) ? false : !this.brain.hasMemoryValue(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
    }

    @Nullable
    private IronGolem trySpawnGolem(ServerLevel world) {
        BlockPos blockposition = this.blockPosition();

        for (int i = 0; i < 10; ++i) {
            double d0 = (double) (world.random.nextInt(16) - 8);
            double d1 = (double) (world.random.nextInt(16) - 8);
            BlockPos blockposition1 = this.findSpawnPositionForGolemInColumn(blockposition, d0, d1);

            if (blockposition1 != null) {
                // Paper start - Call PreCreatureSpawnEvent
                com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent event;
                event = new com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent(
                    net.minecraft.server.MCUtil.toLocation(level, blockposition1),
                    org.bukkit.entity.EntityType.IRON_GOLEM,
                    org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE
                );
                if (!event.callEvent()) {
                    if (event.shouldAbortSpawn()) {
                        GolemSensor.golemDetected(this); // Set Golem Last Seen to stop it from spawning another one
                        return null;
                    }
                    break;
                }
                // Paper end
                IronGolem entityirongolem = (IronGolem) EntityType.IRON_GOLEM.create(world, (CompoundTag) null, (Component) null, (Player) null, blockposition1, MobSpawnType.MOB_SUMMONED, false, false);

                if (entityirongolem != null) {
                    if (entityirongolem.checkSpawnRules(world, MobSpawnType.MOB_SUMMONED) && entityirongolem.checkSpawnObstruction(world)) {
                        world.addFreshEntityWithPassengers(entityirongolem, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE); // CraftBukkit
                        return entityirongolem;
                    }

                    entityirongolem.discard();
                }
            }
        }

        return null;
    }

    @Nullable
    private BlockPos findSpawnPositionForGolemInColumn(BlockPos pos, double x, double z) {
        boolean flag = true;
        BlockPos blockposition1 = pos.offset(x, 6.0D, z);
        BlockState iblockdata = this.level.getBlockState(blockposition1);

        for (int i = 6; i >= -6; --i) {
            BlockPos blockposition2 = blockposition1;
            BlockState iblockdata1 = iblockdata;

            blockposition1 = blockposition1.below();
            iblockdata = this.level.getBlockState(blockposition1);
            if ((iblockdata1.isAir() || iblockdata1.getMaterial().isLiquid()) && iblockdata.getMaterial().isSolidBlocking()) {
                return blockposition2;
            }
        }

        return null;
    }

    @Override
    public void onReputationEventFrom(ReputationEventType interaction, Entity entity) {
        if (interaction == ReputationEventType.ZOMBIE_VILLAGER_CURED) {
            // Paper start - fix MC-181190
            if (level.paperConfig.fixCuringZombieVillagerDiscountExploit) {
                final GossipContainer.EntityGossips playerReputation = this.getGossips().getReputations().get(entity.getUUID());
                if (playerReputation != null) {
                    playerReputation.remove(GossipType.MAJOR_POSITIVE);
                    playerReputation.remove(GossipType.MINOR_POSITIVE);
                }
            }
            // Paper end
            this.gossips.add(entity.getUUID(), GossipType.MAJOR_POSITIVE, 20);
            this.gossips.add(entity.getUUID(), GossipType.MINOR_POSITIVE, 25);
        } else if (interaction == ReputationEventType.TRADE) {
            this.gossips.add(entity.getUUID(), GossipType.TRADING, 2);
        } else if (interaction == ReputationEventType.VILLAGER_HURT) {
            this.gossips.add(entity.getUUID(), GossipType.MINOR_NEGATIVE, 25);
        } else if (interaction == ReputationEventType.VILLAGER_KILLED) {
            this.gossips.add(entity.getUUID(), GossipType.MAJOR_NEGATIVE, 25);
        }

    }

    @Override
    public int getVillagerXp() {
        return this.villagerXp;
    }

    public void setVillagerXp(int experience) {
        this.villagerXp = experience;
    }

    private void resetNumberOfRestocks() {
        this.catchUpDemand();
        this.numberOfRestocksToday = 0;
    }

    public GossipContainer getGossips() {
        return this.gossips;
    }

    public void setGossips(Tag nbt) {
        this.gossips.update(new Dynamic(NbtOps.INSTANCE, nbt));
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    public void startSleeping(BlockPos pos) {
        super.startSleeping(pos);
        this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.level.getGameTime()); // CraftBukkit - decompile error
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    @Override
    public void stopSleeping() {
        super.stopSleeping();
        this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.level.getGameTime()); // CraftBukkit - decompile error
    }

    private boolean golemSpawnConditionsMet(long worldTime) {
        Optional<Long> optional = this.brain.getMemory(MemoryModuleType.LAST_SLEPT);

        return optional.isPresent() ? worldTime - (Long) optional.get() < 24000L : false;
    }
}
