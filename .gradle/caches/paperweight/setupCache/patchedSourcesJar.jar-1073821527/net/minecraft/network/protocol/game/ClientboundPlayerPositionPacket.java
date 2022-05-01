package net.minecraft.network.protocol.game;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundPlayerPositionPacket implements Packet<ClientGamePacketListener> {
    private final double x;
    private final double y;
    private final double z;
    private final float yRot;
    private final float xRot;
    private final Set<ClientboundPlayerPositionPacket.RelativeArgument> relativeArguments;
    private final int id;
    private final boolean dismountVehicle;

    public ClientboundPlayerPositionPacket(double x, double y, double z, float yaw, float pitch, Set<ClientboundPlayerPositionPacket.RelativeArgument> flags, int teleportId, boolean shouldDismount) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yaw;
        this.xRot = pitch;
        this.relativeArguments = flags;
        this.id = teleportId;
        this.dismountVehicle = shouldDismount;
    }

    public ClientboundPlayerPositionPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yRot = buf.readFloat();
        this.xRot = buf.readFloat();
        this.relativeArguments = ClientboundPlayerPositionPacket.RelativeArgument.unpack(buf.readUnsignedByte());
        this.id = buf.readVarInt();
        this.dismountVehicle = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.yRot);
        buf.writeFloat(this.xRot);
        buf.writeByte(ClientboundPlayerPositionPacket.RelativeArgument.pack(this.relativeArguments));
        buf.writeVarInt(this.id);
        buf.writeBoolean(this.dismountVehicle);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleMovePlayer(this);
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

    public float getYRot() {
        return this.yRot;
    }

    public float getXRot() {
        return this.xRot;
    }

    public int getId() {
        return this.id;
    }

    public boolean requestDismountVehicle() {
        return this.dismountVehicle;
    }

    public Set<ClientboundPlayerPositionPacket.RelativeArgument> getRelativeArguments() {
        return this.relativeArguments;
    }

    public static enum RelativeArgument {
        X(0),
        Y(1),
        Z(2),
        Y_ROT(3),
        X_ROT(4);

        private final int bit;

        private RelativeArgument(int shift) {
            this.bit = shift;
        }

        private int getMask() {
            return 1 << this.bit;
        }

        private boolean isSet(int mask) {
            return (mask & this.getMask()) == this.getMask();
        }

        public static Set<ClientboundPlayerPositionPacket.RelativeArgument> unpack(int mask) {
            Set<ClientboundPlayerPositionPacket.RelativeArgument> set = EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class);

            for(ClientboundPlayerPositionPacket.RelativeArgument relativeArgument : values()) {
                if (relativeArgument.isSet(mask)) {
                    set.add(relativeArgument);
                }
            }

            return set;
        }

        public static int pack(Set<ClientboundPlayerPositionPacket.RelativeArgument> flags) {
            int i = 0;

            for(ClientboundPlayerPositionPacket.RelativeArgument relativeArgument : flags) {
                i |= relativeArgument.getMask();
            }

            return i;
        }
    }
}
