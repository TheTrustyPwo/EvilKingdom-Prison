package net.minecraft.server.players;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class SleepStatus {
    private int activePlayers;
    private int sleepingPlayers;

    public boolean areEnoughSleeping(int percentage) {
        return this.sleepingPlayers >= this.sleepersNeeded(percentage);
    }

    public boolean areEnoughDeepSleeping(int percentage, List<ServerPlayer> players) {
        int i = (int)players.stream().filter(Player::isSleepingLongEnough).count();
        return i >= this.sleepersNeeded(percentage);
    }

    public int sleepersNeeded(int percentage) {
        return Math.max(1, Mth.ceil((float)(this.activePlayers * percentage) / 100.0F));
    }

    public void removeAllSleepers() {
        this.sleepingPlayers = 0;
    }

    public int amountSleeping() {
        return this.sleepingPlayers;
    }

    public boolean update(List<ServerPlayer> players) {
        int i = this.activePlayers;
        int j = this.sleepingPlayers;
        this.activePlayers = 0;
        this.sleepingPlayers = 0;

        for(ServerPlayer serverPlayer : players) {
            if (!serverPlayer.isSpectator()) {
                ++this.activePlayers;
                if (serverPlayer.isSleeping()) {
                    ++this.sleepingPlayers;
                }
            }
        }

        return (j > 0 || this.sleepingPlayers > 0) && (i != this.activePlayers || j != this.sleepingPlayers);
    }
}
