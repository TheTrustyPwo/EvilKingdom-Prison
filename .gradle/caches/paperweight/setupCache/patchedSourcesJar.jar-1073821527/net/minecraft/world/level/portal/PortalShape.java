package net.minecraft.world.level.portal;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class PortalShape {
    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;
    private static final BlockBehaviour.StatePredicate FRAME = (state, world, pos) -> {
        return state.is(Blocks.OBSIDIAN);
    };
    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final Direction rightDir;
    private int numPortalBlocks;
    @Nullable
    private BlockPos bottomLeft;
    private int height;
    private final int width;

    public static Optional<PortalShape> findEmptyPortalShape(LevelAccessor world, BlockPos pos, Direction.Axis axis) {
        return findPortalShape(world, pos, (portalShape) -> {
            return portalShape.isValid() && portalShape.numPortalBlocks == 0;
        }, axis);
    }

    public static Optional<PortalShape> findPortalShape(LevelAccessor world, BlockPos pos, Predicate<PortalShape> predicate, Direction.Axis axis) {
        Optional<PortalShape> optional = Optional.of(new PortalShape(world, pos, axis)).filter(predicate);
        if (optional.isPresent()) {
            return optional;
        } else {
            Direction.Axis axis2 = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            return Optional.of(new PortalShape(world, pos, axis2)).filter(predicate);
        }
    }

    public PortalShape(LevelAccessor world, BlockPos pos, Direction.Axis axis) {
        this.level = world;
        this.axis = axis;
        this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        this.bottomLeft = this.calculateBottomLeft(pos);
        if (this.bottomLeft == null) {
            this.bottomLeft = pos;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = this.calculateWidth();
            if (this.width > 0) {
                this.height = this.calculateHeight();
            }
        }

    }

    @Nullable
    private BlockPos calculateBottomLeft(BlockPos pos) {
        for(int i = Math.max(this.level.getMinBuildHeight(), pos.getY() - 21); pos.getY() > i && isEmpty(this.level.getBlockState(pos.below())); pos = pos.below()) {
        }

        Direction direction = this.rightDir.getOpposite();
        int j = this.getDistanceUntilEdgeAboveFrame(pos, direction) - 1;
        return j < 0 ? null : pos.relative(direction, j);
    }

    private int calculateWidth() {
        int i = this.getDistanceUntilEdgeAboveFrame(this.bottomLeft, this.rightDir);
        return i >= 2 && i <= 21 ? i : 0;
    }

    private int getDistanceUntilEdgeAboveFrame(BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(int i = 0; i <= 21; ++i) {
            mutableBlockPos.set(pos).move(direction, i);
            BlockState blockState = this.level.getBlockState(mutableBlockPos);
            if (!isEmpty(blockState)) {
                if (FRAME.test(blockState, this.level, mutableBlockPos)) {
                    return i;
                }
                break;
            }

            BlockState blockState2 = this.level.getBlockState(mutableBlockPos.move(Direction.DOWN));
            if (!FRAME.test(blockState2, this.level, mutableBlockPos)) {
                break;
            }
        }

        return 0;
    }

    private int calculateHeight() {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        int i = this.getDistanceUntilTop(mutableBlockPos);
        return i >= 3 && i <= 21 && this.hasTopFrame(mutableBlockPos, i) ? i : 0;
    }

    private boolean hasTopFrame(BlockPos.MutableBlockPos mutableBlockPos, int i) {
        for(int j = 0; j < this.width; ++j) {
            BlockPos.MutableBlockPos mutableBlockPos2 = mutableBlockPos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, j);
            if (!FRAME.test(this.level.getBlockState(mutableBlockPos2), this.level, mutableBlockPos2)) {
                return false;
            }
        }

        return true;
    }

    private int getDistanceUntilTop(BlockPos.MutableBlockPos mutableBlockPos) {
        for(int i = 0; i < 21; ++i) {
            mutableBlockPos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, -1);
            if (!FRAME.test(this.level.getBlockState(mutableBlockPos), this.level, mutableBlockPos)) {
                return i;
            }

            mutableBlockPos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, this.width);
            if (!FRAME.test(this.level.getBlockState(mutableBlockPos), this.level, mutableBlockPos)) {
                return i;
            }

            for(int j = 0; j < this.width; ++j) {
                mutableBlockPos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, j);
                BlockState blockState = this.level.getBlockState(mutableBlockPos);
                if (!isEmpty(blockState)) {
                    return i;
                }

                if (blockState.is(Blocks.NETHER_PORTAL)) {
                    ++this.numPortalBlocks;
                }
            }
        }

        return 21;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(Blocks.NETHER_PORTAL);
    }

    public boolean isValid() {
        return this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
    }

    public void createPortalBlocks() {
        BlockState blockState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1)).forEach((blockPos) -> {
            this.level.setBlock(blockPos, blockState, 18);
        });
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    public static Vec3 getRelativePosition(BlockUtil.FoundRectangle portalRect, Direction.Axis portalAxis, Vec3 entityPos, EntityDimensions entityDimensions) {
        double d = (double)portalRect.axis1Size - (double)entityDimensions.width;
        double e = (double)portalRect.axis2Size - (double)entityDimensions.height;
        BlockPos blockPos = portalRect.minCorner;
        double g;
        if (d > 0.0D) {
            float f = (float)blockPos.get(portalAxis) + entityDimensions.width / 2.0F;
            g = Mth.clamp(Mth.inverseLerp(entityPos.get(portalAxis) - (double)f, 0.0D, d), 0.0D, 1.0D);
        } else {
            g = 0.5D;
        }

        double i;
        if (e > 0.0D) {
            Direction.Axis axis = Direction.Axis.Y;
            i = Mth.clamp(Mth.inverseLerp(entityPos.get(axis) - (double)blockPos.get(axis), 0.0D, e), 0.0D, 1.0D);
        } else {
            i = 0.0D;
        }

        Direction.Axis axis2 = portalAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        double k = entityPos.get(axis2) - ((double)blockPos.get(axis2) + 0.5D);
        return new Vec3(g, i, k);
    }

    public static PortalInfo createPortalInfo(ServerLevel destination, BlockUtil.FoundRectangle portalRect, Direction.Axis portalAxis, Vec3 offset, EntityDimensions dimensions, Vec3 velocity, float yaw, float pitch) {
        BlockPos blockPos = portalRect.minCorner;
        BlockState blockState = destination.getBlockState(blockPos);
        Direction.Axis axis = blockState.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        double d = (double)portalRect.axis1Size;
        double e = (double)portalRect.axis2Size;
        int i = portalAxis == axis ? 0 : 90;
        Vec3 vec3 = portalAxis == axis ? velocity : new Vec3(velocity.z, velocity.y, -velocity.x);
        double f = (double)dimensions.width / 2.0D + (d - (double)dimensions.width) * offset.x();
        double g = (e - (double)dimensions.height) * offset.y();
        double h = 0.5D + offset.z();
        boolean bl = axis == Direction.Axis.X;
        Vec3 vec32 = new Vec3((double)blockPos.getX() + (bl ? f : h), (double)blockPos.getY() + g, (double)blockPos.getZ() + (bl ? h : f));
        return new PortalInfo(vec32, vec3, yaw + (float)i, pitch);
    }
}
