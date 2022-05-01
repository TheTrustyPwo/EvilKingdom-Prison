package net.minecraft.core.dispenser;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
// CraftBukkit start
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

public abstract class AbstractProjectileDispenseBehavior extends DefaultDispenseItemBehavior {

    public AbstractProjectileDispenseBehavior() {}

    @Override
    public ItemStack execute(BlockSource pointer, ItemStack stack) {
        ServerLevel worldserver = pointer.getLevel();
        Position iposition = DispenserBlock.getDispensePosition(pointer);
        Direction enumdirection = (Direction) pointer.getBlockState().getValue(DispenserBlock.FACING);
        Projectile iprojectile = this.getProjectile(worldserver, iposition, stack);

        // CraftBukkit start
        // iprojectile.shoot((double) enumdirection.getStepX(), (double) ((float) enumdirection.getStepY() + 0.1F), (double) enumdirection.getStepZ(), this.getPower(), this.getUncertainty());
        ItemStack itemstack1 = stack.split(1);
        org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(pointer.getPos().getX(), pointer.getPos().getY(), pointer.getPos().getZ());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector((double) enumdirection.getStepX(), (double) ((float) enumdirection.getStepY() + 0.1F), (double) enumdirection.getStepZ()));
        if (!DispenserBlock.eventFired) {
            worldserver.getCraftServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            stack.grow(1);
            return stack;
        }

        if (!event.getItem().equals(craftItem)) {
            stack.grow(1);
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            DispenseItemBehavior idispensebehavior = (DispenseItemBehavior) DispenserBlock.DISPENSER_REGISTRY.get(eventStack.getItem());
            if (idispensebehavior != DispenseItemBehavior.NOOP && idispensebehavior != this) {
                idispensebehavior.dispense(pointer, eventStack);
                return stack;
            }
        }

        iprojectile.shoot(event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ(), this.getPower(), this.getUncertainty());
        ((Entity) iprojectile).projectileSource = new org.bukkit.craftbukkit.v1_18_R2.projectiles.CraftBlockProjectileSource((DispenserBlockEntity) pointer.getEntity());
        // CraftBukkit end
        worldserver.addFreshEntity(iprojectile);
        // itemstack.shrink(1); // CraftBukkit - Handled during event processing
        return stack;
    }

    @Override
    protected void playSound(BlockSource pointer) {
        pointer.getLevel().levelEvent(1002, pointer.getPos(), 0);
    }

    protected abstract Projectile getProjectile(Level world, Position position, ItemStack stack);

    protected float getUncertainty() {
        return 6.0F;
    }

    protected float getPower() {
        return 1.1F;
    }
}
