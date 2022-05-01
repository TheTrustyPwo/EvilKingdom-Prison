package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class DefaultRandomPos {
    @Nullable
    public static Vec3 getPos(PathfinderMob entity, int horizontalRange, int verticalRange) {
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return RandomPos.generateRandomPos(entity, () -> {
            BlockPos blockPos = RandomPos.generateRandomDirection(entity.getRandom(), horizontalRange, verticalRange);
            return generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
        });
    }

    @Nullable
    public static Vec3 getPosTowards(PathfinderMob entity, int horizontalRange, int verticalRange, Vec3 end, double angleRange) {
        Vec3 vec3 = end.subtract(entity.getX(), entity.getY(), entity.getZ());
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return RandomPos.generateRandomPos(entity, () -> {
            BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, 0, vec3.x, vec3.z, angleRange);
            return blockPos == null ? null : generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
        });
    }

    @Nullable
    public static Vec3 getPosAway(PathfinderMob entity, int horizontalRange, int verticalRange, Vec3 start) {
        Vec3 vec3 = entity.position().subtract(start);
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return RandomPos.generateRandomPos(entity, () -> {
            BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(entity.getRandom(), horizontalRange, verticalRange, 0, vec3.x, vec3.z, (double)((float)Math.PI / 2F));
            return blockPos == null ? null : generateRandomPosTowardDirection(entity, horizontalRange, bl, blockPos);
        });
    }

    @Nullable
    private static BlockPos generateRandomPosTowardDirection(PathfinderMob entity, int horizontalRange, boolean posTargetInRange, BlockPos fuzz) {
        BlockPos blockPos = RandomPos.generateRandomPosTowardDirection(entity, horizontalRange, entity.getRandom(), fuzz);
        return !GoalUtils.isOutsideLimits(blockPos, entity) && !GoalUtils.isRestricted(posTargetInRange, entity, blockPos) && !GoalUtils.isNotStable(entity.getNavigation(), blockPos) && !GoalUtils.hasMalus(entity, blockPos) ? blockPos : null;
    }
}
