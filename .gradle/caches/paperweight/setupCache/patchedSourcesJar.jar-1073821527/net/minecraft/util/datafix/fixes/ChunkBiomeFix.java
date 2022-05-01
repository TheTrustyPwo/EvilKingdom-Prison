package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

public class ChunkBiomeFix extends DataFix {
    public ChunkBiomeFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.CHUNK);
        OpticFinder<?> opticFinder = type.findField("Level");
        return this.fixTypeEverywhereTyped("Leaves fix", type, (typed) -> {
            return typed.updateTyped(opticFinder, (typedx) -> {
                return typedx.update(DSL.remainderFinder(), (dynamic) -> {
                    Optional<IntStream> optional = dynamic.get("Biomes").asIntStreamOpt().result();
                    if (optional.isEmpty()) {
                        return dynamic;
                    } else {
                        int[] is = optional.get().toArray();
                        if (is.length != 256) {
                            return dynamic;
                        } else {
                            int[] js = new int[1024];

                            for(int i = 0; i < 4; ++i) {
                                for(int j = 0; j < 4; ++j) {
                                    int k = (j << 2) + 2;
                                    int l = (i << 2) + 2;
                                    int m = l << 4 | k;
                                    js[i << 2 | j] = is[m];
                                }
                            }

                            for(int n = 1; n < 64; ++n) {
                                System.arraycopy(js, 0, js, n * 16, 16);
                            }

                            return dynamic.set("Biomes", dynamic.createIntList(Arrays.stream(js)));
                        }
                    }
                });
            });
        });
    }
}
