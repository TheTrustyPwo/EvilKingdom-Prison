package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public class ClientboundUpdateRecipesPacket implements Packet<ClientGamePacketListener> {
    private final List<Recipe<?>> recipes;

    public ClientboundUpdateRecipesPacket(Collection<Recipe<?>> recipes) {
        this.recipes = Lists.newArrayList(recipes);
    }

    public ClientboundUpdateRecipesPacket(FriendlyByteBuf buf) {
        this.recipes = buf.readList(ClientboundUpdateRecipesPacket::fromNetwork);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(this.recipes, ClientboundUpdateRecipesPacket::toNetwork);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleUpdateRecipes(this);
    }

    public List<Recipe<?>> getRecipes() {
        return this.recipes;
    }

    public static Recipe<?> fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation resourceLocation = buf.readResourceLocation();
        ResourceLocation resourceLocation2 = buf.readResourceLocation();
        return Registry.RECIPE_SERIALIZER.getOptional(resourceLocation).orElseThrow(() -> {
            return new IllegalArgumentException("Unknown recipe serializer " + resourceLocation);
        }).fromNetwork(resourceLocation2, buf);
    }

    public static <T extends Recipe<?>> void toNetwork(FriendlyByteBuf buf, T recipe) {
        buf.writeResourceLocation(Registry.RECIPE_SERIALIZER.getKey(recipe.getSerializer()));
        buf.writeResourceLocation(recipe.getId());
        recipe.getSerializer().toNetwork(buf, recipe);
    }
}
