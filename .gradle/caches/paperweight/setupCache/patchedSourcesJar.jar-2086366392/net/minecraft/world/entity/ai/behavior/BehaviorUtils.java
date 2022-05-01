package net.minecraft.world.entity.ai.behavior;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class BehaviorUtils {

    private BehaviorUtils() {}

    public static void lockGazeAndWalkToEachOther(LivingEntity first, LivingEntity second, float speed) {
        BehaviorUtils.lookAtEachOther(first, second);
        BehaviorUtils.setWalkAndLookTargetMemoriesToEachOther(first, second, speed);
    }

    public static boolean entityIsVisible(Brain<?> brain, LivingEntity target) {
        Optional<NearestVisibleLivingEntities> optional = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);

        return optional.isPresent() && ((NearestVisibleLivingEntities) optional.get()).contains(target);
    }

    public static boolean targetIsValid(Brain<?> brain, MemoryModuleType<? extends LivingEntity> memoryModuleType, EntityType<?> entityType) {
        return BehaviorUtils.targetIsValid(brain, memoryModuleType, (entityliving) -> {
            return entityliving.getType() == entityType;
        });
    }

    private static boolean targetIsValid(Brain<?> brain, MemoryModuleType<? extends LivingEntity> memoryType, Predicate<LivingEntity> filter) {
        return brain.getMemory(memoryType).filter(filter).filter(LivingEntity::isAlive).filter((entityliving) -> {
            return BehaviorUtils.entityIsVisible(brain, entityliving);
        }).isPresent();
    }

    private static void lookAtEachOther(LivingEntity first, LivingEntity second) {
        BehaviorUtils.lookAtEntity(first, second);
        BehaviorUtils.lookAtEntity(second, first);
    }

    public static void lookAtEntity(LivingEntity entity, LivingEntity target) {
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, (new EntityTracker(target, true))); // CraftBukkit - decompile error
    }

    private static void setWalkAndLookTargetMemoriesToEachOther(LivingEntity first, LivingEntity second, float speed) {
        boolean flag = true;

        BehaviorUtils.setWalkAndLookTargetMemories(first, (Entity) second, speed, 2);
        BehaviorUtils.setWalkAndLookTargetMemories(second, (Entity) first, speed, 2);
    }

    public static void setWalkAndLookTargetMemories(LivingEntity entity, Entity target, float speed, int completionRange) {
        WalkTarget memorytarget = new WalkTarget(new EntityTracker(target, false), speed, completionRange);

        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, (new EntityTracker(target, true))); // CraftBukkit - decompile error
        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, memorytarget); // CraftBukkit - decompile error
    }

    public static void setWalkAndLookTargetMemories(LivingEntity entity, BlockPos target, float speed, int completionRange) {
        WalkTarget memorytarget = new WalkTarget(new BlockPosTracker(target), speed, completionRange);

        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, (new BlockPosTracker(target))); // CraftBukkit - decompile error
        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, memorytarget); // CraftBukkit - decompile error
    }

    public static void throwItem(LivingEntity entity, ItemStack stack, Vec3 targetLocation) {
        if (stack.isEmpty()) return; // CraftBukkit - SPIGOT-4940: no empty loot
        double d0 = entity.getEyeY() - 0.30000001192092896D;
        ItemEntity entityitem = new ItemEntity(entity.level, entity.getX(), d0, entity.getZ(), stack);
        float f = 0.3F;
        Vec3 vec3d1 = targetLocation.subtract(entity.position());

        vec3d1 = vec3d1.normalize().scale(0.30000001192092896D);
        entityitem.setDeltaMovement(vec3d1);
        entityitem.setDefaultPickUpDelay();
        // CraftBukkit start
        org.bukkit.event.entity.EntityDropItemEvent event = new org.bukkit.event.entity.EntityDropItemEvent(entity.getBukkitEntity(), (org.bukkit.entity.Item) entityitem.getBukkitEntity());
        entityitem.level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        // CraftBukkit end
        entity.level.addFreshEntity(entityitem);
    }

    public static SectionPos findSectionClosestToVillage(ServerLevel world, SectionPos center, int radius) {
        int j = world.sectionsToVillage(center);
        Stream<SectionPos> stream = SectionPos.cube(center, radius).filter((sectionposition1) -> { // CraftBukkit - decompile error
            return world.sectionsToVillage(sectionposition1) < j;
        });

        Objects.requireNonNull(world);
        return (SectionPos) stream.min(Comparator.comparingInt(world::sectionsToVillage)).orElse(center);
    }

    public static boolean isWithinAttackRange(Mob mob, LivingEntity target, int rangedWeaponReachReduction) {
        Item item = mob.getMainHandItem().getItem();

        if (item instanceof ProjectileWeaponItem) {
            ProjectileWeaponItem itemprojectileweapon = (ProjectileWeaponItem) item;

            if (mob.canFireProjectileWeapon((ProjectileWeaponItem) item)) {
                int j = itemprojectileweapon.getDefaultProjectileRange() - rangedWeaponReachReduction;

                return mob.closerThan(target, (double) j);
            }
        }

        return BehaviorUtils.isWithinMeleeAttackRange(mob, target);
    }

    public static boolean isWithinMeleeAttackRange(Mob source, LivingEntity target) {
        double d0 = source.distanceToSqr(target.getX(), target.getY(), target.getZ());

        return d0 <= source.getMeleeAttackRangeSqr(target);
    }

    public static boolean isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(LivingEntity source, LivingEntity target, double extraDistance) {
        Optional<LivingEntity> optional = source.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);

        if (optional.isEmpty()) {
            return false;
        } else {
            double d1 = source.distanceToSqr(((LivingEntity) optional.get()).position());
            double d2 = source.distanceToSqr(target.position());

            return d2 > d1 + extraDistance * extraDistance;
        }
    }

    public static boolean canSee(LivingEntity source, LivingEntity target) {
        Brain<?> behaviorcontroller = source.getBrain();

        return !behaviorcontroller.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES) ? false : ((NearestVisibleLivingEntities) behaviorcontroller.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get()).contains(target);
    }

    public static LivingEntity getNearestTarget(LivingEntity source, Optional<LivingEntity> first, LivingEntity second) {
        return first.isEmpty() ? second : BehaviorUtils.getTargetNearestMe(source, (LivingEntity) first.get(), second);
    }

    public static LivingEntity getTargetNearestMe(LivingEntity source, LivingEntity first, LivingEntity second) {
        Vec3 vec3d = first.position();
        Vec3 vec3d1 = second.position();

        return source.distanceToSqr(vec3d) < source.distanceToSqr(vec3d1) ? first : second;
    }

    public static Optional<LivingEntity> getLivingEntityFromUUIDMemory(LivingEntity entity, MemoryModuleType<UUID> uuidMemoryModule) {
        Optional<UUID> optional = entity.getBrain().getMemory(uuidMemoryModule);

        return optional.map((uuid) -> {
            return ((ServerLevel) entity.level).getEntity(uuid);
        }).map((entity1) -> { // Paper - remap fix
            LivingEntity entityliving1;

            if (entity1 instanceof LivingEntity) { // Paper - remap fix
                LivingEntity entityliving2 = (LivingEntity) entity1; // Paper - remap fix

                entityliving1 = entityliving2;
            } else {
                entityliving1 = null;
            }

            return entityliving1;
        });
    }

    public static Stream<Villager> getNearbyVillagersWithCondition(Villager villager, Predicate<Villager> filter) {
        return (Stream) villager.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).map((list) -> {
            return list.stream().filter((entityliving) -> {
                return entityliving instanceof Villager && entityliving != villager;
            }).map((entityliving) -> {
                return (Villager) entityliving;
            }).filter(LivingEntity::isAlive).filter(filter);
        }).orElseGet(Stream::empty);
    }

    @Nullable
    public static Vec3 getRandomSwimmablePos(PathfinderMob entity, int horizontalRange, int verticalRange) {
        Vec3 vec3d = DefaultRandomPos.getPos(entity, horizontalRange, verticalRange);

        for (int k = 0; vec3d != null && !entity.level.getBlockState(new BlockPos(vec3d)).isPathfindable(entity.level, new BlockPos(vec3d), PathComputationType.WATER) && k++ < 10; vec3d = DefaultRandomPos.getPos(entity, horizontalRange, verticalRange)) {
            ;
        }

        return vec3d;
    }
}
