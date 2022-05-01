package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundSetCarriedItemPacket implements Packet<ServerGamePacketListener> {
    private final int slot;

    public ServerboundSetCarriedItemPacket(int selectedSlot) {
        this.slot = selectedSlot;
    }

    public ServerboundSetCarriedItemPacket(FriendlyByteBuf buf) {
        this.slot = buf.readShort();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeShort(this.slot);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSetCarriedItem(this);
    }

    public int getSlot() {
        return this.slot;
    }
}
