package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTables extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = Deserializers.createLootTableSerializer().create();
    private Map<ResourceLocation, LootTable> tables = ImmutableMap.of();
    public Map<LootTable, ResourceLocation> lootTableToKey = ImmutableMap.of(); // CraftBukkit
    private final PredicateManager predicateManager;

    public LootTables(PredicateManager conditionManager) {
        super(LootTables.GSON, "loot_tables");
        this.predicateManager = conditionManager;
    }

    public LootTable get(ResourceLocation id) {
        return (LootTable) this.tables.getOrDefault(id, LootTable.EMPTY);
    }

    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        Builder<ResourceLocation, LootTable> builder = ImmutableMap.builder();
        JsonElement jsonelement = (JsonElement) prepared.remove(BuiltInLootTables.EMPTY);

        if (jsonelement != null) {
            LootTables.LOGGER.warn("Datapack tried to redefine {} loot table, ignoring", BuiltInLootTables.EMPTY);
        }

        prepared.forEach((minecraftkey, jsonelement1) -> {
            try {
                LootTable loottable = (LootTable) LootTables.GSON.fromJson(jsonelement1, LootTable.class);

                builder.put(minecraftkey, loottable);
            } catch (Exception exception) {
                LootTables.LOGGER.error("Couldn't parse loot table {}", minecraftkey, exception);
            }

        });
        builder.put(BuiltInLootTables.EMPTY, LootTable.EMPTY);
        ImmutableMap<ResourceLocation, LootTable> immutablemap = builder.build();
        LootContextParamSet lootcontextparameterset = LootContextParamSets.ALL_PARAMS;
        PredicateManager lootpredicatemanager = this.predicateManager;

        Objects.requireNonNull(this.predicateManager);
        Function<ResourceLocation, net.minecraft.world.level.storage.loot.predicates.LootItemCondition> function = lootpredicatemanager::get; // CraftBukkit - decompile error

        Objects.requireNonNull(immutablemap);
        ValidationContext lootcollector = new ValidationContext(lootcontextparameterset, function, immutablemap::get);

        immutablemap.forEach((minecraftkey, loottable) -> {
            LootTables.validate(lootcollector, minecraftkey, loottable);
        });
        lootcollector.getProblems().forEach((s, s1) -> {
            LootTables.LOGGER.warn("Found validation problem in {}: {}", s, s1);
        });
        this.tables = immutablemap;
        // CraftBukkit start - build a reversed registry map
        ImmutableMap.Builder<LootTable, ResourceLocation> lootTableToKeyBuilder = ImmutableMap.builder();
        this.tables.forEach((lootTable, key) -> lootTableToKeyBuilder.put(key, lootTable));
        this.lootTableToKey = lootTableToKeyBuilder.build();
        // CraftBukkit end
    }

    public static void validate(ValidationContext reporter, ResourceLocation id, LootTable table) {
        table.validate(reporter.setParams(table.getParamSet()).enterTable("{" + id + "}", id));
    }

    public static JsonElement serialize(LootTable table) {
        return LootTables.GSON.toJsonTree(table);
    }

    public Set<ResourceLocation> getIds() {
        return this.tables.keySet();
    }
}
