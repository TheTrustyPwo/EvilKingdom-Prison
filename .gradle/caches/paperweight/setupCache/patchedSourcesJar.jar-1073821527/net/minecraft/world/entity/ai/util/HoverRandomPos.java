package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class HoverRandomPos {
    @Nullable
    public static Vec3 getPos(PathfinderMob entity, int horizontalRange, int verticalRange, double x, double z, float angle, int maxAboveSolid, int minAboveSolid) {
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return RandomPos.generateRandomPos(entity, () -> {
            BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, 0, x, z, (double)angle);
            if (blockPos == null) {
                return null;
            } else {
                BlockPos blockPos2 = LandRandomPos.generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
                if (blockPos2 == null) {
                    return null;
                } else {
                    blockPos2 = RandomPos.moveUpToAboveSolid(blockPos2, entity.getRandom().nextInt(maxAboveSolid - minAboveSolid + 1) + minAboveSolid, entity.level.getMaxBuildHeight(), (pos) -> {
                        return GoalUtils.isSolid(entity, pos);
                    });
                    return !GoalUtils.isWater(entity, blockPos2) && !GoalUtils.hasMalus(entity, blockPos2) ? blockPos2 : null;
                }
            }
        });
    }
}
