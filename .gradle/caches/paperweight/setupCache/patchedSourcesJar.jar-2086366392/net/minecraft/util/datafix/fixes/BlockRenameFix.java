package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class BlockRenameFix extends DataFix {
    private final String name;

    public BlockRenameFix(Schema oldSchema, String name) {
        super(oldSchema, false);
        this.name = name;
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.BLOCK_NAME);
        Type<Pair<String, String>> type2 = DSL.named(References.BLOCK_NAME.typeName(), NamespacedSchema.namespacedString());
        if (!Objects.equals(type, type2)) {
            throw new IllegalStateException("block type is not what was expected.");
        } else {
            TypeRewriteRule typeRewriteRule = this.fixTypeEverywhere(this.name + " for block", type2, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapSecond(this::fixBlock);
                };
            });
            TypeRewriteRule typeRewriteRule2 = this.fixTypeEverywhereTyped(this.name + " for block_state", this.getInputSchema().getType(References.BLOCK_STATE), (typed) -> {
                return typed.update(DSL.remainderFinder(), (dynamic) -> {
                    Optional<String> optional = dynamic.get("Name").asString().result();
                    return optional.isPresent() ? dynamic.set("Name", dynamic.createString(this.fixBlock(optional.get()))) : dynamic;
                });
            });
            return TypeRewriteRule.seq(typeRewriteRule, typeRewriteRule2);
        }
    }

    protected abstract String fixBlock(String oldName);

    public static DataFix create(Schema oldSchema, String name, Function<String, String> rename) {
        return new BlockRenameFix(oldSchema, name) {
            @Override
            protected String fixBlock(String oldName) {
                return rename.apply(oldName);
            }
        };
    }
}
