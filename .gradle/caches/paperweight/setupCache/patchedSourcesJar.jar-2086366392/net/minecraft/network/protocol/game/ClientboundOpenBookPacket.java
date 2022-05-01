package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;

public class ClientboundOpenBookPacket implements Packet<ClientGamePacketListener> {
    private final InteractionHand hand;

    public ClientboundOpenBookPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public ClientboundOpenBookPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleOpenBook(this);
    }

    public InteractionHand getHand() {
        return this.hand;
    }
}
