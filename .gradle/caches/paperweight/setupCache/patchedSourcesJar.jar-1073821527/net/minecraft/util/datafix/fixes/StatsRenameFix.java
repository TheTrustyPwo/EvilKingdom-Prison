package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import java.util.Map;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class StatsRenameFix extends DataFix {
    private final String name;
    private final Map<String, String> renames;

    public StatsRenameFix(Schema outputSchema, String name, Map<String, String> replacements) {
        super(outputSchema, false);
        this.name = name;
        this.renames = replacements;
    }

    protected TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq(this.createStatRule(), this.createCriteriaRule());
    }

    private TypeRewriteRule createCriteriaRule() {
        Type<?> type = this.getOutputSchema().getType(References.OBJECTIVE);
        Type<?> type2 = this.getInputSchema().getType(References.OBJECTIVE);
        OpticFinder<?> opticFinder = type2.findField("CriteriaType");
        TaggedChoiceType<?> taggedChoiceType = opticFinder.type().findChoiceType("type", -1).orElseThrow(() -> {
            return new IllegalStateException("Can't find choice type for criteria");
        });
        Type<?> type3 = taggedChoiceType.types().get("minecraft:custom");
        if (type3 == null) {
            throw new IllegalStateException("Failed to find custom criterion type variant");
        } else {
            OpticFinder<?> opticFinder2 = DSL.namedChoice("minecraft:custom", type3);
            OpticFinder<String> opticFinder3 = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
            return this.fixTypeEverywhereTyped(this.name, type2, type, (typed) -> {
                return typed.updateTyped(opticFinder, (typedx) -> {
                    return typedx.updateTyped(opticFinder2, (typed) -> {
                        return typed.update(opticFinder3, (old) -> {
                            return this.renames.getOrDefault(old, old);
                        });
                    });
                });
            });
        }
    }

    private TypeRewriteRule createStatRule() {
        Type<?> type = this.getOutputSchema().getType(References.STATS);
        Type<?> type2 = this.getInputSchema().getType(References.STATS);
        OpticFinder<?> opticFinder = type2.findField("stats");
        OpticFinder<?> opticFinder2 = opticFinder.type().findField("minecraft:custom");
        OpticFinder<String> opticFinder3 = NamespacedSchema.namespacedString().finder();
        return this.fixTypeEverywhereTyped(this.name, type2, type, (typed) -> {
            return typed.updateTyped(opticFinder, (typedx) -> {
                return typedx.updateTyped(opticFinder2, (typed) -> {
                    return typed.update(opticFinder3, (old) -> {
                        return this.renames.getOrDefault(old, old);
                    });
                });
            });
        });
    }
}
