package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundOpenSignEditorPacket implements Packet<ClientGamePacketListener> {
    private final BlockPos pos;

    public ClientboundOpenSignEditorPacket(BlockPos pos) {
        this.pos = pos;
    }

    public ClientboundOpenSignEditorPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleOpenSignEditor(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
