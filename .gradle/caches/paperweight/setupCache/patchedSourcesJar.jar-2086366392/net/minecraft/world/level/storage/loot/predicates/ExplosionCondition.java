package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Random;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ExplosionCondition implements LootItemCondition {

    static final ExplosionCondition INSTANCE = new ExplosionCondition();

    private ExplosionCondition() {}

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.SURVIVES_EXPLOSION;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.EXPLOSION_RADIUS);
    }

    public boolean test(LootContext loottableinfo) {
        Float ofloat = (Float) loottableinfo.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);

        if (ofloat != null) {
            Random random = loottableinfo.getRandom();
            float f = 1.0F / ofloat;

            // CraftBukkit - <= to < to allow for plugins to completely disable block drops from explosions
            return random.nextFloat() < f;
        } else {
            return true;
        }
    }

    public static LootItemCondition.Builder survivesExplosion() {
        return () -> {
            return ExplosionCondition.INSTANCE;
        };
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ExplosionCondition> {

        public Serializer() {}

        public void serialize(JsonObject json, ExplosionCondition object, JsonSerializationContext context) {}

        @Override
        public ExplosionCondition deserialize(JsonObject json, JsonDeserializationContext context) {
            return ExplosionCondition.INSTANCE;
        }
    }
}
