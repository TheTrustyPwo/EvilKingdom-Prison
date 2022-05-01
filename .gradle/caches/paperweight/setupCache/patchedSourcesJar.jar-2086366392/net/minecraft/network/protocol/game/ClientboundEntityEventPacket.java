package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientboundEntityEventPacket implements Packet<ClientGamePacketListener> {
    private final int entityId;
    private final byte eventId;

    public ClientboundEntityEventPacket(Entity entity, byte status) {
        this.entityId = entity.getId();
        this.eventId = status;
    }

    public ClientboundEntityEventPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.eventId = buf.readByte();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeByte(this.eventId);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleEntityEvent(this);
    }

    @Nullable
    public Entity getEntity(Level world) {
        return world.getEntity(this.entityId);
    }

    public byte getEventId() {
        return this.eventId;
    }
}
