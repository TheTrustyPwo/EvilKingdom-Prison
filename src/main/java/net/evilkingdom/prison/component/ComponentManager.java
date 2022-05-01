package net.evilkingdom.prison.component;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.components.rank.RankComponent;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.currency.CurrencyComponent;
import net.evilkingdom.prison.component.components.data.DataComponent;
import net.evilkingdom.prison.component.components.file.FileComponent;
import net.evilkingdom.prison.component.components.mine.MineComponent;
import net.evilkingdom.prison.component.components.scoreboard.ScoreboardComponent;
import org.bukkit.Bukkit;

public class ComponentManager {

    private final Prison plugin;

    private FileComponent fileComponent;
    private DataComponent dataComponent;
    private CurrencyComponent currencyComponent;
    private RankComponent rankComponent;
    private MineComponent mineComponent;
    private ScoreboardComponent scoreboardComponent;

    /**
     * Allows you to create the Component Manager.
     */
    public ComponentManager() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to initialize the Component Manager.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » ComponentManager] &aInitializing..."));
        this.fileComponent = new FileComponent();
        this.fileComponent.initialize();
        this.currencyComponent = new CurrencyComponent();
        this.currencyComponent.initialize();
        this.rankComponent = new RankComponent();
        this.rankComponent.initialize();
        this.mineComponent = new MineComponent();
        this.mineComponent.initialize();
        this.dataComponent = new DataComponent();
        this.dataComponent.initialize();
        this.scoreboardComponent = new ScoreboardComponent();
        this.scoreboardComponent.initialize();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » ComponentManager] &aInitialized."));
    }

    /**
     * Allows you to terminate the Component Manager.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » ComponentManager] &cTerminating..."));
        if (this.scoreboardComponent != null) {
            this.scoreboardComponent.terminate();
        }
        if (this.mineComponent != null) {
            this.mineComponent.terminate();
        }
        if (this.rankComponent != null) {
            this.rankComponent.terminate();
        }
        if (this.currencyComponent != null) {
            this.currencyComponent.terminate();
        }
        if (this.dataComponent != null) {
            this.dataComponent.terminate();
        }
        if (this.fileComponent != null) {
            this.fileComponent.terminate();
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » ComponentManager] &cTerminated."));
    }

    /**
     * Allows you to retrieve the File component.
     *
     * @return The File component.
     */
    public FileComponent getFileComponent() {
        return this.fileComponent;
    }

    /**
     * Allows you to retrieve the Data component.
     *
     * @return The Data component.
     */
    public DataComponent getDataComponent() {
        return this.dataComponent;
    }

    /**
     * Allows you to retrieve the Mine component.
     *
     * @return The Mine component.
     */
    public MineComponent getMineComponent() {
        return this.mineComponent;
    }

    /**
     * Allows you to retrieve the Rank component.
     *
     * @return The Rank component.
     */
    public RankComponent getRankComponent() {
        return this.rankComponent;
    }

    /**
     * Allows you to retrieve the Currency component.
     *
     * @return The Currency component.
     */
    public CurrencyComponent getCurrencyComponent() {
        return this.currencyComponent;
    }

    /**
     * Allows you to retrieve the Scoreboard component.
     *
     * @return The Scoreboard component.
     */
    public ScoreboardComponent getScoreboardComponent() {
        return this.scoreboardComponent;
    }

}
