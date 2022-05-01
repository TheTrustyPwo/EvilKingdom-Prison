package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeType;

public class BlastFurnaceMenu extends AbstractFurnaceMenu {
    public BlastFurnaceMenu(int syncId, Inventory playerInventory) {
        super(MenuType.BLAST_FURNACE, RecipeType.BLASTING, RecipeBookType.BLAST_FURNACE, syncId, playerInventory);
    }

    public BlastFurnaceMenu(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(MenuType.BLAST_FURNACE, RecipeType.BLASTING, RecipeBookType.BLAST_FURNACE, syncId, playerInventory, inventory, propertyDelegate);
    }
}
