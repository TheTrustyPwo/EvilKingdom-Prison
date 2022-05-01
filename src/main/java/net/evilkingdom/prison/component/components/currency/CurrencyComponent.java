package net.evilkingdom.prison.component.components.currency;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.item.objects.Item;
import net.evilkingdom.commons.item.objects.ItemData;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.components.currency.listeners.ItemListener;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.currency.commands.BalanceCommand;
import net.evilkingdom.prison.component.components.currency.commands.PayCommand;
import net.evilkingdom.prison.component.components.currency.commands.WithdrawCommand;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class CurrencyComponent {

    private final Prison plugin;

    /**
     * Allows you to create the component.
     */
    public CurrencyComponent() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Currency] &aInitializing..."));
        this.registerCommands();
        this.registerListeners();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Currency] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Currency] &cTerminating..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Currency] &cTerminated."));
    }

    /**
     * Allows you to register the commands.
     */
    private void registerCommands() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Currency] &aRegistering commands..."));
        new BalanceCommand().register();
        new WithdrawCommand().register();
        new PayCommand().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Currency] &aRegistered commands."));
    }

    /**
     * Allows you to register the listeners.
     */
    private void registerListeners() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Currency] &aRegistering listeners..."));
        new ItemListener().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Currency] &aRegistered listeners."));
    }

    /**
     * Allows you to retrieve a note item.
     *
     * @param currency ~ The note's currency (either "gems" or "tokens").
     * @param amount ~ The note's amount (it's value).
     * @param withdrawer ~ The note's withdrawer (their IGN).
     * @return The note item.
     */
    public ItemStack getNoteItem(final String currency, final long amount, final String withdrawer) {
        final String formattedAmount = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols." + currency) + NumberUtilities.format(amount, NumberFormatType.COMMAS);
        final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.note.item.material"));
        final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.note.item.name").replace("%currency%", WordUtils.capitalizeFully(currency)).replace("%withdrawer%", withdrawer).replace("%amount%", formattedAmount)));
        final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.note.item.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine.replace("%currency%", WordUtils.capitalizeFully(currency)).replace("%withdrawer%", withdrawer).replace("%amount%", formattedAmount))).collect(Collectors.toList())));
        final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.currency.commands.withdraw.note.item.glowing");
        final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.currency.commands.withdraw.note.item.unbreakable");
        final Item item = new Item(new ItemStack(material));
        name.ifPresent(presentName -> item.setName(presentName));
        lore.ifPresent(presentLore -> item.setLore(presentLore));
        if (unbreakable) {
            item.unbreakable();
        }
        if (glowing) {
            item.glow();
        }
        final ItemData itemData = new ItemData(this.plugin, item.getItemStack());
        itemData.setValue("note", currency.toLowerCase(), PersistentDataType.STRING);
        itemData.setValue("amount", amount, PersistentDataType.LONG);
        return itemData.getItemStack();
    }

}
