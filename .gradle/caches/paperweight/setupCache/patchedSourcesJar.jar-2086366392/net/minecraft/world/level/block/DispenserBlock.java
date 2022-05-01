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
import net.minecraft.world.Container;
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
    public static final Map<Item, DispenseItemBehavior> DISPENSER_REGISTRY = (Map) Util.make(new Object2ObjectOpenHashMap(), (object2objectopenhashmap) -> {
        object2objectopenhashmap.defaultReturnValue(new DefaultDispenseItemBehavior());
    });
    private static final int TRIGGER_DURATION = 4;
    public static boolean eventFired = false; // CraftBukkit

    public static void registerBehavior(ItemLike provider, DispenseItemBehavior behavior) {
        DispenserBlock.DISPENSER_REGISTRY.put(provider.asItem(), behavior);
    }

    protected DispenserBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState) ((BlockState) ((BlockState) this.stateDefinition.any()).setValue(DispenserBlock.FACING, Direction.NORTH)).setValue(DispenserBlock.TRIGGERED, false));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity tileentity = world.getBlockEntity(pos);

            if (tileentity instanceof DispenserBlockEntity) {
                player.openMenu((DispenserBlockEntity) tileentity);
                if (tileentity instanceof DropperBlockEntity) {
                    player.awardStat(Stats.INSPECT_DROPPER);
                } else {
                    player.awardStat(Stats.INSPECT_DISPENSER);
                }
            }

            return InteractionResult.CONSUME;
        }
    }

    public void dispenseFrom(ServerLevel world, BlockPos pos) {
        BlockSourceImpl sourceblock = new BlockSourceImpl(world, pos);
        DispenserBlockEntity tileentitydispenser = (DispenserBlockEntity) sourceblock.getEntity();
        int i = tileentitydispenser.getRandomSlot();

        if (i < 0) {
            if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.handleBlockFailedDispenseEvent(world, pos)) {// Paper - BlockFailedDispenseEvent is called here
            world.levelEvent(1001, pos, 0);
            world.gameEvent(GameEvent.DISPENSE_FAIL, pos);
            } // Paper
        } else {
            ItemStack itemstack = tileentitydispenser.getItem(i);
            DispenseItemBehavior idispensebehavior = this.getDispenseMethod(itemstack);

            if (idispensebehavior != DispenseItemBehavior.NOOP) {
                if (!org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.handleBlockPreDispenseEvent(world, pos, itemstack, i)) return; // Paper - BlockPreDispenseEvent is called here
                DispenserBlock.eventFired = false; // CraftBukkit - reset event status
                tileentitydispenser.setItem(i, idispensebehavior.dispense(sourceblock, itemstack));
            }

        }
    }

    protected DispenseItemBehavior getDispenseMethod(ItemStack stack) {
        return (DispenseItemBehavior) DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem());
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        boolean flag1 = world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.above());
        boolean flag2 = (Boolean) state.getValue(DispenserBlock.TRIGGERED);

        if (flag1 && !flag2) {
            world.scheduleTick(pos, (Block) this, 4);
            world.setBlock(pos, (BlockState) state.setValue(DispenserBlock.TRIGGERED, true), 4);
        } else if (!flag1 && flag2) {
            world.setBlock(pos, (BlockState) state.setValue(DispenserBlock.TRIGGERED, false), 4);
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
        return (BlockState) this.defaultBlockState().setValue(DispenserBlock.FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomHoverName()) {
            BlockEntity tileentity = world.getBlockEntity(pos);

            if (tileentity instanceof DispenserBlockEntity) {
                ((DispenserBlockEntity) tileentity).setCustomName(itemStack.getHoverName());
            }
        }

    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity tileentity = world.getBlockEntity(pos);

            if (tileentity instanceof DispenserBlockEntity) {
                Containers.dropContents(world, pos, (Container) ((DispenserBlockEntity) tileentity));
                world.updateNeighbourForOutputSignal(pos, this);
            }

            super.onRemove(state, world, pos, newState, moved);
        }
    }

    public static Position getDispensePosition(BlockSource pointer) {
        Direction enumdirection = (Direction) pointer.getBlockState().getValue(DispenserBlock.FACING);
        double d0 = pointer.x() + 0.7D * (double) enumdirection.getStepX();
        double d1 = pointer.y() + 0.7D * (double) enumdirection.getStepY();
        double d2 = pointer.z() + 0.7D * (double) enumdirection.getStepZ();

        return new PositionImpl(d0, d1, d2);
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
        return (BlockState) state.setValue(DispenserBlock.FACING, rotation.rotate((Direction) state.getValue(DispenserBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation((Direction) state.getValue(DispenserBlock.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DispenserBlock.FACING, DispenserBlock.TRIGGERED);
    }
}
