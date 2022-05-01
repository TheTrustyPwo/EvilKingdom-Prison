package net.minecraft.world.level.gameevent;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface PositionSourceType<T extends PositionSource> {
    PositionSourceType<BlockPositionSource> BLOCK = register("block", new BlockPositionSource.Type());
    PositionSourceType<EntityPositionSource> ENTITY = register("entity", new EntityPositionSource.Type());

    T read(FriendlyByteBuf buf);

    void write(FriendlyByteBuf buf, T positionSource);

    Codec<T> codec();

    static <S extends PositionSourceType<T>, T extends PositionSource> S register(String id, S positionSourceType) {
        return Registry.register(Registry.POSITION_SOURCE_TYPE, id, positionSourceType);
    }

    static PositionSource fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation resourceLocation = buf.readResourceLocation();
        return Registry.POSITION_SOURCE_TYPE.getOptional(resourceLocation).orElseThrow(() -> {
            return new IllegalArgumentException("Unknown position source type " + resourceLocation);
        }).read(buf);
    }

    static <T extends PositionSource> void toNetwork(T positionSource, FriendlyByteBuf buf) {
        buf.writeResourceLocation(Registry.POSITION_SOURCE_TYPE.getKey(positionSource.getType()));
        positionSource.getType().write(buf, positionSource);
    }
}
