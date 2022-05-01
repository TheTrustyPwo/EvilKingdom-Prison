package net.minecraft.world.item.crafting;

import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;

public interface RecipeType<T extends Recipe<?>> {
    RecipeType<CraftingRecipe> CRAFTING = register("crafting");
    RecipeType<SmeltingRecipe> SMELTING = register("smelting");
    RecipeType<BlastingRecipe> BLASTING = register("blasting");
    RecipeType<SmokingRecipe> SMOKING = register("smoking");
    RecipeType<CampfireCookingRecipe> CAMPFIRE_COOKING = register("campfire_cooking");
    RecipeType<StonecutterRecipe> STONECUTTING = register("stonecutting");
    RecipeType<UpgradeRecipe> SMITHING = register("smithing");

    static <T extends Recipe<?>> RecipeType<T> register(String id) {
        return Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(id), new RecipeType<T>() {
            @Override
            public String toString() {
                return id;
            }
        });
    }

    default <C extends Container> Optional<T> tryMatch(Recipe<C> recipe, Level world, C inventory) {
        return recipe.matches(inventory, world) ? Optional.of((T)recipe) : Optional.empty();
    }
}
