package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeType;

public class FurnaceMenu extends AbstractFurnaceMenu {
    public FurnaceMenu(int syncId, Inventory playerInventory) {
        super(MenuType.FURNACE, RecipeType.SMELTING, RecipeBookType.FURNACE, syncId, playerInventory);
    }

    public FurnaceMenu(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(MenuType.FURNACE, RecipeType.SMELTING, RecipeBookType.FURNACE, syncId, playerInventory, inventory, propertyDelegate);
    }
}
