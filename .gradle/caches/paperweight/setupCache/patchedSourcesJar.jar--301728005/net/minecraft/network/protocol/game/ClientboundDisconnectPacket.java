package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundDisconnectPacket implements Packet<ClientGamePacketListener> {
    private final Component reason;

    public ClientboundDisconnectPacket(Component reason) {
        this.reason = reason;
    }

    public ClientboundDisconnectPacket(FriendlyByteBuf buf) {
        this.reason = buf.readComponent();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeComponent(this.reason);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleDisconnect(this);
    }

    public Component getReason() {
        return this.reason;
    }
}
