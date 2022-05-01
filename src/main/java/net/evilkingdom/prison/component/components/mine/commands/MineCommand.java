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
import net.evilkingdom.commons.menu.objects.Menu;
import net.evilkingdom.commons.menu.objects.MenuItem;
import net.evilkingdom.commons.utilities.luckperms.LuckPermsUtilities;
import net.evilkingdom.commons.utilities.mojang.MojangUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.commons.utilities.time.TimeUtilities;
import net.evilkingdom.prison.component.components.data.objects.MineData;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
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
     * Just uses the bukkit arguments since bukkit handles the magic.
     */
    @Override
    public boolean onExecution(final CommandSender commandSender, final String[] arguments) {
        if (arguments.length == 0) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.invalid-usage").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
            if (commandSender instanceof Player) {
                final Player player = (Player) commandSender;
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.pitch"));
            }
            return false;
        }
        final String subCommand = arguments[0].toLowerCase();
        if (!Arrays.asList("help", "create", "go", "reset", "ban").contains(subCommand)) {
            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.invalid-usage").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
            if (commandSender instanceof Player) {
                final Player player = (Player) commandSender;
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.pitch"));
            }
            return false;
        }
        switch (subCommand) {
            case "help" -> {
                if (!(commandSender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.invalid-executor").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
                    return false;
                }
                final Player player = (Player) commandSender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.help.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.error.pitch"));
                    return false;
                }
                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.help.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.help.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.help.sounds.success.pitch"));
            }
            case "go" -> {
                if (!(commandSender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-executor").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
                    return false;
                }
                final Player player = (Player) commandSender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                    return false;
                }
                PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
                    if (playerData.getMine().isEmpty()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.invalid-go").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.error.pitch"));
                        return;
                    }
                    MineData.get(playerData.getMine().get()).whenComplete((mineData, mineDataThrowable) -> {
                        Bukkit.getScheduler().runTask(this.plugin, () -> player.teleport(mineData.getGo()));
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.go.messages.success").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.go.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.go.sounds.success.pitch"));
                        final Border border = new Border(this.plugin, player, mineData.getCenter().toCenterLocation(), mineData.getBorderSize(), BorderColor.RED);
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> border.show(), 20L);
                    });
                });
            }
            case "create" -> {
                if (!(commandSender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.invalid-executor").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
                    return false;
                }
                final Player player = (Player) commandSender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.pitch"));
                    return false;
                }
                PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
                    if (playerData.getMine().isPresent()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.invalid-creation.already-has-a-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.pitch"));
                        return;
                    }
                    if (this.plugin.getComponentManager().getMineComponent().getPlayersWaitingForMineCreation().contains(player.getUniqueId())) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.invalid-creation.already-creating").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.error.pitch"));
                        return;
                    }
                    this.openCreateSelectThemeMenu(player);
                });
            }
            case "reset" -> {
                if (!(commandSender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-executor").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
                    return false;
                }
                final Player player = (Player) commandSender;
                if (arguments.length != 1) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                    return false;
                }
                PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
                    if (playerData.getMine().isEmpty()) {
                        this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-reset.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                        player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                        return;
                    }
                    MineData.get(playerData.getMine().get()).whenComplete((mineData, mineDataThrowable) -> {
                        final Optional<Cooldown> optionalCooldown = mineData.getCooldowns().stream().filter(cooldown -> cooldown.getIdentifier().equals("mine-" + mineData.getUUID() + "-reset")).findFirst();
                        if (optionalCooldown.isPresent()) {
                            final Cooldown cooldown = optionalCooldown.get();
                            final String formattedTimeLeft = TimeUtilities.format((cooldown.getTimeLeft() * 50));
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-reset.on-cooldown").forEach(string -> player.sendMessage(StringUtilities.colorize(string.replace("%time_left%", formattedTimeLeft))));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                            return;
                        }
                        this.plugin.getComponentManager().getMineComponent().getMinePercentage(mineData.getUUID()).whenComplete((minePercentage, minePercentageThrowable) -> {
                            if (minePercentage > this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.reset-percentages.manual")) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-reset.mine-too-full").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                                return;
                            }
                            if (this.plugin.getComponentManager().getMineComponent().getMinesDoingTask().contains(mineData.getUUID())) {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.invalid-reset.already-resetting").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.error.pitch"));
                                return;
                            }
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.success.resetting").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.success.pitch"));
                            this.plugin.getComponentManager().getMineComponent().resetMine(mineData.getUUID()).whenComplete((resetSuccessful, resetSuccessfulThrowable) -> {
                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.reset.messages.success.reset").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.reset.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.reset.sounds.success.pitch"));
                                LuckPermsUtilities.getPermissions(player.getUniqueId()).whenComplete((playerPermissions, playerPermissionsThrowable) -> {
                                    final ArrayList<Long> durations = new ArrayList<Long>(List.of(this.plugin.getComponentManager().getFileComponent().getConfiguration().getLong("components.mine.commands.mine.sub-commands.reset.default-cooldown")));
                                    playerPermissions.forEach(permission -> {
                                        if (permission.startsWith("prison.mine.commands.mine.sub-commands.reset.cooldown.")) {
                                            durations.add((TimeUtilities.get(permission.replaceFirst("prison.mine.commands.mine.sub-commands.reset.cooldown.", "")) / 50));
                                        }
                                    });
                                    Collections.sort(durations);
                                    final Cooldown cooldown = new Cooldown(this.plugin, "mine-" + mineData.getUUID() + "-reset", durations.get(0));
                                    cooldown.start();
                                });
                            });
                        });
                    });
                });
            }
            case "ban" -> {
                if (!(commandSender instanceof Player)) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-executor").forEach(string -> commandSender.sendMessage(StringUtilities.colorize(string)));
                    return false;
                }
                final Player player = (Player) commandSender;
                if (arguments.length != 2) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-usage").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                    return false;
                }
                if (arguments[1].equalsIgnoreCase(player.getName())) {
                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-player.self-ban").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                    return false;
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
                            PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
                                if (playerData.getMine().isEmpty()) {
                                    this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.invalid-ban.has-no-mine").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                                    player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.error.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.error.pitch"));
                                    return;
                                }
                                MineData.get(playerData.getMine().get()).whenComplete((mineData, mineDataThrowable) -> {
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
                                        final ConstructorRegion constructorRegion = new ConstructorRegion(this.plugin, mineData.getCornerOne(), mineData.getCornerTwo());
                                        if (constructorRegion.isWithin(target.getLocation())) {
                                            SelfData.get().whenComplete((selfData, selfDataThrowable) -> {
                                                Bukkit.getScheduler().runTask(this.plugin, () -> target.teleport(selfData.getSpawn()));
                                                this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.ban.messages.success.target").forEach(string -> target.sendMessage(StringUtilities.colorize(string)));
                                                target.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.ban.sounds.success.target.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.success.target.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.ban.sounds.success.target.pitch"));
                                            });
                                        }
                                    }
                                });
                            });
                        });
                    });
                });
            }
        }
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
            case 1 -> tabCompletion.addAll(Arrays.asList("help", "create", "reset", "go", "ban"));
            case 2 -> {
                final String subCommand = arguments[0];
                switch (subCommand) {
                    case "ban" -> {
                        final ArrayList<String> playerNames = new ArrayList<String>(Bukkit.getOnlinePlayers().stream().map(onlinePlayer -> onlinePlayer.getName()).collect(Collectors.toList()));
                        playerNames.remove(player.getName());
                        tabCompletion.addAll(playerNames);
                    }
                }
            }
        }
        return tabCompletion;
    }

    /**
     * Allows you to open the "create" sub-command's select theme menu.
     *
     * @param player ~ The player to open it for.
     */
    private void openCreateSelectThemeMenu(final Player player) {
        final InventoryType inventoryType = InventoryType.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.type"));
        final String title = StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.title"));
        Menu menu;
        if (inventoryType == InventoryType.CHEST) {
            final int rows = this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.rows");
            menu = new Menu(this.plugin, player, rows, title);
        } else {
            menu = new Menu(this.plugin, player, inventoryType, title);
        }
        this.plugin.getComponentManager().getFileComponent().getConfiguration().getConfigurationSection("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.items").getKeys(false).stream().map(key -> Integer.parseInt(key)).collect(Collectors.toList()).forEach(slot -> {
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.items." + slot + ".material"));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.items." + slot + ".name")));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.items." + slot + ".lore").stream().map(loreLine -> StringUtilities.colorize(loreLine)).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.items." + slot + ".glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.create.menus.theme-selector.inventory.items." + slot + ".unbreakable");
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
            final Material material = Material.getMaterial(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.theme-selector.items.theme.material").replace("%related_material%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".related-material")));
            final Optional<String> name = Optional.of(StringUtilities.colorize(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.menus.theme-selector.items.theme.name").replace("%name%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".prettified-name"))));
            final Optional<ArrayList<String>> lore = Optional.of(new ArrayList<String>(this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.menus.theme-selector.items.theme.lore").stream().map(loreLine -> StringUtilities.colorize(loreLine.replace("%name%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".prettified-name")).replace("%description%", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".description")))).collect(Collectors.toList())));
            final boolean glowing = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.create.menus.theme-selector.items.theme.glowing");
            final boolean unbreakable = this.plugin.getComponentManager().getFileComponent().getConfiguration().getBoolean("components.mine.commands.mine.sub-commands.create.menus.theme-selector.items.theme.unbreakable");
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
                this.plugin.getComponentManager().getMineComponent().createMine(player, theme).whenComplete((mineUUID, mineUUIDThrowable) -> {
                    if (mineUUID.isEmpty()) {
                        return;
                    }
                    MineData.get(mineUUID.get()).whenComplete((mineData, mineDataThrowable) -> {
                        if (player.isOnline()) {
                            Bukkit.getScheduler().runTask(this.plugin, () -> player.teleport(mineData.getGo()));
                            this.plugin.getComponentManager().getFileComponent().getConfiguration().getStringList("components.mine.commands.mine.sub-commands.create.messages.success.created").forEach(string -> player.sendMessage(StringUtilities.colorize(string)));
                            player.playSound(player.getLocation(), Sound.valueOf(this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.commands.mine.sub-commands.create.sounds.success.sound")), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.success.volume"), (float) this.plugin.getComponentManager().getFileComponent().getConfiguration().getDouble("components.mine.commands.mine.sub-commands.create.sounds.success.pitch"));
                            final Border border = new Border(this.plugin, player, mineData.getCenter().toCenterLocation(), mineData.getBorderSize(), BorderColor.RED);
                            Bukkit.getScheduler().runTaskLater(this.plugin, () -> border.show(), 20L);
                        }
                    });
                });
            });
            menu.getItems().put(slot, menuItem);
        });
        Bukkit.getScheduler().runTask(this.plugin, () -> menu.open());
    }

}
