package net.minecraft.world.level.block;

import com.google.common.base.MoreObjects;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripWireHookBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    protected static final int WIRE_DIST_MIN = 1;
    protected static final int WIRE_DIST_MAX = 42;
    private static final int RECHECK_PERIOD = 10;
    protected static final int AABB_OFFSET = 3;
    protected static final VoxelShape NORTH_AABB = Block.box(5.0D, 0.0D, 10.0D, 11.0D, 10.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(5.0D, 0.0D, 0.0D, 11.0D, 10.0D, 6.0D);
    protected static final VoxelShape WEST_AABB = Block.box(10.0D, 0.0D, 5.0D, 16.0D, 10.0D, 11.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 5.0D, 6.0D, 10.0D, 11.0D);

    public TripWireHookBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(ATTACHED, Boolean.valueOf(false)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        switch((Direction)state.getValue(FACING)) {
        case EAST:
        default:
            return EAST_AABB;
        case WEST:
            return WEST_AABB;
        case SOUTH:
            return SOUTH_AABB;
        case NORTH:
            return NORTH_AABB;
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos blockPos = pos.relative(direction.getOpposite());
        BlockState blockState = world.getBlockState(blockPos);
        return direction.getAxis().isHorizontal() && blockState.isFaceSturdy(world, blockPos, direction);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return direction.getOpposite() == state.getValue(FACING) && !state.canSurvive(world, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState blockState = this.defaultBlockState().setValue(POWERED, Boolean.valueOf(false)).setValue(ATTACHED, Boolean.valueOf(false));
        LevelReader levelReader = ctx.getLevel();
        BlockPos blockPos = ctx.getClickedPos();
        Direction[] directions = ctx.getNearestLookingDirections();

        for(Direction direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                Direction direction2 = direction.getOpposite();
                blockState = blockState.setValue(FACING, direction2);
                if (blockState.canSurvive(levelReader, blockPos)) {
                    return blockState;
                }
            }
        }

        return null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        this.calculateState(world, pos, state, false, false, -1, (BlockState)null);
    }

    public void calculateState(Level world, BlockPos pos, BlockState state, boolean beingRemoved, boolean bl, int i, @Nullable BlockState blockState) {
        Direction direction = state.getValue(FACING);
        boolean bl2 = state.getValue(ATTACHED);
        boolean bl3 = state.getValue(POWERED);
        boolean bl4 = !beingRemoved;
        boolean bl5 = false;
        int j = 0;
        BlockState[] blockStates = new BlockState[42];

        for(int k = 1; k < 42; ++k) {
            BlockPos blockPos = pos.relative(direction, k);
            BlockState blockState2 = world.getBlockState(blockPos);
            if (blockState2.is(Blocks.TRIPWIRE_HOOK)) {
                if (blockState2.getValue(FACING) == direction.getOpposite()) {
                    j = k;
                }
                break;
            }

            if (!blockState2.is(Blocks.TRIPWIRE) && k != i) {
                blockStates[k] = null;
                bl4 = false;
            } else {
                if (k == i) {
                    blockState2 = MoreObjects.firstNonNull(blockState, blockState2);
                }

                boolean bl6 = !blockState2.getValue(TripWireBlock.DISARMED);
                boolean bl7 = blockState2.getValue(TripWireBlock.POWERED);
                bl5 |= bl6 && bl7;
                blockStates[k] = blockState2;
                if (k == i) {
                    world.scheduleTick(pos, this, 10);
                    bl4 &= bl6;
                }
            }
        }

        bl4 &= j > 1;
        bl5 &= bl4;
        BlockState blockState3 = this.defaultBlockState().setValue(ATTACHED, Boolean.valueOf(bl4)).setValue(POWERED, Boolean.valueOf(bl5));
        if (j > 0) {
            BlockPos blockPos2 = pos.relative(direction, j);
            Direction direction2 = direction.getOpposite();
            world.setBlock(blockPos2, blockState3.setValue(FACING, direction2), 3);
            this.notifyNeighbors(world, blockPos2, direction2);
            this.playSound(world, blockPos2, bl4, bl5, bl2, bl3);
        }

        this.playSound(world, pos, bl4, bl5, bl2, bl3);
        if (!beingRemoved) {
            world.setBlock(pos, blockState3.setValue(FACING, direction), 3);
            if (bl) {
                this.notifyNeighbors(world, pos, direction);
            }
        }

        if (bl2 != bl4) {
            for(int l = 1; l < j; ++l) {
                BlockPos blockPos3 = pos.relative(direction, l);
                BlockState blockState4 = blockStates[l];
                if (blockState4 != null) {
                    world.setBlock(blockPos3, blockState4.setValue(ATTACHED, Boolean.valueOf(bl4)), 3);
                    if (!world.getBlockState(blockPos3).isAir()) {
                    }
                }
            }
        }

    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        this.calculateState(world, pos, state, false, true, -1, (BlockState)null);
    }

    private void playSound(Level world, BlockPos pos, boolean attached, boolean on, boolean detached, boolean off) {
        if (on && !off) {
            world.playSound((Player)null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.6F);
            world.gameEvent(GameEvent.BLOCK_PRESS, pos);
        } else if (!on && off) {
            world.playSound((Player)null, pos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.5F);
            world.gameEvent(GameEvent.BLOCK_UNPRESS, pos);
        } else if (attached && !detached) {
            world.playSound((Player)null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4F, 0.7F);
            world.gameEvent(GameEvent.BLOCK_ATTACH, pos);
        } else if (!attached && detached) {
            world.playSound((Player)null, pos, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4F, 1.2F / (world.random.nextFloat() * 0.2F + 0.9F));
            world.gameEvent(GameEvent.BLOCK_DETACH, pos);
        }

    }

    private void notifyNeighbors(Level world, BlockPos pos, Direction direction) {
        world.updateNeighborsAt(pos, this);
        world.updateNeighborsAt(pos.relative(direction.getOpposite()), this);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            boolean bl = state.getValue(ATTACHED);
            boolean bl2 = state.getValue(POWERED);
            if (bl || bl2) {
                this.calculateState(world, pos, state, true, false, -1, (BlockState)null);
            }

            if (bl2) {
                world.updateNeighborsAt(pos, this);
                world.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
            }

            super.onRemove(state, world, pos, newState, moved);
        }
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (!state.getValue(POWERED)) {
            return 0;
        } else {
            return state.getValue(FACING) == direction ? 15 : 0;
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, ATTACHED);
    }
}
