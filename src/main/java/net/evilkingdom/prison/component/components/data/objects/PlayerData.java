package net.evilkingdom.prison.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.DatapointModel;
import net.evilkingdom.commons.datapoint.objects.DatapointObject;
import net.evilkingdom.commons.datapoint.objects.Datasite;
import net.evilkingdom.prison.Prison;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerData {

    private final Prison plugin;

    private final UUID uuid;
    private Optional<UUID> mine;
    private double multiplier;
    private long rank, tokens, gems, blocksMined;

    private static final HashSet<PlayerData> cache = new HashSet<PlayerData>();

    /**
     * Allows you to create a PlayerData.
     *
     * @param uuid ~ The UUID of the player.
     */
    public PlayerData(final UUID uuid) {
        this.plugin = Prison.getPlugin();

        this.uuid = uuid;
        this.rank = 1L;
        this.tokens = 0L;
        this.gems = 0L;
        this.blocksMined = 0L;
        this.multiplier = 1.0;
        this.mine = Optional.empty();
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
            final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_players")).findFirst().get();
            return datapoint.exists(this.uuid.toString());
        }
    }

    /**
     * Allows you to load the data from the Mongo database.
     * Runs asynchronously in order to keep the server from lagging.
     *
     * @return If the data could be loaded or not.
     */
    private CompletableFuture<Boolean> load() {
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_players")).findFirst().get();
        return datapoint.get(this.uuid.toString()).thenApply(optionalDatapointModel -> {
            if (optionalDatapointModel.isEmpty()) {
                return false;
            }
            final DatapointModel datapointModel = optionalDatapointModel.get();
            if (datapointModel.getObjects().containsKey("mine")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("mine");
                this.mine = Optional.of(UUID.fromString(((String) datapointObject.getObject())));
            }
            if (datapointModel.getObjects().containsKey("tokens")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("tokens");
                this.tokens = (long) datapointObject.getObject();
            }
            if (datapointModel.getObjects().containsKey("gems")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("gems");
                this.gems = (long) datapointObject.getObject();
            }
            if (datapointModel.getObjects().containsKey("rank")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("rank");
                this.rank = (long) datapointObject.getObject();
            }
            if (datapointModel.getObjects().containsKey("blocksMined")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("blocksMined");
                this.blocksMined = (long) datapointObject.getObject();
            }
            if (datapointModel.getObjects().containsKey("multiplier")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("multiplier");
                this.multiplier = (double) datapointObject.getObject();
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
        final DatapointModel datapointModel = new DatapointModel(this.uuid.toString());
        if (this.mine.isPresent()) {
            datapointModel.getObjects().put("mine", new DatapointObject(this.mine.get().toString()));
        } else {
            datapointModel.getObjects().put("mine", new DatapointObject());
        }
        datapointModel.getObjects().put("tokens", new DatapointObject(this.tokens));
        datapointModel.getObjects().put("gems", new DatapointObject(this.gems));
        datapointModel.getObjects().put("rank", new DatapointObject(this.rank));
        datapointModel.getObjects().put("blocksMined", new DatapointObject(this.blocksMined));
        datapointModel.getObjects().put("multiplier", new DatapointObject(this.multiplier));
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_players")).findFirst().get();
        datapoint.save(datapointModel, asynchronous);
    }

    /**
     * Allows you to set the data's gems.
     *
     * @param gems ~ The data's gems to set.
     */
    public void setGems(final long gems) {
        this.gems = gems;
    }

    /**
     * Allows you to retrieve the data's gems.
     *
     * @return The data's gems.
     */
    public Long getGems() {
        return this.gems;
    }

    /**
     * Allows you to set the data's mine.
     *
     * @param mine ~ The data's mine to set.
     */
    public void setMine(final Optional<UUID> mine) {
        this.mine = mine;
    }

    /**
     * Allows you to retrieve the data's mine.
     *
     * @return The data's mine.
     */
    public Optional<UUID> getMine() {
        return this.mine;
    }

    /**
     * Allows you to set the data's tokens.
     *
     * @param tokens ~ The data's tokens to set.
     */
    public void setTokens(final long tokens) {
        this.tokens = tokens;
    }

    /**
     * Allows you to retrieve the data's tokens.
     *
     * @return The data's tokens.
     */
    public Long getTokens() {
        return this.tokens;
    }

    /**
     * Allows you to set the data's blocks mined.
     *
     * @param blocksMined ~ The data's blocks mined to set.
     */
    public void setBlocksMined(final long blocksMined) {
        this.blocksMined = blocksMined;
    }

    /**
     * Allows you to retrieve the data's blocks mined.
     *
     * @return The data's blocks mined.
     */
    public Long getBlocksMined() {
        return this.blocksMined;
    }

    /**
     * Allows you to set the data's multiplier.
     *
     * @param multiplier ~ The data's multiplier to set.
     */
    public void setMultiplier(final double multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * Allows you to retrieve the data's multiplier.
     *
     * @return The data's multiplier.
     */
    public Double getMultiplier() {
        return this.multiplier;
    }

    /**
     * Allows you to set the data's rank.
     *
     * @param rank ~ The data's rank to set.
     */
    public void setRank(final long rank) {
        this.rank = rank;
    }

    /**
     * Allows you to retrieve the data's rank.
     *
     * @return The data's rank.
     */
    public Long getRank() {
        return this.rank;
    }

    /**
     * Allows you to retrieve the data's UUID.
     *
     * @return The data's UUID.
     */
    public UUID getUUID() {
        return this.uuid;
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
    public static HashSet<PlayerData> getCache() {
        return cache;
    }

    /**
     * Allows you to retrieve a PlayerData from a UUID.
     * It will automatically either create a new PlayerData if it doesn't exist, load from the PlayerData if cached, or the fetch the PlayerData from the database.
     *
     * @param uuid ~ The player's UUID.
     * @return The self class.
     */
    public static CompletableFuture<PlayerData> get(final UUID uuid) {
        final Optional<PlayerData> optionalPlayerData = cache.stream().filter(playerData -> playerData.getUUID() == uuid).findAny();
        if (optionalPlayerData.isPresent()) {
            return CompletableFuture.supplyAsync(() -> optionalPlayerData.get());
        } else {
            final PlayerData playerData = new PlayerData(uuid);
            return playerData.load().thenApply(loadSuccessful -> playerData);
        }
    }

    /**
     * Allows you to retrieve a PlayerData from the cache directly.
     * This should only be used if you know this will be cached.
     *
     * @return The self class.
     */
    public static Optional<PlayerData> getViaCache(final UUID uuid) {
        return cache.stream().filter(playerData -> playerData.getUUID() == uuid).findFirst();
    }

}
