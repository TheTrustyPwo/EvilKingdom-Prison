package net.minecraft.world.entity;

import com.google.common.base.Predicates;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;

public final class EntitySelector {
    public static final Predicate<Entity> ENTITY_STILL_ALIVE = Entity::isAlive;
    public static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE = (entity) -> {
        return entity.isAlive() && entity instanceof LivingEntity;
    };
    public static final Predicate<Entity> ENTITY_NOT_BEING_RIDDEN = (entity) -> {
        return entity.isAlive() && !entity.isVehicle() && !entity.isPassenger();
    };
    public static final Predicate<Entity> CONTAINER_ENTITY_SELECTOR = (entity) -> {
        return entity instanceof Container && entity.isAlive();
    };
    public static final Predicate<Entity> NO_CREATIVE_OR_SPECTATOR = (entity) -> {
        return !(entity instanceof Player) || !entity.isSpectator() && !((Player)entity).isCreative();
    };
    public static final Predicate<Entity> NO_SPECTATORS = (entity) -> {
        return !entity.isSpectator();
    };
    public static final Predicate<Entity> CAN_BE_COLLIDED_WITH = NO_SPECTATORS.and(Entity::canBeCollidedWith);

    private EntitySelector() {
    }

    public static Predicate<Entity> withinDistance(double x, double y, double z, double max) {
        double d = max * max;
        return (entity) -> {
            return entity != null && entity.distanceToSqr(x, y, z) <= d;
        };
    }

    public static Predicate<Entity> pushableBy(Entity entity) {
        Team team = entity.getTeam();
        Team.CollisionRule collisionRule = team == null ? Team.CollisionRule.ALWAYS : team.getCollisionRule();
        return (Predicate<Entity>)(collisionRule == Team.CollisionRule.NEVER ? Predicates.alwaysFalse() : NO_SPECTATORS.and((entityx) -> {
            if (!entityx.isPushable()) {
                return false;
            } else if (!entity.level.isClientSide || entityx instanceof Player && ((Player)entityx).isLocalPlayer()) {
                Team team2 = entityx.getTeam();
                Team.CollisionRule collisionRule2 = team2 == null ? Team.CollisionRule.ALWAYS : team2.getCollisionRule();
                if (collisionRule2 == Team.CollisionRule.NEVER) {
                    return false;
                } else {
                    boolean bl = team != null && team.isAlliedTo(team2);
                    if ((collisionRule == Team.CollisionRule.PUSH_OWN_TEAM || collisionRule2 == Team.CollisionRule.PUSH_OWN_TEAM) && bl) {
                        return false;
                    } else {
                        return collisionRule != Team.CollisionRule.PUSH_OTHER_TEAMS && collisionRule2 != Team.CollisionRule.PUSH_OTHER_TEAMS || bl;
                    }
                }
            } else {
                return false;
            }
        }));
    }

    public static Predicate<Entity> notRiding(Entity entity) {
        return (entity2) -> {
            while(true) {
                if (entity2.isPassenger()) {
                    entity2 = entity2.getVehicle();
                    if (entity2 != entity) {
                        continue;
                    }

                    return false;
                }

                return true;
            }
        };
    }

    public static class MobCanWearArmorEntitySelector implements Predicate<Entity> {
        private final ItemStack itemStack;

        public MobCanWearArmorEntitySelector(ItemStack stack) {
            this.itemStack = stack;
        }

        @Override
        public boolean test(@Nullable Entity entity) {
            if (!entity.isAlive()) {
                return false;
            } else if (!(entity instanceof LivingEntity)) {
                return false;
            } else {
                LivingEntity livingEntity = (LivingEntity)entity;
                return livingEntity.canTakeItem(this.itemStack);
            }
        }
    }
}
