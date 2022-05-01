package net.evilkingdom.prison.component.components.mine;

/*
 * Made with love by https://kodirati.com/.
 */

import net.evilkingdom.commons.constructor.objects.ConstructorRegion;
import net.evilkingdom.commons.constructor.objects.ConstructorSchematic;
import net.evilkingdom.commons.utilities.grid.GridUtilities;
import net.evilkingdom.commons.utilities.string.StringUtilities;
import net.evilkingdom.prison.component.components.data.objects.MineData;
import net.evilkingdom.prison.component.components.data.objects.SelfData;
import net.evilkingdom.prison.component.components.mine.commands.MineCommand;
import net.evilkingdom.prison.component.components.mine.implementations.VoidGenerator;
import net.evilkingdom.prison.component.components.mine.objects.MineLocation;
import net.evilkingdom.prison.component.components.rank.objects.Rank;
import net.evilkingdom.prison.Prison;
import net.evilkingdom.prison.component.components.data.objects.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MineComponent {

    private final Prison plugin;

    private World world;
    private BukkitTask task;
    private ArrayList<UUID> playersWaitingForMineCreation, minesDoingTask;

    /**
     * Allows you to create the component.
     */
    public MineComponent() {
        this.plugin = Prison.getPlugin();
    }

    /**
     * Allows you to initialize the component.
     */
    public void initialize() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aInitializing..."));
        this.initializeWorld();
        this.minesDoingTask = new ArrayList<UUID>();
        this.playersWaitingForMineCreation = new ArrayList<UUID>();
        this.initializeTask();
        this.registerCommands();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aInitialized."));
    }

    /**
     * Allows you to terminate the component.
     */
    public void terminate() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Mine] &cTerminating..."));
        this.terminateTask();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Mine] &cTerminated."));
    }

    /**
     * Allows you to register the commands.
     */
    private void registerCommands() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aRegistering commands..."));
        new MineCommand().register();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aRegistered commands."));
    }

    /**
     * Allows you to initialize the world.
     */
    public void initializeWorld() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aInitializing world..."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aChecking if world exists..."));
        final String worldName = this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.world");
        if (new File(worldName).exists()) {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aWorld exists, loading it now..."));
        } else {
            Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aWorld does not exist, creating and loading it now..."));
        }
        final WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.generator(new VoidGenerator());
        final World world = Bukkit.createWorld(worldCreator);
        this.world = world;
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        }, 20L);
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aWorld loaded."));
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aInitialized world."));
    }

    /**
     * Allows you to initialize the task.
     */
    public void initializeTask() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aInitializing task..."));
        this.task = Bukkit.getServer().getScheduler().runTaskTimer(this.plugin, () -> MineData.getCache().forEach(mineData -> this.getMinePercentage(mineData.getUUID()).whenComplete((minePercentage, minePercentageThrowable) -> {
            if (minePercentage <= this.plugin.getComponentManager().getFileComponent().getConfiguration().getInt("components.mine.reset-percentages.automatic")) {
                this.resetMine(mineData.getUUID());
            }
        })), 0L, 100L);
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&2[Prison » Component » Components » Mine] &aInitialized task."));
    }

    /**
     * Allows you to terminate the task.
     */
    public void terminateTask() {
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Mine] &cTerminating task..."));
        this.task.cancel();
        Bukkit.getConsoleSender().sendMessage(StringUtilities.colorize("&4[Prison » Component » Components » Mine] &cTerminated tasks."));
    }

    /**
     * Allows you to retrieve the mine uuid's that are doing a task.
     * This is used to prevent double-actions which may cause game-breaking issues.
     *
     * @return The mine uuid's that are doing a task.
     */
    public ArrayList<UUID> getMinesDoingTask() {
        return this.minesDoingTask;
    }

    /**
     * Allows you to retrieve the player uuid's that are doing a mine creation.
     * This is used to prevent double-actions which may cause game-breaking issues.
     *
     * @return The player uuid's that are doing a mine creation.
     */
    public ArrayList<UUID> getPlayersWaitingForMineCreation() {
        return this.playersWaitingForMineCreation;
    }

    /**
     * Allows you to retrieve the world.
     *
     * @return The world.
     */
    public World getWorld() {
        return this.world;
    }

    /**
     * Allows you to reset a mine.
     *
     * @param uuid ~ The UUID of the mine.
     * @return If the mine reset task was successful.
     */
    public CompletableFuture<Boolean> resetMine(final UUID uuid) {
        if (this.minesDoingTask.contains(uuid)) {
            return CompletableFuture.supplyAsync(() -> false);
        }
        return MineData.get(uuid).thenCompose(mineData -> mineData.exists().thenCompose(mineExists -> {
           if (!mineExists) {
               return CompletableFuture.supplyAsync(() -> false);
           }
           this.minesDoingTask.add(uuid);
           return PlayerData.get(mineData.getOwner()).thenCompose(playerData -> SelfData.get().thenCompose(selfData -> {
               final Rank rank = selfData.getRanks().stream().filter(dataRank -> dataRank.getRank().equals(playerData.getRank())).collect(Collectors.toList()).get(0);
               final ConstructorRegion constructorRegion = new ConstructorRegion(this.plugin, mineData.getMineCornerOne(), mineData.getMineCornerTwo());
               return constructorRegion.fill(rank.getBlockPallet()).thenApply(fillSuccessful -> {
                   this.minesDoingTask.remove(uuid);
                   return fillSuccessful;
               });
           }));
        }));
    }

    /**
     * Allows you to get a mine's percentage.
     *
     * @param uuid ~ The mine's UUID.
     * @return The mine's percentage (out of 100).
     */
    public CompletableFuture<Double> getMinePercentage(final UUID uuid) {
        return MineData.get(uuid).thenCompose(mineData -> mineData.exists().thenCompose(mineExists -> {
            if (!mineExists) {
                return CompletableFuture.supplyAsync(() -> 100.0);
            }
            final ConstructorRegion constructorRegion = new ConstructorRegion(this.plugin, mineData.getMineCornerOne(), mineData.getMineCornerTwo());
            return constructorRegion.getBlockComposition().thenApply(blockComposition -> {
                double air = blockComposition.getOrDefault(Material.AIR, 0);
                double total = blockComposition.values().stream().mapToInt(integer -> integer).sum();
                return ((total - air) / total) * 100;
            });
        }));
    }

    /**
     * Allows you to create a mine.
     * It uses the SelfData's mine locations and gets the first unused one- after it is used, it'll set it to used and generate another one for the next mine creation.
     *
     * @param player ~ The mine's owner.
     * @param theme ~ The mine's theme.
     * @return The UUID of the mine when it reaches the completion state (or empty if it doesn't).
     */
    public CompletableFuture<Optional<UUID>> createMine(final Player player, final String theme) {
        final UUID uuid = UUID.randomUUID();
        this.minesDoingTask.add(uuid);
        this.playersWaitingForMineCreation.add(player.getUniqueId());
        return SelfData.get().thenApply(selfData -> {
            final ArrayList<MineLocation> previousMineLocations = selfData.getMineLocations();
            final MineLocation mineLocation = selfData.getMineLocations().stream().filter(dataMineLocation -> !dataMineLocation.isUsed()).findFirst().get();
            previousMineLocations.remove(mineLocation);
            final MineLocation replacementMineLocation = new MineLocation(mineLocation.getX(), mineLocation.getZ(), true);
            previousMineLocations.add(replacementMineLocation);
            this.plugin.getComponentManager().getMineComponent().generateMineLocations(replacementMineLocation.getX(), replacementMineLocation.getZ(), 1).whenComplete((generatedMineLocations, generatedMineLocationsThrowable) -> {
                previousMineLocations.addAll(generatedMineLocations);
                selfData.setMineLocations(previousMineLocations);
            });
            return mineLocation;
        }).thenCompose(mineLocation -> {
            if (mineLocation == null) {
                return CompletableFuture.supplyAsync(() -> Optional.empty());
            }
            final Location center = new Location(this.world, mineLocation.getX(), 175, mineLocation.getZ());
            final ConstructorSchematic constructorSchematic = new ConstructorSchematic(this.plugin, center);
            final File schematicFile = new File(this.plugin.getDataFolder() + File.separator + "schematics", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".schematic"));
            return constructorSchematic.load(schematicFile).thenCompose(loadSuccessful -> {
                if (!loadSuccessful) {
                    return CompletableFuture.supplyAsync(() -> Optional.empty());
                }
                return constructorSchematic.paste().thenApply(pasteSuccessful -> {
                    if (!pasteSuccessful) {
                        return Optional.empty();
                    }
                    final MineData mineData = new MineData(uuid);
                    mineData.setCenter(center);
                    mineData.setOwner(player.getUniqueId());
                    mineData.setTheme(theme);
                    mineData.cache();
                    this.minesDoingTask.remove(uuid);
                    PlayerData.get(player.getUniqueId()).whenComplete((playerData, playerDataThrowable) -> {
                        this.playersWaitingForMineCreation.remove(player.getUniqueId());
                        playerData.setMine(Optional.of(mineData.getUUID()));
                        if (!playerData.isCached()) {
                            playerData.save(true);
                            mineData.save(true);
                            mineData.uncache();
                        } else {
                            this.resetMine(uuid);
                        }
                    });
                    return Optional.of(uuid);
                });
            });
        });
    }

    /**
     * Allows you to change a mine's theme.
     *
     * @param uuid ~ The mine's UUID.
     * @param theme ~ The mine's theme to set.
     * @return The mine's theme change completion state.
     */
    public CompletableFuture<Boolean> changeMineTheme(final UUID uuid, final String theme) {
        return MineData.get(uuid).thenCompose(mineData -> mineData.exists().thenCompose(mineExists -> {
            if (!mineExists) {
                return CompletableFuture.supplyAsync(() -> false);
            }
            if (this.minesDoingTask.contains(uuid)) {
                return CompletableFuture.supplyAsync(() -> false);
            }
            this.minesDoingTask.add(mineData.getUUID());
            final ConstructorRegion constructorRegion = new ConstructorRegion(this.plugin, mineData.getCornerOne(), mineData.getCornerTwo());
            return constructorRegion.fill(Material.AIR).thenCompose(fillSuccessful -> {
                if (!fillSuccessful) {
                    return CompletableFuture.supplyAsync(() -> false);
                }
                final ConstructorSchematic constructorSchematic = new ConstructorSchematic(this.plugin, mineData.getCenter());
                final File schematicFile = new File(this.plugin.getDataFolder() + File.separator + "schematics", this.plugin.getComponentManager().getFileComponent().getConfiguration().getString("components.mine.themes." + theme + ".schematic"));
                return constructorSchematic.load(schematicFile).thenCompose(loadSuccessful -> {
                    if (!loadSuccessful) {
                        return CompletableFuture.supplyAsync(() -> false);
                    }
                    return constructorSchematic.paste().thenApply(pasteSuccessful -> {
                        if (!pasteSuccessful) {
                            return false;
                        }
                        mineData.setTheme(theme);
                        this.minesDoingTask.remove(mineData.getUUID());
                        this.resetMine(mineData.getUUID());
                        return true;
                    });
                });
            });
        }));
    }

    /**
     * Allows you to generate a certain amount of mine locations.
     *
     * @param x ~ The x coordinate to start with.
     * @param z ~ The z coordinate to start with.
     * @param amount ~ The amount of mine locations to generate.
     */
    public CompletableFuture<ArrayList<MineLocation>> generateMineLocations(final int x, final int z, final long amount) {
        return CompletableFuture.supplyAsync(() -> {
            final ArrayList<MineLocation> mineLocations = new ArrayList<MineLocation>();
            boolean valid = false;
            for (int i = 0; true; i++) {
                final int[] point = GridUtilities.getPoint(i);
                final int currentX;
                final int currentZ;
                if (i == 0) {
                    currentX = 0;
                    currentZ = 0;
                } else {
                    currentX = (point[0] * 550);
                    currentZ = (point[1] * 550);
                }
                if (valid) {
                    final MineLocation mineLocation = new MineLocation(currentX, currentZ, false);
                    mineLocations.add(mineLocation);
                }
                if ((currentX == x) && (currentZ == z)) {
                    valid = true;
                }
                if (mineLocations.size() == amount) {
                    break;
                }
            }
            return mineLocations;
        });
    }

}
