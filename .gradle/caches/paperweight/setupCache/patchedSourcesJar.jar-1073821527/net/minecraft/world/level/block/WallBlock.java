package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final EnumProperty<WallSide> EAST_WALL = BlockStateProperties.EAST_WALL;
    public static final EnumProperty<WallSide> NORTH_WALL = BlockStateProperties.NORTH_WALL;
    public static final EnumProperty<WallSide> SOUTH_WALL = BlockStateProperties.SOUTH_WALL;
    public static final EnumProperty<WallSide> WEST_WALL = BlockStateProperties.WEST_WALL;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final Map<BlockState, VoxelShape> shapeByIndex;
    private final Map<BlockState, VoxelShape> collisionShapeByIndex;
    private static final int WALL_WIDTH = 3;
    private static final int WALL_HEIGHT = 14;
    private static final int POST_WIDTH = 4;
    private static final int POST_COVER_WIDTH = 1;
    private static final int WALL_COVER_START = 7;
    private static final int WALL_COVER_END = 9;
    private static final VoxelShape POST_TEST = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape NORTH_TEST = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape SOUTH_TEST = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_TEST = Block.box(0.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape EAST_TEST = Block.box(7.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);

    public WallBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(UP, Boolean.valueOf(true)).setValue(NORTH_WALL, WallSide.NONE).setValue(EAST_WALL, WallSide.NONE).setValue(SOUTH_WALL, WallSide.NONE).setValue(WEST_WALL, WallSide.NONE).setValue(WATERLOGGED, Boolean.valueOf(false)));
        this.shapeByIndex = this.makeShapes(4.0F, 3.0F, 16.0F, 0.0F, 14.0F, 16.0F);
        this.collisionShapeByIndex = this.makeShapes(4.0F, 3.0F, 24.0F, 0.0F, 24.0F, 24.0F);
    }

    private static VoxelShape applyWallShape(VoxelShape base, WallSide wallShape, VoxelShape tall, VoxelShape low) {
        if (wallShape == WallSide.TALL) {
            return Shapes.or(base, low);
        } else {
            return wallShape == WallSide.LOW ? Shapes.or(base, tall) : base;
        }
    }

    private Map<BlockState, VoxelShape> makeShapes(float f, float g, float h, float i, float j, float k) {
        float l = 8.0F - f;
        float m = 8.0F + f;
        float n = 8.0F - g;
        float o = 8.0F + g;
        VoxelShape voxelShape = Block.box((double)l, 0.0D, (double)l, (double)m, (double)h, (double)m);
        VoxelShape voxelShape2 = Block.box((double)n, (double)i, 0.0D, (double)o, (double)j, (double)o);
        VoxelShape voxelShape3 = Block.box((double)n, (double)i, (double)n, (double)o, (double)j, 16.0D);
        VoxelShape voxelShape4 = Block.box(0.0D, (double)i, (double)n, (double)o, (double)j, (double)o);
        VoxelShape voxelShape5 = Block.box((double)n, (double)i, (double)n, 16.0D, (double)j, (double)o);
        VoxelShape voxelShape6 = Block.box((double)n, (double)i, 0.0D, (double)o, (double)k, (double)o);
        VoxelShape voxelShape7 = Block.box((double)n, (double)i, (double)n, (double)o, (double)k, 16.0D);
        VoxelShape voxelShape8 = Block.box(0.0D, (double)i, (double)n, (double)o, (double)k, (double)o);
        VoxelShape voxelShape9 = Block.box((double)n, (double)i, (double)n, 16.0D, (double)k, (double)o);
        Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        for(Boolean boolean_ : UP.getPossibleValues()) {
            for(WallSide wallSide : EAST_WALL.getPossibleValues()) {
                for(WallSide wallSide2 : NORTH_WALL.getPossibleValues()) {
                    for(WallSide wallSide3 : WEST_WALL.getPossibleValues()) {
                        for(WallSide wallSide4 : SOUTH_WALL.getPossibleValues()) {
                            VoxelShape voxelShape10 = Shapes.empty();
                            voxelShape10 = applyWallShape(voxelShape10, wallSide, voxelShape5, voxelShape9);
                            voxelShape10 = applyWallShape(voxelShape10, wallSide3, voxelShape4, voxelShape8);
                            voxelShape10 = applyWallShape(voxelShape10, wallSide2, voxelShape2, voxelShape6);
                            voxelShape10 = applyWallShape(voxelShape10, wallSide4, voxelShape3, voxelShape7);
                            if (boolean_) {
                                voxelShape10 = Shapes.or(voxelShape10, voxelShape);
                            }

                            BlockState blockState = this.defaultBlockState().setValue(UP, boolean_).setValue(EAST_WALL, wallSide).setValue(WEST_WALL, wallSide3).setValue(NORTH_WALL, wallSide2).setValue(SOUTH_WALL, wallSide4);
                            builder.put(blockState.setValue(WATERLOGGED, Boolean.valueOf(false)), voxelShape10);
                            builder.put(blockState.setValue(WATERLOGGED, Boolean.valueOf(true)), voxelShape10);
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.shapeByIndex.get(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.collisionShapeByIndex.get(state);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        return false;
    }

    private boolean connectsTo(BlockState state, boolean faceFullSquare, Direction side) {
        Block block = state.getBlock();
        boolean bl = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, side);
        return state.is(BlockTags.WALLS) || !isExceptionForConnection(state) && faceFullSquare || block instanceof IronBarsBlock || bl;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        LevelReader levelReader = ctx.getLevel();
        BlockPos blockPos = ctx.getClickedPos();
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockPos blockPos2 = blockPos.north();
        BlockPos blockPos3 = blockPos.east();
        BlockPos blockPos4 = blockPos.south();
        BlockPos blockPos5 = blockPos.west();
        BlockPos blockPos6 = blockPos.above();
        BlockState blockState = levelReader.getBlockState(blockPos2);
        BlockState blockState2 = levelReader.getBlockState(blockPos3);
        BlockState blockState3 = levelReader.getBlockState(blockPos4);
        BlockState blockState4 = levelReader.getBlockState(blockPos5);
        BlockState blockState5 = levelReader.getBlockState(blockPos6);
        boolean bl = this.connectsTo(blockState, blockState.isFaceSturdy(levelReader, blockPos2, Direction.SOUTH), Direction.SOUTH);
        boolean bl2 = this.connectsTo(blockState2, blockState2.isFaceSturdy(levelReader, blockPos3, Direction.WEST), Direction.WEST);
        boolean bl3 = this.connectsTo(blockState3, blockState3.isFaceSturdy(levelReader, blockPos4, Direction.NORTH), Direction.NORTH);
        boolean bl4 = this.connectsTo(blockState4, blockState4.isFaceSturdy(levelReader, blockPos5, Direction.EAST), Direction.EAST);
        BlockState blockState6 = this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(fluidState.getType() == Fluids.WATER));
        return this.updateShape(levelReader, blockState6, blockPos6, blockState5, bl, bl2, bl3, bl4);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        if (direction == Direction.DOWN) {
            return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
        } else {
            return direction == Direction.UP ? this.topUpdate(world, state, neighborPos, neighborState) : this.sideUpdate(world, pos, state, neighborPos, neighborState, direction);
        }
    }

    private static boolean isConnected(BlockState state, Property<WallSide> property) {
        return state.getValue(property) != WallSide.NONE;
    }

    private static boolean isCovered(VoxelShape aboveShape, VoxelShape tallShape) {
        return !Shapes.joinIsNotEmpty(tallShape, aboveShape, BooleanOp.ONLY_FIRST);
    }

    private BlockState topUpdate(LevelReader world, BlockState state, BlockPos pos, BlockState aboveState) {
        boolean bl = isConnected(state, NORTH_WALL);
        boolean bl2 = isConnected(state, EAST_WALL);
        boolean bl3 = isConnected(state, SOUTH_WALL);
        boolean bl4 = isConnected(state, WEST_WALL);
        return this.updateShape(world, state, pos, aboveState, bl, bl2, bl3, bl4);
    }

    private BlockState sideUpdate(LevelReader world, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        Direction direction2 = direction.getOpposite();
        boolean bl = direction == Direction.NORTH ? this.connectsTo(neighborState, neighborState.isFaceSturdy(world, neighborPos, direction2), direction2) : isConnected(state, NORTH_WALL);
        boolean bl2 = direction == Direction.EAST ? this.connectsTo(neighborState, neighborState.isFaceSturdy(world, neighborPos, direction2), direction2) : isConnected(state, EAST_WALL);
        boolean bl3 = direction == Direction.SOUTH ? this.connectsTo(neighborState, neighborState.isFaceSturdy(world, neighborPos, direction2), direction2) : isConnected(state, SOUTH_WALL);
        boolean bl4 = direction == Direction.WEST ? this.connectsTo(neighborState, neighborState.isFaceSturdy(world, neighborPos, direction2), direction2) : isConnected(state, WEST_WALL);
        BlockPos blockPos = pos.above();
        BlockState blockState = world.getBlockState(blockPos);
        return this.updateShape(world, state, blockPos, blockState, bl, bl2, bl3, bl4);
    }

    private BlockState updateShape(LevelReader world, BlockState state, BlockPos pos, BlockState aboveState, boolean north, boolean east, boolean south, boolean west) {
        VoxelShape voxelShape = aboveState.getCollisionShape(world, pos).getFaceShape(Direction.DOWN);
        BlockState blockState = this.updateSides(state, north, east, south, west, voxelShape);
        return blockState.setValue(UP, Boolean.valueOf(this.shouldRaisePost(blockState, aboveState, voxelShape)));
    }

    private boolean shouldRaisePost(BlockState state, BlockState aboveState, VoxelShape aboveShape) {
        boolean bl = aboveState.getBlock() instanceof WallBlock && aboveState.getValue(UP);
        if (bl) {
            return true;
        } else {
            WallSide wallSide = state.getValue(NORTH_WALL);
            WallSide wallSide2 = state.getValue(SOUTH_WALL);
            WallSide wallSide3 = state.getValue(EAST_WALL);
            WallSide wallSide4 = state.getValue(WEST_WALL);
            boolean bl2 = wallSide2 == WallSide.NONE;
            boolean bl3 = wallSide4 == WallSide.NONE;
            boolean bl4 = wallSide3 == WallSide.NONE;
            boolean bl5 = wallSide == WallSide.NONE;
            boolean bl6 = bl5 && bl2 && bl3 && bl4 || bl5 != bl2 || bl3 != bl4;
            if (bl6) {
                return true;
            } else {
                boolean bl7 = wallSide == WallSide.TALL && wallSide2 == WallSide.TALL || wallSide3 == WallSide.TALL && wallSide4 == WallSide.TALL;
                if (bl7) {
                    return false;
                } else {
                    return aboveState.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(aboveShape, POST_TEST);
                }
            }
        }
    }

    private BlockState updateSides(BlockState state, boolean north, boolean east, boolean south, boolean west, VoxelShape aboveShape) {
        return state.setValue(NORTH_WALL, this.makeWallState(north, aboveShape, NORTH_TEST)).setValue(EAST_WALL, this.makeWallState(east, aboveShape, EAST_TEST)).setValue(SOUTH_WALL, this.makeWallState(south, aboveShape, SOUTH_TEST)).setValue(WEST_WALL, this.makeWallState(west, aboveShape, WEST_TEST));
    }

    private WallSide makeWallState(boolean connected, VoxelShape aboveShape, VoxelShape tallShape) {
        if (connected) {
            return isCovered(aboveShape, tallShape) ? WallSide.TALL : WallSide.LOW;
        } else {
            return WallSide.NONE;
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH_WALL, EAST_WALL, WEST_WALL, SOUTH_WALL, WATERLOGGED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            return state.setValue(NORTH_WALL, state.getValue(SOUTH_WALL)).setValue(EAST_WALL, state.getValue(WEST_WALL)).setValue(SOUTH_WALL, state.getValue(NORTH_WALL)).setValue(WEST_WALL, state.getValue(EAST_WALL));
        case COUNTERCLOCKWISE_90:
            return state.setValue(NORTH_WALL, state.getValue(EAST_WALL)).setValue(EAST_WALL, state.getValue(SOUTH_WALL)).setValue(SOUTH_WALL, state.getValue(WEST_WALL)).setValue(WEST_WALL, state.getValue(NORTH_WALL));
        case CLOCKWISE_90:
            return state.setValue(NORTH_WALL, state.getValue(WEST_WALL)).setValue(EAST_WALL, state.getValue(NORTH_WALL)).setValue(SOUTH_WALL, state.getValue(EAST_WALL)).setValue(WEST_WALL, state.getValue(SOUTH_WALL));
        default:
            return state;
        }
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        switch(mirror) {
        case LEFT_RIGHT:
            return state.setValue(NORTH_WALL, state.getValue(SOUTH_WALL)).setValue(SOUTH_WALL, state.getValue(NORTH_WALL));
        case FRONT_BACK:
            return state.setValue(EAST_WALL, state.getValue(WEST_WALL)).setValue(WEST_WALL, state.getValue(EAST_WALL));
        default:
            return super.mirror(state, mirror);
        }
    }
}
