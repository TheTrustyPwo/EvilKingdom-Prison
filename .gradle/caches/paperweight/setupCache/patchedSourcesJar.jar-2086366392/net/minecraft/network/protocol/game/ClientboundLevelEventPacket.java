package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundLevelEventPacket implements Packet<ClientGamePacketListener> {
    private final int type;
    private final BlockPos pos;
    private final int data;
    private final boolean globalEvent;

    public ClientboundLevelEventPacket(int eventId, BlockPos pos, int data, boolean global) {
        this.type = eventId;
        this.pos = pos.immutable();
        this.data = data;
        this.globalEvent = global;
    }

    public ClientboundLevelEventPacket(FriendlyByteBuf buf) {
        this.type = buf.readInt();
        this.pos = buf.readBlockPos();
        this.data = buf.readInt();
        this.globalEvent = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.type);
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.data);
        buf.writeBoolean(this.globalEvent);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleLevelEvent(this);
    }

    public boolean isGlobalEvent() {
        return this.globalEvent;
    }

    public int getType() {
        return this.type;
    }

    public int getData() {
        return this.data;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
