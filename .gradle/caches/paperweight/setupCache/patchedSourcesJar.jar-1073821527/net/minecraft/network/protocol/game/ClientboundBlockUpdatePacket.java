package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ClientboundBlockUpdatePacket implements Packet<ClientGamePacketListener> {
    private final BlockPos pos;
    public final BlockState blockState;

    public ClientboundBlockUpdatePacket(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.blockState = state;
    }

    public ClientboundBlockUpdatePacket(BlockGetter world, BlockPos pos) {
        this(pos, world.getBlockState(pos));
    }

    public ClientboundBlockUpdatePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.blockState = Block.BLOCK_STATE_REGISTRY.byId(buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(Block.getId(this.blockState));
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleBlockUpdate(this);
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
