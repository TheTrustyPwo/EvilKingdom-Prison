package net.minecraft.network.protocol.game;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.apache.commons.lang3.Validate;

public class ClientboundSoundPacket implements Packet<ClientGamePacketListener> {
    public static final float LOCATION_ACCURACY = 8.0F;
    private final SoundEvent sound;
    private final SoundSource source;
    private final int x;
    private final int y;
    private final int z;
    private final float volume;
    private final float pitch;

    public ClientboundSoundPacket(SoundEvent sound, SoundSource category, double x, double y, double z, float volume, float pitch) {
        Validate.notNull(sound, "sound");
        this.sound = sound;
        this.source = category;
        this.x = (int)(x * 8.0D);
        this.y = (int)(y * 8.0D);
        this.z = (int)(z * 8.0D);
        this.volume = volume;
        this.pitch = pitch;
    }

    public ClientboundSoundPacket(FriendlyByteBuf buf) {
        this.sound = Registry.SOUND_EVENT.byId(buf.readVarInt());
        this.source = buf.readEnum(SoundSource.class);
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(Registry.SOUND_EVENT.getId(this.sound));
        buf.writeEnum(this.source);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
    }

    public SoundEvent getSound() {
        return this.sound;
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
        listener.handleSoundEvent(this);
    }
}
