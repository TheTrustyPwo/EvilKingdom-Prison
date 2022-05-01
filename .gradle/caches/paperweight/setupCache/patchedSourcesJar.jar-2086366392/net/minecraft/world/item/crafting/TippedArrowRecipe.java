package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

public class TippedArrowRecipe extends CustomRecipe {
    public TippedArrowRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        if (inventory.getWidth() == 3 && inventory.getHeight() == 3) {
            for(int i = 0; i < inventory.getWidth(); ++i) {
                for(int j = 0; j < inventory.getHeight(); ++j) {
                    ItemStack itemStack = inventory.getItem(i + j * inventory.getWidth());
                    if (itemStack.isEmpty()) {
                        return false;
                    }

                    if (i == 1 && j == 1) {
                        if (!itemStack.is(Items.LINGERING_POTION)) {
                            return false;
                        }
                    } else if (!itemStack.is(Items.ARROW)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory) {
        ItemStack itemStack = inventory.getItem(1 + inventory.getWidth());
        if (!itemStack.is(Items.LINGERING_POTION)) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemStack2 = new ItemStack(Items.TIPPED_ARROW, 8);
            PotionUtils.setPotion(itemStack2, PotionUtils.getPotion(itemStack));
            PotionUtils.setCustomEffects(itemStack2, PotionUtils.getCustomEffects(itemStack));
            return itemStack2;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.TIPPED_ARROW;
    }
}
