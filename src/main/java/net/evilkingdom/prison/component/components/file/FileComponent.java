package net.evilkingdom.prison.component.components.file;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.Prison;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileComponent {

    private final Prison plugin;

    public FileConfiguration configuration;

    /**
     * Allows you to create the component.
     */
    public FileComponent() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aInitializing..."));
        this.initializeFiles();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » File] &cTerminating..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » File] &cTerminated."));
    }

    /**
     * Allows you to initialize the files.
     */
    private void initializeFiles() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aInitializing files..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aChecking if root folder exists..."));
        final File rootFolder = this.plugin.getDataFolder();
        if (!rootFolder.exists()) {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aRoot folder does not exist, creating it now..."));
            rootFolder.mkdirs();
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aRoot folder created."));
        } else {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aRoot folder exists."));
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aChecking if schematics folder exists..."));
        final File schematicsFolder = new File(rootFolder, "schematics");
        if (!schematicsFolder.exists()) {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aSchematics folder does not exist, creating it now..."));
            schematicsFolder.mkdirs();
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aSchematics folder created."));
        } else {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aSchematics folder exists."));
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aChecking if configuration file exists..."));
        final File configurationFile = new File(rootFolder, "configuration.yml");
        if (!configurationFile.exists()) {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aConfiguration file does not exist, creating it now..."));
            configurationFile.getParentFile().mkdirs();
            this.plugin.saveResource("configuration.yml", false);
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aConfiguration file created."));
        } else {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aConfiguration file exists."));
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aLoading configuration file..."));
        this.configuration = new YamlConfiguration();
        try {
            this.configuration.load(configurationFile);
        } catch (final IOException | InvalidConfigurationException ioException) {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » File] &cFailed to load configuration file, terminating to prevent a shitshow."));
            this.plugin.getPluginLoader().disablePlugin(this.plugin);
        }
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aConfiguration file loaded."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » File] &aFiles initialized."));
    }

    /**
     * Allows you to retrieve the configuration.
     *
     * @return The configuration.
     */
    public FileConfiguration getConfiguration() {
        return this.configuration;
    }

}
