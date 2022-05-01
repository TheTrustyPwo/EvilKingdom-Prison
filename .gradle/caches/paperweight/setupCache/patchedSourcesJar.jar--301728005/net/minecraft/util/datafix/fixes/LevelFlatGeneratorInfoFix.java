package net.minecraft.util.datafix.fixes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.math.NumberUtils;

public class LevelFlatGeneratorInfoFix extends DataFix {
    private static final String GENERATOR_OPTIONS = "generatorOptions";
    @VisibleForTesting
    static final String DEFAULT = "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;1;village";
    private static final Splitter SPLITTER = Splitter.on(';').limit(5);
    private static final Splitter LAYER_SPLITTER = Splitter.on(',');
    private static final Splitter OLD_AMOUNT_SPLITTER = Splitter.on('x').limit(2);
    private static final Splitter AMOUNT_SPLITTER = Splitter.on('*').limit(2);
    private static final Splitter BLOCK_SPLITTER = Splitter.on(':').limit(3);

    public LevelFlatGeneratorInfoFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("LevelFlatGeneratorInfoFix", this.getInputSchema().getType(References.LEVEL), (typed) -> {
            return typed.update(DSL.remainderFinder(), this::fix);
        });
    }

    private Dynamic<?> fix(Dynamic<?> dynamic) {
        return dynamic.get("generatorName").asString("").equalsIgnoreCase("flat") ? dynamic.update("generatorOptions", (dynamicx) -> {
            return DataFixUtils.orElse(dynamicx.asString().map(this::fixString).map(dynamicx::createString).result(), dynamicx);
        }) : dynamic;
    }

    @VisibleForTesting
    String fixString(String generatorOptions) {
        if (generatorOptions.isEmpty()) {
            return "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;1;village";
        } else {
            Iterator<String> iterator = SPLITTER.split(generatorOptions).iterator();
            String string = iterator.next();
            int i;
            String string2;
            if (iterator.hasNext()) {
                i = NumberUtils.toInt(string, 0);
                string2 = iterator.next();
            } else {
                i = 0;
                string2 = string;
            }

            if (i >= 0 && i <= 3) {
                StringBuilder stringBuilder = new StringBuilder();
                Splitter splitter = i < 3 ? OLD_AMOUNT_SPLITTER : AMOUNT_SPLITTER;
                stringBuilder.append(StreamSupport.stream(LAYER_SPLITTER.split(string2).spliterator(), false).map((stringx) -> {
                    List<String> list = splitter.splitToList(stringx);
                    int j;
                    String string2;
                    if (list.size() == 2) {
                        j = NumberUtils.toInt(list.get(0));
                        string2 = list.get(1);
                    } else {
                        j = 1;
                        string2 = list.get(0);
                    }

                    List<String> list2 = BLOCK_SPLITTER.splitToList(string2);
                    int l = list2.get(0).equals("minecraft") ? 1 : 0;
                    String string4 = list2.get(l);
                    int m = i == 3 ? EntityBlockStateFix.getBlockId("minecraft:" + string4) : NumberUtils.toInt(string4, 0);
                    int n = l + 1;
                    int o = list2.size() > n ? NumberUtils.toInt(list2.get(n), 0) : 0;
                    return (j == 1 ? "" : j + "*") + BlockStateData.getTag(m << 4 | o).get("Name").asString("");
                }).collect(Collectors.joining(",")));

                while(iterator.hasNext()) {
                    stringBuilder.append(';').append(iterator.next());
                }

                return stringBuilder.toString();
            } else {
                return "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;1;village";
            }
        }
    }
}
