package net.minecraft.world;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface WorldlyContainer extends Container {
    int[] getSlotsForFace(Direction side);

    boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir);

    boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir);
}
