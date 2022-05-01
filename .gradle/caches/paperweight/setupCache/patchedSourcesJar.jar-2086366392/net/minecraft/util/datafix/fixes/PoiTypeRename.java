package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;

public abstract class PoiTypeRename extends DataFix {
    public PoiTypeRename(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type<Pair<String, Dynamic<?>>> type = DSL.named(References.POI_CHUNK.typeName(), DSL.remainderType());
        if (!Objects.equals(type, this.getInputSchema().getType(References.POI_CHUNK))) {
            throw new IllegalStateException("Poi type is not what was expected.");
        } else {
            return this.fixTypeEverywhere("POI rename", type, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapSecond(this::cap);
                };
            });
        }
    }

    private <T> Dynamic<T> cap(Dynamic<T> dynamic) {
        return dynamic.update("Sections", (dynamicx) -> {
            return dynamicx.updateMapValues((pair) -> {
                return pair.mapSecond((dynamic) -> {
                    return dynamic.update("Records", (dynamicx) -> {
                        return DataFixUtils.orElse(this.renameRecords(dynamicx), dynamicx);
                    });
                });
            });
        });
    }

    private <T> Optional<Dynamic<T>> renameRecords(Dynamic<T> dynamic) {
        return dynamic.asStreamOpt().map((stream) -> {
            return dynamic.createList(stream.map((dynamicx) -> {
                return dynamicx.update("type", (dynamic) -> {
                    return DataFixUtils.orElse(dynamic.asString().map(this::rename).map(dynamic::createString).result(), dynamic);
                });
            }));
        }).result();
    }

    protected abstract String rename(String input);
}
