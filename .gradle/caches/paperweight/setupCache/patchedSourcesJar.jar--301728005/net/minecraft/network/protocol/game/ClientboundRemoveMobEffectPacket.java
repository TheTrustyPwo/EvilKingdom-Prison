package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientboundRemoveMobEffectPacket implements Packet<ClientGamePacketListener> {
    private final int entityId;
    private final MobEffect effect;

    public ClientboundRemoveMobEffectPacket(int entityId, MobEffect effectType) {
        this.entityId = entityId;
        this.effect = effectType;
    }

    public ClientboundRemoveMobEffectPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.effect = MobEffect.byId(buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(MobEffect.getId(this.effect));
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleRemoveMobEffect(this);
    }

    @Nullable
    public Entity getEntity(Level world) {
        return world.getEntity(this.entityId);
    }

    @Nullable
    public MobEffect getEffect() {
        return this.effect;
    }
}
