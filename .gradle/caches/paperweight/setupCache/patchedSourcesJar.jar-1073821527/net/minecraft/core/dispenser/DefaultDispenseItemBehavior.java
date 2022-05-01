package net.minecraft.core.dispenser;

import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
    @Override
    public final ItemStack dispense(BlockSource pointer, ItemStack stack) {
        ItemStack itemStack = this.execute(pointer, stack);
        this.playSound(pointer);
        this.playAnimation(pointer, pointer.getBlockState().getValue(DispenserBlock.FACING));
        return itemStack;
    }

    protected ItemStack execute(BlockSource pointer, ItemStack stack) {
        Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
        Position position = DispenserBlock.getDispensePosition(pointer);
        ItemStack itemStack = stack.split(1);
        spawnItem(pointer.getLevel(), itemStack, 6, direction, position);
        return stack;
    }

    public static void spawnItem(Level world, ItemStack stack, int speed, Direction side, Position pos) {
        double d = pos.x();
        double e = pos.y();
        double f = pos.z();
        if (side.getAxis() == Direction.Axis.Y) {
            e -= 0.125D;
        } else {
            e -= 0.15625D;
        }

        ItemEntity itemEntity = new ItemEntity(world, d, e, f, stack);
        double g = world.random.nextDouble() * 0.1D + 0.2D;
        itemEntity.setDeltaMovement(world.random.nextGaussian() * (double)0.0075F * (double)speed + (double)side.getStepX() * g, world.random.nextGaussian() * (double)0.0075F * (double)speed + (double)0.2F, world.random.nextGaussian() * (double)0.0075F * (double)speed + (double)side.getStepZ() * g);
        world.addFreshEntity(itemEntity);
    }

    protected void playSound(BlockSource pointer) {
        pointer.getLevel().levelEvent(1000, pointer.getPos(), 0);
    }

    protected void playAnimation(BlockSource pointer, Direction side) {
        pointer.getLevel().levelEvent(2000, pointer.getPos(), side.get3DDataValue());
    }
}
