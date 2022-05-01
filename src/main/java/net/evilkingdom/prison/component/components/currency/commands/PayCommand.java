package net.evilkingdom.prison.component.components.currency.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import net.evilkingdom.prison.Prison;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class PayCommand extends CommandHandler {

    private final Prison plugin;

    /**
     * Allows you to create the command.
     */
    public PayCommand() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "pay");
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
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.invalid-executor").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
            return false;
        }
        final Player player = (Player) commandSender;
        if (arguments.length != 3) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.pitch"));
            return false;
        }
        final Optional<? extends Player> optionalTarget = Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.getName().equalsIgnoreCase(arguments[0])).findFirst();
        if (optionalTarget.isEmpty()) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[0]))));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.pitch"));
            return false;
        }
        final Player target = optionalTarget.get().getPlayer();
        final String currency = arguments[1].toLowerCase();
        if (!Arrays.asList("gems", "tokens").contains(currency)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.invalid-currency").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.pitch"));
            return false;
        }
        if (!NumberUtilities.isLong(arguments[2])) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.invalid-amount.not-a-long").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.pitch"));
            return false;
        }
        final long amount = Long.parseLong(arguments[2]);
        if (amount <= 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.invalid-amount.amount-too-little").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.pitch"));
            return false;
        }
        final String formattedAmount = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols." + currency) + NumberUtilities.format(amount, NumberFormatType.COMMAS);
        PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
            switch (currency) {
                case "tokens" -> {
                    if (playerData.getTokens() < amount) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.invalid-amount.not-enough").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%currency%", currency))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.pitch"));
                        return;
                    }
                }
                case "gems" -> {
                    if (playerData.getGems() < amount) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.invalid-amount.not-enough").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%currency%", currency))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.error.pitch"));
                        return;
                    }
                }
            }
           PlayerData.get(target.getUniqueId()).whenComplete((targetData, targetDataThrowable) -> {
               switch (currency) {
                   case "tokens" -> {
                       playerData.setTokens(playerData.getTokens() - amount);
                       targetData.setTokens(targetData.getTokens() + amount);
                   }
                   case "gems" -> {
                       playerData.setGems(playerData.getGems() - amount);
                       targetData.setGems(targetData.getGems() + amount);
                   }
               }
               this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", target.getName()).replace("%amount%", formattedAmount).replace("%currency%", currency))));
               player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.success.player.pitch"));
               this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.pay.messages.success.target").forEach(string -> target.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%amount%", formattedAmount).replace("%currency%", currency))));
               target.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.pay.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.pay.sounds.success.target.pitch"));
           });
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
            case 1 -> {
                final ArrayList<String> playerNames = new ArrayList<String>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
                playerNames.remove(player.getName());
                tabCompletion.addAll(playerNames);
            }
            case 2 -> tabCompletion.addAll(Arrays.asList("gems", "tokens"));
        }
        return tabCompletion;
    }

}
