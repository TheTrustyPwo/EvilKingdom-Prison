package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class VillagerFollowRangeFix extends NamedEntityFix {
    private static final double ORIGINAL_VALUE = 16.0D;
    private static final double NEW_BASE_VALUE = 48.0D;

    public VillagerFollowRangeFix(Schema outputSchema) {
        super(outputSchema, false, "Villager Follow Range Fix", References.ENTITY, "minecraft:villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), VillagerFollowRangeFix::fixValue);
    }

    private static Dynamic<?> fixValue(Dynamic<?> dynamic) {
        return dynamic.update("Attributes", (dynamic2) -> {
            return dynamic.createList(dynamic2.asStream().map((dynamicx) -> {
                return dynamicx.get("Name").asString("").equals("generic.follow_range") && dynamicx.get("Base").asDouble(0.0D) == 16.0D ? dynamicx.set("Base", dynamicx.createDouble(48.0D)) : dynamicx;
            }));
        });
    }
}
