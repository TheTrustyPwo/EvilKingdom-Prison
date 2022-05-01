package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import java.util.Map;

public class BlockEntityIdFix extends DataFix {
    private static final Map<String, String> ID_MAP = DataFixUtils.make(Maps.newHashMap(), (map) -> {
        map.put("Airportal", "minecraft:end_portal");
        map.put("Banner", "minecraft:banner");
        map.put("Beacon", "minecraft:beacon");
        map.put("Cauldron", "minecraft:brewing_stand");
        map.put("Chest", "minecraft:chest");
        map.put("Comparator", "minecraft:comparator");
        map.put("Control", "minecraft:command_block");
        map.put("DLDetector", "minecraft:daylight_detector");
        map.put("Dropper", "minecraft:dropper");
        map.put("EnchantTable", "minecraft:enchanting_table");
        map.put("EndGateway", "minecraft:end_gateway");
        map.put("EnderChest", "minecraft:ender_chest");
        map.put("FlowerPot", "minecraft:flower_pot");
        map.put("Furnace", "minecraft:furnace");
        map.put("Hopper", "minecraft:hopper");
        map.put("MobSpawner", "minecraft:mob_spawner");
        map.put("Music", "minecraft:noteblock");
        map.put("Piston", "minecraft:piston");
        map.put("RecordPlayer", "minecraft:jukebox");
        map.put("Sign", "minecraft:sign");
        map.put("Skull", "minecraft:skull");
        map.put("Structure", "minecraft:structure_block");
        map.put("Trap", "minecraft:dispenser");
    });

    public BlockEntityIdFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        Type<?> type2 = this.getOutputSchema().getType(References.ITEM_STACK);
        TaggedChoiceType<String> taggedChoiceType = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
        TaggedChoiceType<String> taggedChoiceType2 = this.getOutputSchema().findChoiceType(References.BLOCK_ENTITY);
        return TypeRewriteRule.seq(this.convertUnchecked("item stack block entity name hook converter", type, type2), this.fixTypeEverywhere("BlockEntityIdFix", taggedChoiceType, taggedChoiceType2, (dynamicOps) -> {
            return (pair) -> {
                return pair.mapFirst((string) -> {
                    return ID_MAP.getOrDefault(string, string);
                });
            };
        }));
    }
}
