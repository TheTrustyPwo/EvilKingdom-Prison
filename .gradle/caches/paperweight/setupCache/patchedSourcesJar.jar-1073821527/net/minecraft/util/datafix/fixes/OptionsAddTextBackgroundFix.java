package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsAddTextBackgroundFix extends DataFix {
    public OptionsAddTextBackgroundFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("OptionsAddTextBackgroundFix", this.getInputSchema().getType(References.OPTIONS), (typed) -> {
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                return DataFixUtils.orElse(dynamic.get("chatOpacity").asString().map((string) -> {
                    return dynamic.set("textBackgroundOpacity", dynamic.createDouble(this.calculateBackground(string)));
                }).result(), dynamic);
            });
        });
    }

    private double calculateBackground(String chatOpacity) {
        try {
            double d = 0.9D * Double.parseDouble(chatOpacity) + 0.1D;
            return d / 2.0D;
        } catch (NumberFormatException var4) {
            return 0.5D;
        }
    }
}
