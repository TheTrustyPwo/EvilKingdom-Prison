package net.evilkingdom.prison.component.components.rank;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.rank.objects.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RankComponent {

    private final Prison plugin;

    /**
     * Allows you to create the component.
     */
    public RankComponent() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Rank] &aInitializing..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Rank] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Rank] &cTerminating..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Rank] &cTerminated."));
    }

    /**
     * Allows you to generate a certain amount of ranks.
     *
     * @param startingRank ~ The rank to start with.
     * @param amount ~ The amount of ranks to generate.
     */
    public CompletableFuture<ArrayList<Rank>> generateRanks(final long startingRank, final long amount) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Rank> ranks = new ArrayList<Rank>();
            for (long rankNumber = (startingRank + 1); rankNumber < (amount + 1); rankNumber++) {
                long price = this.plugin.getComponentManager().getFileComponent().getConfiguration().getLong("components.rank.1-2-price");
                if (rankNumber > 1) {
                    for (long interiorRankNumber = 1; interiorRankNumber <= rankNumber; interiorRankNumber++) {
                        price = Math.round((price * this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.rank.multiplier")));
                    }
                }
                final HashMap<Material, Double> blockPallet = this.generateBlockPallet(rankNumber);
                final Rank rank = new Rank(rankNumber, price, blockPallet);
                ranks.add(rank);
            }
            return ranks;
        });
    }

    /**
     * Allows you to generate a block pallet for a rank.
     * Automatically calculates the block pallet using the positioning of the block on the list as well as the rank.
     *
     * @param rank ~ The rank to generate a block pallet for.
     */
    private HashMap<Material, Double> generateBlockPallet(final long rank) {
        final ArrayList<Material> blocks = new ArrayList<Material>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.blocks").stream().map(blockString -> Material.getMaterial(blockString)).collect(Collectors.toList()));
        Collections.reverse(blocks);
        final HashMap<Material, Double> blockPercentages = new HashMap<Material, Double>();
        for (int i = 0; i < blocks.size(); i++) {
            final Material block = blocks.get(i);
            final double rarity = Math.min(Math.max(0.05 * rank - 50 + i * (100d / blocks.size()), 0), 100);
            if (rarity > 0) {
                blockPercentages.put(block, rarity);
            }
        }
        return blockPercentages;
    }

}
