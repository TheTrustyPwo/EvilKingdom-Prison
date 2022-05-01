package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class TransientEntitySectionManager<T extends EntityAccess> {
    static final Logger LOGGER = LogUtils.getLogger();
    final LevelCallback<T> callbacks;
    final EntityLookup<T> entityStorage;
    final EntitySectionStorage<T> sectionStorage;
    private final LongSet tickingChunks = new LongOpenHashSet();
    private final LevelEntityGetter<T> entityGetter;

    public TransientEntitySectionManager(Class<T> entityClass, LevelCallback<T> handler) {
        this.entityStorage = new EntityLookup<>();
        this.sectionStorage = new EntitySectionStorage<>(entityClass, (pos) -> {
            return this.tickingChunks.contains(pos) ? Visibility.TICKING : Visibility.TRACKED;
        });
        this.callbacks = handler;
        this.entityGetter = new LevelEntityGetterAdapter<>(this.entityStorage, this.sectionStorage);
    }

    public void startTicking(ChunkPos pos) {
        long l = pos.toLong();
        this.tickingChunks.add(l);
        this.sectionStorage.getExistingSectionsInChunk(l).forEach((sections) -> {
            Visibility visibility = sections.updateChunkStatus(Visibility.TICKING);
            if (!visibility.isTicking()) {
                sections.getEntities().filter((e) -> {
                    return !e.isAlwaysTicking();
                }).forEach(this.callbacks::onTickingStart);
            }

        });
    }

    public void stopTicking(ChunkPos pos) {
        long l = pos.toLong();
        this.tickingChunks.remove(l);
        this.sectionStorage.getExistingSectionsInChunk(l).forEach((sections) -> {
            Visibility visibility = sections.updateChunkStatus(Visibility.TRACKED);
            if (visibility.isTicking()) {
                sections.getEntities().filter((e) -> {
                    return !e.isAlwaysTicking();
                }).forEach(this.callbacks::onTickingEnd);
            }

        });
    }

    public LevelEntityGetter<T> getEntityGetter() {
        return this.entityGetter;
    }

    public void addEntity(T entity) {
        this.entityStorage.add(entity);
        long l = SectionPos.asLong(entity.blockPosition());
        EntitySection<T> entitySection = this.sectionStorage.getOrCreateSection(l);
        entitySection.add(entity);
        entity.setLevelCallback(new TransientEntitySectionManager.Callback(entity, l, entitySection));
        this.callbacks.onCreated(entity);
        this.callbacks.onTrackingStart(entity);
        if (entity.isAlwaysTicking() || entitySection.getStatus().isTicking()) {
            this.callbacks.onTickingStart(entity);
        }

    }

    @VisibleForDebug
    public int count() {
        return this.entityStorage.count();
    }

    void removeSectionIfEmpty(long packedChunkSection, EntitySection<T> entities) {
        if (entities.isEmpty()) {
            this.sectionStorage.remove(packedChunkSection);
        }

    }

    @VisibleForDebug
    public String gatherStats() {
        return this.entityStorage.count() + "," + this.sectionStorage.count() + "," + this.tickingChunks.size();
    }

    class Callback implements EntityInLevelCallback {
        private final T entity;
        private long currentSectionKey;
        private EntitySection<T> currentSection;

        Callback(T entity, long pos, EntitySection<T> section) {
            this.entity = entity;
            this.currentSectionKey = pos;
            this.currentSection = section;
        }

        @Override
        public void onMove() {
            BlockPos blockPos = this.entity.blockPosition();
            long l = SectionPos.asLong(blockPos);
            if (l != this.currentSectionKey) {
                Visibility visibility = this.currentSection.getStatus();
                if (!this.currentSection.remove(this.entity)) {
                    TransientEntitySectionManager.LOGGER.warn("Entity {} wasn't found in section {} (moving to {})", this.entity, SectionPos.of(this.currentSectionKey), l);
                }

                TransientEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
                EntitySection<T> entitySection = TransientEntitySectionManager.this.sectionStorage.getOrCreateSection(l);
                entitySection.add(this.entity);
                this.currentSection = entitySection;
                this.currentSectionKey = l;
                if (!this.entity.isAlwaysTicking()) {
                    boolean bl = visibility.isTicking();
                    boolean bl2 = entitySection.getStatus().isTicking();
                    if (bl && !bl2) {
                        TransientEntitySectionManager.this.callbacks.onTickingEnd(this.entity);
                    } else if (!bl && bl2) {
                        TransientEntitySectionManager.this.callbacks.onTickingStart(this.entity);
                    }
                }
            }

        }

        @Override
        public void onRemove(Entity.RemovalReason reason) {
            if (!this.currentSection.remove(this.entity)) {
                TransientEntitySectionManager.LOGGER.warn("Entity {} wasn't found in section {} (destroying due to {})", this.entity, SectionPos.of(this.currentSectionKey), reason);
            }

            Visibility visibility = this.currentSection.getStatus();
            if (visibility.isTicking() || this.entity.isAlwaysTicking()) {
                TransientEntitySectionManager.this.callbacks.onTickingEnd(this.entity);
            }

            TransientEntitySectionManager.this.callbacks.onTrackingEnd(this.entity);
            TransientEntitySectionManager.this.callbacks.onDestroyed(this.entity);
            TransientEntitySectionManager.this.entityStorage.remove(this.entity);
            this.entity.setLevelCallback(NULL);
            TransientEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
        }
    }
}
