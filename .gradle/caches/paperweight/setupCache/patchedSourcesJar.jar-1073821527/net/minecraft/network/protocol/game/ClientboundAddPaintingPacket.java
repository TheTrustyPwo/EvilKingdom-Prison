package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.decoration.Motive;
import net.minecraft.world.entity.decoration.Painting;

public class ClientboundAddPaintingPacket implements Packet<ClientGamePacketListener> {
    private final int id;
    private final UUID uuid;
    private final BlockPos pos;
    private final Direction direction;
    private final int motive;

    public ClientboundAddPaintingPacket(Painting entity) {
        this.id = entity.getId();
        this.uuid = entity.getUUID();
        this.pos = entity.getPos();
        this.direction = entity.getDirection();
        this.motive = Registry.MOTIVE.getId(entity.motive);
    }

    public ClientboundAddPaintingPacket(FriendlyByteBuf buf) {
        this.id = buf.readVarInt();
        this.uuid = buf.readUUID();
        this.motive = buf.readVarInt();
        this.pos = buf.readBlockPos();
        this.direction = Direction.from2DDataValue(buf.readUnsignedByte());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeUUID(this.uuid);
        buf.writeVarInt(this.motive);
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.direction.get2DDataValue());
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleAddPainting(this);
    }

    public int getId() {
        return this.id;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public Motive getMotive() {
        return Registry.MOTIVE.byId(this.motive);
    }
}
