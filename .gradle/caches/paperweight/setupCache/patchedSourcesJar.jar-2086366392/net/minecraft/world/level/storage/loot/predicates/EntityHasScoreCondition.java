package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public class EntityHasScoreCondition implements LootItemCondition {
    final Map<String, IntRange> scores;
    final LootContext.EntityTarget entityTarget;

    EntityHasScoreCondition(Map<String, IntRange> scores, LootContext.EntityTarget target) {
        this.scores = ImmutableMap.copyOf(scores);
        this.entityTarget = target;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ENTITY_SCORES;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Stream.concat(Stream.of(this.entityTarget.getParam()), this.scores.values().stream().flatMap((intRange) -> {
            return intRange.getReferencedContextParams().stream();
        })).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public boolean test(LootContext lootContext) {
        Entity entity = lootContext.getParamOrNull(this.entityTarget.getParam());
        if (entity == null) {
            return false;
        } else {
            Scoreboard scoreboard = entity.level.getScoreboard();

            for(Entry<String, IntRange> entry : this.scores.entrySet()) {
                if (!this.hasScore(lootContext, entity, scoreboard, entry.getKey(), entry.getValue())) {
                    return false;
                }
            }

            return true;
        }
    }

    protected boolean hasScore(LootContext context, Entity entity, Scoreboard scoreboard, String objectiveName, IntRange range) {
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return false;
        } else {
            String string = entity.getScoreboardName();
            return !scoreboard.hasPlayerScore(string, objective) ? false : range.test(context, scoreboard.getOrCreatePlayerScore(string, objective).getScore());
        }
    }

    public static EntityHasScoreCondition.Builder hasScores(LootContext.EntityTarget target) {
        return new EntityHasScoreCondition.Builder(target);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final Map<String, IntRange> scores = Maps.newHashMap();
        private final LootContext.EntityTarget entityTarget;

        public Builder(LootContext.EntityTarget target) {
            this.entityTarget = target;
        }

        public EntityHasScoreCondition.Builder withScore(String name, IntRange value) {
            this.scores.put(name, value);
            return this;
        }

        @Override
        public LootItemCondition build() {
            return new EntityHasScoreCondition(this.scores, this.entityTarget);
        }
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<EntityHasScoreCondition> {
        @Override
        public void serialize(JsonObject json, EntityHasScoreCondition object, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();

            for(Entry<String, IntRange> entry : object.scores.entrySet()) {
                jsonObject.add(entry.getKey(), context.serialize(entry.getValue()));
            }

            json.add("scores", jsonObject);
            json.add("entity", context.serialize(object.entityTarget));
        }

        @Override
        public EntityHasScoreCondition deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            Set<Entry<String, JsonElement>> set = GsonHelper.getAsJsonObject(jsonObject, "scores").entrySet();
            Map<String, IntRange> map = Maps.newLinkedHashMap();

            for(Entry<String, JsonElement> entry : set) {
                map.put(entry.getKey(), GsonHelper.convertToObject(entry.getValue(), "score", jsonDeserializationContext, IntRange.class));
            }

            return new EntityHasScoreCondition(map, GsonHelper.getAsObject(jsonObject, "entity", jsonDeserializationContext, LootContext.EntityTarget.class));
        }
    }
}
