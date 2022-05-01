package net.minecraft.world.level.timers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface TimerCallback<T> {
    void handle(T server, TimerQueue<T> events, long time);

    public abstract static class Serializer<T, C extends TimerCallback<T>> {
        private final ResourceLocation id;
        private final Class<?> cls;

        public Serializer(ResourceLocation id, Class<?> callbackClass) {
            this.id = id;
            this.cls = callbackClass;
        }

        public ResourceLocation getId() {
            return this.id;
        }

        public Class<?> getCls() {
            return this.cls;
        }

        public abstract void serialize(CompoundTag nbt, C callback);

        public abstract C deserialize(CompoundTag nbt);
    }
}
