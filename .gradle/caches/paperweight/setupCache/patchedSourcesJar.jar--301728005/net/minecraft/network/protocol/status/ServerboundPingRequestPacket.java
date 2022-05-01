package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundPingRequestPacket implements Packet<ServerStatusPacketListener> {
    private final long time;

    public ServerboundPingRequestPacket(long startTime) {
        this.time = startTime;
    }

    public ServerboundPingRequestPacket(FriendlyByteBuf buf) {
        this.time = buf.readLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(this.time);
    }

    @Override
    public void handle(ServerStatusPacketListener listener) {
        listener.handlePingRequest(this);
    }

    public long getTime() {
        return this.time;
    }
}
