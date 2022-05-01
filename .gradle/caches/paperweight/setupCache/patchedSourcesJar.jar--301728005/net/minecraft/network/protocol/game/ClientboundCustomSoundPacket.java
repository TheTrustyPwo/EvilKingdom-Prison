package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class ClientboundCustomSoundPacket implements Packet<ClientGamePacketListener> {
    public static final float LOCATION_ACCURACY = 8.0F;
    private final ResourceLocation name;
    private final SoundSource source;
    private final int x;
    private final int y;
    private final int z;
    private final float volume;
    private final float pitch;

    public ClientboundCustomSoundPacket(ResourceLocation sound, SoundSource category, Vec3 pos, float volume, float pitch) {
        this.name = sound;
        this.source = category;
        this.x = (int)(pos.x * 8.0D);
        this.y = (int)(pos.y * 8.0D);
        this.z = (int)(pos.z * 8.0D);
        this.volume = volume;
        this.pitch = pitch;
    }

    public ClientboundCustomSoundPacket(FriendlyByteBuf buf) {
        this.name = buf.readResourceLocation();
        this.source = buf.readEnum(SoundSource.class);
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.name);
        buf.writeEnum(this.source);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public SoundSource getSource() {
        return this.source;
    }

    public double getX() {
        return (double)((float)this.x / 8.0F);
    }

    public double getY() {
        return (double)((float)this.y / 8.0F);
    }

    public double getZ() {
        return (double)((float)this.z / 8.0F);
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleCustomSoundEvent(this);
    }
}
