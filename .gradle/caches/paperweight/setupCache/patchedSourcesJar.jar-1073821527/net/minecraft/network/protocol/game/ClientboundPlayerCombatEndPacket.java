package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatEndPacket implements Packet<ClientGamePacketListener> {
    private final int killerId;
    private final int duration;

    public ClientboundPlayerCombatEndPacket(CombatTracker damageTracker) {
        this(damageTracker.getKillerId(), damageTracker.getCombatDuration());
    }

    public ClientboundPlayerCombatEndPacket(int attackerId, int timeSinceLastAttack) {
        this.killerId = attackerId;
        this.duration = timeSinceLastAttack;
    }

    public ClientboundPlayerCombatEndPacket(FriendlyByteBuf buf) {
        this.duration = buf.readVarInt();
        this.killerId = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.duration);
        buf.writeInt(this.killerId);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePlayerCombatEnd(this);
    }
}
