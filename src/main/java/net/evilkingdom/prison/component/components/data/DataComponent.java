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
        new Datapoint(datasite, "prison_players");
        new Datapoint(datasite, "prison_mines");
        new Datapoint(datasite, "prison_self");
        try {
            datasite.initialize();
        } catch (final Exception exception) {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&c[Prison » Component » Components » Data] Failed to connect to database, terminating to prevent a shitshow."));
            this.plugin.getPluginLoader().disablePlugin(this.plugin);
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aConnected to database."));
    }

    /**
     * Allows you to disconnect to the Mongo database.
     */
    private void disconnectFromDatabase() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cDisconnecting from database..."));
        final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
        final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
        datasite.terminate();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cDisconnected from database."));
    }

    /**
     * Allows you to initialize the data.
     */
    public void initializeData() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aInitializing data..."));
        CompletableFuture.supplyAsync(() -> {
            final DataImplementor dataImplementor = DataImplementor.get(this.plugin);
            final Datasite datasite = dataImplementor.getDatasites().stream().filter(innerDatasite -> innerDatasite.getPlugin() == this.plugin).findFirst().get();
            if (datasite.getMongoClient().getDatabase(datasite.getName()).getCollection("prison_players").countDocuments() == 0L)  {
                return 1000L;
            } else {
                return (datasite.getMongoClient().getDatabase(datasite.getName()).getCollection("prison_players").find().sort(Sorts.descending("rank")).first().getLong("rank") + 1000L);
            }
        }).whenComplete((rankGenerationAmount, rankGenerationAmountThrowable) -> {
            SelfData.get().whenComplete((selfData, selfDataThrowable) -> {
                selfData.cache();
                selfData.getRanks().clear();
                this.plugin.getComponentManager().getRankComponent().generate(0, rankGenerationAmount).whenComplete((generatedRanks, generatedRanksThrowable) -> selfData.getRanks().addAll(generatedRanks));
                if (selfData.getMineLocations().isEmpty()) {
                    this.plugin.getComponentManager().getMineComponent().generateLocations(0, 0, 100).whenComplete((generatedMineLocations, generatedMineLocationsThrowable) -> {
                        selfData.getMineLocations().addAll(generatedMineLocations);
                    });
                } else {
                    final MineLocation latestMineLocation = selfData.getMineLocations().get((selfData.getMineLocations().size() - 1));
                    this.plugin.getComponentManager().getMineComponent().generateLocations(latestMineLocation.getX(), latestMineLocation.getZ(), 1).whenComplete((generatedMineLocations, generatedMineLocationsThrowable) -> {
                        selfData.getMineLocations().addAll(generatedMineLocations);
                    });
                }
            });
        });
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Data] &aInitialized data."));
    }

    /**
     * Allows you to terminate the data.
     */
    public void terminateData() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cTerminating data..."));
        try {
            final SelfData selfData = SelfData.get().get();
            selfData.save(false);
            selfData.uncache();
        } catch (final ExecutionException | InterruptedException executionException) {
            //Does nothing, just in case! :)
        }
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            try {
                final PlayerData onlinePlayerData = PlayerData.get(onlinePlayer.getUniqueId()).get();
                if (onlinePlayerData.getMine().isPresent()) {
                    final MineData onlinePlayerMineData = MineData.get(onlinePlayerData.getMine().get()).get();
                    onlinePlayerMineData.save(false);
                    onlinePlayerMineData.uncache();
                }
                onlinePlayerData.save(false);
                onlinePlayerData.uncache();
            } catch (final ExecutionException | InterruptedException executionException) {
                //Does nothing, just in case! :)
            }
        });
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Data] &cTerminated data."));
    }

}
