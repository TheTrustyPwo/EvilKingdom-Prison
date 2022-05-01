package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import java.util.stream.Collectors;

public class OptionsKeyTranslationFix extends DataFix {
    public OptionsKeyTranslationFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("OptionsKeyTranslationFix", this.getInputSchema().getType(References.OPTIONS), (typed) -> {
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                return dynamic.getMapValues().map((map) -> {
                    return dynamic.createMap(map.entrySet().stream().map((entry) -> {
                        if (entry.getKey().asString("").startsWith("key_")) {
                            String string = entry.getValue().asString("");
                            if (!string.startsWith("key.mouse") && !string.startsWith("scancode.")) {
                                return Pair.of(entry.getKey(), dynamic.createString("key.keyboard." + string.substring("key.".length())));
                            }
                        }

                        return Pair.of(entry.getKey(), entry.getValue());
                    }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
                }).result().orElse(dynamic);
            });
        });
    }
}
