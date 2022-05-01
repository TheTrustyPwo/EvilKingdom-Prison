package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundKeepAlivePacket implements Packet<ClientGamePacketListener> {
    private final long id;

    public ClientboundKeepAlivePacket(long id) {
        this.id = id;
    }

    public ClientboundKeepAlivePacket(FriendlyByteBuf buf) {
        this.id = buf.readLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(this.id);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleKeepAlive(this);
    }

    public long getId() {
        return this.id;
    }
}
