package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundResourcePackPacket implements Packet<ServerGamePacketListener> {
    public final ServerboundResourcePackPacket.Action action;

    public ServerboundResourcePackPacket(ServerboundResourcePackPacket.Action status) {
        this.action = status;
    }

    public ServerboundResourcePackPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(ServerboundResourcePackPacket.Action.class);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleResourcePackResponse(this);
    }

    public ServerboundResourcePackPacket.Action getAction() {
        return this.action;
    }

    public static enum Action {
        SUCCESSFULLY_LOADED,
        DECLINED,
        FAILED_DOWNLOAD,
        ACCEPTED;
    }
}
