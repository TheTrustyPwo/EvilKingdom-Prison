package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetTitlesAnimationPacket implements Packet<ClientGamePacketListener> {
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    public ClientboundSetTitlesAnimationPacket(int fadeInTicks, int remainTicks, int fadeOutTicks) {
        this.fadeIn = fadeInTicks;
        this.stay = remainTicks;
        this.fadeOut = fadeOutTicks;
    }

    public ClientboundSetTitlesAnimationPacket(FriendlyByteBuf buf) {
        this.fadeIn = buf.readInt();
        this.stay = buf.readInt();
        this.fadeOut = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.fadeIn);
        buf.writeInt(this.stay);
        buf.writeInt(this.fadeOut);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.setTitlesAnimation(this);
    }

    public int getFadeIn() {
        return this.fadeIn;
    }

    public int getStay() {
        return this.stay;
    }

    public int getFadeOut() {
        return this.fadeOut;
    }
}
