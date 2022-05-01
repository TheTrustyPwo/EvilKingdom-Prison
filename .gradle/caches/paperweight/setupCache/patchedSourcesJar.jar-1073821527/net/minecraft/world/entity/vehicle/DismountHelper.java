package net.minecraft.world.entity.vehicle;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DismountHelper {
    public static int[][] offsetsForDirection(Direction movementDirection) {
        Direction direction = movementDirection.getClockWise();
        Direction direction2 = direction.getOpposite();
        Direction direction3 = movementDirection.getOpposite();
        return new int[][]{{direction.getStepX(), direction.getStepZ()}, {direction2.getStepX(), direction2.getStepZ()}, {direction3.getStepX() + direction.getStepX(), direction3.getStepZ() + direction.getStepZ()}, {direction3.getStepX() + direction2.getStepX(), direction3.getStepZ() + direction2.getStepZ()}, {movementDirection.getStepX() + direction.getStepX(), movementDirection.getStepZ() + direction.getStepZ()}, {movementDirection.getStepX() + direction2.getStepX(), movementDirection.getStepZ() + direction2.getStepZ()}, {direction3.getStepX(), direction3.getStepZ()}, {movementDirection.getStepX(), movementDirection.getStepZ()}};
    }

    public static boolean isBlockFloorValid(double height) {
        return !Double.isInfinite(height) && height < 1.0D;
    }

    public static boolean canDismountTo(CollisionGetter world, LivingEntity entity, AABB targetBox) {
        for(VoxelShape voxelShape : world.getBlockCollisions(entity, targetBox)) {
            if (!voxelShape.isEmpty()) {
                return false;
            }
        }

        return world.getWorldBorder().isWithinBounds(targetBox);
    }

    public static boolean canDismountTo(CollisionGetter world, Vec3 offset, LivingEntity entity, Pose pose) {
        return canDismountTo(world, entity, entity.getLocalBoundsForPose(pose).move(offset));
    }

    public static VoxelShape nonClimbableShape(BlockGetter world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return !blockState.is(BlockTags.CLIMBABLE) && (!(blockState.getBlock() instanceof TrapDoorBlock) || !blockState.getValue(TrapDoorBlock.OPEN)) ? blockState.getCollisionShape(world, pos) : Shapes.empty();
    }

    public static double findCeilingFrom(BlockPos pos, int maxDistance, Function<BlockPos, VoxelShape> collisionShapeGetter) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        int i = 0;

        while(i < maxDistance) {
            VoxelShape voxelShape = collisionShapeGetter.apply(mutableBlockPos);
            if (!voxelShape.isEmpty()) {
                return (double)(pos.getY() + i) + voxelShape.min(Direction.Axis.Y);
            }

            ++i;
            mutableBlockPos.move(Direction.UP);
        }

        return Double.POSITIVE_INFINITY;
    }

    @Nullable
    public static Vec3 findSafeDismountLocation(EntityType<?> entityType, CollisionGetter world, BlockPos pos, boolean ignoreInvalidPos) {
        if (ignoreInvalidPos && entityType.isBlockDangerous(world.getBlockState(pos))) {
            return null;
        } else {
            double d = world.getBlockFloorHeight(nonClimbableShape(world, pos), () -> {
                return nonClimbableShape(world, pos.below());
            });
            if (!isBlockFloorValid(d)) {
                return null;
            } else if (ignoreInvalidPos && d <= 0.0D && entityType.isBlockDangerous(world.getBlockState(pos.below()))) {
                return null;
            } else {
                Vec3 vec3 = Vec3.upFromBottomCenterOf(pos, d);
                AABB aABB = entityType.getDimensions().makeBoundingBox(vec3);

                for(VoxelShape voxelShape : world.getBlockCollisions((Entity)null, aABB)) {
                    if (!voxelShape.isEmpty()) {
                        return null;
                    }
                }

                return !world.getWorldBorder().isWithinBounds(aABB) ? null : vec3;
            }
        }
    }
}
