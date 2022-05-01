package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.random.SimpleWeightedRandomList;

public record SpawnData(CompoundTag entityToSpawn, Optional<SpawnData.CustomSpawnRules> customSpawnRules) {
    public static final Codec<SpawnData> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(CompoundTag.CODEC.fieldOf("entity").forGetter((entry) -> {
            return entry.entityToSpawn;
        }), SpawnData.CustomSpawnRules.CODEC.optionalFieldOf("custom_spawn_rules").forGetter((spawnData) -> {
            return spawnData.customSpawnRules;
        })).apply(instance, SpawnData::new);
    });
    public static final Codec<SimpleWeightedRandomList<SpawnData>> LIST_CODEC = SimpleWeightedRandomList.wrappedCodecAllowingEmpty(CODEC);
    public static final String DEFAULT_TYPE = "minecraft:pig";

    public SpawnData() {
        this(Util.make(new CompoundTag(), (nbt) -> {
            nbt.putString("id", "minecraft:pig");
        }), Optional.empty());
    }

    public SpawnData {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(compoundTag.getString("id"));
        compoundTag.putString("id", resourceLocation != null ? resourceLocation.toString() : "minecraft:pig");
    }

    public CompoundTag getEntityToSpawn() {
        return this.entityToSpawn;
    }

    public Optional<SpawnData.CustomSpawnRules> getCustomSpawnRules() {
        return this.customSpawnRules;
    }

    public static record CustomSpawnRules(InclusiveRange<Integer> blockLightLimit, InclusiveRange<Integer> skyLightLimit) {
        private static final InclusiveRange<Integer> LIGHT_RANGE = new InclusiveRange<>(0, 15);
        public static final Codec<SpawnData.CustomSpawnRules> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(InclusiveRange.INT.optionalFieldOf("block_light_limit", LIGHT_RANGE).flatXmap(SpawnData.CustomSpawnRules::checkLightBoundaries, SpawnData.CustomSpawnRules::checkLightBoundaries).forGetter((rules) -> {
                return rules.blockLightLimit;
            }), InclusiveRange.INT.optionalFieldOf("sky_light_limit", LIGHT_RANGE).flatXmap(SpawnData.CustomSpawnRules::checkLightBoundaries, SpawnData.CustomSpawnRules::checkLightBoundaries).forGetter((rules) -> {
                return rules.skyLightLimit;
            })).apply(instance, SpawnData.CustomSpawnRules::new);
        });

        private static DataResult<InclusiveRange<Integer>> checkLightBoundaries(InclusiveRange<Integer> provider) {
            return !LIGHT_RANGE.contains(provider) ? DataResult.error("Light values must be withing range " + LIGHT_RANGE) : DataResult.success(provider);
        }
    }
}
