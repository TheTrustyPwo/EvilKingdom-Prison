package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ClientboundSetEntityMotionPacket implements Packet<ClientGamePacketListener> {
    private final int id;
    private final int xa;
    private final int ya;
    private final int za;

    public ClientboundSetEntityMotionPacket(Entity entity) {
        this(entity.getId(), entity.getDeltaMovement());
    }

    public ClientboundSetEntityMotionPacket(int id, Vec3 velocity) {
        this.id = id;
        double d = 3.9D;
        double e = Mth.clamp(velocity.x, -3.9D, 3.9D);
        double f = Mth.clamp(velocity.y, -3.9D, 3.9D);
        double g = Mth.clamp(velocity.z, -3.9D, 3.9D);
        this.xa = (int)(e * 8000.0D);
        this.ya = (int)(f * 8000.0D);
        this.za = (int)(g * 8000.0D);
    }

    public ClientboundSetEntityMotionPacket(FriendlyByteBuf buf) {
        this.id = buf.readVarInt();
        this.xa = buf.readShort();
        this.ya = buf.readShort();
        this.za = buf.readShort();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeShort(this.xa);
        buf.writeShort(this.ya);
        buf.writeShort(this.za);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetEntityMotion(this);
    }

    public int getId() {
        return this.id;
    }

    public int getXa() {
        return this.xa;
    }

    public int getYa() {
        return this.ya;
    }

    public int getZa() {
        return this.za;
    }
}
