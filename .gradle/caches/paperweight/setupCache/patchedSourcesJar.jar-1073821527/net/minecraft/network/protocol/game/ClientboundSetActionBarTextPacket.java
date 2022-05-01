package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetActionBarTextPacket implements Packet<ClientGamePacketListener> {
    private final Component text;

    public ClientboundSetActionBarTextPacket(Component message) {
        this.text = message;
    }

    public ClientboundSetActionBarTextPacket(FriendlyByteBuf buf) {
        this.text = buf.readComponent();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeComponent(this.text);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.setActionBarText(this);
    }

    public Component getText() {
        return this.text;
    }
}
