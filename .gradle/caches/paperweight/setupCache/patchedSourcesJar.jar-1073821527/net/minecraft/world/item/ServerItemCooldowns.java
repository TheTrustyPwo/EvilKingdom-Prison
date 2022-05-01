package net.minecraft.world.item;

import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.server.level.ServerPlayer;

public class ServerItemCooldowns extends ItemCooldowns {
    private final ServerPlayer player;

    public ServerItemCooldowns(ServerPlayer player) {
        this.player = player;
    }

    @Override
    protected void onCooldownStarted(Item item, int duration) {
        super.onCooldownStarted(item, duration);
        this.player.connection.send(new ClientboundCooldownPacket(item, duration));
    }

    @Override
    protected void onCooldownEnded(Item item) {
        super.onCooldownEnded(item);
        this.player.connection.send(new ClientboundCooldownPacket(item, 0));
    }
}
