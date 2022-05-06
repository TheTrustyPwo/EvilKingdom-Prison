package net.evilkingdom.prison.component.components.data.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.mojang.MojangUtilities;
import net.evilkingdom.commons.utilities.number.NumberUtilities;
import net.evilkingdom.commons.utilities.number.enums.NumberFormatType;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DataCommand extends CommandHandler {

    private final Prison plugin;

    /**
     * Allows you to create the command.
     */
    public DataCommand() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "data");
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
        if (arguments.length == 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.help.messages.invalid-usage").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.error.pitch"));
            }
            return;
        }
        final String subCommand = arguments[0].toLowerCase();
        switch (subCommand) {
            default -> {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.help.messages.invalid-usage").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.error.pitch"));
                }
            }
            case "help" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.help.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("prison.data.commands.data")) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.help.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.error.pitch"));
                    return;
                }
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.help.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.error.pitch"));
                    return;
                }
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.help.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.help.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.help.sounds.success.pitch"));
            }
            case "modify" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (!LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).contains("prison.data.commands.data")) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-permissions").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                    return;
                }
                if (arguments.length != 5) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                    return;
                }
                MojangUtilities.getUUID(arguments[1]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
                    if (optionalTargetUUID.isEmpty()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[1]))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                        return;
                    }
                    final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
                    PlayerData.get(offlineTarget.getUniqueId()).whenComplete((targetData, targetDataThrowable) -> {
                        targetData.exists().whenComplete((targetDataExists, targetDataExistsThrowable) -> {
                            if (!targetDataExists) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                                return;
                            }
                            final String dataType = arguments[2].toLowerCase();
                            if (!Arrays.asList("rank", "tokens", "gems", "blocks_mined", "multiplier").contains(dataType)) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-data-type").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                                return;
                            }
                            final String action = arguments[3].toLowerCase();
                            if (!Arrays.asList("set", "add", "subtract").contains(action)) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-action").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                                return;
                            }
                            if (dataType.equals("multiplier")) {
                                if (!NumberUtilities.isDouble(arguments[4])) {
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-amount.not-a-double").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                                    return;
                                }
                            } else {
                                if (!NumberUtilities.isLong(arguments[4])) {
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-amount.not-a-long").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                                    return;
                                }
                            }
                            final double amount = Double.parseDouble(arguments[4]);
                            if (Double.parseDouble(arguments[4]) <= 0) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.invalid-amount.amount-to-little").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.error.pitch"));
                                return;
                            }
                            String formattedAmount;
                            if (dataType.equals("multiplier")) {
                                formattedAmount = NumberUtilities.format(amount, NumberFormatType.MULTIPLIER);
                            } else if (Arrays.asList("gems", "tokens").contains(dataType)) {
                                formattedAmount = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.currency.symbols." + dataType) + NumberUtilities.format(amount, NumberFormatType.COMMAS);
                            } else {
                                formattedAmount = NumberUtilities.format(amount, NumberFormatType.COMMAS);
                            }
                            switch (action) {
                                case "set" -> {
                                    switch (dataType) {
                                        case "rank" -> targetData.setRank(Math.round(amount));
                                        case "tokens" -> targetData.setTokens(Math.round(amount));
                                        case "gems" -> targetData.setGems(Math.round(amount));
                                        case "blocks_mined" -> targetData.setBlocksMined(Math.round(amount));
                                        case "multiplier" -> targetData.setMultiplier(amount);
                                    }
                                    if (offlineTarget.isOnline()) {
                                        final Player target = offlineTarget.getPlayer();
                                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.success.set.target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%data_type%", dataType).replace("%amount%", formattedAmount))));
                                        target.playSound(target.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.target.pitch"));
                                    } else {
                                        targetData.save(true);
                                    }
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.success.set.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()).replace("%data_type%", dataType).replace("%amount%", formattedAmount))));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.player.pitch"));
                                }
                                case "add" -> {
                                    switch (dataType) {
                                        case "rank" -> targetData.setRank((targetData.getRank() + Math.round(amount)));
                                        case "tokens" -> targetData.setTokens((targetData.getTokens() + Math.round(amount)));
                                        case "gems" -> targetData.setGems((targetData.getGems() + Math.round(amount)));
                                        case "blocks_mined" -> targetData.setBlocksMined((targetData.getBlocksMined() + Math.round(amount)));
                                        case "multiplier" -> targetData.setMultiplier((targetData.getMultiplier() + amount));
                                    }
                                    if (offlineTarget.isOnline()) {
                                        final Player target = offlineTarget.getPlayer();
                                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.success.add.target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%data_type%", dataType).replace("%amount%", formattedAmount))));
                                        target.playSound(target.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.target.pitch"));
                                    } else {
                                        targetData.save(true);
                                    }
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.success.add.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()).replace("%data_type%", dataType).replace("%amount%", formattedAmount))));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.player.pitch"));
                                }
                                case "subtract" -> {
                                    switch (dataType) {
                                        case "rank" -> targetData.setRank((targetData.getRank() - Math.round(amount)));
                                        case "tokens" -> targetData.setTokens((targetData.getTokens() - Math.round(amount)));
                                        case "gems" -> targetData.setGems((targetData.getGems() - Math.round(amount)));
                                        case "blocks_mined" -> targetData.setBlocksMined((targetData.getBlocksMined() - Math.round(amount)));
                                        case "multiplier" -> targetData.setMultiplier((targetData.getMultiplier() - amount));
                                    }
                                    if (offlineTarget.isOnline()) {
                                        final Player target = offlineTarget.getPlayer();
                                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.success.subtract.target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()).replace("%data_type%", dataType).replace("%amount%", formattedAmount))));
                                        target.playSound(target.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.target.pitch"));
                                    } else {
                                        targetData.save(true);
                                    }
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.data.commands.data.sub-commands.modify.messages.success.subtract.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()).replace("%data_type%", dataType).replace("%amount%", formattedAmount))));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.data.commands.data.sub-commands.modify.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.data.commands.data.sub-commands.modify.sounds.success.player.pitch"));
                                }
                            }
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
            case 1 -> tabCompletion.addAll(Arrays.asList("help", "modify"));
            case 2 -> tabCompletion.addAll(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
            case 3 -> tabCompletion.addAll(Arrays.asList("rank", "tokens", "gems", "blocks_mined", "multiplier"));
            case 4 -> tabCompletion.addAll(Arrays.asList("set", "add", "subtract"));
        }
        return tabCompletion;
    }

}
