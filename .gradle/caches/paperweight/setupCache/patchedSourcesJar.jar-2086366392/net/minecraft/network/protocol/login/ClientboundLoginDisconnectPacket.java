package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundLoginDisconnectPacket implements Packet<ClientLoginPacketListener> {
    private final Component reason;

    public ClientboundLoginDisconnectPacket(Component reason) {
        this.reason = reason;
    }

    public ClientboundLoginDisconnectPacket(FriendlyByteBuf buf) {
        this.reason = Component.Serializer.fromJsonLenient(buf.readUtf(262144));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeComponent(this.reason);
    }

    @Override
    public void handle(ClientLoginPacketListener listener) {
        listener.handleDisconnect(this);
    }

    public Component getReason() {
        return this.reason;
    }
}
