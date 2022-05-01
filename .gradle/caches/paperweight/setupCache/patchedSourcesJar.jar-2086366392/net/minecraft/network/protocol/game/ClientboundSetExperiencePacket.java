package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetExperiencePacket implements Packet<ClientGamePacketListener> {
    private final float experienceProgress;
    private final int totalExperience;
    private final int experienceLevel;

    public ClientboundSetExperiencePacket(float barProgress, int experienceLevel, int experience) {
        this.experienceProgress = barProgress;
        this.totalExperience = experienceLevel;
        this.experienceLevel = experience;
    }

    public ClientboundSetExperiencePacket(FriendlyByteBuf buf) {
        this.experienceProgress = buf.readFloat();
        this.experienceLevel = buf.readVarInt();
        this.totalExperience = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(this.experienceProgress);
        buf.writeVarInt(this.experienceLevel);
        buf.writeVarInt(this.totalExperience);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetExperience(this);
    }

    public float getExperienceProgress() {
        return this.experienceProgress;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public int getExperienceLevel() {
        return this.experienceLevel;
    }
}
