package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1920 extends NamespacedSchema {
    public V1920(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerInventory(Schema schema, Map<String, Supplier<TypeTemplate>> map, String string) {
        schema.register(map, string, () -> {
            return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema)));
        });
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
        registerInventory(schema, map, "minecraft:campfire");
        return map;
    }
}
