package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VineBlock extends Block {
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION.entrySet().stream().filter((entry) -> {
        return entry.getKey() != Direction.DOWN;
    }).collect(Util.toMap());
    protected static final float AABB_OFFSET = 1.0F;
    private static final VoxelShape UP_AABB = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_AABB = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private final Map<BlockState, VoxelShape> shapesCache;

    public VineBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(UP, Boolean.valueOf(false)).setValue(NORTH, Boolean.valueOf(false)).setValue(EAST, Boolean.valueOf(false)).setValue(SOUTH, Boolean.valueOf(false)).setValue(WEST, Boolean.valueOf(false)));
        this.shapesCache = ImmutableMap.copyOf(this.stateDefinition.getPossibleStates().stream().collect(Collectors.toMap(Function.identity(), VineBlock::calculateShape)));
    }

    private static VoxelShape calculateShape(BlockState state) {
        VoxelShape voxelShape = Shapes.empty();
        if (state.getValue(UP)) {
            voxelShape = UP_AABB;
        }

        if (state.getValue(NORTH)) {
            voxelShape = Shapes.or(voxelShape, NORTH_AABB);
        }

        if (state.getValue(SOUTH)) {
            voxelShape = Shapes.or(voxelShape, SOUTH_AABB);
        }

        if (state.getValue(EAST)) {
            voxelShape = Shapes.or(voxelShape, EAST_AABB);
        }

        if (state.getValue(WEST)) {
            voxelShape = Shapes.or(voxelShape, WEST_AABB);
        }

        return voxelShape.isEmpty() ? Shapes.block() : voxelShape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.shapesCache.get(state);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return this.hasFaces(this.getUpdatedState(state, world, pos));
    }

    private boolean hasFaces(BlockState state) {
        return this.countFaces(state) > 0;
    }

    private int countFaces(BlockState state) {
        int i = 0;

        for(BooleanProperty booleanProperty : PROPERTY_BY_DIRECTION.values()) {
            if (state.getValue(booleanProperty)) {
                ++i;
            }
        }

        return i;
    }

    private boolean canSupportAtFace(BlockGetter world, BlockPos pos, Direction side) {
        if (side == Direction.DOWN) {
            return false;
        } else {
            BlockPos blockPos = pos.relative(side);
            if (isAcceptableNeighbour(world, blockPos, side)) {
                return true;
            } else if (side.getAxis() == Direction.Axis.Y) {
                return false;
            } else {
                BooleanProperty booleanProperty = PROPERTY_BY_DIRECTION.get(side);
                BlockState blockState = world.getBlockState(pos.above());
                return blockState.is(this) && blockState.getValue(booleanProperty);
            }
        }
    }

    public static boolean isAcceptableNeighbour(BlockGetter world, BlockPos pos, Direction direction) {
        BlockState blockState = world.getBlockState(pos);
        return Block.isFaceFull(blockState.getCollisionShape(world, pos), direction.getOpposite());
    }

    private BlockState getUpdatedState(BlockState state, BlockGetter world, BlockPos pos) {
        BlockPos blockPos = pos.above();
        if (state.getValue(UP)) {
            state = state.setValue(UP, Boolean.valueOf(isAcceptableNeighbour(world, blockPos, Direction.DOWN)));
        }

        BlockState blockState = null;

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BooleanProperty booleanProperty = getPropertyForFace(direction);
            if (state.getValue(booleanProperty)) {
                boolean bl = this.canSupportAtFace(world, pos, direction);
                if (!bl) {
                    if (blockState == null) {
                        blockState = world.getBlockState(blockPos);
                    }

                    bl = blockState.is(this) && blockState.getValue(booleanProperty);
                }

                state = state.setValue(booleanProperty, Boolean.valueOf(bl));
            }
        }

        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
        } else {
            BlockState blockState = this.getUpdatedState(state, world, pos);
            return !this.hasFaces(blockState) ? Blocks.AIR.defaultBlockState() : blockState;
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (random.nextInt(4) == 0) {
            Direction direction = Direction.getRandom(random);
            BlockPos blockPos = pos.above();
            if (direction.getAxis().isHorizontal() && !state.getValue(getPropertyForFace(direction))) {
                if (this.canSpread(world, pos)) {
                    BlockPos blockPos2 = pos.relative(direction);
                    BlockState blockState = world.getBlockState(blockPos2);
                    if (blockState.isAir()) {
                        Direction direction2 = direction.getClockWise();
                        Direction direction3 = direction.getCounterClockWise();
                        boolean bl = state.getValue(getPropertyForFace(direction2));
                        boolean bl2 = state.getValue(getPropertyForFace(direction3));
                        BlockPos blockPos3 = blockPos2.relative(direction2);
                        BlockPos blockPos4 = blockPos2.relative(direction3);
                        if (bl && isAcceptableNeighbour(world, blockPos3, direction2)) {
                            world.setBlock(blockPos2, this.defaultBlockState().setValue(getPropertyForFace(direction2), Boolean.valueOf(true)), 2);
                        } else if (bl2 && isAcceptableNeighbour(world, blockPos4, direction3)) {
                            world.setBlock(blockPos2, this.defaultBlockState().setValue(getPropertyForFace(direction3), Boolean.valueOf(true)), 2);
                        } else {
                            Direction direction4 = direction.getOpposite();
                            if (bl && world.isEmptyBlock(blockPos3) && isAcceptableNeighbour(world, pos.relative(direction2), direction4)) {
                                world.setBlock(blockPos3, this.defaultBlockState().setValue(getPropertyForFace(direction4), Boolean.valueOf(true)), 2);
                            } else if (bl2 && world.isEmptyBlock(blockPos4) && isAcceptableNeighbour(world, pos.relative(direction3), direction4)) {
                                world.setBlock(blockPos4, this.defaultBlockState().setValue(getPropertyForFace(direction4), Boolean.valueOf(true)), 2);
                            } else if ((double)random.nextFloat() < 0.05D && isAcceptableNeighbour(world, blockPos2.above(), Direction.UP)) {
                                world.setBlock(blockPos2, this.defaultBlockState().setValue(UP, Boolean.valueOf(true)), 2);
                            }
                        }
                    } else if (isAcceptableNeighbour(world, blockPos2, direction)) {
                        world.setBlock(pos, state.setValue(getPropertyForFace(direction), Boolean.valueOf(true)), 2);
                    }

                }
            } else {
                if (direction == Direction.UP && pos.getY() < world.getMaxBuildHeight() - 1) {
                    if (this.canSupportAtFace(world, pos, direction)) {
                        world.setBlock(pos, state.setValue(UP, Boolean.valueOf(true)), 2);
                        return;
                    }

                    if (world.isEmptyBlock(blockPos)) {
                        if (!this.canSpread(world, pos)) {
                            return;
                        }

                        BlockState blockState2 = state;

                        for(Direction direction5 : Direction.Plane.HORIZONTAL) {
                            if (random.nextBoolean() || !isAcceptableNeighbour(world, blockPos.relative(direction5), direction5)) {
                                blockState2 = blockState2.setValue(getPropertyForFace(direction5), Boolean.valueOf(false));
                            }
                        }

                        if (this.hasHorizontalConnection(blockState2)) {
                            world.setBlock(blockPos, blockState2, 2);
                        }

                        return;
                    }
                }

                if (pos.getY() > world.getMinBuildHeight()) {
                    BlockPos blockPos5 = pos.below();
                    BlockState blockState3 = world.getBlockState(blockPos5);
                    if (blockState3.isAir() || blockState3.is(this)) {
                        BlockState blockState4 = blockState3.isAir() ? this.defaultBlockState() : blockState3;
                        BlockState blockState5 = this.copyRandomFaces(state, blockState4, random);
                        if (blockState4 != blockState5 && this.hasHorizontalConnection(blockState5)) {
                            world.setBlock(blockPos5, blockState5, 2);
                        }
                    }
                }

            }
        }
    }

    private BlockState copyRandomFaces(BlockState above, BlockState state, Random random) {
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextBoolean()) {
                BooleanProperty booleanProperty = getPropertyForFace(direction);
                if (above.getValue(booleanProperty)) {
                    state = state.setValue(booleanProperty, Boolean.valueOf(true));
                }
            }
        }

        return state;
    }

    private boolean hasHorizontalConnection(BlockState state) {
        return state.getValue(NORTH) || state.getValue(EAST) || state.getValue(SOUTH) || state.getValue(WEST);
    }

    private boolean canSpread(BlockGetter world, BlockPos pos) {
        int i = 4;
        Iterable<BlockPos> iterable = BlockPos.betweenClosed(pos.getX() - 4, pos.getY() - 1, pos.getZ() - 4, pos.getX() + 4, pos.getY() + 1, pos.getZ() + 4);
        int j = 5;

        for(BlockPos blockPos : iterable) {
            if (world.getBlockState(blockPos).is(this)) {
                --j;
                if (j <= 0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if (blockState.is(this)) {
            return this.countFaces(blockState) < PROPERTY_BY_DIRECTION.size();
        } else {
            return super.canBeReplaced(state, context);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState blockState = ctx.getLevel().getBlockState(ctx.getClickedPos());
        boolean bl = blockState.is(this);
        BlockState blockState2 = bl ? blockState : this.defaultBlockState();

        for(Direction direction : ctx.getNearestLookingDirections()) {
            if (direction != Direction.DOWN) {
                BooleanProperty booleanProperty = getPropertyForFace(direction);
                boolean bl2 = bl && blockState.getValue(booleanProperty);
                if (!bl2 && this.canSupportAtFace(ctx.getLevel(), ctx.getClickedPos(), direction)) {
                    return blockState2.setValue(booleanProperty, Boolean.valueOf(true));
                }
            }
        }

        return bl ? blockState2 : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            return state.setValue(NORTH, state.getValue(SOUTH)).setValue(EAST, state.getValue(WEST)).setValue(SOUTH, state.getValue(NORTH)).setValue(WEST, state.getValue(EAST));
        case COUNTERCLOCKWISE_90:
            return state.setValue(NORTH, state.getValue(EAST)).setValue(EAST, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(WEST)).setValue(WEST, state.getValue(NORTH));
        case CLOCKWISE_90:
            return state.setValue(NORTH, state.getValue(WEST)).setValue(EAST, state.getValue(NORTH)).setValue(SOUTH, state.getValue(EAST)).setValue(WEST, state.getValue(SOUTH));
        default:
            return state;
        }
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        switch(mirror) {
        case LEFT_RIGHT:
            return state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
        case FRONT_BACK:
            return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
        default:
            return super.mirror(state, mirror);
        }
    }

    public static BooleanProperty getPropertyForFace(Direction direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }
}
