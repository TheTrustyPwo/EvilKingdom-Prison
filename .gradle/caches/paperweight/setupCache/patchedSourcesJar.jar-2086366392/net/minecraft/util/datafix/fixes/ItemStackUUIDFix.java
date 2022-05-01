package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackUUIDFix extends AbstractUUIDFix {
    public ItemStackUUIDFix(Schema outputSchema) {
        super(outputSchema, References.ITEM_STACK);
    }

    public TypeRewriteRule makeRule() {
        OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        return this.fixTypeEverywhereTyped("ItemStackUUIDFix", this.getInputSchema().getType(this.typeReference), (typed) -> {
            OpticFinder<?> opticFinder2 = typed.getType().findField("tag");
            return typed.updateTyped(opticFinder2, (typed2) -> {
                return typed2.update(DSL.remainderFinder(), (dynamic) -> {
                    dynamic = this.updateAttributeModifiers(dynamic);
                    if (typed.getOptional(opticFinder).map((pair) -> {
                        return "minecraft:player_head".equals(pair.getSecond());
                    }).orElse(false)) {
                        dynamic = this.updateSkullOwner(dynamic);
                    }

                    return dynamic;
                });
            });
        });
    }

    private Dynamic<?> updateAttributeModifiers(Dynamic<?> dynamic) {
        return dynamic.update("AttributeModifiers", (dynamic2) -> {
            return dynamic.createList(dynamic2.asStream().map((dynamicx) -> {
                return replaceUUIDLeastMost(dynamicx, "UUID", "UUID").orElse(dynamicx);
            }));
        });
    }

    private Dynamic<?> updateSkullOwner(Dynamic<?> dynamic) {
        return dynamic.update("SkullOwner", (dynamicx) -> {
            return replaceUUIDString(dynamicx, "Id", "Id").orElse(dynamicx);
        });
    }
}
