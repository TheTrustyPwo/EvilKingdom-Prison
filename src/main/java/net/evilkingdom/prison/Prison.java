package net.evilkingdom.prison;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.Commons;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.ComponentManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Prison extends JavaPlugin {

    private static Prison plugin;
    private ComponentManager componentManager;

    /**
     * Bukkit's detection for the plugin enabling.
     */
    public void onEnable() {
        this.initialize();
    }

    /**
     * Bukkit's detection for the plugin disabling.
     */
    public void onDisable() {
        this.terminate();
    }

    /**
     * Allows you to initialize the plugin.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison] &aInitializing..."));
        plugin = this;
        this.componentManager = new ComponentManager();
        this.componentManager.initialize();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison] &aInitialized."));
    }

    /**
     * Allows you to terminate the plugin.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison] &cTerminating..."));
        this.componentManager.terminate();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison] &cTerminated."));
    }

    /**
     * Allows you to retrieve the plugin.
     *
     * @return The plugin.
     */
    public static Prison getPlugin() {
        return plugin;
    }

    /**
     * Allows you to retrieve the Component Manager.
     *
     * @return The Component Manager.
     */
    public ComponentManager getComponentManager() {
        return this.componentManager;
    }

}
