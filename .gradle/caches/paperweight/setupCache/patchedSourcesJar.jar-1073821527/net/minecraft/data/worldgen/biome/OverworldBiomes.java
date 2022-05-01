package net.minecraft.data.worldgen.biome;

import javax.annotation.Nullable;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;

public class OverworldBiomes {
    protected static final int NORMAL_WATER_COLOR = 4159204;
    protected static final int NORMAL_WATER_FOG_COLOR = 329011;
    private static final int OVERWORLD_FOG_COLOR = 12638463;
    @Nullable
    private static final Music NORMAL_MUSIC = null;

    protected static int calculateSkyColor(float temperature) {
        float f = temperature / 3.0F;
        f = Mth.clamp(f, -1.0F, 1.0F);
        return Mth.hsvToRgb(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F);
    }

    private static Biome biome(Biome.Precipitation precipitation, Biome.BiomeCategory category, float temperature, float downfall, MobSpawnSettings.Builder spawnSettings, BiomeGenerationSettings.Builder generationSettings, @Nullable Music music) {
        return biome(precipitation, category, temperature, downfall, 4159204, 329011, spawnSettings, generationSettings, music);
    }

    private static Biome biome(Biome.Precipitation precipitation, Biome.BiomeCategory category, float temperature, float downfall, int waterColor, int waterFogColor, MobSpawnSettings.Builder spawnSettings, BiomeGenerationSettings.Builder generationSettings, @Nullable Music music) {
        return (new Biome.BiomeBuilder()).precipitation(precipitation).biomeCategory(category).temperature(temperature).downfall(downfall).specialEffects((new BiomeSpecialEffects.Builder()).waterColor(waterColor).waterFogColor(waterFogColor).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).backgroundMusic(music).build()).mobSpawnSettings(spawnSettings.build()).generationSettings(generationSettings.build()).build();
    }

    private static void globalOverworldGeneration(BiomeGenerationSettings.Builder generationSettings) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generationSettings);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generationSettings);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generationSettings);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generationSettings);
        BiomeDefaultFeatures.addDefaultSprings(generationSettings);
        BiomeDefaultFeatures.addSurfaceFreezing(generationSettings);
    }

    public static Biome oldGrowthTaiga(boolean spruce) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(builder);
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4));
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
        if (spruce) {
            BiomeDefaultFeatures.commonSpawns(builder);
        } else {
            BiomeDefaultFeatures.caveSpawns(builder);
            BiomeDefaultFeatures.monsters(builder, 100, 25, 100, false);
        }

        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addMossyStoneBlock(builder2);
        BiomeDefaultFeatures.addFerns(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, spruce ? VegetationPlacements.TREES_OLD_GROWTH_SPRUCE_TAIGA : VegetationPlacements.TREES_OLD_GROWTH_PINE_TAIGA);
        BiomeDefaultFeatures.addDefaultFlowers(builder2);
        BiomeDefaultFeatures.addGiantTaigaVegetation(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        BiomeDefaultFeatures.addCommonBerryBushes(builder2);
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.TAIGA, spruce ? 0.25F : 0.3F, 0.8F, builder, builder2, NORMAL_MUSIC);
    }

    public static Biome sparseJungle() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.baseJungleSpawns(builder);
        return baseJungle(0.8F, false, true, false, builder);
    }

    public static Biome jungle() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.baseJungleSpawns(builder);
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PARROT, 40, 1, 2)).addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.OCELOT, 2, 1, 3)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 1, 1, 2));
        return baseJungle(0.9F, false, false, true, builder);
    }

    public static Biome bambooJungle() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.baseJungleSpawns(builder);
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PARROT, 40, 1, 2)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 80, 1, 2)).addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.OCELOT, 2, 1, 1));
        return baseJungle(0.9F, true, false, true, builder);
    }

    private static Biome baseJungle(float depth, boolean bamboo, boolean sparse, boolean unmodified, MobSpawnSettings.Builder spawnSettings) {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        if (bamboo) {
            BiomeDefaultFeatures.addBambooVegetation(builder);
        } else {
            if (unmodified) {
                BiomeDefaultFeatures.addLightBambooVegetation(builder);
            }

            if (sparse) {
                BiomeDefaultFeatures.addSparseJungleTrees(builder);
            } else {
                BiomeDefaultFeatures.addJungleTrees(builder);
            }
        }

        BiomeDefaultFeatures.addWarmFlowers(builder);
        BiomeDefaultFeatures.addJungleGrass(builder);
        BiomeDefaultFeatures.addDefaultMushrooms(builder);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder);
        BiomeDefaultFeatures.addJungleVines(builder);
        if (sparse) {
            BiomeDefaultFeatures.addSparseJungleMelons(builder);
        } else {
            BiomeDefaultFeatures.addJungleMelons(builder);
        }

        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.JUNGLE, 0.95F, depth, spawnSettings, builder, NORMAL_MUSIC);
    }

    public static Biome windsweptHills(boolean forest) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(builder);
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 5, 4, 6));
        BiomeDefaultFeatures.commonSpawns(builder);
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        if (forest) {
            BiomeDefaultFeatures.addMountainForestTrees(builder2);
        } else {
            BiomeDefaultFeatures.addMountainTrees(builder2);
        }

        BiomeDefaultFeatures.addDefaultFlowers(builder2);
        BiomeDefaultFeatures.addDefaultGrass(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        BiomeDefaultFeatures.addExtraEmeralds(builder2);
        BiomeDefaultFeatures.addInfestedStone(builder2);
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.EXTREME_HILLS, 0.2F, 0.3F, builder, builder2, NORMAL_MUSIC);
    }

    public static Biome desert() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.desertSpawns(builder);
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        BiomeDefaultFeatures.addFossilDecoration(builder2);
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addDefaultFlowers(builder2);
        BiomeDefaultFeatures.addDefaultGrass(builder2);
        BiomeDefaultFeatures.addDesertVegetation(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addDesertExtraVegetation(builder2);
        BiomeDefaultFeatures.addDesertExtraDecoration(builder2);
        return biome(Biome.Precipitation.NONE, Biome.BiomeCategory.DESERT, 2.0F, 0.0F, builder, builder2, NORMAL_MUSIC);
    }

    public static Biome plains(boolean sunflower, boolean snowy, boolean iceSpikes) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        if (snowy) {
            builder.creatureGenerationProbability(0.07F);
            BiomeDefaultFeatures.snowySpawns(builder);
            if (iceSpikes) {
                builder2.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_SPIKE);
                builder2.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_PATCH);
            }
        } else {
            BiomeDefaultFeatures.plainsSpawns(builder);
            BiomeDefaultFeatures.addPlainGrass(builder2);
            if (sunflower) {
                builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUNFLOWER);
            }
        }

        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        if (snowy) {
            BiomeDefaultFeatures.addSnowyTrees(builder2);
            BiomeDefaultFeatures.addDefaultFlowers(builder2);
            BiomeDefaultFeatures.addDefaultGrass(builder2);
        } else {
            BiomeDefaultFeatures.addPlainVegetation(builder2);
        }

        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        if (sunflower) {
            builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUGAR_CANE);
            builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN);
        } else {
            BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        }

        float f = snowy ? 0.0F : 0.8F;
        return biome(snowy ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN, snowy ? Biome.BiomeCategory.ICY : Biome.BiomeCategory.PLAINS, f, snowy ? 0.5F : 0.4F, builder, builder2, NORMAL_MUSIC);
    }

    public static Biome mushroomFields() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.mooshroomSpawns(builder);
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addMushroomFieldVegetation(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.MUSHROOM, 0.9F, 1.0F, builder, builder2, NORMAL_MUSIC);
    }

    public static Biome savanna(boolean windswept, boolean plateau) {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder);
        if (!windswept) {
            BiomeDefaultFeatures.addSavannaGrass(builder);
        }

        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        if (windswept) {
            BiomeDefaultFeatures.addShatteredSavannaTrees(builder);
            BiomeDefaultFeatures.addDefaultFlowers(builder);
            BiomeDefaultFeatures.addShatteredSavannaGrass(builder);
        } else {
            BiomeDefaultFeatures.addSavannaTrees(builder);
            BiomeDefaultFeatures.addWarmFlowers(builder);
            BiomeDefaultFeatures.addSavannaExtraGrass(builder);
        }

        BiomeDefaultFeatures.addDefaultMushrooms(builder);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder);
        MobSpawnSettings.Builder builder2 = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(builder2);
        builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.HORSE, 1, 2, 6)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DONKEY, 1, 1, 1));
        BiomeDefaultFeatures.commonSpawns(builder2);
        if (plateau) {
            builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 8, 4, 4));
        }

        return biome(Biome.Precipitation.NONE, Biome.BiomeCategory.SAVANNA, 2.0F, 0.0F, builder2, builder, NORMAL_MUSIC);
    }

    public static Biome badlands(boolean plateau) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(builder);
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addExtraGold(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        if (plateau) {
            BiomeDefaultFeatures.addBadlandsTrees(builder2);
        }

        BiomeDefaultFeatures.addBadlandGrass(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addBadlandExtraVegetation(builder2);
        return (new Biome.BiomeBuilder()).precipitation(Biome.Precipitation.NONE).biomeCategory(Biome.BiomeCategory.MESA).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeSpecialEffects.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(2.0F)).foliageColorOverride(10387789).grassColorOverride(9470285).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    private static Biome baseOcean(MobSpawnSettings.Builder spawnSettings, int waterColor, int waterFogColor, BiomeGenerationSettings.Builder builder) {
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.OCEAN, 0.5F, 0.5F, waterColor, waterFogColor, spawnSettings, builder, NORMAL_MUSIC);
    }

    private static BiomeGenerationSettings.Builder baseOceanGeneration() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        BiomeDefaultFeatures.addWaterTrees(builder);
        BiomeDefaultFeatures.addDefaultFlowers(builder);
        BiomeDefaultFeatures.addDefaultGrass(builder);
        BiomeDefaultFeatures.addDefaultMushrooms(builder);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder);
        return builder;
    }

    public static Biome coldOcean(boolean deep) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.oceanSpawns(builder, 3, 4, 15);
        builder.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5));
        BiomeGenerationSettings.Builder builder2 = baseOceanGeneration();
        builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP_COLD : AquaticPlacements.SEAGRASS_COLD);
        BiomeDefaultFeatures.addDefaultSeagrass(builder2);
        BiomeDefaultFeatures.addColdOceanExtraVegetation(builder2);
        return baseOcean(builder, 4020182, 329011, builder2);
    }

    public static Biome ocean(boolean deep) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.oceanSpawns(builder, 1, 4, 10);
        builder.addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 1, 1, 2));
        BiomeGenerationSettings.Builder builder2 = baseOceanGeneration();
        builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP : AquaticPlacements.SEAGRASS_NORMAL);
        BiomeDefaultFeatures.addDefaultSeagrass(builder2);
        BiomeDefaultFeatures.addColdOceanExtraVegetation(builder2);
        return baseOcean(builder, 4159204, 329011, builder2);
    }

    public static Biome lukeWarmOcean(boolean deep) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        if (deep) {
            BiomeDefaultFeatures.oceanSpawns(builder, 8, 4, 8);
        } else {
            BiomeDefaultFeatures.oceanSpawns(builder, 10, 2, 15);
        }

        builder.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 5, 1, 3)).addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 8, 8)).addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 2, 1, 2));
        BiomeGenerationSettings.Builder builder2 = baseOceanGeneration();
        builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP_WARM : AquaticPlacements.SEAGRASS_WARM);
        if (deep) {
            BiomeDefaultFeatures.addDefaultSeagrass(builder2);
        }

        BiomeDefaultFeatures.addLukeWarmKelp(builder2);
        return baseOcean(builder, 4566514, 267827, builder2);
    }

    public static Biome warmOcean() {
        MobSpawnSettings.Builder builder = (new MobSpawnSettings.Builder()).addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 15, 1, 3));
        BiomeDefaultFeatures.warmOceanSpawns(builder, 10, 4);
        BiomeGenerationSettings.Builder builder2 = baseOceanGeneration().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.WARM_OCEAN_VEGETATION).addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_WARM).addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEA_PICKLE);
        return baseOcean(builder, 4445678, 270131, builder2);
    }

    public static Biome frozenOcean(boolean monument) {
        MobSpawnSettings.Builder builder = (new MobSpawnSettings.Builder()).addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 1, 1, 4)).addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.POLAR_BEAR, 1, 1, 2));
        BiomeDefaultFeatures.commonSpawns(builder);
        builder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 5, 1, 1));
        float f = monument ? 0.5F : 0.0F;
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        BiomeDefaultFeatures.addIcebergs(builder2);
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addBlueIce(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addWaterTrees(builder2);
        BiomeDefaultFeatures.addDefaultFlowers(builder2);
        BiomeDefaultFeatures.addDefaultGrass(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        return (new Biome.BiomeBuilder()).precipitation(monument ? Biome.Precipitation.RAIN : Biome.Precipitation.SNOW).biomeCategory(Biome.BiomeCategory.OCEAN).temperature(f).temperatureAdjustment(Biome.TemperatureModifier.FROZEN).downfall(0.5F).specialEffects((new BiomeSpecialEffects.Builder()).waterColor(3750089).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(f)).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static Biome forest(boolean birch, boolean oldGrowth, boolean flower) {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder);
        if (flower) {
            builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FOREST_FLOWERS);
        } else {
            BiomeDefaultFeatures.addForestFlowers(builder);
        }

        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        if (flower) {
            builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_FLOWER_FOREST);
            builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FLOWER_FOREST);
            BiomeDefaultFeatures.addDefaultGrass(builder);
        } else {
            if (birch) {
                if (oldGrowth) {
                    BiomeDefaultFeatures.addTallBirchTrees(builder);
                } else {
                    BiomeDefaultFeatures.addBirchTrees(builder);
                }
            } else {
                BiomeDefaultFeatures.addOtherBirchTrees(builder);
            }

            BiomeDefaultFeatures.addDefaultFlowers(builder);
            BiomeDefaultFeatures.addForestGrass(builder);
        }

        BiomeDefaultFeatures.addDefaultMushrooms(builder);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder);
        MobSpawnSettings.Builder builder2 = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(builder2);
        BiomeDefaultFeatures.commonSpawns(builder2);
        if (flower) {
            builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
        } else if (!birch) {
            builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 5, 4, 4));
        }

        float f = birch ? 0.6F : 0.7F;
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.FOREST, f, birch ? 0.6F : 0.8F, builder2, builder, NORMAL_MUSIC);
    }

    public static Biome taiga(boolean cold) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(builder);
        builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
        BiomeDefaultFeatures.commonSpawns(builder);
        float f = cold ? -0.5F : 0.25F;
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addFerns(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addTaigaTrees(builder2);
        BiomeDefaultFeatures.addDefaultFlowers(builder2);
        BiomeDefaultFeatures.addTaigaGrass(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        if (cold) {
            BiomeDefaultFeatures.addRareBerryBushes(builder2);
        } else {
            BiomeDefaultFeatures.addCommonBerryBushes(builder2);
        }

        return biome(cold ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN, Biome.BiomeCategory.TAIGA, f, cold ? 0.4F : 0.8F, cold ? 4020182 : 4159204, 329011, builder, builder2, NORMAL_MUSIC);
    }

    public static Biome darkForest() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(builder);
        BiomeDefaultFeatures.commonSpawns(builder);
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.DARK_FOREST_VEGETATION);
        BiomeDefaultFeatures.addForestFlowers(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addDefaultFlowers(builder2);
        BiomeDefaultFeatures.addForestGrass(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        return (new Biome.BiomeBuilder()).precipitation(Biome.Precipitation.RAIN).biomeCategory(Biome.BiomeCategory.FOREST).temperature(0.7F).downfall(0.8F).specialEffects((new BiomeSpecialEffects.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.7F)).grassColorModifier(BiomeSpecialEffects.GrassColorModifier.DARK_FOREST).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static Biome swamp() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(builder);
        BiomeDefaultFeatures.commonSpawns(builder);
        builder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 1, 1));
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        BiomeDefaultFeatures.addFossilDecoration(builder2);
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addSwampClayDisk(builder2);
        BiomeDefaultFeatures.addSwampVegetation(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addSwampExtraVegetation(builder2);
        builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);
        return (new Biome.BiomeBuilder()).precipitation(Biome.Precipitation.RAIN).biomeCategory(Biome.BiomeCategory.SWAMP).temperature(0.8F).downfall(0.9F).specialEffects((new BiomeSpecialEffects.Builder()).waterColor(6388580).waterFogColor(2302743).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).foliageColorOverride(6975545).grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static Biome river(boolean frozen) {
        MobSpawnSettings.Builder builder = (new MobSpawnSettings.Builder()).addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 2, 1, 4)).addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 5, 1, 5));
        BiomeDefaultFeatures.commonSpawns(builder);
        builder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, frozen ? 1 : 100, 1, 1));
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addWaterTrees(builder2);
        BiomeDefaultFeatures.addDefaultFlowers(builder2);
        BiomeDefaultFeatures.addDefaultGrass(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        if (!frozen) {
            builder2.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_RIVER);
        }

        float f = frozen ? 0.0F : 0.5F;
        return biome(frozen ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN, Biome.BiomeCategory.RIVER, f, 0.5F, frozen ? 3750089 : 4159204, 329011, builder, builder2, NORMAL_MUSIC);
    }

    public static Biome beach(boolean snowy, boolean stony) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        boolean bl = !stony && !snowy;
        if (bl) {
            builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.TURTLE, 5, 2, 5));
        }

        BiomeDefaultFeatures.commonSpawns(builder);
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addDefaultFlowers(builder2);
        BiomeDefaultFeatures.addDefaultGrass(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        float f;
        if (snowy) {
            f = 0.05F;
        } else if (stony) {
            f = 0.2F;
        } else {
            f = 0.8F;
        }

        return biome(snowy ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN, Biome.BiomeCategory.BEACH, f, bl ? 0.4F : 0.3F, snowy ? 4020182 : 4159204, 329011, builder, builder2, NORMAL_MUSIC);
    }

    public static Biome theVoid() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        builder.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, MiscOverworldPlacements.VOID_START_PLATFORM);
        return biome(Biome.Precipitation.NONE, Biome.BiomeCategory.NONE, 0.5F, 0.5F, new MobSpawnSettings.Builder(), builder, NORMAL_MUSIC);
    }

    public static Biome meadow() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        MobSpawnSettings.Builder builder2 = new MobSpawnSettings.Builder();
        builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DONKEY, 1, 1, 2)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 2, 6)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 2, 2, 4));
        BiomeDefaultFeatures.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        BiomeDefaultFeatures.addPlainGrass(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        BiomeDefaultFeatures.addMeadowVegetation(builder);
        BiomeDefaultFeatures.addExtraEmeralds(builder);
        BiomeDefaultFeatures.addInfestedStone(builder);
        Music music = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_MEADOW);
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.MOUNTAIN, 0.5F, 0.8F, 937679, 329011, builder2, builder, music);
    }

    public static Biome frozenPeaks() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        MobSpawnSettings.Builder builder2 = new MobSpawnSettings.Builder();
        builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
        BiomeDefaultFeatures.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        BiomeDefaultFeatures.addFrozenSprings(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        BiomeDefaultFeatures.addExtraEmeralds(builder);
        BiomeDefaultFeatures.addInfestedStone(builder);
        Music music = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FROZEN_PEAKS);
        return biome(Biome.Precipitation.SNOW, Biome.BiomeCategory.MOUNTAIN, -0.7F, 0.9F, builder2, builder, music);
    }

    public static Biome jaggedPeaks() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        MobSpawnSettings.Builder builder2 = new MobSpawnSettings.Builder();
        builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
        BiomeDefaultFeatures.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        BiomeDefaultFeatures.addFrozenSprings(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        BiomeDefaultFeatures.addExtraEmeralds(builder);
        BiomeDefaultFeatures.addInfestedStone(builder);
        Music music = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_JAGGED_PEAKS);
        return biome(Biome.Precipitation.SNOW, Biome.BiomeCategory.MOUNTAIN, -0.7F, 0.9F, builder2, builder, music);
    }

    public static Biome stonyPeaks() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        MobSpawnSettings.Builder builder2 = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        BiomeDefaultFeatures.addExtraEmeralds(builder);
        BiomeDefaultFeatures.addInfestedStone(builder);
        Music music = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_STONY_PEAKS);
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.MOUNTAIN, 1.0F, 0.3F, builder2, builder, music);
    }

    public static Biome snowySlopes() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        MobSpawnSettings.Builder builder2 = new MobSpawnSettings.Builder();
        builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
        BiomeDefaultFeatures.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        BiomeDefaultFeatures.addFrozenSprings(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder);
        BiomeDefaultFeatures.addExtraEmeralds(builder);
        BiomeDefaultFeatures.addInfestedStone(builder);
        Music music = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SNOWY_SLOPES);
        return biome(Biome.Precipitation.SNOW, Biome.BiomeCategory.MOUNTAIN, -0.3F, 0.9F, builder2, builder, music);
    }

    public static Biome grove() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        MobSpawnSettings.Builder builder2 = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(builder2);
        builder2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3)).addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
        BiomeDefaultFeatures.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        BiomeDefaultFeatures.addFrozenSprings(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);
        BiomeDefaultFeatures.addGroveTrees(builder);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder);
        BiomeDefaultFeatures.addExtraEmeralds(builder);
        BiomeDefaultFeatures.addInfestedStone(builder);
        Music music = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_GROVE);
        return biome(Biome.Precipitation.SNOW, Biome.BiomeCategory.FOREST, -0.2F, 0.8F, builder2, builder, music);
    }

    public static Biome lushCaves() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        builder.addSpawn(MobCategory.AXOLOTLS, new MobSpawnSettings.SpawnerData(EntityType.AXOLOTL, 10, 4, 6));
        builder.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 8, 8));
        BiomeDefaultFeatures.commonSpawns(builder);
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addPlainGrass(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2);
        BiomeDefaultFeatures.addLushCavesSpecialOres(builder2);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addLushCavesVegetationFeatures(builder2);
        Music music = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_LUSH_CAVES);
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.UNDERGROUND, 0.5F, 0.5F, builder, builder2, music);
    }

    public static Biome dripstoneCaves() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.dripstoneCavesSpawns(builder);
        BiomeGenerationSettings.Builder builder2 = new BiomeGenerationSettings.Builder();
        globalOverworldGeneration(builder2);
        BiomeDefaultFeatures.addPlainGrass(builder2);
        BiomeDefaultFeatures.addDefaultOres(builder2, true);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder2);
        BiomeDefaultFeatures.addPlainVegetation(builder2);
        BiomeDefaultFeatures.addDefaultMushrooms(builder2);
        BiomeDefaultFeatures.addDefaultExtraVegetation(builder2);
        BiomeDefaultFeatures.addDripstone(builder2);
        Music music = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_DRIPSTONE_CAVES);
        return biome(Biome.Precipitation.RAIN, Biome.BiomeCategory.UNDERGROUND, 0.8F, 0.4F, builder, builder2, music);
    }
}
