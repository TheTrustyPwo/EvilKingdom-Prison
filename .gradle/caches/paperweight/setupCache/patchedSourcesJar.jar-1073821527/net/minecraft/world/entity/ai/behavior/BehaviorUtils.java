package net.minecraft.world.entity.ai.behavior;

import java.util.Comparator;
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
    private BehaviorUtils() {
    }

    public static void lockGazeAndWalkToEachOther(LivingEntity first, LivingEntity second, float speed) {
        lookAtEachOther(first, second);
        setWalkAndLookTargetMemoriesToEachOther(first, second, speed);
    }

    public static boolean entityIsVisible(Brain<?> brain, LivingEntity target) {
        Optional<NearestVisibleLivingEntities> optional = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        return optional.isPresent() && optional.get().contains(target);
    }

    public static boolean targetIsValid(Brain<?> brain, MemoryModuleType<? extends LivingEntity> memoryModuleType, EntityType<?> entityType) {
        return targetIsValid(brain, memoryModuleType, (entity) -> {
            return entity.getType() == entityType;
        });
    }

    private static boolean targetIsValid(Brain<?> brain, MemoryModuleType<? extends LivingEntity> memoryType, Predicate<LivingEntity> filter) {
        return brain.getMemory(memoryType).filter(filter).filter(LivingEntity::isAlive).filter((target) -> {
            return entityIsVisible(brain, target);
        }).isPresent();
    }

    private static void lookAtEachOther(LivingEntity first, LivingEntity second) {
        lookAtEntity(first, second);
        lookAtEntity(second, first);
    }

    public static void lookAtEntity(LivingEntity entity, LivingEntity target) {
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
    }

    private static void setWalkAndLookTargetMemoriesToEachOther(LivingEntity first, LivingEntity second, float speed) {
        int i = 2;
        setWalkAndLookTargetMemories(first, second, speed, 2);
        setWalkAndLookTargetMemories(second, first, speed, 2);
    }

    public static void setWalkAndLookTargetMemories(LivingEntity entity, Entity target, float speed, int completionRange) {
        WalkTarget walkTarget = new WalkTarget(new EntityTracker(target, false), speed, completionRange);
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    public static void setWalkAndLookTargetMemories(LivingEntity entity, BlockPos target, float speed, int completionRange) {
        WalkTarget walkTarget = new WalkTarget(new BlockPosTracker(target), speed, completionRange);
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target));
        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    public static void throwItem(LivingEntity entity, ItemStack stack, Vec3 targetLocation) {
        double d = entity.getEyeY() - (double)0.3F;
        ItemEntity itemEntity = new ItemEntity(entity.level, entity.getX(), d, entity.getZ(), stack);
        float f = 0.3F;
        Vec3 vec3 = targetLocation.subtract(entity.position());
        vec3 = vec3.normalize().scale((double)0.3F);
        itemEntity.setDeltaMovement(vec3);
        itemEntity.setDefaultPickUpDelay();
        entity.level.addFreshEntity(itemEntity);
    }

    public static SectionPos findSectionClosestToVillage(ServerLevel world, SectionPos center, int radius) {
        int i = world.sectionsToVillage(center);
        return SectionPos.cube(center, radius).filter((sectionPos) -> {
            return world.sectionsToVillage(sectionPos) < i;
        }).min(Comparator.comparingInt(world::sectionsToVillage)).orElse(center);
    }

    public static boolean isWithinAttackRange(Mob mob, LivingEntity target, int rangedWeaponReachReduction) {
        Item item = mob.getMainHandItem().getItem();
        if (item instanceof ProjectileWeaponItem) {
            ProjectileWeaponItem projectileWeaponItem = (ProjectileWeaponItem)item;
            if (mob.canFireProjectileWeapon((ProjectileWeaponItem)item)) {
                int i = projectileWeaponItem.getDefaultProjectileRange() - rangedWeaponReachReduction;
                return mob.closerThan(target, (double)i);
            }
        }

        return isWithinMeleeAttackRange(mob, target);
    }

    public static boolean isWithinMeleeAttackRange(Mob source, LivingEntity target) {
        double d = source.distanceToSqr(target.getX(), target.getY(), target.getZ());
        return d <= source.getMeleeAttackRangeSqr(target);
    }

    public static boolean isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(LivingEntity source, LivingEntity target, double extraDistance) {
        Optional<LivingEntity> optional = source.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (optional.isEmpty()) {
            return false;
        } else {
            double d = source.distanceToSqr(optional.get().position());
            double e = source.distanceToSqr(target.position());
            return e > d + extraDistance * extraDistance;
        }
    }

    public static boolean canSee(LivingEntity source, LivingEntity target) {
        Brain<?> brain = source.getBrain();
        return !brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES) ? false : brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().contains(target);
    }

    public static LivingEntity getNearestTarget(LivingEntity source, Optional<LivingEntity> first, LivingEntity second) {
        return first.isEmpty() ? second : getTargetNearestMe(source, first.get(), second);
    }

    public static LivingEntity getTargetNearestMe(LivingEntity source, LivingEntity first, LivingEntity second) {
        Vec3 vec3 = first.position();
        Vec3 vec32 = second.position();
        return source.distanceToSqr(vec3) < source.distanceToSqr(vec32) ? first : second;
    }

    public static Optional<LivingEntity> getLivingEntityFromUUIDMemory(LivingEntity entity, MemoryModuleType<UUID> uuidMemoryModule) {
        Optional<UUID> optional = entity.getBrain().getMemory(uuidMemoryModule);
        return optional.map((uuid) -> {
            return ((ServerLevel)entity.level).getEntity(uuid);
        }).map((target) -> {
            LivingEntity var10000;
            if (target instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)target;
                var10000 = livingEntity;
            } else {
                var10000 = null;
            }

            return var10000;
        });
    }

    public static Stream<Villager> getNearbyVillagersWithCondition(Villager villager, Predicate<Villager> filter) {
        return villager.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).map((list) -> {
            return list.stream().filter((entity) -> {
                return entity instanceof Villager && entity != villager;
            }).map((livingEntity) -> {
                return (Villager)livingEntity;
            }).filter(LivingEntity::isAlive).filter(filter);
        }).orElseGet(Stream::empty);
    }

    @Nullable
    public static Vec3 getRandomSwimmablePos(PathfinderMob entity, int horizontalRange, int verticalRange) {
        Vec3 vec3 = DefaultRandomPos.getPos(entity, horizontalRange, verticalRange);

        for(int i = 0; vec3 != null && !entity.level.getBlockState(new BlockPos(vec3)).isPathfindable(entity.level, new BlockPos(vec3), PathComputationType.WATER) && i++ < 10; vec3 = DefaultRandomPos.getPos(entity, horizontalRange, verticalRange)) {
        }

        return vec3;
    }
}
