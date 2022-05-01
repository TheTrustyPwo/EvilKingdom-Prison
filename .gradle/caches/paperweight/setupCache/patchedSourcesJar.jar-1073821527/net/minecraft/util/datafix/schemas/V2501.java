package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2501 extends NamespacedSchema {
    public V2501(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    private static void registerFurnace(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> {
            return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema)), "RecipesUsed", DSL.compoundList(References.RECIPE.in(schema), DSL.constType(DSL.intType())));
        });
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
        registerFurnace(schema, map, "minecraft:furnace");
        registerFurnace(schema, map, "minecraft:smoker");
        registerFurnace(schema, map, "minecraft:blast_furnace");
        return map;
    }
}
