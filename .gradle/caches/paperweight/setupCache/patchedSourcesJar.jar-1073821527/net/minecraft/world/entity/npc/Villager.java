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
import java.util.List;
import java.util.Map;
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

public class Villager extends AbstractVillager implements ReputationEventHandler, VillagerDataHolder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<VillagerData> DATA_VILLAGER_DATA = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.VILLAGER_DATA);
    public static final int BREEDING_FOOD_THRESHOLD = 12;
    public static final Map<Item, Integer> FOOD_POINTS = ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
    private static final int TRADES_PER_LEVEL = 2;
    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(Items.BREAD, Items.POTATO, Items.CARROT, Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT, Items.BEETROOT_SEEDS);
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
    private final GossipContainer gossips = new GossipContainer();
    private long lastGossipTime;
    private long lastGossipDecayTime;
    private int villagerXp;
    private long lastRestockGameTime;
    public int numberOfRestocksToday;
    private long lastRestockCheckDayTime;
    private boolean assignProfessionWhenSpawned;
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.WALK_TARGET, MemoryModuleType.LOOK_TARGET, MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.BREED_TARGET, MemoryModuleType.PATH, MemoryModuleType.DOORS_TO_CLOSE, MemoryModuleType.NEAREST_BED, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_HOSTILE, MemoryModuleType.SECONDARY_JOB_SITE, MemoryModuleType.HIDING_PLACE, MemoryModuleType.HEARD_BELL_TIME, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.LAST_SLEPT, MemoryModuleType.LAST_WOKEN, MemoryModuleType.LAST_WORKED_AT_POI, MemoryModuleType.GOLEM_DETECTED_RECENTLY);
    private static final ImmutableList<SensorType<? extends Sensor<? super Villager>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_BED, SensorType.HURT_BY, SensorType.VILLAGER_HOSTILES, SensorType.VILLAGER_BABIES, SensorType.SECONDARY_POIS, SensorType.GOLEM_DETECTED);
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<Villager, PoiType>> POI_MEMORIES = ImmutableMap.of(MemoryModuleType.HOME, (villager, poiType) -> {
        return poiType == PoiType.HOME;
    }, MemoryModuleType.JOB_SITE, (villager, poiType) -> {
        return villager.getVillagerData().getProfession().getJobPoiType() == poiType;
    }, MemoryModuleType.POTENTIAL_JOB_SITE, (villager, poiType) -> {
        return PoiType.ALL_JOBS.test(poiType);
    }, MemoryModuleType.MEETING_POINT, (villager, poiType) -> {
        return poiType == PoiType.MEETING;
    });

    public Villager(EntityType<? extends Villager> entityType, Level world) {
        this(entityType, world, VillagerType.PLAINS);
    }

    public Villager(EntityType<? extends Villager> entityType, Level world, VillagerType type) {
        super(entityType, world);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.setCanPickUpLoot(true);
        this.setVillagerData(this.getVillagerData().setType(type).setProfession(VillagerProfession.NONE));
    }

    @Override
    public Brain<Villager> getBrain() {
        return super.getBrain();
    }

    @Override
    protected Brain.Provider<Villager> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<Villager> brain = this.brainProvider().makeBrain(dynamic);
        this.registerBrainGoals(brain);
        return brain;
    }

    public void refreshBrain(ServerLevel world) {
        Brain<Villager> brain = this.getBrain();
        brain.stopAll(world, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBrain());
    }

    private void registerBrainGoals(Brain<Villager> brain) {
        VillagerProfession villagerProfession = this.getVillagerData().getProfession();
        if (this.isBaby()) {
            brain.setSchedule(Schedule.VILLAGER_BABY);
            brain.addActivity(Activity.PLAY, VillagerGoalPackages.getPlayPackage(0.5F));
        } else {
            brain.setSchedule(Schedule.VILLAGER_DEFAULT);
            brain.addActivityWithConditions(Activity.WORK, VillagerGoalPackages.getWorkPackage(villagerProfession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        }

        brain.addActivity(Activity.CORE, VillagerGoalPackages.getCorePackage(villagerProfession, 0.5F));
        brain.addActivityWithConditions(Activity.MEET, VillagerGoalPackages.getMeetPackage(villagerProfession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        brain.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.IDLE, VillagerGoalPackages.getIdlePackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.PRE_RAID, VillagerGoalPackages.getPreRaidPackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.RAID, VillagerGoalPackages.getRaidPackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.HIDE, VillagerGoalPackages.getHidePackage(villagerProfession, 0.5F));
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(this.level.getDayTime(), this.level.getGameTime());
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (this.level instanceof ServerLevel) {
            this.refreshBrain((ServerLevel)this.level);
        }

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5D).add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    public boolean assignProfessionWhenSpawned() {
        return this.assignProfessionWhenSpawned;
    }

    @Override
    protected void customServerAiStep() {
        this.level.getProfiler().push("villagerBrain");
        this.getBrain().tick((ServerLevel)this.level, this);
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

                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            }
        }

        if (this.lastTradedPlayer != null && this.level instanceof ServerLevel) {
            ((ServerLevel)this.level).onReputationEvent(ReputationEventType.TRADE, this.lastTradedPlayer, this);
            this.level.broadcastEntityEvent(this, (byte)14);
            this.lastTradedPlayer = null;
        }

        if (!this.isNoAi() && this.random.nextInt(100) == 0) {
            Raid raid = ((ServerLevel)this.level).getRaidAt(this.blockPosition());
            if (raid != null && raid.isActive() && !raid.isOver()) {
                this.level.broadcastEntityEvent(this, (byte)42);
            }
        }

        if (this.getVillagerData().getProfession() == VillagerProfession.NONE && this.isTrading()) {
            this.stopTrading();
        }

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
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.VILLAGER_SPAWN_EGG) && this.isAlive() && !this.isTrading() && !this.isSleeping()) {
            if (this.isBaby()) {
                this.setUnhappy();
                return InteractionResult.sidedSuccess(this.level.isClientSide);
            } else {
                boolean bl = this.getOffers().isEmpty();
                if (hand == InteractionHand.MAIN_HAND) {
                    if (bl && !this.level.isClientSide) {
                        this.setUnhappy();
                    }

                    player.awardStat(Stats.TALKED_TO_VILLAGER);
                }

                if (bl) {
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
        boolean bl = this.getTradingPlayer() != null && customer == null;
        super.setTradingPlayer(customer);
        if (bl) {
            this.stopTrading();
        }

    }

    @Override
    protected void stopTrading() {
        super.stopTrading();
        this.resetSpecialPrices();
    }

    private void resetSpecialPrices() {
        for(MerchantOffer merchantOffer : this.getOffers()) {
            merchantOffer.resetSpecialPriceDiff();
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

        for(MerchantOffer merchantOffer : this.getOffers()) {
            merchantOffer.resetUses();
        }

        this.lastRestockGameTime = this.level.getGameTime();
        ++this.numberOfRestocksToday;
    }

    private boolean needsToRestock() {
        for(MerchantOffer merchantOffer : this.getOffers()) {
            if (merchantOffer.needsRestock()) {
                return true;
            }
        }

        return false;
    }

    private boolean allowedToRestock() {
        return this.numberOfRestocksToday == 0 || this.numberOfRestocksToday < 2 && this.level.getGameTime() > this.lastRestockGameTime + 2400L;
    }

    public boolean shouldRestock() {
        long l = this.lastRestockGameTime + 12000L;
        long m = this.level.getGameTime();
        boolean bl = m > l;
        long n = this.level.getDayTime();
        if (this.lastRestockCheckDayTime > 0L) {
            long o = this.lastRestockCheckDayTime / 24000L;
            long p = n / 24000L;
            bl |= p > o;
        }

        this.lastRestockCheckDayTime = n;
        if (bl) {
            this.lastRestockGameTime = m;
            this.resetNumberOfRestocks();
        }

        return this.allowedToRestock() && this.needsToRestock();
    }

    private void catchUpDemand() {
        int i = 2 - this.numberOfRestocksToday;
        if (i > 0) {
            for(MerchantOffer merchantOffer : this.getOffers()) {
                merchantOffer.resetUses();
            }
        }

        for(int j = 0; j < i; ++j) {
            this.updateDemand();
        }

    }

    private void updateDemand() {
        for(MerchantOffer merchantOffer : this.getOffers()) {
            merchantOffer.updateDemand();
        }

    }

    private void updateSpecialPrices(Player player) {
        int i = this.getPlayerReputation(player);
        if (i != 0) {
            for(MerchantOffer merchantOffer : this.getOffers()) {
                merchantOffer.addToSpecialPriceDiff(-Mth.floor((float)i * merchantOffer.getPriceMultiplier()));
            }
        }

        if (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            MobEffectInstance mobEffectInstance = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
            int j = mobEffectInstance.getAmplifier();

            for(MerchantOffer merchantOffer2 : this.getOffers()) {
                double d = 0.3D + 0.0625D * (double)j;
                int k = (int)Math.floor(d * (double)merchantOffer2.getBaseCostA().getCount());
                merchantOffer2.addToSpecialPriceDiff(-Math.max(k, 1));
            }
        }

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, this.getVillagerData()).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.put("VillagerData", tag);
        });
        nbt.putByte("FoodLevel", (byte)this.foodLevel);
        nbt.put("Gossips", this.gossips.store(NbtOps.INSTANCE).getValue());
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
            DataResult<VillagerData> dataResult = VillagerData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.get("VillagerData")));
            dataResult.resultOrPartial(LOGGER::error).ifPresent(this::setVillagerData);
        }

        if (nbt.contains("Offers", 10)) {
            this.offers = new MerchantOffers(nbt.getCompound("Offers"));
        }

        if (nbt.contains("FoodLevel", 1)) {
            this.foodLevel = nbt.getByte("FoodLevel");
        }

        ListTag listTag = nbt.getList("Gossips", 10);
        this.gossips.update(new Dynamic<>(NbtOps.INSTANCE, listTag));
        if (nbt.contains("Xp", 3)) {
            this.villagerXp = nbt.getInt("Xp");
        }

        this.lastRestockGameTime = nbt.getLong("LastRestock");
        this.lastGossipDecayTime = nbt.getLong("LastGossipDecay");
        this.setCanPickUpLoot(true);
        if (this.level instanceof ServerLevel) {
            this.refreshBrain((ServerLevel)this.level);
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
        if (this.isSleeping()) {
            return null;
        } else {
            return this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
        }
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
        SoundEvent soundEvent = this.getVillagerData().getProfession().getWorkSound();
        if (soundEvent != null) {
            this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    @Override
    public void setVillagerData(VillagerData villagerData) {
        VillagerData villagerData2 = this.getVillagerData();
        if (villagerData2.getProfession() != villagerData.getProfession()) {
            this.offers = null;
        }

        this.entityData.set(DATA_VILLAGER_DATA, villagerData);
    }

    @Override
    public VillagerData getVillagerData() {
        return this.entityData.get(DATA_VILLAGER_DATA);
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
            this.level.addFreshEntity(new ExperienceOrb(this.level, this.getX(), this.getY() + 0.5D, this.getZ(), i));
        }

    }

    public void setChasing(boolean bl) {
        this.chasing = bl;
    }

    public boolean isChasing() {
        return this.chasing;
    }

    @Override
    public void setLastHurtByMob(@Nullable LivingEntity attacker) {
        if (attacker != null && this.level instanceof ServerLevel) {
            ((ServerLevel)this.level).onReputationEvent(ReputationEventType.VILLAGER_HURT, attacker, this);
            if (this.isAlive() && attacker instanceof Player) {
                this.level.broadcastEntityEvent(this, (byte)13);
            }
        }

        super.setLastHurtByMob(attacker);
    }

    @Override
    public void die(DamageSource source) {
        LOGGER.info("Villager {} died, message: '{}'", this, source.getLocalizedDeathMessage(this).getString());
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
        Level optional = this.level;
        if (optional instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)optional;
            Optional<NearestVisibleLivingEntities> optional = this.brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
            if (!optional.isEmpty()) {
                optional.get().findAll(ReputationEventHandler.class::isInstance).forEach((livingEntity) -> {
                    serverLevel.onReputationEvent(ReputationEventType.VILLAGER_KILLED, killer, (ReputationEventHandler)livingEntity);
                });
            }
        }
    }

    public void releasePoi(MemoryModuleType<GlobalPos> memoryModuleType) {
        if (this.level instanceof ServerLevel) {
            MinecraftServer minecraftServer = ((ServerLevel)this.level).getServer();
            this.brain.getMemory(memoryModuleType).ifPresent((pos) -> {
                ServerLevel serverLevel = minecraftServer.getLevel(pos.dimension());
                if (serverLevel != null) {
                    PoiManager poiManager = serverLevel.getPoiManager();
                    Optional<PoiType> optional = poiManager.getType(pos.pos());
                    BiPredicate<Villager, PoiType> biPredicate = POI_MEMORIES.get(memoryModuleType);
                    if (optional.isPresent() && biPredicate.test(this, optional.get())) {
                        poiManager.release(pos.pos());
                        DebugPackets.sendPoiTicketCountPacket(serverLevel, pos.pos());
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
            for(int i = 0; i < this.getInventory().getContainerSize(); ++i) {
                ItemStack itemStack = this.getInventory().getItem(i);
                if (!itemStack.isEmpty()) {
                    Integer integer = FOOD_POINTS.get(itemStack.getItem());
                    if (integer != null) {
                        int j = itemStack.getCount();

                        for(int k = j; k > 0; --k) {
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
        return this.gossips.getReputation(player.getUUID(), (gossipType) -> {
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
        return new TranslatableComponent(this.getType().getDescriptionId() + "." + Registry.VILLAGER_PROFESSION.getKey(this.getVillagerData().getProfession()).getPath());
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
    public Villager getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        double d = this.random.nextDouble();
        VillagerType villagerType;
        if (d < 0.5D) {
            villagerType = VillagerType.byBiome(serverLevel.getBiome(this.blockPosition()));
        } else if (d < 0.75D) {
            villagerType = this.getVillagerData().getType();
        } else {
            villagerType = ((Villager)ageableMob).getVillagerData().getType();
        }

        Villager villager = new Villager(EntityType.VILLAGER, serverLevel, villagerType);
        villager.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(villager.blockPosition()), MobSpawnType.BREEDING, (SpawnGroupData)null, (CompoundTag)null);
        return villager;
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) {
            LOGGER.info("Villager {} was struck by lightning {}.", this, lightning);
            Witch witch = EntityType.WITCH.create(world);
            witch.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            witch.finalizeSpawn(world, world.getCurrentDifficultyAt(witch.blockPosition()), MobSpawnType.CONVERSION, (SpawnGroupData)null, (CompoundTag)null);
            witch.setNoAi(this.isNoAi());
            if (this.hasCustomName()) {
                witch.setCustomName(this.getCustomName());
                witch.setCustomNameVisible(this.isCustomNameVisible());
            }

            witch.setPersistenceRequired();
            world.addFreshEntityWithPassengers(witch);
            this.releaseAllPois();
            this.discard();
        } else {
            super.thunderHit(world, lightning);
        }

    }

    @Override
    protected void pickUpItem(ItemEntity item) {
        ItemStack itemStack = item.getItem();
        if (this.wantsToPickUp(itemStack)) {
            SimpleContainer simpleContainer = this.getInventory();
            boolean bl = simpleContainer.canAddItem(itemStack);
            if (!bl) {
                return;
            }

            this.onItemPickup(item);
            this.take(item, itemStack.getCount());
            ItemStack itemStack2 = simpleContainer.addItem(itemStack);
            if (itemStack2.isEmpty()) {
                item.discard();
            } else {
                itemStack.setCount(itemStack2.getCount());
            }
        }

    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        Item item = stack.getItem();
        return (WANTED_ITEMS.contains(item) || this.getVillagerData().getProfession().getRequestedItems().contains(item)) && this.getInventory().canAddItem(stack);
    }

    public boolean hasExcessFood() {
        return this.countFoodPointsInInventory() >= 24;
    }

    public boolean wantsMoreFood() {
        return this.countFoodPointsInInventory() < 12;
    }

    private int countFoodPointsInInventory() {
        SimpleContainer simpleContainer = this.getInventory();
        return FOOD_POINTS.entrySet().stream().mapToInt((entry) -> {
            return simpleContainer.countItem(entry.getKey()) * entry.getValue();
        }).sum();
    }

    public boolean hasFarmSeeds() {
        return this.getInventory().hasAnyOf(ImmutableSet.of(Items.WHEAT_SEEDS, Items.POTATO, Items.CARROT, Items.BEETROOT_SEEDS));
    }

    @Override
    protected void updateTrades() {
        VillagerData villagerData = this.getVillagerData();
        Int2ObjectMap<VillagerTrades.ItemListing[]> int2ObjectMap = VillagerTrades.TRADES.get(villagerData.getProfession());
        if (int2ObjectMap != null && !int2ObjectMap.isEmpty()) {
            VillagerTrades.ItemListing[] itemListings = int2ObjectMap.get(villagerData.getLevel());
            if (itemListings != null) {
                MerchantOffers merchantOffers = this.getOffers();
                this.addOffersFromItemListings(merchantOffers, itemListings, 2);
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
        long l = this.level.getGameTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = l;
        } else if (l >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = l;
        }
    }

    public void spawnGolemIfNeeded(ServerLevel world, long time, int requiredCount) {
        if (this.wantsToSpawnGolem(time)) {
            AABB aABB = this.getBoundingBox().inflate(10.0D, 10.0D, 10.0D);
            List<Villager> list = world.getEntitiesOfClass(Villager.class, aABB);
            List<Villager> list2 = list.stream().filter((villager) -> {
                return villager.wantsToSpawnGolem(time);
            }).limit(5L).collect(Collectors.toList());
            if (list2.size() >= requiredCount) {
                IronGolem ironGolem = this.trySpawnGolem(world);
                if (ironGolem != null) {
                    list.forEach(GolemSensor::golemDetected);
                }
            }
        }
    }

    public boolean wantsToSpawnGolem(long time) {
        if (!this.golemSpawnConditionsMet(this.level.getGameTime())) {
            return false;
        } else {
            return !this.brain.hasMemoryValue(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
        }
    }

    @Nullable
    private IronGolem trySpawnGolem(ServerLevel world) {
        BlockPos blockPos = this.blockPosition();

        for(int i = 0; i < 10; ++i) {
            double d = (double)(world.random.nextInt(16) - 8);
            double e = (double)(world.random.nextInt(16) - 8);
            BlockPos blockPos2 = this.findSpawnPositionForGolemInColumn(blockPos, d, e);
            if (blockPos2 != null) {
                IronGolem ironGolem = EntityType.IRON_GOLEM.create(world, (CompoundTag)null, (Component)null, (Player)null, blockPos2, MobSpawnType.MOB_SUMMONED, false, false);
                if (ironGolem != null) {
                    if (ironGolem.checkSpawnRules(world, MobSpawnType.MOB_SUMMONED) && ironGolem.checkSpawnObstruction(world)) {
                        world.addFreshEntityWithPassengers(ironGolem);
                        return ironGolem;
                    }

                    ironGolem.discard();
                }
            }
        }

        return null;
    }

    @Nullable
    private BlockPos findSpawnPositionForGolemInColumn(BlockPos pos, double x, double z) {
        int i = 6;
        BlockPos blockPos = pos.offset(x, 6.0D, z);
        BlockState blockState = this.level.getBlockState(blockPos);

        for(int j = 6; j >= -6; --j) {
            BlockPos blockPos2 = blockPos;
            BlockState blockState2 = blockState;
            blockPos = blockPos.below();
            blockState = this.level.getBlockState(blockPos);
            if ((blockState2.isAir() || blockState2.getMaterial().isLiquid()) && blockState.getMaterial().isSolidBlocking()) {
                return blockPos2;
            }
        }

        return null;
    }

    @Override
    public void onReputationEventFrom(ReputationEventType interaction, Entity entity) {
        if (interaction == ReputationEventType.ZOMBIE_VILLAGER_CURED) {
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
        this.gossips.update(new Dynamic<>(NbtOps.INSTANCE, nbt));
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    public void startSleeping(BlockPos pos) {
        super.startSleeping(pos);
        this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.level.getGameTime());
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    @Override
    public void stopSleeping() {
        super.stopSleeping();
        this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.level.getGameTime());
    }

    private boolean golemSpawnConditionsMet(long worldTime) {
        Optional<Long> optional = this.brain.getMemory(MemoryModuleType.LAST_SLEPT);
        if (optional.isPresent()) {
            return worldTime - optional.get() < 24000L;
        } else {
            return false;
        }
    }
}
