package net.minecraft.world.level.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class PersistentEntitySectionManager<T extends EntityAccess> implements AutoCloseable {
    static final Logger LOGGER = LogUtils.getLogger();
    final Set<UUID> knownUuids = Sets.newHashSet();
    final LevelCallback<T> callbacks;
    public final EntityPersistentStorage<T> permanentStorage;
    private final EntityLookup<T> visibleEntityStorage;
    final EntitySectionStorage<T> sectionStorage;
    private final LevelEntityGetter<T> entityGetter;
    private final Long2ObjectMap<Visibility> chunkVisibility = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<PersistentEntitySectionManager.ChunkLoadStatus> chunkLoadStatuses = new Long2ObjectOpenHashMap<>();
    private final LongSet chunksToUnload = new LongOpenHashSet();
    private final Queue<ChunkEntities<T>> loadingInbox = Queues.newConcurrentLinkedQueue();

    public PersistentEntitySectionManager(Class<T> entityClass, LevelCallback<T> handler, EntityPersistentStorage<T> dataAccess) {
        this.visibleEntityStorage = new EntityLookup<>();
        this.sectionStorage = new EntitySectionStorage<>(entityClass, this.chunkVisibility);
        this.chunkVisibility.defaultReturnValue(Visibility.HIDDEN);
        this.chunkLoadStatuses.defaultReturnValue(PersistentEntitySectionManager.ChunkLoadStatus.FRESH);
        this.callbacks = handler;
        this.permanentStorage = dataAccess;
        this.entityGetter = new LevelEntityGetterAdapter<>(this.visibleEntityStorage, this.sectionStorage);
    }

    void removeSectionIfEmpty(long sectionPos, EntitySection<T> section) {
        if (section.isEmpty()) {
            this.sectionStorage.remove(sectionPos);
        }

    }

    private boolean addEntityUuid(T entity) {
        if (!this.knownUuids.add(entity.getUUID())) {
            LOGGER.warn("UUID of added entity already exists: {}", (Object)entity);
            return false;
        } else {
            return true;
        }
    }

    public boolean addNewEntity(T entity) {
        return this.addEntity(entity, false);
    }

    private boolean addEntity(T entity, boolean existing) {
        if (!this.addEntityUuid(entity)) {
            return false;
        } else {
            long l = SectionPos.asLong(entity.blockPosition());
            EntitySection<T> entitySection = this.sectionStorage.getOrCreateSection(l);
            entitySection.add(entity);
            entity.setLevelCallback(new PersistentEntitySectionManager.Callback(entity, l, entitySection));
            if (!existing) {
                this.callbacks.onCreated(entity);
            }

            Visibility visibility = getEffectiveStatus(entity, entitySection.getStatus());
            if (visibility.isAccessible()) {
                this.startTracking(entity);
            }

            if (visibility.isTicking()) {
                this.startTicking(entity);
            }

            return true;
        }
    }

    static <T extends EntityAccess> Visibility getEffectiveStatus(T entity, Visibility current) {
        return entity.isAlwaysTicking() ? Visibility.TICKING : current;
    }

    public void addLegacyChunkEntities(Stream<T> entities) {
        entities.forEach((entity) -> {
            this.addEntity(entity, true);
        });
    }

    public void addWorldGenChunkEntities(Stream<T> entities) {
        entities.forEach((entity) -> {
            this.addEntity(entity, false);
        });
    }

    void startTicking(T entity) {
        this.callbacks.onTickingStart(entity);
    }

    void stopTicking(T entity) {
        this.callbacks.onTickingEnd(entity);
    }

    void startTracking(T entity) {
        this.visibleEntityStorage.add(entity);
        this.callbacks.onTrackingStart(entity);
    }

    void stopTracking(T entity) {
        this.callbacks.onTrackingEnd(entity);
        this.visibleEntityStorage.remove(entity);
    }

    public void updateChunkStatus(ChunkPos chunkPos, ChunkHolder.FullChunkStatus levelType) {
        Visibility visibility = Visibility.fromFullChunkStatus(levelType);
        this.updateChunkStatus(chunkPos, visibility);
    }

    public void updateChunkStatus(ChunkPos chunkPos, Visibility trackingStatus) {
        long l = chunkPos.toLong();
        if (trackingStatus == Visibility.HIDDEN) {
            this.chunkVisibility.remove(l);
            this.chunksToUnload.add(l);
        } else {
            this.chunkVisibility.put(l, trackingStatus);
            this.chunksToUnload.remove(l);
            this.ensureChunkQueuedForLoad(l);
        }

        this.sectionStorage.getExistingSectionsInChunk(l).forEach((group) -> {
            Visibility visibility2 = group.updateChunkStatus(trackingStatus);
            boolean bl = visibility2.isAccessible();
            boolean bl2 = trackingStatus.isAccessible();
            boolean bl3 = visibility2.isTicking();
            boolean bl4 = trackingStatus.isTicking();
            if (bl3 && !bl4) {
                group.getEntities().filter((entity) -> {
                    return !entity.isAlwaysTicking();
                }).forEach(this::stopTicking);
            }

            if (bl && !bl2) {
                group.getEntities().filter((entity) -> {
                    return !entity.isAlwaysTicking();
                }).forEach(this::stopTracking);
            } else if (!bl && bl2) {
                group.getEntities().filter((entity) -> {
                    return !entity.isAlwaysTicking();
                }).forEach(this::startTracking);
            }

            if (!bl3 && bl4) {
                group.getEntities().filter((entity) -> {
                    return !entity.isAlwaysTicking();
                }).forEach(this::startTicking);
            }

        });
    }

    public void ensureChunkQueuedForLoad(long chunkPos) {
        PersistentEntitySectionManager.ChunkLoadStatus chunkLoadStatus = this.chunkLoadStatuses.get(chunkPos);
        if (chunkLoadStatus == PersistentEntitySectionManager.ChunkLoadStatus.FRESH) {
            this.requestChunkLoad(chunkPos);
        }

    }

    public boolean storeChunkSections(long chunkPos, Consumer<T> action) {
        PersistentEntitySectionManager.ChunkLoadStatus chunkLoadStatus = this.chunkLoadStatuses.get(chunkPos);
        if (chunkLoadStatus == PersistentEntitySectionManager.ChunkLoadStatus.PENDING) {
            return false;
        } else {
            List<T> list = this.sectionStorage.getExistingSectionsInChunk(chunkPos).flatMap((section) -> {
                return section.getEntities().filter(EntityAccess::shouldBeSaved);
            }).collect(Collectors.toList());
            if (list.isEmpty()) {
                if (chunkLoadStatus == PersistentEntitySectionManager.ChunkLoadStatus.LOADED) {
                    this.permanentStorage.storeEntities(new ChunkEntities<>(new ChunkPos(chunkPos), ImmutableList.of()));
                }

                return true;
            } else if (chunkLoadStatus == PersistentEntitySectionManager.ChunkLoadStatus.FRESH) {
                this.requestChunkLoad(chunkPos);
                return false;
            } else {
                this.permanentStorage.storeEntities(new ChunkEntities<>(new ChunkPos(chunkPos), list));
                list.forEach(action);
                return true;
            }
        }
    }

    private void requestChunkLoad(long chunkPos) {
        this.chunkLoadStatuses.put(chunkPos, PersistentEntitySectionManager.ChunkLoadStatus.PENDING);
        ChunkPos chunkPos2 = new ChunkPos(chunkPos);
        this.permanentStorage.loadEntities(chunkPos2).thenAccept(this.loadingInbox::add).exceptionally((throwable) -> {
            LOGGER.error("Failed to read chunk {}", chunkPos2, throwable);
            return null;
        });
    }

    private boolean processChunkUnload(long chunkPos) {
        boolean bl = this.storeChunkSections(chunkPos, (entity) -> {
            entity.getPassengersAndSelf().forEach(this::unloadEntity);
        });
        if (!bl) {
            return false;
        } else {
            this.chunkLoadStatuses.remove(chunkPos);
            return true;
        }
    }

    private void unloadEntity(EntityAccess entity) {
        entity.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        entity.setLevelCallback(EntityInLevelCallback.NULL);
    }

    private void processUnloads() {
        this.chunksToUnload.removeIf((pos) -> {
            return this.chunkVisibility.get(pos) != Visibility.HIDDEN ? true : this.processChunkUnload(pos);
        });
    }

    private void processPendingLoads() {
        ChunkEntities<T> chunkEntities;
        while((chunkEntities = this.loadingInbox.poll()) != null) {
            chunkEntities.getEntities().forEach((entity) -> {
                this.addEntity(entity, true);
            });
            this.chunkLoadStatuses.put(chunkEntities.getPos().toLong(), PersistentEntitySectionManager.ChunkLoadStatus.LOADED);
        }

    }

    public void tick() {
        this.processPendingLoads();
        this.processUnloads();
    }

    private LongSet getAllChunksToSave() {
        LongSet longSet = this.sectionStorage.getAllChunksWithExistingSections();

        for(Entry<PersistentEntitySectionManager.ChunkLoadStatus> entry : Long2ObjectMaps.fastIterable(this.chunkLoadStatuses)) {
            if (entry.getValue() == PersistentEntitySectionManager.ChunkLoadStatus.LOADED) {
                longSet.add(entry.getLongKey());
            }
        }

        return longSet;
    }

    public void autoSave() {
        this.getAllChunksToSave().forEach((pos) -> {
            boolean bl = this.chunkVisibility.get(pos) == Visibility.HIDDEN;
            if (bl) {
                this.processChunkUnload(pos);
            } else {
                this.storeChunkSections(pos, (entity) -> {
                });
            }

        });
    }

    public void saveAll() {
        LongSet longSet = this.getAllChunksToSave();

        while(!longSet.isEmpty()) {
            this.permanentStorage.flush(false);
            this.processPendingLoads();
            longSet.removeIf((pos) -> {
                boolean bl = this.chunkVisibility.get(pos) == Visibility.HIDDEN;
                return bl ? this.processChunkUnload(pos) : this.storeChunkSections(pos, (entity) -> {
                });
            });
        }

        this.permanentStorage.flush(true);
    }

    @Override
    public void close() throws IOException {
        this.saveAll();
        this.permanentStorage.close();
    }

    public boolean isLoaded(UUID uuid) {
        return this.knownUuids.contains(uuid);
    }

    public LevelEntityGetter<T> getEntityGetter() {
        return this.entityGetter;
    }

    public boolean canPositionTick(BlockPos pos) {
        return this.chunkVisibility.get(ChunkPos.asLong(pos)).isTicking();
    }

    public boolean canPositionTick(ChunkPos pos) {
        return this.chunkVisibility.get(pos.toLong()).isTicking();
    }

    public boolean areEntitiesLoaded(long chunkPos) {
        return this.chunkLoadStatuses.get(chunkPos) == PersistentEntitySectionManager.ChunkLoadStatus.LOADED;
    }

    public void dumpSections(Writer writer) throws IOException {
        CsvOutput csvOutput = CsvOutput.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("visibility").addColumn("load_status").addColumn("entity_count").build(writer);
        this.sectionStorage.getAllChunksWithExistingSections().forEach((chunkPos) -> {
            PersistentEntitySectionManager.ChunkLoadStatus chunkLoadStatus = this.chunkLoadStatuses.get(chunkPos);
            this.sectionStorage.getExistingSectionPositionsInChunk(chunkPos).forEach((sectionPos) -> {
                EntitySection<T> entitySection = this.sectionStorage.getSection(sectionPos);
                if (entitySection != null) {
                    try {
                        csvOutput.writeRow(SectionPos.x(sectionPos), SectionPos.y(sectionPos), SectionPos.z(sectionPos), entitySection.getStatus(), chunkLoadStatus, entitySection.size());
                    } catch (IOException var7) {
                        throw new UncheckedIOException(var7);
                    }
                }

            });
        });
    }

    @VisibleForDebug
    public String gatherStats() {
        return this.knownUuids.size() + "," + this.visibleEntityStorage.count() + "," + this.sectionStorage.count() + "," + this.chunkLoadStatuses.size() + "," + this.chunkVisibility.size() + "," + this.loadingInbox.size() + "," + this.chunksToUnload.size();
    }

    class Callback implements EntityInLevelCallback {
        private final T entity;
        private long currentSectionKey;
        private EntitySection<T> currentSection;

        Callback(T entity, long sectionPos, EntitySection<T> section) {
            this.entity = entity;
            this.currentSectionKey = sectionPos;
            this.currentSection = section;
        }

        @Override
        public void onMove() {
            BlockPos blockPos = this.entity.blockPosition();
            long l = SectionPos.asLong(blockPos);
            if (l != this.currentSectionKey) {
                Visibility visibility = this.currentSection.getStatus();
                if (!this.currentSection.remove(this.entity)) {
                    PersistentEntitySectionManager.LOGGER.warn("Entity {} wasn't found in section {} (moving to {})", this.entity, SectionPos.of(this.currentSectionKey), l);
                }

                PersistentEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
                EntitySection<T> entitySection = PersistentEntitySectionManager.this.sectionStorage.getOrCreateSection(l);
                entitySection.add(this.entity);
                this.currentSection = entitySection;
                this.currentSectionKey = l;
                this.updateStatus(visibility, entitySection.getStatus());
            }

        }

        private void updateStatus(Visibility oldStatus, Visibility newStatus) {
            Visibility visibility = PersistentEntitySectionManager.getEffectiveStatus(this.entity, oldStatus);
            Visibility visibility2 = PersistentEntitySectionManager.getEffectiveStatus(this.entity, newStatus);
            if (visibility != visibility2) {
                boolean bl = visibility.isAccessible();
                boolean bl2 = visibility2.isAccessible();
                if (bl && !bl2) {
                    PersistentEntitySectionManager.this.stopTracking(this.entity);
                } else if (!bl && bl2) {
                    PersistentEntitySectionManager.this.startTracking(this.entity);
                }

                boolean bl3 = visibility.isTicking();
                boolean bl4 = visibility2.isTicking();
                if (bl3 && !bl4) {
                    PersistentEntitySectionManager.this.stopTicking(this.entity);
                } else if (!bl3 && bl4) {
                    PersistentEntitySectionManager.this.startTicking(this.entity);
                }

            }
        }

        @Override
        public void onRemove(Entity.RemovalReason reason) {
            if (!this.currentSection.remove(this.entity)) {
                PersistentEntitySectionManager.LOGGER.warn("Entity {} wasn't found in section {} (destroying due to {})", this.entity, SectionPos.of(this.currentSectionKey), reason);
            }

            Visibility visibility = PersistentEntitySectionManager.getEffectiveStatus(this.entity, this.currentSection.getStatus());
            if (visibility.isTicking()) {
                PersistentEntitySectionManager.this.stopTicking(this.entity);
            }

            if (visibility.isAccessible()) {
                PersistentEntitySectionManager.this.stopTracking(this.entity);
            }

            if (reason.shouldDestroy()) {
                PersistentEntitySectionManager.this.callbacks.onDestroyed(this.entity);
            }

            PersistentEntitySectionManager.this.knownUuids.remove(this.entity.getUUID());
            this.entity.setLevelCallback(NULL);
            PersistentEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
        }
    }

    static enum ChunkLoadStatus {
        FRESH,
        PENDING,
        LOADED;
    }
}
