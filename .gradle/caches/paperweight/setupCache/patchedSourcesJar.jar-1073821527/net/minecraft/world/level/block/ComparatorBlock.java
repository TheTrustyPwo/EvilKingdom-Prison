package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;

public class ComparatorBlock extends DiodeBlock implements EntityBlock {
    public static final EnumProperty<ComparatorMode> MODE = BlockStateProperties.MODE_COMPARATOR;

    public ComparatorBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(MODE, ComparatorMode.COMPARE));
    }

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    @Override
    protected int getOutputSignal(BlockGetter world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)blockEntity).getOutputSignal() : 0;
    }

    private int calculateOutputSignal(Level world, BlockPos pos, BlockState state) {
        int i = this.getInputSignal(world, pos, state);
        if (i == 0) {
            return 0;
        } else {
            int j = this.getAlternateSignal(world, pos, state);
            if (j > i) {
                return 0;
            } else {
                return state.getValue(MODE) == ComparatorMode.SUBTRACT ? i - j : i;
            }
        }
    }

    @Override
    protected boolean shouldTurnOn(Level world, BlockPos pos, BlockState state) {
        int i = this.getInputSignal(world, pos, state);
        if (i == 0) {
            return false;
        } else {
            int j = this.getAlternateSignal(world, pos, state);
            if (i > j) {
                return true;
            } else {
                return i == j && state.getValue(MODE) == ComparatorMode.COMPARE;
            }
        }
    }

    @Override
    protected int getInputSignal(Level world, BlockPos pos, BlockState state) {
        int i = super.getInputSignal(world, pos, state);
        Direction direction = state.getValue(FACING);
        BlockPos blockPos = pos.relative(direction);
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.hasAnalogOutputSignal()) {
            i = blockState.getAnalogOutputSignal(world, blockPos);
        } else if (i < 15 && blockState.isRedstoneConductor(world, blockPos)) {
            blockPos = blockPos.relative(direction);
            blockState = world.getBlockState(blockPos);
            ItemFrame itemFrame = this.getItemFrame(world, direction, blockPos);
            int j = Math.max(itemFrame == null ? Integer.MIN_VALUE : itemFrame.getAnalogOutput(), blockState.hasAnalogOutputSignal() ? blockState.getAnalogOutputSignal(world, blockPos) : Integer.MIN_VALUE);
            if (j != Integer.MIN_VALUE) {
                i = j;
            }
        }

        return i;
    }

    @Nullable
    private ItemFrame getItemFrame(Level world, Direction facing, BlockPos pos) {
        List<ItemFrame> list = world.getEntitiesOfClass(ItemFrame.class, new AABB((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1)), (itemFrame) -> {
            return itemFrame != null && itemFrame.getDirection() == facing;
        });
        return list.size() == 1 ? list.get(0) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            state = state.cycle(MODE);
            float f = state.getValue(MODE) == ComparatorMode.SUBTRACT ? 0.55F : 0.5F;
            world.playSound(player, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, f);
            world.setBlock(pos, state, 2);
            this.refreshOutputState(world, pos, state);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    @Override
    protected void checkTickOnNeighbor(Level world, BlockPos pos, BlockState state) {
        if (!world.getBlockTicks().willTickThisTick(pos, this)) {
            int i = this.calculateOutputSignal(world, pos, state);
            BlockEntity blockEntity = world.getBlockEntity(pos);
            int j = blockEntity instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)blockEntity).getOutputSignal() : 0;
            if (i != j || state.getValue(POWERED) != this.shouldTurnOn(world, pos, state)) {
                TickPriority tickPriority = this.shouldPrioritize(world, pos, state) ? TickPriority.HIGH : TickPriority.NORMAL;
                world.scheduleTick(pos, this, 2, tickPriority);
            }

        }
    }

    private void refreshOutputState(Level world, BlockPos pos, BlockState state) {
        int i = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int j = 0;
        if (blockEntity instanceof ComparatorBlockEntity) {
            ComparatorBlockEntity comparatorBlockEntity = (ComparatorBlockEntity)blockEntity;
            j = comparatorBlockEntity.getOutputSignal();
            comparatorBlockEntity.setOutputSignal(i);
        }

        if (j != i || state.getValue(MODE) == ComparatorMode.COMPARE) {
            boolean bl = this.shouldTurnOn(world, pos, state);
            boolean bl2 = state.getValue(POWERED);
            if (bl2 && !bl) {
                world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(false)), 2);
            } else if (!bl2 && bl) {
                world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(true)), 2);
            }

            this.updateNeighborsInFront(world, pos, state);
        }

    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        this.refreshOutputState(world, pos, state);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        super.triggerEvent(state, world, pos, type, data);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(type, data);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComparatorBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, POWERED);
    }
}
