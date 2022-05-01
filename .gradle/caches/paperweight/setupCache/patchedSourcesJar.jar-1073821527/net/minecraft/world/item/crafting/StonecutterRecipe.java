package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class StonecutterRecipe extends SingleItemRecipe {
    public StonecutterRecipe(ResourceLocation id, String group, Ingredient input, ItemStack output) {
        super(RecipeType.STONECUTTING, RecipeSerializer.STONECUTTER, id, group, input, output);
    }

    @Override
    public boolean matches(Container inventory, Level world) {
        return this.ingredient.test(inventory.getItem(0));
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.STONECUTTER);
    }
}
