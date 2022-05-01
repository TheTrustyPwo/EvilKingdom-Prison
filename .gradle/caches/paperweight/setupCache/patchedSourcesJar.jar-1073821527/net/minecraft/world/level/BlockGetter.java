package net.minecraft.world.level;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface BlockGetter extends LevelHeightAccessor {
    @Nullable
    BlockEntity getBlockEntity(BlockPos pos);

    default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        BlockEntity blockEntity = this.getBlockEntity(pos);
        return blockEntity != null && blockEntity.getType() == type ? Optional.of((T)blockEntity) : Optional.empty();
    }

    BlockState getBlockState(BlockPos pos);

    FluidState getFluidState(BlockPos pos);

    default int getLightEmission(BlockPos pos) {
        return this.getBlockState(pos).getLightEmission();
    }

    default int getMaxLightLevel() {
        return 15;
    }

    default Stream<BlockState> getBlockStates(AABB box) {
        return BlockPos.betweenClosedStream(box).map(this::getBlockState);
    }

    default BlockHitResult isBlockInLine(ClipBlockStateContext context) {
        return traverseBlocks(context.getFrom(), context.getTo(), context, (contextx, pos) -> {
            BlockState blockState = this.getBlockState(pos);
            Vec3 vec3 = contextx.getFrom().subtract(contextx.getTo());
            return contextx.isTargetBlock().test(blockState) ? new BlockHitResult(contextx.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos(contextx.getTo()), false) : null;
        }, (contextx) -> {
            Vec3 vec3 = contextx.getFrom().subtract(contextx.getTo());
            return BlockHitResult.miss(contextx.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos(contextx.getTo()));
        });
    }

    default BlockHitResult clip(ClipContext context) {
        return traverseBlocks(context.getFrom(), context.getTo(), context, (contextx, pos) -> {
            BlockState blockState = this.getBlockState(pos);
            FluidState fluidState = this.getFluidState(pos);
            Vec3 vec3 = contextx.getFrom();
            Vec3 vec32 = contextx.getTo();
            VoxelShape voxelShape = contextx.getBlockShape(blockState, this, pos);
            BlockHitResult blockHitResult = this.clipWithInteractionOverride(vec3, vec32, pos, voxelShape, blockState);
            VoxelShape voxelShape2 = contextx.getFluidShape(fluidState, this, pos);
            BlockHitResult blockHitResult2 = voxelShape2.clip(vec3, vec32, pos);
            double d = blockHitResult == null ? Double.MAX_VALUE : contextx.getFrom().distanceToSqr(blockHitResult.getLocation());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : contextx.getFrom().distanceToSqr(blockHitResult2.getLocation());
            return d <= e ? blockHitResult : blockHitResult2;
        }, (contextx) -> {
            Vec3 vec3 = contextx.getFrom().subtract(contextx.getTo());
            return BlockHitResult.miss(contextx.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos(contextx.getTo()));
        });
    }

    @Nullable
    default BlockHitResult clipWithInteractionOverride(Vec3 start, Vec3 end, BlockPos pos, VoxelShape shape, BlockState state) {
        BlockHitResult blockHitResult = shape.clip(start, end, pos);
        if (blockHitResult != null) {
            BlockHitResult blockHitResult2 = state.getInteractionShape(this, pos).clip(start, end, pos);
            if (blockHitResult2 != null && blockHitResult2.getLocation().subtract(start).lengthSqr() < blockHitResult.getLocation().subtract(start).lengthSqr()) {
                return blockHitResult.withDirection(blockHitResult2.getDirection());
            }
        }

        return blockHitResult;
    }

    default double getBlockFloorHeight(VoxelShape blockCollisionShape, Supplier<VoxelShape> belowBlockCollisionShapeGetter) {
        if (!blockCollisionShape.isEmpty()) {
            return blockCollisionShape.max(Direction.Axis.Y);
        } else {
            double d = belowBlockCollisionShapeGetter.get().max(Direction.Axis.Y);
            return d >= 1.0D ? d - 1.0D : Double.NEGATIVE_INFINITY;
        }
    }

    default double getBlockFloorHeight(BlockPos pos) {
        return this.getBlockFloorHeight(this.getBlockState(pos).getCollisionShape(this, pos), () -> {
            BlockPos blockPos2 = pos.below();
            return this.getBlockState(blockPos2).getCollisionShape(this, blockPos2);
        });
    }

    static <T, C> T traverseBlocks(Vec3 start, Vec3 end, C context, BiFunction<C, BlockPos, T> blockHitFactory, Function<C, T> missFactory) {
        if (start.equals(end)) {
            return missFactory.apply(context);
        } else {
            double d = Mth.lerp(-1.0E-7D, end.x, start.x);
            double e = Mth.lerp(-1.0E-7D, end.y, start.y);
            double f = Mth.lerp(-1.0E-7D, end.z, start.z);
            double g = Mth.lerp(-1.0E-7D, start.x, end.x);
            double h = Mth.lerp(-1.0E-7D, start.y, end.y);
            double i = Mth.lerp(-1.0E-7D, start.z, end.z);
            int j = Mth.floor(g);
            int k = Mth.floor(h);
            int l = Mth.floor(i);
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(j, k, l);
            T object = blockHitFactory.apply(context, mutableBlockPos);
            if (object != null) {
                return object;
            } else {
                double m = d - g;
                double n = e - h;
                double o = f - i;
                int p = Mth.sign(m);
                int q = Mth.sign(n);
                int r = Mth.sign(o);
                double s = p == 0 ? Double.MAX_VALUE : (double)p / m;
                double t = q == 0 ? Double.MAX_VALUE : (double)q / n;
                double u = r == 0 ? Double.MAX_VALUE : (double)r / o;
                double v = s * (p > 0 ? 1.0D - Mth.frac(g) : Mth.frac(g));
                double w = t * (q > 0 ? 1.0D - Mth.frac(h) : Mth.frac(h));
                double x = u * (r > 0 ? 1.0D - Mth.frac(i) : Mth.frac(i));

                while(v <= 1.0D || w <= 1.0D || x <= 1.0D) {
                    if (v < w) {
                        if (v < x) {
                            j += p;
                            v += s;
                        } else {
                            l += r;
                            x += u;
                        }
                    } else if (w < x) {
                        k += q;
                        w += t;
                    } else {
                        l += r;
                        x += u;
                    }

                    T object2 = blockHitFactory.apply(context, mutableBlockPos.set(j, k, l));
                    if (object2 != null) {
                        return object2;
                    }
                }

                return missFactory.apply(context);
            }
        }
    }
}
