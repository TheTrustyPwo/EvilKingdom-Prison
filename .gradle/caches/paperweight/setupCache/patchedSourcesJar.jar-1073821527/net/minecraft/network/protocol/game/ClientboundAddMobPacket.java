package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddMobPacket implements Packet<ClientGamePacketListener> {
    private final int id;
    private final UUID uuid;
    private final int type;
    private final double x;
    private final double y;
    private final double z;
    private final int xd;
    private final int yd;
    private final int zd;
    private final byte yRot;
    private final byte xRot;
    private final byte yHeadRot;

    public ClientboundAddMobPacket(LivingEntity entity) {
        this.id = entity.getId();
        this.uuid = entity.getUUID();
        this.type = Registry.ENTITY_TYPE.getId(entity.getType());
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
        this.yRot = (byte)((int)(entity.getYRot() * 256.0F / 360.0F));
        this.xRot = (byte)((int)(entity.getXRot() * 256.0F / 360.0F));
        this.yHeadRot = (byte)((int)(entity.yHeadRot * 256.0F / 360.0F));
        double d = 3.9D;
        Vec3 vec3 = entity.getDeltaMovement();
        double e = Mth.clamp(vec3.x, -3.9D, 3.9D);
        double f = Mth.clamp(vec3.y, -3.9D, 3.9D);
        double g = Mth.clamp(vec3.z, -3.9D, 3.9D);
        this.xd = (int)(e * 8000.0D);
        this.yd = (int)(f * 8000.0D);
        this.zd = (int)(g * 8000.0D);
    }

    public ClientboundAddMobPacket(FriendlyByteBuf buf) {
        this.id = buf.readVarInt();
        this.uuid = buf.readUUID();
        this.type = buf.readVarInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yRot = buf.readByte();
        this.xRot = buf.readByte();
        this.yHeadRot = buf.readByte();
        this.xd = buf.readShort();
        this.yd = buf.readShort();
        this.zd = buf.readShort();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeUUID(this.uuid);
        buf.writeVarInt(this.type);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeByte(this.yRot);
        buf.writeByte(this.xRot);
        buf.writeByte(this.yHeadRot);
        buf.writeShort(this.xd);
        buf.writeShort(this.yd);
        buf.writeShort(this.zd);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleAddMob(this);
    }

    public int getId() {
        return this.id;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public int getType() {
        return this.type;
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

    public int getXd() {
        return this.xd;
    }

    public int getYd() {
        return this.yd;
    }

    public int getZd() {
        return this.zd;
    }

    public byte getyRot() {
        return this.yRot;
    }

    public byte getxRot() {
        return this.xRot;
    }

    public byte getyHeadRot() {
        return this.yHeadRot;
    }
}
