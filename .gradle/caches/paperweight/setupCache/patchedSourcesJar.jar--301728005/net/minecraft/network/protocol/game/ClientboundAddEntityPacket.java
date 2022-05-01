package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddEntityPacket implements Packet<ClientGamePacketListener> {
    public static final double MAGICAL_QUANTIZATION = 8000.0D;
    private final int id;
    private final UUID uuid;
    private final double x;
    private final double y;
    private final double z;
    private final int xa;
    private final int ya;
    private final int za;
    private final int xRot;
    private final int yRot;
    private final EntityType<?> type;
    private final int data;
    public static final double LIMIT = 3.9D;

    public ClientboundAddEntityPacket(int id, UUID uuid, double x, double y, double z, float pitch, float yaw, EntityType<?> entityTypeId, int entityData, Vec3 velocity) {
        this.id = id;
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = Mth.floor(pitch * 256.0F / 360.0F);
        this.yRot = Mth.floor(yaw * 256.0F / 360.0F);
        this.type = entityTypeId;
        this.data = entityData;
        this.xa = (int)(Mth.clamp(velocity.x, -3.9D, 3.9D) * 8000.0D);
        this.ya = (int)(Mth.clamp(velocity.y, -3.9D, 3.9D) * 8000.0D);
        this.za = (int)(Mth.clamp(velocity.z, -3.9D, 3.9D) * 8000.0D);
    }

    public ClientboundAddEntityPacket(Entity entity) {
        this(entity, 0);
    }

    public ClientboundAddEntityPacket(Entity entity, int entityData) {
        this(entity.getId(), entity.getUUID(), entity.getX(), entity.getY(), entity.getZ(), entity.getXRot(), entity.getYRot(), entity.getType(), entityData, entity.getDeltaMovement());
    }

    public ClientboundAddEntityPacket(Entity entity, EntityType<?> entityType, int data, BlockPos pos) {
        this(entity.getId(), entity.getUUID(), (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), entity.getXRot(), entity.getYRot(), entityType, data, entity.getDeltaMovement());
    }

    public ClientboundAddEntityPacket(FriendlyByteBuf buf) {
        this.id = buf.readVarInt();
        this.uuid = buf.readUUID();
        this.type = Registry.ENTITY_TYPE.byId(buf.readVarInt());
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.xRot = buf.readByte();
        this.yRot = buf.readByte();
        this.data = buf.readInt();
        this.xa = buf.readShort();
        this.ya = buf.readShort();
        this.za = buf.readShort();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeUUID(this.uuid);
        buf.writeVarInt(Registry.ENTITY_TYPE.getId(this.type));
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeByte(this.xRot);
        buf.writeByte(this.yRot);
        buf.writeInt(this.data);
        buf.writeShort(this.xa);
        buf.writeShort(this.ya);
        buf.writeShort(this.za);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleAddEntity(this);
    }

    public int getId() {
        return this.id;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getXa() {
        return (double)this.xa / 8000.0D;
    }

    public double getYa() {
        return (double)this.ya / 8000.0D;
    }

    public double getZa() {
        return (double)this.za / 8000.0D;
    }

    public int getxRot() {
        return this.xRot;
    }

    public int getyRot() {
        return this.yRot;
    }

    public EntityType<?> getType() {
        return this.type;
    }

    public int getData() {
        return this.data;
    }
}
