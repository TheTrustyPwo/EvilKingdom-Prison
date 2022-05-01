package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetCarriedItemPacket implements Packet<ClientGamePacketListener> {
    private final int slot;

    public ClientboundSetCarriedItemPacket(int slot) {
        this.slot = slot;
    }

    public ClientboundSetCarriedItemPacket(FriendlyByteBuf buf) {
        this.slot = buf.readByte();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.slot);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetCarriedItem(this);
    }

    public int getSlot() {
        return this.slot;
    }
}
