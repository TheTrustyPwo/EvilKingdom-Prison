package net.evilkingdom.prison.component.components.data.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.objects.MineData;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class ConnectionListener implements Listener {

    private final Prison plugin;

    /**
     * Allows you to create the listener.
     */
    public ConnectionListener() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to register the listener.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    /**
     * The listener for player joins.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent playerJoinEvent) {
        final Player player = playerJoinEvent.getPlayer();
        PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
            if (playerData.getMine().isPresent()) {
                //DONT DO THIS IF ITS ALRAEADY CACHED LOL
                MineData.get(playerData.getMine().get()).whenComplete((mineData, mineDataThrowable) -> mineData.cache());
            }
            playerData.cache();
        });
    }

    /**
     * The listener for player quits.
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent playerQuitEvent) {
        final Player player = playerQuitEvent.getPlayer();
        PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
            if (!playerData.isCached()) {
                return;
            }
            if (playerData.getMine().isPresent()) {
                MineData.get(playerData.getMine().get()).whenComplete((mineData, mineDataThrowable) -> {
                    if (!mineData.isCached()) {
                        return;
                    }
                    //IN THE FUTURE ADD A SYSTEM TO CHECK IF THE MINE HAS ANY PLAYERS AT IT AND DONT UNSAVE AND UNCACHE THEN.
                    //INSTEAD ITLL UNSAVE AND UNCACHE AUTOMATICALLY WHEN IT'S EMPTY AND CACHE WHEN IT ISN'T
                    mineData.save(true);
                    mineData.uncache();
                });
            }
            playerData.save(true);
            playerData.uncache();
        });
    }

}
