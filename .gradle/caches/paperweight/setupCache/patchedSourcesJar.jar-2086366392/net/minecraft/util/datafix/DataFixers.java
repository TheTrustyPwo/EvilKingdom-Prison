package net.minecraft.util.datafix;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.datafix.fixes.*;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V100;
import net.minecraft.util.datafix.schemas.V102;
import net.minecraft.util.datafix.schemas.V1022;
import net.minecraft.util.datafix.schemas.V106;
import net.minecraft.util.datafix.schemas.V107;
import net.minecraft.util.datafix.schemas.V1125;
import net.minecraft.util.datafix.schemas.V135;
import net.minecraft.util.datafix.schemas.V143;
import net.minecraft.util.datafix.schemas.V1451;
import net.minecraft.util.datafix.schemas.V1451_1;
import net.minecraft.util.datafix.schemas.V1451_2;
import net.minecraft.util.datafix.schemas.V1451_3;
import net.minecraft.util.datafix.schemas.V1451_4;
import net.minecraft.util.datafix.schemas.V1451_5;
import net.minecraft.util.datafix.schemas.V1451_6;
import net.minecraft.util.datafix.schemas.V1451_7;
import net.minecraft.util.datafix.schemas.V1460;
import net.minecraft.util.datafix.schemas.V1466;
import net.minecraft.util.datafix.schemas.V1470;
import net.minecraft.util.datafix.schemas.V1481;
import net.minecraft.util.datafix.schemas.V1483;
import net.minecraft.util.datafix.schemas.V1486;
import net.minecraft.util.datafix.schemas.V1510;
import net.minecraft.util.datafix.schemas.V1800;
import net.minecraft.util.datafix.schemas.V1801;
import net.minecraft.util.datafix.schemas.V1904;
import net.minecraft.util.datafix.schemas.V1906;
import net.minecraft.util.datafix.schemas.V1909;
import net.minecraft.util.datafix.schemas.V1920;
import net.minecraft.util.datafix.schemas.V1928;
import net.minecraft.util.datafix.schemas.V1929;
import net.minecraft.util.datafix.schemas.V1931;
import net.minecraft.util.datafix.schemas.V2100;
import net.minecraft.util.datafix.schemas.V2501;
import net.minecraft.util.datafix.schemas.V2502;
import net.minecraft.util.datafix.schemas.V2505;
import net.minecraft.util.datafix.schemas.V2509;
import net.minecraft.util.datafix.schemas.V2519;
import net.minecraft.util.datafix.schemas.V2522;
import net.minecraft.util.datafix.schemas.V2551;
import net.minecraft.util.datafix.schemas.V2568;
import net.minecraft.util.datafix.schemas.V2571;
import net.minecraft.util.datafix.schemas.V2684;
import net.minecraft.util.datafix.schemas.V2686;
import net.minecraft.util.datafix.schemas.V2688;
import net.minecraft.util.datafix.schemas.V2704;
import net.minecraft.util.datafix.schemas.V2707;
import net.minecraft.util.datafix.schemas.V2831;
import net.minecraft.util.datafix.schemas.V2832;
import net.minecraft.util.datafix.schemas.V2842;
import net.minecraft.util.datafix.schemas.V501;
import net.minecraft.util.datafix.schemas.V700;
import net.minecraft.util.datafix.schemas.V701;
import net.minecraft.util.datafix.schemas.V702;
import net.minecraft.util.datafix.schemas.V703;
import net.minecraft.util.datafix.schemas.V704;
import net.minecraft.util.datafix.schemas.V705;
import net.minecraft.util.datafix.schemas.V808;
import net.minecraft.util.datafix.schemas.V99;

public class DataFixers {

    private static final BiFunction<Integer, Schema, Schema> SAME = Schema::new;
    private static final BiFunction<Integer, Schema, Schema> SAME_NAMESPACED = NamespacedSchema::new;
    private static final DataFixer DATA_FIXER = DataFixers.createFixerUpper();

    public DataFixers() {}

    private static DataFixer createFixerUpper() {
        DataFixerBuilder datafixerbuilder = new DataFixerBuilder(SharedConstants.getCurrentVersion().getWorldVersion());

        DataFixers.addFixers(datafixerbuilder);
        return datafixerbuilder.build(Util.bootstrapExecutor());
    }

    public static DataFixer getDataFixer() {
        return DataFixers.DATA_FIXER;
    }

    private static void addFixers(DataFixerBuilder builder) {
        Schema schema = builder.addSchema(99, V99::new);
        Schema schema1 = builder.addSchema(100, V100::new);

        builder.addFixer(new EntityEquipmentToArmorAndHandFix(schema1, true));
        Schema schema2 = builder.addSchema(101, DataFixers.SAME);

        builder.addFixer(new BlockEntitySignTextStrictJsonFix(schema2, false));
        Schema schema3 = builder.addSchema(102, V102::new);

        builder.addFixer(new ItemIdFix(schema3, true));
        builder.addFixer(new ItemPotionFix(schema3, false));
        Schema schema4 = builder.addSchema(105, DataFixers.SAME);

        builder.addFixer(new ItemSpawnEggFix(schema4, true));
        Schema schema5 = builder.addSchema(106, V106::new);

        builder.addFixer(new MobSpawnerEntityIdentifiersFix(schema5, true));
        Schema schema6 = builder.addSchema(107, V107::new);

        builder.addFixer(new EntityMinecartIdentifiersFix(schema6, true));
        Schema schema7 = builder.addSchema(108, DataFixers.SAME);

        builder.addFixer(new EntityStringUuidFix(schema7, true));
        Schema schema8 = builder.addSchema(109, DataFixers.SAME);

        builder.addFixer(new EntityHealthFix(schema8, true));
        Schema schema9 = builder.addSchema(110, DataFixers.SAME);

        builder.addFixer(new EntityHorseSaddleFix(schema9, true));
        Schema schema10 = builder.addSchema(111, DataFixers.SAME);

        builder.addFixer(new EntityPaintingItemFrameDirectionFix(schema10, true));
        Schema schema11 = builder.addSchema(113, DataFixers.SAME);

        builder.addFixer(new EntityRedundantChanceTagsFix(schema11, true));
        Schema schema12 = builder.addSchema(135, V135::new);

        builder.addFixer(new EntityRidingToPassengersFix(schema12, true));
        Schema schema13 = builder.addSchema(143, V143::new);

        builder.addFixer(new EntityTippedArrowFix(schema13, true));
        Schema schema14 = builder.addSchema(147, DataFixers.SAME);

        builder.addFixer(new EntityArmorStandSilentFix(schema14, true));
        Schema schema15 = builder.addSchema(165, DataFixers.SAME);

        builder.addFixer(new ItemWrittenBookPagesStrictJsonFix(schema15, true));
        Schema schema16 = builder.addSchema(501, V501::new);

        builder.addFixer(new AddNewChoices(schema16, "Add 1.10 entities fix", References.ENTITY));
        Schema schema17 = builder.addSchema(502, DataFixers.SAME);

        builder.addFixer(ItemRenameFix.create(schema17, "cooked_fished item renamer", (s) -> {
            return Objects.equals(NamespacedSchema.ensureNamespaced(s), "minecraft:cooked_fished") ? "minecraft:cooked_fish" : s;
        }));
        builder.addFixer(new EntityZombieVillagerTypeFix(schema17, false));
        Schema schema18 = builder.addSchema(505, DataFixers.SAME);

        builder.addFixer(new OptionsForceVBOFix(schema18, false));
        Schema schema19 = builder.addSchema(700, V700::new);

        builder.addFixer(new EntityElderGuardianSplitFix(schema19, true));
        Schema schema20 = builder.addSchema(701, V701::new);

        builder.addFixer(new EntitySkeletonSplitFix(schema20, true));
        Schema schema21 = builder.addSchema(702, V702::new);

        builder.addFixer(new EntityZombieSplitFix(schema21, true));
        Schema schema22 = builder.addSchema(703, V703::new);

        builder.addFixer(new EntityHorseSplitFix(schema22, true));
        Schema schema23 = builder.addSchema(704, V704::new);

        builder.addFixer(new BlockEntityIdFix(schema23, true));
        Schema schema24 = builder.addSchema(705, V705::new);

        builder.addFixer(new EntityIdFix(schema24, true));
        Schema schema25 = builder.addSchema(804, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ItemBannerColorFix(schema25, true));
        Schema schema26 = builder.addSchema(806, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ItemWaterPotionFix(schema26, false));
        Schema schema27 = builder.addSchema(808, V808::new);

        builder.addFixer(new AddNewChoices(schema27, "added shulker box", References.BLOCK_ENTITY));
        Schema schema28 = builder.addSchema(808, 1, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new EntityShulkerColorFix(schema28, false));
        Schema schema29 = builder.addSchema(813, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ItemShulkerBoxColorFix(schema29, false));
        builder.addFixer(new BlockEntityShulkerBoxColorFix(schema29, false));
        Schema schema30 = builder.addSchema(816, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new OptionsLowerCaseLanguageFix(schema30, false));
        Schema schema31 = builder.addSchema(820, DataFixers.SAME_NAMESPACED);

        builder.addFixer(ItemRenameFix.create(schema31, "totem item renamer", DataFixers.createRenamer("minecraft:totem", "minecraft:totem_of_undying")));
        Schema schema32 = builder.addSchema(1022, V1022::new);

        builder.addFixer(new WriteAndReadFix(schema32, "added shoulder entities to players", References.PLAYER));
        Schema schema33 = builder.addSchema(1125, V1125::new);

        builder.addFixer(new ChunkBedBlockEntityInjecterFix(schema33, true));
        builder.addFixer(new BedItemColorFix(schema33, false));
        Schema schema34 = builder.addSchema(1344, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new OptionsKeyLwjgl3Fix(schema34, false));
        Schema schema35 = builder.addSchema(1446, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new OptionsKeyTranslationFix(schema35, false));
        Schema schema36 = builder.addSchema(1450, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new BlockStateStructureTemplateFix(schema36, false));
        Schema schema37 = builder.addSchema(1451, V1451::new);

        builder.addFixer(new AddNewChoices(schema37, "AddTrappedChestFix", References.BLOCK_ENTITY));
        Schema schema38 = builder.addSchema(1451, 1, V1451_1::new);

        builder.addFixer(new ChunkPalettedStorageFix(schema38, true));
        Schema schema39 = builder.addSchema(1451, 2, V1451_2::new);

        builder.addFixer(new BlockEntityBlockStateFix(schema39, true));
        Schema schema40 = builder.addSchema(1451, 3, V1451_3::new);

        builder.addFixer(new EntityBlockStateFix(schema40, true));
        builder.addFixer(new ItemStackMapIdFix(schema40, false));
        Schema schema41 = builder.addSchema(1451, 4, V1451_4::new);

        builder.addFixer(new BlockNameFlatteningFix(schema41, true));
        builder.addFixer(new ItemStackTheFlatteningFix(schema41, false));
        Schema schema42 = builder.addSchema(1451, 5, V1451_5::new);

        builder.addFixer(new AddNewChoices(schema42, "RemoveNoteBlockFlowerPotFix", References.BLOCK_ENTITY));
        builder.addFixer(new ItemStackSpawnEggFix(schema42, false));
        builder.addFixer(new EntityWolfColorFix(schema42, false));
        builder.addFixer(new BlockEntityBannerColorFix(schema42, false));
        builder.addFixer(new LevelFlatGeneratorInfoFix(schema42, false));
        Schema schema43 = builder.addSchema(1451, 6, V1451_6::new);

        builder.addFixer(new StatsCounterFix(schema43, true));
        builder.addFixer(new WriteAndReadFix(schema43, "Rewrite objectives", References.OBJECTIVE));
        builder.addFixer(new BlockEntityJukeboxFix(schema43, false));
        Schema schema44 = builder.addSchema(1451, 7, V1451_7::new);

        builder.addFixer(new SavedDataVillageCropFix(schema44, true));
        Schema schema45 = builder.addSchema(1451, 7, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new VillagerTradeFix(schema45, false));
        Schema schema46 = builder.addSchema(1456, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new EntityItemFrameDirectionFix(schema46, false));
        Schema schema47 = builder.addSchema(1458, DataFixers.SAME_NAMESPACED);

        // CraftBukkit start
        builder.addFixer(new com.mojang.datafixers.DataFix(schema47, false) {
            @Override
            protected com.mojang.datafixers.TypeRewriteRule makeRule() {
                return this.fixTypeEverywhereTyped("Player CustomName", this.getInputSchema().getType(References.PLAYER), (typed) -> {
                    return typed.update(DSL.remainderFinder(), (dynamic) -> {
                        return EntityCustomNameToComponentFix.fixTagCustomName(dynamic);
                    });
                });
            }
        });
        // CraftBukkit end
        builder.addFixer(new EntityCustomNameToComponentFix(schema47, false));
        builder.addFixer(new ItemCustomNameToComponentFix(schema47, false));
        builder.addFixer(new BlockEntityCustomNameToComponentFix(schema47, false));
        Schema schema48 = builder.addSchema(1460, V1460::new);

        builder.addFixer(new EntityPaintingMotiveFix(schema48, false));
        Schema schema49 = builder.addSchema(1466, V1466::new);

        builder.addFixer(new ChunkToProtochunkFix(schema49, true));
        Schema schema50 = builder.addSchema(1470, V1470::new);

        builder.addFixer(new AddNewChoices(schema50, "Add 1.13 entities fix", References.ENTITY));
        Schema schema51 = builder.addSchema(1474, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ColorlessShulkerEntityFix(schema51, false));
        builder.addFixer(BlockRenameFix.create(schema51, "Colorless shulker block fixer", (s) -> {
            return Objects.equals(NamespacedSchema.ensureNamespaced(s), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : s;
        }));
        builder.addFixer(ItemRenameFix.create(schema51, "Colorless shulker item fixer", (s) -> {
            return Objects.equals(NamespacedSchema.ensureNamespaced(s), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : s;
        }));
        Schema schema52 = builder.addSchema(1475, DataFixers.SAME_NAMESPACED);

        builder.addFixer(BlockRenameFix.create(schema52, "Flowing fixer", DataFixers.createRenamer(ImmutableMap.of("minecraft:flowing_water", "minecraft:water", "minecraft:flowing_lava", "minecraft:lava"))));
        Schema schema53 = builder.addSchema(1480, DataFixers.SAME_NAMESPACED);

        builder.addFixer(BlockRenameFix.create(schema53, "Rename coral blocks", DataFixers.createRenamer(RenamedCoralFix.RENAMED_IDS)));
        builder.addFixer(ItemRenameFix.create(schema53, "Rename coral items", DataFixers.createRenamer(RenamedCoralFix.RENAMED_IDS)));
        Schema schema54 = builder.addSchema(1481, V1481::new);

        builder.addFixer(new AddNewChoices(schema54, "Add conduit", References.BLOCK_ENTITY));
        Schema schema55 = builder.addSchema(1483, V1483::new);

        builder.addFixer(new EntityPufferfishRenameFix(schema55, true));
        builder.addFixer(ItemRenameFix.create(schema55, "Rename pufferfish egg item", DataFixers.createRenamer(EntityPufferfishRenameFix.RENAMED_IDS)));
        Schema schema56 = builder.addSchema(1484, DataFixers.SAME_NAMESPACED);

        builder.addFixer(ItemRenameFix.create(schema56, "Rename seagrass items", DataFixers.createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))));
        builder.addFixer(BlockRenameFix.create(schema56, "Rename seagrass blocks", DataFixers.createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))));
        builder.addFixer(new HeightmapRenamingFix(schema56, false));
        Schema schema57 = builder.addSchema(1486, V1486::new);

        builder.addFixer(new EntityCodSalmonFix(schema57, true));
        builder.addFixer(ItemRenameFix.create(schema57, "Rename cod/salmon egg items", DataFixers.createRenamer(EntityCodSalmonFix.RENAMED_EGG_IDS)));
        Schema schema58 = builder.addSchema(1487, DataFixers.SAME_NAMESPACED);

        builder.addFixer(ItemRenameFix.create(schema58, "Rename prismarine_brick(s)_* blocks", DataFixers.createRenamer(ImmutableMap.of("minecraft:prismarine_bricks_slab", "minecraft:prismarine_brick_slab", "minecraft:prismarine_bricks_stairs", "minecraft:prismarine_brick_stairs"))));
        builder.addFixer(BlockRenameFix.create(schema58, "Rename prismarine_brick(s)_* items", DataFixers.createRenamer(ImmutableMap.of("minecraft:prismarine_bricks_slab", "minecraft:prismarine_brick_slab", "minecraft:prismarine_bricks_stairs", "minecraft:prismarine_brick_stairs"))));
        Schema schema59 = builder.addSchema(1488, DataFixers.SAME_NAMESPACED);

        builder.addFixer(BlockRenameFix.create(schema59, "Rename kelp/kelptop", DataFixers.createRenamer(ImmutableMap.of("minecraft:kelp_top", "minecraft:kelp", "minecraft:kelp", "minecraft:kelp_plant"))));
        builder.addFixer(ItemRenameFix.create(schema59, "Rename kelptop", DataFixers.createRenamer("minecraft:kelp_top", "minecraft:kelp")));
        builder.addFixer(new NamedEntityFix(schema59, false, "Command block block entity custom name fix", References.BLOCK_ENTITY, "minecraft:command_block") {
            @Override
            protected Typed<?> fix(Typed<?> inputType) {
                return inputType.update(DSL.remainderFinder(), EntityCustomNameToComponentFix::fixTagCustomName);
            }
        });
        builder.addFixer(new NamedEntityFix(schema59, false, "Command block minecart custom name fix", References.ENTITY, "minecraft:commandblock_minecart") {
            @Override
            protected Typed<?> fix(Typed<?> inputType) {
                return inputType.update(DSL.remainderFinder(), EntityCustomNameToComponentFix::fixTagCustomName);
            }
        });
        builder.addFixer(new IglooMetadataRemovalFix(schema59, false));
        Schema schema60 = builder.addSchema(1490, DataFixers.SAME_NAMESPACED);

        builder.addFixer(BlockRenameFix.create(schema60, "Rename melon_block", DataFixers.createRenamer("minecraft:melon_block", "minecraft:melon")));
        builder.addFixer(ItemRenameFix.create(schema60, "Rename melon_block/melon/speckled_melon", DataFixers.createRenamer(ImmutableMap.of("minecraft:melon_block", "minecraft:melon", "minecraft:melon", "minecraft:melon_slice", "minecraft:speckled_melon", "minecraft:glistering_melon_slice"))));
        Schema schema61 = builder.addSchema(1492, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ChunkStructuresTemplateRenameFix(schema61, false));
        Schema schema62 = builder.addSchema(1494, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ItemStackEnchantmentNamesFix(schema62, false));
        Schema schema63 = builder.addSchema(1496, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new LeavesFix(schema63, false));
        Schema schema64 = builder.addSchema(1500, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new BlockEntityKeepPacked(schema64, false));
        Schema schema65 = builder.addSchema(1501, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new AdvancementsFix(schema65, false));
        Schema schema66 = builder.addSchema(1502, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new RecipesFix(schema66, false));
        Schema schema67 = builder.addSchema(1506, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new LevelDataGeneratorOptionsFix(schema67, false));
        Schema schema68 = builder.addSchema(1510, V1510::new);

        builder.addFixer(BlockRenameFix.create(schema68, "Block renamening fix", DataFixers.createRenamer(EntityTheRenameningFix.RENAMED_BLOCKS)));
        builder.addFixer(ItemRenameFix.create(schema68, "Item renamening fix", DataFixers.createRenamer(EntityTheRenameningFix.RENAMED_ITEMS)));
        builder.addFixer(new RecipesRenameningFix(schema68, false));
        builder.addFixer(new EntityTheRenameningFix(schema68, true));
        builder.addFixer(new StatsRenameFix(schema68, "SwimStatsRenameFix", ImmutableMap.of("minecraft:swim_one_cm", "minecraft:walk_on_water_one_cm", "minecraft:dive_one_cm", "minecraft:walk_under_water_one_cm")));
        Schema schema69 = builder.addSchema(1514, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ObjectiveDisplayNameFix(schema69, false));
        builder.addFixer(new TeamDisplayNameFix(schema69, false));
        builder.addFixer(new ObjectiveRenderTypeFix(schema69, false));
        Schema schema70 = builder.addSchema(1515, DataFixers.SAME_NAMESPACED);

        builder.addFixer(BlockRenameFix.create(schema70, "Rename coral fan blocks", DataFixers.createRenamer(RenamedCoralFansFix.RENAMED_IDS)));
        Schema schema71 = builder.addSchema(1624, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new TrappedChestBlockEntityFix(schema71, false));
        Schema schema72 = builder.addSchema(1800, V1800::new);

        builder.addFixer(new AddNewChoices(schema72, "Added 1.14 mobs fix", References.ENTITY));
        builder.addFixer(ItemRenameFix.create(schema72, "Rename dye items", DataFixers.createRenamer(DyeItemRenameFix.RENAMED_IDS)));
        Schema schema73 = builder.addSchema(1801, V1801::new);

        builder.addFixer(new AddNewChoices(schema73, "Added Illager Beast", References.ENTITY));
        Schema schema74 = builder.addSchema(1802, DataFixers.SAME_NAMESPACED);

        builder.addFixer(BlockRenameFix.create(schema74, "Rename sign blocks & stone slabs", DataFixers.createRenamer(ImmutableMap.of("minecraft:stone_slab", "minecraft:smooth_stone_slab", "minecraft:sign", "minecraft:oak_sign", "minecraft:wall_sign", "minecraft:oak_wall_sign"))));
        builder.addFixer(ItemRenameFix.create(schema74, "Rename sign item & stone slabs", DataFixers.createRenamer(ImmutableMap.of("minecraft:stone_slab", "minecraft:smooth_stone_slab", "minecraft:sign", "minecraft:oak_sign"))));
        Schema schema75 = builder.addSchema(1803, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ItemLoreFix(schema75, false));
        Schema schema76 = builder.addSchema(1904, V1904::new);

        builder.addFixer(new AddNewChoices(schema76, "Added Cats", References.ENTITY));
        builder.addFixer(new EntityCatSplitFix(schema76, false));
        Schema schema77 = builder.addSchema(1905, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ChunkStatusFix(schema77, false));
        Schema schema78 = builder.addSchema(1906, V1906::new);

        builder.addFixer(new AddNewChoices(schema78, "Add POI Blocks", References.BLOCK_ENTITY));
        Schema schema79 = builder.addSchema(1909, V1909::new);

        builder.addFixer(new AddNewChoices(schema79, "Add jigsaw", References.BLOCK_ENTITY));
        Schema schema80 = builder.addSchema(1911, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ChunkStatusFix2(schema80, false));
        Schema schema81 = builder.addSchema(1914, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new WeaponSmithChestLootTableFix(schema81, false));
        Schema schema82 = builder.addSchema(1917, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new CatTypeFix(schema82, false));
        Schema schema83 = builder.addSchema(1918, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new VillagerDataFix(schema83, "minecraft:villager"));
        builder.addFixer(new VillagerDataFix(schema83, "minecraft:zombie_villager"));
        Schema schema84 = builder.addSchema(1920, V1920::new);

        builder.addFixer(new NewVillageFix(schema84, false));
        builder.addFixer(new AddNewChoices(schema84, "Add campfire", References.BLOCK_ENTITY));
        Schema schema85 = builder.addSchema(1925, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new MapIdFix(schema85, false));
        Schema schema86 = builder.addSchema(1928, V1928::new);

        builder.addFixer(new EntityRavagerRenameFix(schema86, true));
        builder.addFixer(ItemRenameFix.create(schema86, "Rename ravager egg item", DataFixers.createRenamer(EntityRavagerRenameFix.RENAMED_IDS)));
        Schema schema87 = builder.addSchema(1929, V1929::new);

        builder.addFixer(new AddNewChoices(schema87, "Add Wandering Trader and Trader Llama", References.ENTITY));
        Schema schema88 = builder.addSchema(1931, V1931::new);

        builder.addFixer(new AddNewChoices(schema88, "Added Fox", References.ENTITY));
        Schema schema89 = builder.addSchema(1936, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new OptionsAddTextBackgroundFix(schema89, false));
        Schema schema90 = builder.addSchema(1946, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ReorganizePoi(schema90, false));
        Schema schema91 = builder.addSchema(1948, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new OminousBannerRenameFix(schema91, false));
        Schema schema92 = builder.addSchema(1953, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new OminousBannerBlockEntityRenameFix(schema92, false));
        Schema schema93 = builder.addSchema(1955, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new VillagerRebuildLevelAndXpFix(schema93, false));
        builder.addFixer(new ZombieVillagerRebuildXpFix(schema93, false));
        Schema schema94 = builder.addSchema(1961, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ChunkLightRemoveFix(schema94, false));
        Schema schema95 = builder.addSchema(1963, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new RemoveGolemGossipFix(schema95, false));
        Schema schema96 = builder.addSchema(2100, V2100::new);

        builder.addFixer(new AddNewChoices(schema96, "Added Bee and Bee Stinger", References.ENTITY));
        builder.addFixer(new AddNewChoices(schema96, "Add beehive", References.BLOCK_ENTITY));
        builder.addFixer(new RecipesRenameFix(schema96, false, "Rename sugar recipe", DataFixers.createRenamer("minecraft:sugar", "sugar_from_sugar_cane")));
        builder.addFixer(new AdvancementsRenameFix(schema96, false, "Rename sugar recipe advancement", DataFixers.createRenamer("minecraft:recipes/misc/sugar", "minecraft:recipes/misc/sugar_from_sugar_cane")));
        Schema schema97 = builder.addSchema(2202, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ChunkBiomeFix(schema97, false));
        Schema schema98 = builder.addSchema(2209, DataFixers.SAME_NAMESPACED);

        builder.addFixer(ItemRenameFix.create(schema98, "Rename bee_hive item to beehive", DataFixers.createRenamer("minecraft:bee_hive", "minecraft:beehive")));
        builder.addFixer(new BeehivePoiRenameFix(schema98));
        builder.addFixer(BlockRenameFix.create(schema98, "Rename bee_hive block to beehive", DataFixers.createRenamer("minecraft:bee_hive", "minecraft:beehive")));
        Schema schema99 = builder.addSchema(2211, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new StructureReferenceCountFix(schema99, false));
        Schema schema100 = builder.addSchema(2218, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ForcePoiRebuild(schema100, false));
        Schema schema101 = builder.addSchema(2501, V2501::new);

        builder.addFixer(new FurnaceRecipeFix(schema101, true));
        Schema schema102 = builder.addSchema(2502, V2502::new);

        builder.addFixer(new AddNewChoices(schema102, "Added Hoglin", References.ENTITY));
        Schema schema103 = builder.addSchema(2503, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new WallPropertyFix(schema103, false));
        builder.addFixer(new AdvancementsRenameFix(schema103, false, "Composter category change", DataFixers.createRenamer("minecraft:recipes/misc/composter", "minecraft:recipes/decorations/composter")));
        Schema schema104 = builder.addSchema(2505, V2505::new);

        builder.addFixer(new AddNewChoices(schema104, "Added Piglin", References.ENTITY));
        builder.addFixer(new MemoryExpiryDataFix(schema104, "minecraft:villager"));
        Schema schema105 = builder.addSchema(2508, DataFixers.SAME_NAMESPACED);

        builder.addFixer(ItemRenameFix.create(schema105, "Renamed fungi items to fungus", DataFixers.createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))));
        builder.addFixer(BlockRenameFix.create(schema105, "Renamed fungi blocks to fungus", DataFixers.createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))));
        Schema schema106 = builder.addSchema(2509, V2509::new);

        builder.addFixer(new EntityZombifiedPiglinRenameFix(schema106));
        builder.addFixer(ItemRenameFix.create(schema106, "Rename zombie pigman egg item", DataFixers.createRenamer(EntityZombifiedPiglinRenameFix.RENAMED_IDS)));
        Schema schema107 = builder.addSchema(2511, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new EntityProjectileOwnerFix(schema107));
        Schema schema108 = builder.addSchema(2514, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new EntityUUIDFix(schema108));
        builder.addFixer(new BlockEntityUUIDFix(schema108));
        builder.addFixer(new PlayerUUIDFix(schema108));
        builder.addFixer(new LevelUUIDFix(schema108));
        builder.addFixer(new SavedDataUUIDFix(schema108));
        builder.addFixer(new ItemStackUUIDFix(schema108));
        Schema schema109 = builder.addSchema(2516, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new GossipUUIDFix(schema109, "minecraft:villager"));
        builder.addFixer(new GossipUUIDFix(schema109, "minecraft:zombie_villager"));
        Schema schema110 = builder.addSchema(2518, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new JigsawPropertiesFix(schema110, false));
        builder.addFixer(new JigsawRotationFix(schema110, false));
        Schema schema111 = builder.addSchema(2519, V2519::new);

        builder.addFixer(new AddNewChoices(schema111, "Added Strider", References.ENTITY));
        Schema schema112 = builder.addSchema(2522, V2522::new);

        builder.addFixer(new AddNewChoices(schema112, "Added Zoglin", References.ENTITY));
        Schema schema113 = builder.addSchema(2523, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new AttributesRename(schema113));
        Schema schema114 = builder.addSchema(2527, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new BitStorageAlignFix(schema114));
        Schema schema115 = builder.addSchema(2528, DataFixers.SAME_NAMESPACED);

        builder.addFixer(ItemRenameFix.create(schema115, "Rename soul fire torch and soul fire lantern", DataFixers.createRenamer(ImmutableMap.of("minecraft:soul_fire_torch", "minecraft:soul_torch", "minecraft:soul_fire_lantern", "minecraft:soul_lantern"))));
        builder.addFixer(BlockRenameFix.create(schema115, "Rename soul fire torch and soul fire lantern", DataFixers.createRenamer(ImmutableMap.of("minecraft:soul_fire_torch", "minecraft:soul_torch", "minecraft:soul_fire_wall_torch", "minecraft:soul_wall_torch", "minecraft:soul_fire_lantern", "minecraft:soul_lantern"))));
        Schema schema116 = builder.addSchema(2529, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new StriderGravityFix(schema116, false));
        Schema schema117 = builder.addSchema(2531, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new RedstoneWireConnectionsFix(schema117));
        Schema schema118 = builder.addSchema(2533, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new VillagerFollowRangeFix(schema118));
        Schema schema119 = builder.addSchema(2535, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new EntityShulkerRotationFix(schema119));
        Schema schema120 = builder.addSchema(2550, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new WorldGenSettingsFix(schema120));
        Schema schema121 = builder.addSchema(2551, V2551::new);

        builder.addFixer(new WriteAndReadFix(schema121, "add types to WorldGenData", References.WORLD_GEN_SETTINGS));
        Schema schema122 = builder.addSchema(2552, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new RenameBiomesFix(schema122, false, "Nether biome rename", ImmutableMap.of("minecraft:nether", "minecraft:nether_wastes")));
        Schema schema123 = builder.addSchema(2553, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new BiomeFix(schema123, false));
        Schema schema124 = builder.addSchema(2558, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new MissingDimensionFix(schema124, false));
        builder.addFixer(new OptionsRenameFieldFix(schema124, false, "Rename swapHands setting", "key_key.swapHands", "key_key.swapOffhand"));
        Schema schema125 = builder.addSchema(2568, V2568::new);

        builder.addFixer(new AddNewChoices(schema125, "Added Piglin Brute", References.ENTITY));
        Schema schema126 = builder.addSchema(2571, V2571::new);

        builder.addFixer(new AddNewChoices(schema126, "Added Goat", References.ENTITY));
        Schema schema127 = builder.addSchema(2679, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new CauldronRenameFix(schema127, false));
        Schema schema128 = builder.addSchema(2680, DataFixers.SAME_NAMESPACED);

        builder.addFixer(ItemRenameFix.create(schema128, "Renamed grass path item to dirt path", DataFixers.createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
        builder.addFixer(BlockRenameFixWithJigsaw.create(schema128, "Renamed grass path block to dirt path", DataFixers.createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
        Schema schema129 = builder.addSchema(2684, V2684::new);

        builder.addFixer(new AddNewChoices(schema129, "Added Sculk Sensor", References.BLOCK_ENTITY));
        Schema schema130 = builder.addSchema(2686, V2686::new);

        builder.addFixer(new AddNewChoices(schema130, "Added Axolotl", References.ENTITY));
        Schema schema131 = builder.addSchema(2688, V2688::new);

        builder.addFixer(new AddNewChoices(schema131, "Added Glow Squid", References.ENTITY));
        builder.addFixer(new AddNewChoices(schema131, "Added Glow Item Frame", References.ENTITY));
        Schema schema132 = builder.addSchema(2690, DataFixers.SAME_NAMESPACED);
        // CraftBukkit - decompile error
        ImmutableMap<String, String> immutablemap = ImmutableMap.<String, String>builder().put("minecraft:weathered_copper_block", "minecraft:oxidized_copper_block").put("minecraft:semi_weathered_copper_block", "minecraft:weathered_copper_block").put("minecraft:lightly_weathered_copper_block", "minecraft:exposed_copper_block").put("minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper").put("minecraft:semi_weathered_cut_copper", "minecraft:weathered_cut_copper").put("minecraft:lightly_weathered_cut_copper", "minecraft:exposed_cut_copper").put("minecraft:weathered_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs").put("minecraft:semi_weathered_cut_copper_stairs", "minecraft:weathered_cut_copper_stairs").put("minecraft:lightly_weathered_cut_copper_stairs", "minecraft:exposed_cut_copper_stairs").put("minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab").put("minecraft:semi_weathered_cut_copper_slab", "minecraft:weathered_cut_copper_slab").put("minecraft:lightly_weathered_cut_copper_slab", "minecraft:exposed_cut_copper_slab").put("minecraft:waxed_semi_weathered_copper", "minecraft:waxed_weathered_copper").put("minecraft:waxed_lightly_weathered_copper", "minecraft:waxed_exposed_copper").put("minecraft:waxed_semi_weathered_cut_copper", "minecraft:waxed_weathered_cut_copper").put("minecraft:waxed_lightly_weathered_cut_copper", "minecraft:waxed_exposed_cut_copper").put("minecraft:waxed_semi_weathered_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper_stairs").put("minecraft:waxed_lightly_weathered_cut_copper_stairs", "minecraft:waxed_exposed_cut_copper_stairs").put("minecraft:waxed_semi_weathered_cut_copper_slab", "minecraft:waxed_weathered_cut_copper_slab").put("minecraft:waxed_lightly_weathered_cut_copper_slab", "minecraft:waxed_exposed_cut_copper_slab").build();

        builder.addFixer(ItemRenameFix.create(schema132, "Renamed copper block items to new oxidized terms", DataFixers.createRenamer(immutablemap)));
        builder.addFixer(BlockRenameFixWithJigsaw.create(schema132, "Renamed copper blocks to new oxidized terms", DataFixers.createRenamer(immutablemap)));
        Schema schema133 = builder.addSchema(2691, DataFixers.SAME_NAMESPACED);
        // CraftBukkit - decompile error
        ImmutableMap<String, String> immutablemap1 = ImmutableMap.<String, String>builder().put("minecraft:waxed_copper", "minecraft:waxed_copper_block").put("minecraft:oxidized_copper_block", "minecraft:oxidized_copper").put("minecraft:weathered_copper_block", "minecraft:weathered_copper").put("minecraft:exposed_copper_block", "minecraft:exposed_copper").build();

        builder.addFixer(ItemRenameFix.create(schema133, "Rename copper item suffixes", DataFixers.createRenamer(immutablemap1)));
        builder.addFixer(BlockRenameFixWithJigsaw.create(schema133, "Rename copper blocks suffixes", DataFixers.createRenamer(immutablemap1)));
        Schema schema134 = builder.addSchema(2693, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new AddFlagIfNotPresentFix(schema134, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
        Schema schema135 = builder.addSchema(2696, DataFixers.SAME_NAMESPACED);
        // CraftBukkit - decompile error
        ImmutableMap<String, String> immutablemap2 = ImmutableMap.<String, String>builder().put("minecraft:grimstone", "minecraft:deepslate").put("minecraft:grimstone_slab", "minecraft:cobbled_deepslate_slab").put("minecraft:grimstone_stairs", "minecraft:cobbled_deepslate_stairs").put("minecraft:grimstone_wall", "minecraft:cobbled_deepslate_wall").put("minecraft:polished_grimstone", "minecraft:polished_deepslate").put("minecraft:polished_grimstone_slab", "minecraft:polished_deepslate_slab").put("minecraft:polished_grimstone_stairs", "minecraft:polished_deepslate_stairs").put("minecraft:polished_grimstone_wall", "minecraft:polished_deepslate_wall").put("minecraft:grimstone_tiles", "minecraft:deepslate_tiles").put("minecraft:grimstone_tile_slab", "minecraft:deepslate_tile_slab").put("minecraft:grimstone_tile_stairs", "minecraft:deepslate_tile_stairs").put("minecraft:grimstone_tile_wall", "minecraft:deepslate_tile_wall").put("minecraft:grimstone_bricks", "minecraft:deepslate_bricks").put("minecraft:grimstone_brick_slab", "minecraft:deepslate_brick_slab").put("minecraft:grimstone_brick_stairs", "minecraft:deepslate_brick_stairs").put("minecraft:grimstone_brick_wall", "minecraft:deepslate_brick_wall").put("minecraft:chiseled_grimstone", "minecraft:chiseled_deepslate").build();

        builder.addFixer(ItemRenameFix.create(schema135, "Renamed grimstone block items to deepslate", DataFixers.createRenamer(immutablemap2)));
        builder.addFixer(BlockRenameFixWithJigsaw.create(schema135, "Renamed grimstone blocks to deepslate", DataFixers.createRenamer(immutablemap2)));
        Schema schema136 = builder.addSchema(2700, DataFixers.SAME_NAMESPACED);

        builder.addFixer(BlockRenameFixWithJigsaw.create(schema136, "Renamed cave vines blocks", DataFixers.createRenamer(ImmutableMap.of("minecraft:cave_vines_head", "minecraft:cave_vines", "minecraft:cave_vines_body", "minecraft:cave_vines_plant"))));
        Schema schema137 = builder.addSchema(2701, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new SavedDataFeaturePoolElementFix(schema137));
        Schema schema138 = builder.addSchema(2702, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new AbstractArrowPickupFix(schema138));
        Schema schema139 = builder.addSchema(2704, V2704::new);

        builder.addFixer(new AddNewChoices(schema139, "Added Goat", References.ENTITY));
        Schema schema140 = builder.addSchema(2707, V2707::new);

        builder.addFixer(new AddNewChoices(schema140, "Added Marker", References.ENTITY));
        builder.addFixer(new AddFlagIfNotPresentFix(schema140, References.WORLD_GEN_SETTINGS, "has_increased_height_already", true));
        Schema schema141 = builder.addSchema(2710, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new StatsRenameFix(schema141, "Renamed play_one_minute stat to play_time", ImmutableMap.of("minecraft:play_one_minute", "minecraft:play_time")));
        Schema schema142 = builder.addSchema(2717, DataFixers.SAME_NAMESPACED);

        builder.addFixer(ItemRenameFix.create(schema142, "Rename azalea_leaves_flowers", DataFixers.createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))));
        builder.addFixer(BlockRenameFix.create(schema142, "Rename azalea_leaves_flowers items", DataFixers.createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))));
        Schema schema143 = builder.addSchema(2825, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new AddFlagIfNotPresentFix(schema143, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
        Schema schema144 = builder.addSchema(2831, V2831::new);

        builder.addFixer(new SpawnerDataFix(schema144));
        Schema schema145 = builder.addSchema(2832, V2832::new);

        builder.addFixer(new WorldGenSettingsHeightAndBiomeFix(schema145));
        builder.addFixer(new ChunkHeightAndBiomeFix(schema145));
        Schema schema146 = builder.addSchema(2833, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix(schema146));
        Schema schema147 = builder.addSchema(2838, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new RenameBiomesFix(schema147, false, "Caves and Cliffs biome renames", CavesAndCliffsRenames.RENAMES));
        Schema schema148 = builder.addSchema(2841, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new ChunkProtoTickListFix(schema148));
        Schema schema149 = builder.addSchema(2842, V2842::new);

        builder.addFixer(new ChunkRenamesFix(schema149));
        Schema schema150 = builder.addSchema(2843, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new OverreachingTickFix(schema150));
        builder.addFixer(new RenameBiomesFix(schema150, false, "Remove Deep Warm Ocean", Map.of("minecraft:deep_warm_ocean", "minecraft:warm_ocean")));
        Schema schema151 = builder.addSchema(2846, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new AdvancementsRenameFix(schema151, false, "Rename some C&C part 2 advancements", DataFixers.createRenamer(ImmutableMap.of("minecraft:husbandry/play_jukebox_in_meadows", "minecraft:adventure/play_jukebox_in_meadows", "minecraft:adventure/caves_and_cliff", "minecraft:adventure/fall_from_world_height", "minecraft:adventure/ride_strider_in_overworld_lava", "minecraft:nether/ride_strider_in_overworld_lava"))));
        Schema schema152 = builder.addSchema(2852, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix(schema152));
        Schema schema153 = builder.addSchema(2967, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new StructureSettingsFlattenFix(schema153));
        Schema schema154 = builder.addSchema(2970, DataFixers.SAME_NAMESPACED);

        builder.addFixer(new StructuresBecomeConfiguredFix(schema154));
    }

    private static UnaryOperator<String> createRenamer(Map<String, String> replacements) {
        return (s) -> {
            return (String) replacements.getOrDefault(s, s);
        };
    }

    private static UnaryOperator<String> createRenamer(String old, String current) {
        return (s2) -> {
            return Objects.equals(s2, old) ? current : s2;
        };
    }
}
