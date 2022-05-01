package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Objects;

public class EntityMinecartIdentifiersFix extends DataFix {
    private static final List<String> MINECART_BY_ID = Lists.newArrayList("MinecartRideable", "MinecartChest", "MinecartFurnace");

    public EntityMinecartIdentifiersFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        TaggedChoiceType<String> taggedChoiceType = this.getInputSchema().findChoiceType(References.ENTITY);
        TaggedChoiceType<String> taggedChoiceType2 = this.getOutputSchema().findChoiceType(References.ENTITY);
        return this.fixTypeEverywhere("EntityMinecartIdentifiersFix", taggedChoiceType, taggedChoiceType2, (dynamicOps) -> {
            return (pair) -> {
                if (!Objects.equals(pair.getFirst(), "Minecart")) {
                    return pair;
                } else {
                    Typed<? extends Pair<String, ?>> typed = taggedChoiceType.point(dynamicOps, "Minecart", pair.getSecond()).orElseThrow(IllegalStateException::new);
                    Dynamic<?> dynamic = typed.getOrCreate(DSL.remainderFinder());
                    int i = dynamic.get("Type").asInt(0);
                    String string;
                    if (i > 0 && i < MINECART_BY_ID.size()) {
                        string = MINECART_BY_ID.get(i);
                    } else {
                        string = "MinecartRideable";
                    }

                    return Pair.of(string, typed.write().map((dynamicx) -> {
                        return taggedChoiceType2.types().get(string).read(dynamicx);
                    }).result().orElseThrow(() -> {
                        return new IllegalStateException("Could not read the new minecart.");
                    }));
                }
            };
        });
    }
}
