package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Arrays;
import java.util.function.Function;

public class EntityProjectileOwnerFix extends DataFix {
    public EntityProjectileOwnerFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Schema schema = this.getInputSchema();
        return this.fixTypeEverywhereTyped("EntityProjectileOwner", schema.getType(References.ENTITY), this::updateProjectiles);
    }

    private Typed<?> updateProjectiles(Typed<?> typed) {
        typed = this.updateEntity(typed, "minecraft:egg", this::updateOwnerThrowable);
        typed = this.updateEntity(typed, "minecraft:ender_pearl", this::updateOwnerThrowable);
        typed = this.updateEntity(typed, "minecraft:experience_bottle", this::updateOwnerThrowable);
        typed = this.updateEntity(typed, "minecraft:snowball", this::updateOwnerThrowable);
        typed = this.updateEntity(typed, "minecraft:potion", this::updateOwnerThrowable);
        typed = this.updateEntity(typed, "minecraft:potion", this::updateItemPotion);
        typed = this.updateEntity(typed, "minecraft:llama_spit", this::updateOwnerLlamaSpit);
        typed = this.updateEntity(typed, "minecraft:arrow", this::updateOwnerArrow);
        typed = this.updateEntity(typed, "minecraft:spectral_arrow", this::updateOwnerArrow);
        return this.updateEntity(typed, "minecraft:trident", this::updateOwnerArrow);
    }

    private Dynamic<?> updateOwnerArrow(Dynamic<?> dynamic) {
        long l = dynamic.get("OwnerUUIDMost").asLong(0L);
        long m = dynamic.get("OwnerUUIDLeast").asLong(0L);
        return this.setUUID(dynamic, l, m).remove("OwnerUUIDMost").remove("OwnerUUIDLeast");
    }

    private Dynamic<?> updateOwnerLlamaSpit(Dynamic<?> dynamic) {
        OptionalDynamic<?> optionalDynamic = dynamic.get("Owner");
        long l = optionalDynamic.get("OwnerUUIDMost").asLong(0L);
        long m = optionalDynamic.get("OwnerUUIDLeast").asLong(0L);
        return this.setUUID(dynamic, l, m).remove("Owner");
    }

    private Dynamic<?> updateItemPotion(Dynamic<?> dynamic) {
        OptionalDynamic<?> optionalDynamic = dynamic.get("Potion");
        return dynamic.set("Item", optionalDynamic.orElseEmptyMap()).remove("Potion");
    }

    private Dynamic<?> updateOwnerThrowable(Dynamic<?> dynamic) {
        String string = "owner";
        OptionalDynamic<?> optionalDynamic = dynamic.get("owner");
        long l = optionalDynamic.get("M").asLong(0L);
        long m = optionalDynamic.get("L").asLong(0L);
        return this.setUUID(dynamic, l, m).remove("owner");
    }

    private Dynamic<?> setUUID(Dynamic<?> dynamic, long most, long least) {
        String string = "OwnerUUID";
        return most != 0L && least != 0L ? dynamic.set("OwnerUUID", dynamic.createIntList(Arrays.stream(createUUIDArray(most, least)))) : dynamic;
    }

    private static int[] createUUIDArray(long most, long least) {
        return new int[]{(int)(most >> 32), (int)most, (int)(least >> 32), (int)least};
    }

    private Typed<?> updateEntity(Typed<?> typed, String string, Function<Dynamic<?>, Dynamic<?>> function) {
        Type<?> type = this.getInputSchema().getChoiceType(References.ENTITY, string);
        Type<?> type2 = this.getOutputSchema().getChoiceType(References.ENTITY, string);
        return typed.updateTyped(DSL.namedChoice(string, type), type2, (typedx) -> {
            return typedx.update(DSL.remainderFinder(), function);
        });
    }
}
