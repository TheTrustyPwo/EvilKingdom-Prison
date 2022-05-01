package net.minecraft.world.inventory;

import net.minecraft.world.item.ItemStack;

public interface ContainerListener {
    void slotChanged(AbstractContainerMenu handler, int slotId, ItemStack stack);

    void dataChanged(AbstractContainerMenu handler, int property, int value);
}
