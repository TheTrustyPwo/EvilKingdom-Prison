package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.destroystokyo.paper.exception.ServerInternalException;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.EuclideanGameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public class LevelChunk extends ChunkAccess {

    static final Logger LOGGER = LogUtils.getLogger();
    private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
        @Override
        public void tick() {}

        @Override
        public boolean isRemoved() {
            return true;
        }

        @Override
        public BlockPos getPos() {
            return BlockPos.ZERO;
        }

        @Override
        public String getType() {
            return "<null>";
        }
    };
    private final Map<BlockPos, LevelChunk.RebindableTickingBlockEntityWrapper> tickersInLevel;
    public boolean loaded;
    private boolean clientLightReady;
    public final ServerLevel level; // CraftBukkit - type
    @Nullable
    private Supplier<ChunkHolder.FullChunkStatus> fullStatus;
    @Nullable
    private LevelChunk.PostLoadProcessor postLoad;
    private final Int2ObjectMap<GameEventDispatcher> gameEventDispatcherSections;
    private final LevelChunkTicks<Block> blockTicks;
    private final LevelChunkTicks<Fluid> fluidTicks;
    // Paper start - track last save time
    public long lastSaveTime;
    public void setLastSaved(long ticks) {
        this.lastSaveTime = ticks;
    }
    // Paper end

    public LevelChunk(Level world, ChunkPos pos) {
        this(world, pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, (LevelChunkSection[]) null, (LevelChunk.PostLoadProcessor) null, (BlendingData) null);
    }

    public LevelChunk(Level world, ChunkPos pos, UpgradeData upgradeData, LevelChunkTicks<Block> blockTickScheduler, LevelChunkTicks<Fluid> fluidTickScheduler, long inhabitedTime, @Nullable LevelChunkSection[] sectionArrayInitializer, @Nullable LevelChunk.PostLoadProcessor entityLoader, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, world, net.minecraft.server.MinecraftServer.getServer().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), inhabitedTime, sectionArrayInitializer, blendingData); // Paper - Anti-Xray - The world isnt ready yet, use server singleton for registry
        // Paper start - rewrite light engine
        this.setBlockNibbles(ca.spottedleaf.starlight.common.light.StarLightEngine.getFilledEmptyLight(world));
        this.setSkyNibbles(ca.spottedleaf.starlight.common.light.StarLightEngine.getFilledEmptyLight(world));
        // Paper end - rewrite light engine
        this.tickersInLevel = Maps.newHashMap();
        this.clientLightReady = false;
        this.level = (ServerLevel) world; // CraftBukkit - type
        this.gameEventDispatcherSections = new Int2ObjectOpenHashMap();
        Heightmap.Types[] aheightmap_type = Heightmap.Types.values();
        int j = aheightmap_type.length;

        for (int k = 0; k < j; ++k) {
            Heightmap.Types heightmap_type = aheightmap_type[k];

            if (ChunkStatus.FULL.heightmapsAfter().contains(heightmap_type)) {
                this.heightmaps.put(heightmap_type, new Heightmap(this, heightmap_type));
            }
        }

        this.postLoad = entityLoader;
        this.blockTicks = blockTickScheduler;
        this.fluidTicks = fluidTickScheduler;
        // CraftBukkit start
        this.bukkitChunk = new org.bukkit.craftbukkit.v1_18_R2.CraftChunk(this);
    }

    public org.bukkit.Chunk bukkitChunk;
    public org.bukkit.Chunk getBukkitChunk() {
        return this.bukkitChunk;
    }

    public boolean mustNotSave;
    public boolean needsDecoration;

    // CraftBukkit end

    // Paper start
    public final com.destroystokyo.paper.util.maplist.EntityList entities = new com.destroystokyo.paper.util.maplist.EntityList();
    public @Nullable ChunkHolder playerChunk;

    static final int NEIGHBOUR_CACHE_RADIUS = 3;
    public static int getNeighbourCacheRadius() {
        return NEIGHBOUR_CACHE_RADIUS;
    }

    boolean loadedTicketLevel; public final boolean wasLoadCallbackInvoked() { return this.loadedTicketLevel; } // Paper - public accessor
    private long neighbourChunksLoadedBitset;
    private final LevelChunk[] loadedNeighbourChunks = new LevelChunk[(NEIGHBOUR_CACHE_RADIUS * 2 + 1) * (NEIGHBOUR_CACHE_RADIUS * 2 + 1)];

    private static int getNeighbourIndex(final int relativeX, final int relativeZ) {
        // index = (relativeX + NEIGHBOUR_CACHE_RADIUS) + (relativeZ + NEIGHBOUR_CACHE_RADIUS) * (NEIGHBOUR_CACHE_RADIUS * 2 + 1)
        // optimised variant of the above by moving some of the ops to compile time
        return relativeX + (relativeZ * (NEIGHBOUR_CACHE_RADIUS * 2 + 1)) + (NEIGHBOUR_CACHE_RADIUS + NEIGHBOUR_CACHE_RADIUS * ((NEIGHBOUR_CACHE_RADIUS * 2 + 1)));
    }

    public final LevelChunk getRelativeNeighbourIfLoaded(final int relativeX, final int relativeZ) {
        return this.loadedNeighbourChunks[getNeighbourIndex(relativeX, relativeZ)];
    }

    public final boolean isNeighbourLoaded(final int relativeX, final int relativeZ) {
        return (this.neighbourChunksLoadedBitset & (1L << getNeighbourIndex(relativeX, relativeZ))) != 0;
    }

    public final void setNeighbourLoaded(final int relativeX, final int relativeZ, final LevelChunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk must be non-null, neighbour: (" + relativeX + "," + relativeZ + "), chunk: " + this.chunkPos);
        }
        final long before = this.neighbourChunksLoadedBitset;
        final int index = getNeighbourIndex(relativeX, relativeZ);
        this.loadedNeighbourChunks[index] = chunk;
        this.neighbourChunksLoadedBitset |= (1L << index);
        this.onNeighbourChange(before, this.neighbourChunksLoadedBitset);
    }

    public final void setNeighbourUnloaded(final int relativeX, final int relativeZ) {
        final long before = this.neighbourChunksLoadedBitset;
        final int index = getNeighbourIndex(relativeX, relativeZ);
        this.loadedNeighbourChunks[index] = null;
        this.neighbourChunksLoadedBitset &= ~(1L << index);
        this.onNeighbourChange(before, this.neighbourChunksLoadedBitset);
    }

    public final void resetNeighbours() {
        final long before = this.neighbourChunksLoadedBitset;
        this.neighbourChunksLoadedBitset = 0L;
        java.util.Arrays.fill(this.loadedNeighbourChunks, null);
        this.onNeighbourChange(before, 0L);
    }

    protected void onNeighbourChange(final long bitsetBefore, final long bitsetAfter) {

        // Paper start - no-tick view distance
        ServerChunkCache chunkProviderServer = ((ServerLevel)this.level).getChunkSource();
        net.minecraft.server.level.ChunkMap chunkMap = chunkProviderServer.chunkMap;
        // this code handles the addition of ticking tickets - the distance map handles the removal
        if (!areNeighboursLoaded(bitsetBefore, 2) && areNeighboursLoaded(bitsetAfter, 2)) {
            if (chunkMap.playerChunkManager.tickMap.getObjectsInRange(this.coordinateKey) != null) { // Paper - replace old player chunk loading system
                // now we're ready for entity ticking
                chunkProviderServer.mainThreadProcessor.execute(() -> {
                    // double check that this condition still holds.
                    if (LevelChunk.this.areNeighboursLoaded(2) && chunkMap.playerChunkManager.tickMap.getObjectsInRange(LevelChunk.this.coordinateKey) != null) { // Paper - replace old player chunk loading system
                        chunkMap.playerChunkManager.onChunkPlayerTickReady(this.chunkPos.x, this.chunkPos.z); // Paper - replace old player chunk
                        chunkProviderServer.addTicketAtLevel(net.minecraft.server.level.TicketType.PLAYER, LevelChunk.this.chunkPos, 31, LevelChunk.this.chunkPos); // 31 -> entity ticking, TODO check on update
                    }
                });
            }
        }

        // this code handles the chunk sending
        if (!areNeighboursLoaded(bitsetBefore, 1) && areNeighboursLoaded(bitsetAfter, 1)) {
            // Paper start - replace old player chunk loading system
            if (chunkMap.playerChunkManager.isChunkNearPlayers(this.chunkPos.x, this.chunkPos.z)) {
                // the post processing is expensive, so we don't want to run it unless we're actually near
                // a player.
                chunkProviderServer.mainThreadProcessor.execute(() -> {
                    if (!LevelChunk.this.areNeighboursLoaded(1)) {
                        return;
                    }
                    LevelChunk.this.postProcessGeneration();
                    if (!LevelChunk.this.areNeighboursLoaded(1)) {
                        return;
                    }
                    chunkMap.playerChunkManager.onChunkSendReady(this.chunkPos.x, this.chunkPos.z);
                });
            }
            // Paper end - replace old player chunk loading system
        }
        // Paper end - no-tick view distance
    }

    public final boolean isAnyNeighborsLoaded() {
        return neighbourChunksLoadedBitset != 0;
    }
    public final boolean areNeighboursLoaded(final int radius) {
        return LevelChunk.areNeighboursLoaded(this.neighbourChunksLoadedBitset, radius);
    }

    public static boolean areNeighboursLoaded(final long bitset, final int radius) {
        // index = relativeX + (relativeZ * (NEIGHBOUR_CACHE_RADIUS * 2 + 1)) + (NEIGHBOUR_CACHE_RADIUS + NEIGHBOUR_CACHE_RADIUS * ((NEIGHBOUR_CACHE_RADIUS * 2 + 1)))
        switch (radius) {
            case 0: {
                return (bitset & (1L << getNeighbourIndex(0, 0))) != 0;
            }
            case 1: {
                long mask = 0L;
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        mask |= (1L << getNeighbourIndex(dx, dz));
                    }
                }
                return (bitset & mask) == mask;
            }
            case 2: {
                long mask = 0L;
                for (int dx = -2; dx <= 2; ++dx) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        mask |= (1L << getNeighbourIndex(dx, dz));
                    }
                }
                return (bitset & mask) == mask;
            }
            case 3: {
                long mask = 0L;
                for (int dx = -3; dx <= 3; ++dx) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        mask |= (1L << getNeighbourIndex(dx, dz));
                    }
                }
                return (bitset & mask) == mask;
            }

            default:
                throw new IllegalArgumentException("Radius not recognized: " + radius);
        }
    }
    // Paper end
    // Paper start - optimise checkDespawn
    private boolean playerGeneralAreaCacheSet;
    private com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<net.minecraft.server.level.ServerPlayer> playerGeneralAreaCache;

    public com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<net.minecraft.server.level.ServerPlayer> getPlayerGeneralAreaCache() {
        if (!this.playerGeneralAreaCacheSet) {
            this.updateGeneralAreaCache();
        }
        return this.playerGeneralAreaCache;
    }

    public void updateGeneralAreaCache() {
        this.updateGeneralAreaCache(((ServerLevel)this.level).getChunkSource().chunkMap.playerGeneralAreaMap.getObjectsInRange(this.coordinateKey));
    }

    public void removeGeneralAreaCache() {
        this.playerGeneralAreaCacheSet = false;
        this.playerGeneralAreaCache = null;
    }

    public void updateGeneralAreaCache(com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<net.minecraft.server.level.ServerPlayer> value) {
        this.playerGeneralAreaCacheSet = true;
        this.playerGeneralAreaCache = value;
    }

    public net.minecraft.server.level.ServerPlayer findNearestPlayer(double sourceX, double sourceY, double sourceZ,
                                                                     double maxRange, java.util.function.Predicate<Entity> predicate) {
        if (!this.playerGeneralAreaCacheSet) {
            this.updateGeneralAreaCache();
        }

        com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<net.minecraft.server.level.ServerPlayer> nearby = this.playerGeneralAreaCache;

        if (nearby == null) {
            return null;
        }

        Object[] backingSet = nearby.getBackingSet();
        double closestDistance = maxRange < 0.0 ? Double.MAX_VALUE : maxRange * maxRange;
        net.minecraft.server.level.ServerPlayer closest = null;
        for (int i = 0, len = backingSet.length; i < len; ++i) {
            Object _player = backingSet[i];
            if (!(_player instanceof net.minecraft.server.level.ServerPlayer)) {
                continue;
            }
            net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer)_player;

            double distance = player.distanceToSqr(sourceX, sourceY, sourceZ);
            if (distance < closestDistance && predicate.test(player)) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }

    public void getNearestPlayers(double sourceX, double sourceY, double sourceZ, java.util.function.Predicate<Entity> predicate,
                                  double range, java.util.List<net.minecraft.server.level.ServerPlayer> ret) {
        if (!this.playerGeneralAreaCacheSet) {
            this.updateGeneralAreaCache();
        }

        com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<net.minecraft.server.level.ServerPlayer> nearby = this.playerGeneralAreaCache;

        if (nearby == null) {
            return;
        }

        double rangeSquared = range * range;

        Object[] backingSet = nearby.getBackingSet();
        for (int i = 0, len = backingSet.length; i < len; ++i) {
            Object _player = backingSet[i];
            if (!(_player instanceof net.minecraft.server.level.ServerPlayer)) {
                continue;
            }
            net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer)_player;

            if (range >= 0.0) {
                double distanceSquared = player.distanceToSqr(sourceX, sourceY, sourceZ);
                if (distanceSquared > rangeSquared) {
                    continue;
                }
            }

            if (predicate == null || predicate.test(player)) {
                ret.add(player);
            }
        }
    }
    // Paper end - optimise checkDespawn

    public LevelChunk(ServerLevel world, ProtoChunk protoChunk, @Nullable LevelChunk.PostLoadProcessor entityLoader) {
        this(world, protoChunk.getPos(), protoChunk.getUpgradeData(), protoChunk.unpackBlockTicks(), protoChunk.unpackFluidTicks(), protoChunk.getInhabitedTime(), protoChunk.getSections(), entityLoader, protoChunk.getBlendingData());
        // Paper start - rewrite light engine
        this.setBlockNibbles(protoChunk.getBlockNibbles());
        this.setSkyNibbles(protoChunk.getSkyNibbles());
        this.setSkyEmptinessMap(protoChunk.getSkyEmptinessMap());
        this.setBlockEmptinessMap(protoChunk.getBlockEmptinessMap());
        // Paper end - rewrite light engine
        Iterator iterator = protoChunk.getBlockEntities().values().iterator();

        while (iterator.hasNext()) {
            BlockEntity tileentity = (BlockEntity) iterator.next();

            this.setBlockEntity(tileentity);
        }

        this.pendingBlockEntities.putAll(protoChunk.getBlockEntityNbts());

        for (int i = 0; i < protoChunk.getPostProcessing().length; ++i) {
            this.postProcessing[i] = protoChunk.getPostProcessing()[i];
        }

        this.setAllStarts(protoChunk.getAllStarts());
        this.setAllReferences(protoChunk.getAllReferences());
        iterator = protoChunk.getHeightmaps().iterator();

        while (iterator.hasNext()) {
            Entry<Heightmap.Types, Heightmap> entry = (Entry) iterator.next();

            if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
                this.setHeightmap((Heightmap.Types) entry.getKey(), ((Heightmap) entry.getValue()).getRawData());
            }
        }

        this.setLightCorrect(protoChunk.isLightCorrect());
        this.unsaved = true;
        this.needsDecoration = true; // CraftBukkit
        // CraftBukkit start
        this.persistentDataContainer = protoChunk.persistentDataContainer; // SPIGOT-6814: copy PDC to account for 1.17 to 1.18 chunk upgrading.
        // CraftBukkit end
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess.TicksToSave getTicksForSerialization() {
        return new ChunkAccess.TicksToSave(this.blockTicks, this.fluidTicks);
    }

    // Paper start
    @Override
    public long getInhabitedTime() {
        return this.level.paperConfig.fixedInhabitedTime < 0 ? super.getInhabitedTime() : this.level.paperConfig.fixedInhabitedTime;
    }
    // Paper end

    @Override
    public GameEventDispatcher getEventDispatcher(int ySectionCoord) {
        return (GameEventDispatcher) this.gameEventDispatcherSections.computeIfAbsent(ySectionCoord, (j) -> {
            return new EuclideanGameEventDispatcher(this.level);
        });
    }

    // Paper start - Optimize getBlockData to reduce instructions
    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockStateFinal(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public BlockState getBlockState(final int x, final int y, final int z) {
        return this.getBlockStateFinal(x, y, z);
    }
    public final BlockState getBlockStateFinal(final int x, final int y, final int z) {
        // Method body / logic copied from below
        final int i = this.getSectionIndex(y);
        if (i < 0 || i >= this.sections.length || this.sections[i].nonEmptyBlockCount == 0 || this.sections[i].hasOnlyAir()) {
            return Blocks.AIR.defaultBlockState();
        }
        // Inlined ChunkSection.getType() and DataPaletteBlock.a(int,int,int)
        return this.sections[i].states.get((y & 15) << 8 | (z & 15) << 4 | x & 15);

    }

    public BlockState getBlockState_unused(int i, int j, int k) {
        // Paper end
        if (this.level.isDebug()) {
            BlockState iblockdata = null;

            if (j == 60) {
                iblockdata = Blocks.BARRIER.defaultBlockState();
            }

            if (j == 70) {
                iblockdata = DebugLevelSource.getBlockStateFor(i, k);
            }

            return iblockdata == null ? Blocks.AIR.defaultBlockState() : iblockdata;
        } else {
            try {
                int l = this.getSectionIndex(j);

                if (l >= 0 && l < this.sections.length) {
                    LevelChunkSection chunksection = this.sections[l];

                    if (!chunksection.hasOnlyAir()) {
                        return chunksection.getBlockState(i & 15, j & 15, k & 15);
                    }
                }

                return Blocks.AIR.defaultBlockState();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting block state");
                CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Block being got");

                crashreportsystemdetails.setDetail("Location", () -> {
                    return CrashReportCategory.formatLocation(this, i, j, k);
                });
                throw new ReportedException(crashreport);
            }
        }
    }

    // Paper start - If loaded util
    @Override
    public final FluidState getFluidIfLoaded(BlockPos blockposition) {
        return this.getFluidState(blockposition);
    }

    @Override
    public final BlockState getBlockStateIfLoaded(BlockPos blockposition) {
        return this.getBlockState(blockposition);
    }
    // Paper end

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
    }

    public FluidState getFluidState(int x, int y, int z) {
        // try {  // Paper - remove try catch
        // Paper start - reduce the number of ops in this call
        int index = this.getSectionIndex(y);
            if (index >= 0 && index < this.sections.length) {
                LevelChunkSection chunksection = this.sections[index];

                if (!chunksection.hasOnlyAir()) {
                    return chunksection.states.get((y & 15) << 8 | (z & 15) << 4 | x & 15).getFluidState();
                    // Paper end
                }
            }

            return Fluids.EMPTY.defaultFluidState();
        /* // Paper - remove try catch
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting fluid state");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Block being got");

            crashreportsystemdetails.setDetail("Location", () -> {
                return CrashReportCategory.formatLocation(this, x, y, z);
            });
            throw new ReportedException(crashreport);
        }
         */  // Paper - remove try catch
    }

    // CraftBukkit start
    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        return this.setBlockState(pos, state, moved, true);
    }

    @Nullable
    public BlockState setBlockState(BlockPos blockposition, BlockState iblockdata, boolean flag, boolean doPlace) {
        // CraftBukkit end
        int i = blockposition.getY();
        LevelChunkSection chunksection = this.getSection(this.getSectionIndex(i));
        boolean flag1 = chunksection.hasOnlyAir();

        if (flag1 && iblockdata.isAir()) {
            return null;
        } else {
            int j = blockposition.getX() & 15;
            int k = i & 15;
            int l = blockposition.getZ() & 15;
            BlockState iblockdata1 = chunksection.setBlockState(j, k, l, iblockdata);

            if (iblockdata1 == iblockdata) {
                return null;
            } else {
                Block block = iblockdata.getBlock();

                ((Heightmap) this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING)).update(j, i, l, iblockdata);
                ((Heightmap) this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)).update(j, i, l, iblockdata);
                ((Heightmap) this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR)).update(j, i, l, iblockdata);
                ((Heightmap) this.heightmaps.get(Heightmap.Types.WORLD_SURFACE)).update(j, i, l, iblockdata);
                boolean flag2 = chunksection.hasOnlyAir();

                if (flag1 != flag2) {
                    this.level.getChunkSource().getLightEngine().updateSectionStatus(blockposition, flag2);
                }

                boolean flag3 = iblockdata1.hasBlockEntity();

                if (!this.level.isClientSide) {
                    iblockdata1.onRemove(this.level, blockposition, iblockdata, flag);
                } else if (!iblockdata1.is(block) && flag3) {
                    this.removeBlockEntity(blockposition);
                }

                if (!chunksection.getBlockState(j, k, l).is(block)) {
                    return null;
                } else {
                    // CraftBukkit - Don't place while processing the BlockPlaceEvent, unless it's a BlockContainer. Prevents blocks such as TNT from activating when cancelled.
                    if (!this.level.isClientSide && doPlace && (!this.level.captureBlockStates || block instanceof net.minecraft.world.level.block.BaseEntityBlock)) {
                        iblockdata.onPlace(this.level, blockposition, iblockdata1, flag);
                    }

                    if (iblockdata.hasBlockEntity()) {
                        BlockEntity tileentity = this.getBlockEntity(blockposition, LevelChunk.EntityCreationType.CHECK);

                        if (tileentity == null) {
                            tileentity = ((EntityBlock) block).newBlockEntity(blockposition, iblockdata);
                            if (tileentity != null) {
                                this.addAndRegisterBlockEntity(tileentity);
                            }
                        } else {
                            tileentity.setBlockState(iblockdata);
                            this.updateBlockEntityTicker(tileentity);
                        }
                    }

                    this.unsaved = true;
                    return iblockdata1;
                }
            }
        }
    }

    /** @deprecated */
    @Deprecated
    @Override
    public void addEntity(Entity entity) {}

    @Nullable
    private BlockEntity createBlockEntity(BlockPos pos) {
        BlockState iblockdata = this.getBlockState(pos);

        return !iblockdata.hasBlockEntity() ? null : ((EntityBlock) iblockdata.getBlock()).newBlockEntity(pos, iblockdata);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
    }

    @Deprecated @Nullable public final BlockEntity getTileEntityImmediately(BlockPos pos) { return this.getBlockEntity(pos, EntityCreationType.IMMEDIATE); } // Paper - OBFHELPER
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationType) {
        // CraftBukkit start
        BlockEntity tileentity = level.capturedTileEntities.get(pos);
        if (tileentity == null) {
            tileentity = (BlockEntity) this.blockEntities.get(pos);
        }
        // CraftBukkit end

        if (tileentity == null) {
            CompoundTag nbttagcompound = (CompoundTag) this.pendingBlockEntities.remove(pos);

            if (nbttagcompound != null) {
                BlockEntity tileentity1 = this.promotePendingBlockEntity(pos, nbttagcompound);

                if (tileentity1 != null) {
                    return tileentity1;
                }
            }
        }

        if (tileentity == null) {
            if (creationType == LevelChunk.EntityCreationType.IMMEDIATE) {
                tileentity = this.createBlockEntity(pos);
                if (tileentity != null) {
                    this.addAndRegisterBlockEntity(tileentity);
                }
            }
        } else if (tileentity.isRemoved()) {
            this.blockEntities.remove(pos);
            return null;
        }

        return tileentity;
    }

    public void addAndRegisterBlockEntity(BlockEntity blockEntity) {
        this.setBlockEntity(blockEntity);
        if (this.isInLevel()) {
            this.addGameEventListener(blockEntity);
            this.updateBlockEntityTicker(blockEntity);
        }

    }

    private boolean isInLevel() {
        return this.loaded || this.level.isClientSide();
    }

    boolean isTicking(BlockPos pos) {
        if (!this.level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        } else {
            Level world = this.level;

            if (!(world instanceof ServerLevel)) {
                return true;
            } else {
                ServerLevel worldserver = (ServerLevel) world;

                return this.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING) && worldserver.areEntitiesLoaded(ChunkPos.asLong(pos));
            }
        }
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos blockposition = blockEntity.getBlockPos();

        if (this.getBlockState(blockposition).hasBlockEntity()) {
            blockEntity.setLevel(this.level);
            blockEntity.clearRemoved();
            BlockEntity tileentity1 = (BlockEntity) this.blockEntities.put(blockposition.immutable(), blockEntity);

            if (tileentity1 != null && tileentity1 != blockEntity) {
                tileentity1.setRemoved();
            }

            // CraftBukkit start
        } else {
            // Paper start
            ServerInternalException e = new ServerInternalException(
                "Attempted to place a tile entity (" + blockEntity + ") at " + blockEntity.getBlockPos().getX() + ","
                    + blockEntity.getBlockPos().getY() + "," + blockEntity.getBlockPos().getZ()
                    + " (" + getBlockState(blockposition) + ") where there was no entity tile!\n" +
                    "Chunk coordinates: " + (this.chunkPos.x * 16) + "," + (this.chunkPos.z * 16));
            e.printStackTrace();
            ServerInternalException.reportInternalException(e);
            // Paper end
            // CraftBukkit end
        }
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) {
        BlockEntity tileentity = this.getBlockEntity(pos);
        CompoundTag nbttagcompound;

        if (tileentity != null && !tileentity.isRemoved()) {
            nbttagcompound = tileentity.saveWithFullMetadata();
            nbttagcompound.putBoolean("keepPacked", false);
            return nbttagcompound;
        } else {
            nbttagcompound = (CompoundTag) this.pendingBlockEntities.get(pos);
            if (nbttagcompound != null) {
                nbttagcompound = nbttagcompound.copy();
                nbttagcompound.putBoolean("keepPacked", true);
            }

            return nbttagcompound;
        }
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        if (this.isInLevel()) {
            BlockEntity tileentity = (BlockEntity) this.blockEntities.remove(pos);

            // CraftBukkit start - SPIGOT-5561: Also remove from pending map
            if (!pendingBlockEntities.isEmpty()) {
                pendingBlockEntities.remove(pos);
            }
            // CraftBukkit end

            if (tileentity != null) {
                this.removeGameEventListener(tileentity);
                tileentity.setRemoved();
            }
        }

        this.removeBlockEntityTicker(pos);
    }

    private <T extends BlockEntity> void removeGameEventListener(T blockEntity) {
        if (!this.level.isClientSide) {
            Block block = blockEntity.getBlockState().getBlock();

            if (block instanceof EntityBlock) {
                GameEventListener gameeventlistener = ((EntityBlock) block).getListener(this.level, blockEntity);

                if (gameeventlistener != null) {
                    int i = SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY());
                    GameEventDispatcher gameeventdispatcher = this.getEventDispatcher(i);

                    gameeventdispatcher.unregister(gameeventlistener);
                    if (gameeventdispatcher.isEmpty()) {
                        this.gameEventDispatcherSections.remove(i);
                    }
                }
            }

        }
    }

    private void removeBlockEntityTicker(BlockPos pos) {
        LevelChunk.RebindableTickingBlockEntityWrapper chunk_d = (LevelChunk.RebindableTickingBlockEntityWrapper) this.tickersInLevel.remove(pos);

        if (chunk_d != null) {
            chunk_d.rebind(LevelChunk.NULL_TICKER);
        }

    }

    public void runPostLoad() {
        if (this.postLoad != null) {
            this.postLoad.run(this);
            this.postLoad = null;
        }

    }

    // CraftBukkit start
    public void loadCallback() {
        if (this.loadedTicketLevel) { LOGGER.error("Double calling chunk load!", new Throwable()); } // Paper
        // Paper start - neighbour cache
        int chunkX = this.chunkPos.x;
        int chunkZ = this.chunkPos.z;
        ServerChunkCache chunkProvider = this.level.getChunkSource();
        for (int dx = -NEIGHBOUR_CACHE_RADIUS; dx <= NEIGHBOUR_CACHE_RADIUS; ++dx) {
            for (int dz = -NEIGHBOUR_CACHE_RADIUS; dz <= NEIGHBOUR_CACHE_RADIUS; ++dz) {
                LevelChunk neighbour = chunkProvider.getChunkAtIfLoadedMainThreadNoCache(chunkX + dx, chunkZ + dz);
                if (neighbour != null) {
                    neighbour.setNeighbourLoaded(-dx, -dz, this);
                    // should be in cached already
                    this.setNeighbourLoaded(dx, dz, neighbour);
                }
            }
        }
        this.setNeighbourLoaded(0, 0, this);
        this.loadedTicketLevel = true;
        // Paper end - neighbour cache
        org.bukkit.Server server = this.level.getCraftServer();
        this.level.getChunkSource().addLoadedChunk(this); // Paper
        ((ServerLevel)this.level).getChunkSource().chunkMap.playerChunkManager.onChunkLoad(this.chunkPos.x, this.chunkPos.z); // Paper - rewrite player chunk management
        if (server != null) {
            /*
             * If it's a new world, the first few chunks are generated inside
             * the World constructor. We can't reliably alter that, so we have
             * no way of creating a CraftWorld/CraftServer at that point.
             */
            server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(this.bukkitChunk, this.needsDecoration));

            if (this.needsDecoration) {
                try (co.aikar.timings.Timing ignored = this.level.timings.chunkLoadPopulate.startTiming()) { // Paper
                this.needsDecoration = false;
                java.util.Random random = new java.util.Random();
                random.setSeed(this.level.getSeed());
                long xRand = random.nextLong() / 2L * 2L + 1L;
                long zRand = random.nextLong() / 2L * 2L + 1L;
                random.setSeed((long) this.chunkPos.x * xRand + (long) this.chunkPos.z * zRand ^ this.level.getSeed());

                org.bukkit.World world = this.level.getWorld();
                if (world != null) {
                    this.level.populating = true;
                    try {
                        for (org.bukkit.generator.BlockPopulator populator : world.getPopulators()) {
                            populator.populate(world, random, bukkitChunk);
                        }
                    } finally {
                        this.level.populating = false;
                    }
                }
                server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkPopulateEvent(this.bukkitChunk));
                } // Paper
            }
        }
    }

    public void unloadCallback() {
        if (!this.loadedTicketLevel) { LOGGER.error("Double calling chunk unload!", new Throwable()); } // Paper
        org.bukkit.Server server = this.level.getCraftServer();
        org.bukkit.event.world.ChunkUnloadEvent unloadEvent = new org.bukkit.event.world.ChunkUnloadEvent(this.bukkitChunk, this.isUnsaved());
        server.getPluginManager().callEvent(unloadEvent);
        // note: saving can be prevented, but not forced if no saving is actually required
        this.mustNotSave = !unloadEvent.isSaveChunk();
        this.level.getChunkSource().removeLoadedChunk(this); // Paper
        // Paper start - neighbour cache
        int chunkX = this.chunkPos.x;
        int chunkZ = this.chunkPos.z;
        ServerChunkCache chunkProvider = this.level.getChunkSource();
        for (int dx = -NEIGHBOUR_CACHE_RADIUS; dx <= NEIGHBOUR_CACHE_RADIUS; ++dx) {
            for (int dz = -NEIGHBOUR_CACHE_RADIUS; dz <= NEIGHBOUR_CACHE_RADIUS; ++dz) {
                LevelChunk neighbour = chunkProvider.getChunkAtIfLoadedMainThreadNoCache(chunkX + dx, chunkZ + dz);
                if (neighbour != null) {
                    neighbour.setNeighbourUnloaded(-dx, -dz);
                }
            }
        }
        this.loadedTicketLevel = false;
        this.resetNeighbours();
        // Paper end
    }

    @Override
    public boolean isUnsaved() {
        return super.isUnsaved() && !this.mustNotSave;
    }
    // CraftBukkit end

    public boolean isEmpty() {
        return false;
    }

    public void replaceWithPacketData(FriendlyByteBuf buf, CompoundTag nbt, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer) {
        this.clearAllBlockEntities();
        LevelChunkSection[] achunksection = this.sections;
        int i = achunksection.length;

        int j;

        for (j = 0; j < i; ++j) {
            LevelChunkSection chunksection = achunksection[j];

            chunksection.read(buf);
        }

        Heightmap.Types[] aheightmap_type = Heightmap.Types.values();

        i = aheightmap_type.length;

        for (j = 0; j < i; ++j) {
            Heightmap.Types heightmap_type = aheightmap_type[j];
            String s = heightmap_type.getSerializationKey();

            if (nbt.contains(s, 12)) {
                this.setHeightmap(heightmap_type, nbt.getLongArray(s));
            }
        }

        consumer.accept((blockposition, tileentitytypes, nbttagcompound1) -> {
            BlockEntity tileentity = this.getBlockEntity(blockposition, LevelChunk.EntityCreationType.IMMEDIATE);

            if (tileentity != null && nbttagcompound1 != null && tileentity.getType() == tileentitytypes) {
                tileentity.load(nbttagcompound1);
            }

        });
    }

    public void setLoaded(boolean loadedToWorld) {
        this.loaded = loadedToWorld;
    }

    public Level getLevel() {
        return this.level;
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    @Override
    public Stream<BlockPos> getLights() {
        return StreamSupport.stream(BlockPos.betweenClosed(this.chunkPos.getMinBlockX(), this.getMinBuildHeight(), this.chunkPos.getMinBlockZ(), this.chunkPos.getMaxBlockX(), this.getMaxBuildHeight() - 1, this.chunkPos.getMaxBlockZ()).spliterator(), false).filter((blockposition) -> {
            return this.getBlockState(blockposition).getLightEmission() != 0;
        });
    }

    public boolean isPostProcessingDone; // Paper - replace chunk loader system

    public void postProcessGeneration() {
        try { // Paper - replace chunk loader system
        ChunkPos chunkcoordintpair = this.getPos();

        for (int i = 0; i < this.postProcessing.length; ++i) {
            if (this.postProcessing[i] != null) {
                ShortListIterator shortlistiterator = this.postProcessing[i].iterator();

                while (shortlistiterator.hasNext()) {
                    Short oshort = (Short) shortlistiterator.next();
                    BlockPos blockposition = ProtoChunk.unpackOffsetCoordinates(oshort, this.getSectionYFromSectionIndex(i), chunkcoordintpair);
                    BlockState iblockdata = this.getBlockState(blockposition);
                    FluidState fluid = iblockdata.getFluidState();

                    if (!fluid.isEmpty()) {
                        fluid.tick(this.level, blockposition);
                    }

                    if (!(iblockdata.getBlock() instanceof LiquidBlock)) {
                        BlockState iblockdata1 = Block.updateFromNeighbourShapes(iblockdata, this.level, blockposition);

                        this.level.setBlock(blockposition, iblockdata1, 20);
                    }
                }

                this.postProcessing[i].clear();
            }
        }

        UnmodifiableIterator unmodifiableiterator = ImmutableList.copyOf(this.pendingBlockEntities.keySet()).iterator();

        while (unmodifiableiterator.hasNext()) {
            BlockPos blockposition1 = (BlockPos) unmodifiableiterator.next();

            this.getBlockEntity(blockposition1);
        }

        this.pendingBlockEntities.clear();
        this.upgradeData.upgrade(this);
        } finally { // Paper start - replace chunk loader system
            this.isPostProcessingDone = true;
            this.level.getChunkSource().chunkMap.playerChunkManager.onChunkPostProcessing(this.chunkPos.x, this.chunkPos.z);
        }
        // Paper end - replace chunk loader system
    }

    @Nullable
    private BlockEntity promotePendingBlockEntity(BlockPos pos, CompoundTag nbt) {
        BlockState iblockdata = this.getBlockState(pos);
        BlockEntity tileentity;

        if ("DUMMY".equals(nbt.getString("id"))) {
            if (iblockdata.hasBlockEntity()) {
                tileentity = ((EntityBlock) iblockdata.getBlock()).newBlockEntity(pos, iblockdata);
            } else {
                tileentity = null;
                LevelChunk.LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, iblockdata);
            }
        } else {
            tileentity = BlockEntity.loadStatic(pos, iblockdata, nbt);
        }

        if (tileentity != null) {
            tileentity.setLevel(this.level);
            this.addAndRegisterBlockEntity(tileentity);
        } else {
            LevelChunk.LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", iblockdata, pos);
        }

        return tileentity;
    }

    public void unpackTicks(long time) {
        this.blockTicks.unpack(time);
        this.fluidTicks.unpack(time);
    }

    public void registerTickContainerInLevel(ServerLevel world) {
        world.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
        world.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
    }

    public void unregisterTickContainerFromLevel(ServerLevel world) {
        world.getBlockTicks().removeContainer(this.chunkPos);
        world.getFluidTicks().removeContainer(this.chunkPos);
    }

    @Override
    public ChunkStatus getStatus() {
        return ChunkStatus.FULL;
    }

    public ChunkHolder.FullChunkStatus getFullStatus() {
        return this.fullStatus == null ? ChunkHolder.FullChunkStatus.BORDER : (ChunkHolder.FullChunkStatus) this.fullStatus.get();
    }

    public void setFullStatus(Supplier<ChunkHolder.FullChunkStatus> levelTypeProvider) {
        this.fullStatus = levelTypeProvider;
    }

    public void clearAllBlockEntities() {
        this.blockEntities.values().forEach(BlockEntity::setRemoved);
        this.blockEntities.clear();
        this.tickersInLevel.values().forEach((chunk_d) -> {
            chunk_d.rebind(LevelChunk.NULL_TICKER);
        });
        this.tickersInLevel.clear();
    }

    public void registerAllBlockEntitiesAfterLevelLoad() {
        this.blockEntities.values().forEach((tileentity) -> {
            this.addGameEventListener(tileentity);
            this.updateBlockEntityTicker(tileentity);
        });
    }

    private <T extends BlockEntity> void addGameEventListener(T blockEntity) {
        if (!this.level.isClientSide) {
            Block block = blockEntity.getBlockState().getBlock();

            if (block instanceof EntityBlock) {
                GameEventListener gameeventlistener = ((EntityBlock) block).getListener(this.level, blockEntity);

                if (gameeventlistener != null) {
                    GameEventDispatcher gameeventdispatcher = this.getEventDispatcher(SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY()));

                    gameeventdispatcher.register(gameeventlistener);
                }
            }

        }
    }

    private <T extends BlockEntity> void updateBlockEntityTicker(T blockEntity) {
        BlockState iblockdata = blockEntity.getBlockState();
        BlockEntityTicker<T> blockentityticker = iblockdata.getTicker(this.level, (BlockEntityType<T>) blockEntity.getType()); // CraftBukkit - decompile error

        if (blockentityticker == null) {
            this.removeBlockEntityTicker(blockEntity.getBlockPos());
        } else {
            this.tickersInLevel.compute(blockEntity.getBlockPos(), (blockposition, chunk_d) -> {
                TickingBlockEntity tickingblockentity = this.createTicker(blockEntity, blockentityticker);

                if (chunk_d != null) {
                    chunk_d.rebind(tickingblockentity);
                    return chunk_d;
                } else if (this.isInLevel()) {
                    LevelChunk.RebindableTickingBlockEntityWrapper chunk_d1 = new LevelChunk.RebindableTickingBlockEntityWrapper(tickingblockentity);

                    this.level.addBlockEntityTicker(chunk_d1);
                    return chunk_d1;
                } else {
                    return null;
                }
            });
        }

    }

    private <T extends BlockEntity> TickingBlockEntity createTicker(T blockEntity, BlockEntityTicker<T> blockEntityTicker) {
        return new LevelChunk.BoundTickingBlockEntity<>(blockEntity, blockEntityTicker);
    }

    public boolean isClientLightReady() {
        return this.clientLightReady;
    }

    public void setClientLightReady(boolean shouldRenderOnUpdate) {
        this.clientLightReady = shouldRenderOnUpdate;
    }

    @FunctionalInterface
    public interface PostLoadProcessor {

        void run(LevelChunk chunk);
    }

    public static enum EntityCreationType {

        IMMEDIATE, QUEUED, CHECK;

        private EntityCreationType() {}
    }

    private class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {

        private TickingBlockEntity ticker;

        RebindableTickingBlockEntityWrapper(TickingBlockEntity tickingblockentity) {
            this.ticker = tickingblockentity;
        }

        void rebind(TickingBlockEntity wrapped) {
            this.ticker = wrapped;
        }

        @Override
        public void tick() {
            this.ticker.tick();
        }

        @Override
        public boolean isRemoved() {
            return this.ticker.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.ticker.getPos();
        }

        @Override
        public String getType() {
            return this.ticker.getType();
        }

        public String toString() {
            return this.ticker.toString() + " <wrapped>";
        }
    }

    private class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {

        private final T blockEntity;
        private final BlockEntityTicker<T> ticker;
        private boolean loggedInvalidBlockState;

        BoundTickingBlockEntity(BlockEntity tileentity, BlockEntityTicker blockentityticker) {
            this.blockEntity = (T) tileentity; // CraftBukkit - decompile error
            this.ticker = blockentityticker;
        }

        @Override
        public void tick() {
            if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
                BlockPos blockposition = this.blockEntity.getBlockPos();

                if (LevelChunk.this.isTicking(blockposition)) {
                    try {
                        ProfilerFiller gameprofilerfiller = LevelChunk.this.level.getProfiler();

                        gameprofilerfiller.push(this::getType);
                        this.blockEntity.tickTimer.startTiming(); // Spigot
                        BlockState iblockdata = LevelChunk.this.getBlockState(blockposition);

                        if (this.blockEntity.getType().isValid(iblockdata)) {
                            this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), iblockdata, this.blockEntity);
                            this.loggedInvalidBlockState = false;
                        } else if (!this.loggedInvalidBlockState) {
                            this.loggedInvalidBlockState = true;
                            LevelChunk.LOGGER.warn("Block entity {} @ {} state {} invalid for ticking:", new Object[]{LogUtils.defer(this::getType), LogUtils.defer(this::getPos), iblockdata});
                        }

                        gameprofilerfiller.pop();
                    } catch (Throwable throwable) {
                        if (throwable instanceof ThreadDeath) throw throwable; // Paper
                        // Paper start - Prevent tile entity and entity crashes
                        final String msg = String.format("BlockEntity threw exception at %s:%s,%s,%s", LevelChunk.this.getLevel().getWorld().getName(), this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
                        net.minecraft.server.MinecraftServer.LOGGER.error(msg, throwable);
                        net.minecraft.world.level.chunk.LevelChunk.this.level.getCraftServer().getPluginManager().callEvent(new com.destroystokyo.paper.event.server.ServerExceptionEvent(new ServerInternalException(msg, throwable)));
                        LevelChunk.this.removeBlockEntity(this.getPos());
                        // Paper end
                        // Spigot start
                    } finally {
                        this.blockEntity.tickTimer.stopTiming();
                        // Spigot end
                    }
                }
            }

        }

        @Override
        public boolean isRemoved() {
            return this.blockEntity.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.blockEntity.getBlockPos();
        }

        @Override
        public String getType() {
            return BlockEntityType.getKey(this.blockEntity.getType()).toString();
        }

        public String toString() {
            String s = this.getType();

            return "Level ticker for " + s + "@" + this.getPos();
        }
    }
}
