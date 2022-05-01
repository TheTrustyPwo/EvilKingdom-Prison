package net.minecraft.world.entity.ai.village.poi;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap; // Paper
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.SectionTracker;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.SectionStorage;

public class PoiManager extends SectionStorage<PoiSection> {
    public static final int MAX_VILLAGE_DISTANCE = 6;
    public static final int VILLAGE_SECTION_SIZE = 1;
    // Paper start - unload poi data
    // the vanilla tracker needs to be replaced because it does not support level removes
    private final io.papermc.paper.util.misc.Delayed26WayDistancePropagator3D villageDistanceTracker = new io.papermc.paper.util.misc.Delayed26WayDistancePropagator3D();
    static final int POI_DATA_SOURCE = 7;
    public static int convertBetweenLevels(final int level) {
        return POI_DATA_SOURCE - level;
    }

    protected void updateDistanceTracking(long section) {
        if (this.isVillageCenter(section)) {
            this.villageDistanceTracker.setSource(section, POI_DATA_SOURCE);
        } else {
            this.villageDistanceTracker.removeSource(section);
        }
    }
    // Paper end - unload poi data
    private final LongSet loadedChunks = new LongOpenHashSet();
    public final net.minecraft.server.level.ServerLevel world; // Paper // Paper public

    public PoiManager(Path path, DataFixer dataFixer, boolean dsync, LevelHeightAccessor world) {
        super(path, PoiSection::codec, PoiSection::new, dataFixer, DataFixTypes.POI_CHUNK, dsync, world);
        if (world == null) { throw new IllegalStateException("world must be non-null"); } // Paper - require non-null
        this.world = (net.minecraft.server.level.ServerLevel)world; // Paper
    }

    // Paper start - actually unload POI data
    private final java.util.TreeSet<QueuedUnload> queuedUnloads = new java.util.TreeSet<>();
    private final Long2ObjectOpenHashMap<QueuedUnload> queuedUnloadsByCoordinate = new Long2ObjectOpenHashMap<>();

    static final class QueuedUnload implements Comparable<QueuedUnload> {

        private final long unloadTick;
        private final long coordinate;

        public QueuedUnload(long unloadTick, long coordinate) {
            this.unloadTick = unloadTick;
            this.coordinate = coordinate;
        }

        @Override
        public int compareTo(QueuedUnload other) {
            if (other.unloadTick == this.unloadTick) {
                return Long.compare(this.coordinate, other.coordinate);
            } else {
                return Long.compare(this.unloadTick, other.unloadTick);
            }
        }

        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 31 + Long.hashCode(this.unloadTick);
            hash = hash * 31 + Long.hashCode(this.coordinate);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != QueuedUnload.class) {
                return false;
            }
            QueuedUnload other = (QueuedUnload)obj;
            return other.unloadTick == this.unloadTick && other.coordinate == this.coordinate;
        }
    }

    long determineDelay(long coordinate) {
        if (this.isEmpty(coordinate)) {
            return 5 * 60 * 20;
        } else {
            return 60 * 20;
        }
    }

    public void queueUnload(long coordinate, long minTarget) {
        io.papermc.paper.util.TickThread.softEnsureTickThread("async poi unload queue");
        QueuedUnload unload = new QueuedUnload(minTarget + this.determineDelay(coordinate), coordinate);
        QueuedUnload existing = this.queuedUnloadsByCoordinate.put(coordinate, unload);
        if (existing != null) {
            this.queuedUnloads.remove(existing);
        }
        this.queuedUnloads.add(unload);
    }

    public void dequeueUnload(long coordinate) {
        io.papermc.paper.util.TickThread.softEnsureTickThread("async poi unload dequeue");
        QueuedUnload unload = this.queuedUnloadsByCoordinate.remove(coordinate);
        if (unload != null) {
            this.queuedUnloads.remove(unload);
        }
    }

    public void pollUnloads(BooleanSupplier canSleepForTick) {
        io.papermc.paper.util.TickThread.softEnsureTickThread("async poi unload");
        long currentTick = net.minecraft.server.MinecraftServer.currentTickLong;
        net.minecraft.server.level.ServerChunkCache chunkProvider = this.world.getChunkSource();
        net.minecraft.server.level.ChunkMap playerChunkMap = chunkProvider.chunkMap;
        // copied target determination from PlayerChunkMap

        java.util.Iterator<QueuedUnload> iterator = this.queuedUnloads.iterator();
        for (int i = 0; iterator.hasNext() && (i < 200 || this.queuedUnloads.size() > 2000 || canSleepForTick.getAsBoolean()); i++) {
            QueuedUnload unload = iterator.next();
            if (unload.unloadTick > currentTick) {
                break;
            }

            long coordinate = unload.coordinate;

            iterator.remove();
            this.queuedUnloadsByCoordinate.remove(coordinate);

            if (playerChunkMap.getUnloadingChunkHolder(net.minecraft.server.MCUtil.getCoordinateX(coordinate), net.minecraft.server.MCUtil.getCoordinateZ(coordinate)) != null
                || playerChunkMap.getUpdatingChunkIfPresent(coordinate) != null) {
                continue;
            }

            this.unloadData(coordinate);
        }
    }

    @Override
    public void unloadData(long coordinate) {
        io.papermc.paper.util.TickThread.softEnsureTickThread("async unloading poi data");
        super.unloadData(coordinate);
    }

    @Override
    protected void onUnload(long coordinate) {
        io.papermc.paper.util.TickThread.softEnsureTickThread("async poi unload callback");
        this.loadedChunks.remove(coordinate);
        int chunkX = net.minecraft.server.MCUtil.getCoordinateX(coordinate);
        int chunkZ = net.minecraft.server.MCUtil.getCoordinateZ(coordinate);
        for (int section = this.levelHeightAccessor.getMinSection(); section < this.levelHeightAccessor.getMaxSection(); ++section) {
            long sectionPos = SectionPos.asLong(chunkX, section, chunkZ);
            this.updateDistanceTracking(sectionPos);
        }
    }
    // Paper end - actually unload POI data

    public void add(BlockPos pos, PoiType type) {
        this.getOrCreate(SectionPos.asLong(pos)).add(pos, type);
    }

    public void remove(BlockPos pos) {
        this.getOrLoad(SectionPos.asLong(pos)).ifPresent((poiSet) -> {
            poiSet.remove(pos);
        });
    }

    public long getCountInRange(Predicate<PoiType> typePredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        return this.getInRange(typePredicate, pos, radius, occupationStatus).count();
    }

    public boolean existsAtPosition(PoiType type, BlockPos pos) {
        return this.exists(pos, type::equals);
    }

    public Stream<PoiRecord> getInSquare(Predicate<PoiType> typePredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        int i = Math.floorDiv(radius, 16) + 1;
        return ChunkPos.rangeClosed(new ChunkPos(pos), i).flatMap((chunkPos) -> {
            return this.getInChunk(typePredicate, chunkPos, occupationStatus);
        }).filter((poi) -> {
            BlockPos blockPos2 = poi.getPos();
            return Math.abs(blockPos2.getX() - pos.getX()) <= radius && Math.abs(blockPos2.getZ() - pos.getZ()) <= radius;
        });
    }

    public Stream<PoiRecord> getInRange(Predicate<PoiType> typePredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        int i = radius * radius;
        return this.getInSquare(typePredicate, pos, radius, occupationStatus).filter((poi) -> {
            return poi.getPos().distSqr(pos) <= (double)i;
        });
    }

    @VisibleForDebug
    public Stream<PoiRecord> getInChunk(Predicate<PoiType> typePredicate, ChunkPos chunkPos, PoiManager.Occupancy occupationStatus) {
        return IntStream.range(this.levelHeightAccessor.getMinSection(), this.levelHeightAccessor.getMaxSection()).boxed().map((integer) -> {
            return this.getOrLoad(SectionPos.of(chunkPos, integer).asLong());
        }).filter(Optional::isPresent).flatMap((optional) -> {
            return optional.get().getRecords(typePredicate, occupationStatus);
        });
    }

    public Stream<BlockPos> findAll(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        return this.getInRange(typePredicate, pos, radius, occupationStatus).map(PoiRecord::getPos).filter(posPredicate);
    }

    public Stream<BlockPos> findAllClosestFirst(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        return this.findAll(typePredicate, posPredicate, pos, radius, occupationStatus).sorted(Comparator.comparingDouble((blockPos2) -> {
            return blockPos2.distSqr(pos);
        }));
    }

    public Optional<BlockPos> find(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        // Paper start - re-route to faster logic
        BlockPos ret = io.papermc.paper.util.PoiAccess.findAnyPoiPosition(this, typePredicate, posPredicate, pos, radius, occupationStatus, false);
        return Optional.ofNullable(ret);
        // Paper end - re-route to faster logic
    }

    public Optional<BlockPos> findClosest(Predicate<PoiType> typePredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        // Paper start - re-route to faster logic
        BlockPos ret = io.papermc.paper.util.PoiAccess.findClosestPoiDataPosition(this, typePredicate, null, pos, radius, radius*radius, occupationStatus, false);
        return Optional.ofNullable(ret);
        // Paper end - re-route to faster logic
    }

    public Optional<BlockPos> findClosest(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        // Paper start - re-route to faster logic
        BlockPos ret = io.papermc.paper.util.PoiAccess.findClosestPoiDataPosition(this, typePredicate, posPredicate, pos, radius, radius * radius, occupationStatus, false);
        return Optional.ofNullable(ret);
        // Paper end - re-route to faster logic
    }

    public Optional<BlockPos> take(Predicate<PoiType> typePredicate, Predicate<BlockPos> positionPredicate, BlockPos pos, int radius) {
        // Paper start - re-route to faster logic
        PoiRecord ret = io.papermc.paper.util.PoiAccess.findAnyPoiRecord(
            this, typePredicate, positionPredicate, pos, radius, PoiManager.Occupancy.HAS_SPACE, false
        );
        if (ret == null) {
            return Optional.empty();
        }
        ret.acquireTicket();
        return Optional.of(ret.getPos());
        // Paper end - re-route to faster logic
    }

    public Optional<BlockPos> getRandom(Predicate<PoiType> typePredicate, Predicate<BlockPos> positionPredicate, PoiManager.Occupancy occupationStatus, BlockPos pos, int radius, Random random) {
        // Paper start - re-route to faster logic
        List<PoiRecord> list = new java.util.ArrayList<>();
        io.papermc.paper.util.PoiAccess.findAnyPoiRecords(
            this, typePredicate, positionPredicate, pos, radius, occupationStatus, false, Integer.MAX_VALUE, list
        );

        // the old method shuffled the list and then tried to find the first element in it that
        // matched positionPredicate, however we moved positionPredicate into the poi search. This means we can avoid a
        // shuffle entirely, and just pick a random element from list
        if (list.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(list.get(random.nextInt(list.size())).getPos());
        // Paper end - re-route to faster logic
    }

    public boolean release(BlockPos pos) {
        return this.getOrLoad(SectionPos.asLong(pos)).map((poiSet) -> {
            return poiSet.release(pos);
        }).orElseThrow(() -> {
            return Util.pauseInIde(new IllegalStateException("POI never registered at " + pos));
        });
    }

    public boolean exists(BlockPos pos, Predicate<PoiType> predicate) {
        return this.getOrLoad(SectionPos.asLong(pos)).map((poiSet) -> {
            return poiSet.exists(pos, predicate);
        }).orElse(false);
    }

    public Optional<PoiType> getType(BlockPos pos) {
        return this.getOrLoad(SectionPos.asLong(pos)).flatMap((poiSet) -> {
            return poiSet.getType(pos);
        });
    }

    /** @deprecated */
    @Deprecated
    @VisibleForDebug
    public int getFreeTickets(BlockPos pos) {
        return this.getOrLoad(SectionPos.asLong(pos)).map((poiSet) -> {
            return poiSet.getFreeTickets(pos);
        }).orElse(0);
    }

    public int sectionsToVillage(SectionPos pos) {
        this.villageDistanceTracker.propagateUpdates(); // Paper - replace distance tracking util
        return convertBetweenLevels(this.villageDistanceTracker.getLevel(io.papermc.paper.util.CoordinateUtils.getChunkSectionKey(pos))); // Paper - replace distance tracking util
    }

    boolean isVillageCenter(long pos) {
        Optional<PoiSection> optional = this.get(pos);
        return optional == null ? false : optional.map((poiSet) -> {
            return poiSet.getRecords(PoiType.ALL, PoiManager.Occupancy.IS_OCCUPIED).count() > 0L;
        }).orElse(false);
    }

    @Override
    public void tick(BooleanSupplier shouldKeepTicking) {
        // Paper start - async chunk io
        while (!this.dirty.isEmpty() && shouldKeepTicking.getAsBoolean() && !this.world.noSave()) { // Paper - unload POI data - don't write to disk if saving is disabled
            ChunkPos chunkcoordintpair = SectionPos.of(this.dirty.firstLong()).chunk();

            net.minecraft.nbt.CompoundTag data;
            try (co.aikar.timings.Timing ignored1 = this.world.timings.poiSaveDataSerialization.startTiming()) {
                data = this.getData(chunkcoordintpair);
            }
            com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE.scheduleSave(this.world,
                chunkcoordintpair.x, chunkcoordintpair.z, data, null, com.destroystokyo.paper.io.PrioritizedTaskQueue.NORMAL_PRIORITY);
        }
        // Paper start - unload POI data
        if (!this.world.noSave()) { // don't write to disk if saving is disabled
            this.pollUnloads(shouldKeepTicking);
        }
        // Paper end - unload POI data
        // Paper end
        this.villageDistanceTracker.propagateUpdates(); // Paper - replace distance tracking until
    }

    @Override
    protected void setDirty(long pos) {
        super.setDirty(pos);
        this.updateDistanceTracking(pos); // Paper - move to new distance tracking util
    }

    @Override
    protected void onSectionLoad(long pos) {
        this.updateDistanceTracking(pos); // Paper - move to new distance tracking util
    }

    public void checkConsistencyWithBlocks(ChunkPos chunkPos, LevelChunkSection chunkSection) {
        SectionPos sectionPos = SectionPos.of(chunkPos, SectionPos.blockToSectionCoord(chunkSection.bottomBlockY()));
        Util.ifElse(this.getOrLoad(sectionPos.asLong()), (poiSet) -> {
            poiSet.refresh((biConsumer) -> {
                if (mayHavePoi(chunkSection)) {
                    this.updateFromSection(chunkSection, sectionPos, biConsumer);
                }

            });
        }, () -> {
            if (mayHavePoi(chunkSection)) {
                PoiSection poiSection = this.getOrCreate(sectionPos.asLong());
                this.updateFromSection(chunkSection, sectionPos, poiSection::add);
            }

        });
    }

    private static boolean mayHavePoi(LevelChunkSection chunkSection) {
        return chunkSection.maybeHas(PoiType.ALL_STATES::contains);
    }

    private void updateFromSection(LevelChunkSection chunkSection, SectionPos sectionPos, BiConsumer<BlockPos, PoiType> biConsumer) {
        sectionPos.blocksInside().forEach((pos) -> {
            BlockState blockState = chunkSection.getBlockState(SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()));
            PoiType.forState(blockState).ifPresent((poiType) -> {
                biConsumer.accept(pos, poiType);
            });
        });
    }

    public void ensureLoadedAndValid(LevelReader world, BlockPos pos, int radius) {
        SectionPos.aroundChunk(new ChunkPos(pos), Math.floorDiv(radius, 16), this.levelHeightAccessor.getMinSection(), this.levelHeightAccessor.getMaxSection()).map((sectionPos) -> {
            return Pair.of(sectionPos, this.getOrLoad(sectionPos.asLong()));
        }).filter((pair) -> {
            return !pair.getSecond().map(PoiSection::isValid).orElse(false);
        }).map((pair) -> {
            return pair.getFirst().chunk();
        }).filter((chunkPos) -> {
            return this.loadedChunks.add(chunkPos.toLong());
        }).forEach((chunkPos) -> {
            world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.EMPTY);
        });
    }

    final class DistanceTracker extends SectionTracker {
        private final Long2ByteMap levels = new Long2ByteOpenHashMap();

        protected DistanceTracker() {
            super(7, 16, 256);
            this.levels.defaultReturnValue((byte)7);
        }

        @Override
        protected int getLevelFromSource(long id) {
            return PoiManager.this.isVillageCenter(id) ? 0 : 7; // Paper - unload poi data - diff on change, this specifies the source level to use for distance tracking
        }

        @Override
        protected int getLevel(long id) {
            return this.levels.get(id);
        }

        @Override
        protected void setLevel(long id, int level) {
            if (level > 6) {
                this.levels.remove(id);
            } else {
                this.levels.put(id, (byte)level);
            }

        }

        public void runAllUpdates() {
            super.runUpdates(Integer.MAX_VALUE);
        }
    }

    // Paper start - Asynchronous chunk io
    @javax.annotation.Nullable
    @Override
    public net.minecraft.nbt.CompoundTag read(ChunkPos chunkcoordintpair) throws java.io.IOException {
        if (this.world != null && Thread.currentThread() != com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE) {
            net.minecraft.nbt.CompoundTag ret = com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE
                .loadChunkDataAsyncFuture(this.world, chunkcoordintpair.x, chunkcoordintpair.z, com.destroystokyo.paper.io.IOUtil.getPriorityForCurrentThread(),
                    true, false, true).join().poiData;

            if (ret == com.destroystokyo.paper.io.PaperFileIOThread.FAILURE_VALUE) {
                throw new java.io.IOException("See logs for further detail");
            }
            return ret;
        }
        return super.read(chunkcoordintpair);
    }

    @Override
    public void write(ChunkPos chunkcoordintpair, net.minecraft.nbt.CompoundTag nbttagcompound) throws java.io.IOException {
        if (this.world != null && Thread.currentThread() != com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE) {
            com.destroystokyo.paper.io.PaperFileIOThread.Holder.INSTANCE.scheduleSave(
                this.world, chunkcoordintpair.x, chunkcoordintpair.z, nbttagcompound, null,
                com.destroystokyo.paper.io.IOUtil.getPriorityForCurrentThread());
            return;
        }
        super.write(chunkcoordintpair, nbttagcompound);
    }
    // Paper end

    public static enum Occupancy {
        HAS_SPACE(PoiRecord::hasSpace),
        IS_OCCUPIED(PoiRecord::isOccupied),
        ANY((poiRecord) -> {
            return true;
        });

        private final Predicate<? super PoiRecord> test;

        private Occupancy(Predicate<? super PoiRecord> predicate) {
            this.test = predicate;
        }

        public Predicate<? super PoiRecord> getTest() {
            return this.test;
        }
    }
}
