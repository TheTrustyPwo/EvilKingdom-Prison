package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CopyMemoryWithExpiry;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.behavior.DismountOrSkipMounting;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.GoToCelebrateLocation;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.Mount;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunIf;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.RunSometimes;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StartCelebratingIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
// CraftBukkit start
import java.util.stream.Collectors;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.event.entity.PiglinBarterEvent;
// CraftBukkit end

public class PiglinAi {

    public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
    public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
    public static final Item BARTERING_ITEM = Items.GOLD_INGOT;
    private static final int PLAYER_ANGER_RANGE = 16;
    private static final int ANGER_DURATION = 600;
    private static final int ADMIRE_DURATION = 120;
    private static final int MAX_DISTANCE_TO_WALK_TO_ITEM = 9;
    private static final int MAX_TIME_TO_WALK_TO_ITEM = 200;
    private static final int HOW_LONG_TIME_TO_DISABLE_ADMIRE_WALKING_IF_CANT_REACH_ITEM = 200;
    private static final int CELEBRATION_TIME = 300;
    private static final UniformInt TIME_BETWEEN_HUNTS = TimeUtil.rangeOfSeconds(30, 120);
    private static final int BABY_FLEE_DURATION_AFTER_GETTING_HIT = 100;
    private static final int HIT_BY_PLAYER_MEMORY_TIMEOUT = 400;
    private static final int MAX_WALK_DISTANCE_TO_START_RIDING = 8;
    private static final UniformInt RIDE_START_INTERVAL = TimeUtil.rangeOfSeconds(10, 40);
    private static final UniformInt RIDE_DURATION = TimeUtil.rangeOfSeconds(10, 30);
    private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final int EAT_COOLDOWN = 200;
    private static final int DESIRED_DISTANCE_FROM_ENTITY_WHEN_AVOIDING = 12;
    private static final int MAX_LOOK_DIST = 8;
    private static final int MAX_LOOK_DIST_FOR_PLAYER_HOLDING_LOVED_ITEM = 14;
    private static final int INTERACTION_RANGE = 8;
    private static final int MIN_DESIRED_DIST_FROM_TARGET_WHEN_HOLDING_CROSSBOW = 5;
    private static final float SPEED_WHEN_STRAFING_BACK_FROM_TARGET = 0.75F;
    private static final int DESIRED_DISTANCE_FROM_ZOMBIFIED = 6;
    private static final UniformInt AVOID_ZOMBIFIED_DURATION = TimeUtil.rangeOfSeconds(5, 7);
    private static final UniformInt BABY_AVOID_NEMESIS_DURATION = TimeUtil.rangeOfSeconds(5, 7);
    private static final float PROBABILITY_OF_CELEBRATION_DANCE = 0.1F;
    private static final float SPEED_MULTIPLIER_WHEN_AVOIDING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_MOUNTING = 0.8F;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_WANTED_ITEM = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_CELEBRATE_LOCATION = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_DANCING = 0.6F;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;

    public PiglinAi() {}

    protected static Brain<?> makeBrain(Piglin piglin, Brain<Piglin> brain) {
        PiglinAi.initCoreActivity(brain);
        PiglinAi.initIdleActivity(brain);
        PiglinAi.initAdmireItemActivity(brain);
        PiglinAi.initFightActivity(piglin, brain);
        PiglinAi.initCelebrateActivity(brain);
        PiglinAi.initRetreatActivity(brain);
        PiglinAi.initRideHoglinActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    protected static void initMemories(Piglin piglin) {
        int i = PiglinAi.TIME_BETWEEN_HUNTS.sample(piglin.level.random);

        piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, (long) i);
    }

    private static void initCoreActivity(Brain<Piglin> piglin) {
        piglin.addActivity(Activity.CORE, 0, ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink(), new InteractWithDoor(), PiglinAi.babyAvoidNemesis(), PiglinAi.avoidZombified(), new StopHoldingItemIfNoLongerAdmiring<>(), new StartAdmiringItemIfSeen<>(120), new StartCelebratingIfTargetDead(300, PiglinAi::wantsToDance), new StopBeingAngryIfTargetDead<>()));
    }

    private static void initIdleActivity(Brain<Piglin> piglin) {
        piglin.addActivity(Activity.IDLE, 10, ImmutableList.of(new SetEntityLookTarget(PiglinAi::isPlayerHoldingLovedItem, 14.0F), new StartAttacking<>(AbstractPiglin::isAdult, PiglinAi::findNearestValidAttackTarget), new RunIf<>(Piglin::canHunt, new StartHuntingHoglin<>()), PiglinAi.avoidRepellent(), PiglinAi.babySometimesRideBabyHoglin(), PiglinAi.createIdleLookBehaviors(), PiglinAi.createIdleMovementBehaviors(), new SetLookAndInteract(EntityType.PLAYER, 4)));
    }

    private static void initFightActivity(Piglin piglin, Brain<Piglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(new StopAttackingIfTargetInvalid<>((entityliving) -> {
            return !PiglinAi.isNearestValidAttackTarget(piglin, entityliving);
        }), new RunIf<>(PiglinAi::hasCrossbow, new BackUpIfTooClose<>(5, 0.75F)), new SetWalkTargetFromAttackTargetIfTargetOutOfReach(1.0F), new MeleeAttack(20), new CrossbowAttack<>(), new RememberIfHoglinWasKilled<>(), new EraseMemoryIf<>(PiglinAi::isNearZombified, MemoryModuleType.ATTACK_TARGET)), MemoryModuleType.ATTACK_TARGET);
    }

    private static void initCelebrateActivity(Brain<Piglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.CELEBRATE, 10, ImmutableList.of(PiglinAi.avoidRepellent(), new SetEntityLookTarget(PiglinAi::isPlayerHoldingLovedItem, 14.0F), new StartAttacking<>(AbstractPiglin::isAdult, PiglinAi::findNearestValidAttackTarget), new RunIf<>((entitypiglin) -> {
            return !entitypiglin.isDancing();
        }, new GoToCelebrateLocation<>(2, 1.0F)), new RunIf<>(Piglin::isDancing, new GoToCelebrateLocation<>(4, 0.6F)), new RunOne<>(ImmutableList.of(Pair.of(new SetEntityLookTarget(EntityType.PIGLIN, 8.0F), 1), Pair.of(new RandomStroll(0.6F, 2, 1), 1), Pair.of(new DoNothing(10, 20), 1)))), MemoryModuleType.CELEBRATE_LOCATION);
    }

    private static void initAdmireItemActivity(Brain<Piglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.ADMIRE_ITEM, 10, ImmutableList.of(new GoToWantedItem<>(PiglinAi::isNotHoldingLovedItemInOffHand, 1.0F, true, 9), new StopAdmiringIfItemTooFarAway<>(9), new StopAdmiringIfTiredOfTryingToReachItem<>(200, 200)), MemoryModuleType.ADMIRING_ITEM);
    }

    private static void initRetreatActivity(Brain<Piglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.AVOID, 10, ImmutableList.of(SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.0F, 12, true), PiglinAi.createIdleLookBehaviors(), PiglinAi.createIdleMovementBehaviors(), new EraseMemoryIf<>(PiglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)), MemoryModuleType.AVOID_TARGET);
    }

    private static void initRideHoglinActivity(Brain<Piglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.RIDE, 10, ImmutableList.of(new Mount<>(0.8F), new SetEntityLookTarget(PiglinAi::isPlayerHoldingLovedItem, 8.0F), new RunIf<>(Entity::isPassenger, PiglinAi.createIdleLookBehaviors()), new DismountOrSkipMounting<>(8, PiglinAi::wantsToStopRiding)), MemoryModuleType.RIDE_TARGET);
    }

    private static RunOne<Piglin> createIdleLookBehaviors() {
        return new RunOne<>(ImmutableList.of(Pair.of(new SetEntityLookTarget(EntityType.PLAYER, 8.0F), 1), Pair.of(new SetEntityLookTarget(EntityType.PIGLIN, 8.0F), 1), Pair.of(new SetEntityLookTarget(8.0F), 1), Pair.of(new DoNothing(30, 60), 1)));
    }

    private static RunOne<Piglin> createIdleMovementBehaviors() {
        return new RunOne<>(ImmutableList.of(Pair.of(new RandomStroll(0.6F), 2), Pair.of(InteractWith.of(EntityType.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2), Pair.of(new RunIf<>(PiglinAi::doesntSeeAnyPlayerHoldingLovedItem, new SetWalkTargetFromLookTarget(0.6F, 3)), 2), Pair.of(new DoNothing(30, 60), 1)));
    }

    private static SetWalkTargetAwayFrom<BlockPos> avoidRepellent() {
        return SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, false);
    }

    private static CopyMemoryWithExpiry<Piglin, LivingEntity> babyAvoidNemesis() {
        return new CopyMemoryWithExpiry<>(Piglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.AVOID_TARGET, PiglinAi.BABY_AVOID_NEMESIS_DURATION);
    }

    private static CopyMemoryWithExpiry<Piglin, LivingEntity> avoidZombified() {
        return new CopyMemoryWithExpiry<>(PiglinAi::isNearZombified, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.AVOID_TARGET, PiglinAi.AVOID_ZOMBIFIED_DURATION);
    }

    protected static void updateActivity(Piglin piglin) {
        Brain<Piglin> behaviorcontroller = piglin.getBrain();
        Activity activity = (Activity) behaviorcontroller.getActiveNonCoreActivity().orElse(null); // CraftBukkit - decompile error

        behaviorcontroller.setActiveActivityToFirstValid(ImmutableList.of(Activity.ADMIRE_ITEM, Activity.FIGHT, Activity.AVOID, Activity.CELEBRATE, Activity.RIDE, Activity.IDLE));
        Activity activity1 = (Activity) behaviorcontroller.getActiveNonCoreActivity().orElse(null); // CraftBukkit - decompile error

        if (activity != activity1) {
            Optional<SoundEvent> optional = PiglinAi.getSoundForCurrentActivity(piglin); // CraftBukkit - decompile error

            Objects.requireNonNull(piglin);
            optional.ifPresent(piglin::playSound);
        }

        piglin.setAggressive(behaviorcontroller.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
        if (!behaviorcontroller.hasMemoryValue(MemoryModuleType.RIDE_TARGET) && PiglinAi.isBabyRidingBaby(piglin)) {
            piglin.stopRiding();
        }

        if (!behaviorcontroller.hasMemoryValue(MemoryModuleType.CELEBRATE_LOCATION)) {
            behaviorcontroller.eraseMemory(MemoryModuleType.DANCING);
        }

        piglin.setDancing(behaviorcontroller.hasMemoryValue(MemoryModuleType.DANCING));
    }

    private static boolean isBabyRidingBaby(Piglin piglin) {
        if (!piglin.isBaby()) {
            return false;
        } else {
            Entity entity = piglin.getVehicle();

            return entity instanceof Piglin && ((Piglin) entity).isBaby() || entity instanceof Hoglin && ((Hoglin) entity).isBaby();
        }
    }

    protected static void pickUpItem(Piglin piglin, ItemEntity drop) {
        PiglinAi.stopWalking(piglin);
        ItemStack itemstack;

        // CraftBukkit start
        if (drop.getItem().is(Items.GOLD_NUGGET) && !org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityPickupItemEvent(piglin, drop, 0, false).isCancelled()) {
            piglin.take(drop, drop.getItem().getCount());
            itemstack = drop.getItem();
            drop.discard();
        } else if (!org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityPickupItemEvent(piglin, drop, drop.getItem().getCount() - 1, false).isCancelled()) {
            piglin.take(drop, 1);
            itemstack = PiglinAi.removeOneItemFromItemEntity(drop);
        } else {
            return;
        }
        // CraftBukkit end

        if (PiglinAi.isLovedItem(itemstack, piglin)) { // CraftBukkit - Changes to allow for custom payment in bartering
            piglin.getBrain().eraseMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            PiglinAi.holdInOffhand(piglin, itemstack);
            PiglinAi.admireGoldItem(piglin);
        } else if (PiglinAi.isFood(itemstack) && !PiglinAi.hasEatenRecently(piglin)) {
            PiglinAi.eat(piglin);
        } else {
            boolean flag = piglin.equipItemIfPossible(itemstack, drop); // CraftBukkit

            if (!flag) {
                PiglinAi.putInInventory(piglin, itemstack);
            }
        }
    }

    private static void holdInOffhand(Piglin piglin, ItemStack stack) {
        if (PiglinAi.isHoldingItemInOffHand(piglin)) {
            piglin.spawnAtLocation(piglin.getItemInHand(InteractionHand.OFF_HAND));
        }

        piglin.holdInOffHand(stack);
    }

    private static ItemStack removeOneItemFromItemEntity(ItemEntity stack) {
        ItemStack itemstack = stack.getItem();
        ItemStack itemstack1 = itemstack.split(1);

        if (itemstack.isEmpty()) {
            stack.discard();
        } else {
            stack.setItem(itemstack);
        }

        return itemstack1;
    }

    protected static void stopHoldingOffHandItem(Piglin piglin, boolean barter) {
        ItemStack itemstack = piglin.getItemInHand(InteractionHand.OFF_HAND);

        piglin.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        boolean flag1;

        if (piglin.isAdult()) {
            flag1 = PiglinAi.isBarterCurrency(itemstack, piglin); // CraftBukkit - Changes to allow custom payment for bartering
            if (barter && flag1) {
                // CraftBukkit start
                PiglinBarterEvent event = CraftEventFactory.callPiglinBarterEvent(piglin, PiglinAi.getBarterResponseItems(piglin), itemstack);
                if (!event.isCancelled()) {
                    PiglinAi.throwItems(piglin, event.getOutcome().stream().map(CraftItemStack::asNMSCopy).collect(Collectors.toList()));
                }
                // CraftBukkit end
            } else if (!flag1) {
                boolean flag2 = piglin.equipItemIfPossible(itemstack);

                if (!flag2) {
                    PiglinAi.putInInventory(piglin, itemstack);
                }
            }
        } else {
            flag1 = piglin.equipItemIfPossible(itemstack);
            if (!flag1) {
                ItemStack itemstack1 = piglin.getMainHandItem();

                if (PiglinAi.isLovedItem(itemstack1, piglin)) { // CraftBukkit - Changes to allow for custom payment in bartering
                    PiglinAi.putInInventory(piglin, itemstack1);
                } else {
                    PiglinAi.throwItems(piglin, Collections.singletonList(itemstack1));
                }

                piglin.holdInMainHand(itemstack);
            }
        }

    }

    protected static void cancelAdmiring(Piglin piglin) {
        if (PiglinAi.isAdmiringItem(piglin) && !piglin.getOffhandItem().isEmpty()) {
            piglin.spawnAtLocation(piglin.getOffhandItem());
            piglin.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

    }

    private static void putInInventory(Piglin piglin, ItemStack stack) {
        ItemStack itemstack1 = piglin.addToInventory(stack);

        PiglinAi.throwItemsTowardRandomPos(piglin, Collections.singletonList(itemstack1));
    }

    private static void throwItems(Piglin piglin, List<ItemStack> items) {
        Optional<Player> optional = piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);

        if (optional.isPresent()) {
            PiglinAi.throwItemsTowardPlayer(piglin, (Player) optional.get(), items);
        } else {
            PiglinAi.throwItemsTowardRandomPos(piglin, items);
        }

    }

    private static void throwItemsTowardRandomPos(Piglin piglin, List<ItemStack> items) {
        PiglinAi.throwItemsTowardPos(piglin, items, PiglinAi.getRandomNearbyPos(piglin));
    }

    private static void throwItemsTowardPlayer(Piglin piglin, Player player, List<ItemStack> items) {
        PiglinAi.throwItemsTowardPos(piglin, items, player.position());
    }

    private static void throwItemsTowardPos(Piglin piglin, List<ItemStack> items, Vec3 pos) {
        if (!items.isEmpty()) {
            piglin.swing(InteractionHand.OFF_HAND);
            Iterator iterator = items.iterator();

            while (iterator.hasNext()) {
                ItemStack itemstack = (ItemStack) iterator.next();

                BehaviorUtils.throwItem(piglin, itemstack, pos.add(0.0D, 1.0D, 0.0D));
            }
        }

    }

    private static List<ItemStack> getBarterResponseItems(Piglin piglin) {
        LootTable loottable = piglin.level.getServer().getLootTables().get(BuiltInLootTables.PIGLIN_BARTERING);
        List<ItemStack> list = loottable.getRandomItems((new LootContext.Builder((ServerLevel) piglin.level)).withParameter(LootContextParams.THIS_ENTITY, piglin).withRandom(piglin.level.random).create(LootContextParamSets.PIGLIN_BARTER));

        return list;
    }

    private static boolean wantsToDance(LivingEntity piglin, LivingEntity target) {
        return target.getType() != EntityType.HOGLIN ? false : (new Random(piglin.level.getGameTime())).nextFloat() < 0.1F;
    }

    protected static boolean wantsToPickup(Piglin piglin, ItemStack stack) {
        if (piglin.isBaby() && stack.is(ItemTags.IGNORED_BY_PIGLIN_BABIES)) {
            return false;
        } else if (stack.is(ItemTags.PIGLIN_REPELLENTS)) {
            return false;
        } else if (PiglinAi.isAdmiringDisabled(piglin) && piglin.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            return false;
        } else if (PiglinAi.isBarterCurrency(stack, piglin)) { // CraftBukkit
            return PiglinAi.isNotHoldingLovedItemInOffHand(piglin);
        } else {
            boolean flag = piglin.canAddToInventory(stack);

            return stack.is(Items.GOLD_NUGGET) ? flag : (PiglinAi.isFood(stack) ? !PiglinAi.hasEatenRecently(piglin) && flag : (!PiglinAi.isLovedItem(stack) ? piglin.canReplaceCurrentItem(stack) : PiglinAi.isNotHoldingLovedItemInOffHand(piglin) && flag));
        }
    }

    // CraftBukkit start - Added method to allow checking for custom payment items
    protected static boolean isLovedItem(ItemStack itemstack, Piglin piglin) {
        return PiglinAi.isLovedItem(itemstack) || (piglin.interestItems.contains(itemstack.getItem()) || piglin.allowedBarterItems.contains(itemstack.getItem()));
    }
    // CraftBukkit end

    protected static boolean isLovedItem(ItemStack stack) {
        return stack.is(ItemTags.PIGLIN_LOVED);
    }

    private static boolean wantsToStopRiding(Piglin piglin, Entity ridden) {
        if (!(ridden instanceof Mob)) {
            return false;
        } else {
            Mob entityinsentient = (Mob) ridden;

            return !entityinsentient.isBaby() || !entityinsentient.isAlive() || PiglinAi.wasHurtRecently(piglin) || PiglinAi.wasHurtRecently(entityinsentient) || entityinsentient instanceof Piglin && entityinsentient.getVehicle() == null;
        }
    }

    private static boolean isNearestValidAttackTarget(Piglin piglin, LivingEntity target) {
        return PiglinAi.findNearestValidAttackTarget(piglin).filter((entityliving1) -> {
            return entityliving1 == target;
        }).isPresent();
    }

    private static boolean isNearZombified(Piglin piglin) {
        Brain<Piglin> behaviorcontroller = piglin.getBrain();

        if (behaviorcontroller.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED)) {
            LivingEntity entityliving = (LivingEntity) behaviorcontroller.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).get();

            return piglin.closerThan(entityliving, 6.0D);
        } else {
            return false;
        }
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(Piglin piglin) {
        Brain<Piglin> behaviorcontroller = piglin.getBrain();

        if (PiglinAi.isNearZombified(piglin)) {
            return Optional.empty();
        } else {
            Optional<LivingEntity> optional = BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);

            if (optional.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(piglin, (LivingEntity) optional.get())) {
                return optional;
            } else {
                Optional optional1;

                if (behaviorcontroller.hasMemoryValue(MemoryModuleType.UNIVERSAL_ANGER)) {
                    optional1 = behaviorcontroller.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
                    if (optional1.isPresent()) {
                        return optional1;
                    }
                }

                optional1 = behaviorcontroller.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
                if (optional1.isPresent()) {
                    return optional1;
                } else {
                    Optional<Player> optional2 = behaviorcontroller.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);

                    return optional2.isPresent() && Sensor.isEntityAttackable(piglin, (LivingEntity) optional2.get()) ? optional2 : Optional.empty();
                }
            }
        }
    }

    public static void angerNearbyPiglins(Player player, boolean blockOpen) {
        if (!player.level.paperConfig.piglinsGuardChests) return; // Paper
        List<Piglin> list = player.level.getEntitiesOfClass(Piglin.class, player.getBoundingBox().inflate(16.0D));

        list.stream().filter(PiglinAi::isIdle).filter((entitypiglin) -> {
            return !blockOpen || BehaviorUtils.canSee(entitypiglin, player);
        }).forEach((entitypiglin) -> {
            if (entitypiglin.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                PiglinAi.setAngerTargetToNearestTargetablePlayerIfFound(entitypiglin, player);
            } else {
                PiglinAi.setAngerTarget(entitypiglin, player);
            }

        });
    }

    public static InteractionResult mobInteract(Piglin piglin, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (PiglinAi.canAdmire(piglin, itemstack)) {
            ItemStack itemstack1 = itemstack.split(1);

            PiglinAi.holdInOffhand(piglin, itemstack1);
            PiglinAi.admireGoldItem(piglin);
            PiglinAi.stopWalking(piglin);
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.PASS;
        }
    }

    protected static boolean canAdmire(Piglin piglin, ItemStack nearbyItems) {
        return !PiglinAi.isAdmiringDisabled(piglin) && !PiglinAi.isAdmiringItem(piglin) && piglin.isAdult() && PiglinAi.isBarterCurrency(nearbyItems, piglin); // CraftBukkit
    }

    protected static void wasHurtBy(Piglin piglin, LivingEntity attacker) {
        if (!(attacker instanceof Piglin)) {
            if (PiglinAi.isHoldingItemInOffHand(piglin)) {
                PiglinAi.stopHoldingOffHandItem(piglin, false);
            }

            Brain<Piglin> behaviorcontroller = piglin.getBrain();

            behaviorcontroller.eraseMemory(MemoryModuleType.CELEBRATE_LOCATION);
            behaviorcontroller.eraseMemory(MemoryModuleType.DANCING);
            behaviorcontroller.eraseMemory(MemoryModuleType.ADMIRING_ITEM);
            if (attacker instanceof Player) {
                behaviorcontroller.setMemoryWithExpiry(MemoryModuleType.ADMIRING_DISABLED, true, 400L);
            }

            PiglinAi.getAvoidTarget(piglin).ifPresent((entityliving1) -> {
                if (entityliving1.getType() != attacker.getType()) {
                    behaviorcontroller.eraseMemory(MemoryModuleType.AVOID_TARGET);
                }

            });
            if (piglin.isBaby()) {
                behaviorcontroller.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, attacker, 100L);
                if (Sensor.isEntityAttackableIgnoringLineOfSight(piglin, attacker)) {
                    PiglinAi.broadcastAngerTarget(piglin, attacker);
                }

            } else if (attacker.getType() == EntityType.HOGLIN && PiglinAi.hoglinsOutnumberPiglins(piglin)) {
                PiglinAi.setAvoidTargetAndDontHuntForAWhile(piglin, attacker);
                PiglinAi.broadcastRetreat(piglin, attacker);
            } else {
                PiglinAi.maybeRetaliate(piglin, attacker);
            }
        }
    }

    protected static void maybeRetaliate(AbstractPiglin piglin, LivingEntity target) {
        if (!piglin.getBrain().isActive(Activity.AVOID)) {
            if (Sensor.isEntityAttackableIgnoringLineOfSight(piglin, target)) {
                if (!BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(piglin, target, 4.0D)) {
                    if (target.getType() == EntityType.PLAYER && piglin.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                        PiglinAi.setAngerTargetToNearestTargetablePlayerIfFound(piglin, target);
                        PiglinAi.broadcastUniversalAnger(piglin);
                    } else {
                        PiglinAi.setAngerTarget(piglin, target);
                        PiglinAi.broadcastAngerTarget(piglin, target);
                    }

                }
            }
        }
    }

    public static Optional<SoundEvent> getSoundForCurrentActivity(Piglin piglin) {
        return piglin.getBrain().getActiveNonCoreActivity().map((activity) -> {
            return PiglinAi.getSoundForActivity(piglin, activity);
        });
    }

    private static SoundEvent getSoundForActivity(Piglin piglin, Activity activity) {
        return activity == Activity.FIGHT ? SoundEvents.PIGLIN_ANGRY : (piglin.isConverting() ? SoundEvents.PIGLIN_RETREAT : (activity == Activity.AVOID && PiglinAi.isNearAvoidTarget(piglin) ? SoundEvents.PIGLIN_RETREAT : (activity == Activity.ADMIRE_ITEM ? SoundEvents.PIGLIN_ADMIRING_ITEM : (activity == Activity.CELEBRATE ? SoundEvents.PIGLIN_CELEBRATE : (PiglinAi.seesPlayerHoldingLovedItem(piglin) ? SoundEvents.PIGLIN_JEALOUS : (PiglinAi.isNearRepellent(piglin) ? SoundEvents.PIGLIN_RETREAT : SoundEvents.PIGLIN_AMBIENT))))));
    }

    private static boolean isNearAvoidTarget(Piglin piglin) {
        Brain<Piglin> behaviorcontroller = piglin.getBrain();

        return !behaviorcontroller.hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? false : ((LivingEntity) behaviorcontroller.getMemory(MemoryModuleType.AVOID_TARGET).get()).closerThan(piglin, 12.0D);
    }

    protected static boolean hasAnyoneNearbyHuntedRecently(Piglin piglin) {
        return piglin.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY) || PiglinAi.getVisibleAdultPiglins(piglin).stream().anyMatch((entitypiglinabstract) -> {
            return entitypiglinabstract.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY);
        });
    }

    private static List<AbstractPiglin> getVisibleAdultPiglins(Piglin piglin) {
        return (List) piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    private static List<AbstractPiglin> getAdultPiglins(AbstractPiglin piglin) {
        return (List) piglin.getBrain().getMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    public static boolean isWearingGold(LivingEntity entity) {
        Iterable<ItemStack> iterable = entity.getArmorSlots();
        Iterator iterator = iterable.iterator();

        Item item;

        do {
            if (!iterator.hasNext()) {
                return false;
            }

            ItemStack itemstack = (ItemStack) iterator.next();

            item = itemstack.getItem();
        } while (!(item instanceof ArmorItem) || ((ArmorItem) item).getMaterial() != ArmorMaterials.GOLD);

        return true;
    }

    private static void stopWalking(Piglin piglin) {
        piglin.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        piglin.getNavigation().stop();
    }

    private static RunSometimes<Piglin> babySometimesRideBabyHoglin() {
        return new RunSometimes<>(new CopyMemoryWithExpiry<>(Piglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.RIDE_TARGET, PiglinAi.RIDE_DURATION), PiglinAi.RIDE_START_INTERVAL);
    }

    protected static void broadcastAngerTarget(AbstractPiglin piglin, LivingEntity target) {
        PiglinAi.getAdultPiglins(piglin).forEach((entitypiglinabstract1) -> {
            if (target.getType() != EntityType.HOGLIN || entitypiglinabstract1.canHunt() && ((Hoglin) target).canBeHunted()) {
                PiglinAi.setAngerTargetIfCloserThanCurrent(entitypiglinabstract1, target);
            }
        });
    }

    protected static void broadcastUniversalAnger(AbstractPiglin piglin) {
        PiglinAi.getAdultPiglins(piglin).forEach((entitypiglinabstract1) -> {
            PiglinAi.getNearestVisibleTargetablePlayer(entitypiglinabstract1).ifPresent((entityhuman) -> {
                PiglinAi.setAngerTarget(entitypiglinabstract1, entityhuman);
            });
        });
    }

    protected static void broadcastDontKillAnyMoreHoglinsForAWhile(Piglin piglin) {
        PiglinAi.getVisibleAdultPiglins(piglin).forEach(PiglinAi::dontKillAnyMoreHoglinsForAWhile);
    }

    protected static void setAngerTarget(AbstractPiglin piglin, LivingEntity target) {
        if (Sensor.isEntityAttackableIgnoringLineOfSight(piglin, target)) {
            piglin.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 600L);
            if (target.getType() == EntityType.HOGLIN && piglin.canHunt()) {
                PiglinAi.dontKillAnyMoreHoglinsForAWhile(piglin);
            }

            if (target.getType() == EntityType.PLAYER && piglin.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, 600L);
            }

        }
    }

    private static void setAngerTargetToNearestTargetablePlayerIfFound(AbstractPiglin piglin, LivingEntity player) {
        Optional<Player> optional = PiglinAi.getNearestVisibleTargetablePlayer(piglin);

        if (optional.isPresent()) {
            PiglinAi.setAngerTarget(piglin, (LivingEntity) optional.get());
        } else {
            PiglinAi.setAngerTarget(piglin, player);
        }

    }

    private static void setAngerTargetIfCloserThanCurrent(AbstractPiglin piglin, LivingEntity target) {
        Optional<LivingEntity> optional = PiglinAi.getAngerTarget(piglin);
        LivingEntity entityliving1 = BehaviorUtils.getNearestTarget(piglin, optional, target);

        if (!optional.isPresent() || optional.get() != entityliving1) {
            PiglinAi.setAngerTarget(piglin, entityliving1);
        }
    }

    private static Optional<LivingEntity> getAngerTarget(AbstractPiglin piglin) {
        return BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
    }

    public static Optional<LivingEntity> getAvoidTarget(Piglin piglin) {
        return piglin.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? piglin.getBrain().getMemory(MemoryModuleType.AVOID_TARGET) : Optional.empty();
    }

    public static Optional<Player> getNearestVisibleTargetablePlayer(AbstractPiglin piglin) {
        return piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) ? piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) : Optional.empty();
    }

    private static void broadcastRetreat(Piglin piglin, LivingEntity target) {
        PiglinAi.getVisibleAdultPiglins(piglin).stream().filter((entitypiglinabstract) -> {
            return entitypiglinabstract instanceof Piglin;
        }).forEach((entitypiglinabstract) -> {
            PiglinAi.retreatFromNearestTarget((Piglin) entitypiglinabstract, target);
        });
    }

    private static void retreatFromNearestTarget(Piglin piglin, LivingEntity target) {
        Brain<Piglin> behaviorcontroller = piglin.getBrain();
        LivingEntity entityliving1 = BehaviorUtils.getNearestTarget(piglin, behaviorcontroller.getMemory(MemoryModuleType.AVOID_TARGET), target);

        entityliving1 = BehaviorUtils.getNearestTarget(piglin, behaviorcontroller.getMemory(MemoryModuleType.ATTACK_TARGET), entityliving1);
        PiglinAi.setAvoidTargetAndDontHuntForAWhile(piglin, entityliving1);
    }

    private static boolean wantsToStopFleeing(Piglin piglin) {
        Brain<Piglin> behaviorcontroller = piglin.getBrain();

        if (!behaviorcontroller.hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
            return true;
        } else {
            LivingEntity entityliving = (LivingEntity) behaviorcontroller.getMemory(MemoryModuleType.AVOID_TARGET).get();
            EntityType<?> entitytypes = entityliving.getType();

            return entitytypes == EntityType.HOGLIN ? PiglinAi.piglinsEqualOrOutnumberHoglins(piglin) : (PiglinAi.isZombified(entitytypes) ? !behaviorcontroller.isMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, entityliving) : false);
        }
    }

    private static boolean piglinsEqualOrOutnumberHoglins(Piglin piglin) {
        return !PiglinAi.hoglinsOutnumberPiglins(piglin);
    }

    private static boolean hoglinsOutnumberPiglins(Piglin piglins) {
        int i = (Integer) piglins.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
        int j = (Integer) piglins.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);

        return j > i;
    }

    private static void setAvoidTargetAndDontHuntForAWhile(Piglin piglin, LivingEntity target) {
        piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        piglin.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, target, (long) PiglinAi.RETREAT_DURATION.sample(piglin.level.random));
        PiglinAi.dontKillAnyMoreHoglinsForAWhile(piglin);
    }

    protected static void dontKillAnyMoreHoglinsForAWhile(AbstractPiglin piglin) {
        piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, (long) PiglinAi.TIME_BETWEEN_HUNTS.sample(piglin.level.random));
    }

    private static boolean seesPlayerHoldingWantedItem(Piglin piglin) {
        return piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    private static void eat(Piglin piglin) {
        piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ATE_RECENTLY, true, 200L);
    }

    private static Vec3 getRandomNearbyPos(Piglin piglin) {
        Vec3 vec3d = LandRandomPos.getPos(piglin, 4, 2);

        return vec3d == null ? piglin.position() : vec3d;
    }

    private static boolean hasEatenRecently(Piglin piglin) {
        return piglin.getBrain().hasMemoryValue(MemoryModuleType.ATE_RECENTLY);
    }

    protected static boolean isIdle(AbstractPiglin piglin) {
        return piglin.getBrain().isActive(Activity.IDLE);
    }

    private static boolean hasCrossbow(LivingEntity piglin) {
        return piglin.isHolding(Items.CROSSBOW);
    }

    private static void admireGoldItem(LivingEntity entity) {
        entity.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 120L);
    }

    private static boolean isAdmiringItem(Piglin entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM);
    }

    // CraftBukkit start - Changes to allow custom payment for bartering
    private static boolean isBarterCurrency(ItemStack itemstack, Piglin piglin) {
        return PiglinAi.isBarterCurrency(itemstack) || piglin.allowedBarterItems.contains(itemstack.getItem());
    }
    // CraftBukkit end

    private static boolean isBarterCurrency(ItemStack stack) {
        return stack.is(PiglinAi.BARTERING_ITEM);
    }

    private static boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.PIGLIN_FOOD);
    }

    private static boolean isNearRepellent(Piglin piglin) {
        return piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean seesPlayerHoldingLovedItem(LivingEntity entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    private static boolean doesntSeeAnyPlayerHoldingLovedItem(LivingEntity piglin) {
        return !PiglinAi.seesPlayerHoldingLovedItem(piglin);
    }

    public static boolean isPlayerHoldingLovedItem(LivingEntity target) {
        return target.getType() == EntityType.PLAYER && target.isHolding(PiglinAi::isLovedItem);
    }

    private static boolean isAdmiringDisabled(Piglin piglin) {
        return piglin.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_DISABLED);
    }

    private static boolean wasHurtRecently(LivingEntity piglin) {
        return piglin.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }

    private static boolean isHoldingItemInOffHand(Piglin piglin) {
        return !piglin.getOffhandItem().isEmpty();
    }

    private static boolean isNotHoldingLovedItemInOffHand(Piglin piglin) {
        return piglin.getOffhandItem().isEmpty() || !PiglinAi.isLovedItem(piglin.getOffhandItem(), piglin); // CraftBukkit - Changes to allow custom payment for bartering
    }

    public static boolean isZombified(EntityType<?> entityType) {
        return entityType == EntityType.ZOMBIFIED_PIGLIN || entityType == EntityType.ZOGLIN;
    }
}
