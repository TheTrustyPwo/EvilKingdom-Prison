package net.evilkingdom.prison.component.components.currency.listeners;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.item.objects.ItemData;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemListener implements Listener {

    private final Prison plugin;

    /**
     * Allows you to create the listener.
     */
    public ItemListener() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to register the listener.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    /**
     * The listener for item clicks.
     */
    @EventHandler
    public void onInventoryClick(final PlayerInteractEvent playerInteractEvent) {
        if (playerInteractEvent.getItem() == null) {
            return;
        }
        if ((playerInteractEvent.getAction() != Action.RIGHT_CLICK_AIR) && (playerInteractEvent.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        final Player player = playerInteractEvent.getPlayer();
        final ItemStack item = playerInteractEvent.getItem();
        final ItemData itemData = new ItemData(this.plugin, item);
        if (!itemData.hasKey("note", PersistentDataType.STRING)) {
            return;
        }
        playerInteractEvent.setCancelled(true);
        final String currency = (String) itemData.getValue("note", PersistentDataType.STRING);
        final long amount = (long) itemData.getValue("amount", PersistentDataType.LONG);
        final String formattedAmount = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols." + currency) + NumberUtilities.format(amount, NumberFormatType.COMMAS);
        PlayerData.get(player.getUniqueId()).whenComplete(((playerData, throwable) -> {
            switch (currency) {
                case "tokens" -> playerData.setTokens((playerData.getTokens() + amount));
                case "gems" -> playerData.setGems((playerData.getGems() + amount));
            }
            player.sendMessage(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraaw.note.messages.success").replace("%currency%", currency).replace("%amount%", formattedAmount)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraaw.note.sounds.success.sound")), ((float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.note.sounds.success.volume")), ((float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.note.sounds.success.pitch")));
            item.setAmount((item.getAmount() - 1));
            player.getInventory().setItem(playerInteractEvent.getHand(), item);
        }));
    }

}
