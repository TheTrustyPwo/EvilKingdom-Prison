package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundSetBeaconPacket implements Packet<ServerGamePacketListener> {
    private final int primary;
    private final int secondary;

    public ServerboundSetBeaconPacket(int primaryEffectId, int secondaryEffectId) {
        this.primary = primaryEffectId;
        this.secondary = secondaryEffectId;
    }

    public ServerboundSetBeaconPacket(FriendlyByteBuf buf) {
        this.primary = buf.readVarInt();
        this.secondary = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.primary);
        buf.writeVarInt(this.secondary);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSetBeaconPacket(this);
    }

    public int getPrimary() {
        return this.primary;
    }

    public int getSecondary() {
        return this.secondary;
    }
}
