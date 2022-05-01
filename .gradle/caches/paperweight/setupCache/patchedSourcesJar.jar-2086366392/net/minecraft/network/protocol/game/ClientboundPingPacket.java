package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundPingPacket implements Packet<ClientGamePacketListener> {
    private final int id;

    public ClientboundPingPacket(int parameter) {
        this.id = parameter;
    }

    public ClientboundPingPacket(FriendlyByteBuf buf) {
        this.id = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.id);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePing(this);
    }

    public int getId() {
        return this.id;
    }
}
