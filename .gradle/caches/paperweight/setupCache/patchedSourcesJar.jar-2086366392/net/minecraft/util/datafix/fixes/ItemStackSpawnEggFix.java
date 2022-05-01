package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackSpawnEggFix extends DataFix {
    private static final Map<String, String> MAP = DataFixUtils.make(Maps.newHashMap(), (map) -> {
        map.put("minecraft:bat", "minecraft:bat_spawn_egg");
        map.put("minecraft:blaze", "minecraft:blaze_spawn_egg");
        map.put("minecraft:cave_spider", "minecraft:cave_spider_spawn_egg");
        map.put("minecraft:chicken", "minecraft:chicken_spawn_egg");
        map.put("minecraft:cow", "minecraft:cow_spawn_egg");
        map.put("minecraft:creeper", "minecraft:creeper_spawn_egg");
        map.put("minecraft:donkey", "minecraft:donkey_spawn_egg");
        map.put("minecraft:elder_guardian", "minecraft:elder_guardian_spawn_egg");
        map.put("minecraft:enderman", "minecraft:enderman_spawn_egg");
        map.put("minecraft:endermite", "minecraft:endermite_spawn_egg");
        map.put("minecraft:evocation_illager", "minecraft:evocation_illager_spawn_egg");
        map.put("minecraft:ghast", "minecraft:ghast_spawn_egg");
        map.put("minecraft:guardian", "minecraft:guardian_spawn_egg");
        map.put("minecraft:horse", "minecraft:horse_spawn_egg");
        map.put("minecraft:husk", "minecraft:husk_spawn_egg");
        map.put("minecraft:llama", "minecraft:llama_spawn_egg");
        map.put("minecraft:magma_cube", "minecraft:magma_cube_spawn_egg");
        map.put("minecraft:mooshroom", "minecraft:mooshroom_spawn_egg");
        map.put("minecraft:mule", "minecraft:mule_spawn_egg");
        map.put("minecraft:ocelot", "minecraft:ocelot_spawn_egg");
        map.put("minecraft:pufferfish", "minecraft:pufferfish_spawn_egg");
        map.put("minecraft:parrot", "minecraft:parrot_spawn_egg");
        map.put("minecraft:pig", "minecraft:pig_spawn_egg");
        map.put("minecraft:polar_bear", "minecraft:polar_bear_spawn_egg");
        map.put("minecraft:rabbit", "minecraft:rabbit_spawn_egg");
        map.put("minecraft:sheep", "minecraft:sheep_spawn_egg");
        map.put("minecraft:shulker", "minecraft:shulker_spawn_egg");
        map.put("minecraft:silverfish", "minecraft:silverfish_spawn_egg");
        map.put("minecraft:skeleton", "minecraft:skeleton_spawn_egg");
        map.put("minecraft:skeleton_horse", "minecraft:skeleton_horse_spawn_egg");
        map.put("minecraft:slime", "minecraft:slime_spawn_egg");
        map.put("minecraft:spider", "minecraft:spider_spawn_egg");
        map.put("minecraft:squid", "minecraft:squid_spawn_egg");
        map.put("minecraft:stray", "minecraft:stray_spawn_egg");
        map.put("minecraft:turtle", "minecraft:turtle_spawn_egg");
        map.put("minecraft:vex", "minecraft:vex_spawn_egg");
        map.put("minecraft:villager", "minecraft:villager_spawn_egg");
        map.put("minecraft:vindication_illager", "minecraft:vindication_illager_spawn_egg");
        map.put("minecraft:witch", "minecraft:witch_spawn_egg");
        map.put("minecraft:wither_skeleton", "minecraft:wither_skeleton_spawn_egg");
        map.put("minecraft:wolf", "minecraft:wolf_spawn_egg");
        map.put("minecraft:zombie", "minecraft:zombie_spawn_egg");
        map.put("minecraft:zombie_horse", "minecraft:zombie_horse_spawn_egg");
        map.put("minecraft:zombie_pigman", "minecraft:zombie_pigman_spawn_egg");
        map.put("minecraft:zombie_villager", "minecraft:zombie_villager_spawn_egg");
    });

    public ItemStackSpawnEggFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder<String> opticFinder2 = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
        OpticFinder<?> opticFinder3 = type.findField("tag");
        OpticFinder<?> opticFinder4 = opticFinder3.type().findField("EntityTag");
        return this.fixTypeEverywhereTyped("ItemInstanceSpawnEggFix", type, (typed) -> {
            Optional<Pair<String, String>> optional = typed.getOptional(opticFinder);
            if (optional.isPresent() && Objects.equals(optional.get().getSecond(), "minecraft:spawn_egg")) {
                Typed<?> typed2 = typed.getOrCreateTyped(opticFinder3);
                Typed<?> typed3 = typed2.getOrCreateTyped(opticFinder4);
                Optional<String> optional2 = typed3.getOptional(opticFinder2);
                if (optional2.isPresent()) {
                    return typed.set(opticFinder, Pair.of(References.ITEM_NAME.typeName(), MAP.getOrDefault(optional2.get(), "minecraft:pig_spawn_egg")));
                }
            }

            return typed;
        });
    }
}
