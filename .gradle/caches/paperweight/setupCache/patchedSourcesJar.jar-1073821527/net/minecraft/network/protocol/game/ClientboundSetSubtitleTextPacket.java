package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetSubtitleTextPacket implements Packet<ClientGamePacketListener> {
    private final Component text;

    public ClientboundSetSubtitleTextPacket(Component subtitle) {
        this.text = subtitle;
    }

    public ClientboundSetSubtitleTextPacket(FriendlyByteBuf buf) {
        this.text = buf.readComponent();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeComponent(this.text);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.setSubtitleText(this);
    }

    public Component getText() {
        return this.text;
    }
}
