package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1451_3 extends NamespacedSchema {
    public V1451_3(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
        schema.registerSimple(map, "minecraft:egg");
        schema.registerSimple(map, "minecraft:ender_pearl");
        schema.registerSimple(map, "minecraft:fireball");
        schema.register(map, "minecraft:potion", (string) -> {
            return DSL.optionalFields("Potion", References.ITEM_STACK.in(schema));
        });
        schema.registerSimple(map, "minecraft:small_fireball");
        schema.registerSimple(map, "minecraft:snowball");
        schema.registerSimple(map, "minecraft:wither_skull");
        schema.registerSimple(map, "minecraft:xp_bottle");
        schema.register(map, "minecraft:arrow", () -> {
            return DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(schema));
        });
        schema.register(map, "minecraft:enderman", () -> {
            return DSL.optionalFields("carriedBlockState", References.BLOCK_STATE.in(schema), V100.equipment(schema));
        });
        schema.register(map, "minecraft:falling_block", () -> {
            return DSL.optionalFields("BlockState", References.BLOCK_STATE.in(schema), "TileEntityData", References.BLOCK_ENTITY.in(schema));
        });
        schema.register(map, "minecraft:spectral_arrow", () -> {
            return DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(schema));
        });
        schema.register(map, "minecraft:chest_minecart", () -> {
            return DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(schema), "Items", DSL.list(References.ITEM_STACK.in(schema)));
        });
        schema.register(map, "minecraft:commandblock_minecart", () -> {
            return DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(schema));
        });
        schema.register(map, "minecraft:furnace_minecart", () -> {
            return DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(schema));
        });
        schema.register(map, "minecraft:hopper_minecart", () -> {
            return DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(schema), "Items", DSL.list(References.ITEM_STACK.in(schema)));
        });
        schema.register(map, "minecraft:minecart", () -> {
            return DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(schema));
        });
        schema.register(map, "minecraft:spawner_minecart", () -> {
            return DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(schema), References.UNTAGGED_SPAWNER.in(schema));
        });
        schema.register(map, "minecraft:tnt_minecart", () -> {
            return DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(schema));
        });
        return map;
    }
}
