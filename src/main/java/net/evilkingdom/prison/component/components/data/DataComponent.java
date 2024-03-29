package net.evilkingdom.prison.component.components.data;

/*
 * Made with love by https://kodirati.com/.
 */

import com.mongodb.MongoClientException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import io.papermc.paper.text.PaperComponents;
import net.evilkingdom.commons.cooldown.CooldownImplementor;
import net.evilkingdom.commons.datapoint.DataImplementor;
import net.evilkingdom.commons.datapoint.enums.DatasiteType;
import net.evilkingdom.commons.datapoint.objects.Datapoint;
import net.evilkingdom.commons.datapoint.objects.Datasite;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.components.data.objects.MineData;
import net.evilkingdom.prison.component.components.data.objects.SelfData;
import net.evilkingdom.prison.component.components.mine.objects.MineLocation;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.commands.DataCommand;
import net.evilkingdom.prison.component.components.data.listeners.ConnectionListener;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import org.bson.Document;
import org.bukkit.Bukkit;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DataComponent {

    private final Prison plugin;

    /**
     * Allows you to create the component.
     */
    public DataComponent() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aInitializing..."));
        this.connectToDatabase();
        this.initializeData();
        this.registerListeners();
        this.registerCommands();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cTerminating..."));
        this.terminateData();
        this.disconnectFromDatabase();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cTerminated."));
    }

    /**
     * Allows you to register the commands.
     */
    private void registerCommands() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aRegistering commands..."));
        new DataCommand().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aRegistered commands."));
    }

    /**
     * Allows you to register the listeners.
     */
    private void registerListeners() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aRegistering listeners..."));
        new ConnectionListener().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aRegistered listeners."));
    }

    /**
     * Allows you to connect to the Mongo database.
     */
    private void connectToDatabase() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aConnecting to database..."));
        final Datasite datasite = new Datasite(this.plugin, this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.database.name"), DatasiteType.MONGO_DATABASE, new String[]{this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.database.connection-string")});
        try {
            datasite.initialize();
        } catch (final Exception exception) {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&c[Prison » Component » Components » Data] Failed to connect to database, terminating to prevent a shitshow."));
            this.plugin.getPluginLoader().disablePlugin(this.plugin);
            return;
        }
        new Datapoint(datasite, "prison_players").register();
        new Datapoint(datasite, "prison_mines").register();
        new Datapoint(datasite, "prison_self").register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aConnected to database."));
    }

    /**
     * Allows you to disconnect to the Mongo database.
     */
    private void disconnectFromDatabase() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cDisconnecting from database..."));
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        datasite.terminate();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cDisconnected from database."));
    }

    /**
     * Allows you to initialize the data.
     */
    public void initializeData() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aInitializing data..."));
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getSites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        final Datapoint datapoint = datasite.getPoints().stream().filter(innerDatapoint -> innerDatapoint.getName().equals("prison_players")).findFirst().get();
        datapoint.countAll().thenCompose(count -> {
            if (count == 0L) {
                return CompletableFuture.supplyAsync(() -> 1000L);
            } else {
                return datapoint.getAll().thenApply(jsonObjects -> jsonObjects.stream().map(jsonObject -> jsonObject.get("rank").getAsLong()).sorted(Collections.reverseOrder()).findFirst().get() + 1000L);
            }
        }).whenComplete((generationAmount, generationAmountThrowable) -> SelfData.get().whenComplete((selfData, selfDataThrowable) -> {
            selfData.cache();
            this.plugin.getComponentManager().getRankComponent().generate(0, generationAmount).whenComplete((generated, generatedThrowable) -> selfData.getRanks().addAll(generated));
            if (selfData.getMineLocations().isEmpty()) {
                this.plugin.getComponentManager().getMineComponent().generateLocations(0, 0, 100).whenComplete((generatedLocations, generatedLocationsThrowable) -> {
                    selfData.getMineLocations().addAll(generatedLocations);
                });
            } else {
                final MineLocation latestMineLocation = selfData.getMineLocations().stream().sorted(Collections.reverseOrder()).findFirst().get();
                this.plugin.getComponentManager().getMineComponent().generateLocations(latestMineLocation.getX(), latestMineLocation.getZ(), 1).whenComplete((generatedLocations, generatedLocationsThrowable) -> {
                    selfData.getMineLocations().addAll(generatedLocations);
                });
            }
        }));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aInitialized data."));
    }

    /**
     * Allows you to terminate the data.
     */
    public void terminateData() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cTerminating data..."));
        final SelfData selfData = SelfData.getViaCache().get();
        selfData.save(false);
        selfData.uncache();
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            final PlayerData onlinePlayerData = PlayerData.getViaCache(onlinePlayer.getUniqueId()).get();
            if (onlinePlayerData.getMine().isPresent()) {
                final MineData onlinePlayerMineData = MineData.getViaCache(onlinePlayerData.getMine().get()).get();
                onlinePlayerMineData.save(false);
                onlinePlayerMineData.uncache();
            }
            onlinePlayerData.save(false);
            onlinePlayerData.uncache();
        });
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cTerminated data."));
    }

}
