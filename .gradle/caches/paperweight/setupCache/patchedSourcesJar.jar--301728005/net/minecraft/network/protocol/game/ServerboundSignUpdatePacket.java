package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundSignUpdatePacket implements Packet<ServerGamePacketListener> {
    private static final int MAX_STRING_LENGTH = 384;
    private final BlockPos pos;
    private final String[] lines;

    public ServerboundSignUpdatePacket(BlockPos pos, String line1, String line2, String line3, String line4) {
        this.pos = pos;
        this.lines = new String[]{line1, line2, line3, line4};
    }

    public ServerboundSignUpdatePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.lines = new String[4];

        for(int i = 0; i < 4; ++i) {
            this.lines[i] = buf.readUtf(384);
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);

        for(int i = 0; i < 4; ++i) {
            buf.writeUtf(this.lines[i]);
        }

    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSignUpdate(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public String[] getLines() {
        return this.lines;
    }
}
