package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;

public class V1451_6 extends NamespacedSchema {
    public static final String SPECIAL_OBJECTIVE_MARKER = "_special";
    protected static final HookFunction UNPACK_OBJECTIVE_ID = new HookFunction() {
        public <T> T apply(DynamicOps<T> dynamicOps, T object) {
            Dynamic<T> dynamic = new Dynamic<>(dynamicOps, object);
            return DataFixUtils.orElse(dynamic.get("CriteriaName").asString().get().left().map((string) -> {
                int i = string.indexOf(58);
                if (i < 0) {
                    return Pair.of("_special", string);
                } else {
                    try {
                        ResourceLocation resourceLocation = ResourceLocation.of(string.substring(0, i), '.');
                        ResourceLocation resourceLocation2 = ResourceLocation.of(string.substring(i + 1), '.');
                        return Pair.of(resourceLocation.toString(), resourceLocation2.toString());
                    } catch (Exception var4) {
                        return Pair.of("_special", string);
                    }
                }
            }).map((pair) -> {
                return dynamic.set("CriteriaType", dynamic.createMap(ImmutableMap.of(dynamic.createString("type"), dynamic.createString(pair.getFirst()), dynamic.createString("id"), dynamic.createString(pair.getSecond()))));
            }), dynamic).getValue();
        }
    };
    protected static final HookFunction REPACK_OBJECTIVE_ID = new HookFunction() {
        private String packWithDot(String string) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
            return resourceLocation != null ? resourceLocation.getNamespace() + "." + resourceLocation.getPath() : string;
        }

        public <T> T apply(DynamicOps<T> dynamicOps, T object) {
            Dynamic<T> dynamic = new Dynamic<>(dynamicOps, object);
            Optional<Dynamic<T>> optional = dynamic.get("CriteriaType").get().get().left().flatMap((dynamic2) -> {
                Optional<String> optional = dynamic2.get("type").asString().get().left();
                Optional<String> optional2 = dynamic2.get("id").asString().get().left();
                if (optional.isPresent() && optional2.isPresent()) {
                    String string = optional.get();
                    return string.equals("_special") ? Optional.of(dynamic.createString(optional2.get())) : Optional.of(dynamic2.createString(this.packWithDot(string) + ":" + this.packWithDot(optional2.get())));
                } else {
                    return Optional.empty();
                }
            });
            return DataFixUtils.orElse(optional.map((dynamic2) -> {
                return dynamic.set("CriteriaName", dynamic2).remove("CriteriaType");
            }), dynamic).getValue();
        }
    };

    public V1451_6(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
        super.registerTypes(schema, map, map2);
        Supplier<TypeTemplate> supplier = () -> {
            return DSL.compoundList(References.ITEM_NAME.in(schema), DSL.constType(DSL.intType()));
        };
        schema.registerType(false, References.STATS, () -> {
            return DSL.optionalFields("stats", DSL.optionalFields("minecraft:mined", DSL.compoundList(References.BLOCK_NAME.in(schema), DSL.constType(DSL.intType())), "minecraft:crafted", supplier.get(), "minecraft:used", supplier.get(), "minecraft:broken", supplier.get(), "minecraft:picked_up", supplier.get(), DSL.optionalFields("minecraft:dropped", supplier.get(), "minecraft:killed", DSL.compoundList(References.ENTITY_NAME.in(schema), DSL.constType(DSL.intType())), "minecraft:killed_by", DSL.compoundList(References.ENTITY_NAME.in(schema), DSL.constType(DSL.intType())), "minecraft:custom", DSL.compoundList(DSL.constType(namespacedString()), DSL.constType(DSL.intType())))));
        });
        Map<String, Supplier<TypeTemplate>> map3 = createCriterionTypes(schema);
        schema.registerType(false, References.OBJECTIVE, () -> {
            return DSL.hook(DSL.optionalFields("CriteriaType", DSL.taggedChoiceLazy("type", DSL.string(), map3)), UNPACK_OBJECTIVE_ID, REPACK_OBJECTIVE_ID);
        });
    }

    protected static Map<String, Supplier<TypeTemplate>> createCriterionTypes(Schema schema) {
        Supplier<TypeTemplate> supplier = () -> {
            return DSL.optionalFields("id", References.ITEM_NAME.in(schema));
        };
        Supplier<TypeTemplate> supplier2 = () -> {
            return DSL.optionalFields("id", References.BLOCK_NAME.in(schema));
        };
        Supplier<TypeTemplate> supplier3 = () -> {
            return DSL.optionalFields("id", References.ENTITY_NAME.in(schema));
        };
        Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
        map.put("minecraft:mined", supplier2);
        map.put("minecraft:crafted", supplier);
        map.put("minecraft:used", supplier);
        map.put("minecraft:broken", supplier);
        map.put("minecraft:picked_up", supplier);
        map.put("minecraft:dropped", supplier);
        map.put("minecraft:killed", supplier3);
        map.put("minecraft:killed_by", supplier3);
        map.put("minecraft:custom", () -> {
            return DSL.optionalFields("id", DSL.constType(namespacedString()));
        });
        map.put("_special", () -> {
            return DSL.optionalFields("id", DSL.constType(DSL.string()));
        });
        return map;
    }
}
