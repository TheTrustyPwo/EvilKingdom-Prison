package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class ClientboundStopSoundPacket implements Packet<ClientGamePacketListener> {
    private static final int HAS_SOURCE = 1;
    private static final int HAS_SOUND = 2;
    @Nullable
    private final ResourceLocation name;
    @Nullable
    private final SoundSource source;

    public ClientboundStopSoundPacket(@Nullable ResourceLocation soundId, @Nullable SoundSource category) {
        this.name = soundId;
        this.source = category;
    }

    public ClientboundStopSoundPacket(FriendlyByteBuf buf) {
        int i = buf.readByte();
        if ((i & 1) > 0) {
            this.source = buf.readEnum(SoundSource.class);
        } else {
            this.source = null;
        }

        if ((i & 2) > 0) {
            this.name = buf.readResourceLocation();
        } else {
            this.name = null;
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        if (this.source != null) {
            if (this.name != null) {
                buf.writeByte(3);
                buf.writeEnum(this.source);
                buf.writeResourceLocation(this.name);
            } else {
                buf.writeByte(1);
                buf.writeEnum(this.source);
            }
        } else if (this.name != null) {
            buf.writeByte(2);
            buf.writeResourceLocation(this.name);
        } else {
            buf.writeByte(0);
        }

    }

    @Nullable
    public ResourceLocation getName() {
        return this.name;
    }

    @Nullable
    public SoundSource getSource() {
        return this.source;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleStopSoundEvent(this);
    }
}
