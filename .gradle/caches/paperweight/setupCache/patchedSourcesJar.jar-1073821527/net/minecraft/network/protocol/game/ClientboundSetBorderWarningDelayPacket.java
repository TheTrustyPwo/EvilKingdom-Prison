package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderWarningDelayPacket implements Packet<ClientGamePacketListener> {
    private final int warningDelay;

    public ClientboundSetBorderWarningDelayPacket(WorldBorder worldBorder) {
        this.warningDelay = worldBorder.getWarningTime();
    }

    public ClientboundSetBorderWarningDelayPacket(FriendlyByteBuf buf) {
        this.warningDelay = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.warningDelay);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetBorderWarningDelay(this);
    }

    public int getWarningDelay() {
        return this.warningDelay;
    }
}
