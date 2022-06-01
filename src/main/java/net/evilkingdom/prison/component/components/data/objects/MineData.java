package net.evilkingdom.prison.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import net.evilkingdom.commons.cooldown.CooldownImplementor;
import net.evilkingdom.commons.cooldown.objects.Cooldown;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.Datasite;
import net.evilkingdom.prison.Prison;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MineData {

    private final Prison plugin;

    private double tax;
    private String theme;
    private UUID owner;
    private final UUID uuid;
    private boolean privacy;
    private Location center;
    private final ArrayList<Cooldown> cooldowns;
    private final ArrayList<UUID> banned, whitelisted;

    private static final HashSet<MineData> cache = new HashSet<MineData>();

    /**
     * Allows you to create a MineData.
     *
     * @param uuid ~ The UUID of the mine.
     */
    public MineData(final UUID uuid) {
        this.plugin = Prison.getPlugin();

        this.uuid = uuid;
        this.theme = "Unknown";
        this.owner = UUID.randomUUID();
        this.tax = 0.0;
        this.privacy = false;
        this.center = new Location(this.plugin.getComponentManager().getMineComponent().getWorld(), 0, 175, 0);
        this.banned = new ArrayList<UUID>();
        this.whitelisted = new ArrayList<UUID>();
        this.cooldowns = new ArrayList<Cooldown>();
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
            final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_mines")).findFirst().get();
            return datapoint.exists(this.uuid.toString());
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
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_mines")).findFirst().get();
        return datapoint.get(this.uuid.toString()).thenApply(optionalJsonObject -> {
            if (optionalJsonObject.isEmpty()) {
                return false;
            }
            final JsonObject jsonObject = optionalJsonObject.get();
            if (jsonObject.has("tax")) {
                this.tax = jsonObject.get("tax").getAsDouble();
            }
            if (jsonObject.has("theme")) {
                this.theme = jsonObject.get("theme").getAsString();
            }
            if (jsonObject.has("owner")) {
                this.owner = UUID.fromString(jsonObject.get("owner").getAsString());
            }
            if (jsonObject.has("privacy")) {
                this.privacy = jsonObject.get("privacy").getAsBoolean();
            }
            if (jsonObject.has("center")) {
                final JsonObject centerJsonObject = jsonObject.get("center").getAsJsonObject();
                this.center = new Location(this.plugin.getComponentManager().getMineComponent().getWorld(), centerJsonObject.get("x").getAsInt(), 175, centerJsonObject.get("z").getAsInt());
            }
            if (jsonObject.has("banned")) {
                jsonObject.get("banned").getAsJsonArray().forEach(jsonElement -> this.banned.add(UUID.fromString(jsonElement.getAsString())));
            }
            if (jsonObject.has("whitelisted")) {
                jsonObject.get("whitelisted").getAsJsonArray().forEach(jsonElement -> this.banned.add(UUID.fromString(jsonElement.getAsString())));
            }
            if (jsonObject.has("cooldowns")) {
                jsonObject.get("cooldowns").getAsJsonArray().forEach(jsonElement -> {
                    final JsonObject cooldownJsonObject = jsonElement.getAsJsonObject();
                    this.cooldowns.add(new Cooldown(this.plugin, "mine-" + this.uuid + "-" + cooldownJsonObject.get("type").getAsString(), cooldownJsonObject.get("timeLeft").getAsLong()));
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
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("tax", this.tax);
        jsonObject.addProperty("owner", this.owner.toString());
        jsonObject.addProperty("theme", this.theme);
        jsonObject.addProperty("privacy", this.privacy);
        final JsonObject centerJsonObject = new JsonObject();
        centerJsonObject.addProperty("x", this.center.getBlockX());
        centerJsonObject.addProperty("z", this.center.getBlockZ());
        jsonObject.add("center", centerJsonObject);
        final JsonArray bannedJsonArray = new JsonArray();
        this.banned.forEach(uuid -> bannedJsonArray.add(uuid.toString()));
        jsonObject.add("banned", bannedJsonArray);
        final JsonArray whitelistedJsonArray = new JsonArray();
        this.whitelisted.forEach(uuid -> whitelistedJsonArray.add(uuid.toString()));
        jsonObject.add("whitelisted", whitelistedJsonArray);
        final JsonArray cooldownsJsonArray = new JsonArray();
        this.getCooldowns().forEach(cooldown -> {
            final JsonObject cooldownJsonObject = new JsonObject();
            cooldownJsonObject.addProperty("type", cooldown.getIdentifier().replaceFirst("mine-" + this.uuid + "-", ""));
            cooldownJsonObject.addProperty("timeLeft", cooldown.getTimeLeft());
            cooldownsJsonArray.add(cooldownJsonObject);
        });
        jsonObject.add("cooldowns", cooldownsJsonArray);
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_mines")).findFirst().get();
        datapoint.save(jsonObject, this.uuid.toString(), asynchronous);
    }

    /**
     * Allows you to set the data's tax.
     *
     * @param tax ~ The data's tax to set.
     */
    public void setTax(final double tax) {
        this.tax = tax;
    }

    /**
     * Allows you to retrieve the data's tax.
     *
     * @return The data's tax.
     */
    public Double getTax() {
        return this.tax;
    }

    /**
     * Allows you to set the data's theme.
     *
     * @param theme ~ The data's theme to set.
     */
    public void setTheme(final String theme) {
        this.theme = theme;
    }

    /**
     * Allows you to retrieve the data's theme.
     *
     * @return The data's theme.
     */
    public String getTheme() {
        return this.theme;
    }

    /**
     * Allows you to set the data's owner.
     *
     * @param owner ~ The data's owner to set.
     */
    public void setOwner(final UUID owner) {
        this.owner = owner;
    }

    /**
     * Allows you to retrieve the data's owner.
     *
     * @return The data's owner.
     */
    public UUID getOwner() {
        return this.owner;
    }

    /**
     * Allows you to set the data's privacy.
     *
     * @param privacy ~ The data's privacy to set.
     */
    public void setPrivate(final boolean privacy) {
        this.privacy = privacy;
    }

    /**
     * Allows you to retrieve the data's privacy state.
     *
     * @return The data's privacy state.
     */
    public Boolean isPrivate() {
        return this.privacy;
    }

    /**
     * Allows you to retrieve the data's cooldowns.
     *
     * @return The data's cooldowns.
     */
    public ArrayList<Cooldown> getCooldowns() {
        final CooldownImplementor cooldownImplementor = CooldownImplementor.get(this.plugin);
        return new ArrayList<Cooldown>(cooldownImplementor.getCooldowns().stream().filter(cooldown -> cooldown.getIdentifier().startsWith("mine-" + this.uuid)).collect(Collectors.toList()));
    }

    /**
     * Allows you to set the data's center.
     *
     * @param center ~ The data's center to set.
     */
    public void setCenter(final Location center) {
        this.center = center;
    }

    /**
     * Allows you to retrieve the data's center.
     *
     * @return The data's center.
     */
    public Location getCenter() {
        return this.center;
    }

    /**
     * Allows you to retrieve the data's corner one.
     * It'll calculate using the center location alongside the theme's offset.
     *
     * @return The data's corner one.
     */
    public Location getCornerOne() {
        final double xOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.corner-one.x");
        final double yOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.corner-one.y");
        final double zOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.corner-one.z");
        return this.center.clone().add(xOffset, yOffset, zOffset);
    }

    /**
     * Allows you to retrieve the data's corner two.
     * It'll calculate using the center location alongside the theme's offset.
     *
     * @return The data's corner two.
     */
    public Location getCornerTwo() {
        final double xOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.corner-two.x");
        final double yOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.corner-two.y");
        final double zOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.corner-two.z");
        return this.center.clone().add(xOffset, yOffset, zOffset);
    }

    /**
     * Allows you to retrieve the data's mine corner one.
     * It'll calculate using the center location alongside the theme's offset.
     *
     * @return The data's mine corner one.
     */
    public Location getMineCornerOne() {
        final double xOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.mine-corner-one.x");
        final double yOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.mine-corner-one.y");
        final double zOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.mine-corner-one.z");
        return this.center.clone().add(xOffset, yOffset, zOffset);
    }

    /**
     * Allows you to retrieve the data's mine corner two.
     * It'll calculate using the center location alongside the theme's offset.
     *
     * @return The data's mine corner two.
     */
    public Location getMineCornerTwo() {
        final double xOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.mine-corner-two.x");
        final double yOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.mine-corner-two.y");
        final double zOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.mine-corner-two.z");
        return this.center.clone().add(xOffset, yOffset, zOffset);
    }

    /**
     * Allows you to retrieve the data's corner go.
     * It'll calculate using the center location alongside the theme's offset.
     *
     * @return The data's corner go.
     */
    public Location getGo() {
        final double xOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.go.x");
        final double yOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.go.y");
        final double zOffset = plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.themes." + this.theme + ".offsets.go.z");
        final Location goLocation = this.center.clone().add(xOffset, yOffset, zOffset).toCenterLocation();
        return goLocation.setDirection(this.center.toCenterLocation().subtract(goLocation.toVector()).toVector());
    }

    /**
     * Allows you to retrieve the data's border size.
     * It'll calculate using the center location alongside the theme's offset.
     *
     * @return The data's border size.
     */
    public double getBorderSize() {
        final double highestValue;
        final double lowestValue;
        if (this.getCornerOne().getX() > this.getCenter().getX()) {
            highestValue = this.getCornerOne().getX();
            lowestValue = this.center.getX();
        } else {
            lowestValue = this.getCornerOne().getX();
            highestValue = this.center.getX();
        }
        return (highestValue - lowestValue) * 2;
    }

    /**
     * Allows you to retrieve the data's banned.
     *
     * @return The data's banned.
     */
    public ArrayList<UUID> getBanned() {
        return this.banned;
    }

    /**
     * Allows you to retrieve the data's whitelisted.
     *
     * @return The data's whitelisted.
     */
    public ArrayList<UUID> getWhitelisted() {
        return this.whitelisted;
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
        this.cooldowns.forEach(cooldown -> cooldown.start());
    }

    /**
     * Allows you to uncache the data.
     */
    public void uncache() {
        this.getCooldowns().forEach(cooldown -> cooldown.stop());
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
    public static HashSet<MineData> getCache() {
        return cache;
    }

    /**
     * Allows you to retrieve a MineData from a UUID.
     * It will automatically either create a new MineData if it doesn't exist, load from the MineData if cached, or the fetch the MineData from the database.
     *
     * @param uuid ~ The mine's UUID.
     * @return The self class.
     */
    public static CompletableFuture<MineData> get(final UUID uuid) {
        final Optional<MineData> optionalMineData = cache.stream().filter(mineData -> mineData.getUUID() == uuid).findAny();
        if (optionalMineData.isPresent()) {
            return CompletableFuture.supplyAsync(() -> optionalMineData.get());
        } else {
            final MineData mineData = new MineData(uuid);
            return mineData.load().thenApply(loadSuccessful -> mineData);
        }
    }

    /**
     * Allows you to retrieve a MineData from the cache directly.
     * This should only be used if you know this will be cached.
     *
     * @param uuid ~ The mine's UUID.
     * @return The self class.
     */
    public static Optional<MineData> getViaCache(final UUID uuid) {
        return cache.stream().filter(mineData -> mineData.getUUID() == uuid).findFirst();
    }

}
