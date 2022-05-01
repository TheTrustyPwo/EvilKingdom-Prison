package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;

public class LevelEntityGetterAdapter<T extends EntityAccess> implements LevelEntityGetter<T> {
    private final EntityLookup<T> visibleEntities;
    private final EntitySectionStorage<T> sectionStorage;

    public LevelEntityGetterAdapter(EntityLookup<T> index, EntitySectionStorage<T> cache) {
        this.visibleEntities = index;
        this.sectionStorage = cache;
    }

    @Nullable
    @Override
    public T get(int id) {
        return this.visibleEntities.getEntity(id);
    }

    @Nullable
    @Override
    public T get(UUID uuid) {
        return this.visibleEntities.getEntity(uuid);
    }

    @Override
    public Iterable<T> getAll() {
        return this.visibleEntities.getAllEntities();
    }

    @Override
    public <U extends T> void get(EntityTypeTest<T, U> filter, Consumer<U> action) {
        this.visibleEntities.getEntities(filter, action);
    }

    @Override
    public void get(AABB box, Consumer<T> action) {
        // Paper start
        this.get(box, action, false);
    }
    @Override
    public void get(AABB box, Consumer<T> action, boolean isContainerSearch) {
        this.sectionStorage.getEntities(box, action, isContainerSearch);
        // Paper end
    }

    @Override
    public <U extends T> void get(EntityTypeTest<T, U> filter, AABB box, Consumer<U> action) {
        this.sectionStorage.getEntities(filter, box, action);
    }
}
