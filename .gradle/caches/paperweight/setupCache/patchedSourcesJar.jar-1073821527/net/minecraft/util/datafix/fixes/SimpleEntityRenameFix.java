package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public abstract class SimpleEntityRenameFix extends EntityRenameFix {
    public SimpleEntityRenameFix(String name, Schema outputSchema, boolean changesType) {
        super(name, outputSchema, changesType);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String choice, Typed<?> typed) {
        Pair<String, Dynamic<?>> pair = this.getNewNameAndTag(choice, typed.getOrCreate(DSL.remainderFinder()));
        return Pair.of(pair.getFirst(), typed.set(DSL.remainderFinder(), pair.getSecond()));
    }

    protected abstract Pair<String, Dynamic<?>> getNewNameAndTag(String choice, Dynamic<?> dynamic);
}
