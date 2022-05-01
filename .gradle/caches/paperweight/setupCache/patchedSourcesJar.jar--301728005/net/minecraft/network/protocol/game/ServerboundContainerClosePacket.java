package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundContainerClosePacket implements Packet<ServerGamePacketListener> {
    private final int containerId;

    public ServerboundContainerClosePacket(int syncId) {
        this.containerId = syncId;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleContainerClose(this);
    }

    public ServerboundContainerClosePacket(FriendlyByteBuf buf) {
        this.containerId = buf.readByte();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.containerId);
    }

    public int getContainerId() {
        return this.containerId;
    }
}
