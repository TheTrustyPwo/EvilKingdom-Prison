package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class AirAndWaterRandomPos {
    @Nullable
    public static Vec3 getPos(PathfinderMob entity, int horizontalRange, int verticalRange, int startHeight, double directionX, double directionZ, double rangeAngle) {
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return RandomPos.generateRandomPos(entity, () -> {
            return generateRandomPos(entity, horizontalRange, verticalRange, startHeight, directionX, directionZ, rangeAngle, bl);
        });
    }

    @Nullable
    public static BlockPos generateRandomPos(PathfinderMob entity, int horizontalRange, int verticalRange, int startHeight, double directionX, double directionZ, double rangeAngle, boolean posTargetInRange) {
        BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, startHeight, directionX, directionZ, rangeAngle);
        if (blockPos == null) {
            return null;
        } else {
            BlockPos blockPos2 = RandomPos.generateRandomPosTowardDirection(entity, horizontalRange, entity.getRandom(), blockPos);
            if (!GoalUtils.isOutsideLimits(blockPos2, entity) && !GoalUtils.isRestricted(posTargetInRange, entity, blockPos2)) {
                blockPos2 = RandomPos.moveUpOutOfSolid(blockPos2, entity.level.getMaxBuildHeight(), (pos) -> {
                    return GoalUtils.isSolid(entity, pos);
                });
                return GoalUtils.hasMalus(entity, blockPos2) ? null : blockPos2;
            } else {
                return null;
            }
        }
    }
}
