package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundPlayerActionPacket implements Packet<ServerGamePacketListener> {
    private final BlockPos pos;
    private final Direction direction;
    private final ServerboundPlayerActionPacket.Action action;

    public ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction direction) {
        this.action = action;
        this.pos = pos.immutable();
        this.direction = direction;
    }

    public ServerboundPlayerActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(ServerboundPlayerActionPacket.Action.class);
        this.pos = buf.readBlockPos();
        this.direction = Direction.from3DDataValue(buf.readUnsignedByte());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.direction.get3DDataValue());
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePlayerAction(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public ServerboundPlayerActionPacket.Action getAction() {
        return this.action;
    }

    public static enum Action {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM,
        SWAP_ITEM_WITH_OFFHAND;
    }
}
