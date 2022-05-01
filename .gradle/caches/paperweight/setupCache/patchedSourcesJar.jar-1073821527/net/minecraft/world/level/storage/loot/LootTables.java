package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTables extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = Deserializers.createLootTableSerializer().create();
    private Map<ResourceLocation, LootTable> tables = ImmutableMap.of();
    private final PredicateManager predicateManager;

    public LootTables(PredicateManager conditionManager) {
        super(GSON, "loot_tables");
        this.predicateManager = conditionManager;
    }

    public LootTable get(ResourceLocation id) {
        return this.tables.getOrDefault(id, LootTable.EMPTY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        Builder<ResourceLocation, LootTable> builder = ImmutableMap.builder();
        JsonElement jsonElement = prepared.remove(BuiltInLootTables.EMPTY);
        if (jsonElement != null) {
            LOGGER.warn("Datapack tried to redefine {} loot table, ignoring", (Object)BuiltInLootTables.EMPTY);
        }

        prepared.forEach((id, json) -> {
            try {
                LootTable lootTable = GSON.fromJson(json, LootTable.class);
                builder.put(id, lootTable);
            } catch (Exception var4) {
                LOGGER.error("Couldn't parse loot table {}", id, var4);
            }

        });
        builder.put(BuiltInLootTables.EMPTY, LootTable.EMPTY);
        ImmutableMap<ResourceLocation, LootTable> immutableMap = builder.build();
        ValidationContext validationContext = new ValidationContext(LootContextParamSets.ALL_PARAMS, this.predicateManager::get, immutableMap::get);
        immutableMap.forEach((id, lootTable) -> {
            validate(validationContext, id, lootTable);
        });
        validationContext.getProblems().forEach((key, value) -> {
            LOGGER.warn("Found validation problem in {}: {}", key, value);
        });
        this.tables = immutableMap;
    }

    public static void validate(ValidationContext reporter, ResourceLocation id, LootTable table) {
        table.validate(reporter.setParams(table.getParamSet()).enterTable("{" + id + "}", id));
    }

    public static JsonElement serialize(LootTable table) {
        return GSON.toJsonTree(table);
    }

    public Set<ResourceLocation> getIds() {
        return this.tables.keySet();
    }
}
