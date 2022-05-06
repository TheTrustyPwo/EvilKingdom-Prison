package net.evilkingdom.prison.component.components.currency.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.mojang.MojangUtilities;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import net.evilkingdom.prison.Prison;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class BalanceCommand extends CommandHandler {

    private final Prison plugin;

    /**
     * Allows you to create the command.
     */
    public BalanceCommand() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "balance");
        command.setAliases(new ArrayList<>(Arrays.asList("bal", "gems", "tokens", "money", "cash", "currency")));
        command.setHandler(this);
        command.register();
    }

    /**
     * The execution of the command.
     *
     * @param sender ~ The command's sender.
     * @param arguments ~ The command's arguments.
     */
    @Override
    public void onExecution(final CommandSender sender, final String[] arguments) {
        if (!(sender instanceof Player)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.balance.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            return;
        }
        final Player player = (Player) sender;
        if (arguments.length < 1 || arguments.length > 2) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.balance.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.balance.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.sounds.error.pitch"));
            return;
        }
        final String currency = arguments[0].toLowerCase();
        if (!Arrays.asList("gems", "tokens").contains(currency)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.balance.messages.invalid-currency").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.balance.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.sounds.error.pitch"));
            return;
        }
        switch (arguments.length) {
            case 1 -> {
                final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                long amount = 0L;
                switch (currency) {
                    case "gems" -> amount = playerData.getGems();
                    case "tokens" -> amount = playerData.getTokens();
                }
                final String formattedAmount = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols." + currency) + NumberUtilities.format(amount, NumberFormatType.COMMAS);
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.balance.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%currency%", currency).replace("%amount%", formattedAmount))));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.balance.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.sounds.success.pitch"));
            }
            case 2 -> {
                if (arguments[1].equalsIgnoreCase(player.getName())) {
                    player.chat("/balance " + currency);
                    return;
                }
                MojangUtilities.getUUID(arguments[1]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
                    if (optionalTargetUUID.isEmpty()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.balance.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[1]))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                        return;
                    }
                    final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
                    PlayerData.get(offlineTarget.getUniqueId()).whenComplete((targetData, targetDataThrowable) -> {
                        targetData.exists().whenComplete((targetDataExists, targetDataExistsThrowable) -> {
                            if (!targetDataExists) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.balance.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                                return;
                            }
                            long amount = 0L;
                            switch (currency) {
                                case "gems" -> amount = targetData.getGems();
                                case "tokens" -> amount = targetData.getTokens();
                            }
                            final String formattedAmount = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols." + currency) + NumberUtilities.format(amount, NumberFormatType.COMMAS);
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.currency.commands.balance.messages.success.player-with-target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()).replace("%currency%", currency).replace("%amount%", formattedAmount))));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.commands.balance.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.currency.commands.balance.sounds.success.pitch"));
                        });
                    });
                });
            }
        }
    }

    /**
     * The tab completion of the command.
     *
     * @param sender ~ The command's sender.
     * @param arguments ~ The command's arguments.
     */
    @Override
    public ArrayList<String> onTabCompletion(final CommandSender sender, final String[] arguments) {
        if (!(sender instanceof Player)) {
            return new ArrayList<String>();
        }
        final Player player = (Player) sender;
        ArrayList<String> tabCompletion = new ArrayList<String>();
        switch (arguments.length) {
            case 1 -> tabCompletion.addAll(Arrays.asList("gems", "tokens"));
            case 2 -> {
                final ArrayList<String> playerNames = new ArrayList<String>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
                playerNames.remove(player.getName());
                tabCompletion.addAll(playerNames);
            }
        }
        return tabCompletion;
    }

}
