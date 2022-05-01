package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.math.Vector3f;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedStoneWireBlock extends Block {
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST));
    protected static final int H = 1;
    protected static final int W = 3;
    protected static final int E = 13;
    protected static final int N = 3;
    protected static final int S = 13;
    private static final VoxelShape SHAPE_DOT = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D);
    private static final Map<Direction, VoxelShape> SHAPES_FLOOR = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, Block.box(3.0D, 0.0D, 0.0D, 13.0D, 1.0D, 13.0D), Direction.SOUTH, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 16.0D), Direction.EAST, Block.box(3.0D, 0.0D, 3.0D, 16.0D, 1.0D, 13.0D), Direction.WEST, Block.box(0.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D)));
    private static final Map<Direction, VoxelShape> SHAPES_UP = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, Shapes.or(SHAPES_FLOOR.get(Direction.NORTH), Block.box(3.0D, 0.0D, 0.0D, 13.0D, 16.0D, 1.0D)), Direction.SOUTH, Shapes.or(SHAPES_FLOOR.get(Direction.SOUTH), Block.box(3.0D, 0.0D, 15.0D, 13.0D, 16.0D, 16.0D)), Direction.EAST, Shapes.or(SHAPES_FLOOR.get(Direction.EAST), Block.box(15.0D, 0.0D, 3.0D, 16.0D, 16.0D, 13.0D)), Direction.WEST, Shapes.or(SHAPES_FLOOR.get(Direction.WEST), Block.box(0.0D, 0.0D, 3.0D, 1.0D, 16.0D, 13.0D))));
    private static final Map<BlockState, VoxelShape> SHAPES_CACHE = Maps.newHashMap();
    private static final Vec3[] COLORS = Util.make(new Vec3[16], (vec3s) -> {
        for(int i = 0; i <= 15; ++i) {
            float f = (float)i / 15.0F;
            float g = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
            float h = Mth.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float j = Mth.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
            vec3s[i] = new Vec3((double)g, (double)h, (double)j);
        }

    });
    private static final float PARTICLE_DENSITY = 0.2F;
    private final BlockState crossState;
    public boolean shouldSignal = true;

    public RedStoneWireBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, RedstoneSide.NONE).setValue(EAST, RedstoneSide.NONE).setValue(SOUTH, RedstoneSide.NONE).setValue(WEST, RedstoneSide.NONE).setValue(POWER, Integer.valueOf(0)));
        this.crossState = this.defaultBlockState().setValue(NORTH, RedstoneSide.SIDE).setValue(EAST, RedstoneSide.SIDE).setValue(SOUTH, RedstoneSide.SIDE).setValue(WEST, RedstoneSide.SIDE);

        for(BlockState blockState : this.getStateDefinition().getPossibleStates()) {
            if (blockState.getValue(POWER) == 0) {
                SHAPES_CACHE.put(blockState, this.calculateShape(blockState));
            }
        }

    }

    private VoxelShape calculateShape(BlockState state) {
        VoxelShape voxelShape = SHAPE_DOT;

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide redstoneSide = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (redstoneSide == RedstoneSide.SIDE) {
                voxelShape = Shapes.or(voxelShape, SHAPES_FLOOR.get(direction));
            } else if (redstoneSide == RedstoneSide.UP) {
                voxelShape = Shapes.or(voxelShape, SHAPES_UP.get(direction));
            }
        }

        return voxelShape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES_CACHE.get(state.setValue(POWER, Integer.valueOf(0)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.getConnectionState(ctx.getLevel(), this.crossState, ctx.getClickedPos());
    }

    private BlockState getConnectionState(BlockGetter world, BlockState state, BlockPos pos) {
        boolean bl = isDot(state);
        state = this.getMissingConnections(world, this.defaultBlockState().setValue(POWER, state.getValue(POWER)), pos);
        if (bl && isDot(state)) {
            return state;
        } else {
            boolean bl2 = state.getValue(NORTH).isConnected();
            boolean bl3 = state.getValue(SOUTH).isConnected();
            boolean bl4 = state.getValue(EAST).isConnected();
            boolean bl5 = state.getValue(WEST).isConnected();
            boolean bl6 = !bl2 && !bl3;
            boolean bl7 = !bl4 && !bl5;
            if (!bl5 && bl6) {
                state = state.setValue(WEST, RedstoneSide.SIDE);
            }

            if (!bl4 && bl6) {
                state = state.setValue(EAST, RedstoneSide.SIDE);
            }

            if (!bl2 && bl7) {
                state = state.setValue(NORTH, RedstoneSide.SIDE);
            }

            if (!bl3 && bl7) {
                state = state.setValue(SOUTH, RedstoneSide.SIDE);
            }

            return state;
        }
    }

    private BlockState getMissingConnections(BlockGetter world, BlockState state, BlockPos pos) {
        boolean bl = !world.getBlockState(pos.above()).isRedstoneConductor(world, pos);

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            if (!state.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()) {
                RedstoneSide redstoneSide = this.getConnectingSide(world, pos, direction, bl);
                state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), redstoneSide);
            }
        }

        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state;
        } else if (direction == Direction.UP) {
            return this.getConnectionState(world, state, pos);
        } else {
            RedstoneSide redstoneSide = this.getConnectingSide(world, pos, direction);
            return redstoneSide.isConnected() == state.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() && !isCross(state) ? state.setValue(PROPERTY_BY_DIRECTION.get(direction), redstoneSide) : this.getConnectionState(world, this.crossState.setValue(POWER, state.getValue(POWER)).setValue(PROPERTY_BY_DIRECTION.get(direction), redstoneSide), pos);
        }
    }

    private static boolean isCross(BlockState state) {
        return state.getValue(NORTH).isConnected() && state.getValue(SOUTH).isConnected() && state.getValue(EAST).isConnected() && state.getValue(WEST).isConnected();
    }

    private static boolean isDot(BlockState state) {
        return !state.getValue(NORTH).isConnected() && !state.getValue(SOUTH).isConnected() && !state.getValue(EAST).isConnected() && !state.getValue(WEST).isConnected();
    }

    @Override
    public void updateIndirectNeighbourShapes(BlockState state, LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide redstoneSide = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (redstoneSide != RedstoneSide.NONE && !world.getBlockState(mutableBlockPos.setWithOffset(pos, direction)).is(this)) {
                mutableBlockPos.move(Direction.DOWN);
                BlockState blockState = world.getBlockState(mutableBlockPos);
                if (!blockState.is(Blocks.OBSERVER)) {
                    BlockPos blockPos = mutableBlockPos.relative(direction.getOpposite());
                    BlockState blockState2 = blockState.updateShape(direction.getOpposite(), world.getBlockState(blockPos), world, mutableBlockPos, blockPos);
                    updateOrDestroy(blockState, blockState2, world, mutableBlockPos, flags, maxUpdateDepth);
                }

                mutableBlockPos.setWithOffset(pos, direction).move(Direction.UP);
                BlockState blockState3 = world.getBlockState(mutableBlockPos);
                if (!blockState3.is(Blocks.OBSERVER)) {
                    BlockPos blockPos2 = mutableBlockPos.relative(direction.getOpposite());
                    BlockState blockState4 = blockState3.updateShape(direction.getOpposite(), world.getBlockState(blockPos2), world, mutableBlockPos, blockPos2);
                    updateOrDestroy(blockState3, blockState4, world, mutableBlockPos, flags, maxUpdateDepth);
                }
            }
        }

    }

    private RedstoneSide getConnectingSide(BlockGetter world, BlockPos pos, Direction direction) {
        return this.getConnectingSide(world, pos, direction, !world.getBlockState(pos.above()).isRedstoneConductor(world, pos));
    }

    private RedstoneSide getConnectingSide(BlockGetter world, BlockPos pos, Direction direction, boolean bl) {
        BlockPos blockPos = pos.relative(direction);
        BlockState blockState = world.getBlockState(blockPos);
        if (bl) {
            boolean bl2 = this.canSurviveOn(world, blockPos, blockState);
            if (bl2 && shouldConnectTo(world.getBlockState(blockPos.above()))) {
                if (blockState.isFaceSturdy(world, blockPos, direction.getOpposite())) {
                    return RedstoneSide.UP;
                }

                return RedstoneSide.SIDE;
            }
        }

        return !shouldConnectTo(blockState, direction) && (blockState.isRedstoneConductor(world, blockPos) || !shouldConnectTo(world.getBlockState(blockPos.below()))) ? RedstoneSide.NONE : RedstoneSide.SIDE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos blockPos = pos.below();
        BlockState blockState = world.getBlockState(blockPos);
        return this.canSurviveOn(world, blockPos, blockState);
    }

    private boolean canSurviveOn(BlockGetter world, BlockPos pos, BlockState floor) {
        return floor.isFaceSturdy(world, pos, Direction.UP) || floor.is(Blocks.HOPPER);
    }

    private void updatePowerStrength(Level world, BlockPos pos, BlockState state) {
        int i = this.calculateTargetStrength(world, pos);
        if (state.getValue(POWER) != i) {
            if (world.getBlockState(pos) == state) {
                world.setBlock(pos, state.setValue(POWER, Integer.valueOf(i)), 2);
            }

            Set<BlockPos> set = Sets.newHashSet();
            set.add(pos);

            for(Direction direction : Direction.values()) {
                set.add(pos.relative(direction));
            }

            for(BlockPos blockPos : set) {
                world.updateNeighborsAt(blockPos, this);
            }
        }

    }

    private int calculateTargetStrength(Level world, BlockPos pos) {
        this.shouldSignal = false;
        int i = world.getBestNeighborSignal(pos);
        this.shouldSignal = true;
        int j = 0;
        if (i < 15) {
            for(Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos blockPos = pos.relative(direction);
                BlockState blockState = world.getBlockState(blockPos);
                j = Math.max(j, this.getWireSignal(blockState));
                BlockPos blockPos2 = pos.above();
                if (blockState.isRedstoneConductor(world, blockPos) && !world.getBlockState(blockPos2).isRedstoneConductor(world, blockPos2)) {
                    j = Math.max(j, this.getWireSignal(world.getBlockState(blockPos.above())));
                } else if (!blockState.isRedstoneConductor(world, blockPos)) {
                    j = Math.max(j, this.getWireSignal(world.getBlockState(blockPos.below())));
                }
            }
        }

        return Math.max(i, j - 1);
    }

    private int getWireSignal(BlockState state) {
        return state.is(this) ? state.getValue(POWER) : 0;
    }

    private void checkCornerChangeAt(Level world, BlockPos pos) {
        if (world.getBlockState(pos).is(this)) {
            world.updateNeighborsAt(pos, this);

            for(Direction direction : Direction.values()) {
                world.updateNeighborsAt(pos.relative(direction), this);
            }

        }
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock()) && !world.isClientSide) {
            this.updatePowerStrength(world, pos, state);

            for(Direction direction : Direction.Plane.VERTICAL) {
                world.updateNeighborsAt(pos.relative(direction), this);
            }

            this.updateNeighborsOfNeighboringWires(world, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            super.onRemove(state, world, pos, newState, moved);
            if (!world.isClientSide) {
                for(Direction direction : Direction.values()) {
                    world.updateNeighborsAt(pos.relative(direction), this);
                }

                this.updatePowerStrength(world, pos, state);
                this.updateNeighborsOfNeighboringWires(world, pos);
            }
        }
    }

    private void updateNeighborsOfNeighboringWires(Level world, BlockPos pos) {
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            this.checkCornerChangeAt(world, pos.relative(direction));
        }

        for(Direction direction2 : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos = pos.relative(direction2);
            if (world.getBlockState(blockPos).isRedstoneConductor(world, blockPos)) {
                this.checkCornerChangeAt(world, blockPos.above());
            } else {
                this.checkCornerChangeAt(world, blockPos.below());
            }
        }

    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (!world.isClientSide) {
            if (state.canSurvive(world, pos)) {
                this.updatePowerStrength(world, pos, state);
            } else {
                dropResources(state, world, pos);
                world.removeBlock(pos, false);
            }

        }
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return !this.shouldSignal ? 0 : state.getSignal(world, pos, direction);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (this.shouldSignal && direction != Direction.DOWN) {
            int i = state.getValue(POWER);
            if (i == 0) {
                return 0;
            } else {
                return direction != Direction.UP && !this.getConnectionState(world, state, pos).getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite())).isConnected() ? 0 : i;
            }
        } else {
            return 0;
        }
    }

    protected static boolean shouldConnectTo(BlockState state) {
        return shouldConnectTo(state, (Direction)null);
    }

    protected static boolean shouldConnectTo(BlockState state, @Nullable Direction dir) {
        if (state.is(Blocks.REDSTONE_WIRE)) {
            return true;
        } else if (state.is(Blocks.REPEATER)) {
            Direction direction = state.getValue(RepeaterBlock.FACING);
            return direction == dir || direction.getOpposite() == dir;
        } else if (state.is(Blocks.OBSERVER)) {
            return dir == state.getValue(ObserverBlock.FACING);
        } else {
            return state.isSignalSource() && dir != null;
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return this.shouldSignal;
    }

    public static int getColorForPower(int powerLevel) {
        Vec3 vec3 = COLORS[powerLevel];
        return Mth.color((float)vec3.x(), (float)vec3.y(), (float)vec3.z());
    }

    private void spawnParticlesAlongLine(Level world, Random random, BlockPos pos, Vec3 color, Direction direction, Direction direction2, float f, float g) {
        float h = g - f;
        if (!(random.nextFloat() >= 0.2F * h)) {
            float i = 0.4375F;
            float j = f + h * random.nextFloat();
            double d = 0.5D + (double)(0.4375F * (float)direction.getStepX()) + (double)(j * (float)direction2.getStepX());
            double e = 0.5D + (double)(0.4375F * (float)direction.getStepY()) + (double)(j * (float)direction2.getStepY());
            double k = 0.5D + (double)(0.4375F * (float)direction.getStepZ()) + (double)(j * (float)direction2.getStepZ());
            world.addParticle(new DustParticleOptions(new Vector3f(color), 1.0F), (double)pos.getX() + d, (double)pos.getY() + e, (double)pos.getZ() + k, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
        int i = state.getValue(POWER);
        if (i != 0) {
            for(Direction direction : Direction.Plane.HORIZONTAL) {
                RedstoneSide redstoneSide = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
                switch(redstoneSide) {
                case UP:
                    this.spawnParticlesAlongLine(world, random, pos, COLORS[i], direction, Direction.UP, -0.5F, 0.5F);
                case SIDE:
                    this.spawnParticlesAlongLine(world, random, pos, COLORS[i], Direction.DOWN, direction, 0.0F, 0.5F);
                    break;
                case NONE:
                default:
                    this.spawnParticlesAlongLine(world, random, pos, COLORS[i], Direction.DOWN, direction, 0.0F, 0.3F);
                }
            }

        }
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            if (isCross(state) || isDot(state)) {
                BlockState blockState = isCross(state) ? this.defaultBlockState() : this.crossState;
                blockState = blockState.setValue(POWER, state.getValue(POWER));
                blockState = this.getConnectionState(world, blockState, pos);
                if (blockState != state) {
                    world.setBlock(pos, blockState, 3);
                    this.updatesOnShapeChange(world, pos, state, blockState);
                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.PASS;
        }
    }

    private void updatesOnShapeChange(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos = pos.relative(direction);
            if (oldState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() != newState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() && world.getBlockState(blockPos).isRedstoneConductor(world, blockPos)) {
                world.updateNeighborsAtExceptFromFacing(blockPos, newState.getBlock(), direction.getOpposite());
            }
        }

    }
}
