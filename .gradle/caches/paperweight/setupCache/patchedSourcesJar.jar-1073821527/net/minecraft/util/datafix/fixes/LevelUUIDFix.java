package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import org.slf4j.Logger;

public class LevelUUIDFix extends AbstractUUIDFix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public LevelUUIDFix(Schema outputSchema) {
        super(outputSchema, References.LEVEL);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("LevelUUIDFix", this.getInputSchema().getType(this.typeReference), (typed) -> {
            return typed.updateTyped(DSL.remainderFinder(), (typedx) -> {
                return typedx.update(DSL.remainderFinder(), (dynamic) -> {
                    dynamic = this.updateCustomBossEvents(dynamic);
                    dynamic = this.updateDragonFight(dynamic);
                    return this.updateWanderingTrader(dynamic);
                });
            });
        });
    }

    private Dynamic<?> updateWanderingTrader(Dynamic<?> dynamic) {
        return replaceUUIDString(dynamic, "WanderingTraderId", "WanderingTraderId").orElse(dynamic);
    }

    private Dynamic<?> updateDragonFight(Dynamic<?> dynamic) {
        return dynamic.update("DimensionData", (dynamicx) -> {
            return dynamicx.updateMapValues((pair) -> {
                return pair.mapSecond((dynamic) -> {
                    return dynamic.update("DragonFight", (dynamicx) -> {
                        return replaceUUIDLeastMost(dynamicx, "DragonUUID", "Dragon").orElse(dynamicx);
                    });
                });
            });
        });
    }

    private Dynamic<?> updateCustomBossEvents(Dynamic<?> dynamic) {
        return dynamic.update("CustomBossEvents", (dynamicx) -> {
            return dynamicx.updateMapValues((pair) -> {
                return pair.mapSecond((dynamic) -> {
                    return dynamic.update("Players", (dynamic2) -> {
                        return dynamic.createList(dynamic2.asStream().map((dynamicx) -> {
                            return createUUIDFromML(dynamicx).orElseGet(() -> {
                                LOGGER.warn("CustomBossEvents contains invalid UUIDs.");
                                return dynamicx;
                            });
                        }));
                    });
                });
            });
        });
    }
}
