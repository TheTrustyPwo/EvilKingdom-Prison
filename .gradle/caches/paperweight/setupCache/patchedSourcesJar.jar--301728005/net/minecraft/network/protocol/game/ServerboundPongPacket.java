package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundPongPacket implements Packet<ServerGamePacketListener> {
    private final int id;

    public ServerboundPongPacket(int parameter) {
        this.id = parameter;
    }

    public ServerboundPongPacket(FriendlyByteBuf buf) {
        this.id = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.id);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePong(this);
    }

    public int getId() {
        return this.id;
    }
}
