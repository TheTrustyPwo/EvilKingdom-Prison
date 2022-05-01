package net.minecraft.data.recipes;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public interface RecipeBuilder {
    RecipeBuilder unlockedBy(String name, CriterionTriggerInstance conditions);

    RecipeBuilder group(@Nullable String group);

    Item getResult();

    void save(Consumer<FinishedRecipe> exporter, ResourceLocation recipeId);

    default void save(Consumer<FinishedRecipe> exporter) {
        this.save(exporter, getDefaultRecipeId(this.getResult()));
    }

    default void save(Consumer<FinishedRecipe> exporter, String recipePath) {
        ResourceLocation resourceLocation = getDefaultRecipeId(this.getResult());
        ResourceLocation resourceLocation2 = new ResourceLocation(recipePath);
        if (resourceLocation2.equals(resourceLocation)) {
            throw new IllegalStateException("Recipe " + recipePath + " should remove its 'save' argument as it is equal to default one");
        } else {
            this.save(exporter, resourceLocation2);
        }
    }

    static ResourceLocation getDefaultRecipeId(ItemLike item) {
        return Registry.ITEM.getKey(item.asItem());
    }
}
