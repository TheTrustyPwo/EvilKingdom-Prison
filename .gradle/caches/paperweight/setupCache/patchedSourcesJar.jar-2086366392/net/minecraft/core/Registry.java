package net.minecraft.core;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.decoration.Motive;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.score.LootScoreProviderType;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public abstract class Registry<T> implements Keyable, IdMap<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, Supplier<?>> LOADERS = Maps.newLinkedHashMap();
    public static final ResourceLocation ROOT_REGISTRY_NAME = new ResourceLocation("root");
    protected static final WritableRegistry<WritableRegistry<?>> WRITABLE_REGISTRY = new MappedRegistry<>(createRegistryKey("root"), Lifecycle.experimental(), (Function<WritableRegistry<?>, Holder.Reference<WritableRegistry<?>>>)null);
    public static final Registry<? extends Registry<?>> REGISTRY = WRITABLE_REGISTRY;
    public static final ResourceKey<Registry<SoundEvent>> SOUND_EVENT_REGISTRY = createRegistryKey("sound_event");
    public static final ResourceKey<Registry<Fluid>> FLUID_REGISTRY = createRegistryKey("fluid");
    public static final ResourceKey<Registry<MobEffect>> MOB_EFFECT_REGISTRY = createRegistryKey("mob_effect");
    public static final ResourceKey<Registry<Block>> BLOCK_REGISTRY = createRegistryKey("block");
    public static final ResourceKey<Registry<Enchantment>> ENCHANTMENT_REGISTRY = createRegistryKey("enchantment");
    public static final ResourceKey<Registry<EntityType<?>>> ENTITY_TYPE_REGISTRY = createRegistryKey("entity_type");
    public static final ResourceKey<Registry<Item>> ITEM_REGISTRY = createRegistryKey("item");
    public static final ResourceKey<Registry<Potion>> POTION_REGISTRY = createRegistryKey("potion");
    public static final ResourceKey<Registry<ParticleType<?>>> PARTICLE_TYPE_REGISTRY = createRegistryKey("particle_type");
    public static final ResourceKey<Registry<BlockEntityType<?>>> BLOCK_ENTITY_TYPE_REGISTRY = createRegistryKey("block_entity_type");
    public static final ResourceKey<Registry<Motive>> MOTIVE_REGISTRY = createRegistryKey("motive");
    public static final ResourceKey<Registry<ResourceLocation>> CUSTOM_STAT_REGISTRY = createRegistryKey("custom_stat");
    public static final ResourceKey<Registry<ChunkStatus>> CHUNK_STATUS_REGISTRY = createRegistryKey("chunk_status");
    public static final ResourceKey<Registry<RuleTestType<?>>> RULE_TEST_REGISTRY = createRegistryKey("rule_test");
    public static final ResourceKey<Registry<PosRuleTestType<?>>> POS_RULE_TEST_REGISTRY = createRegistryKey("pos_rule_test");
    public static final ResourceKey<Registry<MenuType<?>>> MENU_REGISTRY = createRegistryKey("menu");
    public static final ResourceKey<Registry<RecipeType<?>>> RECIPE_TYPE_REGISTRY = createRegistryKey("recipe_type");
    public static final ResourceKey<Registry<RecipeSerializer<?>>> RECIPE_SERIALIZER_REGISTRY = createRegistryKey("recipe_serializer");
    public static final ResourceKey<Registry<Attribute>> ATTRIBUTE_REGISTRY = createRegistryKey("attribute");
    public static final ResourceKey<Registry<GameEvent>> GAME_EVENT_REGISTRY = createRegistryKey("game_event");
    public static final ResourceKey<Registry<PositionSourceType<?>>> POSITION_SOURCE_TYPE_REGISTRY = createRegistryKey("position_source_type");
    public static final ResourceKey<Registry<StatType<?>>> STAT_TYPE_REGISTRY = createRegistryKey("stat_type");
    public static final ResourceKey<Registry<VillagerType>> VILLAGER_TYPE_REGISTRY = createRegistryKey("villager_type");
    public static final ResourceKey<Registry<VillagerProfession>> VILLAGER_PROFESSION_REGISTRY = createRegistryKey("villager_profession");
    public static final ResourceKey<Registry<PoiType>> POINT_OF_INTEREST_TYPE_REGISTRY = createRegistryKey("point_of_interest_type");
    public static final ResourceKey<Registry<MemoryModuleType<?>>> MEMORY_MODULE_TYPE_REGISTRY = createRegistryKey("memory_module_type");
    public static final ResourceKey<Registry<SensorType<?>>> SENSOR_TYPE_REGISTRY = createRegistryKey("sensor_type");
    public static final ResourceKey<Registry<Schedule>> SCHEDULE_REGISTRY = createRegistryKey("schedule");
    public static final ResourceKey<Registry<Activity>> ACTIVITY_REGISTRY = createRegistryKey("activity");
    public static final ResourceKey<Registry<LootPoolEntryType>> LOOT_ENTRY_REGISTRY = createRegistryKey("loot_pool_entry_type");
    public static final ResourceKey<Registry<LootItemFunctionType>> LOOT_FUNCTION_REGISTRY = createRegistryKey("loot_function_type");
    public static final ResourceKey<Registry<LootItemConditionType>> LOOT_ITEM_REGISTRY = createRegistryKey("loot_condition_type");
    public static final ResourceKey<Registry<LootNumberProviderType>> LOOT_NUMBER_PROVIDER_REGISTRY = createRegistryKey("loot_number_provider_type");
    public static final ResourceKey<Registry<LootNbtProviderType>> LOOT_NBT_PROVIDER_REGISTRY = createRegistryKey("loot_nbt_provider_type");
    public static final ResourceKey<Registry<LootScoreProviderType>> LOOT_SCORE_PROVIDER_REGISTRY = createRegistryKey("loot_score_provider_type");
    public static final ResourceKey<Registry<DimensionType>> DIMENSION_TYPE_REGISTRY = createRegistryKey("dimension_type");
    public static final ResourceKey<Registry<Level>> DIMENSION_REGISTRY = createRegistryKey("dimension");
    public static final ResourceKey<Registry<LevelStem>> LEVEL_STEM_REGISTRY = createRegistryKey("dimension");
    public static final DefaultedRegistry<GameEvent> GAME_EVENT = registerDefaulted(GAME_EVENT_REGISTRY, "step", GameEvent::builtInRegistryHolder, (registry) -> {
        return GameEvent.STEP;
    });
    public static final Registry<SoundEvent> SOUND_EVENT = registerSimple(SOUND_EVENT_REGISTRY, (registry) -> {
        return SoundEvents.ITEM_PICKUP;
    });
    public static final DefaultedRegistry<Fluid> FLUID = registerDefaulted(FLUID_REGISTRY, "empty", Fluid::builtInRegistryHolder, (registry) -> {
        return Fluids.EMPTY;
    });
    public static final Registry<MobEffect> MOB_EFFECT = registerSimple(MOB_EFFECT_REGISTRY, (registry) -> {
        return MobEffects.LUCK;
    });
    public static final DefaultedRegistry<Block> BLOCK = registerDefaulted(BLOCK_REGISTRY, "air", Block::builtInRegistryHolder, (registry) -> {
        return Blocks.AIR;
    });
    public static final Registry<Enchantment> ENCHANTMENT = registerSimple(ENCHANTMENT_REGISTRY, (registry) -> {
        return Enchantments.BLOCK_FORTUNE;
    });
    public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = registerDefaulted(ENTITY_TYPE_REGISTRY, "pig", EntityType::builtInRegistryHolder, (registry) -> {
        return EntityType.PIG;
    });
    public static final DefaultedRegistry<Item> ITEM = registerDefaulted(ITEM_REGISTRY, "air", Item::builtInRegistryHolder, (registry) -> {
        return Items.AIR;
    });
    public static final DefaultedRegistry<Potion> POTION = registerDefaulted(POTION_REGISTRY, "empty", (registry) -> {
        return Potions.EMPTY;
    });
    public static final Registry<ParticleType<?>> PARTICLE_TYPE = registerSimple(PARTICLE_TYPE_REGISTRY, (registry) -> {
        return ParticleTypes.BLOCK;
    });
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE = registerSimple(BLOCK_ENTITY_TYPE_REGISTRY, (registry) -> {
        return BlockEntityType.FURNACE;
    });
    public static final DefaultedRegistry<Motive> MOTIVE = registerDefaulted(MOTIVE_REGISTRY, "kebab", (registry) -> {
        return Motive.KEBAB;
    });
    public static final Registry<ResourceLocation> CUSTOM_STAT = registerSimple(CUSTOM_STAT_REGISTRY, (registry) -> {
        return Stats.JUMP;
    });
    public static final DefaultedRegistry<ChunkStatus> CHUNK_STATUS = registerDefaulted(CHUNK_STATUS_REGISTRY, "empty", (registry) -> {
        return ChunkStatus.EMPTY;
    });
    public static final Registry<RuleTestType<?>> RULE_TEST = registerSimple(RULE_TEST_REGISTRY, (registry) -> {
        return RuleTestType.ALWAYS_TRUE_TEST;
    });
    public static final Registry<PosRuleTestType<?>> POS_RULE_TEST = registerSimple(POS_RULE_TEST_REGISTRY, (registry) -> {
        return PosRuleTestType.ALWAYS_TRUE_TEST;
    });
    public static final Registry<MenuType<?>> MENU = registerSimple(MENU_REGISTRY, (registry) -> {
        return MenuType.ANVIL;
    });
    public static final Registry<RecipeType<?>> RECIPE_TYPE = registerSimple(RECIPE_TYPE_REGISTRY, (registry) -> {
        return RecipeType.CRAFTING;
    });
    public static final Registry<RecipeSerializer<?>> RECIPE_SERIALIZER = registerSimple(RECIPE_SERIALIZER_REGISTRY, (registry) -> {
        return RecipeSerializer.SHAPELESS_RECIPE;
    });
    public static final Registry<Attribute> ATTRIBUTE = registerSimple(ATTRIBUTE_REGISTRY, (registry) -> {
        return Attributes.LUCK;
    });
    public static final Registry<PositionSourceType<?>> POSITION_SOURCE_TYPE = registerSimple(POSITION_SOURCE_TYPE_REGISTRY, (registry) -> {
        return PositionSourceType.BLOCK;
    });
    public static final Registry<StatType<?>> STAT_TYPE = registerSimple(STAT_TYPE_REGISTRY, (registry) -> {
        return Stats.ITEM_USED;
    });
    public static final DefaultedRegistry<VillagerType> VILLAGER_TYPE = registerDefaulted(VILLAGER_TYPE_REGISTRY, "plains", (registry) -> {
        return VillagerType.PLAINS;
    });
    public static final DefaultedRegistry<VillagerProfession> VILLAGER_PROFESSION = registerDefaulted(VILLAGER_PROFESSION_REGISTRY, "none", (registry) -> {
        return VillagerProfession.NONE;
    });
    public static final DefaultedRegistry<PoiType> POINT_OF_INTEREST_TYPE = registerDefaulted(POINT_OF_INTEREST_TYPE_REGISTRY, "unemployed", (registry) -> {
        return PoiType.UNEMPLOYED;
    });
    public static final DefaultedRegistry<MemoryModuleType<?>> MEMORY_MODULE_TYPE = registerDefaulted(MEMORY_MODULE_TYPE_REGISTRY, "dummy", (registry) -> {
        return MemoryModuleType.DUMMY;
    });
    public static final DefaultedRegistry<SensorType<?>> SENSOR_TYPE = registerDefaulted(SENSOR_TYPE_REGISTRY, "dummy", (registry) -> {
        return SensorType.DUMMY;
    });
    public static final Registry<Schedule> SCHEDULE = registerSimple(SCHEDULE_REGISTRY, (registry) -> {
        return Schedule.EMPTY;
    });
    public static final Registry<Activity> ACTIVITY = registerSimple(ACTIVITY_REGISTRY, (registry) -> {
        return Activity.IDLE;
    });
    public static final Registry<LootPoolEntryType> LOOT_POOL_ENTRY_TYPE = registerSimple(LOOT_ENTRY_REGISTRY, (registry) -> {
        return LootPoolEntries.EMPTY;
    });
    public static final Registry<LootItemFunctionType> LOOT_FUNCTION_TYPE = registerSimple(LOOT_FUNCTION_REGISTRY, (registry) -> {
        return LootItemFunctions.SET_COUNT;
    });
    public static final Registry<LootItemConditionType> LOOT_CONDITION_TYPE = registerSimple(LOOT_ITEM_REGISTRY, (registry) -> {
        return LootItemConditions.INVERTED;
    });
    public static final Registry<LootNumberProviderType> LOOT_NUMBER_PROVIDER_TYPE = registerSimple(LOOT_NUMBER_PROVIDER_REGISTRY, (registry) -> {
        return NumberProviders.CONSTANT;
    });
    public static final Registry<LootNbtProviderType> LOOT_NBT_PROVIDER_TYPE = registerSimple(LOOT_NBT_PROVIDER_REGISTRY, (registry) -> {
        return NbtProviders.CONTEXT;
    });
    public static final Registry<LootScoreProviderType> LOOT_SCORE_PROVIDER_TYPE = registerSimple(LOOT_SCORE_PROVIDER_REGISTRY, (registry) -> {
        return ScoreboardNameProviders.CONTEXT;
    });
    public static final ResourceKey<Registry<FloatProviderType<?>>> FLOAT_PROVIDER_TYPE_REGISTRY = createRegistryKey("float_provider_type");
    public static final Registry<FloatProviderType<?>> FLOAT_PROVIDER_TYPES = registerSimple(FLOAT_PROVIDER_TYPE_REGISTRY, (registry) -> {
        return FloatProviderType.CONSTANT;
    });
    public static final ResourceKey<Registry<IntProviderType<?>>> INT_PROVIDER_TYPE_REGISTRY = createRegistryKey("int_provider_type");
    public static final Registry<IntProviderType<?>> INT_PROVIDER_TYPES = registerSimple(INT_PROVIDER_TYPE_REGISTRY, (tegistry) -> {
        return IntProviderType.CONSTANT;
    });
    public static final ResourceKey<Registry<HeightProviderType<?>>> HEIGHT_PROVIDER_TYPE_REGISTRY = createRegistryKey("height_provider_type");
    public static final Registry<HeightProviderType<?>> HEIGHT_PROVIDER_TYPES = registerSimple(HEIGHT_PROVIDER_TYPE_REGISTRY, (registry) -> {
        return HeightProviderType.CONSTANT;
    });
    public static final ResourceKey<Registry<BlockPredicateType<?>>> BLOCK_PREDICATE_TYPE_REGISTRY = createRegistryKey("block_predicate_type");
    public static final Registry<BlockPredicateType<?>> BLOCK_PREDICATE_TYPES = registerSimple(BLOCK_PREDICATE_TYPE_REGISTRY, (registry) -> {
        return BlockPredicateType.NOT;
    });
    public static final ResourceKey<Registry<NoiseGeneratorSettings>> NOISE_GENERATOR_SETTINGS_REGISTRY = createRegistryKey("worldgen/noise_settings");
    public static final ResourceKey<Registry<ConfiguredWorldCarver<?>>> CONFIGURED_CARVER_REGISTRY = createRegistryKey("worldgen/configured_carver");
    public static final ResourceKey<Registry<ConfiguredFeature<?, ?>>> CONFIGURED_FEATURE_REGISTRY = createRegistryKey("worldgen/configured_feature");
    public static final ResourceKey<Registry<PlacedFeature>> PLACED_FEATURE_REGISTRY = createRegistryKey("worldgen/placed_feature");
    public static final ResourceKey<Registry<ConfiguredStructureFeature<?, ?>>> CONFIGURED_STRUCTURE_FEATURE_REGISTRY = createRegistryKey("worldgen/configured_structure_feature");
    public static final ResourceKey<Registry<StructureSet>> STRUCTURE_SET_REGISTRY = createRegistryKey("worldgen/structure_set");
    public static final ResourceKey<Registry<StructureProcessorList>> PROCESSOR_LIST_REGISTRY = createRegistryKey("worldgen/processor_list");
    public static final ResourceKey<Registry<StructureTemplatePool>> TEMPLATE_POOL_REGISTRY = createRegistryKey("worldgen/template_pool");
    public static final ResourceKey<Registry<Biome>> BIOME_REGISTRY = createRegistryKey("worldgen/biome");
    public static final ResourceKey<Registry<NormalNoise.NoiseParameters>> NOISE_REGISTRY = createRegistryKey("worldgen/noise");
    public static final ResourceKey<Registry<DensityFunction>> DENSITY_FUNCTION_REGISTRY = createRegistryKey("worldgen/density_function");
    public static final ResourceKey<Registry<WorldCarver<?>>> CARVER_REGISTRY = createRegistryKey("worldgen/carver");
    public static final Registry<WorldCarver<?>> CARVER = registerSimple(CARVER_REGISTRY, (registry) -> {
        return WorldCarver.CAVE;
    });
    public static final ResourceKey<Registry<Feature<?>>> FEATURE_REGISTRY = createRegistryKey("worldgen/feature");
    public static final Registry<Feature<?>> FEATURE = registerSimple(FEATURE_REGISTRY, (registry) -> {
        return Feature.ORE;
    });
    public static final ResourceKey<Registry<StructureFeature<?>>> STRUCTURE_FEATURE_REGISTRY = createRegistryKey("worldgen/structure_feature");
    public static final Registry<StructureFeature<?>> STRUCTURE_FEATURE = registerSimple(STRUCTURE_FEATURE_REGISTRY, (registry) -> {
        return StructureFeature.MINESHAFT;
    });
    public static final ResourceKey<Registry<StructurePlacementType<?>>> STRUCTURE_PLACEMENT_TYPE_REGISTRY = createRegistryKey("worldgen/structure_placement");
    public static final Registry<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPE = registerSimple(STRUCTURE_PLACEMENT_TYPE_REGISTRY, (registry) -> {
        return StructurePlacementType.RANDOM_SPREAD;
    });
    public static final ResourceKey<Registry<StructurePieceType>> STRUCTURE_PIECE_REGISTRY = createRegistryKey("worldgen/structure_piece");
    public static final Registry<StructurePieceType> STRUCTURE_PIECE = registerSimple(STRUCTURE_PIECE_REGISTRY, (registry) -> {
        return StructurePieceType.MINE_SHAFT_ROOM;
    });
    public static final ResourceKey<Registry<PlacementModifierType<?>>> PLACEMENT_MODIFIER_REGISTRY = createRegistryKey("worldgen/placement_modifier_type");
    public static final Registry<PlacementModifierType<?>> PLACEMENT_MODIFIERS = registerSimple(PLACEMENT_MODIFIER_REGISTRY, (registry) -> {
        return PlacementModifierType.COUNT;
    });
    public static final ResourceKey<Registry<BlockStateProviderType<?>>> BLOCK_STATE_PROVIDER_TYPE_REGISTRY = createRegistryKey("worldgen/block_state_provider_type");
    public static final ResourceKey<Registry<FoliagePlacerType<?>>> FOLIAGE_PLACER_TYPE_REGISTRY = createRegistryKey("worldgen/foliage_placer_type");
    public static final ResourceKey<Registry<TrunkPlacerType<?>>> TRUNK_PLACER_TYPE_REGISTRY = createRegistryKey("worldgen/trunk_placer_type");
    public static final ResourceKey<Registry<TreeDecoratorType<?>>> TREE_DECORATOR_TYPE_REGISTRY = createRegistryKey("worldgen/tree_decorator_type");
    public static final ResourceKey<Registry<FeatureSizeType<?>>> FEATURE_SIZE_TYPE_REGISTRY = createRegistryKey("worldgen/feature_size_type");
    public static final ResourceKey<Registry<Codec<? extends BiomeSource>>> BIOME_SOURCE_REGISTRY = createRegistryKey("worldgen/biome_source");
    public static final ResourceKey<Registry<Codec<? extends ChunkGenerator>>> CHUNK_GENERATOR_REGISTRY = createRegistryKey("worldgen/chunk_generator");
    public static final ResourceKey<Registry<Codec<? extends SurfaceRules.ConditionSource>>> CONDITION_REGISTRY = createRegistryKey("worldgen/material_condition");
    public static final ResourceKey<Registry<Codec<? extends SurfaceRules.RuleSource>>> RULE_REGISTRY = createRegistryKey("worldgen/material_rule");
    public static final ResourceKey<Registry<Codec<? extends DensityFunction>>> DENSITY_FUNCTION_TYPE_REGISTRY = createRegistryKey("worldgen/density_function_type");
    public static final ResourceKey<Registry<StructureProcessorType<?>>> STRUCTURE_PROCESSOR_REGISTRY = createRegistryKey("worldgen/structure_processor");
    public static final ResourceKey<Registry<StructurePoolElementType<?>>> STRUCTURE_POOL_ELEMENT_REGISTRY = createRegistryKey("worldgen/structure_pool_element");
    public static final Registry<BlockStateProviderType<?>> BLOCKSTATE_PROVIDER_TYPES = registerSimple(BLOCK_STATE_PROVIDER_TYPE_REGISTRY, (registry) -> {
        return BlockStateProviderType.SIMPLE_STATE_PROVIDER;
    });
    public static final Registry<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPES = registerSimple(FOLIAGE_PLACER_TYPE_REGISTRY, (registry) -> {
        return FoliagePlacerType.BLOB_FOLIAGE_PLACER;
    });
    public static final Registry<TrunkPlacerType<?>> TRUNK_PLACER_TYPES = registerSimple(TRUNK_PLACER_TYPE_REGISTRY, (registry) -> {
        return TrunkPlacerType.STRAIGHT_TRUNK_PLACER;
    });
    public static final Registry<TreeDecoratorType<?>> TREE_DECORATOR_TYPES = registerSimple(TREE_DECORATOR_TYPE_REGISTRY, (registry) -> {
        return TreeDecoratorType.LEAVE_VINE;
    });
    public static final Registry<FeatureSizeType<?>> FEATURE_SIZE_TYPES = registerSimple(FEATURE_SIZE_TYPE_REGISTRY, (registry) -> {
        return FeatureSizeType.TWO_LAYERS_FEATURE_SIZE;
    });
    public static final Registry<Codec<? extends BiomeSource>> BIOME_SOURCE = registerSimple(BIOME_SOURCE_REGISTRY, Lifecycle.stable(), (registry) -> {
        return BiomeSource.CODEC;
    });
    public static final Registry<Codec<? extends ChunkGenerator>> CHUNK_GENERATOR = registerSimple(CHUNK_GENERATOR_REGISTRY, Lifecycle.stable(), (registry) -> {
        return ChunkGenerator.CODEC;
    });
    public static final Registry<Codec<? extends SurfaceRules.ConditionSource>> CONDITION = registerSimple(CONDITION_REGISTRY, SurfaceRules.ConditionSource::bootstrap);
    public static final Registry<Codec<? extends SurfaceRules.RuleSource>> RULE = registerSimple(RULE_REGISTRY, SurfaceRules.RuleSource::bootstrap);
    public static final Registry<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES = registerSimple(DENSITY_FUNCTION_TYPE_REGISTRY, DensityFunctions::bootstrap);
    public static final Registry<StructureProcessorType<?>> STRUCTURE_PROCESSOR = registerSimple(STRUCTURE_PROCESSOR_REGISTRY, (registry) -> {
        return StructureProcessorType.BLOCK_IGNORE;
    });
    public static final Registry<StructurePoolElementType<?>> STRUCTURE_POOL_ELEMENT = registerSimple(STRUCTURE_POOL_ELEMENT_REGISTRY, (registry) -> {
        return StructurePoolElementType.EMPTY;
    });
    private final ResourceKey<? extends Registry<T>> key;
    private final Lifecycle lifecycle;

    private static <T> ResourceKey<Registry<T>> createRegistryKey(String registryId) {
        return ResourceKey.createRegistryKey(new ResourceLocation(registryId));
    }

    public static <T extends Registry<?>> void checkRegistry(Registry<T> registries) {
        registries.forEach((registry) -> {
            if (registry.keySet().isEmpty()) {
                Util.logAndPauseIfInIde("Registry '" + registries.getKey(registry) + "' was empty after loading");
            }

            if (registry instanceof DefaultedRegistry) {
                ResourceLocation resourceLocation = ((DefaultedRegistry)registry).getDefaultKey();
                Validate.notNull(registry.get(resourceLocation), "Missing default of DefaultedMappedRegistry: " + resourceLocation);
            }

        });
    }

    private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> key, Registry.RegistryBootstrap<T> defaultEntryGetter) {
        return registerSimple(key, Lifecycle.experimental(), defaultEntryGetter);
    }

    private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> key, String defaultId, Registry.RegistryBootstrap<T> defaultEntryGetter) {
        return registerDefaulted(key, defaultId, Lifecycle.experimental(), defaultEntryGetter);
    }

    private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> key, String defaultId, Function<T, Holder.Reference<T>> valueToEntryFunction, Registry.RegistryBootstrap<T> defaultEntryGetter) {
        return registerDefaulted(key, defaultId, Lifecycle.experimental(), valueToEntryFunction, defaultEntryGetter);
    }

    private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, Registry.RegistryBootstrap<T> defaultEntryGetter) {
        return internalRegister(key, new MappedRegistry<>(key, lifecycle, (Function<T, Holder.Reference<T>>)null), defaultEntryGetter, lifecycle);
    }

    private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, Function<T, Holder.Reference<T>> valueToEntryFunction, Registry.RegistryBootstrap<T> defaultEntryGetter) {
        return internalRegister(key, new MappedRegistry<>(key, lifecycle, valueToEntryFunction), defaultEntryGetter, lifecycle);
    }

    private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> key, String defaultId, Lifecycle lifecycle, Registry.RegistryBootstrap<T> defaultEntryGetter) {
        return internalRegister(key, new DefaultedRegistry<>(defaultId, key, lifecycle, (Function<T, Holder.Reference<T>>)null), defaultEntryGetter, lifecycle);
    }

    private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> key, String defaultId, Lifecycle lifecycle, Function<T, Holder.Reference<T>> valueToEntryFunction, Registry.RegistryBootstrap<T> defaultEntryGetter) {
        return internalRegister(key, new DefaultedRegistry<>(defaultId, key, lifecycle, valueToEntryFunction), defaultEntryGetter, lifecycle);
    }

    private static <T, R extends WritableRegistry<T>> R internalRegister(ResourceKey<? extends Registry<T>> key, R registry, Registry.RegistryBootstrap<T> defaultEntryGetter, Lifecycle lifecycle) {
        ResourceLocation resourceLocation = key.location();
        LOADERS.put(resourceLocation, () -> {
            return defaultEntryGetter.run(registry);
        });
        WRITABLE_REGISTRY.register(key, registry, lifecycle);
        return registry;
    }

    protected Registry(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle) {
        Bootstrap.checkBootstrapCalled(() -> {
            return "registry " + key;
        });
        this.key = key;
        this.lifecycle = lifecycle;
    }

    public static void freezeBuiltins() {
        for(Registry<?> registry : REGISTRY) {
            registry.freeze();
        }

    }

    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    public Lifecycle lifecycle() {
        return this.lifecycle;
    }

    @Override
    public String toString() {
        return "Registry[" + this.key + " (" + this.lifecycle + ")]";
    }

    public Codec<T> byNameCodec() {
        Codec<T> codec = ResourceLocation.CODEC.flatXmap((id) -> {
            return Optional.ofNullable(this.get(id)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown registry key in " + this.key + ": " + id);
            });
        }, (value) -> {
            return this.getResourceKey(value).map(ResourceKey::location).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown registry element in " + this.key + ":" + value);
            });
        });
        Codec<T> codec2 = ExtraCodecs.idResolverCodec((value) -> {
            return this.getResourceKey(value).isPresent() ? this.getId(value) : -1;
        }, this::byId, -1);
        return ExtraCodecs.overrideLifecycle(ExtraCodecs.orCompressed(codec, codec2), this::lifecycle, (value) -> {
            return this.lifecycle;
        });
    }

    public Codec<Holder<T>> holderByNameCodec() {
        Codec<Holder<T>> codec = ResourceLocation.CODEC.flatXmap((id) -> {
            return this.getHolder(ResourceKey.create(this.key, id)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown registry key in " + this.key + ": " + id);
            });
        }, (entry) -> {
            return entry.unwrapKey().map(ResourceKey::location).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown registry element in " + this.key + ":" + entry);
            });
        });
        return ExtraCodecs.overrideLifecycle(codec, (entry) -> {
            return this.lifecycle(entry.value());
        }, (entry) -> {
            return this.lifecycle;
        });
    }

    public <U> Stream<U> keys(DynamicOps<U> dynamicOps) {
        return this.keySet().stream().map((id) -> {
            return dynamicOps.createString(id.toString());
        });
    }

    @Nullable
    public abstract ResourceLocation getKey(T value);

    public abstract Optional<ResourceKey<T>> getResourceKey(T entry);

    @Override
    public abstract int getId(@Nullable T value);

    @Nullable
    public abstract T get(@Nullable ResourceKey<T> key);

    @Nullable
    public abstract T get(@Nullable ResourceLocation id);

    public abstract Lifecycle lifecycle(T entry);

    public abstract Lifecycle elementsLifecycle();

    public Optional<T> getOptional(@Nullable ResourceLocation id) {
        return Optional.ofNullable(this.get(id));
    }

    public Optional<T> getOptional(@Nullable ResourceKey<T> key) {
        return Optional.ofNullable(this.get(key));
    }

    public T getOrThrow(ResourceKey<T> key) {
        T object = this.get(key);
        if (object == null) {
            throw new IllegalStateException("Missing key in " + this.key + ": " + key);
        } else {
            return object;
        }
    }

    public abstract Set<ResourceLocation> keySet();

    public abstract Set<Entry<ResourceKey<T>, T>> entrySet();

    public abstract Optional<Holder<T>> getRandom(Random random);

    public Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    public abstract boolean containsKey(ResourceLocation id);

    public abstract boolean containsKey(ResourceKey<T> key);

    public static <T> T register(Registry<? super T> registry, String id, T entry) {
        return register(registry, new ResourceLocation(id), entry);
    }

    public static <V, T extends V> T register(Registry<V> registry, ResourceLocation id, T entry) {
        return register(registry, ResourceKey.create(registry.key, id), entry);
    }

    public static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> key, T entry) {
        ((WritableRegistry)registry).register(key, (V)entry, Lifecycle.stable());
        return entry;
    }

    public static <V, T extends V> T registerMapping(Registry<V> registry, int rawId, String id, T entry) {
        ((WritableRegistry)registry).registerMapping(rawId, ResourceKey.create(registry.key, new ResourceLocation(id)), (V)entry, Lifecycle.stable());
        return entry;
    }

    public abstract Registry<T> freeze();

    public abstract Holder<T> getOrCreateHolder(ResourceKey<T> key);

    public abstract Holder.Reference<T> createIntrusiveHolder(T value);

    public abstract Optional<Holder<T>> getHolder(int rawId);

    public abstract Optional<Holder<T>> getHolder(ResourceKey<T> key);

    public Holder<T> getHolderOrThrow(ResourceKey<T> key) {
        return this.getHolder(key).orElseThrow(() -> {
            return new IllegalStateException("Missing key in " + this.key + ": " + key);
        });
    }

    public abstract Stream<Holder.Reference<T>> holders();

    public abstract Optional<HolderSet.Named<T>> getTag(TagKey<T> tag);

    public Iterable<Holder<T>> getTagOrEmpty(TagKey<T> tag) {
        return DataFixUtils.orElse(this.getTag(tag), List.<T>of());
    }

    public abstract HolderSet.Named<T> getOrCreateTag(TagKey<T> tag);

    public abstract Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags();

    public abstract Stream<TagKey<T>> getTagNames();

    public abstract boolean isKnownTagName(TagKey<T> tag);

    public abstract void resetTags();

    public abstract void bindTags(Map<TagKey<T>, List<Holder<T>>> tagEntries);

    public IdMap<Holder<T>> asHolderIdMap() {
        return new IdMap<Holder<T>>() {
            @Override
            public int getId(Holder<T> value) {
                return Registry.this.getId(value.value());
            }

            @Nullable
            @Override
            public Holder<T> byId(int i) {
                return (Holder)Registry.this.getHolder(i).orElse((T)null);
            }

            @Override
            public int size() {
                return Registry.this.size();
            }

            @Override
            public Iterator<Holder<T>> iterator() {
                return Registry.this.holders().map((entry) -> {
                    return entry;
                }).iterator();
            }
        };
    }

    static {
        BuiltinRegistries.bootstrap();
        LOADERS.forEach((id, defaultEntry) -> {
            if (defaultEntry.get() == null) {
                LOGGER.error("Unable to bootstrap registry '{}'", (Object)id);
            }

        });
        checkRegistry(WRITABLE_REGISTRY);
    }

    @FunctionalInterface
    interface RegistryBootstrap<T> {
        T run(Registry<T> registry);
    }
}
