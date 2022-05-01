package net.minecraft.server.level;

import com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import io.papermc.paper.adventure.PaperAdventure; // Paper
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorldBorder;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftPortalEvent;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftDimensionUtil;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.MainHand;
// CraftBukkit end

public class ServerPlayer extends Player {

    private static final Logger LOGGER = LogUtils.getLogger();
    public long lastSave = MinecraftServer.currentTick; // Paper
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
    public ServerGamePacketListenerImpl connection;
    public net.minecraft.network.Connection networkManager; // Paper
    public final MinecraftServer server;
    public final ServerPlayerGameMode gameMode;
    private final PlayerAdvancements advancements;
    private final ServerStatsCounter stats;
    private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
    private int lastRecordedFoodLevel = Integer.MIN_VALUE;
    private int lastRecordedAirLevel = Integer.MIN_VALUE;
    private int lastRecordedArmor = Integer.MIN_VALUE;
    private int lastRecordedLevel = Integer.MIN_VALUE;
    private int lastRecordedExperience = Integer.MIN_VALUE;
    public boolean isRealPlayer; // Paper - chunk priority
    private float lastSentHealth = -1.0E8F;
    private int lastSentFood = -99999999;
    private boolean lastFoodSaturationZero = true;
    public int lastSentExp = -99999999;
    public int spawnInvulnerableTime = 60;
    private ChatVisiblity chatVisibility;
    private boolean canChatColor;
    private long lastActionTime;
    @Nullable
    private Entity camera;
    public boolean isChangingDimension;
    private boolean seenCredits;
    private final ServerRecipeBook recipeBook;
    @Nullable
    private Vec3 levitationStartPos;
    private int levitationStartTime;
    private boolean disconnected;
    @Nullable
    private Vec3 startingToFallPosition;
    @Nullable
    private Vec3 enteredNetherPosition;
    @Nullable
    private Vec3 enteredLavaOnVehiclePosition;
    private SectionPos lastSectionPos;
    private ResourceKey<Level> respawnDimension;
    @Nullable
    private BlockPos respawnPosition;
    private boolean respawnForced;
    private float respawnAngle;
    private final TextFilter textFilter;
    private boolean textFilteringEnabled;
    private boolean allowsListing;
    private final ContainerSynchronizer containerSynchronizer;
    private final ContainerListener containerListener;
    private int containerCounter;
    public int latency;
    public boolean wonGame;
    private int containerUpdateDelay; // Paper
    public long loginTime; // Paper
    public int patrolSpawnDelay; // Paper - per player patrol spawns
    // Paper start - cancellable death event
    public boolean queueHealthUpdatePacket = false;
    public net.minecraft.network.protocol.game.ClientboundSetHealthPacket queuedHealthUpdatePacket;
    // Paper end
    // Paper start - mob spawning rework
    public static final int MOBCATEGORY_TOTAL_ENUMS = net.minecraft.world.entity.MobCategory.values().length;
    public final int[] mobCounts = new int[MOBCATEGORY_TOTAL_ENUMS]; // Paper
    public final com.destroystokyo.paper.util.PooledHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> cachedSingleMobDistanceMap;
    // Paper end

    // CraftBukkit start
    public String displayName;
    public net.kyori.adventure.text.Component adventure$displayName; // Paper
    public Component listName;
    public org.bukkit.Location compassTarget;
    public int newExp = 0;
    public int newLevel = 0;
    public int newTotalExp = 0;
    public boolean keepLevel = false;
    public double maxHealthCache;
    public boolean joining = true;
    public boolean sentListPacket = false;
    public boolean supressTrackerForLogin = false; // Paper
    public boolean didPlayerJoinEvent = false; // Paper
    public Integer clientViewDistance;
    // CraftBukkit end
    public PlayerNaturallySpawnCreaturesEvent playerNaturallySpawnedEvent; // Paper

    public double lastEntitySpawnRadiusSquared; // Paper - optimise isOutsideRange, this field is in blocks
    public final com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> cachedSingleHashSet; // Paper
    public org.bukkit.event.player.PlayerQuitEvent.QuitReason quitReason = null; // Paper - there are a lot of changes to do if we change all methods leading to the event

    public ServerPlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(world, world.getSharedSpawnPos(), world.getSharedSpawnAngle(), profile);
        this.chatVisibility = ChatVisiblity.FULL;
        this.canChatColor = true;
        this.lastActionTime = Util.getMillis();
        this.recipeBook = new ServerRecipeBook();
        this.lastSectionPos = SectionPos.of(0, 0, 0);
        this.respawnDimension = Level.OVERWORLD;
        this.allowsListing = true;
        this.containerSynchronizer = new ContainerSynchronizer() {
            @Override
            public void sendInitialData(AbstractContainerMenu handler, NonNullList<ItemStack> stacks, ItemStack cursorStack, int[] properties) {
                ServerPlayer.this.connection.send(new ClientboundContainerSetContentPacket(handler.containerId, handler.incrementStateId(), stacks, cursorStack));

                for (int i = 0; i < properties.length; ++i) {
                    this.broadcastDataValue(handler, i, properties[i]);
                }

            }

            @Override
            public void sendSlotChange(AbstractContainerMenu handler, int slot, ItemStack stack) {
                ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(handler.containerId, handler.incrementStateId(), slot, stack));
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu handler, ItemStack stack) {
                ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(-1, handler.incrementStateId(), -1, stack));
            }

            @Override
            public void sendDataChange(AbstractContainerMenu handler, int property, int value) {
                this.broadcastDataValue(handler, property, value);
            }

            private void broadcastDataValue(AbstractContainerMenu handler, int property, int value) {
                ServerPlayer.this.connection.send(new ClientboundContainerSetDataPacket(handler.containerId, property, value));
            }
        };
        this.containerListener = new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu handler, int slotId, ItemStack stack) {
                Slot slot = handler.getSlot(slotId);

                if (!(slot instanceof ResultSlot)) {
                    if (slot.container == ServerPlayer.this.getInventory()) {
                        CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), stack);
                    }

                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu handler, int property, int value) {}
        };
        this.textFilter = server.createTextFilterForPlayer(this);
        this.gameMode = server.createGameModeForPlayer(this);
        this.server = server;
        this.stats = server.getPlayerList().getPlayerStats(this);
        this.advancements = server.getPlayerList().getPlayerAdvancements(this);
        this.maxUpStep = 1.0F;
        //this.fudgeSpawnLocation(world); // Paper - don't move to spawn on login, only first join

        this.cachedSingleHashSet = new com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<>(this); // Paper

        // CraftBukkit start
        this.displayName = this.getScoreboardName();
        this.adventure$displayName = net.kyori.adventure.text.Component.text(this.getScoreboardName()); // Paper
        this.bukkitPickUpLoot = true;
        this.maxHealthCache = this.getMaxHealth();
        this.cachedSingleMobDistanceMap = new com.destroystokyo.paper.util.PooledHashSets.PooledObjectLinkedOpenHashSet<>(this); // Paper
    }
    // Paper start - Chunk priority
    public BlockPos getPointInFront(double inFront) {
        double rads = Math.toRadians(net.minecraft.server.MCUtil.normalizeYaw(this.yRot + 90)); // MC rotates yaw 90 for some odd reason
        final double x = getX() + inFront * Math.cos(rads);
        final double z = getZ() + inFront * Math.sin(rads);
        return new BlockPos(x, getY(), z);
    }

    public ChunkPos getChunkInFront(double inFront) {
        double rads = Math.toRadians(net.minecraft.server.MCUtil.normalizeYaw(this.yRot + 90)); // MC rotates yaw 90 for some odd reason
        final double x = getX() + (inFront * 16) * Math.cos(rads);
        final double z = getZ() + (inFront * 16) * Math.sin(rads);
        return new ChunkPos(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
    }
    // Paper end

    // Yes, this doesn't match Vanilla, but it's the best we can do for now.
    // If this is an issue, PRs are welcome
    public final BlockPos getSpawnPoint(ServerLevel worldserver) {
        BlockPos blockposition = worldserver.getSharedSpawnPos();

        if (worldserver.dimensionType().hasSkyLight() && worldserver.serverLevelData.getGameType() != GameType.ADVENTURE) {
            int i = Math.max(0, this.server.getSpawnRadius(worldserver));
            int j = Mth.floor(worldserver.getWorldBorder().getDistanceToBorder((double) blockposition.getX(), (double) blockposition.getZ()));

            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long k = (long) (i * 2 + 1);
            long l = k * k;
            int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int) l;
            int j1 = this.getCoprime(i1);
            int k1 = (new Random()).nextInt(i1);

            for (int l1 = 0; l1 < i1; ++l1) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                BlockPos blockposition1 = PlayerRespawnLogic.getOverworldRespawnPos(worldserver, blockposition.getX() + j2 - i, blockposition.getZ() + k2 - i);

                if (blockposition1 != null) {
                    return blockposition1;
                }
            }
        }

        return blockposition;
    }
    // CraftBukkit end

    public void fudgeSpawnLocation(ServerLevel world) {
        BlockPos blockposition = world.getSharedSpawnPos();

        if (world.dimensionType().hasSkyLight() && world.serverLevelData.getGameType() != GameType.ADVENTURE) { // CraftBukkit
            int i = Math.max(0, this.server.getSpawnRadius(world));
            int j = Mth.floor(world.getWorldBorder().getDistanceToBorder((double) blockposition.getX(), (double) blockposition.getZ()));

            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long k = (long) (i * 2 + 1);
            long l = k * k;
            int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int) l;
            int j1 = this.getCoprime(i1);
            int k1 = (new Random()).nextInt(i1);

            for (int l1 = 0; l1 < i1; ++l1) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                BlockPos blockposition1 = PlayerRespawnLogic.getOverworldRespawnPos(world, blockposition.getX() + j2 - i, blockposition.getZ() + k2 - i);

                if (blockposition1 != null) {
                    this.moveTo(blockposition1, 0.0F, 0.0F);
                    if (world.noCollision(this, this.getBoundingBox(), true)) { // Paper - make sure this loads chunks, we default to NOT loading now
                        break;
                    }
                }
            }
        } else {
            this.moveTo(blockposition, 0.0F, 0.0F);

            while (!world.noCollision(this, this.getBoundingBox(), true) && this.getY() < (double) (world.getMaxBuildHeight() - 1)) { // Paper - make sure this loads chunks, we default to NOT loading now
                this.setPos(this.getX(), this.getY() + 1.0D, this.getZ());
            }
        }

    }

    private int getCoprime(int horizontalSpawnArea) {
        return horizontalSpawnArea <= 16 ? horizontalSpawnArea - 1 : 17;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("enteredNetherPosition", 10)) {
            CompoundTag nbttagcompound1 = nbt.getCompound("enteredNetherPosition");

            this.enteredNetherPosition = new Vec3(nbttagcompound1.getDouble("x"), nbttagcompound1.getDouble("y"), nbttagcompound1.getDouble("z"));
        }

        this.seenCredits = nbt.getBoolean("seenCredits");
        if (nbt.contains("recipeBook", 10)) {
            this.recipeBook.fromNbt(nbt.getCompound("recipeBook"), this.server.getRecipeManager());
        }
        this.getBukkitEntity().readExtraData(nbt); // CraftBukkit

        if (this.isSleeping()) {
            this.stopSleeping();
        }

        // CraftBukkit start
        String spawnWorld = nbt.getString("SpawnWorld");
        CraftWorld oldWorld = (CraftWorld) Bukkit.getWorld(spawnWorld);
        if (oldWorld != null) {
            this.respawnDimension = oldWorld.getHandle().dimension();
        }
        // CraftBukkit end

        if (nbt.contains("SpawnX", 99) && nbt.contains("SpawnY", 99) && nbt.contains("SpawnZ", 99)) {
            this.respawnPosition = new BlockPos(nbt.getInt("SpawnX"), nbt.getInt("SpawnY"), nbt.getInt("SpawnZ"));
            this.respawnForced = nbt.getBoolean("SpawnForced");
            this.respawnAngle = nbt.getFloat("SpawnAngle");
            if (nbt.contains("SpawnDimension")) {
                DataResult<ResourceKey<Level>> dataresult = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, nbt.get("SpawnDimension")); // CraftBukkit - decompile error
                Logger logger = ServerPlayer.LOGGER;

                Objects.requireNonNull(logger);
                this.respawnDimension = (ResourceKey) dataresult.resultOrPartial(logger::error).orElse(Level.OVERWORLD);
            }
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        this.storeGameTypes(nbt);
        nbt.putBoolean("seenCredits", this.seenCredits);
        if (this.enteredNetherPosition != null) {
            CompoundTag nbttagcompound1 = new CompoundTag();

            nbttagcompound1.putDouble("x", this.enteredNetherPosition.x);
            nbttagcompound1.putDouble("y", this.enteredNetherPosition.y);
            nbttagcompound1.putDouble("z", this.enteredNetherPosition.z);
            nbt.put("enteredNetherPosition", nbttagcompound1);
        }

        Entity entity = this.getRootVehicle();
        Entity entity1 = this.getVehicle();

        // CraftBukkit start - handle non-persistent vehicles
        boolean persistVehicle = true;
        if (entity1 != null) {
            Entity vehicle;
            for (vehicle = entity1; vehicle != null; vehicle = vehicle.getVehicle()) {
                if (!vehicle.persist) {
                    persistVehicle = false;
                    break;
                }
            }
        }

        if (persistVehicle && entity1 != null && entity != this && entity.hasExactlyOnePlayerPassenger() && !entity.isRemoved()) { // Paper
            // CraftBukkit end
            CompoundTag nbttagcompound2 = new CompoundTag();
            CompoundTag nbttagcompound3 = new CompoundTag();

            entity.save(nbttagcompound3);
            nbttagcompound2.putUUID("Attach", entity1.getUUID());
            nbttagcompound2.put("Entity", nbttagcompound3);
            nbt.put("RootVehicle", nbttagcompound2);
        }

        nbt.put("recipeBook", this.recipeBook.toNbt());
        nbt.putString("Dimension", this.level.dimension().location().toString());
        if (this.respawnPosition != null) {
            nbt.putInt("SpawnX", this.respawnPosition.getX());
            nbt.putInt("SpawnY", this.respawnPosition.getY());
            nbt.putInt("SpawnZ", this.respawnPosition.getZ());
            nbt.putBoolean("SpawnForced", this.respawnForced);
            nbt.putFloat("SpawnAngle", this.respawnAngle);
            DataResult<Tag> dataresult = ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, this.respawnDimension.location()); // CraftBukkit - decompile error
            Logger logger = ServerPlayer.LOGGER;

            Objects.requireNonNull(logger);
            dataresult.resultOrPartial(logger::error).ifPresent((nbtbase) -> {
                nbt.put("SpawnDimension", nbtbase);
            });
        }
        this.getBukkitEntity().setExtraData(nbt); // CraftBukkit

    }

    // CraftBukkit start - World fallback code, either respawn location or global spawn
    public void spawnIn(Level world) {
        this.level = world;
        if (world == null) {
            this.unsetRemoved();
            Vec3 position = null;
            if (this.respawnDimension != null) {
                world = this.getLevel().getCraftServer().getHandle().getServer().getLevel(this.respawnDimension);
                if (world != null && this.getRespawnPosition() != null) {
                    position = Player.findRespawnPositionAndUseSpawnBlock((ServerLevel) world, this.getRespawnPosition(), this.getRespawnAngle(), false, false).orElse(null);
                }
            }
            if (world == null || position == null) {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = Vec3.atCenterOf(((ServerLevel) world).getSharedSpawnPos());
            }
            this.level = world;
            this.setPosRaw(position.x(), position.y(), position.z()); // Paper - don't register to chunks yet
        }
        this.gameMode.setLevel((ServerLevel) world);
    }
    // CraftBukkit end

    public void setExperiencePoints(int points) {
        float f = (float) this.getXpNeededForNextLevel();
        float f1 = (f - 1.0F) / f;

        this.experienceProgress = Mth.clamp((float) points / f, 0.0F, f1);
        this.lastSentExp = -1;
    }

    public void setExperienceLevels(int level) {
        this.experienceLevel = level;
        this.lastSentExp = -1;
    }

    @Override
    public void giveExperienceLevels(int levels) {
        super.giveExperienceLevels(levels);
        this.lastSentExp = -1;
    }

    @Override
    public void onEnchantmentPerformed(ItemStack enchantedItem, int experienceLevels) {
        super.onEnchantmentPerformed(enchantedItem, experienceLevels);
        this.lastSentExp = -1;
    }

    public void initMenu(AbstractContainerMenu screenHandler) {
        screenHandler.addSlotListener(this.containerListener);
        screenHandler.setSynchronizer(this.containerSynchronizer);
    }

    public void initInventoryMenu() {
        this.initMenu(this.inventoryMenu);
    }

    @Override
    public void onEnterCombat() {
        super.onEnterCombat();
        this.connection.send(new ClientboundPlayerCombatEnterPacket());
    }

    @Override
    public void onLeaveCombat() {
        super.onLeaveCombat();
        this.connection.send(new ClientboundPlayerCombatEndPacket(this.getCombatTracker()));
    }

    @Override
    protected void onInsideBlock(BlockState state) {
        CriteriaTriggers.ENTER_BLOCK.trigger(this, state);
    }

    @Override
    protected ItemCooldowns createItemCooldowns() {
        return new ServerItemCooldowns(this);
    }

    @Override
    public void tick() {
        // CraftBukkit start
        if (this.joining) {
            this.joining = false;
        }
        // CraftBukkit end
        this.gameMode.tick();
        --this.spawnInvulnerableTime;
        if (this.invulnerableTime > 0) {
            --this.invulnerableTime;
        }

        // Paper start - Configurable container update tick rate
        if (--containerUpdateDelay <= 0) {
            this.containerMenu.broadcastChanges();
            containerUpdateDelay = level.paperConfig.containerUpdateTickRate;
        }
        // Paper end
        if (!this.level.isClientSide && this.containerMenu != this.inventoryMenu && (isImmobile() || !this.containerMenu.stillValid(this))) { // Paper - auto close while frozen
            this.closeContainer(org.bukkit.event.inventory.InventoryCloseEvent.Reason.CANT_USE); // Paper
            this.containerMenu = this.inventoryMenu;
        }

        Entity entity = this.getCamera();

        if (entity != this) {
            if (entity.isAlive()) {
                this.absMoveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                this.getLevel().getChunkSource().move(this);
                if (this.wantsToStopRiding()) {
                    this.setCamera(this);
                }
            } else {
                this.setCamera(this);
            }
        }

        CriteriaTriggers.TICK.trigger(this);
        if (this.levitationStartPos != null) {
            CriteriaTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
        }

        this.trackStartFallingPosition();
        this.trackEnteredOrExitedLavaOnVehicle();
        this.advancements.flushDirty(this);
    }

    public void doTick() {
        try {
            if (valid && !this.isSpectator() || !this.touchingUnloadedChunk()) { // Paper - don't tick dead players that are not in the world currently (pending respawn)
                super.tick();
            }

            for (int i = 0; i < this.getInventory().getContainerSize(); ++i) {
                ItemStack itemstack = this.getInventory().getItem(i);

                if (itemstack.getItem().isComplex()) {
                    Packet<?> packet = ((ComplexItem) itemstack.getItem()).getUpdatePacket(itemstack, this.level, this);

                    if (packet != null) {
                        this.connection.send(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastSentHealth || this.lastSentFood != this.foodData.getFoodLevel() || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
                this.connection.send(new ClientboundSetHealthPacket(this.getBukkitEntity().getScaledHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel())); // CraftBukkit
                this.lastSentHealth = this.getHealth();
                this.lastSentFood = this.foodData.getFoodLevel();
                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption) {
                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
                this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
            }

            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
                this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil((float) this.lastRecordedFoodLevel));
            }

            if (this.getAirSupply() != this.lastRecordedAirLevel) {
                this.lastRecordedAirLevel = this.getAirSupply();
                this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil((float) this.lastRecordedAirLevel));
            }

            if (this.getArmorValue() != this.lastRecordedArmor) {
                this.lastRecordedArmor = this.getArmorValue();
                this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil((float) this.lastRecordedArmor));
            }

            if (this.totalExperience != this.lastRecordedExperience) {
                this.lastRecordedExperience = this.totalExperience;
                this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil((float) this.lastRecordedExperience));
            }

            // CraftBukkit start - Force max health updates
            if (this.maxHealthCache != this.getMaxHealth()) {
                this.getBukkitEntity().updateScaledHealth();
            }
            // CraftBukkit end

            if (this.experienceLevel != this.lastRecordedLevel) {
                this.lastRecordedLevel = this.experienceLevel;
                this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil((float) this.lastRecordedLevel));
            }

            if (this.totalExperience != this.lastSentExp) {
                this.lastSentExp = this.totalExperience;
                this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
            }

            if (this.tickCount % 20 == 0) {
                CriteriaTriggers.LOCATION.trigger(this);
            }

            // CraftBukkit start - initialize oldLevel, fire PlayerLevelChangeEvent, and tick client-sided world border
            if (this.oldLevel == -1) {
                this.oldLevel = this.experienceLevel;
            }

            if (this.oldLevel != this.experienceLevel) {
                CraftEventFactory.callPlayerLevelChangeEvent(this.getBukkitEntity(), this.oldLevel, this.experienceLevel);
                this.oldLevel = this.experienceLevel;
            }

            if (this.getBukkitEntity().hasClientWorldBorder()) {
                ((CraftWorldBorder) this.getBukkitEntity().getWorldBorder()).getHandle().tick();
            }
            // CraftBukkit end
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking player");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Player being ticked");

            this.fillCrashReportCategory(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void resetFallDistance() {
        if (this.getHealth() > 0.0F && this.startingToFallPosition != null) {
            CriteriaTriggers.FALL_FROM_HEIGHT.trigger(this, this.startingToFallPosition);
        }

        this.startingToFallPosition = null;
        super.resetFallDistance();
    }

    public void trackStartFallingPosition() {
        if (this.fallDistance > 0.0F && this.startingToFallPosition == null) {
            this.startingToFallPosition = this.position();
        }

    }

    public void trackEnteredOrExitedLavaOnVehicle() {
        if (this.getVehicle() != null && this.getVehicle().isInLava()) {
            if (this.enteredLavaOnVehiclePosition == null) {
                this.enteredLavaOnVehiclePosition = this.position();
            } else {
                CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.trigger(this, this.enteredLavaOnVehiclePosition);
            }
        }

        if (this.enteredLavaOnVehiclePosition != null && (this.getVehicle() == null || !this.getVehicle().isInLava())) {
            this.enteredLavaOnVehiclePosition = null;
        }

    }

    private void updateScoreForCriteria(ObjectiveCriteria criterion, int score) {
        // CraftBukkit - Use our scores instead
        this.level.getCraftServer().getScoreboardManager().getScoreboardScores(criterion, this.getScoreboardName(), (scoreboardscore) -> {
            scoreboardscore.setScore(score);
        });
    }

    // Paper start - process inventory
    private static void processKeep(org.bukkit.event.entity.PlayerDeathEvent event, NonNullList<ItemStack> inv) {
        List<org.bukkit.inventory.ItemStack> itemsToKeep = event.getItemsToKeep();
        if (inv == null) {
            // remainder of items left in toKeep - plugin added stuff on death that wasn't in the initial loot?
            if (!itemsToKeep.isEmpty()) {
                for (org.bukkit.inventory.ItemStack itemStack : itemsToKeep) {
                    event.getEntity().getInventory().addItem(itemStack);
                }
            }

            return;
        }

        for (int i = 0; i < inv.size(); ++i) {
            ItemStack item = inv.get(i);
            if (EnchantmentHelper.hasVanishingCurse(item) || itemsToKeep.isEmpty() || item.isEmpty()) {
                inv.set(i, ItemStack.EMPTY);
                continue;
            }

            final org.bukkit.inventory.ItemStack bukkitStack = item.getBukkitStack();
            boolean keep = false;
            final Iterator<org.bukkit.inventory.ItemStack> iterator = itemsToKeep.iterator();
            while (iterator.hasNext()) {
                final org.bukkit.inventory.ItemStack itemStack = iterator.next();
                if (bukkitStack.equals(itemStack)) {
                    iterator.remove();
                    keep = true;
                    break;
                }
            }

            if (!keep) {
                inv.set(i, ItemStack.EMPTY);
            }
        }
    }
    // Paper end

    @Override
    public void die(DamageSource source) {
        boolean flag = this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        // CraftBukkit start - fire PlayerDeathEvent
        if (this.isRemoved()) {
            return;
        }
        java.util.List<org.bukkit.inventory.ItemStack> loot = new java.util.ArrayList<org.bukkit.inventory.ItemStack>(this.getInventory().getContainerSize());
        boolean keepInventory = this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || this.isSpectator();

        if (!keepInventory) {
            for (ItemStack item : this.getInventory().getContents()) {
                if (!item.isEmpty() && !EnchantmentHelper.hasVanishingCurse(item)) {
                    loot.add(CraftItemStack.asCraftMirror(item));
                }
            }
        }
        if (this.shouldDropLoot() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) { // Paper - preserve this check from vanilla
        // SPIGOT-5071: manually add player loot tables (SPIGOT-5195 - ignores keepInventory rule)
        this.dropFromLootTable(source, this.lastHurtByPlayerTime > 0);
        for (org.bukkit.inventory.ItemStack item : this.drops) {
            loot.add(item);
        }
        this.drops.clear(); // SPIGOT-5188: make sure to clear
        } // Paper

        Component defaultMessage = this.getCombatTracker().getDeathMessage();

        String deathmessage = defaultMessage.getString();
        this.keepLevel = keepInventory; // SPIGOT-2222: pre-set keepLevel
        org.bukkit.event.entity.PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent(this, loot, PaperAdventure.asAdventure(defaultMessage), defaultMessage.getString(), keepInventory); // Paper - Adventure
        // Paper start - cancellable death event
        if (event.isCancelled()) {
            // make compatible with plugins that might have already set the health in an event listener
            if (this.getHealth() <= 0) {
                this.setHealth((float) event.getReviveHealth());
            }
            return;
        }
        // Paper end

        // SPIGOT-943 - only call if they have an inventory open
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer(org.bukkit.event.inventory.InventoryCloseEvent.Reason.DEATH); // Paper
        }

        net.kyori.adventure.text.Component deathMessage = event.deathMessage() != null ? event.deathMessage() : net.kyori.adventure.text.Component.empty(); // Paper - Adventure

        if (deathMessage != null && deathMessage != net.kyori.adventure.text.Component.empty() && flag) { // Paper - Adventure // TODO: allow plugins to override?
            Component ichatbasecomponent = PaperAdventure.asVanilla(deathMessage); // Paper - Adventure

            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), ichatbasecomponent), (future) -> {
                if (!future.isSuccess()) {
                    boolean flag1 = true;
                    String s = ichatbasecomponent.getString(256);
                    TranslatableComponent chatmessage = new TranslatableComponent("death.attack.message_too_long", new Object[]{(new TextComponent(s)).withStyle(ChatFormatting.YELLOW)});
                    MutableComponent ichatmutablecomponent = (new TranslatableComponent("death.attack.even_more_magic", new Object[]{this.getDisplayName()})).withStyle((chatmodifier) -> {
                        return chatmodifier.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, chatmessage));
                    });

                    this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), ichatmutablecomponent));
                }

            });
            Team scoreboardteambase = this.getTeam();

            if (scoreboardteambase != null && scoreboardteambase.getDeathMessageVisibility() != Team.Visibility.ALWAYS) {
                if (scoreboardteambase.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().broadcastToTeam(this, ichatbasecomponent);
                } else if (scoreboardteambase.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().broadcastToAllExceptTeam(this, ichatbasecomponent);
                }
            } else {
                this.server.getPlayerList().broadcastMessage(ichatbasecomponent, ChatType.SYSTEM, Util.NIL_UUID);
            }
        } else {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), TextComponent.EMPTY));
        }

        this.removeEntitiesOnShoulder();
        if (this.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }
        // SPIGOT-5478 must be called manually now
        if (event.shouldDropExperience()) this.dropExperience(); // Paper - tie to event
        // we clean the player's inventory after the EntityDeathEvent is called so plugins can get the exact state of the inventory.
        if (!event.getKeepInventory()) {
            // Paper start - replace logic
            for (NonNullList<ItemStack> inv : this.getInventory().compartments) {
                processKeep(event, inv);
            }
            processKeep(event, null);
            // Paper end
        }

        this.setCamera(this); // Remove spectated target
        // CraftBukkit end

        // CraftBukkit - Get our scores instead
        this.level.getCraftServer().getScoreboardManager().getScoreboardScores(ObjectiveCriteria.DEATH_COUNT, this.getScoreboardName(), Score::increment);
        LivingEntity entityliving = this.getKillCredit();

        if (entityliving != null) {
            this.awardStat(Stats.ENTITY_KILLED_BY.get(entityliving.getType()));
            entityliving.awardKillScore(this, this.deathScore, source);
            this.createWitherRose(entityliving);
        }

        this.level.broadcastEntityEvent(this, (byte) 3);
        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);
        this.getCombatTracker().recheckStatus();
    }

    private void tellNeutralMobsThatIDied() {
        AABB axisalignedbb = (new AABB(this.blockPosition())).inflate(32.0D, 10.0D, 32.0D);

        this.level.getEntitiesOfClass(Mob.class, axisalignedbb, EntitySelector.NO_SPECTATORS).stream().filter((entityinsentient) -> {
            return entityinsentient instanceof NeutralMob;
        }).forEach((entityinsentient) -> {
            ((NeutralMob) entityinsentient).playerDied(this);
        });
    }

    @Override
    public void awardKillScore(Entity entityKilled, int score, DamageSource damageSource) {
        if (entityKilled != this) {
            super.awardKillScore(entityKilled, score, damageSource);
            this.increaseScore(score);
            String s = this.getScoreboardName();
            String s1 = entityKilled.getScoreboardName();

            // CraftBukkit - Get our scores instead
            this.level.getCraftServer().getScoreboardManager().getScoreboardScores(ObjectiveCriteria.KILL_COUNT_ALL, s, Score::increment);
            if (entityKilled instanceof Player) {
                this.awardStat(Stats.PLAYER_KILLS);
                // CraftBukkit - Get our scores instead
                this.level.getCraftServer().getScoreboardManager().getScoreboardScores(ObjectiveCriteria.KILL_COUNT_PLAYERS, s, Score::increment);
            } else {
                this.awardStat(Stats.MOB_KILLS);
            }

            this.handleTeamKill(s, s1, ObjectiveCriteria.TEAM_KILL);
            this.handleTeamKill(s1, s, ObjectiveCriteria.KILLED_BY_TEAM);
            CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(this, entityKilled, damageSource);
        }
    }

    private void handleTeamKill(String playerName, String team, ObjectiveCriteria[] criterions) {
        PlayerTeam scoreboardteam = this.getScoreboard().getPlayersTeam(team);

        if (scoreboardteam != null) {
            int i = scoreboardteam.getColor().getId();

            if (i >= 0 && i < criterions.length) {
                // CraftBukkit - Get our scores instead
                this.level.getCraftServer().getScoreboardManager().getScoreboardScores(criterions[i], playerName, Score::increment);
            }
        }

    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            boolean flag = this.server.isDedicatedServer() && this.isPvpAllowed() && "fall".equals(source.msgId);

            if (!flag && this.spawnInvulnerableTime > 0 && source != DamageSource.OUT_OF_WORLD) {
                return false;
            } else {
                if (source instanceof EntityDamageSource) {
                    Entity entity = source.getEntity();

                    if (entity instanceof Player && !this.canHarmPlayer((Player) entity)) {
                        return false;
                    }

                    if (entity instanceof AbstractArrow) {
                        AbstractArrow entityarrow = (AbstractArrow) entity;
                        Entity entity1 = entityarrow.getOwner();

                        if (entity1 instanceof Player && !this.canHarmPlayer((Player) entity1)) {
                            return false;
                        }
                    }
                }
                // Paper start - cancellable death events
                //return super.hurt(source, amount);
                this.queueHealthUpdatePacket = true;
                boolean damaged = super.hurt(source, amount);
                this.queueHealthUpdatePacket = false;
                if (this.queuedHealthUpdatePacket != null) {
                    this.connection.send(this.queuedHealthUpdatePacket);
                    this.queuedHealthUpdatePacket = null;
                }
                return damaged;
                // Paper end
            }
        }
    }

    @Override
    public boolean canHarmPlayer(Player player) {
        return !this.isPvpAllowed() ? false : super.canHarmPlayer(player);
    }

    private boolean isPvpAllowed() {
        // CraftBukkit - this.server.isPvpAllowed() -> this.world.pvpMode
        return this.level.pvpMode;
    }

    @Nullable
    @Override
    protected PortalInfo findDimensionEntryPoint(ServerLevel destination) {
        PortalInfo shapedetectorshape = super.findDimensionEntryPoint(destination);
        destination = (shapedetectorshape == null) ? destination : shapedetectorshape.world; // CraftBukkit

        if (shapedetectorshape != null && this.level.getTypeKey() == LevelStem.OVERWORLD && destination != null && destination.getTypeKey() == LevelStem.END) { // CraftBukkit
            Vec3 vec3d = shapedetectorshape.pos.add(0.0D, -1.0D, 0.0D);

            return new PortalInfo(vec3d, Vec3.ZERO, 90.0F, 0.0F, destination, shapedetectorshape.portalEventInfo); // CraftBukkit
        } else {
            return shapedetectorshape;
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(ServerLevel destination) {
        // CraftBukkit start
        return this.changeDimension(destination, TeleportCause.UNKNOWN);
    }

    @Nullable
    public Entity changeDimension(ServerLevel worldserver, PlayerTeleportEvent.TeleportCause cause) {
        // CraftBukkit end
        if (this.isSleeping()) return this; // CraftBukkit - SPIGOT-3154
        // this.isChangingDimension = true; // CraftBukkit - Moved down and into PlayerList#changeDimension
        ServerLevel worldserver1 = this.getLevel();
        ResourceKey<LevelStem> resourcekey = worldserver1.getTypeKey(); // CraftBukkit

        if (resourcekey == LevelStem.END && worldserver != null && worldserver.getTypeKey() == LevelStem.OVERWORLD) { // CraftBukkit
            this.isChangingDimension = true; // CraftBukkit - Moved down from above
            this.unRide();
            this.getLevel().removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            if (!this.wonGame) {
                if (level.paperConfig.disableEndCredits) this.seenCredits = true; // Paper - Toggle to always disable end credits
                this.wonGame = true;
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return this;
        } else {
            // CraftBukkit start
            /*
            WorldData worlddata = worldserver.getLevelData();

            this.connection.send(new PacketPlayOutRespawn(worldserver.dimensionTypeRegistration(), worldserver.dimension(), BiomeManager.obfuscateSeed(worldserver.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), worldserver.isDebug(), worldserver.isFlat(), true));
            this.connection.send(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
            PlayerList playerlist = this.server.getPlayerList();

            playerlist.sendPlayerPermissionLevel(this);
            worldserver1.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            */
            // CraftBukkit end
            PortalInfo shapedetectorshape = this.findDimensionEntryPoint(worldserver);

            if (shapedetectorshape != null) {
                worldserver1.getProfiler().push("moving");
                worldserver = shapedetectorshape.world; // CraftBukkit
                if (worldserver == null) { } else // CraftBukkit - empty to fall through to null to event
                if (resourcekey == LevelStem.OVERWORLD && worldserver.getTypeKey() == LevelStem.NETHER) { // CraftBukkit
                    this.enteredNetherPosition = this.position();
                } else if (worldserver.getTypeKey() == LevelStem.END && shapedetectorshape.portalEventInfo != null && shapedetectorshape.portalEventInfo.getCanCreatePortal()) { // CraftBukkit
                    this.createEndPlatform(worldserver, new BlockPos(shapedetectorshape.pos));
                }
                // CraftBukkit start
            } else {
                return null;
            }
            Location enter = this.getBukkitEntity().getLocation();
            Location exit = (worldserver == null) ? null : new Location(worldserver.getWorld(), shapedetectorshape.pos.x, shapedetectorshape.pos.y, shapedetectorshape.pos.z, shapedetectorshape.yRot, shapedetectorshape.xRot);
            PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.getBukkitEntity(), enter, exit, cause);
            Bukkit.getServer().getPluginManager().callEvent(tpEvent);
            if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
                return null;
            }
            exit = tpEvent.getTo();
            worldserver = ((CraftWorld) exit.getWorld()).getHandle();
            // CraftBukkit end

            worldserver1.getProfiler().pop();
            worldserver1.getProfiler().push("placing");
            if (true) { // CraftBukkit
                this.isChangingDimension = true; // CraftBukkit - Set teleport invulnerability only if player changing worlds

                this.connection.send(new ClientboundRespawnPacket(worldserver.dimensionTypeRegistration(), worldserver.dimension(), BiomeManager.obfuscateSeed(worldserver.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), worldserver.isDebug(), worldserver.isFlat(), true));
                this.connection.send(new ClientboundChangeDifficultyPacket(worldserver.getDifficulty(), this.level.getLevelData().isDifficultyLocked())); // Paper - fix difficulty sync issue
                PlayerList playerlist = this.server.getPlayerList();

                playerlist.sendPlayerPermissionLevel(this);
                worldserver1.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
                this.unsetRemoved();

                // CraftBukkit end
                this.setLevel(worldserver);
                worldserver.addDuringPortalTeleport(this);
                this.connection.teleport(exit); // CraftBukkit - use internal teleport without event
                this.connection.resetPosition(); // CraftBukkit - sync position after changing it (from PortalTravelAgent#findAndteleport)
                worldserver1.getProfiler().pop();
                this.triggerDimensionChangeTriggers(worldserver1);
                this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                playerlist.sendLevelInfo(this, worldserver);
                playerlist.sendAllPlayerInfo(this);
                Iterator iterator = this.getActiveEffects().iterator();

                while (iterator.hasNext()) {
                    MobEffectInstance mobeffect = (MobEffectInstance) iterator.next();

                    this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), mobeffect));
                }

                this.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;

                // CraftBukkit start
                PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.getBukkitEntity(), worldserver1.getWorld());
                this.level.getCraftServer().getPluginManager().callEvent(changeEvent);
                // CraftBukkit end
            }
            // Paper start
            if (this.isBlocking()) {
                this.stopUsingItem();
            }
            // Paper end

            return this;
        }
    }

    // CraftBukkit start
    @Override
    protected CraftPortalEvent callPortalEvent(Entity entity, ServerLevel exitWorldServer, BlockPos exitPosition, TeleportCause cause, int searchRadius, int creationRadius) {
        Location enter = this.getBukkitEntity().getLocation();
        Location exit = new Location(exitWorldServer.getWorld(), exitPosition.getX(), exitPosition.getY(), exitPosition.getZ(), getYRot(), getXRot());
        PlayerPortalEvent event = new PlayerPortalEvent(this.getBukkitEntity(), enter, exit, cause, searchRadius, true, creationRadius);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null) {
            return null;
        }
        return new CraftPortalEvent(event);
    }
    // CraftBukkit end

    private void createEndPlatform(ServerLevel world, BlockPos centerPos) {
        BlockPos.MutableBlockPos blockposition_mutableblockposition = centerPos.mutable();

        org.bukkit.craftbukkit.v1_18_R2.util.BlockStateListPopulator blockList = new org.bukkit.craftbukkit.v1_18_R2.util.BlockStateListPopulator(world); // Paper
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                for (int k = -1; k < 3; ++k) {
                    BlockState iblockdata = k == -1 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();

                    blockList.setBlock(blockposition_mutableblockposition.set(centerPos).move(j, k, i), iblockdata, 3); // Paper
                }
            }
        }
        // Paper start
        if (new org.bukkit.event.world.PortalCreateEvent((List< org.bukkit.block.BlockState>) (List) blockList.getList(), world.getWorld(), this.getBukkitEntity(), org.bukkit.event.world.PortalCreateEvent.CreateReason.END_PLATFORM).callEvent()) {
            blockList.updateList();
        }
        // Paper end

    }

    @Override
    protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel worldserver, BlockPos blockposition, boolean flag, WorldBorder worldborder, int searchRadius, boolean canCreatePortal, int createRadius) { // CraftBukkit
        Optional<BlockUtil.FoundRectangle> optional = super.getExitPortal(worldserver, blockposition, flag, worldborder, searchRadius, canCreatePortal, createRadius); // CraftBukkit

        if (optional.isPresent() || !canCreatePortal) { // CraftBukkit
            return optional;
        } else {
            Direction.Axis enumdirection_enumaxis = (Direction.Axis) this.level.getBlockState(this.portalEntrancePos).getOptionalValue(NetherPortalBlock.AXIS).orElse(Direction.Axis.X);
            Optional<BlockUtil.FoundRectangle> optional1 = worldserver.getPortalForcer().createPortal(blockposition, enumdirection_enumaxis, this, createRadius); // CraftBukkit

            if (!optional1.isPresent()) {
                // EntityPlayer.LOGGER.error("Unable to create a portal, likely target out of worldborder"); // CraftBukkit
            }

            return optional1;
        }
    }

    public void triggerDimensionChangeTriggers(ServerLevel origin) {
        ResourceKey<Level> resourcekey = origin.dimension();
        ResourceKey<Level> resourcekey1 = this.level.dimension();
        // CraftBukkit start
        ResourceKey<Level> maindimensionkey = CraftDimensionUtil.getMainDimensionKey(origin);
        ResourceKey<Level> maindimensionkey1 = CraftDimensionUtil.getMainDimensionKey(this.level);

        CriteriaTriggers.CHANGED_DIMENSION.trigger(this, maindimensionkey, maindimensionkey1);
        if (maindimensionkey != resourcekey || maindimensionkey1 != resourcekey1) {
            CriteriaTriggers.CHANGED_DIMENSION.trigger(this, resourcekey, resourcekey1);
        }

        if (maindimensionkey == Level.NETHER && maindimensionkey1 == Level.OVERWORLD && this.enteredNetherPosition != null) {
            // CraftBukkit end
            CriteriaTriggers.NETHER_TRAVEL.trigger(this, this.enteredNetherPosition);
        }

        if (maindimensionkey1 != Level.NETHER) { // CraftBukkit
            this.enteredNetherPosition = null;
        }

    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer spectator) {
        return spectator.isSpectator() ? this.getCamera() == this : (this.isSpectator() ? false : super.broadcastToPlayer(spectator));
    }

    @Override
    public void take(Entity item, int count) {
        super.take(item, count);
        this.containerMenu.broadcastChanges();
    }

    // CraftBukkit start - moved bed result checks from below into separate method
    private Either<Player.BedSleepingProblem, Unit> getBedResult(BlockPos blockposition, Direction enumdirection) {
        if (!this.isSleeping() && this.isAlive()) {
            if (!this.level.dimensionType().natural() || !this.level.dimensionType().bedWorks()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
            } else if (!this.bedInRange(blockposition, enumdirection)) {
                return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
            } else if (this.bedBlocked(blockposition, enumdirection)) {
                return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
            } else {
                this.setRespawnPosition(this.level.dimension(), blockposition, this.getYRot(), false, true, com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause.BED); // Paper - PlayerSetSpawnEvent
                if (this.level.isDay()) {
                    return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
                } else {
                    if (!this.isCreative()) {
                        double d0 = 8.0D;
                        double d1 = 5.0D;
                        Vec3 vec3d = Vec3.atBottomCenterOf(blockposition);
                        List<Monster> list = this.level.getEntitiesOfClass(Monster.class, new AABB(vec3d.x() - 8.0D, vec3d.y() - 5.0D, vec3d.z() - 8.0D, vec3d.x() + 8.0D, vec3d.y() + 5.0D, vec3d.z() + 8.0D), (entitymonster) -> {
                            return entitymonster.isPreventingPlayerRest(this);
                        });

                        if (!list.isEmpty()) {
                            return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                        }
                    }

                    return Either.right(Unit.INSTANCE);
                }
            }
        } else {
            return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @Override
    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos blockposition, boolean force) {
        Direction enumdirection = (Direction) this.level.getBlockState(blockposition).getValue(HorizontalDirectionalBlock.FACING);
        Either<Player.BedSleepingProblem, Unit> bedResult = this.getBedResult(blockposition, enumdirection);

        if (bedResult.left().orElse(null) == Player.BedSleepingProblem.OTHER_PROBLEM) {
            return bedResult; // return immediately if the result is not bypassable by plugins
        }

        if (force) {
            bedResult = Either.right(Unit.INSTANCE);
        }

        bedResult = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPlayerBedEnterEvent(this, blockposition, bedResult);
        if (bedResult.left().isPresent()) {
            return bedResult;
        }

        {
            {
                {
                    Either<Player.BedSleepingProblem, Unit> either = super.startSleepInBed(blockposition, force).ifRight((unit) -> {
                        this.awardStat(Stats.SLEEP_IN_BED);
                        CriteriaTriggers.SLEPT_IN_BED.trigger(this);
                    });

                    if (!this.getLevel().canSleepThroughNights()) {
                        this.displayClientMessage(new TranslatableComponent("sleep.not_possible"), true);
                    }

                    ((ServerLevel) this.level).updateSleepingPlayerList();
                    return either;
                }
            }
        }
        // CraftBukkit end
    }

    @Override
    public void startSleeping(BlockPos pos) {
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        super.startSleeping(pos);
    }

    private boolean bedInRange(BlockPos pos, Direction direction) {
        return this.isReachableBedBlock(pos) || this.isReachableBedBlock(pos.relative(direction.getOpposite()));
    }

    private boolean isReachableBedBlock(BlockPos pos) {
        Vec3 vec3d = Vec3.atBottomCenterOf(pos);

        return Math.abs(this.getX() - vec3d.x()) <= 3.0D && Math.abs(this.getY() - vec3d.y()) <= 2.0D && Math.abs(this.getZ() - vec3d.z()) <= 3.0D;
    }

    private boolean bedBlocked(BlockPos pos, Direction direction) {
        BlockPos blockposition1 = pos.above();

        return !this.freeAt(blockposition1) || !this.freeAt(blockposition1.relative(direction.getOpposite()));
    }

    @Override
    public void stopSleepInBed(boolean skipSleepTimer, boolean updateSleepingPlayers) {
        if (!this.isSleeping()) return; // CraftBukkit - Can't leave bed if not in one!
        // CraftBukkit start - fire PlayerBedLeaveEvent
        CraftPlayer player = this.getBukkitEntity();
        BlockPos bedPosition = this.getSleepingPos().orElse(null);

        org.bukkit.block.Block bed;
        if (bedPosition != null) {
            bed = this.level.getWorld().getBlockAt(bedPosition.getX(), bedPosition.getY(), bedPosition.getZ());
        } else {
            bed = this.level.getWorld().getBlockAt(player.getLocation());
        }

        PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
        this.level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        // CraftBukkit end
        if (this.isSleeping()) {
            this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(this, 2));
        }

        super.stopSleepInBed(skipSleepTimer, updateSleepingPlayers);
        if (this.connection != null) {
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }

    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        Entity entity1 = this.getVehicle();

        if (!super.startRiding(entity, force)) {
            return false;
        } else {
            Entity entity2 = this.getVehicle();

            if (entity2 != entity1 && this.connection != null) {
                this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            }

            return true;
        }
    }

    // Paper start
    @Override public void stopRiding() { stopRiding(false); }
    @Override public void stopRiding(boolean suppressCancellation) {
        // Paper end
        Entity entity = this.getVehicle();

        super.stopRiding(suppressCancellation); // Paper
        Entity entity1 = this.getVehicle();

        if (entity1 != entity && this.connection != null) {
            this.connection.dismount(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }

    }

    @Override
    public void dismountTo(double destX, double destY, double destZ) {
        this.removeVehicle();
        if (this.connection != null) {
            this.connection.dismount(destX, destY, destZ, this.getYRot(), this.getXRot());
        }

    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return super.isInvulnerableTo(damageSource) || this.isChangingDimension() || this.getAbilities().invulnerable && damageSource == DamageSource.WITHER || !level.paperConfig.allowPlayerCrammingDamage && damageSource == DamageSource.CRAMMING; // Paper - disable player cramming
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {}

    @Override
    protected void onChangedBlock(BlockPos pos) {
        if (!this.isSpectator()) {
            super.onChangedBlock(pos);
        }

    }

    public void doCheckFallDamage(double heightDifference, boolean onGround) {
        if (!this.touchingUnloadedChunk()) {
            BlockPos blockposition = this.getOnPos();

            super.checkFallDamage(heightDifference, onGround, this.level.getBlockState(blockposition), blockposition);
        }
    }

    @Override
    public void openTextEdit(SignBlockEntity sign) {
        sign.setAllowedPlayerEditor(this.getUUID());
        this.connection.send(new ClientboundBlockUpdatePacket(this.level, sign.getBlockPos()));
        this.connection.send(new ClientboundOpenSignEditorPacket(sign.getBlockPos()));
    }

    public int nextContainerCounter() { // CraftBukkit - void -> int
        this.containerCounter = this.containerCounter % 100 + 1;
        return this.containerCounter; // CraftBukkit
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider factory) {
        if (factory == null) {
            return OptionalInt.empty();
        } else {
            // CraftBukkit start - SPIGOT-6552: Handle inventory closing in CraftEventFactory#callInventoryOpenEvent(...)
            /*
            if (this.containerMenu != this.inventoryMenu) {
                this.closeContainer();
            }
            */
            // CraftBukkit end

            this.nextContainerCounter();
            AbstractContainerMenu container = factory.createMenu(this.containerCounter, this.getInventory(), this);

            // CraftBukkit start - Inventory open hook
            if (container != null) {
                container.setTitle(factory.getDisplayName());

                boolean cancelled = false;
                container = CraftEventFactory.callInventoryOpenEvent(this, container, cancelled);
                if (container == null && !cancelled) { // Let pre-cancelled events fall through
                    // SPIGOT-5263 - close chest if cancelled
                    if (factory instanceof Container) {
                        ((Container) factory).stopOpen(this);
                    } else if (factory instanceof ChestBlock.DoubleInventory) {
                        // SPIGOT-5355 - double chests too :(
                        ((ChestBlock.DoubleInventory) factory).inventorylargechest.stopOpen(this);
                    }
                    return OptionalInt.empty();
                }
            }
            // CraftBukkit end
            if (container == null) {
                if (this.isSpectator()) {
                    this.displayClientMessage((new TranslatableComponent("container.spectatorCantOpen")).withStyle(ChatFormatting.RED), true);
                }

                return OptionalInt.empty();
            } else {
                // CraftBukkit start
                this.containerMenu = container;
                if (!isImmobile()) this.connection.send(new ClientboundOpenScreenPacket(container.containerId, container.getType(), container.getTitle())); // Paper
                // CraftBukkit end
                this.initMenu(container);
                return OptionalInt.of(this.containerCounter);
            }
        }
    }

    @Override
    public void sendMerchantOffers(int syncId, MerchantOffers offers, int levelProgress, int experience, boolean leveled, boolean refreshable) {
        this.connection.send(new ClientboundMerchantOffersPacket(syncId, offers, levelProgress, experience, leveled, refreshable));
    }

    @Override
    public void openHorseInventory(AbstractHorse horse, Container inventory) {
        // CraftBukkit start - Inventory open hook
        this.nextContainerCounter();
        AbstractContainerMenu container = new HorseInventoryMenu(this.containerCounter, this.getInventory(), inventory, horse);
        container.setTitle(horse.getDisplayName());
        container = CraftEventFactory.callInventoryOpenEvent(this, container);

        if (container == null) {
            inventory.stopOpen(this);
            return;
        }
        // CraftBukkit end
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer(org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW); // Paper
        }

        // this.nextContainerCounter(); // CraftBukkit - moved up
        this.connection.send(new ClientboundHorseScreenOpenPacket(this.containerCounter, inventory.getContainerSize(), horse.getId()));
        this.containerMenu = container; // CraftBukkit
        this.initMenu(this.containerMenu);
    }

    @Override
    public void openItemGui(ItemStack book, InteractionHand hand) {
        if (book.is(Items.WRITTEN_BOOK)) {
            if (WrittenBookItem.resolveBookComponents(book, this.createCommandSourceStack(), this)) {
                this.containerMenu.broadcastChanges();
            }

            this.connection.send(new ClientboundOpenBookPacket(hand));
        }

    }

    @Override
    public void openCommandBlock(CommandBlockEntity commandBlock) {
        this.connection.send(ClientboundBlockEntityDataPacket.create(commandBlock, BlockEntity::saveWithoutMetadata));
    }

    @Override
    public void closeContainer() {
        // Paper start
        this.closeContainer(org.bukkit.event.inventory.InventoryCloseEvent.Reason.UNKNOWN);
    }
    @Override
    public void closeContainer(org.bukkit.event.inventory.InventoryCloseEvent.Reason reason) {
        CraftEventFactory.handleInventoryCloseEvent(this, reason); // CraftBukkit
        // Paper end
        this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
        this.doCloseContainer();
    }
    // Paper start - special close for unloaded inventory
    @Override
    public void closeUnloadedInventory(org.bukkit.event.inventory.InventoryCloseEvent.Reason reason) {
        // copied from above
        CraftEventFactory.handleInventoryCloseEvent(this, reason); // CraftBukkit
        // Paper end
        // copied from below
        this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
        this.containerMenu = this.inventoryMenu;
        // do not run close logic
    }
    // Paper end - special close for unloaded inventory

    public void doCloseContainer() {
        this.containerMenu.removed(this);
        this.inventoryMenu.transferState(this.containerMenu);
        this.containerMenu = this.inventoryMenu;
    }

    public void setPlayerInput(float sidewaysSpeed, float forwardSpeed, boolean jumping, boolean sneaking) {
        if (this.isPassenger()) {
            if (sidewaysSpeed >= -1.0F && sidewaysSpeed <= 1.0F) {
                this.xxa = sidewaysSpeed;
            }

            if (forwardSpeed >= -1.0F && forwardSpeed <= 1.0F) {
                this.zza = forwardSpeed;
            }

            this.jumping = jumping;
            this.setShiftKeyDown(sneaking);
        }

    }

    @Override
    public void awardStat(Stat<?> stat, int amount) {
        this.stats.increment(this, stat, amount);
        this.level.getCraftServer().getScoreboardManager().getScoreboardScores(stat, this.getScoreboardName(), (scoreboardscore) -> { // CraftBukkit - Get our scores instead
            scoreboardscore.add(amount);
        });
    }

    @Override
    public void resetStat(Stat<?> stat) {
        this.stats.setValue(this, stat, 0);
        this.level.getCraftServer().getScoreboardManager().getScoreboardScores(stat, this.getScoreboardName(), Score::reset); // CraftBukkit - Get our scores instead
    }

    @Override
    public int awardRecipes(Collection<Recipe<?>> recipes) {
        return this.recipeBook.addRecipes(recipes, this);
    }

    @Override
    public void awardRecipesByKey(ResourceLocation[] ids) {
        List<Recipe<?>> list = Lists.newArrayList();
        ResourceLocation[] aminecraftkey1 = ids;
        int i = ids.length;

        for (int j = 0; j < i; ++j) {
            ResourceLocation minecraftkey = aminecraftkey1[j];
            Optional<? extends Recipe<?>> optional = this.server.getRecipeManager().byKey(minecraftkey); // CraftBukkit - decompile error

            Objects.requireNonNull(list);
            optional.ifPresent(list::add);
        }

        this.awardRecipes(list);
    }

    @Override
    public int resetRecipes(Collection<Recipe<?>> recipes) {
        return this.recipeBook.removeRecipes(recipes, this);
    }

    @Override
    public void giveExperiencePoints(int experience) {
        super.giveExperiencePoints(experience);
        this.lastSentExp = -1;
    }

    public void disconnect() {
        this.disconnected = true;
        this.ejectPassengers();

        // Paper start - Workaround an issue where the vehicle doesn't track the passenger disconnection dismount.
        if (this.isPassenger() && this.getVehicle() instanceof ServerPlayer) {
            this.stopRiding();
        }
        // Paper end

        if (this.isSleeping()) {
            this.stopSleepInBed(true, false);
        }

    }

    public boolean hasDisconnected() {
        return this.disconnected;
    }

    public void resetSentInfo() {
        this.lastSentHealth = -1.0E8F;
        this.lastSentExp = -1; // CraftBukkit - Added to reset
    }

    // CraftBukkit start - Support multi-line messages
    public void sendMessage(UUID uuid, Component[] ichatbasecomponent) {
        for (Component component : ichatbasecomponent) {
            this.sendMessage(component, (uuid == null) ? Util.NIL_UUID : uuid);
        }
    }
    // CraftBukkit end

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
        this.sendMessage(message, actionBar ? ChatType.GAME_INFO : ChatType.CHAT, Util.NIL_UUID);
    }

    @Override
    protected void completeUsingItem() {
        if (!this.useItem.isEmpty() && this.isUsingItem()) {
            this.connection.send(new ClientboundEntityEventPacket(this, (byte) 9));
            super.completeUsingItem();
        }

    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor anchorPoint, Vec3 target) {
        super.lookAt(anchorPoint, target);
        this.connection.send(new ClientboundPlayerLookAtPacket(anchorPoint, target.x, target.y, target.z));
    }

    public void lookAt(EntityAnchorArgument.Anchor anchorPoint, Entity targetEntity, EntityAnchorArgument.Anchor targetAnchor) {
        Vec3 vec3d = targetAnchor.apply(targetEntity);

        super.lookAt(anchorPoint, vec3d);
        this.connection.send(new ClientboundPlayerLookAtPacket(anchorPoint, targetEntity, targetAnchor));
    }

    public void restoreFrom(ServerPlayer oldPlayer, boolean alive) {
        this.textFilteringEnabled = oldPlayer.textFilteringEnabled;
        this.gameMode.setGameModeForPlayer(oldPlayer.gameMode.getGameModeForPlayer(), oldPlayer.gameMode.getPreviousGameModeForPlayer());
        if (alive) {
            this.getInventory().replaceWith(oldPlayer.getInventory());
            this.setHealth(oldPlayer.getHealth());
            this.foodData = oldPlayer.foodData;
            this.experienceLevel = oldPlayer.experienceLevel;
            this.totalExperience = oldPlayer.totalExperience;
            this.experienceProgress = oldPlayer.experienceProgress;
            this.setScore(oldPlayer.getScore());
            this.portalEntrancePos = oldPlayer.portalEntrancePos;
        } else if (this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || oldPlayer.isSpectator()) {
            this.getInventory().replaceWith(oldPlayer.getInventory());
            this.experienceLevel = oldPlayer.experienceLevel;
            this.totalExperience = oldPlayer.totalExperience;
            this.experienceProgress = oldPlayer.experienceProgress;
            this.setScore(oldPlayer.getScore());
        }

        this.enchantmentSeed = oldPlayer.enchantmentSeed;
        this.enderChestInventory = oldPlayer.enderChestInventory;
        this.getEntityData().set(ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION, (Byte) oldPlayer.getEntityData().get(ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION));
        this.lastSentExp = -1;
        this.lastSentHealth = -1.0F;
        this.lastSentFood = -1;
        // this.recipeBook.copyOverData(entityplayer.recipeBook); // CraftBukkit
        this.seenCredits = oldPlayer.seenCredits;
        this.enteredNetherPosition = oldPlayer.enteredNetherPosition;
        this.setShoulderEntityLeft(oldPlayer.getShoulderEntityLeft());
        this.setShoulderEntityRight(oldPlayer.getShoulderEntityRight());
    }

    @Override
    protected void onEffectAdded(MobEffectInstance effect, @Nullable Entity source) {
        super.onEffectAdded(effect, source);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effect));
        if (effect.getEffect() == MobEffects.LEVITATION) {
            this.levitationStartTime = this.tickCount;
            this.levitationStartPos = this.position();
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, source);
    }

    @Override
    protected void onEffectUpdated(MobEffectInstance effect, boolean reapplyEffect, @Nullable Entity source) {
        super.onEffectUpdated(effect, reapplyEffect, source);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effect));
        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, source);
    }

    @Override
    protected void onEffectRemoved(MobEffectInstance effect) {
        super.onEffectRemoved(effect);
        this.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), effect.getEffect()));
        if (effect.getEffect() == MobEffects.LEVITATION) {
            this.levitationStartPos = null;
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, (Entity) null);
    }

    @Override
    public void teleportTo(double destX, double destY, double destZ) {
        this.connection.teleport(destX, destY, destZ, this.getYRot(), this.getXRot());
    }

    @Override
    public void moveTo(double x, double y, double z) {
        this.teleportTo(x, y, z);
        this.connection.resetPosition();
    }

    @Override
    public void crit(Entity target) {
        this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(target, 4));
    }

    @Override
    public void magicCrit(Entity target) {
        this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(target, 5));
    }

    @Override
    public void onUpdateAbilities() {
        if (this.connection != null) {
            this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
            this.updateInvisibilityStatus();
        }
    }

    @Override
    public ServerLevel getLevel() {
        return (ServerLevel) this.level;
    }

    public boolean setGameMode(GameType gameMode) {
        // Paper start - Add cause and nullable message to event
        org.bukkit.event.player.PlayerGameModeChangeEvent event = this.setGameMode(gameMode, org.bukkit.event.player.PlayerGameModeChangeEvent.Cause.UNKNOWN, null);
        return event == null ? false : event.isCancelled();
    }
    public org.bukkit.event.player.PlayerGameModeChangeEvent setGameMode(GameType gameMode, org.bukkit.event.player.PlayerGameModeChangeEvent.Cause cause, net.kyori.adventure.text.Component message) {
        org.bukkit.event.player.PlayerGameModeChangeEvent event = this.gameMode.changeGameModeForPlayer(gameMode, cause, message);
        if (event == null || event.isCancelled()) {
            // Paper end
            return null;
        } else {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float) gameMode.getId()));
            if (gameMode == GameType.SPECTATOR) {
                this.removeEntitiesOnShoulder();
                this.stopRiding();
            } else {
                this.setCamera(this);
            }

            this.onUpdateAbilities();
            this.updateEffectVisibility();
            return event; // Paper
        }
    }

    @Override
    public boolean isSpectator() {
        return this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        return this.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
    }

    @Override
    public void sendMessage(Component message, UUID sender) {
        this.sendMessage(message, ChatType.SYSTEM, sender);
    }

    public void sendMessage(Component message, ChatType type, UUID sender) {
        if (this.acceptsChat(type)) {
            this.connection.send(new ClientboundChatPacket(message, type, sender), (future) -> {
                if (!future.isSuccess() && (type == ChatType.GAME_INFO || type == ChatType.SYSTEM) && this.acceptsChat(ChatType.SYSTEM)) {
                    boolean flag = true;
                    String s = message.getString(256);
                    MutableComponent ichatmutablecomponent = (new TextComponent(s)).withStyle(ChatFormatting.YELLOW);

                    this.connection.send(new ClientboundChatPacket((new TranslatableComponent("multiplayer.message_not_delivered", new Object[]{ichatmutablecomponent})).withStyle(ChatFormatting.RED), ChatType.SYSTEM, sender));
                }

            });
        }
    }

    public String getIpAddress() {
        String s = this.connection.connection.getRemoteAddress().toString();

        s = s.substring(s.indexOf("/") + 1);
        s = s.substring(0, s.indexOf(":"));
        return s;
    }

    public String locale = null; // CraftBukkit - add, lowercase // Paper - default to null
    public java.util.Locale adventure$locale = java.util.Locale.US; // Paper
    public void updateOptions(ServerboundClientInformationPacket packet) {
        new com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent(getBukkitEntity(), packet.language, packet.viewDistance, com.destroystokyo.paper.ClientOption.ChatVisibility.valueOf(packet.chatVisibility().name()), packet.chatColors(), new com.destroystokyo.paper.PaperSkinParts(packet.modelCustomisation()), packet.mainHand() == HumanoidArm.LEFT ? MainHand.LEFT : MainHand.RIGHT).callEvent(); // Paper - settings event
        // CraftBukkit start
        if (getMainArm() != packet.mainHand()) {
            PlayerChangedMainHandEvent event = new PlayerChangedMainHandEvent(this.getBukkitEntity(), getMainArm() == HumanoidArm.LEFT ? MainHand.LEFT : MainHand.RIGHT);
            this.server.server.getPluginManager().callEvent(event);
        }
        if (this.locale == null || !this.locale.equals(packet.language)) { // Paper - check for null
            PlayerLocaleChangeEvent event = new PlayerLocaleChangeEvent(this.getBukkitEntity(), packet.language);
            this.server.server.getPluginManager().callEvent(event);
            this.server.server.getPluginManager().callEvent(new com.destroystokyo.paper.event.player.PlayerLocaleChangeEvent(this.getBukkitEntity(), this.locale, packet.language)); // Paper
        }
        this.locale = packet.language;
        // Paper start
        this.adventure$locale = net.kyori.adventure.translation.Translator.parseLocale(this.locale);
        this.connection.connection.channel.attr(PaperAdventure.LOCALE_ATTRIBUTE).set(this.adventure$locale);
        // Paper end
        this.clientViewDistance = packet.viewDistance;
        // CraftBukkit end
        this.chatVisibility = packet.chatVisibility();
        this.canChatColor = packet.chatColors();
        this.textFilteringEnabled = packet.textFilteringEnabled();
        this.allowsListing = packet.allowsListing();
        this.getEntityData().set(ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION, (byte) packet.modelCustomisation());
        this.getEntityData().set(ServerPlayer.DATA_PLAYER_MAIN_HAND, (byte) (packet.mainHand() == HumanoidArm.LEFT ? 0 : 1));
    }

    public boolean canChatInColor() {
        return this.canChatColor;
    }

    public ChatVisiblity getChatVisibility() {
        return this.chatVisibility;
    }

    private boolean acceptsChat(ChatType type) {
        switch (this.chatVisibility) {
            case HIDDEN:
                return type == ChatType.GAME_INFO;
            case SYSTEM:
                return type == ChatType.SYSTEM || type == ChatType.GAME_INFO;
            case FULL:
            default:
                return true;
        }
    }

    public void sendTexturePack(String url, String hash, boolean required, @Nullable Component resourcePackPrompt) {
        this.connection.send(new ClientboundResourcePackPacket(url, hash, required, resourcePackPrompt));
    }

    @Override
    protected int getPermissionLevel() {
        return this.server.getProfilePermissions(this.getGameProfile());
    }

    public void resetLastActionTime() {
        this.lastActionTime = Util.getMillis();
    }

    public ServerStatsCounter getStats() {
        return this.stats;
    }

    public ServerRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    @Override
    protected void updateInvisibilityStatus() {
        if (this.isSpectator()) {
            this.removeEffectParticles();
            this.setInvisible(true);
        } else {
            super.updateInvisibilityStatus();
        }

    }

    public Entity getCamera() {
        return (Entity) (this.camera == null ? this : this.camera);
    }

    public void setCamera(@Nullable Entity entity) {
        // Paper start - Add PlayerStartSpectatingEntityEvent and PlayerStopSpectatingEntity Event and improve implementation
        Entity entity1 = this.getCamera();

        if (entity == null) {
            entity = this;
        }

        if (entity1 == entity) return; // new spec target is the current spec target

        if (entity == this) {
            com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent playerStopSpectatingEntityEvent = new com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent(this.getBukkitEntity(), entity1.getBukkitEntity());

            if (!playerStopSpectatingEntityEvent.callEvent()) {
                return;
            }
        } else {
            com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent playerStartSpectatingEntityEvent = new com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent(this.getBukkitEntity(), entity1.getBukkitEntity(), entity.getBukkitEntity());

            if (!playerStartSpectatingEntityEvent.callEvent()) {
                return;
            }
        }
        // Validate
        if (entity != this) {
            if (entity.isRemoved() || !entity.valid || entity.level == null) {
                MinecraftServer.LOGGER.info("Blocking player " + this + " from spectating invalid entity " + entity);
                return;
            }
            if (this.isImmobile()) {
                // use debug: clients might maliciously spam this
                MinecraftServer.LOGGER.debug("Blocking frozen player " + this + " from spectating entity " + entity);
                return;
            }
        }

        this.camera = entity; // only set after validating state

        if (entity != this) {
            // Make sure we're in the right place
            this.ejectPassengers(); // teleport can fail if we have passengers...
            this.getBukkitEntity().teleport(new Location(entity.getCommandSenderWorld().getWorld(), entity.getX(), entity.getY(), entity.getZ(), this.getYRot(), this.getXRot()), TeleportCause.SPECTATE); // Correctly handle cross-world entities from api calls by using CB teleport

            // Make sure we're tracking the entity before sending
            ChunkMap.TrackedEntity tracker = ((ServerLevel)entity.level).getChunkSource().chunkMap.entityMap.get(entity.getId());
            if (tracker != null) { // dumb plugins...
                tracker.updatePlayer(this);
            }
        } else {
            this.connection.teleport(this.camera.getX(), this.camera.getY(), this.camera.getZ(), this.getYRot(), this.getXRot(), TeleportCause.SPECTATE); // CraftBukkit
        }
        this.connection.send(new ClientboundSetCameraPacket(entity));
        // Paper end
    }

    @Override
    protected void processPortalCooldown() {
        if (!this.isChangingDimension) {
            super.processPortalCooldown();
        }

    }

    @Override
    public void attack(Entity target) {
        if (this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            this.setCamera(target);
        } else {
            super.attack(target);
        }

    }

    public long getLastActionTime() {
        return this.lastActionTime;
    }

    @Nullable
    public Component getTabListDisplayName() {
        return this.listName; // CraftBukkit
    }

    @Override
    public void swing(InteractionHand hand) {
        super.swing(hand);
        this.resetAttackStrengthTicker();
    }

    public boolean isChangingDimension() {
        return this.isChangingDimension;
    }

    public void hasChangedDimension() {
        this.isChangingDimension = false;
    }

    public PlayerAdvancements getAdvancements() {
        return this.advancements;
    }

    // CraftBukkit start
    public void teleportTo(ServerLevel targetWorld, double x, double y, double z, float yaw, float pitch) {
        this.teleportTo(targetWorld, x, y, z, yaw, pitch, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void teleportTo(ServerLevel worldserver, double d0, double d1, double d2, float f, float f1, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        // CraftBukkit end
        this.setCamera(this);
        this.stopRiding();
        /* CraftBukkit start - replace with bukkit handling for multi-world
        if (worldserver == this.level) {
            this.connection.teleport(d0, d1, d2, f, f1);
        } else {
            WorldServer worldserver1 = this.getLevel();
            WorldData worlddata = worldserver.getLevelData();

            this.connection.send(new PacketPlayOutRespawn(worldserver.dimensionTypeRegistration(), worldserver.dimension(), BiomeManager.obfuscateSeed(worldserver.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), worldserver.isDebug(), worldserver.isFlat(), true));
            this.connection.send(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
            this.server.getPlayerList().sendPlayerPermissionLevel(this);
            worldserver1.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            this.moveTo(d0, d1, d2, f, f1);
            this.setLevel(worldserver);
            worldserver.addDuringCommandTeleport(this);
            this.triggerDimensionChangeTriggers(worldserver1);
            this.connection.teleport(d0, d1, d2, f, f1);
            this.server.getPlayerList().sendLevelInfo(this, worldserver);
            this.server.getPlayerList().sendAllPlayerInfo(this);
        }
        */
        this.getBukkitEntity().teleport(new Location(worldserver.getWorld(), d0, d1, d2, f, f1), cause);
        // CraftBukkit end

    }

    @Nullable
    public BlockPos getRespawnPosition() {
        return this.respawnPosition;
    }

    public float getRespawnAngle() {
        return this.respawnAngle;
    }

    public ResourceKey<Level> getRespawnDimension() {
        return this.respawnDimension;
    }

    public boolean isRespawnForced() {
        return this.respawnForced;
    }

    @Deprecated // Paper
    public void setRespawnPosition(ResourceKey<Level> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage) {
        // Paper start
        this.setRespawnPosition(dimension, pos, angle, forced, sendMessage, com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause.UNKNOWN);
    }
    public void setRespawnPosition(ResourceKey<Level> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause cause) {
        Location spawnLoc = null;
        boolean willNotify = false;
        if (pos != null) {
            boolean flag2 = pos.equals(this.respawnPosition) && dimension.equals(this.respawnDimension);
            spawnLoc = net.minecraft.server.MCUtil.toLocation(this.getServer().getLevel(dimension), pos);
            spawnLoc.setYaw(angle);
            willNotify = sendMessage && !flag2;
        }
        com.destroystokyo.paper.event.player.PlayerSetSpawnEvent event = new com.destroystokyo.paper.event.player.PlayerSetSpawnEvent(this.getBukkitEntity(), cause, spawnLoc, forced, willNotify, willNotify ? net.kyori.adventure.text.Component.translatable("block.minecraft.set_spawn") : null);
        if (!event.callEvent()) {
            return;
        }
        if (event.getLocation() != null) {
            dimension = event.getLocation().getWorld() != null ? ((CraftWorld) event.getLocation().getWorld()).getHandle().dimension() : dimension;
            pos = net.minecraft.server.MCUtil.toBlockPosition(event.getLocation());
            angle = (float) event.getLocation().getYaw();
            forced = event.isForced();
            // Paper end

            if (event.willNotifyPlayer() && event.getNotification() != null) { // Paper
                this.sendMessage(PaperAdventure.asVanilla(event.getNotification()), Util.NIL_UUID); // Paper
            }

            this.respawnPosition = pos;
            this.respawnDimension = dimension;
            this.respawnAngle = angle;
            this.respawnForced = forced;
        } else {
            this.respawnPosition = null;
            this.respawnDimension = Level.OVERWORLD;
            this.respawnAngle = 0.0F;
            this.respawnForced = false;
        }

    }

    public void trackChunk(ChunkPos chunkPos, Packet<?> chunkDataPacket) {
        this.connection.send(chunkDataPacket);
        // Paper start
        if(io.papermc.paper.event.packet.PlayerChunkLoadEvent.getHandlerList().getRegisteredListeners().length > 0){
            new io.papermc.paper.event.packet.PlayerChunkLoadEvent(this.getBukkitEntity().getWorld().getChunkAt(chunkPos.longKey), this.getBukkitEntity()).callEvent();
        }
        // Paper end
    }

    public void untrackChunk(ChunkPos chunkPos) {
        if (this.isAlive()) {
            this.connection.send(new ClientboundForgetLevelChunkPacket(chunkPos.x, chunkPos.z));
            // Paper start
            if(io.papermc.paper.event.packet.PlayerChunkUnloadEvent.getHandlerList().getRegisteredListeners().length > 0){
                new io.papermc.paper.event.packet.PlayerChunkUnloadEvent(this.getBukkitEntity().getWorld().getChunkAt(chunkPos.longKey), this.getBukkitEntity()).callEvent();
            }
            // Paper end
        }

    }

    public SectionPos getLastSectionPos() {
        return this.lastSectionPos;
    }

    public void setLastSectionPos(SectionPos section) {
        this.lastSectionPos = section;
    }

    @Override
    public void playNotifySound(SoundEvent event, SoundSource category, float volume, float pitch) {
        this.connection.send(new ClientboundSoundPacket(event, category, this.getX(), this.getY(), this.getZ(), volume, pitch));
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddPlayerPacket(this);
    }

    @Override
    public ItemEntity drop(ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        ItemEntity entityitem = super.drop(stack, throwRandomly, retainOwnership);

        if (entityitem == null) {
            return null;
        } else {
            this.level.addFreshEntity(entityitem);
            ItemStack itemstack1 = entityitem.getItem();

            if (retainOwnership) {
                if (!itemstack1.isEmpty()) {
                    this.awardStat(Stats.ITEM_DROPPED.get(itemstack1.getItem()), itemstack1.getCount()); // Paper
                }

                this.awardStat(Stats.DROP);
            }

            return entityitem;
        }
    }

    public TextFilter getTextFilter() {
        return this.textFilter;
    }

    public void setLevel(ServerLevel world) {
        this.level = world;
        this.gameMode.setLevel(world);
    }

    @Nullable
    private static GameType readPlayerMode(@Nullable CompoundTag nbt, String key) {
        return nbt != null && nbt.contains(key, 99) ? GameType.byId(nbt.getInt(key)) : null;
    }

    private GameType calculateGameModeForNewPlayer(@Nullable GameType backupGameMode) {
        GameType enumgamemode1 = this.server.getForcedGameType();

        return enumgamemode1 != null ? enumgamemode1 : (backupGameMode != null ? backupGameMode : this.server.getDefaultGameType());
    }

    public void loadGameTypes(@Nullable CompoundTag nbt) {
        // Paper start
        if (this.server.getForcedGameType() != null && this.server.getForcedGameType() != ServerPlayer.readPlayerMode(nbt, "playerGameType")) {
            if (new org.bukkit.event.player.PlayerGameModeChangeEvent(this.getBukkitEntity(), org.bukkit.GameMode.getByValue(this.server.getDefaultGameType().getId()), org.bukkit.event.player.PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null).callEvent()) {
                this.gameMode.setGameModeForPlayer(this.server.getForcedGameType(), GameType.DEFAULT_MODE);
            } else {
                this.gameMode.setGameModeForPlayer(ServerPlayer.readPlayerMode(nbt,"playerGameType"), ServerPlayer.readPlayerMode(nbt, "previousPlayerGameType"));
            }
            return;
        }
        // Paper end
        this.gameMode.setGameModeForPlayer(this.calculateGameModeForNewPlayer(ServerPlayer.readPlayerMode(nbt, "playerGameType")), ServerPlayer.readPlayerMode(nbt, "previousPlayerGameType"));
    }

    private void storeGameTypes(CompoundTag nbt) {
        nbt.putInt("playerGameType", this.gameMode.getGameModeForPlayer().getId());
        GameType enumgamemode = this.gameMode.getPreviousGameModeForPlayer();

        if (enumgamemode != null) {
            nbt.putInt("previousPlayerGameType", enumgamemode.getId());
        }

    }

    public boolean isTextFilteringEnabled() {
        return this.textFilteringEnabled;
    }

    public boolean shouldFilterMessageTo(ServerPlayer player) {
        return player == this ? false : this.textFilteringEnabled || player.textFilteringEnabled;
    }

    @Override
    public boolean mayInteract(Level world, BlockPos pos) {
        return super.mayInteract(world, pos) && world.mayInteract(this, pos);
    }

    @Override
    protected void updateUsingItem(ItemStack stack) {
        CriteriaTriggers.USING_ITEM.trigger(this, stack);
        super.updateUsingItem(stack);
    }

    public boolean drop(boolean entireStack) {
        Inventory playerinventory = this.getInventory();
        ItemStack itemstack = playerinventory.removeFromSelected(entireStack);

        this.containerMenu.findSlot(playerinventory, playerinventory.selected).ifPresent((i) -> {
            this.containerMenu.setRemoteSlot(i, playerinventory.getSelected());
        });
        return this.drop(itemstack, false, true) != null;
    }

    public boolean allowsListing() {
        return this.allowsListing;
    }

    // CraftBukkit start - Add per-player time and weather.
    public long timeOffset = 0;
    public boolean relativeTime = true;

    public long getPlayerTime() {
        if (this.relativeTime) {
            // Adds timeOffset to the current server time.
            return this.level.getDayTime() + this.timeOffset;
        } else {
            // Adds timeOffset to the beginning of this day.
            return this.level.getDayTime() - (this.level.getDayTime() % 24000) + this.timeOffset;
        }
    }

    public WeatherType weather = null;

    public WeatherType getPlayerWeather() {
        return this.weather;
    }

    public void setPlayerWeather(WeatherType type, boolean plugin) {
        if (!plugin && this.weather != null) {
            return;
        }

        if (plugin) {
            this.weather = type;
        }

        if (type == WeatherType.DOWNFALL) {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0));
        } else {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
        }
    }

    private float pluginRainPosition;
    private float pluginRainPositionPrevious;

    public void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder) {
        if (this.weather == null) {
            // Vanilla
            if (oldRain != newRain) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, newRain));
            }
        } else {
            // Plugin
            if (this.pluginRainPositionPrevious != this.pluginRainPosition) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.pluginRainPosition));
            }
        }

        if (oldThunder != newThunder) {
            if (this.weather == WeatherType.DOWNFALL || this.weather == null) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, newThunder));
            } else {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0));
            }
        }
    }

    public void tickWeather() {
        if (this.weather == null) return;

        this.pluginRainPositionPrevious = this.pluginRainPosition;
        if (this.weather == WeatherType.DOWNFALL) {
            this.pluginRainPosition += 0.01;
        } else {
            this.pluginRainPosition -= 0.01;
        }

        this.pluginRainPosition = Mth.clamp(pluginRainPosition, 0.0F, 1.0F);
    }

    public void resetPlayerWeather() {
        this.weather = null;
        this.setPlayerWeather(this.level.getLevelData().isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR, false);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + this.getScoreboardName() + " at " + this.getX() + "," + this.getY() + "," + this.getZ() + ")";
    }

    // SPIGOT-1903, MC-98153
    public void forceSetPositionRotation(double x, double y, double z, float yaw, float pitch) {
        this.moveTo(x, y, z, yaw, pitch);
        this.connection.resetPosition();
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || (this.connection != null && this.connection.isDisconnected()); // Paper
    }

    @Override
    public Scoreboard getScoreboard() {
        return this.getBukkitEntity().getScoreboard().getHandle();
    }

    public void reset() {
        float exp = 0;
        boolean keepInventory = this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);

        if (this.keepLevel) { // CraftBukkit - SPIGOT-6687: Only use keepLevel (was pre-set with RULE_KEEPINVENTORY value in PlayerDeathEvent)
            exp = this.experienceProgress;
            this.newTotalExp = this.totalExperience;
            this.newLevel = this.experienceLevel;
        }

        this.setHealth(this.getMaxHealth());
        this.stopUsingItem(); // CraftBukkit - SPIGOT-6682: Clear active item on reset
        this.setAirSupply(this.getMaxAirSupply()); // Paper
        this.remainingFireTicks = 0;
        this.fallDistance = 0;
        this.foodData = new FoodData(this);
        this.experienceLevel = this.newLevel;
        this.totalExperience = this.newTotalExp;
        this.experienceProgress = 0;
        this.deathTime = 0;
        this.setArrowCount(0, true); // CraftBukkit - ArrowBodyCountChangeEvent
        this.removeAllEffects(org.bukkit.event.entity.EntityPotionEffectEvent.Cause.DEATH);
        this.effectsDirty = true;
        this.containerMenu = this.inventoryMenu;
        this.lastHurtByPlayer = null;
        this.lastHurtByMob = null;
        this.combatTracker = new CombatTracker(this);
        this.lastSentExp = -1;
        if (this.keepLevel) { // CraftBukkit - SPIGOT-6687: Only use keepLevel (was pre-set with RULE_KEEPINVENTORY value in PlayerDeathEvent)
            this.experienceProgress = exp;
        } else {
            this.giveExperiencePoints(this.newExp);
        }
        this.keepLevel = false;
    }

    @Override
    public CraftPlayer getBukkitEntity() {
        return (CraftPlayer) super.getBukkitEntity();
    }
    // CraftBukkit end

    public final int getViewDistance() { throw new UnsupportedOperationException("Use PlayerChunkLoader"); } // Paper - placeholder
}
