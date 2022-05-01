package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class AirRandomPos {
    @Nullable
    public static Vec3 getPosTowards(PathfinderMob entity, int horizontalRange, int verticalRange, int startHeight, Vec3 direction, double angleRange) {
        Vec3 vec3 = direction.subtract(entity.getX(), entity.getY(), entity.getZ());
        boolean bl = GoalUtils.mobRestricted(entity, horizontalRange);
        return RandomPos.generateRandomPos(entity, () -> {
            BlockPos blockPos = AirAndWaterRandomPos.generateRandomPos(entity, horizontalRange, verticalRange, startHeight, vec3.x, vec3.z, angleRange, bl);
            return blockPos != null && !GoalUtils.isWater(entity, blockPos) ? blockPos : null;
        });
    }
}
