package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface EntityGetter {

    // Paper start
    List<Entity> getHardCollidingEntities(Entity except, AABB box, Predicate<? super Entity> predicate);

    void getEntities(Entity except, AABB box, Predicate<? super Entity> predicate, List<Entity> into);

    void getHardCollidingEntities(Entity except, AABB box, Predicate<? super Entity> predicate, List<Entity> into);

    <T> void getEntitiesByClass(Class<? extends T> clazz, Entity except, final AABB box, List<? super T> into,
                                Predicate<? super T> predicate);
    // Paper end

    List<Entity> getEntities(@Nullable Entity except, AABB box, Predicate<? super Entity> predicate);

    <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AABB box, Predicate<? super T> predicate);

    default <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AABB box, Predicate<? super T> predicate) {
        return this.getEntities(EntityTypeTest.forClass(entityClass), box, predicate);
    }

    List<? extends Player> players();

    default List<Entity> getEntities(@Nullable Entity except, AABB box) {
        return this.getEntities(except, box, EntitySelector.NO_SPECTATORS);
    }

    default boolean isUnobstructed(@Nullable Entity except, VoxelShape shape) {
        if (shape.isEmpty()) {
            return true;
        } else {
            for(Entity entity : this.getEntities(except, shape.bounds())) {
                if (!entity.isRemoved() && entity.blocksBuilding && (except == null || !entity.isPassengerOfSameVehicle(except)) && shape.intersects(entity.getBoundingBox())) { // Paper
                    return false;
                }
            }

            return true;
        }
    }

    default <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AABB box) {
        return this.getEntitiesOfClass(entityClass, box, EntitySelector.NO_SPECTATORS);
    }

    default List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB box) {
        if (box.getSize() < 1.0E-7D) {
            return List.of();
        } else {
            Predicate<Entity> predicate = entity == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(entity::canCollideWith);
            List<Entity> list = this.getEntities(entity, box.inflate(-1.0E-7D), predicate); // Paper - needs to be negated, or else we get things we don't collide with
            if (list.isEmpty()) {
                return List.of();
            } else {
                Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size());

                for(Entity entity2 : list) {
                    builder.add(Shapes.create(entity2.getBoundingBox()));
                }

                return builder.build();
            }
        }
    }

    default Player findNearbyPlayer(Entity entity, double d0, @Nullable Predicate<Entity> predicate) { return this.getNearestPlayer(entity.getX(), entity.getY(), entity.getZ(), d0, predicate); } // Paper
    @Nullable default Player getNearestPlayer(double x, double y, double z, double maxDistance, @Nullable Predicate<Entity> targetPredicate) { // Paper
        double d = -1.0D;
        Player player = null;

        for(Player player2 : this.players()) {
            if (targetPredicate == null || targetPredicate.test(player2)) {
                double e = player2.distanceToSqr(x, y, z);
                if ((maxDistance < 0.0D || e < maxDistance * maxDistance) && (d == -1.0D || e < d)) {
                    d = e;
                    player = player2;
                }
            }
        }

        return player;
    }

    // Paper start
    default List<org.bukkit.entity.HumanEntity> findNearbyBukkitPlayers(double x, double y, double z, double radius, boolean notSpectator) {
        return findNearbyBukkitPlayers(x, y, z, radius, notSpectator ? EntitySelector.NO_SPECTATORS : net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR);
    }

    default List<org.bukkit.entity.HumanEntity> findNearbyBukkitPlayers(double x, double y, double z, double radius, @Nullable Predicate<Entity> predicate) {
        com.google.common.collect.ImmutableList.Builder<org.bukkit.entity.HumanEntity> builder = com.google.common.collect.ImmutableList.builder();

        for (Player human : this.players()) {
            if (predicate == null || predicate.test(human)) {
                double distanceSquared = human.distanceToSqr(x, y, z);

                if (radius < 0.0D || distanceSquared < radius * radius) {
                    builder.add(human.getBukkitEntity());
                }
            }
        }

        return builder.build();
    }
    // Paper end

    @Nullable
    default Player getNearestPlayer(Entity entity, double maxDistance) {
        return this.getNearestPlayer(entity.getX(), entity.getY(), entity.getZ(), maxDistance, false);
    }

    @Nullable
    default Player getNearestPlayer(double x, double y, double z, double maxDistance, boolean ignoreCreative) {
        Predicate<Entity> predicate = ignoreCreative ? EntitySelector.NO_CREATIVE_OR_SPECTATOR : EntitySelector.NO_SPECTATORS;
        return this.getNearestPlayer(x, y, z, maxDistance, predicate);
    }

    // Paper end
    default boolean isAffectsSpawningPlayerNearby(double d0, double d1, double d2, double d3) {
        java.util.Iterator iterator = this.players().iterator();
        double d4;
        do {
            Player entityhuman;
            do {
                if (!iterator.hasNext()) {
                    return false;
                }

                entityhuman = (Player) iterator.next();
            } while (!EntitySelector.affectsSpawning.test(entityhuman));

            d4 = entityhuman.distanceToSqr(d0, d1, d2);
        } while (d3 >= 0.0D && d4 >= d3 * d3);

        return true;
    }
    // Paper end

    default boolean hasNearbyAlivePlayer(double x, double y, double z, double range) {
        for(Player player : this.players()) {
            if (EntitySelector.NO_SPECTATORS.test(player) && EntitySelector.LIVING_ENTITY_STILL_ALIVE.test(player)) {
                double d = player.distanceToSqr(x, y, z);
                if (range < 0.0D || d < range * range) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    default Player getNearestPlayer(TargetingConditions targetPredicate, LivingEntity entity) {
        return this.getNearestEntity(this.players(), targetPredicate, entity, entity.getX(), entity.getY(), entity.getZ());
    }

    @Nullable
    default Player getNearestPlayer(TargetingConditions targetPredicate, LivingEntity entity, double x, double y, double z) {
        return this.getNearestEntity(this.players(), targetPredicate, entity, x, y, z);
    }

    @Nullable
    default Player getNearestPlayer(TargetingConditions targetPredicate, double x, double y, double z) {
        return this.getNearestEntity(this.players(), targetPredicate, (LivingEntity)null, x, y, z);
    }

    @Nullable
    default <T extends LivingEntity> T getNearestEntity(Class<? extends T> entityClass, TargetingConditions targetPredicate, @Nullable LivingEntity entity, double x, double y, double z, AABB box) {
        return this.getNearestEntity(this.getEntitiesOfClass(entityClass, box, (livingEntity) -> {
            return true;
        }), targetPredicate, entity, x, y, z);
    }

    @Nullable
    default <T extends LivingEntity> T getNearestEntity(List<? extends T> entityList, TargetingConditions targetPredicate, @Nullable LivingEntity entity, double x, double y, double z) {
        double d = -1.0D;
        T livingEntity = null;

        for(T livingEntity2 : entityList) {
            if (targetPredicate.test(entity, livingEntity2)) {
                double e = livingEntity2.distanceToSqr(x, y, z);
                if (d == -1.0D || e < d) {
                    d = e;
                    livingEntity = livingEntity2;
                }
            }
        }

        return livingEntity;
    }

    default List<Player> getNearbyPlayers(TargetingConditions targetPredicate, LivingEntity entity, AABB box) {
        List<Player> list = Lists.newArrayList();

        for(Player player : this.players()) {
            if (box.contains(player.getX(), player.getY(), player.getZ()) && targetPredicate.test(entity, player)) {
                list.add(player);
            }
        }

        return list;
    }

    default <T extends LivingEntity> List<T> getNearbyEntities(Class<T> entityClass, TargetingConditions targetPredicate, LivingEntity targetingEntity, AABB box) {
        List<T> list = this.getEntitiesOfClass(entityClass, box, (livingEntityx) -> {
            return true;
        });
        List<T> list2 = Lists.newArrayList();

        for(T livingEntity : list) {
            if (targetPredicate.test(targetingEntity, livingEntity)) {
                list2.add(livingEntity);
            }
        }

        return list2;
    }

    @Nullable
    default Player getPlayerByUUID(UUID uuid) {
        for(int i = 0; i < this.players().size(); ++i) {
            Player player = this.players().get(i);
            if (uuid.equals(player.getUUID())) {
                return player;
            }
        }

        return null;
    }
}
