package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.BlockSourceImpl;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.PositionImpl;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class DispenserBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final Map<Item, DispenseItemBehavior> DISPENSER_REGISTRY = Util.make(new Object2ObjectOpenHashMap<>(), (map) -> {
        map.defaultReturnValue(new DefaultDispenseItemBehavior());
    });
    private static final int TRIGGER_DURATION = 4;

    public static void registerBehavior(ItemLike provider, DispenseItemBehavior behavior) {
        DISPENSER_REGISTRY.put(provider.asItem(), behavior);
    }

    protected DispenserBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, Boolean.valueOf(false)));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DispenserBlockEntity) {
                player.openMenu((DispenserBlockEntity)blockEntity);
                if (blockEntity instanceof DropperBlockEntity) {
                    player.awardStat(Stats.INSPECT_DROPPER);
                } else {
                    player.awardStat(Stats.INSPECT_DISPENSER);
                }
            }

            return InteractionResult.CONSUME;
        }
    }

    public void dispenseFrom(ServerLevel world, BlockPos pos) {
        BlockSourceImpl blockSourceImpl = new BlockSourceImpl(world, pos);
        DispenserBlockEntity dispenserBlockEntity = blockSourceImpl.getEntity();
        int i = dispenserBlockEntity.getRandomSlot();
        if (i < 0) {
            world.levelEvent(1001, pos, 0);
            world.gameEvent(GameEvent.DISPENSE_FAIL, pos);
        } else {
            ItemStack itemStack = dispenserBlockEntity.getItem(i);
            DispenseItemBehavior dispenseItemBehavior = this.getDispenseMethod(itemStack);
            if (dispenseItemBehavior != DispenseItemBehavior.NOOP) {
                dispenserBlockEntity.setItem(i, dispenseItemBehavior.dispense(blockSourceImpl, itemStack));
            }

        }
    }

    protected DispenseItemBehavior getDispenseMethod(ItemStack stack) {
        return DISPENSER_REGISTRY.get(stack.getItem());
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        boolean bl = world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.above());
        boolean bl2 = state.getValue(TRIGGERED);
        if (bl && !bl2) {
            world.scheduleTick(pos, this, 4);
            world.setBlock(pos, state.setValue(TRIGGERED, Boolean.valueOf(true)), 4);
        } else if (!bl && bl2) {
            world.setBlock(pos, state.setValue(TRIGGERED, Boolean.valueOf(false)), 4);
        }

    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        this.dispenseFrom(world, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DispenserBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomHoverName()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DispenserBlockEntity) {
                ((DispenserBlockEntity)blockEntity).setCustomName(itemStack.getHoverName());
            }
        }

    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DispenserBlockEntity) {
                Containers.dropContents(world, pos, (DispenserBlockEntity)blockEntity);
                world.updateNeighbourForOutputSignal(pos, this);
            }

            super.onRemove(state, world, pos, newState, moved);
        }
    }

    public static Position getDispensePosition(BlockSource pointer) {
        Direction direction = pointer.getBlockState().getValue(FACING);
        double d = pointer.x() + 0.7D * (double)direction.getStepX();
        double e = pointer.y() + 0.7D * (double)direction.getStepY();
        double f = pointer.z() + 0.7D * (double)direction.getStepZ();
        return new PositionImpl(d, e, f);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        builder.add(FACING, TRIGGERED);
    }
}
