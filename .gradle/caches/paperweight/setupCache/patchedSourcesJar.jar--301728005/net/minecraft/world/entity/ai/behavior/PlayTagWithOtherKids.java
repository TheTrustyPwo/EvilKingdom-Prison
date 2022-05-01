package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class PlayTagWithOtherKids extends Behavior<PathfinderMob> {
    private static final int MAX_FLEE_XZ_DIST = 20;
    private static final int MAX_FLEE_Y_DIST = 8;
    private static final float FLEE_SPEED_MODIFIER = 0.6F;
    private static final float CHASE_SPEED_MODIFIER = 0.6F;
    private static final int MAX_CHASERS_PER_TARGET = 5;
    private static final int AVERAGE_WAIT_TIME_BETWEEN_RUNS = 10;

    public PlayTagWithOtherKids() {
        super(ImmutableMap.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, PathfinderMob entity) {
        return world.getRandom().nextInt(10) == 0 && this.hasFriendsNearby(entity);
    }

    @Override
    protected void start(ServerLevel world, PathfinderMob entity, long time) {
        LivingEntity livingEntity = this.seeIfSomeoneIsChasingMe(entity);
        if (livingEntity != null) {
            this.fleeFromChaser(world, entity, livingEntity);
        } else {
            Optional<LivingEntity> optional = this.findSomeoneBeingChased(entity);
            if (optional.isPresent()) {
                chaseKid(entity, optional.get());
            } else {
                this.findSomeoneToChase(entity).ifPresent((target) -> {
                    chaseKid(entity, target);
                });
            }
        }
    }

    private void fleeFromChaser(ServerLevel world, PathfinderMob entity, LivingEntity unusedBaby) {
        for(int i = 0; i < 10; ++i) {
            Vec3 vec3 = LandRandomPos.getPos(entity, 20, 8);
            if (vec3 != null && world.isVillage(new BlockPos(vec3))) {
                entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, 0.6F, 0));
                return;
            }
        }

    }

    private static void chaseKid(PathfinderMob entity, LivingEntity target) {
        Brain<?> brain = entity.getBrain();
        brain.setMemory(MemoryModuleType.INTERACTION_TARGET, target);
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(target, false), 0.6F, 1));
    }

    private Optional<LivingEntity> findSomeoneToChase(PathfinderMob entity) {
        return this.getFriendsNearby(entity).stream().findAny();
    }

    private Optional<LivingEntity> findSomeoneBeingChased(PathfinderMob entity) {
        Map<LivingEntity, Integer> map = this.checkHowManyChasersEachFriendHas(entity);
        return map.entrySet().stream().sorted(Comparator.comparingInt(Entry::getValue)).filter((entry) -> {
            return entry.getValue() > 0 && entry.getValue() <= 5;
        }).map(Entry::getKey).findFirst();
    }

    private Map<LivingEntity, Integer> checkHowManyChasersEachFriendHas(PathfinderMob entity) {
        Map<LivingEntity, Integer> map = Maps.newHashMap();
        this.getFriendsNearby(entity).stream().filter(this::isChasingSomeone).forEach((livingEntity) -> {
            map.compute(this.whoAreYouChasing(livingEntity), (livingEntityx, integer) -> {
                return integer == null ? 1 : integer + 1;
            });
        });
        return map;
    }

    private List<LivingEntity> getFriendsNearby(PathfinderMob entity) {
        return entity.getBrain().getMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES).get();
    }

    private LivingEntity whoAreYouChasing(LivingEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
    }

    @Nullable
    private LivingEntity seeIfSomeoneIsChasingMe(LivingEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES).get().stream().filter((livingEntity2) -> {
            return this.isFriendChasingMe(entity, livingEntity2);
        }).findAny().orElse((LivingEntity)null);
    }

    private boolean isChasingSomeone(LivingEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
    }

    private boolean isFriendChasingMe(LivingEntity entity, LivingEntity other) {
        return other.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).filter((livingEntity2) -> {
            return livingEntity2 == entity;
        }).isPresent();
    }

    private boolean hasFriendsNearby(PathfinderMob entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.VISIBLE_VILLAGER_BABIES);
    }
}
