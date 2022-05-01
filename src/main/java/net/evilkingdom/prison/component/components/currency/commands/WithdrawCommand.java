package net.evilkingdom.prison.component.components.currency.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.inventory.InventoryUtilities;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import net.evilkingdom.prison.Prison;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class WithdrawCommand extends CommandHandler {

    private final Prison plugin;

    /**
     * Allows you to create the command.
     */
    public WithdrawCommand() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "withdraw");
        command.setAliases(new ArrayList<>(Arrays.asList("note", "banknote")));
        command.setHandler(this);
        command.register();
    }

    /**
     * The execution of the command.
     * Just uses the bukkit arguments since bukkit handles the magic.
     */
    @Override
    public boolean onExecution(final CommandSender commandSender, final String[] arguments) {
        if (!(commandSender instanceof Player)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.invalid-executor").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
            return false;
        }
        final Player player = (Player) commandSender;
        if (arguments.length != 2) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.pitch"));
            return false;
        }
        final String currency = arguments[0].toLowerCase();
        if (!Arrays.asList("gems", "tokens").contains(currency)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.invalid-currency").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.pitch"));
            return false;
        }
        if (!NumberUtilities.isLong(arguments[1])) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.invalid-amount.not-a-long").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.pitch"));
            return false;
        }
        final long amount = Long.parseLong(arguments[1]);
        if (amount <= 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.invalid-amount.amount-too-little").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.pitch"));
            return false;
        }
        final String formattedAmount = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols." + currency) + NumberUtilities.format(amount, NumberFormatType.COMMAS);
        PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
            switch (currency) {
                case "tokens" -> {
                    if (playerData.getTokens() < amount) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.invalid-amount.not-enough").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%currency%", currency))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.pitch"));
                        return;
                    }
                }
                case "gems" -> {
                    if (playerData.getGems() < amount) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.invalid-amount.not-enough").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%currency%", currency))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.pitch"));
                        return;
                    }
                }
            }
            final ItemStack note = this.plugin.getComponentManager().getCurrencyComponent().getNoteItem(currency, amount, player.getName());
            if (!InventoryUtilities.canFit(player.getInventory(), note)) {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.invalid-inventory").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.error.pitch"));
                return;
            }
            switch (currency) {
                case "tokens" -> playerData.setTokens(playerData.getTokens() - amount);
                case "gems" -> playerData.setGems(playerData.getGems() - amount);
            }
            player.getInventory().addItem(note);
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.withdraw.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%amount%", formattedAmount).replace("%currency%", currency))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.withdraw.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.withdraw.sounds.success.pitch"));
        });
        return true;
    }

    /**
     * The tab completion of the command.
     * Just uses the bukkit arguments since bukkit handles the magic and the converter filters the options returned.
     */
    @Override
    public ArrayList<String> onTabCompletion(final CommandSender commandSender, final String[] arguments) {
        if (!(commandSender instanceof Player)) {
            return new ArrayList<String>();
        }
        final Player player = (Player) commandSender;
        ArrayList<String> tabCompletion = new ArrayList<String>();
        switch (arguments.length) {
            case 1 -> tabCompletion.addAll(Arrays.asList("gems", "tokens"));
        }
        return tabCompletion;
    }

}
