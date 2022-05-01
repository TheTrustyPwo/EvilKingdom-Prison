package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.stream.Stream;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class WorldGenSettingsHeightAndBiomeFix extends DataFix {
    private static final String NAME = "WorldGenSettingsHeightAndBiomeFix";
    public static final String WAS_PREVIOUSLY_INCREASED_KEY = "has_increased_height_already";

    public WorldGenSettingsHeightAndBiomeFix(Schema schema) {
        super(schema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.WORLD_GEN_SETTINGS);
        OpticFinder<?> opticFinder = type.findField("dimensions");
        Type<?> type2 = this.getOutputSchema().getType(References.WORLD_GEN_SETTINGS);
        Type<?> type3 = type2.findFieldType("dimensions");
        return this.fixTypeEverywhereTyped("WorldGenSettingsHeightAndBiomeFix", type, type2, (typed) -> {
            OptionalDynamic<?> optionalDynamic = typed.get(DSL.remainderFinder()).get("has_increased_height_already");
            boolean bl = optionalDynamic.result().isEmpty();
            boolean bl2 = optionalDynamic.asBoolean(true);
            return typed.update(DSL.remainderFinder(), (dynamic) -> {
                return dynamic.remove("has_increased_height_already");
            }).updateTyped(opticFinder, type3, (typedx) -> {
                Dynamic<?> dynamic = typedx.write().result().orElseThrow(() -> {
                    return new IllegalStateException("Malformed WorldGenSettings.dimensions");
                });
                dynamic = dynamic.update("minecraft:overworld", (dynamicx) -> {
                    return dynamicx.update("generator", (dynamic) -> {
                        String string = dynamic.get("type").asString("");
                        if ("minecraft:noise".equals(string)) {
                            MutableBoolean mutableBoolean = new MutableBoolean();
                            dynamic = dynamic.update("biome_source", (dynamicx) -> {
                                String string = dynamicx.get("type").asString("");
                                if ("minecraft:vanilla_layered".equals(string) || bl && "minecraft:multi_noise".equals(string)) {
                                    if (dynamicx.get("large_biomes").asBoolean(false)) {
                                        mutableBoolean.setTrue();
                                    }

                                    return dynamicx.createMap(ImmutableMap.of(dynamicx.createString("preset"), dynamicx.createString("minecraft:overworld"), dynamicx.createString("type"), dynamicx.createString("minecraft:multi_noise")));
                                } else {
                                    return dynamicx;
                                }
                            });
                            return mutableBoolean.booleanValue() ? dynamic.update("settings", (dynamicx) -> {
                                return "minecraft:overworld".equals(dynamicx.asString("")) ? dynamicx.createString("minecraft:large_biomes") : dynamicx;
                            }) : dynamic;
                        } else if ("minecraft:flat".equals(string)) {
                            return bl2 ? dynamic : dynamic.update("settings", (dynamicx) -> {
                                return dynamicx.update("layers", WorldGenSettingsHeightAndBiomeFix::updateLayers);
                            });
                        } else {
                            return dynamic;
                        }
                    });
                });
                return type3.readTyped(dynamic).result().orElseThrow(() -> {
                    return new IllegalStateException("WorldGenSettingsHeightAndBiomeFix failed.");
                }).getFirst();
            });
        });
    }

    private static Dynamic<?> updateLayers(Dynamic<?> dynamic) {
        Dynamic<?> dynamic2 = dynamic.createMap(ImmutableMap.of(dynamic.createString("height"), dynamic.createInt(64), dynamic.createString("block"), dynamic.createString("minecraft:air")));
        return dynamic.createList(Stream.concat(Stream.of(dynamic2), dynamic.asStream()));
    }
}
