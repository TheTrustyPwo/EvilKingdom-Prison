package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderSizePacket implements Packet<ClientGamePacketListener> {
    private final double size;

    public ClientboundSetBorderSizePacket(WorldBorder worldBorder) {
        this.size = worldBorder.getLerpTarget();
    }

    public ClientboundSetBorderSizePacket(FriendlyByteBuf buf) {
        this.size = buf.readDouble();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.size);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetBorderSize(this);
    }

    public double getSize() {
        return this.size;
    }
}
