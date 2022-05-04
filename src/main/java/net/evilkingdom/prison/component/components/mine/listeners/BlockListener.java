package net.evilkingdom.prison.component.components.mine.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.border.enums.BorderColor;
import net.evilkingdom.commons.border.objects.Border;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.objects.MineData;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;
import java.util.UUID;

public class BlockListener implements Listener {

    private final Prison plugin;

    /**
     * Allows you to create the listener.
     */
    public BlockListener() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to register the listener.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    /**
     * The listener for players breaking blocks.
     */
    @EventHandler
    public void onBlockBreak(final BlockBreakEvent blockBreakEvent) {
        final Player player = blockBreakEvent.getPlayer();
        final Block block = blockBreakEvent.getBlock();
        if (player.getWorld() != this.plugin.getComponentManager().getMineComponent().getWorld()) {
            return;
        }
        final Optional<UUID> optionalMineUUID = this.plugin.getComponentManager().getMineComponent().get(block.getLocation());
        if (optionalMineUUID.isPresent()) {
            final UUID mineUUID = optionalMineUUID.get();
            if (!this.plugin.getComponentManager().getMineComponent().isWithinInner(mineUUID, block.getLocation())) {
                blockBreakEvent.setCancelled(true);
                return;
            }
            //fun shit for enchants and all that here!!!!
        }
    }

}
