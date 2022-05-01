package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class LandRandomPos {
    @Nullable
    public static Vec3 getPos(PathfinderMob entity, int horizontalRange, int verticalRange) {
        return getPos(entity, horizontalRange, verticalRange, entity::getWalkTargetValue);
    }

    @Nullable
    public static Vec3 getPos(PathfinderMob entity, int horizontalRange, int verticalRange, ToDoubleFunction<BlockPos> scorer) {
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return RandomPos.generateRandomPos(() -> {
            BlockPos blockPos = RandomPos.generateRandomDirection(entity.getRandom(), horizontalRange, verticalRange);
            BlockPos blockPos2 = generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
            return blockPos2 == null ? null : movePosUpOutOfSolid(entity, blockPos2);
        }, scorer);
    }

    @Nullable
    public static Vec3 getPosTowards(PathfinderMob entity, int horizontalRange, int verticalRange, Vec3 end) {
        Vec3 vec3 = end.subtract(entity.getX(), entity.getY(), entity.getZ());
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return getPosInDirection(entity, horizontalRange, verticalRange, vec3, bl);
    }

    @Nullable
    public static Vec3 getPosAway(PathfinderMob entity, int horizontalRange, int verticalRange, Vec3 start) {
        Vec3 vec3 = entity.position().subtract(start);
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return getPosInDirection(entity, horizontalRange, verticalRange, vec3, bl);
    }

    @Nullable
    private static Vec3 getPosInDirection(PathfinderMob entity, int horizontalRange, int verticalRange, Vec3 direction, boolean posTargetInRange) {
        return RandomPos.generateRandomPos(entity, () -> {
            BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, 0, direction.x, direction.z, (double)((float)Math.PI / 2F));
            if (blockPos == null) {
                return null;
            } else {
                BlockPos blockPos2 = generateRandomPosTowardDirection(entity, horizontalRange, posTargetInRange, blockPos);
                return blockPos2 == null ? null : movePosUpOutOfSolid(entity, blockPos2);
            }
        });
    }

    @Nullable
    public static BlockPos movePosUpOutOfSolid(PathfinderMob entity, BlockPos pos) {
        pos = RandomPos.moveUpOutOfSolid(pos, entity.level.getMaxBuildHeight(), (currentPos) -> {
            return GoalUtils.isSolid(entity, currentPos);
        });
        return !GoalUtils.isWater(entity, pos) && !GoalUtils.hasMalus(entity, pos) ? pos : null;
    }

    @Nullable
    public static BlockPos generateRandomPosTowardDirection(PathfinderMob entity, int horizontalRange, boolean posTargetInRange, BlockPos relativeInRangePos) {
        BlockPos blockPos = RandomPos.generateRandomPosTowardDirection(entity, horizontalRange, entity.getRandom(), relativeInRangePos);
        return !GoalUtils.isOutsideLimits(blockPos, entity) && !GoalUtils.isRestricted(posTargetInRange, entity, blockPos) && !GoalUtils.isNotStable(entity.getNavigation(), blockPos) ? blockPos : null;
    }
}
