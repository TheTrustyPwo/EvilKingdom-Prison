package net.evilkingdom.prison.component.components.data.objects;

/*
 * Made with love by https://kodirati.com/.
 */

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import net.evilkingdom.commons.cooldown.CooldownImplementor;
import net.evilkingdom.commons.cooldown.objects.Cooldown;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.DatapointModel;
import net.evilkingdom.commons.datapoint.objects.DatapointObject;
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
    private ArrayList<Cooldown> cooldowns;
    private ArrayList<UUID> banned, whitelisted;

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
            final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
            final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_mines")).findFirst().get();
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
        final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_mines")).findFirst().get();
        return datapoint.get(this.uuid.toString()).thenApply(optionalDatapointModel -> {
            if (optionalDatapointModel.isEmpty()) {
                return false;
            }
            final DatapointModel datapointModel = optionalDatapointModel.get();
            if (datapointModel.getObjects().containsKey("tax")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("tax");
                this.tax = (Double) datapointObject.getObject();
            }
            if (datapointModel.getObjects().containsKey("theme")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("theme");
                this.theme = (String) datapointObject.getObject();
            }
            if (datapointModel.getObjects().containsKey("owner")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("owner");
                this.owner = UUID.fromString(((String) datapointObject.getObject()));
            }
            if (datapointModel.getObjects().containsKey("privacy")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("privacy");
                this.privacy = (boolean) datapointObject.getObject();
            }
            if (datapointModel.getObjects().containsKey("center")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("center");
                final int x = (int) datapointObject.getInnerObjects().get("x").getObject();
                final int z = (int) datapointObject.getInnerObjects().get("z").getObject();
                this.center = new Location(this.plugin.getComponentManager().getMineComponent().getWorld(), x, 175, z);
            }
            if (datapointModel.getObjects().containsKey("banned")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("banned");
                this.banned = new ArrayList<UUID>(datapointObject.getInnerObjects().values().stream().map(innerDatapointObject -> UUID.fromString(((String) datapointObject.getObject()))).collect(Collectors.toList()));
            }
            if (datapointModel.getObjects().containsKey("whitelisted")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("whitelisted");
                this.whitelisted = new ArrayList<UUID>(datapointObject.getInnerObjects().values().stream().map(innerDatapointObject -> UUID.fromString(((String) datapointObject.getObject()))).collect(Collectors.toList()));
            }
            if (datapointModel.getObjects().containsKey("cooldowns")) {
                final DatapointObject datapointObject = datapointModel.getObjects().get("cooldowns");
                datapointObject.getInnerObjects().values().forEach(innerDatapointObject -> {
                    final String type = (String) innerDatapointObject.getInnerObjects().get("type").getObject();
                    final long timeLeft = (long) innerDatapointObject.getInnerObjects().get("timeLeft").getObject();
                    final Cooldown cooldown = new Cooldown(this.plugin, "mine-" + this.uuid + "-" + type, timeLeft);
                    this.cooldowns.add(cooldown);
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
        final DatapointModel datapointModel = new DatapointModel(this.uuid.toString());
        datapointModel.getObjects().put("tax", new DatapointObject(this.tax));
        datapointModel.getObjects().put("owner", new DatapointObject(this.owner.toString()));
        datapointModel.getObjects().put("theme", new DatapointObject(this.theme));
        datapointModel.getObjects().put("privacy", new DatapointObject(this.privacy));
        final DatapointObject centerDatapointObject = new DatapointObject();
        centerDatapointObject.getInnerObjects().put("x", new DatapointObject(this.center.getBlockX()));
        centerDatapointObject.getInnerObjects().put("z", new DatapointObject(this.center.getBlockZ()));
        datapointModel.getObjects().put("center", centerDatapointObject);
        final DatapointObject bannedDatapointObject = new DatapointObject();
        for (int i = 0; i < this.banned.size(); i++) {
            bannedDatapointObject.getInnerObjects().put(String.valueOf(i), new DatapointObject(this.banned.get(i).toString()));
        }
        datapointModel.getObjects().put("banned", bannedDatapointObject);
        final DatapointObject whitelistedDatapointObject = new DatapointObject();
        for (int i = 0; i < this.whitelisted.size(); i++) {
            whitelistedDatapointObject.getInnerObjects().put(String.valueOf(i), new DatapointObject(this.whitelisted.get(i).toString()));
        }
        datapointModel.getObjects().put("whitelisted", whitelistedDatapointObject);
        final DatapointObject cooldownsDatapointObject = new DatapointObject();
        for (int i = 0; i < this.getCooldowns().size(); i++) {
            final Cooldown cooldown = this.getCooldowns().get(i);
            final DatapointObject cooldownDatapointObject = new DatapointObject();
            cooldownDatapointObject.getInnerObjects().put("type", new DatapointObject(cooldown.getIdentifier().replaceFirst("mine-" + this.uuid + "-", "")));
            cooldownDatapointObject.getInnerObjects().put("timeLeft", new DatapointObject(cooldown.getTimeLeft()));
            cooldownsDatapointObject.getInnerObjects().put(String.valueOf(i), cooldownDatapointObject);
        }
        datapointModel.getObjects().put("cooldowns", cooldownsDatapointObject);
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getDatapoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_mines")).findFirst().get();
        datapoint.save(datapointModel, asynchronous);
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
     * Allows you to set the data's banned.
     *
     * @param banned ~ The data's banned to set.
     */
    public void setBanned(final ArrayList<UUID> banned) {
        this.banned = banned;
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
     * Allows you to set the data's whitelisted.
     *
     * @param whitelisted ~ The data's whitelisted to set.
     */
    public void setWhitelisted(final ArrayList<UUID> whitelisted) {
        this.whitelisted = whitelisted;
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

}
