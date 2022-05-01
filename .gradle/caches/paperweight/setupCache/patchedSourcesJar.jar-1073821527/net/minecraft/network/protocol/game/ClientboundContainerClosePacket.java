package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundContainerClosePacket implements Packet<ClientGamePacketListener> {
    private final int containerId;

    public ClientboundContainerClosePacket(int syncId) {
        this.containerId = syncId;
    }

    public ClientboundContainerClosePacket(FriendlyByteBuf buf) {
        this.containerId = buf.readUnsignedByte();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.containerId);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleContainerClose(this);
    }

    public int getContainerId() {
        return this.containerId;
    }
}
