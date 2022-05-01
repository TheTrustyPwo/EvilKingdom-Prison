package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetDefaultSpawnPositionPacket implements Packet<ClientGamePacketListener> {
    public final BlockPos pos;
    private final float angle;

    public ClientboundSetDefaultSpawnPositionPacket(BlockPos pos, float angle) {
        this.pos = pos;
        this.angle = angle;
    }

    public ClientboundSetDefaultSpawnPositionPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.angle = buf.readFloat();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeFloat(this.angle);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetSpawn(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public float getAngle() {
        return this.angle;
    }
}
