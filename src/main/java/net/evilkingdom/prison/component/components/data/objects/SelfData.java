package net.evilkingdom.prison.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.DatapointModel;
import net.evilkingdom.commons.datapoint.objects.DatapointObject;
import net.evilkingdom.commons.datapoint.objects.Datasite;
import net.evilkingdom.prison.component.components.mine.objects.MineLocation;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.rank.objects.Rank;
import org.bson.Document;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SelfData {

    private final Prison plugin;

    private ArrayList<Rank> ranks;
    private ArrayList<MineLocation> mineLocations;

    private static HashSet<SelfData> cache = new HashSet<SelfData>();

    /**
     * Allows you to create a SelfData.
     */
    public SelfData() {
        this.plugin = Prison.getPlugin();

        this.ranks = new ArrayList<Rank>();
        this.mineLocations = new ArrayList<MineLocation>();
    }

    /**
     * Allows you to retrieve if the data exists in the Mongo database.
     *
     * @return If the data exists or not.
     */
    public CompletableFuture<Boolean> exists() {
        if (cache.contains(this)) {
            return CompletableFuture.supplyAsync(() -> true);
        } else {
            final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
            final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
            final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_self")).findFirst().get();
            return datapoint.exists("self");
        }
    }

    /**
     * Allows you to load the data from the Mongo database.
     *
     * @return If the data could be loaded or not.
     */
    private CompletableFuture<Boolean> load() {
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_self")).findFirst().get();
        return datapoint.get("self").thenApply(optionalDatapointModel -> {
            if (optionalDatapointModel.isEmpty()) {
                return false;
            }
            final DatapointModel datapointModel = optionalDatapointModel.get();
            if (datapointModel.getObjects().containsKey("mineLocations")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("mineLocations");
                datapointObject.getInnerObjects().values().forEach(innerDatapointObject -> {
                    final int x = (int) innerDatapointObject.getInnerObjects().get("x").getObject();
                    final int z = (int) innerDatapointObject.getInnerObjects().get("z").getObject();
                    final boolean used = (boolean) innerDatapointObject.getInnerObjects().get("used").getObject();
                    final MineLocation mineLocation = new MineLocation(x, z, used);
                    this.mineLocations.add(mineLocation);
                });
           }
           if (datapointModel.getObjects().containsKey("ranks")) {
               final DatapointObject datapointObject = datapointModel.getObjects().get("ranks");
               datapointObject.getInnerObjects().forEach((key, value) -> {
                   final long rankNumber = Long.parseLong(key);
                   final long price = (long) value.getInnerObjects().get("price").getObject();
                   final HashMap<Material, Double> blockPercentages = new HashMap<Material, Double>();
                   final DatapointObject blockPalletDatapointObject = value.getInnerObjects().get("blockPallet");
                   blockPalletDatapointObject.getInnerObjects().forEach((innerKey, innerObject) -> {
                       blockPercentages.put(Material.getMaterial(innerKey), (double) innerObject.getObject());
                   });
                   final Rank rank = new Rank(rankNumber, price, blockPercentages);
                   this.ranks.add(rank);
               });
           }
           return true;
        });
    }

    /**
     * Allows you to save the data to the Mongo database.
     *
     * @param asynchronous ~ If the save is asynchronous (should always be unless it's an emergency saves).
     */
    public void save(final boolean asynchronous) {
        final DatapointModel datapointModel = new DatapointModel("self");
        final DatapointObject mineLocationsDatapointObject = new DatapointObject();
        for (int i = 0; i < this.mineLocations.size(); i++) {
            final MineLocation mineLocation = this.mineLocations.get(i);
            final DatapointObject mineLocationDatapointObject = new DatapointObject();
            mineLocationDatapointObject.getInnerObjects().put("x", new DatapointObject(mineLocation.getX()));
            mineLocationDatapointObject.getInnerObjects().put("z", new DatapointObject(mineLocation.getZ()));
            mineLocationDatapointObject.getInnerObjects().put("used", new DatapointObject(mineLocation.isUsed()));
            mineLocationsDatapointObject.getInnerObjects().put(String.valueOf(i), mineLocationDatapointObject);
        }
        datapointModel.getObjects().put("mineLocations", mineLocationsDatapointObject);
        final DatapointObject ranksDatapointObject = new DatapointObject();
        this.ranks.forEach(rank -> {
            final DatapointObject rankDatapointObject = new DatapointObject();
            rankDatapointObject.getInnerObjects().put("price", new DatapointObject(rank.getPrice()));
            final DatapointObject blockPalletDatapointObject = new DatapointObject();
            rank.getBlockPallet().forEach((material, chance) -> blockPalletDatapointObject.getInnerObjects().put(material.name(), new DatapointObject(chance)));
            rankDatapointObject.getInnerObjects().put("blockPallet", blockPalletDatapointObject);
            ranksDatapointObject.getInnerObjects().put(String.valueOf(rank.getRank()), rankDatapointObject);
        });
        datapointModel.getObjects().put("ranks", ranksDatapointObject);
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_self")).findFirst().get();
        datapoint.save(datapointModel, asynchronous);
    }

    /**
     * Allows you to set the data's ranks.
     *
     * @param ranks ~ The data's ranks to set.
     */
    public void setRanks(final ArrayList<Rank> ranks) {
        this.ranks = ranks;
    }

    /**
     * Allows you to retrieve the data's ranks.
     *
     * @return The data's ranks.
     */
    public ArrayList<Rank> getRanks() {
        return this.ranks;
    }

    /**
     * Allows you to set the data's mine locations.
     *
     * @param mineLocations ~ The data's mine locations to set.
     */
    public void setMineLocations(final ArrayList<MineLocation> mineLocations) {
        this.mineLocations = mineLocations;
    }

    /**
     * Allows you to retrieve the data's mine locations.
     *
     * @return The data's mine locations.
     */
    public ArrayList<MineLocation> getMineLocations() {
        return this.mineLocations;
    }

    /**
     * Allows you to cache the data.
     */
    public void cache() {
        final HashSet<SelfData> previousCache = cache;
        previousCache.add(this);
        cache = previousCache;
    }

    /**
     * Allows you to uncache the data.
     */
    public void uncache() {
        final HashSet<SelfData> previousCache = cache;
        previousCache.remove(this);
        cache = previousCache;
    }

    /**
     * Allows you to retrieve if the data is cached.
     *
     * @return If the data is cached.
     */
    public boolean isCached() {
        return cache.contains(this);
    }

    /**
     * Allows you to retrieve the cache.
     *
     * @return The cache.
     */
    public static HashSet<SelfData> getCache() {
        return cache;
    }

    /**
     * Allows you to retrieve a SelfData.
     * It will automatically either create a new SelfData if it doesn't exist, load from the SelfData if cached, or the fetch the SelfData from the database.
     *
     * @return The self class.
     */
    public static CompletableFuture<SelfData> get() {
        final Optional<SelfData> optionalSelfData = cache.stream().findFirst();
        if (optionalSelfData.isPresent()) {
            return CompletableFuture.supplyAsync(() -> optionalSelfData.get());
        } else {
            final SelfData selfData = new SelfData();
            return selfData.load().thenApply(loadSuccessful -> selfData);
        }
    }

}
