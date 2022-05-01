package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Codec;
import com.mojang.serialization.OptionalDynamic;
import java.util.List;

public class EntityRedundantChanceTagsFix extends DataFix {
    private static final Codec<List<Float>> FLOAT_LIST_CODEC = Codec.FLOAT.listOf();

    public EntityRedundantChanceTagsFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("EntityRedundantChanceTagsFix", this.getInputSchema().getType(References.ENTITY), (typed) -> {
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                if (isZeroList(dynamic.get("HandDropChances"), 2)) {
                    dynamic = dynamic.remove("HandDropChances");
                }

                if (isZeroList(dynamic.get("ArmorDropChances"), 4)) {
                    dynamic = dynamic.remove("ArmorDropChances");
                }

                return dynamic;
            });
        });
    }

    private static boolean isZeroList(OptionalDynamic<?> optionalDynamic, int i) {
        return optionalDynamic.flatMap(FLOAT_LIST_CODEC::parse).map((list) -> {
            return list.size() == i && list.stream().allMatch((float_) -> {
                return float_ == 0.0F;
            });
        }).result().orElse(false);
    }
}
