package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundPongResponsePacket implements Packet<ClientStatusPacketListener> {
    private final long time;

    public ClientboundPongResponsePacket(long startTime) {
        this.time = startTime;
    }

    public ClientboundPongResponsePacket(FriendlyByteBuf buf) {
        this.time = buf.readLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(this.time);
    }

    @Override
    public void handle(ClientStatusPacketListener listener) {
        listener.handlePongResponse(this);
    }

    public long getTime() {
        return this.time;
    }
}
