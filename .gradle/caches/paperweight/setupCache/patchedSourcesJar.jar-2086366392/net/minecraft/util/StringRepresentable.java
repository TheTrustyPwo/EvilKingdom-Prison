package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface StringRepresentable {
    String getSerializedName();

    static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnum(Supplier<E[]> enumValues, Function<String, E> fromString) {
        E[] enums = (Enum[])enumValues.get();
        return ExtraCodecs.orCompressed(ExtraCodecs.stringResolverCodec((object) -> {
            return object.getSerializedName();
        }, fromString), ExtraCodecs.idResolverCodec((object) -> {
            return object.ordinal();
        }, (ordinal) -> {
            return (E)(ordinal >= 0 && ordinal < enums.length ? enums[ordinal] : null);
        }, -1));
    }

    static Keyable keys(StringRepresentable[] values) {
        return new Keyable() {
            public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
                return Arrays.stream(values).map(StringRepresentable::getSerializedName).map(dynamicOps::createString);
            }
        };
    }
}
