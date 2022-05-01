package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class ItemModifierManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = Deserializers.createFunctionSerializer().create();
    private final PredicateManager predicateManager;
    private final LootTables lootTables;
    private Map<ResourceLocation, LootItemFunction> functions = ImmutableMap.of();

    public ItemModifierManager(PredicateManager lootConditionManager, LootTables lootManager) {
        super(GSON, "item_modifiers");
        this.predicateManager = lootConditionManager;
        this.lootTables = lootManager;
    }

    @Nullable
    public LootItemFunction get(ResourceLocation id) {
        return this.functions.get(id);
    }

    public LootItemFunction get(ResourceLocation id, LootItemFunction fallback) {
        return this.functions.getOrDefault(id, fallback);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        Builder<ResourceLocation, LootItemFunction> builder = ImmutableMap.builder();
        prepared.forEach((id, json) -> {
            try {
                if (json.isJsonArray()) {
                    LootItemFunction[] lootItemFunctions = GSON.fromJson(json, LootItemFunction[].class);
                    builder.put(id, new ItemModifierManager.FunctionSequence(lootItemFunctions));
                } else {
                    LootItemFunction lootItemFunction = GSON.fromJson(json, LootItemFunction.class);
                    builder.put(id, lootItemFunction);
                }
            } catch (Exception var4) {
                LOGGER.error("Couldn't parse item modifier {}", id, var4);
            }

        });
        Map<ResourceLocation, LootItemFunction> map = builder.build();
        ValidationContext validationContext = new ValidationContext(LootContextParamSets.ALL_PARAMS, this.predicateManager::get, this.lootTables::get);
        map.forEach((id, lootItemFunction) -> {
            lootItemFunction.validate(validationContext);
        });
        validationContext.getProblems().forEach((string, string2) -> {
            LOGGER.warn("Found item modifier validation problem in {}: {}", string, string2);
        });
        this.functions = map;
    }

    public Set<ResourceLocation> getKeys() {
        return Collections.unmodifiableSet(this.functions.keySet());
    }

    static class FunctionSequence implements LootItemFunction {
        protected final LootItemFunction[] functions;
        private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

        public FunctionSequence(LootItemFunction[] functions) {
            this.functions = functions;
            this.compositeFunction = LootItemFunctions.compose(functions);
        }

        @Override
        public ItemStack apply(ItemStack itemStack, LootContext lootContext) {
            return this.compositeFunction.apply(itemStack, lootContext);
        }

        @Override
        public LootItemFunctionType getType() {
            throw new UnsupportedOperationException();
        }
    }
}
