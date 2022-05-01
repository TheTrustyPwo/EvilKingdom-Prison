package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import java.util.function.Function;

public abstract class BlockRenameFixWithJigsaw extends BlockRenameFix {
    private final String name;

    public BlockRenameFixWithJigsaw(Schema oldSchema, String name) {
        super(oldSchema, name);
        this.name = name;
    }

    @Override
    public TypeRewriteRule makeRule() {
        TypeReference typeReference = References.BLOCK_ENTITY;
        String string = "minecraft:jigsaw";
        OpticFinder<?> opticFinder = DSL.namedChoice("minecraft:jigsaw", this.getInputSchema().getChoiceType(typeReference, "minecraft:jigsaw"));
        TypeRewriteRule typeRewriteRule = this.fixTypeEverywhereTyped(this.name + " for jigsaw state", this.getInputSchema().getType(typeReference), this.getOutputSchema().getType(typeReference), (typed) -> {
            return typed.updateTyped(opticFinder, this.getOutputSchema().getChoiceType(typeReference, "minecraft:jigsaw"), (typedx) -> {
                return typedx.update(DSL.remainderFinder(), (dynamic) -> {
                    return dynamic.update("final_state", (dynamic2) -> {
                        return DataFixUtils.orElse(dynamic2.asString().result().map((string) -> {
                            int i = string.indexOf(91);
                            int j = string.indexOf(123);
                            int k = string.length();
                            if (i > 0) {
                                k = Math.min(k, i);
                            }

                            if (j > 0) {
                                k = Math.min(k, j);
                            }

                            String string2 = string.substring(0, k);
                            String string3 = this.fixBlock(string2);
                            return string3 + string.substring(k);
                        }).map(dynamic::createString), dynamic2);
                    });
                });
            });
        });
        return TypeRewriteRule.seq(super.makeRule(), typeRewriteRule);
    }

    public static DataFix create(Schema oldSchema, String name, Function<String, String> rename) {
        return new BlockRenameFixWithJigsaw(oldSchema, name) {
            @Override
            protected String fixBlock(String oldName) {
                return rename.apply(oldName);
            }
        };
    }
}
