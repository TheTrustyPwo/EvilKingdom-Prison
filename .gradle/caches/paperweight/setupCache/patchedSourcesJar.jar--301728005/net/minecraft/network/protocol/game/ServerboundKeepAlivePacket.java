package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundKeepAlivePacket implements Packet<ServerGamePacketListener> {
    private final long id;

    public ServerboundKeepAlivePacket(long id) {
        this.id = id;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleKeepAlive(this);
    }

    public ServerboundKeepAlivePacket(FriendlyByteBuf buf) {
        this.id = buf.readLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(this.id);
    }

    public long getId() {
        return this.id;
    }
}
