package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderLerpSizePacket implements Packet<ClientGamePacketListener> {
    private final double oldSize;
    private final double newSize;
    private final long lerpTime;

    public ClientboundSetBorderLerpSizePacket(WorldBorder worldBorder) {
        this.oldSize = worldBorder.getSize();
        this.newSize = worldBorder.getLerpTarget();
        this.lerpTime = worldBorder.getLerpRemainingTime();
    }

    public ClientboundSetBorderLerpSizePacket(FriendlyByteBuf buf) {
        this.oldSize = buf.readDouble();
        this.newSize = buf.readDouble();
        this.lerpTime = buf.readVarLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.oldSize);
        buf.writeDouble(this.newSize);
        buf.writeVarLong(this.lerpTime);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetBorderLerpSize(this);
    }

    public double getOldSize() {
        return this.oldSize;
    }

    public double getNewSize() {
        return this.newSize;
    }

    public long getLerpTime() {
        return this.lerpTime;
    }
}
