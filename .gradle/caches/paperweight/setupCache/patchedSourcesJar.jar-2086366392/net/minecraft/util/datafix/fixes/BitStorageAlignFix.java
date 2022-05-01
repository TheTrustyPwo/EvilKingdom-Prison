package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.stream.LongStream;
import net.minecraft.util.Mth;

public class BitStorageAlignFix extends DataFix {
    private static final int BIT_TO_LONG_SHIFT = 6;
    private static final int SECTION_WIDTH = 16;
    private static final int SECTION_HEIGHT = 16;
    private static final int SECTION_SIZE = 4096;
    private static final int HEIGHTMAP_BITS = 9;
    private static final int HEIGHTMAP_SIZE = 256;

    public BitStorageAlignFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.CHUNK);
        Type<?> type2 = type.findFieldType("Level");
        OpticFinder<?> opticFinder = DSL.fieldFinder("Level", type2);
        OpticFinder<?> opticFinder2 = opticFinder.type().findField("Sections");
        Type<?> type3 = ((ListType)opticFinder2.type()).getElement();
        OpticFinder<?> opticFinder3 = DSL.typeFinder(type3);
        Type<Pair<String, Dynamic<?>>> type4 = DSL.named(References.BLOCK_STATE.typeName(), DSL.remainderType());
        OpticFinder<List<Pair<String, Dynamic<?>>>> opticFinder4 = DSL.fieldFinder("Palette", DSL.list(type4));
        return this.fixTypeEverywhereTyped("BitStorageAlignFix", type, this.getOutputSchema().getType(References.CHUNK), (chunk) -> {
            return chunk.updateTyped(opticFinder, (level) -> {
                return this.updateHeightmaps(updateSections(opticFinder2, opticFinder3, opticFinder4, level));
            });
        });
    }

    private Typed<?> updateHeightmaps(Typed<?> fixedLevel) {
        return fixedLevel.update(DSL.remainderFinder(), (levelDynamic) -> {
            return levelDynamic.update("Heightmaps", (heightmapsDynamic) -> {
                return heightmapsDynamic.updateMapValues((heightmap) -> {
                    return heightmap.mapSecond((heightmapDynamic) -> {
                        return updateBitStorage(levelDynamic, heightmapDynamic, 256, 9);
                    });
                });
            });
        });
    }

    private static Typed<?> updateSections(OpticFinder<?> levelSectionsFinder, OpticFinder<?> sectionFinder, OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder, Typed<?> level) {
        return level.updateTyped(levelSectionsFinder, (levelSection) -> {
            return levelSection.updateTyped(sectionFinder, (section) -> {
                int i = section.getOptional(paletteFinder).map((palette) -> {
                    return Math.max(4, DataFixUtils.ceillog2(palette.size()));
                }).orElse(0);
                return i != 0 && !Mth.isPowerOfTwo(i) ? section.update(DSL.remainderFinder(), (sectionDynamic) -> {
                    return sectionDynamic.update("BlockStates", (statesDynamic) -> {
                        return updateBitStorage(sectionDynamic, statesDynamic, 4096, i);
                    });
                }) : section;
            });
        });
    }

    private static Dynamic<?> updateBitStorage(Dynamic<?> sectionDynamic, Dynamic<?> statesDynamic, int maxValue, int elementBits) {
        long[] ls = statesDynamic.asLongStream().toArray();
        long[] ms = addPadding(maxValue, elementBits, ls);
        return sectionDynamic.createLongList(LongStream.of(ms));
    }

    public static long[] addPadding(int maxValue, int elementBits, long[] elements) {
        int i = elements.length;
        if (i == 0) {
            return elements;
        } else {
            long l = (1L << elementBits) - 1L;
            int j = 64 / elementBits;
            int k = (maxValue + j - 1) / j;
            long[] ls = new long[k];
            int m = 0;
            int n = 0;
            long o = 0L;
            int p = 0;
            long q = elements[0];
            long r = i > 1 ? elements[1] : 0L;

            for(int s = 0; s < maxValue; ++s) {
                int t = s * elementBits;
                int u = t >> 6;
                int v = (s + 1) * elementBits - 1 >> 6;
                int w = t ^ u << 6;
                if (u != p) {
                    q = r;
                    r = u + 1 < i ? elements[u + 1] : 0L;
                    p = u;
                }

                long x;
                if (u == v) {
                    x = q >>> w & l;
                } else {
                    int y = 64 - w;
                    x = (q >>> w | r << y) & l;
                }

                int aa = n + elementBits;
                if (aa >= 64) {
                    ls[m++] = o;
                    o = x;
                    n = elementBits;
                } else {
                    o |= x << n;
                    n = aa;
                }
            }

            if (o != 0L) {
                ls[m] = o;
            }

            return ls;
        }
    }
}
