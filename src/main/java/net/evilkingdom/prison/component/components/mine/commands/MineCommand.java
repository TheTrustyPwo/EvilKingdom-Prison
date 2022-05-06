package net.evilkingdom.prison.component.components.mine.commands;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.basics.component.components.data.objects.SelfData;
import net.evilkingdom.commons.border.enums.BorderColor;
import net.evilkingdom.commons.border.objects.Border;
import net.evilkingdom.commons.command.abstracts.CommandHandler;
import net.evilkingdom.commons.command.objects.Command;
import net.evilkingdom.commons.constructor.objects.ConstructorRegion;
import net.evilkingdom.commons.cooldown.objects.Cooldown;
import net.evilkingdom.commons.item.objects.Item;
import net.evilkingdom.commons.item.objects.ItemData;
import net.evilkingdom.commons.menu.MenuImplementor;
import net.evilkingdom.commons.menu.objects.Menu;
import net.evilkingdom.commons.menu.objects.MenuItem;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.mojang.MojangUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.commons.utilities.time.TimeUtilities;
import net.evilkingdom.prison.component.components.data.objects.MineData;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import net.evilkingdom.prison.component.components.mine.enums.MineTaskType;
import net.minecraft.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class MineCommand extends CommandHandler {

    private final Prison plugin;

    /**
     * Allows you to create the command.
     */
    public MineCommand() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to register the command.
     */
    public void register() {
        final Command command = new Command(this.plugin, "mine");
        command.setAliases(new ArrayList<String>(Arrays.asList("mines", "cell", "island", "is")));
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
            if (!(sender instanceof Player)) {
                Bukkit.getServer().dispatchCommand(sender, "mine help");
                return;
            }
            final Player player = (Player) sender;
            final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
            if (playerData.getMine().isEmpty()) {
                player.chat("/mine create");
            } else {
                player.chat("/mine panel");
            }
            return;
        }
        final String subCommand = arguments[0].toLowerCase();
        switch (subCommand) {
            default -> {
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.invalid-usage").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.pitch"));
                }
            }
            case "help" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.pitch"));
                    return;
                }
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.help.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.success.pitch"));
            }
            case "go" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length > 2) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                    return;
                }
                switch (arguments.length) {
                    case 1 -> {
                        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                        if (playerData.getMine().isEmpty()) {
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-go.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                            return;
                        }
                        final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                        if (this.plugin.getComponentManager().getMineComponent().getTasks().getOrDefault(mineData.getUUID(), new ArrayList<MineTaskType>()).contains(MineTaskType.CHANGE_THEME)) {
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-go.mine-preforming-large-task").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                            return;
                        }
                        Bukkit.getScheduler().runTask(this.plugin, () -> player.teleport(mineData.getGo()));
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.success.no-target").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.success.pitch"));
                        final Border border = new Border(this.plugin, player, mineData.getCenter().toCenterLocation(), mineData.getBorderSize(), BorderColor.RED);
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> border.show(), 5L);
                    }
                    case 2 -> {
                        if (arguments[1].equalsIgnoreCase(player.getName())) {
                            player.chat("/mine go");
                            return;
                        }
                        MojangUtilities.getUUID(arguments[1]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
                            if (optionalTargetUUID.isEmpty()) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[1]))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                                return;
                            }
                            final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
                            PlayerData.get(offlineTarget.getUniqueId()).whenComplete((targetData, targetDataThrowable) -> {
                                targetData.exists().whenComplete((targetDataExists, targetDataExistsThrowable) -> {
                                    if (!targetDataExists) {
                                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                                        return;
                                    }
                                    if (targetData.getMine().isEmpty()) {
                                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-go.target-has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                                        return;
                                    }
                                    MineData.get(targetData.getMine().get()).whenComplete((mineData, mineDataThrowable) -> {
                                        if (this.plugin.getComponentManager().getMineComponent().getTasks().getOrDefault(mineData.getUUID(), new ArrayList<MineTaskType>()).contains(MineTaskType.CHANGE_THEME)) {
                                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-go.target-mine-preforming-large-task").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                                            return;
                                        }
                                        if (mineData.getBanned().contains(player.getUniqueId())) {
                                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-go.banned-from-target-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                                            return;
                                        }
                                        if (mineData.isPrivate() && !mineData.getWhitelisted().contains(player.getUniqueId())) {
                                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-go.target-mine-private").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                                            return;
                                        }
                                        if (!mineData.isCached()) {
                                            mineData.cache();
                                        }
                                        Bukkit.getScheduler().runTask(this.plugin, () -> player.teleport(mineData.getGo()));
                                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.success.with-target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.success.pitch"));
                                        final Border border = new Border(this.plugin, player, mineData.getCenter().toCenterLocation(), mineData.getBorderSize(), BorderColor.RED);
                                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> border.show(), 5L);
                                    });
                                });
                            });
                        });
                    }
                }
            }
            case "create" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.pitch"));
                    return;
                }
                final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                if (playerData.getMine().isPresent()) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.invalid-creation.already-has-a-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.pitch"));
                    return;
                }
                if (this.plugin.getComponentManager().getMineComponent().getPlayersWaitingForCreation().contains(player.getUniqueId())) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.invalid-creation.already-creating").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.pitch"));
                    return;
                }
                this.openCreateCreateMenu(player);
            }
            case "reset" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                    return;
                }
                final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                if (playerData.getMine().isEmpty()) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-reset.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                    return;
                }
                final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                final Optional<Cooldown> optionalCooldown = mineData.getCooldowns().stream().filter(cooldown -> cooldown.getIdentifier().equals("mine-" + mineData.getUUID() + "-reset")).findFirst();
                if (optionalCooldown.isPresent()) {
                    final Cooldown cooldown = optionalCooldown.get();
                    final String formattedTimeLeft = TimeUtilities.format((cooldown.getTimeLeft() * 50));
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-reset.on-cooldown").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%time_left%", formattedTimeLeft))));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                    return;
                }
                this.plugin.getComponentManager().getMineComponent().getPercentage(mineData.getUUID()).whenComplete((minePercentage, minePercentageThrowable) -> {
                    if (minePercentage > this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.reset-percentages.manual")) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-reset.mine-too-full").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                        return;
                    }
                    if (this.plugin.getComponentManager().getMineComponent().getTasks().getOrDefault(mineData.getUUID(), new ArrayList<MineTaskType>()).contains(MineTaskType.RESET)) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-reset.already-resetting").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                        return;
                    }
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.success.resetting").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.success.pitch"));
                    this.plugin.getComponentManager().getMineComponent().reset(mineData.getUUID()).whenComplete((resetSuccessful, resetSuccessfulThrowable) -> {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.success.reset").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.success.pitch"));
                        final ArrayList<Long> durations = new ArrayList<Long>(LuckPermsUtilities.getPermissionsViaCache(player.getUniqueId()).stream().filter(permission -> permission.startsWith("prison.mine.commands.mine.sub-commands.reset.cooldown.")).map(permission -> TimeUtilities.get(permission.replaceFirst("prison.mine.commands.mine.sub-commands.reset.cooldown.", "")) / 50).collect(Collectors.toList()));
                        Collections.sort(durations);
                        final Cooldown cooldown = new Cooldown(this.plugin, "mine-" + mineData.getUUID() + "-reset", durations.get(0));
                        cooldown.start();
                    });
                });
            }
            case "retheme" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.pitch"));
                    return;
                }
                final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                if (playerData.getMine().isEmpty()) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.invalid-retheme.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.pitch"));
                    return;
                }
                if (this.plugin.getComponentManager().getMineComponent().getTasks().getOrDefault(playerData.getMine().get(), new ArrayList<MineTaskType>()).contains(MineTaskType.CHANGE_THEME)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.invalid-retheme.already-retheming").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.pitch"));
                    return;
                }
                final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                final Optional<Cooldown> optionalCooldown = mineData.getCooldowns().stream().filter(cooldown -> cooldown.getIdentifier().equals("mine-" + mineData.getUUID() + "-retheme")).findFirst();
                if (optionalCooldown.isPresent()) {
                    final Cooldown cooldown = optionalCooldown.get();
                    final String formattedTimeLeft = TimeUtilities.format((cooldown.getTimeLeft() * 50));
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.invalid-retheme.on-cooldown").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%time_left%", formattedTimeLeft))));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.pitch"));
                    return;
                }
                this.openRethemeSelectThemeMenu(player);
            }
            case "ban" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 2) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                    return;
                }
                if (arguments[1].equalsIgnoreCase(player.getName())) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-player.self-ban").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                    return;
                }
                MojangUtilities.getUUID(arguments[1]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
                    if (optionalTargetUUID.isEmpty()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-player.does-not-exist").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[1]))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                        return;
                    }
                    final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
                    PlayerData.get(offlineTarget.getUniqueId()).whenComplete((targetData, targetDataThrowable) -> {
                        targetData.exists().whenComplete((targetDataExists, targetDataExistsThrowable) -> {
                            if (!targetDataExists) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-player.does-not-exist").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                                return;
                            }
                            final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                            if (playerData.getMine().isEmpty()) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-ban.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                                return;
                            }
                            final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                            if (mineData.getBanned().contains(offlineTarget.getUniqueId())) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-ban.already-banned").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                                return;
                            }
                            mineData.getBanned().add(offlineTarget.getUniqueId());
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.success.player.pitch"));
                            if (offlineTarget.isOnline()) {
                                final Player target = offlineTarget.getPlayer();
                                if (this.plugin.getComponentManager().getMineComponent().isWithin(mineData.getUUID(), target.getLocation())) {
                                    final SelfData selfData = SelfData.getViaCache().get();
                                    Bukkit.getScheduler().runTask(this.plugin, () -> target.teleport(selfData.getSpawn()));
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.success.target").forEach(string -> target.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()))));
                                    target.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.success.target.pitch"));
                                }
                            }
                        });
                    });
                });
            }
            case "unban" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 2) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.pitch"));
                    return;
                }
                if (arguments[1].equalsIgnoreCase(player.getName())) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-player.self-unban").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.pitch"));
                    return;
                }
                MojangUtilities.getUUID(arguments[1]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
                    if (optionalTargetUUID.isEmpty()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-player.does-not-exist").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[1]))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.pitch"));
                        return;
                    }
                    final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
                    PlayerData.get(offlineTarget.getUniqueId()).whenComplete((targetData, targetDataThrowable) -> {
                        targetData.exists().whenComplete((targetDataExists, targetDataExistsThrowable) -> {
                            if (!targetDataExists) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-player.does-not-exist").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.pitch"));
                                return;
                            }
                            final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                            if (playerData.getMine().isEmpty()) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-unban.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.pitch"));
                                return;
                            }
                            final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                            if (!mineData.getBanned().contains(offlineTarget.getUniqueId())) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-unban.not-banned").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unban.sounds.error.pitch"));
                                return;
                            }
                            mineData.getBanned().remove(offlineTarget.getUniqueId());
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.success.pitch"));
                        });
                    });
                });
            }
            case "privacy" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.privacy.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.privacy.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.privacy.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.error.pitch"));
                    return;
                }
                final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                if (playerData.getMine().isEmpty()) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.privacy.messages.invalid-privacy.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.privacy.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.error.pitch"));
                    return;
                }
                final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                if (mineData.isPrivate()) {
                    mineData.setPrivate(false);
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.privacy.messages.success.player.not-private").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.privacy.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.success.player.pitch"));
                    this.updatePanelManageVisitationMenu(player);
                } else {
                    mineData.setPrivate(true);
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.privacy.messages.success.player.now-private").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.privacy.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.success.player.pitch"));
                    this.updatePanelManageVisitationMenu(player);
                    Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> this.plugin.getComponentManager().getMineComponent().isWithin(mineData.getUUID(), onlinePlayer.getLocation()) && !mineData.getWhitelisted().contains(onlinePlayer.getUniqueId()) && onlinePlayer.getUniqueId() != player.getUniqueId()).forEach(onlinePlayer -> {
                        final SelfData selfData = SelfData.getViaCache().get();
                        Bukkit.getScheduler().runTask(this.plugin, () -> onlinePlayer.teleport(selfData.getSpawn()));
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.privacy.messages.success.target").forEach(string -> onlinePlayer.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()))));
                        onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.privacy.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.privacy.sounds.success.target.pitch"));
                    });
                }
            }
            case "whitelist" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.whitelist.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 2) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.whitelist.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.whitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.pitch"));
                    return;
                }
                if (arguments[1].equalsIgnoreCase(player.getName())) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.whitelist.messages.invalid-player.self-whitelist").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.whitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.pitch"));
                    return;
                }
                MojangUtilities.getUUID(arguments[1]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
                    if (optionalTargetUUID.isEmpty()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.whitelist.messages.invalid-player.does-not-exist").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[1]))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.whitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.pitch"));
                        return;
                    }
                    final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
                    PlayerData.get(offlineTarget.getUniqueId()).whenComplete((targetData, targetDataThrowable) -> {
                        targetData.exists().whenComplete((targetDataExists, targetDataExistsThrowable) -> {
                            if (!targetDataExists) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.whitelist.messages.invalid-player.does-not-exist").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.whitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.pitch"));
                                return;
                            }
                            final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                            if (playerData.getMine().isEmpty()) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.whitelist.messages.invalid-ban.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.whitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.pitch"));
                                return;
                            }
                            final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                            if (mineData.getWhitelisted().contains(offlineTarget.getUniqueId())) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.whitelist.messages.invalid-whitelist.already-whitelisted").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.whitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.error.pitch"));
                                return;
                            }
                            mineData.getWhitelisted().add(offlineTarget.getUniqueId());
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.whitelist.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.whitelist.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.whitelist.sounds.success.pitch"));
                        });
                    });
                });
            }
            case "unwhitelist" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unwhitelist.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 2) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unwhitelist.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.pitch"));
                    return;
                }
                if (arguments[1].equalsIgnoreCase(player.getName())) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-player.self-unwhitelist").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.pitch"));
                    return;
                }
                MojangUtilities.getUUID(arguments[1]).whenComplete((optionalTargetUUID, uuidThrowable) -> {
                    if (optionalTargetUUID.isEmpty()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unwhitelist.messages.invalid-player.does-not-exist").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", arguments[1]))));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.pitch"));
                        return;
                    }
                    final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(optionalTargetUUID.get());
                    PlayerData.get(offlineTarget.getUniqueId()).whenComplete((targetData, targetDataThrowable) -> {
                        targetData.exists().whenComplete((targetDataExists, targetDataExistsThrowable) -> {
                            if (!targetDataExists) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unwhitelist.messages.invalid-player.does-not-exist").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.pitch"));
                                return;
                            }
                            final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                            if (playerData.getMine().isEmpty()) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-unwhitelist.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.pitch"));
                                return;
                            }
                            final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                            if (!mineData.getWhitelisted().contains(offlineTarget.getUniqueId())) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unban.messages.invalid-unwhitelist.not-whitelisted").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.error.pitch"));
                                return;
                            }
                            mineData.getWhitelisted().remove(offlineTarget.getUniqueId());
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unwhitelist.messages.success.player").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", offlineTarget.getName()))));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unwhitelist.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.success.player.pitch"));
                            if (offlineTarget.isOnline()) {
                                final Player target = offlineTarget.getPlayer();
                                final SelfData selfData = SelfData.getViaCache().get();
                                Bukkit.getScheduler().runTask(this.plugin, () -> target.teleport(selfData.getSpawn()));
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.unwhitelist.messages.success.target").forEach(string -> target.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()))));
                                target.playSound(target.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.unwhitelist.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.unwhitelist.sounds.success.target.pitch"));
                            }
                        });
                    });
                });
            }
            case "panel" -> {
                if (!(sender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.messages.invalid-executor").forEach(string -> sender.sendMessage(StringUtilities.colorize(string)));
                    return;
                }
                final Player player = (Player) sender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.sounds.error.pitch"));
                    return;
                }
                final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                if (playerData.getMine().isEmpty()) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.messages.invalid-panel").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.sounds.error.pitch"));
                    return;
                }
                this.openPanelHomeMenu(player);
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
            case 1 -> tabCompletion.addAll(Arrays.asList("help", "create", "reset", "retheme", "panel", "go", "privacy", "ban", "unban", "whitelist", "unwhitelist"));
            case 2 -> {
                final String subCommand = arguments[0];
                final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                switch (subCommand) {
                    case "ban" -> {
                        if (playerData.getMine().isPresent()) {
                            final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                            tabCompletion.addAll(Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> !mineData.getBanned().contains(onlinePlayer.getUniqueId()) && onlinePlayer.getUniqueId() != player.getUniqueId()).map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
                        }
                    }
                    case "go" -> tabCompletion.addAll(Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> {
                        if (onlinePlayer.getUniqueId() == player.getUniqueId()) {
                            return false;
                        }
                        final PlayerData onlinePlayerData = PlayerData.getViaCache(onlinePlayer.getUniqueId()).get();
                        if (onlinePlayerData.getMine().isPresent()) {
                            final MineData mineData = MineData.getViaCache(onlinePlayerData.getMine().get()).get();
                            if (mineData.isPrivate()) {
                                return mineData.getWhitelisted().contains(player.getUniqueId());
                            } else {
                                return !mineData.getBanned().contains(player.getUniqueId());
                            }
                        }
                        return false;
                    }).map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
                    case "unban" -> {
                        if (playerData.getMine().isPresent()) {
                            final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                            tabCompletion.addAll(mineData.getBanned().stream().map(playerUUID -> Bukkit.getOfflinePlayer(playerUUID).getName()).collect(Collectors.toList()));
                        }
                    }
                    case "whitelist" -> {
                        if (playerData.getMine().isPresent()) {
                            final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                            tabCompletion.addAll(Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> !mineData.getWhitelisted().contains(onlinePlayer.getUniqueId()) && onlinePlayer.getUniqueId() != player.getUniqueId()).map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
                        }
                    }
                    case "unwhitelist" -> {
                        if (playerData.getMine().isPresent()) {
                            final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                            tabCompletion.addAll(mineData.getWhitelisted().stream().map(playerUUID -> Bukkit.getOfflinePlayer(playerUUID).getName()).collect(Collectors.toList()));
                        }
                    }
                }
            }
        }
        return tabCompletion;
    }

    /**
     * Allows you to open the "create" sub-command's create menu.
     *
     * @param player ~ The player to open it for.
     */
    private void openCreateCreateMenu(final Player player) {
        final InventoryType inventoryType = InventoryType.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.create.inventory.type"));
        final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.create.inventory.title"));
        Menu menu;
        if (inventoryType == InventoryType.CHEST) {
            final int rows = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.create.menus.create.inventory.rows");
            menu = new Menu(this.plugin, player, rows, title);
        } else {
            menu = new Menu(this.plugin, player, inventoryType, title);
        }
        menu.setIdentifier("mine-create");
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.create.menus.create.inventory.items").getKeys(false).stream().map(key -> Integer.parseInt(key)).collect(Collectors.toList()).forEach(slot -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.create.inventory.items." + slot + ".material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.create.inventory.items." + slot + ".name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.menus.create.inventory.items." + slot + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.create.menus.create.inventory.items." + slot + ".glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.create.menus.create.inventory.items." + slot + ".unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
            menu.getItems().put((slot - 1), menuItem);
        });
        final ArrayList<Integer> openSlots = new ArrayList<Integer>();
        int size;
        if (menu.getRows().isPresent()) {
            size = (menu.getRows().get() * 9);
        } else {
            size = (menu.getType().getDefaultSize() * 9);
        }
        for (int menuSlot = 0; menuSlot < size; menuSlot++) {
            if (!menu.getItems().containsKey(menuSlot)) {
                openSlots.add(menuSlot);
            }
        }
        final HashMap<Integer, String> themeSlots = new HashMap<Integer, String>();
        for (int i = 0; i < this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.themes").getKeys(false).stream().toList().size(); i++) {
            final String theme = this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.themes").getKeys(false).stream().toList().get(i);
            final int slot = openSlots.get(i);
            themeSlots.put(slot, theme);
        }
        themeSlots.forEach((slot, theme) -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.create.items.theme.material").replace("%related_material%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".related-material")));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.create.items.theme.name").replace("%name%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".prettified-name"))));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.menus.create.items.theme.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine.replace("%name%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".prettified-name")).replace("%description%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".description")))).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.create.menus.create.items.theme.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.create.menus.create.items.theme.unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
                player.closeInventory();
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.success.creating").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.success.pitch"));
                this.plugin.getComponentManager().getMineComponent().create(player, theme).whenComplete((mineUUID, mineUUIDThrowable) -> {
                    if (mineUUID.isEmpty()) {
                        return;
                    }
                    MineData.get(mineUUID.get()).whenComplete((mineData, mineDataThrowable) -> {
                        if (player.isOnline()) {
                            Bukkit.getScheduler().runTask(this.plugin, () -> player.teleport(mineData.getGo()));
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.success.created").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.success.pitch"));
                            final Border border = new Border(this.plugin, player, mineData.getCenter().toCenterLocation(), mineData.getBorderSize(), BorderColor.RED);
                            Bukkit.getScheduler().runTaskLater(this.plugin, () -> border.show(), 5L);
                        }
                    });
                });
            });
            menu.getItems().put(slot, menuItem);
        });
        menu.open();
    }

    /**
     * Allows you to open the "retheme" sub-command's select theme menu.
     *
     * @param player ~ The player to open it for.
     */
    private void openRethemeSelectThemeMenu(final Player player) {
        final InventoryType inventoryType = InventoryType.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.type"));
        final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.title"));
        Menu menu;
        if (inventoryType == InventoryType.CHEST) {
            final int rows = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.rows");
            menu = new Menu(this.plugin, player, rows, title);
        } else {
            menu = new Menu(this.plugin, player, inventoryType, title);
        }
        menu.setIdentifier("mine-retheme-retheme");
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.items").getKeys(false).stream().map(key -> Integer.parseInt(key)).collect(Collectors.toList()).forEach(slot -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.items." + slot + ".material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.items." + slot + ".name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.items." + slot + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.items." + slot + ".glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.retheme.menus.retheme.inventory.items." + slot + ".unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
            menu.getItems().put((slot - 1), menuItem);
        });
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items").getKeys(false).forEach(configurationItem -> {
            if (this.plugin.getComponentManager().getFileComponent().getConfiguration().isSet("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items." + configurationItem + ".slot")) {
                final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items." + configurationItem + ".slot");
                final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items." + configurationItem + ".material"));
                final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items." + configurationItem + ".name")));
                final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items." + configurationItem + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
                final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items." + configurationItem + ".glowing");
                final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items." + configurationItem + ".unbreakable");
                final Item item = new Item(new ItemStack(material));
                name.ifPresent(presentName -> item.setName(presentName));
                lore.ifPresent(presentLore -> item.setLore(presentLore));
                if (unbreakable) {
                    item.unbreakable();
                }
                if (glowing) {
                    item.glow();
                }
                final MenuItem menuItem;
                switch (configurationItem) {
                    case "previous-page" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.menus.retheme.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.menus.retheme.sounds.click.pitch"));
                        this.openPanelHomeMenu(player);
                    });
                    default -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.menus.retheme.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.menus.retheme.sounds.click.pitch"));
                    });
                }
                menu.getItems().put((slot - 1), menuItem);
            }
        });
        final ArrayList<Integer> openSlots = new ArrayList<Integer>();
        int size;
        if (menu.getRows().isPresent()) {
            size = (menu.getRows().get() * 9);
        } else {
            size = (menu.getType().getDefaultSize() * 9);
        }
        for (int menuSlot = 0; menuSlot < size; menuSlot++) {
            if (!menu.getItems().containsKey(menuSlot)) {
                openSlots.add(menuSlot);
            }
        }
        final HashMap<Integer, String> themeSlots = new HashMap<Integer, String>();
        for (int i = 0; i < this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.themes").getKeys(false).stream().toList().size(); i++) {
            final String theme = this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.themes").getKeys(false).stream().toList().get(i);
            final int slot = openSlots.get(i);
            themeSlots.put(slot, theme);
        }
        themeSlots.forEach((slot, theme) -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items.theme.material").replace("%related_material%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".related-material")));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items.theme.name").replace("%name%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".prettified-name"))));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items.theme.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine.replace("%name%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".prettified-name")).replace("%description%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".description")))).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items.theme.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.retheme.menus.retheme.items.theme.unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
                player.closeInventory();
                final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                final MineData preMineData = MineData.getViaCache(playerData.getMine().get()).get();
                if (preMineData.getTheme().equals(theme)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.invalid-retheme.already-using-theme").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%theme%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".prettified-name")))));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.error.pitch"));
                    return;
                }
                final SelfData selfData = SelfData.getViaCache().get();
                player.teleport(selfData.getSpawn());
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.success.player.retheming").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.success.player.pitch"));
                Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> this.plugin.getComponentManager().getMineComponent().isWithin(playerData.getMine().get(), onlinePlayer.getLocation())).forEach(onlinePlayer -> {
                    Bukkit.getScheduler().runTask(this.plugin, () -> onlinePlayer.teleport(selfData.getSpawn()));
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.success.target").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%player%", player.getName()))));
                    onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.success.target.pitch"));
                });
                this.plugin.getComponentManager().getMineComponent().changeTheme(playerData.getMine().get(), theme).whenComplete((changeThemeSuccessful, changeThemeSuccessfulThrowable) -> {
                    MineData.get(playerData.getMine().get()).whenComplete((mineData, mineDataThrowable) -> {
                        final Cooldown cooldown = new Cooldown(this.plugin, "mine-" + mineData.getUUID() + "-retheme", this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.retheme.cooldown"));
                        cooldown.start();
                        if (player.isOnline()) {
                            Bukkit.getScheduler().runTask(this.plugin, () -> player.teleport(mineData.getGo()));
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.retheme.messages.success.player.rethemed").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.retheme.sounds.success.player.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.success.player.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.retheme.sounds.success.player.pitch"));
                            final Border border = new Border(this.plugin, player, mineData.getCenter().toCenterLocation(), mineData.getBorderSize(), BorderColor.RED);
                            Bukkit.getScheduler().runTaskLater(this.plugin, () -> border.show(), 5L);
                        }
                        if (!mineData.isCached()) {
                            mineData.save(true);
                        }
                    });
                });
            });
            menu.getItems().put(slot, menuItem);
        });
        menu.open();
    }

    /**
     * Allows you to open the "panel" sub-command's home menu.
     *
     * @param player ~ The player to open it for.
     */
    private void openPanelHomeMenu(final Player player) {
        final InventoryType inventoryType = InventoryType.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.type"));
        final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.title"));
        Menu menu;
        if (inventoryType == InventoryType.CHEST) {
            final int rows = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.rows");
            menu = new Menu(this.plugin, player, rows, title);
        } else {
            menu = new Menu(this.plugin, player, inventoryType, title);
        }
        menu.setIdentifier("mine-panel-home");
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.items").getKeys(false).stream().map(key -> Integer.parseInt(key)).collect(Collectors.toList()).forEach(slot -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.items." + slot + ".material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.items." + slot + ".name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.items." + slot + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.items." + slot + ".glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.home.inventory.items." + slot + ".unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
            });
            menu.getItems().put((slot - 1), menuItem);
        });
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.home.items").getKeys(false).forEach(configurationItem -> {
            if (this.plugin.getComponentManager().getFileComponent().getConfiguration().isSet("components.mine.commands.mine.sub-commands.panel.menus.home.items." + configurationItem + ".slot")) {
                final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.home.items." + configurationItem + ".slot");
                final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.items." + configurationItem + ".material"));
                final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.items." + configurationItem + ".name")));
                final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.home.items." + configurationItem + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
                final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.home.items." + configurationItem + ".glowing");
                final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.home.items." + configurationItem + ".unbreakable");
                final Item item = new Item(new ItemStack(material));
                name.ifPresent(presentName -> item.setName(presentName));
                lore.ifPresent(presentLore -> item.setLore(presentLore));
                if (unbreakable) {
                    item.unbreakable();
                }
                if (glowing) {
                    item.glow();
                }
                final MenuItem menuItem;
                switch (configurationItem) {
                    case "go" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.pitch"));
                        player.chat("/mine go");
                    });
                    case "reset" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.pitch"));
                        player.chat("/mine reset");
                    });
                    case "retheme" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.pitch"));
                        player.chat("/mine retheme");
                    });
                    case "manage-visitation" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.pitch"));
                        this.openPanelManageVisitationMenu(player);
                    });
                    default -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.home.sounds.click.pitch"));
                    });
                }
                menu.getItems().put((slot - 1), menuItem);
            }
        });
        menu.open();
    }

    /**
     * Allows you to open the "panel" sub-command's manage visitation menu.
     *
     * @param player ~ The player to open it for.
     */
    private void openPanelManageVisitationMenu(final Player player) {
        final InventoryType inventoryType = InventoryType.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.type"));
        final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.title"));
        Menu menu;
        if (inventoryType == InventoryType.CHEST) {
            final int rows = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.rows");
            menu = new Menu(this.plugin, player, rows, title);
        } else {
            menu = new Menu(this.plugin, player, inventoryType, title);
        }
        menu.setIdentifier("mine-panel-manage-visitation");
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.items").getKeys(false).stream().map(key -> Integer.parseInt(key)).collect(Collectors.toList()).forEach(slot -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.items." + slot + ".material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.items." + slot + ".name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.items." + slot + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.items." + slot + ".glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.inventory.items." + slot + ".unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
            });
            menu.getItems().put((slot - 1), menuItem);
        });
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items").getKeys(false).forEach(configurationItem -> {
            if (this.plugin.getComponentManager().getFileComponent().getConfiguration().isSet("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items." + configurationItem + ".slot")) {
                final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items." + configurationItem + ".slot");
                final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items." + configurationItem + ".material"));
                final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items." + configurationItem + ".name")));
                final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items." + configurationItem + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
                final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items." + configurationItem + ".glowing");
                final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items." + configurationItem + ".unbreakable");
                final Item item = new Item(new ItemStack(material));
                name.ifPresent(presentName -> item.setName(presentName));
                lore.ifPresent(presentLore -> item.setLore(presentLore));
                if (unbreakable) {
                    item.unbreakable();
                }
                if (glowing) {
                    item.glow();
                }
                final MenuItem menuItem;
                switch (configurationItem) {
                    case "privacy" -> {
                        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
                        final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
                        String formattedPrivacy = "Disabled";
                        if (mineData.isPrivate()) {
                            formattedPrivacy = "Enabled";
                        }
                        final Optional<String> optionalPrivaciedName = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items." + configurationItem + ".name").replace("%privacy%", formattedPrivacy)));
                        optionalPrivaciedName.ifPresent(privaciedName -> item.setName(privaciedName));
                        menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                            inventoryClickEvent.setCancelled(true);
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.pitch"));
                            player.chat("/mine privacy");
                        });
                    }
                    case "bans" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.pitch"));
                        this.openPanelBansMenu(player);
                    });
                    case "whitelisted" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.pitch"));
                        this.openPanelWhitelistedMenu(player);
                    });
                    case "previous-page" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.pitch"));
                        this.openPanelHomeMenu(player);
                    });
                    default -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.pitch"));
                    });
                }
                menu.getItems().put((slot - 1), menuItem);
            }
        });
        menu.open();
    }

    /**
     * Allows you to update the "panel" sub-command's manage visitation menu.
     * This is used when toggling privacy, and it'll detect if they have a manage visitation menu open, and it'll update it.
     *
     * @param player ~ The player to open it for.
     */
    private void updatePanelManageVisitationMenu(final Player player) {
        final Optional<Menu> optionalMenu = MenuImplementor.get(this.plugin).getMenus().stream().filter(menu -> menu.getPlayer() == player && menu.getIdentifier().equals("mine-panel-manage-visitation")).findFirst();
        if (optionalMenu.isEmpty()) {
            return;
        }
        final Menu menu = optionalMenu.get();
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
        String formattedPrivacy = "Disabled";
        if (mineData.isPrivate()) {
            formattedPrivacy = "Enabled";
        }
        final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items.privacy.slot");
        final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items.privacy.material"));
        final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items.privacy.name").replace("%privacy%", formattedPrivacy)));
        final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items.privacy.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
        final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items.privacy.glowing");
        final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.items.privacy.unbreakable");
        final Item item = new Item(new ItemStack(material));
        name.ifPresent(presentName -> item.setName(presentName));
        lore.ifPresent(presentLore -> item.setLore(presentLore));
        if (unbreakable) {
            item.unbreakable();
        }
        if (glowing) {
            item.glow();
        }
        final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
            inventoryClickEvent.setCancelled(true);
            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.pitch"));
            player.chat("/mine privacy");
        });
        menu.getItems().put((slot - 1), menuItem);
        menu.update();
    }

    /**
     * Allows you to open the "panel" sub-command's bans menu.
     *
     * @param player ~ The player to open it for.
     */
    private void openPanelBansMenu(final Player player) {
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
        final InventoryType inventoryType = InventoryType.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.type"));
        final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.title").replace("%type%", "Bans"));
        Menu menu;
        if (inventoryType == InventoryType.CHEST) {
            final int rows = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.rows");
            menu = new Menu(this.plugin, player, rows, title);
        } else {
            menu = new Menu(this.plugin, player, inventoryType, title);
        }
        menu.setIdentifier("mine-panel-bans");
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items").getKeys(false).stream().map(key -> Integer.parseInt(key)).collect(Collectors.toList()).forEach(slot -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
            });
            menu.getItems().put((slot - 1), menuItem);
        });
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.user-control.items").getKeys(false).forEach(configurationItem -> {
            if (!configurationItem.equals("next-page") && this.plugin.getComponentManager().getFileComponent().getConfiguration().isSet("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".slot")) {
                final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".slot");
                final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".material"));
                final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".name")));
                final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
                final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".glowing");
                final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".unbreakable");
                final Item item = new Item(new ItemStack(material));
                name.ifPresent(presentName -> item.setName(presentName));
                lore.ifPresent(presentLore -> item.setLore(presentLore));
                if (unbreakable) {
                    item.unbreakable();
                }
                if (glowing) {
                    item.glow();
                }
                final MenuItem menuItem;
                switch (configurationItem) {
                    case "previous-page" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                        this.openPanelManageVisitationMenu(player);
                    });
                    default -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                    });
                }
                menu.getItems().put((slot - 1), menuItem);
            }
        });
        final ArrayList<ItemStack> bannedPlayerItems = new ArrayList<ItemStack>();
        mineData.getBanned().forEach(bannedUUID -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.unbreakable");
            final Item item;
            if (material == null) {
                item = new Item(new ItemStack(Material.PLAYER_HEAD));
                item.setHeadOwner(Bukkit.getOfflinePlayer(bannedUUID));
            } else {
                item = new Item(new ItemStack(material));
            }
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final ItemData itemData = new ItemData(this.plugin, item.getItemStack());
            itemData.setValue("uuid", bannedUUID.toString(), PersistentDataType.STRING);
            bannedPlayerItems.add(itemData.getItemStack());
        });
        final ArrayList<Integer> openSlots = new ArrayList<Integer>();
        int size;
        if (menu.getRows().isPresent()) {
            size = (menu.getRows().get() * 9);
        } else {
            size = (menu.getType().getDefaultSize() * 9);
        }
        for (int menuSlot = 0; menuSlot < size; menuSlot++) {
            if (!menu.getItems().containsKey(menuSlot)) {
                openSlots.add(menuSlot);
            }
        }
        final HashMap<Integer, ArrayList<ItemStack>> pageItems = new HashMap<Integer, ArrayList<ItemStack>>();
        int pageIndex = 0;
        for (int i = 0; i < bannedPlayerItems.size(); i += openSlots.size()) {
            pageItems.put(pageIndex, new ArrayList<ItemStack>(bannedPlayerItems.subList(i, Math.min(bannedPlayerItems.size(), i + openSlots.size()))));
            pageIndex++;
        }
        if (!pageItems.getOrDefault(0, new ArrayList<ItemStack>()).isEmpty()) {
            for (int slot = openSlots.get(0); slot < pageItems.get(0).size(); slot++) {
                final ItemStack slotItem = pageItems.get(0).get(slot);
                final ItemData itemData = new ItemData(this.plugin, slotItem);
                final UUID bannedUUID = UUID.fromString((String) itemData.getValue("uuid", PersistentDataType.STRING));
                final MenuItem menuItem = new MenuItem(slotItem, inventoryClickEvent -> {
                    inventoryClickEvent.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                    player.chat("/mine unban " + Bukkit.getOfflinePlayer(bannedUUID).getName());
                    this.updatePanelBansMenu(player, 0);
                });
                menu.getItems().put(slot, menuItem);
            }
        }
        if (!pageItems.getOrDefault(1, new ArrayList<ItemStack>()).isEmpty()) {
            final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.slot");
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                this.updatePanelBansMenu(player, 1);
            });
            menu.getItems().put((slot - 1), menuItem);
        }
        menu.open();
    }

    /**
     * Allows you to update the "panel" sub-command's bans menu.
     * This is used for multi-paging and stuff, it's pretty long for no reason, but it works efficiently.
     *
     * @param player ~ The player to open it for.
     * @param page ~ The page to update it for or to.
     */
    private void updatePanelBansMenu(final Player player, final int page) {
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
        final Optional<Menu> optionalMenu = MenuImplementor.get(this.plugin).getMenus().stream().filter(menu -> menu.getPlayer() == player && menu.getIdentifier().equals("mine-panel-bans")).findFirst();
        if (optionalMenu.isEmpty()) {
            return;
        }
        final Menu menu = optionalMenu.get();
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items").getKeys(false).stream().map(key -> Integer.parseInt(key)).collect(Collectors.toList()).forEach(slot -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
            });
            menu.getItems().put((slot - 1), menuItem);
        });
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.user-control.items").getKeys(false).forEach(configurationItem -> {
            if (!configurationItem.equals("next-page") && this.plugin.getComponentManager().getFileComponent().getConfiguration().isSet("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".slot")) {
                final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".slot");
                final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".material"));
                final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".name")));
                final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
                final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".glowing");
                final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".unbreakable");
                final Item item = new Item(new ItemStack(material));
                name.ifPresent(presentName -> item.setName(presentName));
                lore.ifPresent(presentLore -> item.setLore(presentLore));
                if (unbreakable) {
                    item.unbreakable();
                }
                if (glowing) {
                    item.glow();
                }
                final MenuItem menuItem;
                switch (configurationItem) {
                    case "previous-page" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                        if (page == 0) {
                            this.openPanelManageVisitationMenu(player);
                        } else {
                            this.updatePanelBansMenu(player, (page - 1));
                        }
                    });
                    default -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.manage-visitation.sounds.click.pitch"));
                    });
                }
                menu.getItems().put((slot - 1), menuItem);
            }
        });
        final ArrayList<ItemStack> bannedPlayerItems = new ArrayList<ItemStack>();
        mineData.getBanned().forEach(bannedUUID -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.banned-player.unbreakable");
            final Item item;
            if (material == null) {
                item = new Item(new ItemStack(Material.PLAYER_HEAD));
                item.setHeadOwner(Bukkit.getOfflinePlayer(bannedUUID));
            } else {
                item = new Item(new ItemStack(material));
            }
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final ItemData itemData = new ItemData(this.plugin, item.getItemStack());
            itemData.setValue("uuid", bannedUUID.toString(), PersistentDataType.STRING);
            bannedPlayerItems.add(itemData.getItemStack());
        });
        final ArrayList<Integer> openSlots = new ArrayList<Integer>();
        int size;
        if (menu.getRows().isPresent()) {
            size = (menu.getRows().get() * 9);
        } else {
            size = (menu.getType().getDefaultSize() * 9);
        }
        for (int menuSlot = 0; menuSlot < size; menuSlot++) {
            if (!menu.getItems().containsKey(menuSlot)) {
                openSlots.add(menuSlot);
            }
        }
        final HashMap<Integer, ArrayList<ItemStack>> pageItems = new HashMap<Integer, ArrayList<ItemStack>>();
        int pageIndex = 0;
        for (int i = 0; i < bannedPlayerItems.size(); i += openSlots.size()) {
            pageItems.put(pageIndex, new ArrayList<ItemStack>(bannedPlayerItems.subList(i, Math.min(bannedPlayerItems.size(), i + openSlots.size()))));
            pageIndex++;
        }
        if (!pageItems.getOrDefault(page, new ArrayList<ItemStack>()).isEmpty()) {
            for (int slot = openSlots.get(0); slot < pageItems.get(page).size(); slot++) {
                final ItemStack slotItem = pageItems.get(page).get(slot);
                final ItemData itemData = new ItemData(this.plugin, slotItem);
                final UUID bannedUUID = UUID.fromString((String) itemData.getValue("uuid", PersistentDataType.STRING));
                final MenuItem menuItem = new MenuItem(slotItem, inventoryClickEvent -> {
                    inventoryClickEvent.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                    player.chat("/mine unban " + Bukkit.getOfflinePlayer(bannedUUID).getName());
                    this.updatePanelBansMenu(player, page);
                });
                menu.getItems().put(slot, menuItem);
            }
        }
        if (!pageItems.getOrDefault(page + 1, new ArrayList<ItemStack>()).isEmpty()) {
            final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.slot");
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                this.updatePanelBansMenu(player, (page + 1));
            });
            menu.getItems().put((slot - 1), menuItem);
        }
        menu.update();
    }

    /**
     * Allows you to open the "panel" sub-command's whitelisted menu.
     *
     * @param player ~ The player to open it for.
     */
    private void openPanelWhitelistedMenu(final Player player) {
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
        final InventoryType inventoryType = InventoryType.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.type"));
        final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.title").replace("%type%", "Whitelisted"));
        Menu menu;
        if (inventoryType == InventoryType.CHEST) {
            final int rows = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.rows");
            menu = new Menu(this.plugin, player, rows, title);
        } else {
            menu = new Menu(this.plugin, player, inventoryType, title);
        }
        menu.setIdentifier("mine-panel-whitelisted");
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items").getKeys(false).stream().map(key -> Integer.parseInt(key)).collect(Collectors.toList()).forEach(slot -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.inventory.items." + slot + ".unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
            });
            menu.getItems().put((slot - 1), menuItem);
        });
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.user-control.items").getKeys(false).forEach(configurationItem -> {
            if (!configurationItem.equals("next-page") && this.plugin.getComponentManager().getFileComponent().getConfiguration().isSet("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".slot")) {
                final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".slot");
                final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".material"));
                final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".name")));
                final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
                final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".glowing");
                final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".unbreakable");
                final Item item = new Item(new ItemStack(material));
                name.ifPresent(presentName -> item.setName(presentName));
                lore.ifPresent(presentLore -> item.setLore(presentLore));
                if (unbreakable) {
                    item.unbreakable();
                }
                if (glowing) {
                    item.glow();
                }
                final MenuItem menuItem;
                switch (configurationItem) {
                    case "previous-page" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                        this.openPanelManageVisitationMenu(player);
                    });
                    default -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                    });
                }
                menu.getItems().put((slot - 1), menuItem);
            }
        });
        final ArrayList<ItemStack> whitelistedPlayerItems = new ArrayList<ItemStack>();
        mineData.getWhitelisted().forEach(whitelistedUUID -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.unbreakable");
            final Item item;
            if (material == null) {
                item = new Item(new ItemStack(Material.PLAYER_HEAD));
                item.setHeadOwner(Bukkit.getOfflinePlayer(whitelistedUUID));
            } else {
                item = new Item(new ItemStack(material));
            }
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final ItemData itemData = new ItemData(this.plugin, item.getItemStack());
            itemData.setValue("uuid", whitelistedUUID.toString(), PersistentDataType.STRING);
            whitelistedPlayerItems.add(itemData.getItemStack());
        });
        final ArrayList<Integer> openSlots = new ArrayList<Integer>();
        int size;
        if (menu.getRows().isPresent()) {
            size = (menu.getRows().get() * 9);
        } else {
            size = (menu.getType().getDefaultSize() * 9);
        }
        for (int menuSlot = 0; menuSlot < size; menuSlot++) {
            if (!menu.getItems().containsKey(menuSlot)) {
                openSlots.add(menuSlot);
            }
        }
        final HashMap<Integer, ArrayList<ItemStack>> pageItems = new HashMap<Integer, ArrayList<ItemStack>>();
        int pageIndex = 0;
        for (int i = 0; i < whitelistedPlayerItems.size(); i += openSlots.size()) {
            pageItems.put(pageIndex, new ArrayList<ItemStack>(whitelistedPlayerItems.subList(Math.min(whitelistedPlayerItems.size(), i), Math.min(whitelistedPlayerItems.size(), i + openSlots.size()))));
            pageIndex++;
        }
        if (!pageItems.getOrDefault(0, new ArrayList<ItemStack>()).isEmpty()) {
            for (int slot = openSlots.get(0); slot < pageItems.get(0).size(); slot++) {
                final ItemStack slotItem = pageItems.get(0).get(slot);
                final ItemData itemData = new ItemData(this.plugin, slotItem);
                final UUID whitelistedUUID = UUID.fromString((String) itemData.getValue("uuid", PersistentDataType.STRING));
                final MenuItem menuItem = new MenuItem(slotItem, inventoryClickEvent -> {
                    inventoryClickEvent.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                    player.chat("/mine unwhitelist " + Bukkit.getOfflinePlayer(whitelistedUUID).getName());
                    this.updatePanelWhitelistedMenu(player, 0);
                });
                menu.getItems().put(slot, menuItem);
            }
        }
        if (!pageItems.getOrDefault(1, new ArrayList<ItemStack>()).isEmpty()) {
            final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.slot");
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem  = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                this.updatePanelWhitelistedMenu(player, 1);
            });
            menu.getItems().put((slot - 1), menuItem);
        }
        menu.open();
    }

    /**
     * Allows you to update the "panel" sub-command's whitelisted menu.
     * This is used for multi-paging and stuff, it's pretty long for no reason, but it works efficiently.
     *
     * @param player ~ The player to open it for.
     * @param page ~ The page to update it for or to.
     */
    private void updatePanelWhitelistedMenu(final Player player, final int page) {
        final PlayerData playerData = PlayerData.getViaCache(player.getUniqueId()).get();
        final MineData mineData = MineData.getViaCache(playerData.getMine().get()).get();
        final Optional<Menu> optionalMenu = MenuImplementor.get(this.plugin).getMenus().stream().filter(menu -> menu.getPlayer() == player && menu.getIdentifier().equals("mine-panel-whitelisted")).findFirst();
        if (optionalMenu.isEmpty()) {
            return;
        }
        final Menu menu = optionalMenu.get();
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.panel.menus.user-control.items").getKeys(false).forEach(configurationItem -> {
            if (!configurationItem.equals("next-page") && this.plugin.getComponentManager().getFileComponent().getConfiguration().isSet("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".slot")) {
                final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".slot");
                final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".material"));
                final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".name")));
                final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
                final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".glowing");
                final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items." + configurationItem + ".unbreakable");
                final Item item = new Item(new ItemStack(material));
                name.ifPresent(presentName -> item.setName(presentName));
                lore.ifPresent(presentLore -> item.setLore(presentLore));
                if (unbreakable) {
                    item.unbreakable();
                }
                if (glowing) {
                    item.glow();
                }
                final MenuItem menuItem;
                switch (configurationItem) {
                    case "previous-page" -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                        if (page == 0) {
                            this.openPanelManageVisitationMenu(player);
                        } else {
                            this.updatePanelWhitelistedMenu(player, (page - 1));
                        }
                    });
                    default -> menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                        inventoryClickEvent.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                    });
                }
                menu.getItems().put((slot - 1), menuItem);
            }
        });
        final ArrayList<ItemStack> whitelistedPlayerItems = new ArrayList<ItemStack>();
        mineData.getWhitelisted().forEach(whitelistedUUID -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.whitelisted-player.unbreakable");
            final Item item;
            if (material == null) {
                item = new Item(new ItemStack(Material.PLAYER_HEAD));
                item.setHeadOwner(Bukkit.getOfflinePlayer(whitelistedUUID));
            } else {
                item = new Item(new ItemStack(material));
            }
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final ItemData itemData = new ItemData(this.plugin, item.getItemStack());
            itemData.setValue("uuid", whitelistedUUID.toString(), PersistentDataType.STRING);
            whitelistedPlayerItems.add(itemData.getItemStack());
        });
        final ArrayList<Integer> openSlots = new ArrayList<Integer>();
        int size;
        if (menu.getRows().isPresent()) {
            size = (menu.getRows().get() * 9);
        } else {
            size = (menu.getType().getDefaultSize() * 9);
        }
        for (int menuSlot = 0; menuSlot < size; menuSlot++) {
            if (!menu.getItems().containsKey(menuSlot)) {
                openSlots.add(menuSlot);
            }
        }
        final HashMap<Integer, ArrayList<ItemStack>> pageItems = new HashMap<Integer, ArrayList<ItemStack>>();
        int pageIndex = 0;
        for (int i = 0; i < whitelistedPlayerItems.size(); i += openSlots.size()) {
            pageItems.put(pageIndex, new ArrayList<ItemStack>(whitelistedPlayerItems.subList(Math.min(whitelistedPlayerItems.size(), i), Math.min(whitelistedPlayerItems.size(), i + openSlots.size()))));
            pageIndex++;
        }
        if (!pageItems.getOrDefault(page, new ArrayList<ItemStack>()).isEmpty()) {
            for (int slot = openSlots.get(0); slot < pageItems.get(page).size(); slot++) {
                final ItemStack slotItem = pageItems.get(page).get(slot);
                final ItemData itemData = new ItemData(this.plugin, slotItem);
                final UUID whitelistedUUID = UUID.fromString((String) itemData.getValue("uuid", PersistentDataType.STRING));
                final MenuItem menuItem = new MenuItem(slotItem, inventoryClickEvent -> {
                    inventoryClickEvent.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                    player.chat("/mine unwhitelist " + Bukkit.getOfflinePlayer(whitelistedUUID).getName());
                    this.updatePanelWhitelistedMenu(player, 0);
                });
                menu.getItems().put(slot, menuItem);
            }
        }
        if (!pageItems.getOrDefault(page + 1, new ArrayList<ItemStack>()).isEmpty()) {
            final int slot = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.slot");
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.panel.menus.user-control.items.next-page.unbreakable");
            final Item item = new Item(new ItemStack(material));
            name.ifPresent(presentName -> item.setName(presentName));
            lore.ifPresent(presentLore -> item.setLore(presentLore));
            if (unbreakable) {
                item.unbreakable();
            }
            if (glowing) {
                item.glow();
            }
            final MenuItem menuItem = new MenuItem(item.getItemStack(), inventoryClickEvent -> {
                inventoryClickEvent.setCancelled(true);
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.panel.menus.user-control.sounds.click.pitch"));
                this.updatePanelWhitelistedMenu(player, (page + 1));
            });
            menu.getItems().put((slot - 1), menuItem);
        }
        menu.update();
    }

}
