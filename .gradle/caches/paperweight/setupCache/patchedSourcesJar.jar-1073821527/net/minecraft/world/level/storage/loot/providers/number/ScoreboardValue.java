package net.minecraft.world.level.storage.loot.providers.number;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.providers.score.ContextScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public class ScoreboardValue implements NumberProvider {
    final ScoreboardNameProvider target;
    final String score;
    final float scale;

    ScoreboardValue(ScoreboardNameProvider target, String score, float scale) {
        this.target = target;
        this.score = score;
        this.scale = scale;
    }

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.SCORE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.target.getReferencedContextParams();
    }

    public static ScoreboardValue fromScoreboard(LootContext.EntityTarget target, String score) {
        return fromScoreboard(target, score, 1.0F);
    }

    public static ScoreboardValue fromScoreboard(LootContext.EntityTarget target, String score, float scale) {
        return new ScoreboardValue(ContextScoreboardNameProvider.forTarget(target), score, scale);
    }

    @Override
    public float getFloat(LootContext context) {
        String string = this.target.getScoreboardName(context);
        if (string == null) {
            return 0.0F;
        } else {
            Scoreboard scoreboard = context.getLevel().getScoreboard();
            Objective objective = scoreboard.getObjective(this.score);
            if (objective == null) {
                return 0.0F;
            } else {
                return !scoreboard.hasPlayerScore(string, objective) ? 0.0F : (float)scoreboard.getOrCreatePlayerScore(string, objective).getScore() * this.scale;
            }
        }
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ScoreboardValue> {
        @Override
        public ScoreboardValue deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            String string = GsonHelper.getAsString(jsonObject, "score");
            float f = GsonHelper.getAsFloat(jsonObject, "scale", 1.0F);
            ScoreboardNameProvider scoreboardNameProvider = GsonHelper.getAsObject(jsonObject, "target", jsonDeserializationContext, ScoreboardNameProvider.class);
            return new ScoreboardValue(scoreboardNameProvider, string, f);
        }

        @Override
        public void serialize(JsonObject json, ScoreboardValue object, JsonSerializationContext context) {
            json.addProperty("score", object.score);
            json.add("target", context.serialize(object.target));
            json.addProperty("scale", object.scale);
        }
    }
}
