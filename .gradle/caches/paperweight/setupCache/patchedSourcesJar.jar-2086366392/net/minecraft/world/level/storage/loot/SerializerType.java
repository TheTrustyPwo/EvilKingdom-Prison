package net.minecraft.world.level.storage.loot;

public class SerializerType<T> {
    private final Serializer<? extends T> serializer;

    public SerializerType(Serializer<? extends T> jsonSerializer) {
        this.serializer = jsonSerializer;
    }

    public Serializer<? extends T> getSerializer() {
        return this.serializer;
    }
}
