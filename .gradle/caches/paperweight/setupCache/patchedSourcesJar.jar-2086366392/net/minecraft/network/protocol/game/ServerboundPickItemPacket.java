package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundPickItemPacket implements Packet<ServerGamePacketListener> {
    private final int slot;

    public ServerboundPickItemPacket(int slot) {
        this.slot = slot;
    }

    public ServerboundPickItemPacket(FriendlyByteBuf buf) {
        this.slot = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.slot);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePickItem(this);
    }

    public int getSlot() {
        return this.slot;
    }
}
