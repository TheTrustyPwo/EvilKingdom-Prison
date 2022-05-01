package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundContainerButtonClickPacket implements Packet<ServerGamePacketListener> {
    private final int containerId;
    private final int buttonId;

    public ServerboundContainerButtonClickPacket(int syncId, int buttonId) {
        this.containerId = syncId;
        this.buttonId = buttonId;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleContainerButtonClick(this);
    }

    public ServerboundContainerButtonClickPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readByte();
        this.buttonId = buf.readByte();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.containerId);
        buf.writeByte(this.buttonId);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getButtonId() {
        return this.buttonId;
    }
}
