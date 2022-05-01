package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ClientboundPlayerLookAtPacket implements Packet<ClientGamePacketListener> {
    private final double x;
    private final double y;
    private final double z;
    private final int entity;
    private final EntityAnchorArgument.Anchor fromAnchor;
    private final EntityAnchorArgument.Anchor toAnchor;
    private final boolean atEntity;

    public ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor selfAnchor, double targetX, double targetY, double targetZ) {
        this.fromAnchor = selfAnchor;
        this.x = targetX;
        this.y = targetY;
        this.z = targetZ;
        this.entity = 0;
        this.atEntity = false;
        this.toAnchor = null;
    }

    public ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor selfAnchor, Entity entity, EntityAnchorArgument.Anchor targetAnchor) {
        this.fromAnchor = selfAnchor;
        this.entity = entity.getId();
        this.toAnchor = targetAnchor;
        Vec3 vec3 = targetAnchor.apply(entity);
        this.x = vec3.x;
        this.y = vec3.y;
        this.z = vec3.z;
        this.atEntity = true;
    }

    public ClientboundPlayerLookAtPacket(FriendlyByteBuf buf) {
        this.fromAnchor = buf.readEnum(EntityAnchorArgument.Anchor.class);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.atEntity = buf.readBoolean();
        if (this.atEntity) {
            this.entity = buf.readVarInt();
            this.toAnchor = buf.readEnum(EntityAnchorArgument.Anchor.class);
        } else {
            this.entity = 0;
            this.toAnchor = null;
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.fromAnchor);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeBoolean(this.atEntity);
        if (this.atEntity) {
            buf.writeVarInt(this.entity);
            buf.writeEnum(this.toAnchor);
        }

    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleLookAt(this);
    }

    public EntityAnchorArgument.Anchor getFromAnchor() {
        return this.fromAnchor;
    }

    @Nullable
    public Vec3 getPosition(Level world) {
        if (this.atEntity) {
            Entity entity = world.getEntity(this.entity);
            return entity == null ? new Vec3(this.x, this.y, this.z) : this.toAnchor.apply(entity);
        } else {
            return new Vec3(this.x, this.y, this.z);
        }
    }
}
