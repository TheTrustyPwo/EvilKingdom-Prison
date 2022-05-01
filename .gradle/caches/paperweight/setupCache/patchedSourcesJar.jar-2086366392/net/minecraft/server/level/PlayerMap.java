package net.minecraft.server.level;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Set;

public final class PlayerMap {
    private final Object2BooleanMap<ServerPlayer> players = new Object2BooleanOpenHashMap<>();

    public Set<ServerPlayer> getPlayers(long l) {
        return this.players.keySet();
    }

    public void addPlayer(long l, ServerPlayer player, boolean watchDisabled) {
        this.players.put(player, watchDisabled);
    }

    public void removePlayer(long l, ServerPlayer player) {
        this.players.removeBoolean(player);
    }

    public void ignorePlayer(ServerPlayer player) {
        this.players.replace(player, true);
    }

    public void unIgnorePlayer(ServerPlayer player) {
        this.players.replace(player, false);
    }

    public boolean ignoredOrUnknown(ServerPlayer player) {
        return this.players.getOrDefault(player, true);
    }

    public boolean ignored(ServerPlayer player) {
        return this.players.getBoolean(player);
    }

    public void updatePlayer(long prevPos, long currentPos, ServerPlayer player) {
    }
}
