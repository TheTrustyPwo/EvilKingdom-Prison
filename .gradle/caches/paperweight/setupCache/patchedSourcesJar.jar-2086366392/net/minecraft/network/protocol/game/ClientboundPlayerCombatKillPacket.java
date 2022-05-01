package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatKillPacket implements Packet<ClientGamePacketListener> {
    private final int playerId;
    private final int killerId;
    private final Component message;

    public ClientboundPlayerCombatKillPacket(CombatTracker damageTracker, Component message) {
        this(damageTracker.getMob().getId(), damageTracker.getKillerId(), message);
    }

    public ClientboundPlayerCombatKillPacket(int entityId, int killerId, Component message) {
        this.playerId = entityId;
        this.killerId = killerId;
        this.message = message;
    }

    public ClientboundPlayerCombatKillPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readVarInt();
        this.killerId = buf.readInt();
        this.message = buf.readComponent();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.playerId);
        buf.writeInt(this.killerId);
        buf.writeComponent(this.message);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePlayerCombatKill(this);
    }

    @Override
    public boolean isSkippable() {
        return true;
    }

    public int getKillerId() {
        return this.killerId;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public Component getMessage() {
        return this.message;
    }
}
