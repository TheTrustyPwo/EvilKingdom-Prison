package net.minecraft.network.protocol.game;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public record ClientboundBlockBreakAckPacket(BlockPos pos, BlockState state, ServerboundPlayerActionPacket.Action action, boolean allGood) implements Packet<ClientGamePacketListener> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public ClientboundBlockBreakAckPacket(BlockPos pos, BlockState state, ServerboundPlayerActionPacket.Action action, boolean approved, String reason) {
        this(pos, state, action, approved);
    }

    public ClientboundBlockBreakAckPacket {
        pos = pos.immutable();
    }

    public ClientboundBlockBreakAckPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), Block.BLOCK_STATE_REGISTRY.byId(buf.readVarInt()), buf.readEnum(ServerboundPlayerActionPacket.Action.class), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(Block.getId(this.state));
        buf.writeEnum(this.action);
        buf.writeBoolean(this.allGood);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleBlockBreakAck(this);
    }
}
