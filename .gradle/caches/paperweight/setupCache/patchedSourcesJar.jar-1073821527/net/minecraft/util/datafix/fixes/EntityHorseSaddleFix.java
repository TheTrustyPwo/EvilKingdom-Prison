package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityHorseSaddleFix extends NamedEntityFix {
    public EntityHorseSaddleFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "EntityHorseSaddleFix", References.ENTITY, "EntityHorse");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        Type<?> type = this.getInputSchema().getTypeRaw(References.ITEM_STACK);
        OpticFinder<?> opticFinder2 = DSL.fieldFinder("SaddleItem", type);
        Optional<? extends Typed<?>> optional = inputType.getOptionalTyped(opticFinder2);
        Dynamic<?> dynamic = inputType.get(DSL.remainderFinder());
        if (!optional.isPresent() && dynamic.get("Saddle").asBoolean(false)) {
            Typed<?> typed = type.pointTyped(inputType.getOps()).orElseThrow(IllegalStateException::new);
            typed = typed.set(opticFinder, Pair.of(References.ITEM_NAME.typeName(), "minecraft:saddle"));
            Dynamic<?> dynamic2 = dynamic.emptyMap();
            dynamic2 = dynamic2.set("Count", dynamic2.createByte((byte)1));
            dynamic2 = dynamic2.set("Damage", dynamic2.createShort((short)0));
            typed = typed.set(DSL.remainderFinder(), dynamic2);
            dynamic.remove("Saddle");
            return inputType.set(opticFinder2, typed).set(DSL.remainderFinder(), dynamic);
        } else {
            return inputType;
        }
    }
}
