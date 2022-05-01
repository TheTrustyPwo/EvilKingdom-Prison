package net.evilkingdom.prison.component.components.rank.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import org.bukkit.Material;

import java.util.HashMap;

public class Rank {

    private long rank, price;
    private HashMap<Material, Double> blockPallet;

    /**
     * Allows you to create a rank.
     *
     * @param rank ~ The rank.
     * @param price ~ The rank's price.
     * @param blockPallet ~ The rank's block pallet.
     */
    public Rank(final long rank, final long price, final HashMap<Material, Double> blockPallet) {
        this.rank = rank;
        this.price = price;
        this.blockPallet = blockPallet;
    }

    /**
     * Allows you to retrieve the rank's block pallet.
     *
     * @return The rank's block pallet.
     */
    public HashMap<Material, Double> getBlockPallet() {
        return this.blockPallet;
    }

    /**
     * Allows you to retrieve the rank's price.
     *
     * @return The rank's price.
     */
    public Long getPrice() {
        return this.price;
    }

    /**
     * Allows you to retrieve the rank.
     *
     * @return The rank.
     */
    public Long getRank() {
        return this.rank;
    }

}
