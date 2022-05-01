package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.stream.Stream;

public class SavedDataVillageCropFix extends DataFix {
    public SavedDataVillageCropFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.writeFixAndRead("SavedDataVillageCropFix", this.getInputSchema().getType(References.STRUCTURE_FEATURE), this.getOutputSchema().getType(References.STRUCTURE_FEATURE), this::fixTag);
    }

    private <T> Dynamic<T> fixTag(Dynamic<T> dynamic) {
        return dynamic.update("Children", SavedDataVillageCropFix::updateChildren);
    }

    private static <T> Dynamic<T> updateChildren(Dynamic<T> dynamic) {
        return dynamic.asStreamOpt().map(SavedDataVillageCropFix::updateChildren).map(dynamic::createList).result().orElse(dynamic);
    }

    private static Stream<? extends Dynamic<?>> updateChildren(Stream<? extends Dynamic<?>> villageChildren) {
        return villageChildren.map((dynamic) -> {
            String string = dynamic.get("id").asString("");
            if ("ViF".equals(string)) {
                return updateSingleField(dynamic);
            } else {
                return "ViDF".equals(string) ? updateDoubleField(dynamic) : dynamic;
            }
        });
    }

    private static <T> Dynamic<T> updateSingleField(Dynamic<T> dynamic) {
        dynamic = updateCrop(dynamic, "CA");
        return updateCrop(dynamic, "CB");
    }

    private static <T> Dynamic<T> updateDoubleField(Dynamic<T> dynamic) {
        dynamic = updateCrop(dynamic, "CA");
        dynamic = updateCrop(dynamic, "CB");
        dynamic = updateCrop(dynamic, "CC");
        return updateCrop(dynamic, "CD");
    }

    private static <T> Dynamic<T> updateCrop(Dynamic<T> dynamic, String cropId) {
        return dynamic.get(cropId).asNumber().result().isPresent() ? dynamic.set(cropId, BlockStateData.getTag(dynamic.get(cropId).asInt(0) << 4)) : dynamic;
    }
}
