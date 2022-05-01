package net.minecraft.server.level;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class ServerBossEvent extends BossEvent {
    private final Set<ServerPlayer> players = Sets.newHashSet();
    private final Set<ServerPlayer> unmodifiablePlayers = Collections.unmodifiableSet(this.players);
    public boolean visible = true;

    public ServerBossEvent(Component displayName, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style) {
        super(Mth.createInsecureUUID(), displayName, color, style);
    }

    @Override
    public void setProgress(float percent) {
        if (percent != this.progress) {
            super.setProgress(percent);
            this.broadcast(ClientboundBossEventPacket::createUpdateProgressPacket);
        }

    }

    @Override
    public void setColor(BossEvent.BossBarColor color) {
        if (color != this.color) {
            super.setColor(color);
            this.broadcast(ClientboundBossEventPacket::createUpdateStylePacket);
        }

    }

    @Override
    public void setOverlay(BossEvent.BossBarOverlay style) {
        if (style != this.overlay) {
            super.setOverlay(style);
            this.broadcast(ClientboundBossEventPacket::createUpdateStylePacket);
        }

    }

    @Override
    public BossEvent setDarkenScreen(boolean darkenSky) {
        if (darkenSky != this.darkenScreen) {
            super.setDarkenScreen(darkenSky);
            this.broadcast(ClientboundBossEventPacket::createUpdatePropertiesPacket);
        }

        return this;
    }

    @Override
    public BossEvent setPlayBossMusic(boolean dragonMusic) {
        if (dragonMusic != this.playBossMusic) {
            super.setPlayBossMusic(dragonMusic);
            this.broadcast(ClientboundBossEventPacket::createUpdatePropertiesPacket);
        }

        return this;
    }

    @Override
    public BossEvent setCreateWorldFog(boolean thickenFog) {
        if (thickenFog != this.createWorldFog) {
            super.setCreateWorldFog(thickenFog);
            this.broadcast(ClientboundBossEventPacket::createUpdatePropertiesPacket);
        }

        return this;
    }

    @Override
    public void setName(Component name) {
        if (!Objects.equal(name, this.name)) {
            super.setName(name);
            this.broadcast(ClientboundBossEventPacket::createUpdateNamePacket);
        }

    }

    public void broadcast(Function<BossEvent, ClientboundBossEventPacket> bossBarToPacketFunction) {
        if (this.visible) {
            ClientboundBossEventPacket clientboundBossEventPacket = bossBarToPacketFunction.apply(this);

            for(ServerPlayer serverPlayer : this.players) {
                serverPlayer.connection.send(clientboundBossEventPacket);
            }
        }

    }

    public void addPlayer(ServerPlayer player) {
        if (this.players.add(player) && this.visible) {
            player.connection.send(ClientboundBossEventPacket.createAddPacket(this));
        }

    }

    public void removePlayer(ServerPlayer player) {
        if (this.players.remove(player) && this.visible) {
            player.connection.send(ClientboundBossEventPacket.createRemovePacket(this.getId()));
        }

    }

    public void removeAllPlayers() {
        if (!this.players.isEmpty()) {
            for(ServerPlayer serverPlayer : Lists.newArrayList(this.players)) {
                this.removePlayer(serverPlayer);
            }
        }

    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        if (visible != this.visible) {
            this.visible = visible;

            for(ServerPlayer serverPlayer : this.players) {
                serverPlayer.connection.send(visible ? ClientboundBossEventPacket.createAddPacket(this) : ClientboundBossEventPacket.createRemovePacket(this.getId()));
            }
        }

    }

    public Collection<ServerPlayer> getPlayers() {
        return this.unmodifiablePlayers;
    }
}
