package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class RedstoneWireConnectionsFix extends DataFix {
    public RedstoneWireConnectionsFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Schema schema = this.getInputSchema();
        return this.fixTypeEverywhereTyped("RedstoneConnectionsFix", schema.getType(References.BLOCK_STATE), (typed) -> {
            return typed.update(DSL.remainderFinder(), this::updateRedstoneConnections);
        });
    }

    private <T> Dynamic<T> updateRedstoneConnections(Dynamic<T> dynamic) {
        boolean bl = dynamic.get("Name").asString().result().filter("minecraft:redstone_wire"::equals).isPresent();
        return !bl ? dynamic : dynamic.update("Properties", (dynamicx) -> {
            String string = dynamicx.get("east").asString("none");
            String string2 = dynamicx.get("west").asString("none");
            String string3 = dynamicx.get("north").asString("none");
            String string4 = dynamicx.get("south").asString("none");
            boolean bl = isConnected(string) || isConnected(string2);
            boolean bl2 = isConnected(string3) || isConnected(string4);
            String string5 = !isConnected(string) && !bl2 ? "side" : string;
            String string6 = !isConnected(string2) && !bl2 ? "side" : string2;
            String string7 = !isConnected(string3) && !bl ? "side" : string3;
            String string8 = !isConnected(string4) && !bl ? "side" : string4;
            return dynamicx.update("east", (dynamic) -> {
                return dynamic.createString(string5);
            }).update("west", (dynamic) -> {
                return dynamic.createString(string6);
            }).update("north", (dynamic) -> {
                return dynamic.createString(string7);
            }).update("south", (dynamic) -> {
                return dynamic.createString(string8);
            });
        });
    }

    private static boolean isConnected(String string) {
        return !"none".equals(string);
    }
}
