package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;

public class AbstractArrowPickupFix extends DataFix {
    public AbstractArrowPickupFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Schema schema = this.getInputSchema();
        return this.fixTypeEverywhereTyped("AbstractArrowPickupFix", schema.getType(References.ENTITY), this::updateProjectiles);
    }

    private Typed<?> updateProjectiles(Typed<?> typed) {
        typed = this.updateEntity(typed, "minecraft:arrow", AbstractArrowPickupFix::updatePickup);
        typed = this.updateEntity(typed, "minecraft:spectral_arrow", AbstractArrowPickupFix::updatePickup);
        return this.updateEntity(typed, "minecraft:trident", AbstractArrowPickupFix::updatePickup);
    }

    private static Dynamic<?> updatePickup(Dynamic<?> arrowData) {
        if (arrowData.get("pickup").result().isPresent()) {
            return arrowData;
        } else {
            boolean bl = arrowData.get("player").asBoolean(true);
            return arrowData.set("pickup", arrowData.createByte((byte)(bl ? 1 : 0))).remove("player");
        }
    }

    private Typed<?> updateEntity(Typed<?> typed, String choiceName, Function<Dynamic<?>, Dynamic<?>> updater) {
        Type<?> type = this.getInputSchema().getChoiceType(References.ENTITY, choiceName);
        Type<?> type2 = this.getOutputSchema().getChoiceType(References.ENTITY, choiceName);
        return typed.updateTyped(DSL.namedChoice(choiceName, type), type2, (t) -> {
            return t.update(DSL.remainderFinder(), updater);
        });
    }
}
