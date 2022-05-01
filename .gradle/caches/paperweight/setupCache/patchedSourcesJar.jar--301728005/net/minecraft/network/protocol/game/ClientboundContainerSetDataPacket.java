package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundContainerSetDataPacket implements Packet<ClientGamePacketListener> {
    private final int containerId;
    private final int id;
    private final int value;

    public ClientboundContainerSetDataPacket(int syncId, int propertyId, int value) {
        this.containerId = syncId;
        this.id = propertyId;
        this.value = value;
    }

    public ClientboundContainerSetDataPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readUnsignedByte();
        this.id = buf.readShort();
        this.value = buf.readShort();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.containerId);
        buf.writeShort(this.id);
        buf.writeShort(this.value);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleContainerSetData(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getId() {
        return this.id;
    }

    public int getValue() {
        return this.value;
    }
}
