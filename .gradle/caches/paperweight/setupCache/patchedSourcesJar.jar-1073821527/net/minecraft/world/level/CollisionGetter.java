package net.minecraft.world.level;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CollisionGetter extends BlockGetter {
    WorldBorder getWorldBorder();

    @Nullable
    BlockGetter getChunkForCollisions(int chunkX, int chunkZ);

    default boolean isUnobstructed(@Nullable Entity except, VoxelShape shape) {
        return true;
    }

    default boolean isUnobstructed(BlockState state, BlockPos pos, CollisionContext context) {
        VoxelShape voxelShape = state.getCollisionShape(this, pos, context);
        return voxelShape.isEmpty() || this.isUnobstructed((Entity)null, voxelShape.move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()));
    }

    default boolean isUnobstructed(Entity entity) {
        return this.isUnobstructed(entity, Shapes.create(entity.getBoundingBox()));
    }

    default boolean noCollision(AABB box) {
        return this.noCollision((Entity)null, box);
    }

    default boolean noCollision(Entity entity) {
        return this.noCollision(entity, entity.getBoundingBox());
    }

    default boolean noCollision(@Nullable Entity entity, AABB box) {
        for(VoxelShape voxelShape : this.getBlockCollisions(entity, box)) {
            if (!voxelShape.isEmpty()) {
                return false;
            }
        }

        if (!this.getEntityCollisions(entity, box).isEmpty()) {
            return false;
        } else if (entity == null) {
            return true;
        } else {
            VoxelShape voxelShape2 = this.borderCollision(entity, box);
            return voxelShape2 == null || !Shapes.joinIsNotEmpty(voxelShape2, Shapes.create(box), BooleanOp.AND);
        }
    }

    List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB box);

    default Iterable<VoxelShape> getCollisions(@Nullable Entity entity, AABB box) {
        List<VoxelShape> list = this.getEntityCollisions(entity, box);
        Iterable<VoxelShape> iterable = this.getBlockCollisions(entity, box);
        return list.isEmpty() ? iterable : Iterables.concat(list, iterable);
    }

    default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, AABB box) {
        return () -> {
            return new BlockCollisions(this, entity, box);
        };
    }

    @Nullable
    private VoxelShape borderCollision(Entity entity, AABB box) {
        WorldBorder worldBorder = this.getWorldBorder();
        return worldBorder.isInsideCloseToBorder(entity, box) ? worldBorder.getCollisionShape() : null;
    }

    default boolean collidesWithSuffocatingBlock(@Nullable Entity entity, AABB box) {
        BlockCollisions blockCollisions = new BlockCollisions(this, entity, box, true);

        while(blockCollisions.hasNext()) {
            if (!blockCollisions.next().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    default Optional<Vec3> findFreePosition(@Nullable Entity entity, VoxelShape shape, Vec3 target, double x, double y, double z) {
        if (shape.isEmpty()) {
            return Optional.empty();
        } else {
            AABB aABB = shape.bounds().inflate(x, y, z);
            VoxelShape voxelShape = StreamSupport.stream(this.getBlockCollisions(entity, aABB).spliterator(), false).filter((voxelShapex) -> {
                return this.getWorldBorder() == null || this.getWorldBorder().isWithinBounds(voxelShapex.bounds());
            }).flatMap((voxelShapex) -> {
                return voxelShapex.toAabbs().stream();
            }).map((aABBx) -> {
                return aABBx.inflate(x / 2.0D, y / 2.0D, z / 2.0D);
            }).map(Shapes::create).reduce(Shapes.empty(), Shapes::or);
            VoxelShape voxelShape2 = Shapes.join(shape, voxelShape, BooleanOp.ONLY_FIRST);
            return voxelShape2.closestPointTo(target);
        }
    }
}
