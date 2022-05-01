package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

public abstract class DiodeBlock extends HorizontalDirectionalBlock {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected DiodeBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return canSupportRigidBlock(world, pos.below());
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (!this.isLocked(world, pos, state)) {
            boolean bl = state.getValue(POWERED);
            boolean bl2 = this.shouldTurnOn(world, pos, state);
            if (bl && !bl2) {
                world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(false)), 2);
            } else if (!bl) {
                world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(true)), 2);
                if (!bl2) {
                    world.scheduleTick(pos, this, this.getDelay(state), TickPriority.VERY_HIGH);
                }
            }

        }
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getSignal(world, pos, direction);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (!state.getValue(POWERED)) {
            return 0;
        } else {
            return state.getValue(FACING) == direction ? this.getOutputSignal(world, pos, state) : 0;
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (state.canSurvive(world, pos)) {
            this.checkTickOnNeighbor(world, pos, state);
        } else {
            BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
            dropResources(state, world, pos, blockEntity);
            world.removeBlock(pos, false);

            for(Direction direction : Direction.values()) {
                world.updateNeighborsAt(pos.relative(direction), this);
            }

        }
    }

    protected void checkTickOnNeighbor(Level world, BlockPos pos, BlockState state) {
        if (!this.isLocked(world, pos, state)) {
            boolean bl = state.getValue(POWERED);
            boolean bl2 = this.shouldTurnOn(world, pos, state);
            if (bl != bl2 && !world.getBlockTicks().willTickThisTick(pos, this)) {
                TickPriority tickPriority = TickPriority.HIGH;
                if (this.shouldPrioritize(world, pos, state)) {
                    tickPriority = TickPriority.EXTREMELY_HIGH;
                } else if (bl) {
                    tickPriority = TickPriority.VERY_HIGH;
                }

                world.scheduleTick(pos, this, this.getDelay(state), tickPriority);
            }

        }
    }

    public boolean isLocked(LevelReader world, BlockPos pos, BlockState state) {
        return false;
    }

    protected boolean shouldTurnOn(Level world, BlockPos pos, BlockState state) {
        return this.getInputSignal(world, pos, state) > 0;
    }

    protected int getInputSignal(Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos blockPos = pos.relative(direction);
        int i = world.getSignal(blockPos, direction);
        if (i >= 15) {
            return i;
        } else {
            BlockState blockState = world.getBlockState(blockPos);
            return Math.max(i, blockState.is(Blocks.REDSTONE_WIRE) ? blockState.getValue(RedStoneWireBlock.POWER) : 0);
        }
    }

    protected int getAlternateSignal(LevelReader world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        Direction direction2 = direction.getClockWise();
        Direction direction3 = direction.getCounterClockWise();
        return Math.max(this.getAlternateSignalAt(world, pos.relative(direction2), direction2), this.getAlternateSignalAt(world, pos.relative(direction3), direction3));
    }

    protected int getAlternateSignalAt(LevelReader world, BlockPos pos, Direction dir) {
        BlockState blockState = world.getBlockState(pos);
        if (this.isAlternateInput(blockState)) {
            if (blockState.is(Blocks.REDSTONE_BLOCK)) {
                return 15;
            } else {
                return blockState.is(Blocks.REDSTONE_WIRE) ? blockState.getValue(RedStoneWireBlock.POWER) : world.getDirectSignal(pos, dir);
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (this.shouldTurnOn(world, pos, state)) {
            world.scheduleTick(pos, this, 1);
        }

    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        this.updateNeighborsInFront(world, pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            super.onRemove(state, world, pos, newState, moved);
            this.updateNeighborsInFront(world, pos, state);
        }
    }

    protected void updateNeighborsInFront(Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos blockPos = pos.relative(direction.getOpposite());
        world.neighborChanged(blockPos, this, pos);
        world.updateNeighborsAtExceptFromFacing(blockPos, this, direction);
    }

    protected boolean isAlternateInput(BlockState state) {
        return state.isSignalSource();
    }

    protected int getOutputSignal(BlockGetter world, BlockPos pos, BlockState state) {
        return 15;
    }

    public static boolean isDiode(BlockState state) {
        return state.getBlock() instanceof DiodeBlock;
    }

    public boolean shouldPrioritize(BlockGetter world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING).getOpposite();
        BlockState blockState = world.getBlockState(pos.relative(direction));
        return isDiode(blockState) && blockState.getValue(FACING) != direction;
    }

    protected abstract int getDelay(BlockState state);
}
