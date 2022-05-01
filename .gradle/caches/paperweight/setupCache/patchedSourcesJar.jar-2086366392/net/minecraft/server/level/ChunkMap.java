package net.minecraft.server.level;

import co.aikar.timings.Timing; // Paper
import com.destroystokyo.paper.PaperWorldConfig; // Paper
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.ComparisonChain; // Paper
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap; // Paper
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map; // Paper
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.UUID; // Paper
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.MCUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet; // Paper
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.bukkit.entity.Player;
// CraftBukkit end
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet; // Paper

public class ChunkMap extends ChunkStorage implements ChunkHolder.PlayerProvider {

    private static final byte CHUNK_TYPE_REPLACEABLE = -1;
    private static final byte CHUNK_TYPE_UNKNOWN = 0;
    private static final byte CHUNK_TYPE_FULL = 1;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHUNK_SAVED_PER_TICK = 200;
    private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
    private static final int EAGER_CHUNK_SAVE_COOLDOWN_IN_MILLIS = 10000;
    private static final int MIN_VIEW_DISTANCE = 3;
    public static final int MAX_VIEW_DISTANCE = 33;
    public static final int MAX_CHUNK_DISTANCE = 33 + ChunkStatus.maxDistance();
    // Paper start - Don't copy
    public final com.destroystokyo.paper.util.map.QueuedChangesMapLong2Object<ChunkHolder> updatingChunks = new com.destroystokyo.paper.util.map.QueuedChangesMapLong2Object<>();
    // Paper end - Don't copy
    public static final int FORCED_TICKET_LEVEL = 31;
    // Paper - Don't copy
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;
    public final LongSet entitiesInLevel;
    public final ServerLevel level;
    private final ThreadedLevelLightEngine lightEngine;
    public final BlockableEventLoop<Runnable> mainThreadExecutor; // Paper - public
    final java.util.concurrent.Executor mainInvokingExecutor; // Paper
    public ChunkGenerator generator;
    public final Supplier<DimensionDataStorage> overworldDataStorage;
    private final PoiManager poiManager;
    public final LongSet toDrop;
    private boolean modified;
    private final ChunkTaskPriorityQueueSorter queueSorter;
    private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> worldgenMailbox;
    public final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;
    // Paper start
    final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mailboxLight;
    public void addLightTask(ChunkHolder playerchunk, Runnable run) {
        this.mailboxLight.tell(ChunkTaskPriorityQueueSorter.message(playerchunk, run));
    }
    // Paper end
    public final ChunkProgressListener progressListener;
    private final ChunkStatusUpdateListener chunkStatusListener;
    public final ChunkMap.ChunkDistanceManager distanceManager;
    private final AtomicInteger tickingGenerated;
    public final StructureManager structureManager;
    private final String storageName;
    private final PlayerMap playerMap;
    public final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
    private final Long2ByteMap chunkTypeCache;
    private final Long2LongMap chunkSaveCooldowns;
    private final Queue<Runnable> unloadQueue;
    int viewDistance;
    public final com.destroystokyo.paper.util.misc.PlayerAreaMap playerMobDistanceMap; // Paper
    public final ReferenceOpenHashSet<ChunkHolder> needsChangeBroadcasting = new ReferenceOpenHashSet<>();

    // Paper start - optimise checkDespawn
    public static final int GENERAL_AREA_MAP_SQUARE_RADIUS = 40;
    public static final double GENERAL_AREA_MAP_ACCEPTABLE_SEARCH_RANGE = 16.0 * (GENERAL_AREA_MAP_SQUARE_RADIUS - 1);
    public static final double GENERAL_AREA_MAP_ACCEPTABLE_SEARCH_RANGE_SQUARED = GENERAL_AREA_MAP_ACCEPTABLE_SEARCH_RANGE * GENERAL_AREA_MAP_ACCEPTABLE_SEARCH_RANGE;
    public final com.destroystokyo.paper.util.misc.PlayerAreaMap playerGeneralAreaMap;
    // Paper end - optimise checkDespawn

    // CraftBukkit start - recursion-safe executor for Chunk loadCallback() and unloadCallback()
    public final CallbackExecutor callbackExecutor = new CallbackExecutor();
    public static final class CallbackExecutor implements java.util.concurrent.Executor, Runnable {

        private Runnable queued; // Paper - revert CB changes

        @Override
        public void execute(Runnable runnable) {
            // Paper start - revert CB changes
            org.spigotmc.AsyncCatcher.catchOp("Callback Executor execute");
            if (this.queued != null) {
                LOGGER.error("Failed to schedule runnable", new IllegalStateException("Already queued"));
                throw new IllegalStateException("Already queued");
            }
            this.queued = runnable;
            // Paper end - revert CB changes
        }

        @Override
        public void run() {
            // Paper start - revert CB changes
            org.spigotmc.AsyncCatcher.catchOp("Callback Executor execute");
            Runnable task = this.queued;
            if (task != null) {
                this.queued = null;
                // Paper end - revert CB changes
                task.run();
            }
        }
    };
    // CraftBukkit end

    final CallbackExecutor chunkLoadConversionCallbackExecutor = new CallbackExecutor(); // Paper
    // Paper start - distance maps
    private final com.destroystokyo.paper.util.misc.PooledLinkedHashSets<ServerPlayer> pooledLinkedPlayerHashSets = new com.destroystokyo.paper.util.misc.PooledLinkedHashSets<>();
    // Paper start - optimise ChunkMap#anyPlayerCloseEnoughForSpawning
    // A note about the naming used here:
    // Previously, mojang used a "spawn range" of 8 for controlling both ticking and
    // mob spawn range. However, spigot makes the spawn range configurable by
    // checking if the chunk is in the tick range (8) and the spawn range
    // obviously this means a spawn range > 8 cannot be implemented

    // these maps are named after spigot's uses
    public final com.destroystokyo.paper.util.misc.PlayerAreaMap playerMobSpawnMap; // this map is absent from updateMaps since it's controlled at the start of the chunkproviderserver tick
    public final com.destroystokyo.paper.util.misc.PlayerAreaMap playerChunkTickRangeMap;
    // Paper end - optimise ChunkMap#anyPlayerCloseEnoughForSpawning
    public final io.papermc.paper.chunk.PlayerChunkLoader playerChunkManager = new io.papermc.paper.chunk.PlayerChunkLoader(this, this.pooledLinkedPlayerHashSets); // Paper - replace chunk loader
    // Paper start - use distance map to optimise tracker
    public static boolean isLegacyTrackingEntity(Entity entity) {
        return entity.isLegacyTrackingEntity;
    }

    // inlined EnumMap, TrackingRange.TrackingRangeType
    static final org.spigotmc.TrackingRange.TrackingRangeType[] TRACKING_RANGE_TYPES = org.spigotmc.TrackingRange.TrackingRangeType.values();
    public final com.destroystokyo.paper.util.misc.PlayerAreaMap[] playerEntityTrackerTrackMaps;
    final int[] entityTrackerTrackRanges;
    public final int getEntityTrackerRange(final int ordinal) {
        return this.entityTrackerTrackRanges[ordinal];
    }

    private int convertSpigotRangeToVanilla(final int vanilla) {
        return MinecraftServer.getServer().getScaledTrackingDistance(vanilla);
    }
    // Paper end - use distance map to optimise tracker

    void addPlayerToDistanceMaps(ServerPlayer player) {
        this.playerChunkManager.addPlayer(player); // Paper - replace chunk loader
        int chunkX = MCUtil.getChunkCoordinate(player.getX());
        int chunkZ = MCUtil.getChunkCoordinate(player.getZ());
        // Paper start - use distance map to optimise entity tracker
        for (int i = 0, len = TRACKING_RANGE_TYPES.length; i < len; ++i) {
            com.destroystokyo.paper.util.misc.PlayerAreaMap trackMap = this.playerEntityTrackerTrackMaps[i];
            int trackRange = this.entityTrackerTrackRanges[i];

            trackMap.add(player, chunkX, chunkZ, Math.min(trackRange, io.papermc.paper.chunk.PlayerChunkLoader.getSendViewDistance(player))); // Paper - per player view distances
        }
        // Paper end - use distance map to optimise entity tracker
        // Note: players need to be explicitly added to distance maps before they can be updated
        this.playerChunkTickRangeMap.add(player, chunkX, chunkZ, DistanceManager.MOB_SPAWN_RANGE); // Paper - optimise ChunkMap#anyPlayerCloseEnoughForSpawning
        this.playerGeneralAreaMap.add(player, chunkX, chunkZ, GENERAL_AREA_MAP_SQUARE_RADIUS); // Paper - optimise checkDespawn
        // Paper start - per player mob spawning
        if (this.playerMobDistanceMap != null) {
            this.playerMobDistanceMap.add(player, chunkX, chunkZ, this.distanceManager.getSimulationDistance());
        }
        // Paper end - per player mob spawning
    }

    void removePlayerFromDistanceMaps(ServerPlayer player) {

        // Paper start - use distance map to optimise tracker
        for (int i = 0, len = TRACKING_RANGE_TYPES.length; i < len; ++i) {
            this.playerEntityTrackerTrackMaps[i].remove(player);
        }
        // Paper end - use distance map to optimise tracker
        // Paper start - optimise ChunkMap#anyPlayerCloseEnoughForSpawning
        this.playerMobSpawnMap.remove(player);
        this.playerChunkTickRangeMap.remove(player);
        // Paper end - optimise ChunkMap#anyPlayerCloseEnoughForSpawning
        this.playerGeneralAreaMap.remove(player); // Paper - optimise checkDespawns
        // Paper start - per player mob spawning
        if (this.playerMobDistanceMap != null) {
            this.playerMobDistanceMap.remove(player);
        }
        // Paper end - per player mob spawning
        this.playerChunkManager.removePlayer(player); // Paper - replace chunk loader
    }

    void updateMaps(ServerPlayer player) {
        int chunkX = MCUtil.getChunkCoordinate(player.getX());
        int chunkZ = MCUtil.getChunkCoordinate(player.getZ());
        // Note: players need to be explicitly added to distance maps before they can be updated
        // Paper start - use distance map to optimise entity tracker
        for (int i = 0, len = TRACKING_RANGE_TYPES.length; i < len; ++i) {
            com.destroystokyo.paper.util.misc.PlayerAreaMap trackMap = this.playerEntityTrackerTrackMaps[i];
            int trackRange = this.entityTrackerTrackRanges[i];

            trackMap.update(player, chunkX, chunkZ, Math.min(trackRange, io.papermc.paper.chunk.PlayerChunkLoader.getSendViewDistance(player))); // Paper - per player view distances
        }
        // Paper end - use distance map to optimise entity tracker
        this.playerChunkTickRangeMap.update(player, chunkX, chunkZ, DistanceManager.MOB_SPAWN_RANGE); // Paper - optimise ChunkMap#anyPlayerCloseEnoughForSpawning
        this.playerGeneralAreaMap.update(player, chunkX, chunkZ, GENERAL_AREA_MAP_SQUARE_RADIUS); // Paper - optimise checkDespawn
        // Paper start - per player mob spawning
        if (this.playerMobDistanceMap != null) {
            this.playerMobDistanceMap.update(player, chunkX, chunkZ, this.distanceManager.getSimulationDistance());
        }
        // Paper end - per player mob spawning
        this.playerChunkManager.updatePlayer(player); // Paper - replace chunk loader
    }
    // Paper end
    // Paper start
    public final List<io.papermc.paper.chunk.SingleThreadChunkRegionManager> regionManagers = new java.util.ArrayList<>();
    public final io.papermc.paper.chunk.SingleThreadChunkRegionManager dataRegionManager;

    public static final class DataRegionData implements io.papermc.paper.chunk.SingleThreadChunkRegionManager.RegionData {
        // Paper start - optimise notify()
        private io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<Mob> navigators;

        public io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<Mob> getNavigators() {
            return this.navigators;
        }

        public boolean addToNavigators(final Mob navigator) {
            if (this.navigators == null) {
                this.navigators = new io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<>();
            }
            return this.navigators.add(navigator);
        }

        public boolean removeFromNavigators(final Mob navigator) {
            if (this.navigators == null) {
                return false;
            }
            return this.navigators.remove(navigator);
        }
        // Paper end - optimise notify()
    }

    public static final class DataRegionSectionData implements io.papermc.paper.chunk.SingleThreadChunkRegionManager.RegionSectionData {

        // Paper start - optimise notify()
        private io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<Mob> navigators;

        public io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<Mob> getNavigators() {
            return this.navigators;
        }

        public boolean addToNavigators(final io.papermc.paper.chunk.SingleThreadChunkRegionManager.RegionSection section, final Mob navigator) {
            if (this.navigators == null) {
                this.navigators = new io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<>();
            }
            final boolean ret = this.navigators.add(navigator);
            if (ret) {
                final DataRegionData data = (DataRegionData)section.getRegion().regionData;
                if (!data.addToNavigators(navigator)) {
                    throw new IllegalStateException();
                }
            }
            return ret;
        }

        public boolean removeFromNavigators(final io.papermc.paper.chunk.SingleThreadChunkRegionManager.RegionSection section, final Mob navigator) {
            if (this.navigators == null) {
                return false;
            }
            final boolean ret = this.navigators.remove(navigator);
            if (ret) {
                final DataRegionData data = (DataRegionData)section.getRegion().regionData;
                if (!data.removeFromNavigators(navigator)) {
                    throw new IllegalStateException();
                }
            }
            return ret;
        }
        // Paper end - optimise notify()

        @Override
        public void removeFromRegion(final io.papermc.paper.chunk.SingleThreadChunkRegionManager.RegionSection section,
                                     final io.papermc.paper.chunk.SingleThreadChunkRegionManager.Region from) {
            final DataRegionSectionData sectionData = (DataRegionSectionData)section.sectionData;
            final DataRegionData fromData = (DataRegionData)from.regionData;
            // Paper start - optimise notify()
            if (sectionData.navigators != null) {
                for (final Iterator<Mob> iterator = sectionData.navigators.unsafeIterator(io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet.ITERATOR_FLAG_SEE_ADDITIONS); iterator.hasNext();) {
                    if (!fromData.removeFromNavigators(iterator.next())) {
                        throw new IllegalStateException();
                    }
                }
            }
            // Paper end - optimise notify()
        }

        @Override
        public void addToRegion(final io.papermc.paper.chunk.SingleThreadChunkRegionManager.RegionSection section,
                                final io.papermc.paper.chunk.SingleThreadChunkRegionManager.Region oldRegion,
                                final io.papermc.paper.chunk.SingleThreadChunkRegionManager.Region newRegion) {
            final DataRegionSectionData sectionData = (DataRegionSectionData)section.sectionData;
            final DataRegionData oldRegionData = oldRegion == null ? null : (DataRegionData)oldRegion.regionData;
            final DataRegionData newRegionData = (DataRegionData)newRegion.regionData;
            // Paper start - optimise notify()
            if (sectionData.navigators != null) {
                for (final Iterator<Mob> iterator = sectionData.navigators.unsafeIterator(io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet.ITERATOR_FLAG_SEE_ADDITIONS); iterator.hasNext();) {
                    if (!newRegionData.addToNavigators(iterator.next())) {
                        throw new IllegalStateException();
                    }
                }
            }
            // Paper end - optimise notify()
        }
    }

    public final ChunkHolder getUnloadingChunkHolder(int chunkX, int chunkZ) {
        return this.pendingUnloads.get(io.papermc.paper.util.CoordinateUtils.getChunkKey(chunkX, chunkZ));
    }
    // Paper end

    boolean unloadingPlayerChunk = false; // Paper - do not allow ticket level changes while unloading chunks
    public ChunkMap(ServerLevel world, LevelStorageSource.LevelStorageAccess session, DataFixer dataFixer, StructureManager structureManager, Executor executor, BlockableEventLoop<Runnable> mainThreadExecutor, LightChunkGetter chunkProvider, ChunkGenerator chunkGenerator, ChunkProgressListener worldGenerationProgressListener, ChunkStatusUpdateListener chunkStatusChangeListener, Supplier<DimensionDataStorage> persistentStateManagerFactory, int viewDistance, boolean dsync) {
        super(session.getDimensionPath(world.dimension()).resolve("region"), dataFixer, dsync);
        // Paper - don't copy
        this.pendingUnloads = new Long2ObjectLinkedOpenHashMap();
        this.entitiesInLevel = new LongOpenHashSet();
        this.toDrop = new LongOpenHashSet();
        this.tickingGenerated = new AtomicInteger();
        this.playerMap = new PlayerMap();
        this.entityMap = new Int2ObjectOpenHashMap();
        this.chunkTypeCache = new Long2ByteOpenHashMap();
        this.chunkSaveCooldowns = new Long2LongOpenHashMap();
        this.structureManager = structureManager;
        this.unloadQueue = new com.destroystokyo.paper.utils.CachedSizeConcurrentLinkedQueue<>(); // Paper - need constant-time size()
        Path path = session.getDimensionPath(world.dimension());

        this.storageName = path.getFileName().toString();
        this.level = world;
        this.generator = chunkGenerator;
        this.mainThreadExecutor = mainThreadExecutor;
        // Paper start
        this.mainInvokingExecutor = (run) -> {
            if (MCUtil.isMainThread()) {
                run.run();
            } else {
                mainThreadExecutor.execute(run);
            }
        };
        // Paper end
        ProcessorMailbox<Runnable> threadedmailbox = ProcessorMailbox.create(executor, "worldgen");

        Objects.requireNonNull(mainThreadExecutor);
        ProcessorHandle<Runnable> mailbox = ProcessorHandle.of("main", mainThreadExecutor::tell);

        this.progressListener = worldGenerationProgressListener;
        this.chunkStatusListener = chunkStatusChangeListener;
        ProcessorMailbox<Runnable> lightthreaded; ProcessorMailbox<Runnable> threadedmailbox1 = lightthreaded = ProcessorMailbox.create(executor, "light"); // Paper

        this.queueSorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(threadedmailbox, mailbox, threadedmailbox1), executor, Integer.MAX_VALUE);
        this.worldgenMailbox = this.queueSorter.getProcessor(threadedmailbox, false);
        this.mainThreadMailbox = this.queueSorter.getProcessor(mailbox, false);
        this.mailboxLight = this.queueSorter.getProcessor(lightthreaded, false);// Paper
        this.lightEngine = new ThreadedLevelLightEngine(chunkProvider, this, this.level.dimensionType().hasSkyLight(), threadedmailbox1, this.queueSorter.getProcessor(threadedmailbox1, false));
        this.distanceManager = new ChunkMap.ChunkDistanceManager(executor, mainThreadExecutor);
        this.overworldDataStorage = persistentStateManagerFactory;
        this.poiManager = new PoiManager(path.resolve("poi"), dataFixer, dsync, world);
        this.setViewDistance(viewDistance);
        // Paper start
        this.dataRegionManager = new io.papermc.paper.chunk.SingleThreadChunkRegionManager(this.level, 2, (1.0 / 3.0), 1, 6, "Data", DataRegionData::new, DataRegionSectionData::new);
        this.regionManagers.add(this.dataRegionManager);
        // Paper end
        this.playerMobDistanceMap = this.level.paperConfig.perPlayerMobSpawns ? new com.destroystokyo.paper.util.misc.PlayerAreaMap(this.pooledLinkedPlayerHashSets) : null; // Paper
        // Paper start - use distance map to optimise entity tracker
        this.playerEntityTrackerTrackMaps = new com.destroystokyo.paper.util.misc.PlayerAreaMap[TRACKING_RANGE_TYPES.length];
        this.entityTrackerTrackRanges = new int[TRACKING_RANGE_TYPES.length];

        org.spigotmc.SpigotWorldConfig spigotWorldConfig = this.level.spigotConfig;

        for (int ordinal = 0, len = TRACKING_RANGE_TYPES.length; ordinal < len; ++ordinal) {
            org.spigotmc.TrackingRange.TrackingRangeType trackingRangeType = TRACKING_RANGE_TYPES[ordinal];
            int configuredSpigotValue;
            switch (trackingRangeType) {
                case PLAYER:
                    configuredSpigotValue = spigotWorldConfig.playerTrackingRange;
                    break;
                case ANIMAL:
                    configuredSpigotValue = spigotWorldConfig.animalTrackingRange;
                    break;
                case MONSTER:
                    configuredSpigotValue = spigotWorldConfig.monsterTrackingRange;
                    break;
                case MISC:
                    configuredSpigotValue = spigotWorldConfig.miscTrackingRange;
                    break;
                case OTHER:
                    configuredSpigotValue = spigotWorldConfig.otherTrackingRange;
                    break;
                case ENDERDRAGON:
                    configuredSpigotValue = EntityType.ENDER_DRAGON.clientTrackingRange() * 16;
                    break;
                default:
                    throw new IllegalStateException("Missing case for enum " + trackingRangeType);
            }
            configuredSpigotValue = convertSpigotRangeToVanilla(configuredSpigotValue);

            int trackRange = (configuredSpigotValue >>> 4) + ((configuredSpigotValue & 15) != 0 ? 1 : 0);
            this.entityTrackerTrackRanges[ordinal] = trackRange;

            this.playerEntityTrackerTrackMaps[ordinal] = new com.destroystokyo.paper.util.misc.PlayerAreaMap(this.pooledLinkedPlayerHashSets);
        }
        // Paper end - use distance map to optimise entity tracker
        // Paper start - optimise ChunkMap#anyPlayerCloseEnoughForSpawning
        this.playerChunkTickRangeMap = new com.destroystokyo.paper.util.misc.PlayerAreaMap(this.pooledLinkedPlayerHashSets,
            (ServerPlayer player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
             com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> newState) -> {
                ChunkHolder playerChunk = ChunkMap.this.getUpdatingChunkIfPresent(MCUtil.getCoordinateKey(rangeX, rangeZ));
                if (playerChunk != null) {
                    playerChunk.playersInChunkTickRange = newState;
                }
            },
            (ServerPlayer player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
             com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> newState) -> {
                ChunkHolder playerChunk = ChunkMap.this.getUpdatingChunkIfPresent(MCUtil.getCoordinateKey(rangeX, rangeZ));
                if (playerChunk != null) {
                    playerChunk.playersInChunkTickRange = newState;
                }
            });
        this.playerMobSpawnMap = new com.destroystokyo.paper.util.misc.PlayerAreaMap(this.pooledLinkedPlayerHashSets,
            (ServerPlayer player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
             com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> newState) -> {
                ChunkHolder playerChunk = ChunkMap.this.getUpdatingChunkIfPresent(MCUtil.getCoordinateKey(rangeX, rangeZ));
                if (playerChunk != null) {
                    playerChunk.playersInMobSpawnRange = newState;
                }
            },
            (ServerPlayer player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
             com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> newState) -> {
                ChunkHolder playerChunk = ChunkMap.this.getUpdatingChunkIfPresent(MCUtil.getCoordinateKey(rangeX, rangeZ));
                if (playerChunk != null) {
                    playerChunk.playersInMobSpawnRange = newState;
                }
            });
        // Paper end - optimise ChunkMap#anyPlayerCloseEnoughForSpawning
        // Paper start - optimise checkDespawn
        this.playerGeneralAreaMap = new com.destroystokyo.paper.util.misc.PlayerAreaMap(this.pooledLinkedPlayerHashSets,
            (ServerPlayer player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
             com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> newState) -> {
                LevelChunk chunk = ChunkMap.this.level.getChunkSource().getChunkAtIfCachedImmediately(rangeX, rangeZ);
                if (chunk != null) {
                    chunk.updateGeneralAreaCache(newState);
                }
            },
            (ServerPlayer player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
             com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> newState) -> {
                LevelChunk chunk = ChunkMap.this.level.getChunkSource().getChunkAtIfCachedImmediately(rangeX, rangeZ);
                if (chunk != null) {
                    chunk.updateGeneralAreaCache(newState);
                }
            });
        // Paper end - optimise checkDespawn
    }

    protected ChunkGenerator generator() {
        return this.generator;
    }

    public void debugReloadGenerator() {
        DataResult<JsonElement> dataresult = ChunkGenerator.CODEC.encodeStart(JsonOps.INSTANCE, this.generator);
        DataResult<ChunkGenerator> dataresult1 = dataresult.flatMap((jsonelement) -> {
            return ChunkGenerator.CODEC.parse(JsonOps.INSTANCE, jsonelement);
        });

        dataresult1.result().ifPresent((chunkgenerator) -> {
            this.generator = chunkgenerator;
        });
    }

    // Paper start - Chunk Prioritization
    public void queueHolderUpdate(ChunkHolder playerchunk) {
        Runnable runnable = () -> {
            if (isUnloading(playerchunk)) {
                return; // unloaded
            }
            distanceManager.pendingChunkUpdates.add(playerchunk);
            if (!distanceManager.pollingPendingChunkUpdates) {
                level.getChunkSource().runDistanceManagerUpdates();
            }
        };
        if (MCUtil.isMainThread()) {
            // We can't use executor here because it will not execute tasks if its currently in the middle of executing tasks...
            runnable.run();
        } else {
            mainThreadExecutor.execute(runnable);
        }
    }

    private boolean isUnloading(ChunkHolder playerchunk) {
        return playerchunk == null || toDrop.contains(playerchunk.pos.toLong());
    }

    private void updateChunkPriorityMap(it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap map, long chunk, int level) {
        int prev = map.getOrDefault(chunk, -1);
        if (level > prev) {
            map.put(chunk, level);
        }
    }
    // Paper end

    // Paper start
    public void updatePlayerMobTypeMap(Entity entity) {
        if (!this.level.paperConfig.perPlayerMobSpawns) {
            return;
        }
        int index = entity.getType().getCategory().ordinal();

        final com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> inRange = this.playerMobDistanceMap.getObjectsInRange(entity.chunkPosition());
        if (inRange == null) {
            return;
        }
        final Object[] backingSet = inRange.getBackingSet();
        for (int i = 0; i < backingSet.length; i++) {
            if (!(backingSet[i] instanceof final ServerPlayer player)) {
                continue;
            }
            ++player.mobCounts[index];
        }
    }

    public int getMobCountNear(ServerPlayer entityPlayer, net.minecraft.world.entity.MobCategory mobCategory) {
        return entityPlayer.mobCounts[mobCategory.ordinal()];
    }
    // Paper end

    private static double euclideanDistanceSquared(ChunkPos pos, Entity entity) {
        double d0 = (double) SectionPos.sectionToBlockCoord(pos.x, 8);
        double d1 = (double) SectionPos.sectionToBlockCoord(pos.z, 8);
        double d2 = d0 - entity.getX();
        double d3 = d1 - entity.getZ();

        return d2 * d2 + d3 * d3;
    }

    public static boolean isChunkInRange(int x1, int z1, int x2, int z2, int distance) {
        int j1 = Math.max(0, Math.abs(x1 - x2) - 1);
        int k1 = Math.max(0, Math.abs(z1 - z2) - 1);
        long l1 = (long) Math.max(0, Math.max(j1, k1) - 1);
        long i2 = (long) Math.min(j1, k1);
        long j2 = i2 * i2 + l1 * l1;
        int k2 = distance - 1;
        int l2 = k2 * k2;

        return j2 <= (long) l2;
    }

    private static boolean isChunkOnRangeBorder(int x1, int z1, int x2, int z2, int distance) {
        return !ChunkMap.isChunkInRange(x1, z1, x2, z2, distance) ? false : (!ChunkMap.isChunkInRange(x1 + 1, z1, x2, z2, distance) ? true : (!ChunkMap.isChunkInRange(x1, z1 + 1, x2, z2, distance) ? true : (!ChunkMap.isChunkInRange(x1 - 1, z1, x2, z2, distance) ? true : !ChunkMap.isChunkInRange(x1, z1 - 1, x2, z2, distance))));
    }

    protected ThreadedLevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    @Nullable
    public ChunkHolder getUpdatingChunkIfPresent(long pos) {
        return this.updatingChunks.getUpdating(pos); // Paper - Don't copy
    }

    @Nullable
    public ChunkHolder getVisibleChunkIfPresent(long pos) {
        // Paper start - Don't copy
        if (Thread.currentThread() == this.level.thread) {
            return this.updatingChunks.getVisible(pos);
        }
        return this.updatingChunks.getVisibleAsync(pos);
        // Paper end - Don't copy
    }

    protected IntSupplier getChunkQueueLevel(long pos) {
        return () -> {
            ChunkHolder playerchunk = this.getVisibleChunkIfPresent(pos);

            return playerchunk == null ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1 : Math.min(playerchunk.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
        };
    }

    public String getChunkDebugData(ChunkPos chunkPos) {
        ChunkHolder playerchunk = this.getVisibleChunkIfPresent(chunkPos.toLong());

        if (playerchunk == null) {
            return "null";
        } else {
            String s = playerchunk.getTicketLevel() + "\n";
            ChunkStatus chunkstatus = playerchunk.getLastAvailableStatus();
            ChunkAccess ichunkaccess = playerchunk.getLastAvailable();

            if (chunkstatus != null) {
                s = s + "St: \u00a7" + chunkstatus.getIndex() + chunkstatus + "\u00a7r\n";
            }

            if (ichunkaccess != null) {
                s = s + "Ch: \u00a7" + ichunkaccess.getStatus().getIndex() + ichunkaccess.getStatus() + "\u00a7r\n";
            }

            ChunkHolder.FullChunkStatus playerchunk_state = playerchunk.getFullStatus();

            s = s + "\u00a7" + playerchunk_state.ordinal() + playerchunk_state;
            return s + "\u00a7r";
        }
    }

    // Paper start
    public final int getEffectiveViewDistance() {
        // TODO this needs to be checked on update
        // Mojang currently sets it to +1 of the configured view distance. So subtract one to get the one we really want.
        return this.viewDistance - 1;
    }
    // Paper end

    private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(ChunkPos centerChunk, int margin, IntFunction<ChunkStatus> distanceToStatus) {
        List<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> list = new ArrayList();
        List<ChunkHolder> list1 = new ArrayList();
        int j = centerChunk.x;
        int k = centerChunk.z;
        ChunkHolder requestingNeighbor = getUpdatingChunkIfPresent(centerChunk.toLong()); // Paper

        for (int l = -margin; l <= margin; ++l) {
            for (int i1 = -margin; i1 <= margin; ++i1) {
                int j1 = Math.max(Math.abs(i1), Math.abs(l));
                final ChunkPos chunkcoordintpair1 = new ChunkPos(j + i1, k + l);
                long k1 = chunkcoordintpair1.toLong();
                ChunkHolder playerchunk = this.getUpdatingChunkIfPresent(k1);

                if (playerchunk == null) {
                    return CompletableFuture.completedFuture(Either.right(new ChunkHolder.ChunkLoadingFailure() {
                        public String toString() {
                            return "Unloaded " + chunkcoordintpair1;
                        }
                    }));
                }

                ChunkStatus chunkstatus = (ChunkStatus) distanceToStatus.apply(j1);
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = playerchunk.getOrScheduleFuture(chunkstatus, this);
                // Paper start
                if (requestingNeighbor != null && requestingNeighbor != playerchunk && !completablefuture.isDone()) {
                    requestingNeighbor.onNeighborRequest(playerchunk, chunkstatus);
                    completablefuture.thenAccept(either -> {
                        requestingNeighbor.onNeighborDone(playerchunk, chunkstatus, either.left().orElse(null));
                    });
                }
                // Paper end

                list1.add(playerchunk);
                list.add(completablefuture);
            }
        }

        CompletableFuture<List<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completablefuture1 = Util.sequence(list);
        CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completablefuture2 = completablefuture1.thenApply((list2) -> {
            List<ChunkAccess> list3 = Lists.newArrayList();
            // CraftBukkit start - decompile error
            int cnt = 0;

            for (Iterator iterator = list2.iterator(); iterator.hasNext(); ++cnt) {
                final int l1 = cnt;
                // CraftBukkit end
                final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = (Either) iterator.next();

                if (either == null) {
                    throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
                }

                Optional<ChunkAccess> optional = either.left();

                if (!optional.isPresent()) {
                    return Either.right(new ChunkHolder.ChunkLoadingFailure() {
                        public String toString() {
                            ChunkPos chunkcoordintpair2 = new ChunkPos(j + l1 % (margin * 2 + 1), k + l1 / (margin * 2 + 1));

                            return "Unloaded " + chunkcoordintpair2 + " " + either.right().get();
                        }
                    });
                }

                list3.add((ChunkAccess) optional.get());
            }

            return Either.left(list3);
        });
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            ChunkHolder playerchunk1 = (ChunkHolder) iterator.next();

            playerchunk1.addSaveDependency("getChunkRangeFuture " + centerChunk + " " + margin, completablefuture2);
        }

        return completablefuture2;
    }

    public ReportedException debugFuturesAndCreateReportedException(IllegalStateException exception, String s) {
        StringBuilder stringbuilder = new StringBuilder();
        Consumer<ChunkHolder> consumer = (playerchunk) -> {
            playerchunk.getAllFutures().forEach((pair) -> {
                ChunkStatus chunkstatus = (ChunkStatus) pair.getFirst();
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = (CompletableFuture) pair.getSecond();

                if (completablefuture != null && completablefuture.isDone() && completablefuture.join() == null) {
                    stringbuilder.append(playerchunk.getPos()).append(" - status: ").append(chunkstatus).append(" future: ").append(completablefuture).append(System.lineSeparator());
                }

            });
        };

        stringbuilder.append("Updating:").append(System.lineSeparator());
        this.updatingChunks.getUpdatingValuesCopy().forEach(consumer); // Paper
        stringbuilder.append("Visible:").append(System.lineSeparator());
        this.updatingChunks.getVisibleValuesCopy().forEach(consumer); // Paper
        CrashReport crashreport = CrashReport.forThrowable(exception, "Chunk loading");
        CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Chunk loading");

        crashreportsystemdetails.setDetail("Details", (Object) s);
        crashreportsystemdetails.setDetail("Futures", (Object) stringbuilder);
        return new ReportedException(crashreport);
    }

    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareEntityTickingChunk(ChunkPos pos) {
        return this.getChunkRangeFuture(pos, 2, (i) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((either) -> {
            return either.mapLeft((list) -> {
                return (LevelChunk) list.get(list.size() / 2);
            });
        }, this.mainInvokingExecutor); // Paper
    }

    @Nullable
    ChunkHolder updateChunkScheduling(long pos, int level, @Nullable ChunkHolder holder, int k) {
        if (this.unloadingPlayerChunk) { net.minecraft.server.MinecraftServer.LOGGER.error("Cannot tick distance manager while unloading playerchunks", new Throwable()); throw new IllegalStateException("Cannot tick distance manager while unloading playerchunks"); } // Paper
        if (k > ChunkMap.MAX_CHUNK_DISTANCE && level > ChunkMap.MAX_CHUNK_DISTANCE) {
            return holder;
        } else {
            if (holder != null) {
                holder.setTicketLevel(level);
            }

            if (holder != null) {
                if (level > ChunkMap.MAX_CHUNK_DISTANCE) {
                    this.toDrop.add(pos);
                } else {
                    this.toDrop.remove(pos);
                }
            }

            if (level <= ChunkMap.MAX_CHUNK_DISTANCE && holder == null) {
                holder = (ChunkHolder) this.pendingUnloads.remove(pos);
                if (holder != null) {
                    holder.setTicketLevel(level);
                    holder.onChunkAdd(); // Paper - optimise anyPlayerCloseEnoughForSpawning - PUT HERE AFTER RE-ADDING ONLY
                } else {
                    holder = new ChunkHolder(new ChunkPos(pos), level, this.level, this.lightEngine, this.queueSorter, this);
                    // Paper start
                    for (int index = 0, len = this.regionManagers.size(); index < len; ++index) {
                        this.regionManagers.get(index).addChunk(holder.pos.x, holder.pos.z);
                    }
                    // Paper end
                }
                this.getPoiManager().dequeueUnload(holder.pos.longKey); // Paper - unload POI data

                this.updatingChunks.queueUpdate(pos, holder); // Paper - Don't copy
                this.modified = true;
            }

            return holder;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.queueSorter.close();
            this.level.asyncChunkTaskManager.close(true); // Paper - Required since we're closing regionfiles in the next line
            this.poiManager.close();
        } finally {
            super.close();
        }

    }

    // Paper start - incremental autosave
    final ObjectRBTreeSet<ChunkHolder> autoSaveQueue = new ObjectRBTreeSet<>((playerchunk1, playerchunk2) -> {
        int timeCompare =  Long.compare(playerchunk1.lastAutoSaveTime, playerchunk2.lastAutoSaveTime);
        if (timeCompare != 0) {
            return timeCompare;
        }

        return Long.compare(MCUtil.getCoordinateKey(playerchunk1.pos), MCUtil.getCoordinateKey(playerchunk2.pos));
    });

    protected void saveIncrementally() {
        int savedThisTick = 0;
        // optimized since we search far less chunks to hit ones that need to be saved
        List<ChunkHolder> reschedule = new java.util.ArrayList<>(this.level.paperConfig.maxAutoSaveChunksPerTick);
        long currentTick = this.level.getGameTime();
        long maxSaveTime = currentTick - this.level.paperConfig.autoSavePeriod;

        for (Iterator<ChunkHolder> iterator = this.autoSaveQueue.iterator(); iterator.hasNext();) {
            ChunkHolder playerchunk = iterator.next();
            if (playerchunk.lastAutoSaveTime > maxSaveTime) {
                break;
            }

            iterator.remove();

            ChunkAccess ichunkaccess = playerchunk.getChunkToSave().getNow(null);
            if (ichunkaccess instanceof LevelChunk) {
                boolean shouldSave = ((LevelChunk)ichunkaccess).lastSaveTime <= maxSaveTime;

                if (shouldSave && this.save(ichunkaccess) && this.level.entityManager.storeChunkSections(playerchunk.pos.toLong(), entity -> {})) {
                    ++savedThisTick;

                    if (!playerchunk.setHasBeenLoaded()) {
                        // do not fall through to reschedule logic
                        playerchunk.inactiveTimeStart = currentTick;
                        if (savedThisTick >= this.level.paperConfig.maxAutoSaveChunksPerTick) {
                            break;
                        }
                        continue;
                    }
                }
            }

            reschedule.add(playerchunk);

            if (savedThisTick >= this.level.paperConfig.maxAutoSaveChunksPerTick) {
                break;
            }
        }

        for (int i = 0, len = reschedule.size(); i < len; ++i) {
            ChunkHolder playerchunk = reschedule.get(i);
            playerchunk.lastAutoSaveTime = this.level.getGameTime();
            this.autoSaveQueue.add(playerchunk);
        }
    }
    // Paper end

    protected void saveAllChunks(boolean flush) {
        // Paper start - do not overload I/O threads with too much work when saving
        int[] saved = new int[1];
        int maxAsyncSaves = 50;
        Runnable onChunkSave = () -> {
            if (++saved[0] >= maxAsyncSaves) {
                saved[0] = 0;
                com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE.flush();
            }
        };
        // Paper end - do not overload I/O threads with too much work when saving
        if (flush) {
            List<ChunkHolder> list = (List) this.updatingChunks.getVisibleValuesCopy().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).peek(ChunkHolder::refreshAccessibility).collect(Collectors.toList()); // Paper
            MutableBoolean mutableboolean = new MutableBoolean();

            do {
                boolean isShuttingDown = level.getServer().hasStopped(); // Paper
                mutableboolean.setFalse();
                list.stream().map((playerchunk) -> {
                    CompletableFuture completablefuture;

                    do {
                        completablefuture = playerchunk.getChunkToSave();
                        BlockableEventLoop iasynctaskhandler = this.mainThreadExecutor;

                        Objects.requireNonNull(completablefuture);
                        iasynctaskhandler.managedBlock(completablefuture::isDone);
                    } while (completablefuture != playerchunk.getChunkToSave());

                    return (ChunkAccess) completablefuture.join();
                }).filter((ichunkaccess) -> {
                    return ichunkaccess instanceof ImposterProtoChunk || ichunkaccess instanceof LevelChunk;
                }).filter(this::save).forEach((ichunkaccess) -> {
                    onChunkSave.run(); // Paper - do not overload I/O threads with too much work when saving
                    mutableboolean.setTrue();
                });
            } while (mutableboolean.isTrue());

            this.processUnloads(() -> {
                return true;
            });
            //this.flushWorker(); // Paper - nuke IOWorker
            this.level.asyncChunkTaskManager.flush(); // Paper - flush to preserve behavior compat with pre-async behaviour
        } else {
            this.updatingChunks.getVisibleValuesCopy().forEach(this::saveChunkIfNeeded); // Paper
        }

    }

    protected void tick(BooleanSupplier shouldKeepTicking) {
        ProfilerFiller gameprofilerfiller = this.level.getProfiler();

        try (Timing ignored = this.level.timings.poiUnload.startTiming()) { // Paper
        gameprofilerfiller.push("poi");
        this.poiManager.tick(shouldKeepTicking);
        } // Paper
        gameprofilerfiller.popPush("chunk_unload");
        if (!this.level.noSave()) {
            try (Timing ignored = this.level.timings.chunkUnload.startTiming()) { // Paper
            this.processUnloads(shouldKeepTicking);
            } // Paper
        }

        gameprofilerfiller.pop();
    }

    public boolean hasWork() {
        return this.lightEngine.hasLightWork() || !this.pendingUnloads.isEmpty() || !this.updatingChunks.getUpdatingValuesCopy().isEmpty() || this.poiManager.hasWork() || !this.toDrop.isEmpty() || !this.unloadQueue.isEmpty() || this.queueSorter.hasWork() || this.distanceManager.hasTickets(); // Paper
    }

    private void processUnloads(BooleanSupplier shouldKeepTicking) {
        LongIterator longiterator = this.toDrop.iterator();
        for (int i = 0; longiterator.hasNext() && (shouldKeepTicking.getAsBoolean() || i < 200 || this.toDrop.size() > 2000); longiterator.remove()) { // Paper - diff on change
            long j = longiterator.nextLong();
            ChunkHolder playerchunk = this.updatingChunks.queueRemove(j); // Paper - Don't copy

            if (playerchunk != null) {
                playerchunk.onChunkRemove(); // Paper
                this.pendingUnloads.put(j, playerchunk);
                this.modified = true;
                ++i;
                this.scheduleUnload(j, playerchunk);
            }
        }

        int k = Math.max(100, this.unloadQueue.size() - 2000); // Paper - Unload more than just up to queue size 2000

        Runnable runnable;

        while ((shouldKeepTicking.getAsBoolean() || k > 0) && (runnable = (Runnable) this.unloadQueue.poll()) != null) {
            --k;
            runnable.run();
        }

        int l = 0;
        // Paper - incremental chunk and player saving

    }

    private void scheduleUnload(long pos, ChunkHolder holder) {
        CompletableFuture<ChunkAccess> completablefuture = holder.getChunkToSave();
        Consumer<ChunkAccess> consumer = (ichunkaccess) -> { // CraftBukkit - decompile error
            CompletableFuture<ChunkAccess> completablefuture1 = holder.getChunkToSave();

            if (completablefuture1 != completablefuture) {
                this.scheduleUnload(pos, holder);
            } else {
                // Paper start - do not allow ticket level changes while unloading chunks
                org.spigotmc.AsyncCatcher.catchOp("playerchunk unload");
                boolean unloadingBefore = this.unloadingPlayerChunk;
                this.unloadingPlayerChunk = true;
                try {
                    // Paper end - do not allow ticket level changes while unloading chunks
                // Paper start
                boolean removed;
                if ((removed = this.pendingUnloads.remove(pos, holder)) && ichunkaccess != null) {
                    for (int index = 0, len = this.regionManagers.size(); index < len; ++index) {
                        this.regionManagers.get(index).removeChunk(holder.pos.x, holder.pos.z);
                    }
                    // Paper end
                    this.getPoiManager().queueUnload(holder.pos.longKey, MinecraftServer.currentTickLong + 1); // Paper - unload POI data
                    if (ichunkaccess instanceof LevelChunk) {
                        ((LevelChunk) ichunkaccess).setLoaded(false);
                    }

                    // Paper start - async chunk saving
                    try {
                        this.asyncSave(ichunkaccess);
                    } catch (ThreadDeath ex) {
                        throw ex; // bye
                    } catch (Throwable ex) {
                        LOGGER.error("Failed to prepare async save, attempting synchronous save", ex);
                        this.save(ichunkaccess);
                    }
                    // Paper end - async chunk saving
                    if (this.entitiesInLevel.remove(pos) && ichunkaccess instanceof LevelChunk) {
                        LevelChunk chunk = (LevelChunk) ichunkaccess;

                        this.level.unload(chunk);
                    }
                    this.autoSaveQueue.remove(holder); // Paper

                    this.lightEngine.updateChunkStatus(ichunkaccess.getPos());
                    this.lightEngine.tryScheduleUpdate();
                    this.progressListener.onStatusChange(ichunkaccess.getPos(), (ChunkStatus) null);
                    this.chunkSaveCooldowns.remove(ichunkaccess.getPos().toLong());
                } else if (removed) { // Paper start
                for (int index = 0, len = this.regionManagers.size(); index < len; ++index) {
                    this.regionManagers.get(index).removeChunk(holder.pos.x, holder.pos.z);
                }
                this.getPoiManager().queueUnload(holder.pos.longKey, MinecraftServer.currentTickLong + 1); // Paper - unload POI data
            } // Paper end
                } finally { this.unloadingPlayerChunk = unloadingBefore; } // Paper - do not allow ticket level changes while unloading chunks

            }
        };
        Queue queue = this.unloadQueue;

        Objects.requireNonNull(this.unloadQueue);
        completablefuture.thenAcceptAsync(consumer, queue::add).whenComplete((ovoid, throwable) -> {
            if (throwable != null) {
                ChunkMap.LOGGER.error("Failed to save chunk {}", holder.getPos(), throwable);
            }

        });
    }

    protected boolean promoteChunkMap() {
        if (!this.modified) {
            return false;
        } else {
            // Paper start - Don't copy
            synchronized (this.updatingChunks) {
                this.updatingChunks.performUpdates();
            }
            // Paper end - Don't copy

            this.modified = false;
            return true;
        }
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> schedule(ChunkHolder holder, ChunkStatus requiredStatus) {
        ChunkPos chunkcoordintpair = holder.getPos();

        if (requiredStatus == ChunkStatus.EMPTY) {
            return this.scheduleChunkLoad(chunkcoordintpair);
        } else {
            // Paper start - revert 1.17 chunk system changes
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = holder.getOrScheduleFuture(requiredStatus.getParent(), this);
        return future.thenComposeAsync((either) -> {
            Optional<ChunkAccess> optional = either.left();
            if (!optional.isPresent()) {
                return CompletableFuture.completedFuture(either);
            }
            // Paper end - revert 1.17 chunk system changes
            if (requiredStatus == ChunkStatus.LIGHT) {
                this.distanceManager.addTicket(TicketType.LIGHT, chunkcoordintpair, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), chunkcoordintpair);
            }

            // Paper - revert 1.17 chunk system changes

            if (optional.isPresent() && ((ChunkAccess) optional.get()).getStatus().isOrAfter(requiredStatus)) {
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = requiredStatus.load(this.level, this.structureManager, this.lightEngine, (ichunkaccess) -> {
                    return this.protoChunkToFullChunk(holder);
                }, (ChunkAccess) optional.get());

                this.progressListener.onStatusChange(chunkcoordintpair, requiredStatus);
                return completablefuture;
            } else {
                return this.scheduleChunkGeneration(holder, requiredStatus);
            }
        }, this.mainThreadExecutor).thenComposeAsync(CompletableFuture::completedFuture, this.mainThreadExecutor); // Paper - revert 1.17 chunk system changes
        }
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(ChunkPos pos) {
        // Paper start - Async chunk io
        final java.util.function.BiFunction<ChunkSerializer.InProgressChunkHolder, Throwable, Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> syncLoadComplete = (chunkHolder, ioThrowable) -> {
            try (Timing ignored = this.level.timings.chunkLoad.startTimingIfSync()) { // Paper
                this.level.getProfiler().incrementCounter("chunkLoad");
                // Paper start
                if (ioThrowable != null) {
                    com.destroystokyo.paper.util.SneakyThrow.sneaky(ioThrowable);
                }
                this.poiManager.loadInData(pos, chunkHolder.poiData);
                chunkHolder.tasks.forEach(Runnable::run);
                this.getPoiManager().dequeueUnload(pos.longKey); // Paper
                // Paper end

                if (chunkHolder.protoChunk != null) {try (Timing ignored2 = this.level.timings.chunkLoadLevelTimer.startTimingIfSync()) { // Paper start - timings // Paper - chunk is created async
                    if (true) {
                        ProtoChunk protochunk = chunkHolder.protoChunk;
                        this.markPosition(pos, protochunk.getStatus().getChunkType());
                        return Either.left(protochunk);
                    }

                    ChunkMap.LOGGER.error("Chunk file at {} is missing level data, skipping", pos);
                }} // Paper
            } catch (ReportedException reportedexception) {
                Throwable throwable = reportedexception.getCause();

                if (!(throwable instanceof IOException)) {
                    this.markPositionReplaceable(pos);
                    throw reportedexception;
                }

                ChunkMap.LOGGER.error("Couldn't load chunk {}", pos, throwable);
            } catch (Exception exception) {
                ChunkMap.LOGGER.error("Couldn't load chunk {}", pos, exception);
            }

            this.markPositionReplaceable(pos);
            return Either.left(new ProtoChunk(pos, UpgradeData.EMPTY, this.level, this.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), (BlendingData) null));
            // Paper start - Async chunk io
        };
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> ret = new CompletableFuture<>();

        Consumer<ChunkSerializer.InProgressChunkHolder> chunkHolderConsumer = (ChunkSerializer.InProgressChunkHolder holder) -> {
            // Go into the chunk load queue and not server task queue so we can be popped out even faster.
            com.destroystokyo.paper.io.chunk.ChunkTaskManager.queueChunkWaitTask(() -> {
                try {
                    ret.complete(syncLoadComplete.apply(holder, null));
                } catch (Exception e) {
                    ret.completeExceptionally(e);
                }
            });
        };

        CompletableFuture<CompoundTag> chunkSaveFuture = this.level.asyncChunkTaskManager.getChunkSaveFuture(pos.x, pos.z);
        // Paper start
        ChunkHolder playerChunk = getUpdatingChunkIfPresent(pos.toLong());
        int chunkPriority = playerChunk != null ? playerChunk.requestedPriority : 33;
        int priority = com.destroystokyo.paper.io.PrioritizedTaskQueue.NORMAL_PRIORITY;

        if (chunkPriority <= 10) {
            priority = com.destroystokyo.paper.io.PrioritizedTaskQueue.HIGHEST_PRIORITY;
        } else if (chunkPriority <= 20) {
            priority = com.destroystokyo.paper.io.PrioritizedTaskQueue.HIGH_PRIORITY;
        }
        boolean isHighestPriority = priority == com.destroystokyo.paper.io.PrioritizedTaskQueue.HIGHEST_PRIORITY;
        // Paper end
        if (chunkSaveFuture != null) {
            this.level.asyncChunkTaskManager.scheduleChunkLoad(pos.x, pos.z, priority, chunkHolderConsumer, isHighestPriority, chunkSaveFuture); // Paper
        } else {
            this.level.asyncChunkTaskManager.scheduleChunkLoad(pos.x, pos.z, priority, chunkHolderConsumer, isHighestPriority); // Paper
        }
        this.level.asyncChunkTaskManager.raisePriority(pos.x, pos.z, priority); // Paper
        return ret;
        // Paper end
    }

    private void markPositionReplaceable(ChunkPos pos) {
        this.chunkTypeCache.put(pos.toLong(), (byte) -1);
    }

    private byte markPosition(ChunkPos pos, ChunkStatus.ChunkType type) {
        return this.chunkTypeCache.put(pos.toLong(), (byte) (type == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkGeneration(ChunkHolder holder, ChunkStatus requiredStatus) {
        ChunkPos chunkcoordintpair = holder.getPos();
        CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getChunkRangeFuture(chunkcoordintpair, requiredStatus.getRange(), (i) -> {
            return this.getDependencyStatus(requiredStatus, i);
        });

        this.level.getProfiler().incrementCounter(() -> {
            return "chunkGenerate " + requiredStatus.getName();
        });
        Executor executor = (runnable) -> {
            // Paper start - optimize chunk status progression without jumping through thread pool
            if (holder.canAdvanceStatus()) {
                this.mainInvokingExecutor.execute(runnable);
                return;
            }
            // Paper end
            this.worldgenMailbox.tell(ChunkTaskPriorityQueueSorter.message(holder, runnable));
        };

        return completablefuture.thenComposeAsync((either) -> {
            return (CompletionStage) either.map((list) -> {
                try {
                    CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture1 = requiredStatus.generate(executor, this.level, this.generator, this.structureManager, this.lightEngine, (ichunkaccess) -> {
                        return this.protoChunkToFullChunk(holder);
                    }, list, false);

                    this.progressListener.onStatusChange(chunkcoordintpair, requiredStatus);
                    return completablefuture1;
                } catch (Exception exception) {
                    exception.getStackTrace();
                    CrashReport crashreport = CrashReport.forThrowable(exception, "Exception generating new chunk");
                    CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Chunk to be generated");

                    crashreportsystemdetails.setDetail("Location", (Object) String.format("%d,%d", chunkcoordintpair.x, chunkcoordintpair.z));
                    crashreportsystemdetails.setDetail("Position hash", (Object) ChunkPos.asLong(chunkcoordintpair.x, chunkcoordintpair.z));
                    crashreportsystemdetails.setDetail("Generator", (Object) this.generator);
                    this.mainThreadExecutor.execute(() -> {
                        throw new ReportedException(crashreport);
                    });
                    throw new ReportedException(crashreport);
                }
            }, (playerchunk_failure) -> {
                this.releaseLightTicket(chunkcoordintpair);
                return CompletableFuture.completedFuture(Either.right(playerchunk_failure));
            });
        }, executor).thenComposeAsync((either) -> { // Paper start - force competion on the main thread
            return CompletableFuture.completedFuture(either);
        }, this.mainThreadExecutor); // use the main executor, we want to ensure only one chunk callback can be completed per runnable execute
        // Paper end - force competion on the main thread
    }

    protected void releaseLightTicket(ChunkPos pos) {
        this.mainThreadExecutor.tell(Util.name(() -> {
            this.distanceManager.removeTicket(TicketType.LIGHT, pos, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), pos);
        }, () -> {
            return "release light ticket " + pos;
        }));
    }

    private ChunkStatus getDependencyStatus(ChunkStatus centerChunkTargetStatus, int distance) {
        ChunkStatus chunkstatus1;

        if (distance == 0) {
            chunkstatus1 = centerChunkTargetStatus.getParent();
        } else {
            chunkstatus1 = ChunkStatus.getStatusAroundFullChunk(ChunkStatus.getDistance(centerChunkTargetStatus) + distance);
        }

        return chunkstatus1;
    }

    private static void postLoadProtoChunk(ServerLevel world, List<CompoundTag> nbt) {
        if (!nbt.isEmpty()) {
            // CraftBukkit start - these are spawned serialized (DefinedStructure) and we don't call an add event below at the moment due to ordering complexities
            world.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(nbt, world).filter((entity) -> {
                boolean needsRemoval = false;
                net.minecraft.server.dedicated.DedicatedServer server = world.getCraftServer().getServer();
                if (!server.areNpcsEnabled() && entity instanceof net.minecraft.world.entity.npc.Npc) {
                    entity.discard();
                    needsRemoval = true;
                }
                if (!server.isSpawningAnimals() && (entity instanceof net.minecraft.world.entity.animal.Animal || entity instanceof net.minecraft.world.entity.animal.WaterAnimal)) {
                    entity.discard();
                    needsRemoval = true;
                }
                checkDupeUUID(world, entity); // Paper
                return !needsRemoval;
            }));
            // CraftBukkit end
        }

    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> protoChunkToFullChunk(ChunkHolder chunkHolder) {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = chunkHolder.getFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());

        return completablefuture.thenApplyAsync((either) -> {
            ChunkStatus chunkstatus = ChunkHolder.getStatus(chunkHolder.getTicketLevel());

            return !chunkstatus.isOrAfter(ChunkStatus.FULL) ? ChunkHolder.UNLOADED_CHUNK : either.mapLeft((ichunkaccess) -> {
                try (Timing ignored = level.timings.chunkPostLoad.startTimingIfSync()) { // Paper
                ChunkPos chunkcoordintpair = chunkHolder.getPos();
                ProtoChunk protochunk = (ProtoChunk) ichunkaccess;
                LevelChunk chunk;

                if (protochunk instanceof ImposterProtoChunk) {
                    chunk = ((ImposterProtoChunk) protochunk).getWrapped();
                } else {
                    chunk = new LevelChunk(this.level, protochunk, (chunk1) -> {
                        ChunkMap.postLoadProtoChunk(this.level, protochunk.getEntities());
                    });
                    chunkHolder.replaceProtoChunk(new ImposterProtoChunk(chunk, false));
                }

                chunk.setFullStatus(() -> {
                    return ChunkHolder.getFullChunkStatus(chunkHolder.getTicketLevel());
                });
                chunk.runPostLoad();
                if (this.entitiesInLevel.add(chunkcoordintpair.toLong())) {
                    chunk.setLoaded(true);
                    chunk.registerAllBlockEntitiesAfterLevelLoad();
                    chunk.registerTickContainerInLevel(this.level);
                }

                return chunk;
                } // Paper
            });
        }, (runnable) -> {
            ProcessorHandle mailbox = this.mainThreadMailbox;
            long i = chunkHolder.getPos().toLong();

            Objects.requireNonNull(chunkHolder);
            mailbox.tell(ChunkTaskPriorityQueueSorter.message(runnable, i, () -> 1)); // Paper - final loads are always urgent!
        });
    }

    // Paper start
    private static void checkDupeUUID(ServerLevel level, Entity entity) {
        PaperWorldConfig.DuplicateUUIDMode mode = level.paperConfig.duplicateUUIDMode;
        if (mode != PaperWorldConfig.DuplicateUUIDMode.WARN
            && mode != PaperWorldConfig.DuplicateUUIDMode.DELETE
            && mode != PaperWorldConfig.DuplicateUUIDMode.SAFE_REGEN) {
            return;
        }
        Entity other = level.getEntity(entity.getUUID());

        if (mode == PaperWorldConfig.DuplicateUUIDMode.SAFE_REGEN && other != null && !other.isRemoved()
            && Objects.equals(other.getEncodeId(), entity.getEncodeId())
            && entity.getBukkitEntity().getLocation().distance(other.getBukkitEntity().getLocation()) < level.paperConfig.duplicateUUIDDeleteRange
        ) {
            if (ServerLevel.DEBUG_ENTITIES) LOGGER.warn("[DUPE-UUID] Duplicate UUID found used by " + other + ", deleted entity " + entity + " because it was near the duplicate and likely an actual duplicate. See https://github.com/PaperMC/Paper/issues/1223 for discussion on what this is about.");
            entity.discard();
            return;
        }
        if (other != null && !other.isRemoved()) {
            switch (mode) {
                case SAFE_REGEN: {
                    entity.setUUID(UUID.randomUUID());
                    if (ServerLevel.DEBUG_ENTITIES) LOGGER.warn("[DUPE-UUID] Duplicate UUID found used by " + other + ", regenerated UUID for " + entity + ". See https://github.com/PaperMC/Paper/issues/1223 for discussion on what this is about.");
                    break;
                }
                case DELETE: {
                    if (ServerLevel.DEBUG_ENTITIES) LOGGER.warn("[DUPE-UUID] Duplicate UUID found used by " + other + ", deleted entity " + entity + ". See https://github.com/PaperMC/Paper/issues/1223 for discussion on what this is about.");
                    entity.discard();
                    break;
                }
                default:
                    if (ServerLevel.DEBUG_ENTITIES) LOGGER.warn("[DUPE-UUID] Duplicate UUID found used by " + other + ", doing nothing to " + entity + ". See https://github.com/PaperMC/Paper/issues/1223 for discussion on what this is about.");
                    break;
            }
        }
    }
    // Paper end
    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareTickingChunk(ChunkHolder holder) {
        ChunkPos chunkcoordintpair = holder.getPos();
        CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getChunkRangeFuture(chunkcoordintpair, 1, (i) -> {
            return ChunkStatus.FULL;
        });
        CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completablefuture1 = completablefuture.thenApplyAsync((either) -> {
            return either.mapLeft((list) -> {
                // Paper start - revert 1.18.2 diff
                final LevelChunk chunk = (LevelChunk) list.get(list.size() / 2);
                chunk.postProcessGeneration();
                this.level.startTickingChunk(chunk);
                return chunk;
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(holder, () -> ChunkMap.this.chunkLoadConversionCallbackExecutor.execute(runnable))); // Paper - delay running Chunk post processing until outside of the sorter to prevent a deadlock scenario when post processing causes another chunk request.
        }); // Paper end - revert 1.18.2 diff

        completablefuture1.thenAcceptAsync((either) -> {
            either.ifLeft((chunk) -> {
                this.tickingGenerated.getAndIncrement();
                // Paper - no-tick view distance - moved to Chunk neighbour update
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(holder, runnable));
        });
        return completablefuture1;
    }

    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareAccessibleChunk(ChunkHolder holder) {
        return this.getChunkRangeFuture(holder.getPos(), 1, ChunkStatus::getStatusAroundFullChunk).thenApplyAsync((either) -> {
            return either.mapLeft((list) -> {
                LevelChunk chunk = (LevelChunk) list.get(list.size() / 2);

                return chunk;
            });
        }, this.mainThreadExecutor); // Paper - queue to execute immediately so this doesn't delay chunk unloading
    }

    public int getTickingGenerated() {
        return this.tickingGenerated.get();
    }

    private boolean saveChunkIfNeeded(ChunkHolder chunkHolder) {
        if (!chunkHolder.wasAccessibleSinceLastSave()) {
            return false;
        } else {
            ChunkAccess ichunkaccess = (ChunkAccess) chunkHolder.getChunkToSave().getNow(null); // CraftBukkit - decompile error

            if (!(ichunkaccess instanceof ImposterProtoChunk) && !(ichunkaccess instanceof LevelChunk)) {
                return false;
            } else {
                long i = ichunkaccess.getPos().toLong();
                long j = this.chunkSaveCooldowns.getOrDefault(i, -1L);
                long k = System.currentTimeMillis();

                if (k < j) {
                    return false;
                } else {
                    boolean flag = this.save(ichunkaccess);

                    chunkHolder.refreshAccessibility();
                    if (flag) {
                        this.chunkSaveCooldowns.put(i, k + 10000L);
                    }

                    return flag;
                }
            }
        }
    }

    // Paper start - async chunk save for unload
    // Note: This is very unsafe to call if the chunk is still in use.
    // This is also modeled after PlayerChunkMap#save(IChunkAccess, boolean), with the intentional difference being
    // serializing the chunk is left to a worker thread.
    private void asyncSave(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        CompoundTag poiData;
        try (Timing ignored = this.level.timings.chunkUnloadPOISerialization.startTiming()) {
            poiData = this.poiManager.getData(chunk.getPos());
        }

        com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE.scheduleSave(this.level, chunkPos.x, chunkPos.z,
            poiData, null, com.destroystokyo.paper.io.PrioritizedTaskQueue.NORMAL_PRIORITY);

        if (!chunk.isUnsaved()) {
            return;
        }

        ChunkStatus chunkstatus = chunk.getStatus();

        // Copied from PlayerChunkMap#save(IChunkAccess, boolean)
        if (chunkstatus.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
            // Paper start - Optimize save by using status cache
            if (chunkstatus == ChunkStatus.EMPTY && chunk.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                return;
            }
        }

        ChunkSerializer.AsyncSaveData asyncSaveData;
        try (Timing ignored = this.level.timings.chunkUnloadPrepareSave.startTiming()) {
            asyncSaveData = ChunkSerializer.getAsyncSaveData(this.level, chunk);
        }

        this.level.asyncChunkTaskManager.scheduleChunkSave(chunkPos.x, chunkPos.z, com.destroystokyo.paper.io.PrioritizedTaskQueue.NORMAL_PRIORITY,
            asyncSaveData, chunk);

        chunk.setUnsaved(false);
        chunk.setLastSaved(this.level.getGameTime()); // Paper - track last saved time
    }
    // Paper end

    public boolean save(ChunkAccess chunk) {
        try (co.aikar.timings.Timing ignored = this.level.timings.chunkSave.startTiming()) { // Paper
        this.poiManager.flush(chunk.getPos());
        if (!chunk.isUnsaved()) {
            return false;
        } else {
            chunk.setLastSaved(this.level.getGameTime()); // Paper - track save time
            chunk.setUnsaved(false);
            ChunkPos chunkcoordintpair = chunk.getPos();

            try {
                ChunkStatus chunkstatus = chunk.getStatus();

                if (chunkstatus.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    if (false && this.isExistingChunkFull(chunkcoordintpair)) { // Paper
                        return false;
                    }

                    if (chunkstatus == ChunkStatus.EMPTY && chunk.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return false;
                    }
                }

                this.level.getProfiler().incrementCounter("chunkSave");
                CompoundTag nbttagcompound;
                try (co.aikar.timings.Timing ignored1 = this.level.timings.chunkSaveDataSerialization.startTiming()) { // Paper
                    nbttagcompound = ChunkSerializer.write(this.level, chunk);
                } // Paper

                // Paper start - async chunk io
                com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE.scheduleSave(this.level, chunkcoordintpair.x, chunkcoordintpair.z,
                    null, nbttagcompound, com.destroystokyo.paper.io.PrioritizedTaskQueue.NORMAL_PRIORITY);
                // Paper end - async chunk io
                this.markPosition(chunkcoordintpair, chunkstatus.getChunkType());
                return true;
            } catch (Exception exception) {
                ChunkMap.LOGGER.error("Failed to save chunk {},{}", new Object[]{chunkcoordintpair.x, chunkcoordintpair.z, exception});
                com.destroystokyo.paper.exception.ServerInternalException.reportInternalException(exception); // Paper
                return false;
            }
        }
        } // Paper
    }

    private boolean isExistingChunkFull(ChunkPos pos) {
        byte b0 = this.chunkTypeCache.get(pos.toLong());

        if (b0 != 0) {
            return b0 == 1;
        } else {
            CompoundTag nbttagcompound;

            try {
                nbttagcompound = this.readChunk(pos);
                if (nbttagcompound == null) {
                    this.markPositionReplaceable(pos);
                    return false;
                }
            } catch (Exception exception) {
                ChunkMap.LOGGER.error("Failed to read chunk {}", pos, exception);
                this.markPositionReplaceable(pos);
                return false;
            }

            ChunkStatus.ChunkType chunkstatus_type = ChunkSerializer.getChunkTypeFromTag(nbttagcompound);

            return this.markPosition(pos, chunkstatus_type) == 1;
        }
    }

    public void setViewDistance(int watchDistance) {
        int j = Mth.clamp(watchDistance + 1, (int) 3, (int) 33);

        if (j != this.viewDistance) {
            int k = this.viewDistance;

            this.viewDistance = j;
            this.playerChunkManager.setLoadDistance(this.viewDistance); // Paper - replace player loader system
        }

    }

    // Paper start - replace player loader system
    public void setTickViewDistance(int distance) {
        this.playerChunkManager.setTickDistance(distance);
    }
    // Paper end - replace player loader system

    public void updateChunkTracking(ServerPlayer player, ChunkPos pos, MutableObject<java.util.Map<Object, ClientboundLevelChunkWithLightPacket>> packet, boolean oldWithinViewDistance, boolean newWithinViewDistance) { // Paper - Anti-Xray - Bypass // Paper - public
        if (player.level == this.level) {
            if (newWithinViewDistance && !oldWithinViewDistance) {
                ChunkHolder playerchunk = this.getVisibleChunkIfPresent(pos.toLong());

                if (playerchunk != null) {
                    LevelChunk chunk = playerchunk.getSendingChunk(); // Paper - replace chunk loader system

                    if (chunk != null) {
                        this.playerLoadedChunk(player, packet, chunk);
                    }

                    DebugPackets.sendPoiPacketsForChunk(this.level, pos);
                }
            }

            if (!newWithinViewDistance && oldWithinViewDistance) {
                player.untrackChunk(pos);
            }

        }
    }

    public int size() {
        return this.updatingChunks.getVisibleMap().size(); // Paper - Don't copy
    }

    public DistanceManager getDistanceManager() {
        return this.distanceManager;
    }

    protected Iterable<ChunkHolder> getChunks() {
        return Iterables.unmodifiableIterable(this.updatingChunks.getVisibleValuesCopy()); // Paper
    }

    void dumpChunks(Writer writer) throws IOException {
        CsvOutput csvwriter = CsvOutput.builder().addColumn("x").addColumn("z").addColumn("level").addColumn("in_memory").addColumn("status").addColumn("full_status").addColumn("accessible_ready").addColumn("ticking_ready").addColumn("entity_ticking_ready").addColumn("ticket").addColumn("spawning").addColumn("block_entity_count").addColumn("ticking_ticket").addColumn("ticking_level").addColumn("block_ticks").addColumn("fluid_ticks").build(writer);
        // Paper - replace loader system
        ObjectBidirectionalIterator objectbidirectionaliterator = this.updatingChunks.getVisibleMap().clone().long2ObjectEntrySet().fastIterator(); // Paper

        while (objectbidirectionaliterator.hasNext()) {
            Entry<ChunkHolder> entry = (Entry) objectbidirectionaliterator.next();
            long i = entry.getLongKey();
            ChunkPos chunkcoordintpair = new ChunkPos(i);
            ChunkHolder playerchunk = (ChunkHolder) entry.getValue();
            Optional<ChunkAccess> optional = Optional.ofNullable(playerchunk.getLastAvailable());
            Optional<LevelChunk> optional1 = optional.flatMap((ichunkaccess) -> {
                return ichunkaccess instanceof LevelChunk ? Optional.of((LevelChunk) ichunkaccess) : Optional.empty();
            });

            // CraftBukkit - decompile error
            csvwriter.writeRow(chunkcoordintpair.x, chunkcoordintpair.z, playerchunk.getTicketLevel(), optional.isPresent(), optional.map(ChunkAccess::getStatus).orElse(null), optional1.map(LevelChunk::getFullStatus).orElse(null), ChunkMap.printFuture(playerchunk.getFullChunkFuture()), ChunkMap.printFuture(playerchunk.getTickingChunkFuture()), ChunkMap.printFuture(playerchunk.getEntityTickingChunkFuture()), this.distanceManager.getTicketDebugString(i), this.anyPlayerCloseEnoughForSpawning(chunkcoordintpair), optional1.map((chunk) -> {
                return chunk.getBlockEntities().size();
            }).orElse(0), "Use ticket level", -1000, optional1.map((chunk) -> { // Paper - replace loader system
                return chunk.getBlockTicks().count();
            }).orElse(0), optional1.map((chunk) -> {
                return chunk.getFluidTicks().count();
            }).orElse(0));
        }

    }

    private static String printFuture(CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> future) {
        try {
            Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = (Either) future.getNow(null); // CraftBukkit - decompile error

            return either != null ? (String) either.map((chunk) -> {
                return "done";
            }, (playerchunk_failure) -> {
                return "unloaded";
            }) : "not completed";
        } catch (CompletionException completionexception) {
            return "failed " + completionexception.getCause().getMessage();
        } catch (CancellationException cancellationexception) {
            return "cancelled";
        }
    }

    // Paper start - Asynchronous chunk io
    @Nullable
    @Override
    public CompoundTag read(ChunkPos chunkcoordintpair) throws IOException {
        if (Thread.currentThread() != com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE) {
            CompoundTag ret = com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE
                .loadChunkDataAsyncFuture(this.level, chunkcoordintpair.x, chunkcoordintpair.z, com.destroystokyo.paper.io.IOUtil.getPriorityForCurrentThread(),
                    false, true, true).join().chunkData;

            if (ret == com.destroystokyo.paper.io.PaperFileIOThread.FAILURE_VALUE) {
                throw new IOException("See logs for further detail");
            }
            return ret;
        }
        return super.read(chunkcoordintpair);
    }

    @Override
    public void write(ChunkPos chunkcoordintpair, CompoundTag nbttagcompound) throws IOException {
        if (Thread.currentThread() != com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE) {
            com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE.scheduleSave(
                this.level, chunkcoordintpair.x, chunkcoordintpair.z, null, nbttagcompound,
                com.destroystokyo.paper.io.IOUtil.getPriorityForCurrentThread());
            return;
        }
        super.write(chunkcoordintpair, nbttagcompound);
    }
    // Paper end

    @Nullable
    public CompoundTag readChunk(ChunkPos pos) throws IOException {
        CompoundTag nbttagcompound = this.read(pos);
        // Paper start - Cache chunk status on disk
        if (nbttagcompound == null) {
            return null;
        }

        nbttagcompound = this.upgradeChunkTag(this.level.getTypeKey(), this.overworldDataStorage, nbttagcompound, this.generator.getTypeNameForDataFixer(), pos, level); // CraftBukkit
        if (nbttagcompound == null) {
            return null;
        }

        this.updateChunkStatusOnDisk(pos, nbttagcompound);

        return nbttagcompound;
        // Paper end
    }

    // Paper start - chunk status cache "api"
    public ChunkStatus getChunkStatusOnDiskIfCached(ChunkPos chunkPos) {
        RegionFile regionFile = regionFileCache.getRegionFileIfLoaded(chunkPos);

        return regionFile == null ? null : regionFile.getStatusIfCached(chunkPos.x, chunkPos.z);
    }

    public ChunkStatus getChunkStatusOnDisk(ChunkPos chunkPos) throws IOException {
        RegionFile regionFile = regionFileCache.getRegionFile(chunkPos, true);

        if (regionFile == null || !regionFileCache.chunkExists(chunkPos)) {
            return null;
        }

        ChunkStatus status = regionFile.getStatusIfCached(chunkPos.x, chunkPos.z);

        if (status != null) {
            return status;
        }

        this.readChunk(chunkPos);

        return regionFile.getStatusIfCached(chunkPos.x, chunkPos.z);
    }

    public void updateChunkStatusOnDisk(ChunkPos chunkPos, @Nullable CompoundTag compound) throws IOException {
        RegionFile regionFile = regionFileCache.getRegionFile(chunkPos, false);

        regionFile.setStatus(chunkPos.x, chunkPos.z, ChunkSerializer.getStatus(compound));
    }

    public ChunkAccess getUnloadingChunk(int chunkX, int chunkZ) {
        ChunkHolder chunkHolder = this.pendingUnloads.get(ChunkPos.asLong(chunkX, chunkZ));
        return chunkHolder == null ? null : chunkHolder.getAvailableChunkNow();
    }
    // Paper end

    boolean anyPlayerCloseEnoughForSpawning(ChunkPos pos) {
        // Spigot start
        return this.anyPlayerCloseEnoughForSpawning(pos, false);
    }

    // Paper start - optimise anyPlayerCloseEnoughForSpawning
    final boolean anyPlayerCloseEnoughForSpawning(ChunkPos chunkcoordintpair, boolean reducedRange) {
        return this.anyPlayerCloseEnoughForSpawning(this.getUpdatingChunkIfPresent(chunkcoordintpair.toLong()), chunkcoordintpair, reducedRange);
    }

    final boolean anyPlayerCloseEnoughForSpawning(ChunkHolder playerchunk, ChunkPos chunkcoordintpair, boolean reducedRange) {
        // this function is so hot that removing the map lookup call can have an order of magnitude impact on its performance
        // tested and confirmed via System.nanoTime()
        com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> playersInRange = reducedRange ? playerchunk.playersInMobSpawnRange : playerchunk.playersInChunkTickRange;
        if (playersInRange == null) {
            return false;
        }
        Object[] backingSet = playersInRange.getBackingSet();

        if (reducedRange) {
            for (int i = 0, len = backingSet.length; i < len; ++i) {
                Object raw = backingSet[i];
                if (!(raw instanceof ServerPlayer player)) {
                    continue;
                }
                // don't check spectator and whatnot, already handled by mob spawn map update
                if (euclideanDistanceSquared(chunkcoordintpair, player) < player.lastEntitySpawnRadiusSquared) {
                    return true; // in range
                }
            }
        } else {
            final double range = (DistanceManager.MOB_SPAWN_RANGE * 16) * (DistanceManager.MOB_SPAWN_RANGE * 16);
            // before spigot, mob spawn range was actually mob spawn range + tick range, but it was split
            for (int i = 0, len = backingSet.length; i < len; ++i) {
                Object raw = backingSet[i];
                if (!(raw instanceof ServerPlayer player)) {
                    continue;
                }
                // don't check spectator and whatnot, already handled by mob spawn map update
                if (euclideanDistanceSquared(chunkcoordintpair, player) < range) {
                    return true; // in range
                }
            }
        }
        // no players in range
        return false;
        // Paper end - optimise anyPlayerCloseEnoughForSpawning
    }

    public List<ServerPlayer> getPlayersCloseForSpawning(ChunkPos pos) {
        long i = pos.toLong();

        if (!this.distanceManager.hasPlayersNearby(i)) {
            return List.of();
        } else {
            Builder<ServerPlayer> builder = ImmutableList.builder();
            Iterator iterator = this.playerMap.getPlayers(i).iterator();

            while (iterator.hasNext()) {
                ServerPlayer entityplayer = (ServerPlayer) iterator.next();

                if (this.playerIsCloseEnoughForSpawning(entityplayer, pos, 16384.0D)) { // Spigot
                    builder.add(entityplayer);
                }
            }

            return builder.build();
        }
    }

    private boolean playerIsCloseEnoughForSpawning(ServerPlayer entityplayer, ChunkPos chunkcoordintpair, double range) { // Spigot
        if (entityplayer.isSpectator()) {
            return false;
        } else {
            double d0 = ChunkMap.euclideanDistanceSquared(chunkcoordintpair, entityplayer);

            return d0 < range; // Spigot
        }
    }

    private boolean skipPlayer(ServerPlayer player) {
        return player.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
    }

    void updatePlayerStatus(ServerPlayer player, boolean added) {
        boolean flag1 = this.skipPlayer(player);
        boolean flag2 = this.playerMap.ignoredOrUnknown(player);
        int i = SectionPos.blockToSectionCoord(player.getBlockX());
        int j = SectionPos.blockToSectionCoord(player.getBlockZ());

        if (added) {
            this.playerMap.addPlayer(ChunkPos.asLong(i, j), player, flag1);
            this.updatePlayerPos(player);
            if (!flag1) {
                this.distanceManager.addPlayer(SectionPos.of((Entity) player), player);
            }
            this.addPlayerToDistanceMaps(player); // Paper - distance maps
        } else {
            SectionPos sectionposition = player.getLastSectionPos();

            this.playerMap.removePlayer(sectionposition.chunk().toLong(), player);
            if (!flag2) {
                this.distanceManager.removePlayer(sectionposition, player);
            }
            this.removePlayerFromDistanceMaps(player); // Paper - distance maps
        }

        // Paper - handled by player chunk loader

    }

    private SectionPos updatePlayerPos(ServerPlayer player) {
        SectionPos sectionposition = SectionPos.of((Entity) player);

        player.setLastSectionPos(sectionposition);
        //player.connection.send(new ClientboundSetChunkCacheCenterPacket(sectionposition.x(), sectionposition.z())); // Paper - handled by player chunk loader
        return sectionposition;
    }

    public void move(ServerPlayer player) {
        // Paper - delay this logic for the entity tracker tick, no need to duplicate it

        int i = SectionPos.blockToSectionCoord(player.getBlockX());
        int j = SectionPos.blockToSectionCoord(player.getBlockZ());
        SectionPos sectionposition = player.getLastSectionPos();
        SectionPos sectionposition1 = SectionPos.of((Entity) player);
        long k = sectionposition.chunk().toLong();
        long l = sectionposition1.chunk().toLong();
        boolean flag = this.playerMap.ignored(player);
        boolean flag1 = this.skipPlayer(player);
        boolean flag2 = sectionposition.asLong() != sectionposition1.asLong();

        if (flag2 || flag != flag1) {
            this.updatePlayerPos(player);
            if (!flag) {
                this.distanceManager.removePlayer(sectionposition, player);
            }

            if (!flag1) {
                this.distanceManager.addPlayer(sectionposition1, player);
            }

            if (!flag && flag1) {
                this.playerMap.ignorePlayer(player);
            }

            if (flag && !flag1) {
                this.playerMap.unIgnorePlayer(player);
            }

            if (k != l) {
                this.playerMap.updatePlayer(k, l, player);
            }
        }

        int i1 = sectionposition.x();
        int j1 = sectionposition.z();
        int k1;
        int l1;

        // Paper - replaced by PlayerChunkLoader

        this.updateMaps(player); // Paper - distance maps
        this.playerChunkManager.updatePlayer(player); // Paper - respond to movement immediately

    }

    @Override
    public List<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
        // Paper start - per player view distance
        // there can be potential desync with player's last mapped section and the view distance map, so use the
        // view distance map here.
        List<ServerPlayer> ret = new java.util.ArrayList<>(4);

        com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> players = this.playerChunkManager.broadcastMap.getObjectsInRange(chunkPos);
        if (players == null) {
            return ret;
        }

        Object[] backingSet = players.getBackingSet();
        for (int i = 0, len = backingSet.length; i < len; ++i) {
            Object temp = backingSet[i];
            if (!(temp instanceof ServerPlayer)) {
                continue;
            }
            ServerPlayer player = (ServerPlayer)temp;
            if (!this.playerChunkManager.isChunkSent(player, chunkPos.x, chunkPos.z, onlyOnWatchDistanceEdge)) {
                continue;
            }
            ret.add(player);
        }

        return ret;
        // Paper end - per player view distance
    }

    public void addEntity(Entity entity) {
        org.spigotmc.AsyncCatcher.catchOp("entity track"); // Spigot
        // Paper start - ignore and warn about illegal addEntity calls instead of crashing server
        if (!entity.valid || entity.level != this.level || this.entityMap.containsKey(entity.getId())) {
            new Throwable("[ERROR] Illegal PlayerChunkMap::addEntity for world " + this.level.getWorld().getName()
                + ": " + entity  + (this.entityMap.containsKey(entity.getId()) ? " ALREADY CONTAINED (This would have crashed your server)" : ""))
                .printStackTrace();
            return;
        }
        if (entity instanceof ServerPlayer && ((ServerPlayer) entity).supressTrackerForLogin) return; // Delay adding to tracker until after list packets
        // Paper end
        if (!(entity instanceof EnderDragonPart)) {
            EntityType<?> entitytypes = entity.getType();
            int i = entitytypes.clientTrackingRange() * 16;
            i = org.spigotmc.TrackingRange.getEntityTrackingRange(entity, i); // Spigot

            if (i != 0) {
                int j = entitytypes.updateInterval();

                if (this.entityMap.containsKey(entity.getId())) {
                    throw (IllegalStateException) Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
                } else {
                    ChunkMap.TrackedEntity playerchunkmap_entitytracker = new ChunkMap.TrackedEntity(entity, i, j, entitytypes.trackDeltas());

                    entity.tracker = playerchunkmap_entitytracker; // Paper - Fast access to tracker
                    this.entityMap.put(entity.getId(), playerchunkmap_entitytracker);
                    playerchunkmap_entitytracker.updatePlayers(entity.getPlayersInTrackRange()); // Paper - don't search all players
                    if (entity instanceof ServerPlayer) {
                        ServerPlayer entityplayer = (ServerPlayer) entity;

                        this.updatePlayerStatus(entityplayer, true);
                        ObjectIterator objectiterator = this.entityMap.values().iterator();

                        while (objectiterator.hasNext()) {
                            ChunkMap.TrackedEntity playerchunkmap_entitytracker1 = (ChunkMap.TrackedEntity) objectiterator.next();

                            if (playerchunkmap_entitytracker1.entity != entityplayer) {
                                playerchunkmap_entitytracker1.updatePlayer(entityplayer);
                            }
                        }
                    }

                }
            }
        }
    }

    protected void removeEntity(Entity entity) {
        org.spigotmc.AsyncCatcher.catchOp("entity untrack"); // Spigot
        if (entity instanceof ServerPlayer) {
            ServerPlayer entityplayer = (ServerPlayer) entity;

            this.updatePlayerStatus(entityplayer, false);
            ObjectIterator objectiterator = this.entityMap.values().iterator();

            while (objectiterator.hasNext()) {
                ChunkMap.TrackedEntity playerchunkmap_entitytracker = (ChunkMap.TrackedEntity) objectiterator.next();

                playerchunkmap_entitytracker.removePlayer(entityplayer);
            }
        }

        ChunkMap.TrackedEntity playerchunkmap_entitytracker1 = (ChunkMap.TrackedEntity) this.entityMap.remove(entity.getId());

        if (playerchunkmap_entitytracker1 != null) {
            playerchunkmap_entitytracker1.broadcastRemoved();
        }
        entity.tracker = null; // Paper - We're no longer tracked
    }

    // Paper start - optimised tracker
    private final void processTrackQueue() {
        this.level.timings.tracker1.startTiming();
        try {
            for (TrackedEntity tracker : this.entityMap.values()) {
                // update tracker entry
                tracker.updatePlayers(tracker.entity.getPlayersInTrackRange());
            }
        } finally {
            this.level.timings.tracker1.stopTiming();
        }


        this.level.timings.tracker2.startTiming();
        try {
            for (TrackedEntity tracker : this.entityMap.values()) {
                tracker.serverEntity.sendChanges();
            }
        } finally {
            this.level.timings.tracker2.stopTiming();
        }
    }
    // Paper end - optimised tracker

    protected void tick() {
        // Paper start - optimized tracker
        if (true) {
            this.processTrackQueue();
            return;
        }
        // Paper end - optimized tracker
        List<ServerPlayer> list = Lists.newArrayList();
        List<ServerPlayer> list1 = this.level.players();
        ObjectIterator objectiterator = this.entityMap.values().iterator();
        level.timings.tracker1.startTiming(); // Paper

        ChunkMap.TrackedEntity playerchunkmap_entitytracker;

        while (objectiterator.hasNext()) {
            playerchunkmap_entitytracker = (ChunkMap.TrackedEntity) objectiterator.next();
            SectionPos sectionposition = playerchunkmap_entitytracker.lastSectionPos;
            SectionPos sectionposition1 = SectionPos.of(playerchunkmap_entitytracker.entity);
            boolean flag = !Objects.equals(sectionposition, sectionposition1);

            if (flag) {
                playerchunkmap_entitytracker.updatePlayers(list1);
                Entity entity = playerchunkmap_entitytracker.entity;

                if (entity instanceof ServerPlayer) {
                    list.add((ServerPlayer) entity);
                }

                playerchunkmap_entitytracker.lastSectionPos = sectionposition1;
            }

            if (flag || this.distanceManager.inEntityTickingRange(sectionposition1.chunk().toLong())) {
                playerchunkmap_entitytracker.serverEntity.sendChanges();
            }
        }
        level.timings.tracker1.stopTiming(); // Paper

        if (!list.isEmpty()) {
            objectiterator = this.entityMap.values().iterator();

            level.timings.tracker2.startTiming(); // Paper
            while (objectiterator.hasNext()) {
                playerchunkmap_entitytracker = (ChunkMap.TrackedEntity) objectiterator.next();
                playerchunkmap_entitytracker.updatePlayers(list);
            }
            level.timings.tracker2.stopTiming(); // Paper
        }

    }

    public void broadcast(Entity entity, Packet<?> packet) {
        ChunkMap.TrackedEntity playerchunkmap_entitytracker = (ChunkMap.TrackedEntity) this.entityMap.get(entity.getId());

        if (playerchunkmap_entitytracker != null) {
            playerchunkmap_entitytracker.broadcast(packet);
        }

    }

    protected void broadcastAndSend(Entity entity, Packet<?> packet) {
        ChunkMap.TrackedEntity playerchunkmap_entitytracker = (ChunkMap.TrackedEntity) this.entityMap.get(entity.getId());

        if (playerchunkmap_entitytracker != null) {
            playerchunkmap_entitytracker.broadcastAndSend(packet);
        }

    }

    // Paper start - Anti-Xray - Bypass
    private void playerLoadedChunk(ServerPlayer player, MutableObject<java.util.Map<Object, ClientboundLevelChunkWithLightPacket>> cachedDataPackets, LevelChunk chunk) {
        if (cachedDataPackets.getValue() == null) {
            cachedDataPackets.setValue(new java.util.HashMap<>());
        }

        Boolean shouldModify = chunk.getLevel().chunkPacketBlockController.shouldModify(player, chunk);
        player.trackChunk(chunk.getPos(), cachedDataPackets.getValue().computeIfAbsent(shouldModify, (s) -> {
            return new ClientboundLevelChunkWithLightPacket(chunk, this.lightEngine, (BitSet) null, (BitSet) null, true, (Boolean) s);
        }));
        // Paper end
        DebugPackets.sendPoiPacketsForChunk(this.level, chunk.getPos());
        List<Entity> list = Lists.newArrayList();
        List<Entity> list1 = Lists.newArrayList();
        // Paper start - optimise entity tracker
        // use the chunk entity list, not the whole trackedEntities map...
        Entity[] entities = chunk.entities.getRawData();
        for (int i = 0, size = chunk.entities.size(); i < size; ++i) {
            Entity entity = entities[i];
            if (entity == player) {
                continue;
            }
            ChunkMap.TrackedEntity tracker = this.entityMap.get(entity.getId());
            if (tracker != null) { // dumb plugins... move on...
                tracker.updatePlayer(player);
            }

            // keep the vanilla logic here - this is REQUIRED or else passengers and their vehicles disappear!
            // (and god knows what the leash thing is)

            if (entity instanceof Mob && ((Mob)entity).getLeashHolder() != null) {
                list.add(entity);
            }

            if (!entity.getPassengers().isEmpty()) {
                list1.add(entity);
            }
        }
        // Paper end - optimise entity tracker

        Iterator iterator;
        Entity entity1;

        if (!list.isEmpty()) {
            iterator = list.iterator();

            while (iterator.hasNext()) {
                entity1 = (Entity) iterator.next();
                player.connection.send(new ClientboundSetEntityLinkPacket(entity1, ((Mob) entity1).getLeashHolder()));
            }
        }

        if (!list1.isEmpty()) {
            iterator = list1.iterator();

            while (iterator.hasNext()) {
                entity1 = (Entity) iterator.next();
                player.connection.send(new ClientboundSetPassengersPacket(entity1));
            }
        }

    }

    public PoiManager getPoiManager() {
        return this.poiManager;
    }

    public String getStorageName() {
        return this.storageName;
    }

    void onFullChunkStatusChange(ChunkPos chunkPos, ChunkHolder.FullChunkStatus levelType) {
        this.chunkStatusListener.onChunkStatusChange(chunkPos, levelType);
    }

    public class ChunkDistanceManager extends DistanceManager {

        protected ChunkDistanceManager(Executor workerExecutor, Executor mainThreadExecutor) {
            super(workerExecutor, mainThreadExecutor, ChunkMap.this);
        }

        @Override
        protected boolean isChunkToRemove(long pos) {
            return ChunkMap.this.toDrop.contains(pos);
        }

        @Nullable
        @Override
        protected ChunkHolder getChunk(long pos) {
            return ChunkMap.this.getUpdatingChunkIfPresent(pos);
        }

        @Nullable
        @Override
        protected ChunkHolder updateChunkScheduling(long pos, int level, @Nullable ChunkHolder holder, int k) {
            return ChunkMap.this.updateChunkScheduling(pos, level, holder, k);
        }
    }

    public class TrackedEntity {

        final ServerEntity serverEntity;
        final Entity entity;
        private final int range;
        SectionPos lastSectionPos;
        public final Set<ServerPlayerConnection> seenBy = new ReferenceOpenHashSet<>(); // Paper - optimise map impl

        public TrackedEntity(Entity entity, int i, int j, boolean flag) {
            this.serverEntity = new ServerEntity(ChunkMap.this.level, entity, j, flag, this::broadcast, this.seenBy); // CraftBukkit
            this.entity = entity;
            this.range = i;
            this.lastSectionPos = SectionPos.of(entity);
        }

        // Paper start - use distance map to optimise tracker
        com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> lastTrackerCandidates;

        final void updatePlayers(com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> newTrackerCandidates) {
            com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> oldTrackerCandidates = this.lastTrackerCandidates;
            this.lastTrackerCandidates = newTrackerCandidates;

            if (newTrackerCandidates != null) {
                Object[] rawData = newTrackerCandidates.getBackingSet();
                for (int i = 0, len = rawData.length; i < len; ++i) {
                    Object raw = rawData[i];
                    if (!(raw instanceof ServerPlayer)) {
                        continue;
                    }
                    ServerPlayer player = (ServerPlayer)raw;
                    this.updatePlayer(player);
                }
            }

            if (oldTrackerCandidates == newTrackerCandidates) {
                // this is likely the case.
                // means there has been no range changes, so we can just use the above for tracking.
                return;
            }

            // stuff could have been removed, so we need to check the trackedPlayers set
            // for players that were removed

            for (ServerPlayerConnection conn : this.seenBy.toArray(new ServerPlayerConnection[0])) { // avoid CME
                if (newTrackerCandidates == null || !newTrackerCandidates.contains(conn.getPlayer())) {
                    this.updatePlayer(conn.getPlayer());
                }
            }
        }
        // Paper end - use distance map to optimise tracker

        public boolean equals(Object object) {
            return object instanceof ChunkMap.TrackedEntity ? ((ChunkMap.TrackedEntity) object).entity.getId() == this.entity.getId() : false;
        }

        public int hashCode() {
            return this.entity.getId();
        }

        public void broadcast(Packet<?> packet) {
            Iterator iterator = this.seenBy.iterator();

            while (iterator.hasNext()) {
                ServerPlayerConnection serverplayerconnection = (ServerPlayerConnection) iterator.next();

                serverplayerconnection.send(packet);
            }

        }

        public void broadcastAndSend(Packet<?> packet) {
            this.broadcast(packet);
            if (this.entity instanceof ServerPlayer) {
                ((ServerPlayer) this.entity).connection.send(packet);
            }

        }

        public void broadcastRemoved() {
            Iterator iterator = this.seenBy.iterator();

            while (iterator.hasNext()) {
                ServerPlayerConnection serverplayerconnection = (ServerPlayerConnection) iterator.next();

                this.serverEntity.removePairing(serverplayerconnection.getPlayer());
            }

        }

        public void removePlayer(ServerPlayer player) {
            org.spigotmc.AsyncCatcher.catchOp("player tracker clear"); // Spigot
            if (this.seenBy.remove(player.connection)) {
                this.serverEntity.removePairing(player);
            }

        }

        public void updatePlayer(ServerPlayer player) {
            org.spigotmc.AsyncCatcher.catchOp("player tracker update"); // Spigot
            if (player != this.entity) {
                // Paper start - remove allocation of Vec3D here
                //Vec3 vec3d = player.position().subtract(this.entity.position()); // MC-155077, SPIGOT-5113
                double vec3d_dx = player.getX() - this.entity.getX();
                double vec3d_dz = player.getZ() - this.entity.getZ();
                // Paper end - remove allocation of Vec3D here
                double d0 = (double) Math.min(this.getEffectiveRange(), io.papermc.paper.chunk.PlayerChunkLoader.getSendViewDistance(player) * 16); // Paper - per player view distance
                double d1 = vec3d_dx * vec3d_dx + vec3d_dz * vec3d_dz; // Paper
                double d2 = d0 * d0;
                boolean flag = d1 <= d2 && this.entity.broadcastToPlayer(player);

                // CraftBukkit start - respect vanish API
                if (!player.getBukkitEntity().canSee(this.entity.getBukkitEntity())) {
                    flag = false;
                }
                // CraftBukkit end
                if (flag) {
                    if (this.seenBy.add(player.connection)) {
                        this.serverEntity.addPairing(player);
                    }
                } else if (this.seenBy.remove(player.connection)) {
                    this.serverEntity.removePairing(player);
                }

            }
        }

        private int scaledRange(int initialDistance) {
            return ChunkMap.this.level.getServer().getScaledTrackingDistance(initialDistance);
        }

        private int getEffectiveRange() {
            int i = this.range;
            Iterator iterator = this.entity.getIndirectPassengers().iterator();

            while (iterator.hasNext()) {
                Entity entity = (Entity) iterator.next();
                int j = entity.getType().clientTrackingRange() * 16;
                j = org.spigotmc.TrackingRange.getEntityTrackingRange(entity, j); // Paper

                if (j > i) {
                    i = j;
                }
            }

            return this.scaledRange(i);
        }

        public void updatePlayers(List<ServerPlayer> players) {
            Iterator iterator = players.iterator();

            while (iterator.hasNext()) {
                ServerPlayer entityplayer = (ServerPlayer) iterator.next();

                this.updatePlayer(entityplayer);
            }

        }
    }
}
