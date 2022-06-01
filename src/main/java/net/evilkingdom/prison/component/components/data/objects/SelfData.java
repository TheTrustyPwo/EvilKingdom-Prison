package net.evilkingdom.prison.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.Datasite;
import net.evilkingdom.prison.component.components.mine.objects.MineLocation;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.rank.objects.Rank;
import org.bson.Document;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SelfData {

    private final Prison plugin;

    private final ArrayList<Rank> ranks;
    private final ArrayList<MineLocation> mineLocations;

    private static final HashSet<SelfData> cache = new HashSet<SelfData>();

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
            final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
            final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_self")).findFirst().get();
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
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_self")).findFirst().get();
        return datapoint.get("self").thenApply(optionalJsonObject -> {
            if (optionalJsonObject.isEmpty()) {
                return false;
            }
            final JsonObject jsonObject = optionalJsonObject.get();
            if (jsonObject.has("mineLocations")) {
                jsonObject.get("mineLocations").getAsJsonArray().forEach(jsonElement -> this.mineLocations.add(new MineLocation(jsonElement.getAsJsonObject().get("x").getAsInt(), jsonElement.getAsJsonObject().get("z").getAsInt(), jsonElement.getAsJsonObject().get("used").getAsBoolean())));
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
        final JsonObject jsonObject = new JsonObject();
        final JsonArray mineLocationsJsonArray = new JsonArray();
        this.mineLocations.forEach(mineLocation -> {
            final JsonObject mineLocationJsonObject = new JsonObject();
            mineLocationJsonObject.addProperty("x", mineLocation.getX());
            mineLocationJsonObject.addProperty("z", mineLocation.getZ());
            mineLocationJsonObject.addProperty("used", mineLocation.isUsed());
            mineLocationsJsonArray.add(mineLocationJsonObject);
        });
        jsonObject.add("mineLocations", mineLocationsJsonArray);
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_self")).findFirst().get();
        datapoint.save(jsonObject, "self", asynchronous);
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
        cache.add(this);
    }

    /**
     * Allows you to uncache the data.
     */
    public void uncache() {
        cache.remove(this);
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

    /**
     * Allows you to retrieve a SelfData from the cache directly.
     * This should only be used if you know this will be cached.
     *
     * @return The self class.
     */
    public static Optional<SelfData> getViaCache() {
        return cache.stream().findFirst();
    }

}
