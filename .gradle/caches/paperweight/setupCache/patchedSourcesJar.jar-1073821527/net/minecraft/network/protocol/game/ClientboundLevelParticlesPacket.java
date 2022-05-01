package net.minecraft.network.protocol.game;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundLevelParticlesPacket implements Packet<ClientGamePacketListener> {
    private final double x;
    private final double y;
    private final double z;
    private final float xDist;
    private final float yDist;
    private final float zDist;
    private final float maxSpeed;
    private final int count;
    private final boolean overrideLimiter;
    private final ParticleOptions particle;

    public <T extends ParticleOptions> ClientboundLevelParticlesPacket(T parameters, boolean longDistance, double x, double y, double z, float offsetX, float offsetY, float offsetZ, float speed, int count) {
        this.particle = parameters;
        this.overrideLimiter = longDistance;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xDist = offsetX;
        this.yDist = offsetY;
        this.zDist = offsetZ;
        this.maxSpeed = speed;
        this.count = count;
    }

    public ClientboundLevelParticlesPacket(FriendlyByteBuf buf) {
        ParticleType<?> particleType = Registry.PARTICLE_TYPE.byId(buf.readInt());
        this.overrideLimiter = buf.readBoolean();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.xDist = buf.readFloat();
        this.yDist = buf.readFloat();
        this.zDist = buf.readFloat();
        this.maxSpeed = buf.readFloat();
        this.count = buf.readInt();
        this.particle = this.readParticle(buf, particleType);
    }

    private <T extends ParticleOptions> T readParticle(FriendlyByteBuf buf, ParticleType<T> type) {
        return type.getDeserializer().fromNetwork(type, buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(Registry.PARTICLE_TYPE.getId(this.particle.getType()));
        buf.writeBoolean(this.overrideLimiter);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.xDist);
        buf.writeFloat(this.yDist);
        buf.writeFloat(this.zDist);
        buf.writeFloat(this.maxSpeed);
        buf.writeInt(this.count);
        this.particle.writeToNetwork(buf);
    }

    public boolean isOverrideLimiter() {
        return this.overrideLimiter;
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

    public float getXDist() {
        return this.xDist;
    }

    public float getYDist() {
        return this.yDist;
    }

    public float getZDist() {
        return this.zDist;
    }

    public float getMaxSpeed() {
        return this.maxSpeed;
    }

    public int getCount() {
        return this.count;
    }

    public ParticleOptions getParticle() {
        return this.particle;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleParticleEvent(this);
    }
}
