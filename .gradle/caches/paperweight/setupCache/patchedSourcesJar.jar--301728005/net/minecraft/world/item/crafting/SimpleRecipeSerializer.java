package net.minecraft.world.item.crafting;

import com.google.gson.JsonObject;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class SimpleRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
    private final Function<ResourceLocation, T> constructor;

    public SimpleRecipeSerializer(Function<ResourceLocation, T> factory) {
        this.constructor = factory;
    }

    @Override
    public T fromJson(ResourceLocation id, JsonObject json) {
        return this.constructor.apply(id);
    }

    @Override
    public T fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        return this.constructor.apply(id);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, T recipe) {
    }
}
