package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetChunkCacheRadiusPacket implements Packet<ClientGamePacketListener> {
    private final int radius;

    public ClientboundSetChunkCacheRadiusPacket(int distance) {
        this.radius = distance;
    }

    public ClientboundSetChunkCacheRadiusPacket(FriendlyByteBuf buf) {
        this.radius = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.radius);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetChunkCacheRadius(this);
    }

    public int getRadius() {
        return this.radius;
    }
}
