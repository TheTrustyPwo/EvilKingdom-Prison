package net.minecraft.world.entity.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class GoalUtils {
    public static boolean hasGroundPathNavigation(Mob entity) {
        return entity.getNavigation() instanceof GroundPathNavigation;
    }

    public static boolean mobRestricted(PathfinderMob entity, int extraDistance) {
        return entity.hasRestriction() && entity.getRestrictCenter().closerToCenterThan(entity.position(), (double)(entity.getRestrictRadius() + (float)extraDistance) + 1.0D);
    }

    public static boolean isOutsideLimits(BlockPos pos, PathfinderMob entity) {
        return pos.getY() < entity.level.getMinBuildHeight() || pos.getY() > entity.level.getMaxBuildHeight();
    }

    public static boolean isRestricted(boolean posTargetInRange, PathfinderMob entity, BlockPos pos) {
        return posTargetInRange && !entity.isWithinRestriction(pos);
    }

    public static boolean isNotStable(PathNavigation navigation, BlockPos pos) {
        return !navigation.isStableDestination(pos);
    }

    public static boolean isWater(PathfinderMob entity, BlockPos pos) {
        return entity.level.getFluidState(pos).is(FluidTags.WATER);
    }

    public static boolean hasMalus(PathfinderMob entity, BlockPos pos) {
        return entity.getPathfindingMalus(WalkNodeEvaluator.getBlockPathTypeStatic(entity.level, pos.mutable())) != 0.0F;
    }

    public static boolean isSolid(PathfinderMob entity, BlockPos pos) {
        return entity.level.getBlockState(pos).getMaterial().isSolid();
    }
}
