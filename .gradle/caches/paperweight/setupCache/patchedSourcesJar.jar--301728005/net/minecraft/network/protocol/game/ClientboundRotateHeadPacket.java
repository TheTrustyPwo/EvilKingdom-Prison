package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientboundRotateHeadPacket implements Packet<ClientGamePacketListener> {
    private final int entityId;
    private final byte yHeadRot;

    public ClientboundRotateHeadPacket(Entity entity, byte headYaw) {
        this.entityId = entity.getId();
        this.yHeadRot = headYaw;
    }

    public ClientboundRotateHeadPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.yHeadRot = buf.readByte();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.yHeadRot);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleRotateMob(this);
    }

    public Entity getEntity(Level world) {
        return world.getEntity(this.entityId);
    }

    public byte getYHeadRot() {
        return this.yHeadRot;
    }
}
