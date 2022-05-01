package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public class ClientboundPlaceGhostRecipePacket implements Packet<ClientGamePacketListener> {
    private final int containerId;
    private final ResourceLocation recipe;

    public ClientboundPlaceGhostRecipePacket(int syncId, Recipe<?> recipe) {
        this.containerId = syncId;
        this.recipe = recipe.getId();
    }

    public ClientboundPlaceGhostRecipePacket(FriendlyByteBuf buf) {
        this.containerId = buf.readByte();
        this.recipe = buf.readResourceLocation();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.containerId);
        buf.writeResourceLocation(this.recipe);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePlaceRecipe(this);
    }

    public ResourceLocation getRecipe() {
        return this.recipe;
    }

    public int getContainerId() {
        return this.containerId;
    }
}
