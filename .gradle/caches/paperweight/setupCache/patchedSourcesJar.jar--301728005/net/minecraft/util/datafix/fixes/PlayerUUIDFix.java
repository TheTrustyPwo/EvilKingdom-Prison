package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class PlayerUUIDFix extends AbstractUUIDFix {
    public PlayerUUIDFix(Schema outputSchema) {
        super(outputSchema, References.PLAYER);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("PlayerUUIDFix", this.getInputSchema().getType(this.typeReference), (typed) -> {
            OpticFinder<?> opticFinder = typed.getType().findField("RootVehicle");
            return typed.updateTyped(opticFinder, opticFinder.type(), (typedx) -> {
                return typedx.update(DSL.remainderFinder(), (dynamic) -> {
                    return replaceUUIDLeastMost(dynamic, "Attach", "Attach").orElse(dynamic);
                });
            }).update(DSL.remainderFinder(), (dynamic) -> {
                return EntityUUIDFix.updateEntityUUID(EntityUUIDFix.updateLivingEntity(dynamic));
            });
        });
    }
}
