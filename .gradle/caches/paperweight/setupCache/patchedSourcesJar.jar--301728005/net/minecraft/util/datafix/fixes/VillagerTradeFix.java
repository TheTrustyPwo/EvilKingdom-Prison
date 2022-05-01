package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class VillagerTradeFix extends NamedEntityFix {
    public VillagerTradeFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "Villager trade fix", References.ENTITY, "minecraft:villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        OpticFinder<?> opticFinder = inputType.getType().findField("Offers");
        OpticFinder<?> opticFinder2 = opticFinder.type().findField("Recipes");
        Type<?> type = opticFinder2.type();
        if (!(type instanceof ListType)) {
            throw new IllegalStateException("Recipes are expected to be a list.");
        } else {
            ListType<?> listType = (ListType)type;
            Type<?> type2 = listType.getElement();
            OpticFinder<?> opticFinder3 = DSL.typeFinder(type2);
            OpticFinder<?> opticFinder4 = type2.findField("buy");
            OpticFinder<?> opticFinder5 = type2.findField("buyB");
            OpticFinder<?> opticFinder6 = type2.findField("sell");
            OpticFinder<Pair<String, String>> opticFinder7 = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
            Function<Typed<?>, Typed<?>> function = (typed) -> {
                return this.updateItemStack(opticFinder7, typed);
            };
            return inputType.updateTyped(opticFinder, (typed) -> {
                return typed.updateTyped(opticFinder2, (typedx) -> {
                    return typedx.updateTyped(opticFinder3, (typed) -> {
                        return typed.updateTyped(opticFinder4, function).updateTyped(opticFinder5, function).updateTyped(opticFinder6, function);
                    });
                });
            });
        }
    }

    private Typed<?> updateItemStack(OpticFinder<Pair<String, String>> opticFinder, Typed<?> typed) {
        return typed.update(opticFinder, (pair) -> {
            return pair.mapSecond((string) -> {
                return Objects.equals(string, "minecraft:carved_pumpkin") ? "minecraft:pumpkin" : string;
            });
        });
    }
}
