package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapExtendingRecipe extends ShapedRecipe {
    public MapExtendingRecipe(ResourceLocation id) {
        super(id, "", 3, 3, NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.FILLED_MAP), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER)), new ItemStack(Items.MAP));
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        if (!super.matches(inventory, world)) {
            return false;
        } else {
            ItemStack itemStack = ItemStack.EMPTY;

            for(int i = 0; i < inventory.getContainerSize() && itemStack.isEmpty(); ++i) {
                ItemStack itemStack2 = inventory.getItem(i);
                if (itemStack2.is(Items.FILLED_MAP)) {
                    itemStack = itemStack2;
                }
            }

            if (itemStack.isEmpty()) {
                return false;
            } else {
                MapItemSavedData mapItemSavedData = MapItem.getSavedData(itemStack, world);
                if (mapItemSavedData == null) {
                    return false;
                } else if (mapItemSavedData.isExplorationMap()) {
                    return false;
                } else {
                    return mapItemSavedData.scale < 4;
                }
            }
        }
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory) {
        ItemStack itemStack = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getContainerSize() && itemStack.isEmpty(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            if (itemStack2.is(Items.FILLED_MAP)) {
                itemStack = itemStack2;
            }
        }

        itemStack = itemStack.copy();
        itemStack.setCount(1);
        itemStack.getOrCreateTag().putInt("map_scale_direction", 1);
        return itemStack;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.MAP_EXTENDING;
    }
}
