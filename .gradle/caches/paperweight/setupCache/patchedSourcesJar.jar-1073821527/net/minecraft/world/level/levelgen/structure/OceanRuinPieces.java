package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.OceanRuinConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class OceanRuinPieces {
    private static final ResourceLocation[] WARM_RUINS = new ResourceLocation[]{new ResourceLocation("underwater_ruin/warm_1"), new ResourceLocation("underwater_ruin/warm_2"), new ResourceLocation("underwater_ruin/warm_3"), new ResourceLocation("underwater_ruin/warm_4"), new ResourceLocation("underwater_ruin/warm_5"), new ResourceLocation("underwater_ruin/warm_6"), new ResourceLocation("underwater_ruin/warm_7"), new ResourceLocation("underwater_ruin/warm_8")};
    private static final ResourceLocation[] RUINS_BRICK = new ResourceLocation[]{new ResourceLocation("underwater_ruin/brick_1"), new ResourceLocation("underwater_ruin/brick_2"), new ResourceLocation("underwater_ruin/brick_3"), new ResourceLocation("underwater_ruin/brick_4"), new ResourceLocation("underwater_ruin/brick_5"), new ResourceLocation("underwater_ruin/brick_6"), new ResourceLocation("underwater_ruin/brick_7"), new ResourceLocation("underwater_ruin/brick_8")};
    private static final ResourceLocation[] RUINS_CRACKED = new ResourceLocation[]{new ResourceLocation("underwater_ruin/cracked_1"), new ResourceLocation("underwater_ruin/cracked_2"), new ResourceLocation("underwater_ruin/cracked_3"), new ResourceLocation("underwater_ruin/cracked_4"), new ResourceLocation("underwater_ruin/cracked_5"), new ResourceLocation("underwater_ruin/cracked_6"), new ResourceLocation("underwater_ruin/cracked_7"), new ResourceLocation("underwater_ruin/cracked_8")};
    private static final ResourceLocation[] RUINS_MOSSY = new ResourceLocation[]{new ResourceLocation("underwater_ruin/mossy_1"), new ResourceLocation("underwater_ruin/mossy_2"), new ResourceLocation("underwater_ruin/mossy_3"), new ResourceLocation("underwater_ruin/mossy_4"), new ResourceLocation("underwater_ruin/mossy_5"), new ResourceLocation("underwater_ruin/mossy_6"), new ResourceLocation("underwater_ruin/mossy_7"), new ResourceLocation("underwater_ruin/mossy_8")};
    private static final ResourceLocation[] BIG_RUINS_BRICK = new ResourceLocation[]{new ResourceLocation("underwater_ruin/big_brick_1"), new ResourceLocation("underwater_ruin/big_brick_2"), new ResourceLocation("underwater_ruin/big_brick_3"), new ResourceLocation("underwater_ruin/big_brick_8")};
    private static final ResourceLocation[] BIG_RUINS_MOSSY = new ResourceLocation[]{new ResourceLocation("underwater_ruin/big_mossy_1"), new ResourceLocation("underwater_ruin/big_mossy_2"), new ResourceLocation("underwater_ruin/big_mossy_3"), new ResourceLocation("underwater_ruin/big_mossy_8")};
    private static final ResourceLocation[] BIG_RUINS_CRACKED = new ResourceLocation[]{new ResourceLocation("underwater_ruin/big_cracked_1"), new ResourceLocation("underwater_ruin/big_cracked_2"), new ResourceLocation("underwater_ruin/big_cracked_3"), new ResourceLocation("underwater_ruin/big_cracked_8")};
    private static final ResourceLocation[] BIG_WARM_RUINS = new ResourceLocation[]{new ResourceLocation("underwater_ruin/big_warm_4"), new ResourceLocation("underwater_ruin/big_warm_5"), new ResourceLocation("underwater_ruin/big_warm_6"), new ResourceLocation("underwater_ruin/big_warm_7")};

    private static ResourceLocation getSmallWarmRuin(Random random) {
        return Util.getRandom(WARM_RUINS, random);
    }

    private static ResourceLocation getBigWarmRuin(Random random) {
        return Util.getRandom(BIG_WARM_RUINS, random);
    }

    public static void addPieces(StructureManager manager, BlockPos pos, Rotation rotation, StructurePieceAccessor holder, Random random, OceanRuinConfiguration config) {
        boolean bl = random.nextFloat() <= config.largeProbability;
        float f = bl ? 0.9F : 0.8F;
        addPiece(manager, pos, rotation, holder, random, config, bl, f);
        if (bl && random.nextFloat() <= config.clusterProbability) {
            addClusterRuins(manager, random, rotation, pos, config, holder);
        }

    }

    private static void addClusterRuins(StructureManager manager, Random random, Rotation rotation, BlockPos pos, OceanRuinConfiguration config, StructurePieceAccessor structurePieceAccessor) {
        BlockPos blockPos = new BlockPos(pos.getX(), 90, pos.getZ());
        BlockPos blockPos2 = StructureTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, rotation, BlockPos.ZERO).offset(blockPos);
        BoundingBox boundingBox = BoundingBox.fromCorners(blockPos, blockPos2);
        BlockPos blockPos3 = new BlockPos(Math.min(blockPos.getX(), blockPos2.getX()), blockPos.getY(), Math.min(blockPos.getZ(), blockPos2.getZ()));
        List<BlockPos> list = allPositions(random, blockPos3);
        int i = Mth.nextInt(random, 4, 8);

        for(int j = 0; j < i; ++j) {
            if (!list.isEmpty()) {
                int k = random.nextInt(list.size());
                BlockPos blockPos4 = list.remove(k);
                Rotation rotation2 = Rotation.getRandom(random);
                BlockPos blockPos5 = StructureTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, rotation2, BlockPos.ZERO).offset(blockPos4);
                BoundingBox boundingBox2 = BoundingBox.fromCorners(blockPos4, blockPos5);
                if (!boundingBox2.intersects(boundingBox)) {
                    addPiece(manager, blockPos4, rotation2, structurePieceAccessor, random, config, false, 0.8F);
                }
            }
        }

    }

    private static List<BlockPos> allPositions(Random random, BlockPos pos) {
        List<BlockPos> list = Lists.newArrayList();
        list.add(pos.offset(-16 + Mth.nextInt(random, 1, 8), 0, 16 + Mth.nextInt(random, 1, 7)));
        list.add(pos.offset(-16 + Mth.nextInt(random, 1, 8), 0, Mth.nextInt(random, 1, 7)));
        list.add(pos.offset(-16 + Mth.nextInt(random, 1, 8), 0, -16 + Mth.nextInt(random, 4, 8)));
        list.add(pos.offset(Mth.nextInt(random, 1, 7), 0, 16 + Mth.nextInt(random, 1, 7)));
        list.add(pos.offset(Mth.nextInt(random, 1, 7), 0, -16 + Mth.nextInt(random, 4, 6)));
        list.add(pos.offset(16 + Mth.nextInt(random, 1, 7), 0, 16 + Mth.nextInt(random, 3, 8)));
        list.add(pos.offset(16 + Mth.nextInt(random, 1, 7), 0, Mth.nextInt(random, 1, 7)));
        list.add(pos.offset(16 + Mth.nextInt(random, 1, 7), 0, -16 + Mth.nextInt(random, 4, 8)));
        return list;
    }

    private static void addPiece(StructureManager manager, BlockPos pos, Rotation rotation, StructurePieceAccessor holder, Random random, OceanRuinConfiguration config, boolean large, float integrity) {
        switch(config.biomeTemp) {
        case WARM:
        default:
            ResourceLocation resourceLocation = large ? getBigWarmRuin(random) : getSmallWarmRuin(random);
            holder.addPiece(new OceanRuinPieces.OceanRuinPiece(manager, resourceLocation, pos, rotation, integrity, config.biomeTemp, large));
            break;
        case COLD:
            ResourceLocation[] resourceLocations = large ? BIG_RUINS_BRICK : RUINS_BRICK;
            ResourceLocation[] resourceLocations2 = large ? BIG_RUINS_CRACKED : RUINS_CRACKED;
            ResourceLocation[] resourceLocations3 = large ? BIG_RUINS_MOSSY : RUINS_MOSSY;
            int i = random.nextInt(resourceLocations.length);
            holder.addPiece(new OceanRuinPieces.OceanRuinPiece(manager, resourceLocations[i], pos, rotation, integrity, config.biomeTemp, large));
            holder.addPiece(new OceanRuinPieces.OceanRuinPiece(manager, resourceLocations2[i], pos, rotation, 0.7F, config.biomeTemp, large));
            holder.addPiece(new OceanRuinPieces.OceanRuinPiece(manager, resourceLocations3[i], pos, rotation, 0.5F, config.biomeTemp, large));
        }

    }

    public static class OceanRuinPiece extends TemplateStructurePiece {
        private final OceanRuinFeature.Type biomeType;
        private final float integrity;
        private final boolean isLarge;

        public OceanRuinPiece(StructureManager structureManager, ResourceLocation template, BlockPos pos, Rotation rotation, float integrity, OceanRuinFeature.Type biomeType, boolean large) {
            super(StructurePieceType.OCEAN_RUIN, 0, structureManager, template, template.toString(), makeSettings(rotation), pos);
            this.integrity = integrity;
            this.biomeType = biomeType;
            this.isLarge = large;
        }

        public OceanRuinPiece(StructureManager holder, CompoundTag nbt) {
            super(StructurePieceType.OCEAN_RUIN, nbt, holder, (resourceLocation) -> {
                return makeSettings(Rotation.valueOf(nbt.getString("Rot")));
            });
            this.integrity = nbt.getFloat("Integrity");
            this.biomeType = OceanRuinFeature.Type.valueOf(nbt.getString("BiomeType"));
            this.isLarge = nbt.getBoolean("IsLarge");
        }

        private static StructurePlaceSettings makeSettings(Rotation rotation) {
            return (new StructurePlaceSettings()).setRotation(rotation).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putString("Rot", this.placeSettings.getRotation().name());
            nbt.putFloat("Integrity", this.integrity);
            nbt.putString("BiomeType", this.biomeType.toString());
            nbt.putBoolean("IsLarge", this.isLarge);
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPos pos, ServerLevelAccessor world, Random random, BoundingBox boundingBox) {
            if ("chest".equals(metadata)) {
                world.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, Boolean.valueOf(world.getFluidState(pos).is(FluidTags.WATER))), 2);
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof ChestBlockEntity) {
                    ((ChestBlockEntity)blockEntity).setLootTable(this.isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL, random.nextLong());
                }
            } else if ("drowned".equals(metadata)) {
                Drowned drowned = EntityType.DROWNED.create(world.getLevel());
                drowned.setPersistenceRequired();
                drowned.moveTo(pos, 0.0F, 0.0F);
                drowned.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, (SpawnGroupData)null, (CompoundTag)null);
                world.addFreshEntityWithPassengers(drowned);
                if (pos.getY() > world.getSeaLevel()) {
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                } else {
                    world.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                }
            }

        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.placeSettings.clearProcessors().addProcessor(new BlockRotProcessor(this.integrity)).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
            int i = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
            this.templatePosition = new BlockPos(this.templatePosition.getX(), i, this.templatePosition.getZ());
            BlockPos blockPos = StructureTemplate.transform(new BlockPos(this.template.getSize().getX() - 1, 0, this.template.getSize().getZ() - 1), Mirror.NONE, this.placeSettings.getRotation(), BlockPos.ZERO).offset(this.templatePosition);
            this.templatePosition = new BlockPos(this.templatePosition.getX(), this.getHeight(this.templatePosition, world, blockPos), this.templatePosition.getZ());
            super.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pos);
        }

        private int getHeight(BlockPos start, BlockGetter world, BlockPos end) {
            int i = start.getY();
            int j = 512;
            int k = i - 1;
            int l = 0;

            for(BlockPos blockPos : BlockPos.betweenClosed(start, end)) {
                int m = blockPos.getX();
                int n = blockPos.getZ();
                int o = start.getY() - 1;
                BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(m, o, n);
                BlockState blockState = world.getBlockState(mutableBlockPos);

                for(FluidState fluidState = world.getFluidState(mutableBlockPos); (blockState.isAir() || fluidState.is(FluidTags.WATER) || blockState.is(BlockTags.ICE)) && o > world.getMinBuildHeight() + 1; fluidState = world.getFluidState(mutableBlockPos)) {
                    --o;
                    mutableBlockPos.set(m, o, n);
                    blockState = world.getBlockState(mutableBlockPos);
                }

                j = Math.min(j, o);
                if (o < k - 2) {
                    ++l;
                }
            }

            int p = Math.abs(start.getX() - end.getX());
            if (k - j > 2 && l > p - 2) {
                i = j + 1;
            }

            return i;
        }
    }
}
