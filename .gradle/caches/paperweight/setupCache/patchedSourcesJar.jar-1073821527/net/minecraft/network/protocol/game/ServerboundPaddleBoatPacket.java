package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundPaddleBoatPacket implements Packet<ServerGamePacketListener> {
    private final boolean left;
    private final boolean right;

    public ServerboundPaddleBoatPacket(boolean leftPaddling, boolean rightPaddling) {
        this.left = leftPaddling;
        this.right = rightPaddling;
    }

    public ServerboundPaddleBoatPacket(FriendlyByteBuf buf) {
        this.left = buf.readBoolean();
        this.right = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.left);
        buf.writeBoolean(this.right);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePaddleBoat(this);
    }

    public boolean getLeft() {
        return this.left;
    }

    public boolean getRight() {
        return this.right;
    }
}
