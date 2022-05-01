package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundAcceptTeleportationPacket implements Packet<ServerGamePacketListener> {
    private final int id;

    public ServerboundAcceptTeleportationPacket(int teleportId) {
        this.id = teleportId;
    }

    public ServerboundAcceptTeleportationPacket(FriendlyByteBuf buf) {
        this.id = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleAcceptTeleportPacket(this);
    }

    public int getId() {
        return this.id;
    }
}
