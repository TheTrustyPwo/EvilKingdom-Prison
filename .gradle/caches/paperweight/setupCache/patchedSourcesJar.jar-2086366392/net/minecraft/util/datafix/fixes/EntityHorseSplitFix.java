package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class EntityHorseSplitFix extends EntityRenameFix {
    public EntityHorseSplitFix(Schema outputSchema, boolean changesType) {
        super("EntityHorseSplitFix", outputSchema, changesType);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String choice, Typed<?> typed) {
        Dynamic<?> dynamic = typed.get(DSL.remainderFinder());
        if (Objects.equals("EntityHorse", choice)) {
            int i = dynamic.get("Type").asInt(0);
            String string;
            switch(i) {
            case 0:
            default:
                string = "Horse";
                break;
            case 1:
                string = "Donkey";
                break;
            case 2:
                string = "Mule";
                break;
            case 3:
                string = "ZombieHorse";
                break;
            case 4:
                string = "SkeletonHorse";
            }

            dynamic.remove("Type");
            Type<?> type = this.getOutputSchema().findChoiceType(References.ENTITY).types().get(string);
            return Pair.of(string, (Typed)((Pair)typed.write().flatMap(type::readTyped).result().orElseThrow(() -> {
                return new IllegalStateException("Could not parse the new horse");
            })).getFirst());
        } else {
            return Pair.of(choice, typed);
        }
    }
}
