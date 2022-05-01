package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import org.slf4j.Logger;

public class PredicateManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = Deserializers.createConditionSerializer().create();
    private Map<ResourceLocation, LootItemCondition> conditions = ImmutableMap.of();

    public PredicateManager() {
        super(GSON, "predicates");
    }

    @Nullable
    public LootItemCondition get(ResourceLocation id) {
        return this.conditions.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        Builder<ResourceLocation, LootItemCondition> builder = ImmutableMap.builder();
        prepared.forEach((id, json) -> {
            try {
                if (json.isJsonArray()) {
                    LootItemCondition[] lootItemConditions = GSON.fromJson(json, LootItemCondition[].class);
                    builder.put(id, new PredicateManager.CompositePredicate(lootItemConditions));
                } else {
                    LootItemCondition lootItemCondition = GSON.fromJson(json, LootItemCondition.class);
                    builder.put(id, lootItemCondition);
                }
            } catch (Exception var4) {
                LOGGER.error("Couldn't parse loot table {}", id, var4);
            }

        });
        Map<ResourceLocation, LootItemCondition> map = builder.build();
        ValidationContext validationContext = new ValidationContext(LootContextParamSets.ALL_PARAMS, map::get, (id) -> {
            return null;
        });
        map.forEach((id, condition) -> {
            condition.validate(validationContext.enterCondition("{" + id + "}", id));
        });
        validationContext.getProblems().forEach((string, string2) -> {
            LOGGER.warn("Found validation problem in {}: {}", string, string2);
        });
        this.conditions = map;
    }

    public Set<ResourceLocation> getKeys() {
        return Collections.unmodifiableSet(this.conditions.keySet());
    }

    static class CompositePredicate implements LootItemCondition {
        private final LootItemCondition[] terms;
        private final Predicate<LootContext> composedPredicate;

        CompositePredicate(LootItemCondition[] terms) {
            this.terms = terms;
            this.composedPredicate = LootItemConditions.andConditions(terms);
        }

        @Override
        public final boolean test(LootContext lootContext) {
            return this.composedPredicate.test(lootContext);
        }

        @Override
        public void validate(ValidationContext reporter) {
            LootItemCondition.super.validate(reporter);

            for(int i = 0; i < this.terms.length; ++i) {
                this.terms[i].validate(reporter.forChild(".term[" + i + "]"));
            }

        }

        @Override
        public LootItemConditionType getType() {
            throw new UnsupportedOperationException();
        }
    }
}
