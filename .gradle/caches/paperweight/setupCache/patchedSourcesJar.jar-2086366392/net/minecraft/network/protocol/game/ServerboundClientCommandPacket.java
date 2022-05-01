package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundClientCommandPacket implements Packet<ServerGamePacketListener> {
    private final ServerboundClientCommandPacket.Action action;

    public ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action mode) {
        this.action = mode;
    }

    public ServerboundClientCommandPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(ServerboundClientCommandPacket.Action.class);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleClientCommand(this);
    }

    public ServerboundClientCommandPacket.Action getAction() {
        return this.action;
    }

    public static enum Action {
        PERFORM_RESPAWN,
        REQUEST_STATS;
    }
}
