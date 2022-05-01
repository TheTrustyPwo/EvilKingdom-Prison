package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class JukeboxBlock extends BaseEntityBlock {

    public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;

    protected JukeboxBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState) ((BlockState) this.stateDefinition.any()).setValue(JukeboxBlock.HAS_RECORD, false));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        CompoundTag nbttagcompound = BlockItem.getBlockEntityData(itemStack);

        if (nbttagcompound != null && nbttagcompound.contains("RecordItem")) {
            world.setBlock(pos, (BlockState) state.setValue(JukeboxBlock.HAS_RECORD, true), 2);
        }

    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if ((Boolean) state.getValue(JukeboxBlock.HAS_RECORD)) {
            this.dropRecording(world, pos);
            state = (BlockState) state.setValue(JukeboxBlock.HAS_RECORD, false);
            world.setBlock(pos, state, 2);
            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    public void setRecord(LevelAccessor world, BlockPos pos, BlockState state, ItemStack stack) {
        BlockEntity tileentity = world.getBlockEntity(pos);

        if (tileentity instanceof JukeboxBlockEntity) {
            // CraftBukkit start - There can only be one
            stack = stack.copy();
            if (!stack.isEmpty()) {
                stack.setCount(1);
            }
            ((JukeboxBlockEntity) tileentity).setRecord(stack);
            // CraftBukkit end
            world.setBlock(pos, (BlockState) state.setValue(JukeboxBlock.HAS_RECORD, true), 2);
        }
    }

    public void dropRecording(Level world, BlockPos pos) {
        if (!world.isClientSide) {
            BlockEntity tileentity = world.getBlockEntity(pos);

            if (tileentity instanceof JukeboxBlockEntity) {
                JukeboxBlockEntity tileentityjukebox = (JukeboxBlockEntity) tileentity;
                ItemStack itemstack = tileentityjukebox.getRecord();

                if (!itemstack.isEmpty()) {
                    world.levelEvent(1010, pos, 0);
                    tileentityjukebox.clearContent();
                    float f = 0.7F;
                    double d0 = (double) (world.random.nextFloat() * 0.7F) + 0.15000000596046448D;
                    double d1 = (double) (world.random.nextFloat() * 0.7F) + 0.06000000238418579D + 0.6D;
                    double d2 = (double) (world.random.nextFloat() * 0.7F) + 0.15000000596046448D;
                    ItemStack itemstack1 = itemstack.copy();
                    ItemEntity entityitem = new ItemEntity(world, (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, itemstack1);

                    entityitem.setDefaultPickUpDelay();
                    world.addFreshEntity(entityitem);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            this.dropRecording(world, pos);
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JukeboxBlockEntity(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);

        if (tileentity instanceof JukeboxBlockEntity) {
            Item item = ((JukeboxBlockEntity) tileentity).getRecord().getItem();

            if (item instanceof RecordItem) {
                return ((RecordItem) item).getAnalogOutput();
            }
        }

        return 0;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(JukeboxBlock.HAS_RECORD);
    }
}
