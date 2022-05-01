package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public abstract class AbstractUUIDFix extends DataFix {
    protected TypeReference typeReference;

    public AbstractUUIDFix(Schema outputSchema, TypeReference typeReference) {
        super(outputSchema, false);
        this.typeReference = typeReference;
    }

    protected Typed<?> updateNamedChoice(Typed<?> typed, String name, Function<Dynamic<?>, Dynamic<?>> updater) {
        Type<?> type = this.getInputSchema().getChoiceType(this.typeReference, name);
        Type<?> type2 = this.getOutputSchema().getChoiceType(this.typeReference, name);
        return typed.updateTyped(DSL.namedChoice(name, type), type2, (typedx) -> {
            return typedx.update(DSL.remainderFinder(), updater);
        });
    }

    protected static Optional<Dynamic<?>> replaceUUIDString(Dynamic<?> dynamic, String oldKey, String newKey) {
        return createUUIDFromString(dynamic, oldKey).map((dynamic2) -> {
            return dynamic.remove(oldKey).set(newKey, dynamic2);
        });
    }

    protected static Optional<Dynamic<?>> replaceUUIDMLTag(Dynamic<?> dynamic, String oldKey, String newKey) {
        return dynamic.get(oldKey).result().flatMap(AbstractUUIDFix::createUUIDFromML).map((dynamic2) -> {
            return dynamic.remove(oldKey).set(newKey, dynamic2);
        });
    }

    protected static Optional<Dynamic<?>> replaceUUIDLeastMost(Dynamic<?> dynamic, String oldKey, String newKey) {
        String string = oldKey + "Most";
        String string2 = oldKey + "Least";
        return createUUIDFromLongs(dynamic, string, string2).map((dynamic2) -> {
            return dynamic.remove(string).remove(string2).set(newKey, dynamic2);
        });
    }

    protected static Optional<Dynamic<?>> createUUIDFromString(Dynamic<?> dynamic, String key) {
        return dynamic.get(key).result().flatMap((dynamic2) -> {
            String string = dynamic2.asString((String)null);
            if (string != null) {
                try {
                    UUID uUID = UUID.fromString(string);
                    return createUUIDTag(dynamic, uUID.getMostSignificantBits(), uUID.getLeastSignificantBits());
                } catch (IllegalArgumentException var4) {
                }
            }

            return Optional.empty();
        });
    }

    protected static Optional<Dynamic<?>> createUUIDFromML(Dynamic<?> dynamic) {
        return createUUIDFromLongs(dynamic, "M", "L");
    }

    protected static Optional<Dynamic<?>> createUUIDFromLongs(Dynamic<?> dynamic, String mostBitsKey, String leastBitsKey) {
        long l = dynamic.get(mostBitsKey).asLong(0L);
        long m = dynamic.get(leastBitsKey).asLong(0L);
        return l != 0L && m != 0L ? createUUIDTag(dynamic, l, m) : Optional.empty();
    }

    protected static Optional<Dynamic<?>> createUUIDTag(Dynamic<?> dynamic, long mostBits, long leastBits) {
        return Optional.of(dynamic.createIntList(Arrays.stream(new int[]{(int)(mostBits >> 32), (int)mostBits, (int)(leastBits >> 32), (int)leastBits})));
    }
}
