package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class ServerboundUseItemOnPacket implements Packet<ServerGamePacketListener> {
    private final BlockHitResult blockHit;
    private final InteractionHand hand;

    public ServerboundUseItemOnPacket(InteractionHand hand, BlockHitResult blockHitResult) {
        this.hand = hand;
        this.blockHit = blockHitResult;
    }

    public ServerboundUseItemOnPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.blockHit = buf.readBlockHitResult();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
        buf.writeBlockHitResult(this.blockHit);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleUseItemOn(this);
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public BlockHitResult getHitResult() {
        return this.blockHit;
    }
}
