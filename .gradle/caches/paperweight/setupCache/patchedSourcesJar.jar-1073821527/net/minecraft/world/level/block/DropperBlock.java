package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSourceImpl;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class DropperBlock extends DispenserBlock {
    private static final DispenseItemBehavior DISPENSE_BEHAVIOUR = new DefaultDispenseItemBehavior();

    public DropperBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    protected DispenseItemBehavior getDispenseMethod(ItemStack stack) {
        return DISPENSE_BEHAVIOUR;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DropperBlockEntity(pos, state);
    }

    @Override
    public void dispenseFrom(ServerLevel world, BlockPos pos) {
        BlockSourceImpl blockSourceImpl = new BlockSourceImpl(world, pos);
        DispenserBlockEntity dispenserBlockEntity = blockSourceImpl.getEntity();
        int i = dispenserBlockEntity.getRandomSlot();
        if (i < 0) {
            world.levelEvent(1001, pos, 0);
        } else {
            ItemStack itemStack = dispenserBlockEntity.getItem(i);
            if (!itemStack.isEmpty()) {
                Direction direction = world.getBlockState(pos).getValue(FACING);
                Container container = HopperBlockEntity.getContainerAt(world, pos.relative(direction));
                ItemStack itemStack2;
                if (container == null) {
                    itemStack2 = DISPENSE_BEHAVIOUR.dispense(blockSourceImpl, itemStack);
                } else {
                    itemStack2 = HopperBlockEntity.addItem(dispenserBlockEntity, container, itemStack.copy().split(1), direction.getOpposite());
                    if (itemStack2.isEmpty()) {
                        itemStack2 = itemStack.copy();
                        itemStack2.shrink(1);
                    } else {
                        itemStack2 = itemStack.copy();
                    }
                }

                dispenserBlockEntity.setItem(i, itemStack2);
            }
        }
    }
}
