package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public class ServerboundPlaceRecipePacket implements Packet<ServerGamePacketListener> {
    private final int containerId;
    private final ResourceLocation recipe;
    private final boolean shiftDown;

    public ServerboundPlaceRecipePacket(int syncId, Recipe<?> recipe, boolean craftAll) {
        this.containerId = syncId;
        this.recipe = recipe.getId();
        this.shiftDown = craftAll;
    }

    public ServerboundPlaceRecipePacket(FriendlyByteBuf buf) {
        this.containerId = buf.readByte();
        this.recipe = buf.readResourceLocation();
        this.shiftDown = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.containerId);
        buf.writeResourceLocation(this.recipe);
        buf.writeBoolean(this.shiftDown);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePlaceRecipe(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public ResourceLocation getRecipe() {
        return this.recipe;
    }

    public boolean isShiftDown() {
        return this.shiftDown;
    }
}
