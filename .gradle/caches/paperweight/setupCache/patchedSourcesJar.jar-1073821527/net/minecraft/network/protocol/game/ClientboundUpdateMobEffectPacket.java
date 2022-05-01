package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class ClientboundUpdateMobEffectPacket implements Packet<ClientGamePacketListener> {
    private static final int FLAG_AMBIENT = 1;
    private static final int FLAG_VISIBLE = 2;
    private static final int FLAG_SHOW_ICON = 4;
    private final int entityId;
    private final int effectId;
    private final byte effectAmplifier;
    private final int effectDurationTicks;
    private final byte flags;

    public ClientboundUpdateMobEffectPacket(int entityId, MobEffectInstance effect) {
        this.entityId = entityId;
        this.effectId = MobEffect.getId(effect.getEffect());
        this.effectAmplifier = (byte)(effect.getAmplifier() & 255);
        if (effect.getDuration() > 32767) {
            this.effectDurationTicks = 32767;
        } else {
            this.effectDurationTicks = effect.getDuration();
        }

        byte b = 0;
        if (effect.isAmbient()) {
            b = (byte)(b | 1);
        }

        if (effect.isVisible()) {
            b = (byte)(b | 2);
        }

        if (effect.showIcon()) {
            b = (byte)(b | 4);
        }

        this.flags = b;
    }

    public ClientboundUpdateMobEffectPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.effectId = buf.readVarInt();
        this.effectAmplifier = buf.readByte();
        this.effectDurationTicks = buf.readVarInt();
        this.flags = buf.readByte();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.effectId);
        buf.writeByte(this.effectAmplifier);
        buf.writeVarInt(this.effectDurationTicks);
        buf.writeByte(this.flags);
    }

    public boolean isSuperLongDuration() {
        return this.effectDurationTicks == 32767;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleUpdateMobEffect(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public int getEffectId() {
        return this.effectId;
    }

    public byte getEffectAmplifier() {
        return this.effectAmplifier;
    }

    public int getEffectDurationTicks() {
        return this.effectDurationTicks;
    }

    public boolean isEffectVisible() {
        return (this.flags & 2) == 2;
    }

    public boolean isEffectAmbient() {
        return (this.flags & 1) == 1;
    }

    public boolean effectShowsIcon() {
        return (this.flags & 4) == 4;
    }
}
