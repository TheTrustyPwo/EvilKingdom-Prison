package net.evilkingdom.prison.component.components.scoreboard;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.scoreboard.ScoreboardImplementor;
import net.evilkingdom.commons.scoreboard.objects.Scoreboard;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import net.evilkingdom.prison.component.components.data.objects.SelfData;
import net.evilkingdom.prison.component.components.rank.objects.Rank;
import net.evilkingdom.prison.component.components.scoreboard.listeners.ConnectionListener;
import net.evilkingdom.prison.Prison;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScoreboardComponent {

    private final Prison plugin;

    /**
     * Allows you to create the component.
     */
    public ScoreboardComponent() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Scoreboard] &aInitializing..."));
        this.registerListeners();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Scoreboard] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Scoreboard] &cTerminating..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Scoreboard] &cTerminated."));
    }

    /**
     * Allows you to register the listeners.
     */
    private void registerListeners() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Scoreboard] &aRegistering listeners..."));
        new ConnectionListener().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Scoreboard] &aRegistered listeners."));
    }
    
    /**
     * Allows you to create a scoreboard for a player.
     *
     * @param player ~ The player the scoreboard is for.
     */
    public void createScoreboard(final Player player) {
        final Scoreboard scoreboard = new Scoreboard(this.plugin, player);
        scoreboard.setRunnable(Optional.of(() -> {
            final SelfData selfData = SelfData.getViaCache().get();
            final Optional<PlayerData> optionalPlayerData = PlayerData.getViaCache(player.getUniqueId());
            if (optionalPlayerData.isEmpty()) {
                return;
            }
            final PlayerData playerData = optionalPlayerData.get();
            final String gems = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols.gems") + NumberUtilities.format(playerData.getGems(), NumberFormatType.LETTERS);
            final String tokens = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols.tokens") + NumberUtilities.format(playerData.getTokens(), NumberFormatType.LETTERS);
            final String blocksMined = NumberUtilities.format(playerData.getBlocksMined(), NumberFormatType.LETTERS);
            final String multiplier = NumberUtilities.format(playerData.getMultiplier(), NumberFormatType.MULTIPLIER);
            final String rank = NumberUtilities.format(playerData.getRank(), NumberFormatType.LETTERS);
            final String nextRank = NumberUtilities.format((playerData.getRank() + 1), NumberFormatType.LETTERS);
            final Rank nextRankData = selfData.getRanks().stream().filter(selfDataRank -> selfDataRank.getRank() == (playerData.getRank() + 1L)).findFirst().get();
            final String nextRankPrice = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols.tokens") + NumberUtilities.format(nextRankData.getPrice(), NumberFormatType.LETTERS);
            long rankProgress = nextRankData.getPrice() - playerData.getTokens();
            if (rankProgress < 0) {
                rankProgress = 0;
            }
            final String nextRankProgress = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols.tokens") + NumberUtilities.format(rankProgress, NumberFormatType.LETTERS);
            double rankProgressPercentage = (playerData.getTokens().doubleValue() / nextRankData.getPrice().doubleValue()) * 100;
            if (rankProgressPercentage > 100) {
                rankProgressPercentage = 100;
            }
            final String nextRankProgressPercentage = NumberUtilities.format(rankProgressPercentage, NumberFormatType.MULTIPLIER);
            final ArrayList<String> lines = new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.scoreboard.lines").stream().map(scoreboardLine -> StringUtilities.colorize(scoreboardLine.replace("%player%", player.getName()).replace("%gems%", gems).replace("%tokens%", tokens).replace("%multiplier%", multiplier).replace("%blocks_mined%", blocksMined).replace("%rank%", rank).replace("%next_rank%", nextRank).replace("%next_rank_price%", nextRankPrice).replace("%next_rank_progress%", nextRankProgress).replace("%next_rank_progress_percentage%", nextRankProgressPercentage))).collect(Collectors.toList()));
            final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.scoreboard.title"));
            scoreboard.setTitle(title);
            scoreboard.setLines(lines);
            scoreboard.update();
        }));
        final SelfData selfData = SelfData.getViaCache().get();
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        final String gems = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols.gems") + NumberUtilities.format(playerData.getGems(), NumberFormatType.LETTERS);
        final String tokens = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols.tokens") + NumberUtilities.format(playerData.getTokens(), NumberFormatType.LETTERS);
        final String blocksMined = NumberUtilities.format(playerData.getBlocksMined(), NumberFormatType.LETTERS);
        final String multiplier = NumberUtilities.format(playerData.getMultiplier(), NumberFormatType.MULTIPLIER);
        final String rank = NumberUtilities.format(playerData.getRank(), NumberFormatType.LETTERS);
        final String nextRank = NumberUtilities.format((playerData.getRank() + 1), NumberFormatType.LETTERS);
        final Rank nextRankData = selfData.getRanks().stream().filter(selfDataRank -> selfDataRank.getRank() == (playerData.getRank() + 1L)).findFirst().get();
        final String nextRankPrice = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols.tokens") + NumberUtilities.format(nextRankData.getPrice(), NumberFormatType.LETTERS);
        long rankProgress = nextRankData.getPrice() - playerData.getTokens();
        if (rankProgress < 0) {
            rankProgress = 0;
        }
        final String nextRankProgress = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols.tokens") + NumberUtilities.format(rankProgress, NumberFormatType.LETTERS);
        double rankProgressPercentage = (playerData.getTokens().doubleValue() / nextRankData.getPrice().doubleValue()) * 100;
        if (rankProgressPercentage > 100) {
            rankProgressPercentage = 100;
        }
        final String nextRankProgressPercentage = NumberUtilities.format(rankProgressPercentage, NumberFormatType.MULTIPLIER);
        final ArrayList<String> lines = new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.scoreboard.lines").stream().map(scoreboardLine -> StringUtilities.colorize(scoreboardLine.replace("%player%", player.getName()).replace("%gems%", gems).replace("%tokens%", tokens).replace("%multiplier%", multiplier).replace("%blocks_mined%", blocksMined).replace("%rank%", rank).replace("%next_rank%", nextRank).replace("%next_rank_price%", nextRankPrice).replace("%next_rank_progress%", nextRankProgress).replace("%next_rank_progress_percentage%", nextRankProgressPercentage))).collect(Collectors.toList()));
        final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.scoreboard.title"));
        scoreboard.setTitle(title);
        scoreboard.setLines(lines);
        scoreboard.show();
    }

}
