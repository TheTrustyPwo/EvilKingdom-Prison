package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class EntitySection<T extends EntityAccess> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ClassInstanceMultiMap<T> storage;
    private Visibility chunkStatus;
    // Paper start - track number of items and minecarts
    public int itemCount;
    public int inventoryEntityCount;
    // Paper end

    public EntitySection(Class<T> entityClass, Visibility status) {
        this.chunkStatus = status;
        this.storage = new ClassInstanceMultiMap<>(entityClass);
    }

    public void add(T entity) {
        // Paper start
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
            this.itemCount++;
        } else if (entity instanceof net.minecraft.world.Container) {
            this.inventoryEntityCount++;
        }
        // Paper end
        this.storage.add(entity);
    }

    public boolean remove(T entity) {
        // Paper start
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
            this.itemCount--;
        } else if (entity instanceof net.minecraft.world.Container) {
            this.inventoryEntityCount--;
        }
        // Paper end
        return this.storage.remove(entity);
    }

    public void getEntities(AABB box, Consumer<T> action) {
        for(T entityAccess : this.storage) {
            if (entityAccess.getBoundingBox().intersects(box)) {
                action.accept(entityAccess);
            }
        }

    }

    public <U extends T> void getEntities(EntityTypeTest<T, U> type, AABB box, Consumer<? super U> action) {
        Collection<? extends T> collection = this.storage.find(type.getBaseClass());
        if (!collection.isEmpty()) {
            for(T entityAccess : collection) {
                U entityAccess2 = (U)((EntityAccess)type.tryCast(entityAccess));
                if (entityAccess2 != null && entityAccess.getBoundingBox().intersects(box)) {
                    action.accept(entityAccess2); // Paper - decompile fix
                }
            }

        }
    }

    public boolean isEmpty() {
        return this.storage.isEmpty();
    }

    public Stream<T> getEntities() {
        return this.storage.stream();
    }

    public Visibility getStatus() {
        return this.chunkStatus;
    }

    public Visibility updateChunkStatus(Visibility status) {
        Visibility visibility = this.chunkStatus;
        this.chunkStatus = status;
        return visibility;
    }

    @VisibleForDebug
    public int size() {
        return this.storage.size();
    }
}
