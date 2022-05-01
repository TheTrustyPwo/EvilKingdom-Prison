package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class RandomStroll extends Behavior<PathfinderMob> {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;
    private final float speedModifier;
    protected final int maxHorizontalDistance;
    protected final int maxVerticalDistance;
    private final boolean mayStrollFromWater;

    public RandomStroll(float speed) {
        this(speed, true);
    }

    public RandomStroll(float speed, boolean strollInsideWater) {
        this(speed, 10, 7, strollInsideWater);
    }

    public RandomStroll(float speed, int horizontalRadius, int verticalRadius) {
        this(speed, horizontalRadius, verticalRadius, true);
    }

    public RandomStroll(float speed, int horizontalRadius, int verticalRadius, boolean strollInsideWater) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
        this.maxHorizontalDistance = horizontalRadius;
        this.maxVerticalDistance = verticalRadius;
        this.mayStrollFromWater = strollInsideWater;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, PathfinderMob entity) {
        return this.mayStrollFromWater || !entity.isInWaterOrBubble();
    }

    @Override
    protected void start(ServerLevel world, PathfinderMob entity, long time) {
        Optional<Vec3> optional = Optional.ofNullable(this.getTargetPos(entity));
        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((pos) -> {
            return new WalkTarget(pos, this.speedModifier, 0);
        }));
    }

    @Nullable
    protected Vec3 getTargetPos(PathfinderMob entity) {
        return LandRandomPos.getPos(entity, this.maxHorizontalDistance, this.maxVerticalDistance);
    }
}
