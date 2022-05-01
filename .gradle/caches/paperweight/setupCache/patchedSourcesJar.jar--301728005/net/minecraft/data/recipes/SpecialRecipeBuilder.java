package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;

public class SpecialRecipeBuilder {
    final SimpleRecipeSerializer<?> serializer;

    public SpecialRecipeBuilder(SimpleRecipeSerializer<?> serializer) {
        this.serializer = serializer;
    }

    public static SpecialRecipeBuilder special(SimpleRecipeSerializer<?> serializer) {
        return new SpecialRecipeBuilder(serializer);
    }

    public void save(Consumer<FinishedRecipe> exporter, String recipeId) {
        exporter.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
            }

            @Override
            public RecipeSerializer<?> getType() {
                return SpecialRecipeBuilder.this.serializer;
            }

            @Override
            public ResourceLocation getId() {
                return new ResourceLocation(recipeId);
            }

            @Nullable
            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public ResourceLocation getAdvancementId() {
                return new ResourceLocation("");
            }
        });
    }
}
