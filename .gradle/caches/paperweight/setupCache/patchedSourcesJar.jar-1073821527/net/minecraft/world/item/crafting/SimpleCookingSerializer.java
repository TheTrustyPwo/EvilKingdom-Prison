package net.minecraft.world.item.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

public class SimpleCookingSerializer<T extends AbstractCookingRecipe> implements RecipeSerializer<T> {
    private final int defaultCookingTime;
    private final SimpleCookingSerializer.CookieBaker<T> factory;

    public SimpleCookingSerializer(SimpleCookingSerializer.CookieBaker<T> recipeFactory, int cookingTime) {
        this.defaultCookingTime = cookingTime;
        this.factory = recipeFactory;
    }

    @Override
    public T fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
        String string = GsonHelper.getAsString(jsonObject, "group", "");
        JsonElement jsonElement = (JsonElement)(GsonHelper.isArrayNode(jsonObject, "ingredient") ? GsonHelper.getAsJsonArray(jsonObject, "ingredient") : GsonHelper.getAsJsonObject(jsonObject, "ingredient"));
        Ingredient ingredient = Ingredient.fromJson(jsonElement);
        String string2 = GsonHelper.getAsString(jsonObject, "result");
        ResourceLocation resourceLocation2 = new ResourceLocation(string2);
        ItemStack itemStack = new ItemStack(Registry.ITEM.getOptional(resourceLocation2).orElseThrow(() -> {
            return new IllegalStateException("Item: " + string2 + " does not exist");
        }));
        float f = GsonHelper.getAsFloat(jsonObject, "experience", 0.0F);
        int i = GsonHelper.getAsInt(jsonObject, "cookingtime", this.defaultCookingTime);
        return this.factory.create(resourceLocation, string, ingredient, itemStack, f, i);
    }

    @Override
    public T fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf friendlyByteBuf) {
        String string = friendlyByteBuf.readUtf();
        Ingredient ingredient = Ingredient.fromNetwork(friendlyByteBuf);
        ItemStack itemStack = friendlyByteBuf.readItem();
        float f = friendlyByteBuf.readFloat();
        int i = friendlyByteBuf.readVarInt();
        return this.factory.create(resourceLocation, string, ingredient, itemStack, f, i);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, T recipe) {
        buf.writeUtf(recipe.group);
        recipe.ingredient.toNetwork(buf);
        buf.writeItem(recipe.result);
        buf.writeFloat(recipe.experience);
        buf.writeVarInt(recipe.cookingTime);
    }

    interface CookieBaker<T extends AbstractCookingRecipe> {
        T create(ResourceLocation id, String group, Ingredient input, ItemStack output, float experience, int cookTime);
    }
}
