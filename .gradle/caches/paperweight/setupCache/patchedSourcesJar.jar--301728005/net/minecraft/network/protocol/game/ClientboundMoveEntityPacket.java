package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class ClientboundMoveEntityPacket implements Packet<ClientGamePacketListener> {
    private static final double TRUNCATION_STEPS = 4096.0D;
    protected final int entityId;
    protected final short xa;
    protected final short ya;
    protected final short za;
    protected final byte yRot;
    protected final byte xRot;
    protected final boolean onGround;
    protected final boolean hasRot;
    protected final boolean hasPos;

    public static long entityToPacket(double coord) {
        return Mth.lfloor(coord * 4096.0D); // Paper - check ItemEntity#setPosRaw on update
    }

    public static double packetToEntity(long coord) {
        return (double)coord / 4096.0D; // Paper - check ItemEntity#setPosRaw on update
    }

    public Vec3 updateEntityPosition(Vec3 orig) {
        double d = this.xa == 0 ? orig.x : packetToEntity(entityToPacket(orig.x) + (long)this.xa);
        double e = this.ya == 0 ? orig.y : packetToEntity(entityToPacket(orig.y) + (long)this.ya);
        double f = this.za == 0 ? orig.z : packetToEntity(entityToPacket(orig.z) + (long)this.za);
        return new Vec3(d, e, f);
    }

    public static Vec3 packetToEntity(long x, long y, long z) {
        return (new Vec3((double)x, (double)y, (double)z)).scale((double)2.4414062E-4F);
    }

    protected ClientboundMoveEntityPacket(int entityId, short deltaX, short deltaY, short deltaZ, byte yaw, byte pitch, boolean onGround, boolean rotate, boolean positionChanged) {
        this.entityId = entityId;
        this.xa = deltaX;
        this.ya = deltaY;
        this.za = deltaZ;
        this.yRot = yaw;
        this.xRot = pitch;
        this.onGround = onGround;
        this.hasRot = rotate;
        this.hasPos = positionChanged;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleMoveEntity(this);
    }

    @Override
    public String toString() {
        return "Entity_" + super.toString();
    }

    @Nullable
    public Entity getEntity(Level world) {
        return world.getEntity(this.entityId);
    }

    public short getXa() {
        return this.xa;
    }

    public short getYa() {
        return this.ya;
    }

    public short getZa() {
        return this.za;
    }

    public byte getyRot() {
        return this.yRot;
    }

    public byte getxRot() {
        return this.xRot;
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public static class Pos extends ClientboundMoveEntityPacket {
        public Pos(int entityId, short deltaX, short deltaY, short deltaZ, boolean onGround) {
            super(entityId, deltaX, deltaY, deltaZ, (byte)0, (byte)0, onGround, false, true);
        }

        public static ClientboundMoveEntityPacket.Pos read(FriendlyByteBuf buf) {
            int i = buf.readVarInt();
            short s = buf.readShort();
            short t = buf.readShort();
            short u = buf.readShort();
            boolean bl = buf.readBoolean();
            return new ClientboundMoveEntityPacket.Pos(i, s, t, u, bl);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.entityId);
            buf.writeShort(this.xa);
            buf.writeShort(this.ya);
            buf.writeShort(this.za);
            buf.writeBoolean(this.onGround);
        }
    }

    public static class PosRot extends ClientboundMoveEntityPacket {
        public PosRot(int entityId, short deltaX, short deltaY, short deltaZ, byte yaw, byte pitch, boolean onGround) {
            super(entityId, deltaX, deltaY, deltaZ, yaw, pitch, onGround, true, true);
        }

        public static ClientboundMoveEntityPacket.PosRot read(FriendlyByteBuf buf) {
            int i = buf.readVarInt();
            short s = buf.readShort();
            short t = buf.readShort();
            short u = buf.readShort();
            byte b = buf.readByte();
            byte c = buf.readByte();
            boolean bl = buf.readBoolean();
            return new ClientboundMoveEntityPacket.PosRot(i, s, t, u, b, c, bl);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.entityId);
            buf.writeShort(this.xa);
            buf.writeShort(this.ya);
            buf.writeShort(this.za);
            buf.writeByte(this.yRot);
            buf.writeByte(this.xRot);
            buf.writeBoolean(this.onGround);
        }
    }

    public static class Rot extends ClientboundMoveEntityPacket {
        public Rot(int entityId, byte yaw, byte pitch, boolean onGround) {
            super(entityId, (short)0, (short)0, (short)0, yaw, pitch, onGround, true, false);
        }

        public static ClientboundMoveEntityPacket.Rot read(FriendlyByteBuf buf) {
            int i = buf.readVarInt();
            byte b = buf.readByte();
            byte c = buf.readByte();
            boolean bl = buf.readBoolean();
            return new ClientboundMoveEntityPacket.Rot(i, b, c, bl);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.entityId);
            buf.writeByte(this.yRot);
            buf.writeByte(this.xRot);
            buf.writeBoolean(this.onGround);
        }
    }
}
