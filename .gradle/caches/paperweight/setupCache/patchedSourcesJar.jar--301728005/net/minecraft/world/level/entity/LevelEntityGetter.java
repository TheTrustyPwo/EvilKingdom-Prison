package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;

public interface LevelEntityGetter<T extends EntityAccess> {
    @Nullable
    T get(int id);

    @Nullable
    T get(UUID uuid);

    Iterable<T> getAll();

    <U extends T> void get(EntityTypeTest<T, U> filter, Consumer<U> action);

    void get(AABB box, Consumer<T> action);
    void get(AABB box, Consumer<T> action, boolean isContainerSearch); // Paper

    <U extends T> void get(EntityTypeTest<T, U> filter, AABB box, Consumer<U> action);
}
