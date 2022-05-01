package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class BlockEntityBlockStateFix extends NamedEntityFix {
    public BlockEntityBlockStateFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "BlockEntityBlockStateFix", References.BLOCK_ENTITY, "minecraft:piston");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        Type<?> type = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:piston");
        Type<?> type2 = type.findFieldType("blockState");
        OpticFinder<?> opticFinder = DSL.fieldFinder("blockState", type2);
        Dynamic<?> dynamic = inputType.get(DSL.remainderFinder());
        int i = dynamic.get("blockId").asInt(0);
        dynamic = dynamic.remove("blockId");
        int j = dynamic.get("blockData").asInt(0) & 15;
        dynamic = dynamic.remove("blockData");
        Dynamic<?> dynamic2 = BlockStateData.getTag(i << 4 | j);
        Typed<?> typed = type.pointTyped(inputType.getOps()).orElseThrow(() -> {
            return new IllegalStateException("Could not create new piston block entity.");
        });
        return typed.set(DSL.remainderFinder(), dynamic).set(opticFinder, type2.readTyped(dynamic2).result().orElseThrow(() -> {
            return new IllegalStateException("Could not parse newly created block state tag.");
        }).getFirst());
    }
}
