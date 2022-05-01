package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;

public class ServerboundUseItemPacket implements Packet<ServerGamePacketListener> {
    private final InteractionHand hand;

    public ServerboundUseItemPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public ServerboundUseItemPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleUseItem(this);
    }

    public InteractionHand getHand() {
        return this.hand;
    }
}
