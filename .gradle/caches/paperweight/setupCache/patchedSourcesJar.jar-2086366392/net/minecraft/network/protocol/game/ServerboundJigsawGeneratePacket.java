package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundJigsawGeneratePacket implements Packet<ServerGamePacketListener> {
    private final BlockPos pos;
    private final int levels;
    private final boolean keepJigsaws;

    public ServerboundJigsawGeneratePacket(BlockPos pos, int maxDepth, boolean keepJigsaws) {
        this.pos = pos;
        this.levels = maxDepth;
        this.keepJigsaws = keepJigsaws;
    }

    public ServerboundJigsawGeneratePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.levels = buf.readVarInt();
        this.keepJigsaws = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.levels);
        buf.writeBoolean(this.keepJigsaws);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleJigsawGenerate(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int levels() {
        return this.levels;
    }

    public boolean keepJigsaws() {
        return this.keepJigsaws;
    }
}
