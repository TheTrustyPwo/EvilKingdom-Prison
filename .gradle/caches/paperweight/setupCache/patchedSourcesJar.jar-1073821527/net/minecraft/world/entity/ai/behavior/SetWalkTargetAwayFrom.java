package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetAwayFrom<T> extends Behavior<PathfinderMob> {
    private final MemoryModuleType<T> walkAwayFromMemory;
    private final float speedModifier;
    private final int desiredDistance;
    private final Function<T, Vec3> toPosition;

    public SetWalkTargetAwayFrom(MemoryModuleType<T> memoryType, float speed, int range, boolean requiresWalkTarget, Function<T, Vec3> posRetriever) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, requiresWalkTarget ? MemoryStatus.REGISTERED : MemoryStatus.VALUE_ABSENT, memoryType, MemoryStatus.VALUE_PRESENT));
        this.walkAwayFromMemory = memoryType;
        this.speedModifier = speed;
        this.desiredDistance = range;
        this.toPosition = posRetriever;
    }

    public static SetWalkTargetAwayFrom<BlockPos> pos(MemoryModuleType<BlockPos> memoryType, float speed, int range, boolean requiresWalkTarget) {
        return new SetWalkTargetAwayFrom<>(memoryType, speed, range, requiresWalkTarget, Vec3::atBottomCenterOf);
    }

    public static SetWalkTargetAwayFrom<? extends Entity> entity(MemoryModuleType<? extends Entity> memoryType, float speed, int range, boolean requiresWalkTarget) {
        return new SetWalkTargetAwayFrom<>(memoryType, speed, range, requiresWalkTarget, Entity::position);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, PathfinderMob entity) {
        return this.alreadyWalkingAwayFromPosWithSameSpeed(entity) ? false : entity.position().closerThan(this.getPosToAvoid(entity), (double)this.desiredDistance);
    }

    private Vec3 getPosToAvoid(PathfinderMob entity) {
        return this.toPosition.apply(entity.getBrain().getMemory(this.walkAwayFromMemory).get());
    }

    private boolean alreadyWalkingAwayFromPosWithSameSpeed(PathfinderMob entity) {
        if (!entity.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            return false;
        } else {
            WalkTarget walkTarget = entity.getBrain().getMemory(MemoryModuleType.WALK_TARGET).get();
            if (walkTarget.getSpeedModifier() != this.speedModifier) {
                return false;
            } else {
                Vec3 vec3 = walkTarget.getTarget().currentPosition().subtract(entity.position());
                Vec3 vec32 = this.getPosToAvoid(entity).subtract(entity.position());
                return vec3.dot(vec32) < 0.0D;
            }
        }
    }

    @Override
    protected void start(ServerLevel world, PathfinderMob entity, long time) {
        moveAwayFrom(entity, this.getPosToAvoid(entity), this.speedModifier);
    }

    private static void moveAwayFrom(PathfinderMob entity, Vec3 pos, float speed) {
        for(int i = 0; i < 10; ++i) {
            Vec3 vec3 = LandRandomPos.getPosAway(entity, 16, 7, pos);
            if (vec3 != null) {
                entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, speed, 0));
                return;
            }
        }

    }
}
