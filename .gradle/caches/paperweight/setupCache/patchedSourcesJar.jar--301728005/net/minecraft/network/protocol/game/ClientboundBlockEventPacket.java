package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;

public class ClientboundBlockEventPacket implements Packet<ClientGamePacketListener> {
    private final BlockPos pos;
    private final int b0;
    private final int b1;
    private final Block block;

    public ClientboundBlockEventPacket(BlockPos pos, Block block, int type, int data) {
        this.pos = pos;
        this.block = block;
        this.b0 = type;
        this.b1 = data;
    }

    public ClientboundBlockEventPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.b0 = buf.readUnsignedByte();
        this.b1 = buf.readUnsignedByte();
        this.block = Registry.BLOCK.byId(buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.b0);
        buf.writeByte(this.b1);
        buf.writeVarInt(Registry.BLOCK.getId(this.block));
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleBlockEvent(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getB0() {
        return this.b0;
    }

    public int getB1() {
        return this.b1;
    }

    public Block getBlock() {
        return this.block;
    }
}
