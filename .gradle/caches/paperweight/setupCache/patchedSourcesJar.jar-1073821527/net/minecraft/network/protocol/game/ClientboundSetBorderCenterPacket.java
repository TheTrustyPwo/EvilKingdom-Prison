package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderCenterPacket implements Packet<ClientGamePacketListener> {
    private final double newCenterX;
    private final double newCenterZ;

    public ClientboundSetBorderCenterPacket(WorldBorder worldBorder) {
        this.newCenterX = worldBorder.getCenterX();
        this.newCenterZ = worldBorder.getCenterZ();
    }

    public ClientboundSetBorderCenterPacket(FriendlyByteBuf buf) {
        this.newCenterX = buf.readDouble();
        this.newCenterZ = buf.readDouble();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.newCenterX);
        buf.writeDouble(this.newCenterZ);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetBorderCenter(this);
    }

    public double getNewCenterZ() {
        return this.newCenterZ;
    }

    public double getNewCenterX() {
        return this.newCenterX;
    }
}
