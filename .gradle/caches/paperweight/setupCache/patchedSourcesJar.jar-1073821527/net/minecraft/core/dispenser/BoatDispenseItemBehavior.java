package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class BoatDispenseItemBehavior extends DefaultDispenseItemBehavior {
    private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
    private final Boat.Type type;

    public BoatDispenseItemBehavior(Boat.Type type) {
        this.type = type;
    }

    @Override
    public ItemStack execute(BlockSource pointer, ItemStack stack) {
        Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
        Level level = pointer.getLevel();
        double d = pointer.x() + (double)((float)direction.getStepX() * 1.125F);
        double e = pointer.y() + (double)((float)direction.getStepY() * 1.125F);
        double f = pointer.z() + (double)((float)direction.getStepZ() * 1.125F);
        BlockPos blockPos = pointer.getPos().relative(direction);
        double g;
        if (level.getFluidState(blockPos).is(FluidTags.WATER)) {
            g = 1.0D;
        } else {
            if (!level.getBlockState(blockPos).isAir() || !level.getFluidState(blockPos.below()).is(FluidTags.WATER)) {
                return this.defaultDispenseItemBehavior.dispense(pointer, stack);
            }

            g = 0.0D;
        }

        Boat boat = new Boat(level, d, e + g, f);
        boat.setType(this.type);
        boat.setYRot(direction.toYRot());
        level.addFreshEntity(boat);
        stack.shrink(1);
        return stack;
    }

    @Override
    protected void playSound(BlockSource pointer) {
        pointer.getLevel().levelEvent(1000, pointer.getPos(), 0);
    }
}
