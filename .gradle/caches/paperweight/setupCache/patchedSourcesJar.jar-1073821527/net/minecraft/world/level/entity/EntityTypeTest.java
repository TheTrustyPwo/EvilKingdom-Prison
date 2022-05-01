package net.minecraft.world.level.entity;

import javax.annotation.Nullable;

public interface EntityTypeTest<B, T extends B> {
    static <B, T extends B> EntityTypeTest<B, T> forClass(Class<T> cls) {
        return new EntityTypeTest<B, T>() {
            @Nullable
            @Override
            public T tryCast(B obj) {
                return (T)(cls.isInstance(obj) ? obj : null);
            }

            @Override
            public Class<? extends B> getBaseClass() {
                return cls;
            }
        };
    }

    @Nullable
    T tryCast(B obj);

    Class<? extends B> getBaseClass();
}
