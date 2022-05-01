package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

public class MinecartItem extends Item {
    private static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
        private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

        @Override
        public ItemStack execute(BlockSource pointer, ItemStack stack) {
            Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
            Level level = pointer.getLevel();
            double d = pointer.x() + (double)direction.getStepX() * 1.125D;
            double e = Math.floor(pointer.y()) + (double)direction.getStepY();
            double f = pointer.z() + (double)direction.getStepZ() * 1.125D;
            BlockPos blockPos = pointer.getPos().relative(direction);
            BlockState blockState = level.getBlockState(blockPos);
            RailShape railShape = blockState.getBlock() instanceof BaseRailBlock ? blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
            double g;
            if (blockState.is(BlockTags.RAILS)) {
                if (railShape.isAscending()) {
                    g = 0.6D;
                } else {
                    g = 0.1D;
                }
            } else {
                if (!blockState.isAir() || !level.getBlockState(blockPos.below()).is(BlockTags.RAILS)) {
                    return this.defaultDispenseItemBehavior.dispense(pointer, stack);
                }

                BlockState blockState2 = level.getBlockState(blockPos.below());
                RailShape railShape2 = blockState2.getBlock() instanceof BaseRailBlock ? blockState2.getValue(((BaseRailBlock)blockState2.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
                if (direction != Direction.DOWN && railShape2.isAscending()) {
                    g = -0.4D;
                } else {
                    g = -0.9D;
                }
            }

            AbstractMinecart abstractMinecart = AbstractMinecart.createMinecart(level, d, e + g, f, ((MinecartItem)stack.getItem()).type);
            if (stack.hasCustomHoverName()) {
                abstractMinecart.setCustomName(stack.getHoverName());
            }

            level.addFreshEntity(abstractMinecart);
            stack.shrink(1);
            return stack;
        }

        @Override
        protected void playSound(BlockSource pointer) {
            pointer.getLevel().levelEvent(1000, pointer.getPos(), 0);
        }
    };
    final AbstractMinecart.Type type;

    public MinecartItem(AbstractMinecart.Type type, Item.Properties settings) {
        super(settings);
        this.type = type;
        DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockState blockState = level.getBlockState(blockPos);
        if (!blockState.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        } else {
            ItemStack itemStack = context.getItemInHand();
            if (!level.isClientSide) {
                RailShape railShape = blockState.getBlock() instanceof BaseRailBlock ? blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
                double d = 0.0D;
                if (railShape.isAscending()) {
                    d = 0.5D;
                }

                AbstractMinecart abstractMinecart = AbstractMinecart.createMinecart(level, (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.0625D + d, (double)blockPos.getZ() + 0.5D, this.type);
                if (itemStack.hasCustomHoverName()) {
                    abstractMinecart.setCustomName(itemStack.getHoverName());
                }

                level.addFreshEntity(abstractMinecart);
                level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockPos);
            }

            itemStack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
