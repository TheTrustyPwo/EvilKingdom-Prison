package net.minecraft.core.dispenser;

import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
// CraftBukkit start
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftVector;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
    private Direction enumdirection; // Paper

    public DefaultDispenseItemBehavior() {}

    @Override
    public final ItemStack dispense(BlockSource pointer, ItemStack stack) {
        enumdirection = pointer.getBlockState().getValue(DispenserBlock.FACING); // Paper - cache facing direction
        ItemStack itemstack1 = this.execute(pointer, stack);

        this.playSound(pointer);
        this.playAnimation(pointer, enumdirection); // Paper - cache facing direction
        return itemstack1;
    }

    protected ItemStack execute(BlockSource pointer, ItemStack stack) {
        // Paper - cached enum direction
        Position iposition = DispenserBlock.getDispensePosition(pointer);
        ItemStack itemstack1 = stack.split(1);

        // CraftBukkit start
        if (!DefaultDispenseItemBehavior.spawnItem(pointer.getLevel(), itemstack1, 6, enumdirection, pointer)) {
            stack.grow(1);
        }
        // CraftBukkit end
        return stack;
    }

    // CraftBukkit start - void -> boolean return, IPosition -> ISourceBlock last argument
    public static boolean spawnItem(Level world, ItemStack itemstack, int i, Direction enumdirection, BlockSource isourceblock) {
        if (itemstack.isEmpty()) return true;
        Position iposition = DispenserBlock.getDispensePosition(isourceblock);
        // CraftBukkit end
        double d0 = iposition.x();
        double d1 = iposition.y();
        double d2 = iposition.z();

        if (enumdirection.getAxis() == Direction.Axis.Y) {
            d1 -= 0.125D;
        } else {
            d1 -= 0.15625D;
        }

        ItemEntity entityitem = new ItemEntity(world, d0, d1, d2, itemstack);
        double d3 = world.random.nextDouble() * 0.1D + 0.2D;

        entityitem.setDeltaMovement(world.random.nextGaussian() * 0.007499999832361937D * (double) i + (double) enumdirection.getStepX() * d3, world.random.nextGaussian() * 0.007499999832361937D * (double) i + 0.20000000298023224D, world.random.nextGaussian() * 0.007499999832361937D * (double) i + (double) enumdirection.getStepZ() * d3);

        // CraftBukkit start
        org.bukkit.block.Block block = world.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), CraftVector.toBukkit(entityitem.getDeltaMovement()));
        if (!DispenserBlock.eventFired) {
            world.getCraftServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            return false;
        }

        entityitem.setItem(CraftItemStack.asNMSCopy(event.getItem()));
        entityitem.setDeltaMovement(CraftVector.toNMS(event.getVelocity()));

        if (!event.getItem().getType().equals(craftItem.getType())) {
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            DispenseItemBehavior idispensebehavior = (DispenseItemBehavior) DispenserBlock.DISPENSER_REGISTRY.get(eventStack.getItem());
            if (idispensebehavior != DispenseItemBehavior.NOOP && idispensebehavior.getClass() != DefaultDispenseItemBehavior.class) {
                idispensebehavior.dispense(isourceblock, eventStack);
            } else {
                world.addFreshEntity(entityitem);
            }
            return false;
        }

        world.addFreshEntity(entityitem);

        return true;
        // CraftBukkit end
    }

    protected void playSound(BlockSource pointer) {
        pointer.getLevel().levelEvent(1000, pointer.getPos(), 0);
    }

    protected void playAnimation(BlockSource pointer, Direction side) {
        pointer.getLevel().levelEvent(2000, pointer.getPos(), side.get3DDataValue());
    }
}
