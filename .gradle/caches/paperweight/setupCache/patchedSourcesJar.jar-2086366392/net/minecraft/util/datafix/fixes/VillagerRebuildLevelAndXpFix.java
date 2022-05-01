package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.Mth;

public class VillagerRebuildLevelAndXpFix extends DataFix {
    private static final int TRADES_PER_LEVEL = 2;
    private static final int[] LEVEL_XP_THRESHOLDS = new int[]{0, 10, 50, 100, 150};

    public static int getMinXpPerLevel(int level) {
        return LEVEL_XP_THRESHOLDS[Mth.clamp(level - 1, 0, LEVEL_XP_THRESHOLDS.length - 1)];
    }

    public VillagerRebuildLevelAndXpFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:villager");
        OpticFinder<?> opticFinder = DSL.namedChoice("minecraft:villager", type);
        OpticFinder<?> opticFinder2 = type.findField("Offers");
        Type<?> type2 = opticFinder2.type();
        OpticFinder<?> opticFinder3 = type2.findField("Recipes");
        ListType<?> listType = (ListType)opticFinder3.type();
        OpticFinder<?> opticFinder4 = listType.getElement().finder();
        return this.fixTypeEverywhereTyped("Villager level and xp rebuild", this.getInputSchema().getType(References.ENTITY), (typed) -> {
            return typed.updateTyped(opticFinder, type, (typedx) -> {
                Dynamic<?> dynamic = typedx.get(DSL.remainderFinder());
                int i = dynamic.get("VillagerData").get("level").asInt(0);
                Typed<?> typed2 = typedx;
                if (i == 0 || i == 1) {
                    int j = typedx.getOptionalTyped(opticFinder2).flatMap((typed) -> {
                        return typed.getOptionalTyped(opticFinder3);
                    }).map((typed) -> {
                        return typed.getAllTyped(opticFinder4).size();
                    }).orElse(0);
                    i = Mth.clamp(j / 2, 1, 5);
                    if (i > 1) {
                        typed2 = addLevel(typedx, i);
                    }
                }

                Optional<Number> optional = dynamic.get("Xp").asNumber().result();
                if (!optional.isPresent()) {
                    typed2 = addXpFromLevel(typed2, i);
                }

                return typed2;
            });
        });
    }

    private static Typed<?> addLevel(Typed<?> typed, int i) {
        return typed.update(DSL.remainderFinder(), (dynamic) -> {
            return dynamic.update("VillagerData", (dynamicx) -> {
                return dynamicx.set("level", dynamicx.createInt(i));
            });
        });
    }

    private static Typed<?> addXpFromLevel(Typed<?> typed, int i) {
        int j = getMinXpPerLevel(i);
        return typed.update(DSL.remainderFinder(), (dynamic) -> {
            return dynamic.set("Xp", dynamic.createInt(j));
        });
    }
}
