package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public abstract class ServerboundMovePlayerPacket implements Packet<ServerGamePacketListener> {
    public final double x;
    public final double y;
    public final double z;
    public final float yRot;
    public final float xRot;
    protected final boolean onGround;
    public final boolean hasPos;
    public final boolean hasRot;

    protected ServerboundMovePlayerPacket(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean changePosition, boolean changeLook) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yaw;
        this.xRot = pitch;
        this.onGround = onGround;
        this.hasPos = changePosition;
        this.hasRot = changeLook;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleMovePlayer(this);
    }

    public double getX(double currentX) {
        return this.hasPos ? this.x : currentX;
    }

    public double getY(double currentY) {
        return this.hasPos ? this.y : currentY;
    }

    public double getZ(double currentZ) {
        return this.hasPos ? this.z : currentZ;
    }

    public float getYRot(float currentYaw) {
        return this.hasRot ? this.yRot : currentYaw;
    }

    public float getXRot(float currentPitch) {
        return this.hasRot ? this.xRot : currentPitch;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public static class Pos extends ServerboundMovePlayerPacket {
        public Pos(double x, double y, double z, boolean onGround) {
            super(x, y, z, 0.0F, 0.0F, onGround, true, false);
        }

        public static ServerboundMovePlayerPacket.Pos read(FriendlyByteBuf buf) {
            double d = buf.readDouble();
            double e = buf.readDouble();
            double f = buf.readDouble();
            boolean bl = buf.readUnsignedByte() != 0;
            return new ServerboundMovePlayerPacket.Pos(d, e, f, bl);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeByte(this.onGround ? 1 : 0);
        }
    }

    public static class PosRot extends ServerboundMovePlayerPacket {
        public PosRot(double x, double y, double z, float yaw, float pitch, boolean onGround) {
            super(x, y, z, yaw, pitch, onGround, true, true);
        }

        public static ServerboundMovePlayerPacket.PosRot read(FriendlyByteBuf buf) {
            double d = buf.readDouble();
            double e = buf.readDouble();
            double f = buf.readDouble();
            float g = buf.readFloat();
            float h = buf.readFloat();
            boolean bl = buf.readUnsignedByte() != 0;
            return new ServerboundMovePlayerPacket.PosRot(d, e, f, g, h, bl);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeFloat(this.yRot);
            buf.writeFloat(this.xRot);
            buf.writeByte(this.onGround ? 1 : 0);
        }
    }

    public static class Rot extends ServerboundMovePlayerPacket {
        public Rot(float yaw, float pitch, boolean onGround) {
            super(0.0D, 0.0D, 0.0D, yaw, pitch, onGround, false, true);
        }

        public static ServerboundMovePlayerPacket.Rot read(FriendlyByteBuf buf) {
            float f = buf.readFloat();
            float g = buf.readFloat();
            boolean bl = buf.readUnsignedByte() != 0;
            return new ServerboundMovePlayerPacket.Rot(f, g, bl);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeFloat(this.yRot);
            buf.writeFloat(this.xRot);
            buf.writeByte(this.onGround ? 1 : 0);
        }
    }

    public static class StatusOnly extends ServerboundMovePlayerPacket {
        public StatusOnly(boolean onGround) {
            super(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, onGround, false, false);
        }

        public static ServerboundMovePlayerPacket.StatusOnly read(FriendlyByteBuf buf) {
            boolean bl = buf.readUnsignedByte() != 0;
            return new ServerboundMovePlayerPacket.StatusOnly(bl);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(this.onGround ? 1 : 0);
        }
    }
}
