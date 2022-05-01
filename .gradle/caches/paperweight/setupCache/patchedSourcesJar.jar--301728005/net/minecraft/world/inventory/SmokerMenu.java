package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeType;

public class SmokerMenu extends AbstractFurnaceMenu {
    public SmokerMenu(int syncId, Inventory playerInventory) {
        super(MenuType.SMOKER, RecipeType.SMOKING, RecipeBookType.SMOKER, syncId, playerInventory);
    }

    public SmokerMenu(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(MenuType.SMOKER, RecipeType.SMOKING, RecipeBookType.SMOKER, syncId, playerInventory, inventory, propertyDelegate);
    }
}
