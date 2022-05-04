package net.evilkingdom.prison.component.components.mine.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.border.enums.BorderColor;
import net.evilkingdom.commons.border.objects.Border;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.objects.MineData;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;
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
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.getWorld() != this.plugin.getComponentManager().getMineComponent().getWorld()) {
                return;
            }
            final Optional<UUID> optionalMineUUID = this.plugin.getComponentManager().getMineComponent().get(player.getLocation());
            optionalMineUUID.ifPresent(uuid -> MineData.get(uuid).whenComplete((mineData, mineDataThrowable) -> {
                final Border border = new Border(this.plugin, player, mineData.getCenter().toCenterLocation(), mineData.getBorderSize(), BorderColor.RED);
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> border.show(), 5L);
            }));
        }, 20L);
    }

}
