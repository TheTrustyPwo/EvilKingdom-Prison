package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class IglooPieces {
    public static final int GENERATION_HEIGHT = 90;
    static final ResourceLocation STRUCTURE_LOCATION_IGLOO = new ResourceLocation("igloo/top");
    private static final ResourceLocation STRUCTURE_LOCATION_LADDER = new ResourceLocation("igloo/middle");
    private static final ResourceLocation STRUCTURE_LOCATION_LABORATORY = new ResourceLocation("igloo/bottom");
    static final Map<ResourceLocation, BlockPos> PIVOTS = ImmutableMap.of(STRUCTURE_LOCATION_IGLOO, new BlockPos(3, 5, 5), STRUCTURE_LOCATION_LADDER, new BlockPos(1, 3, 1), STRUCTURE_LOCATION_LABORATORY, new BlockPos(3, 6, 7));
    static final Map<ResourceLocation, BlockPos> OFFSETS = ImmutableMap.of(STRUCTURE_LOCATION_IGLOO, BlockPos.ZERO, STRUCTURE_LOCATION_LADDER, new BlockPos(2, -3, 4), STRUCTURE_LOCATION_LABORATORY, new BlockPos(0, -3, -2));

    public static void addPieces(StructureManager manager, BlockPos pos, Rotation rotation, StructurePieceAccessor holder, Random random) {
        if (random.nextDouble() < 0.5D) {
            int i = random.nextInt(8) + 4;
            holder.addPiece(new IglooPieces.IglooPiece(manager, STRUCTURE_LOCATION_LABORATORY, pos, rotation, i * 3));

            for(int j = 0; j < i - 1; ++j) {
                holder.addPiece(new IglooPieces.IglooPiece(manager, STRUCTURE_LOCATION_LADDER, pos, rotation, j * 3));
            }
        }

        holder.addPiece(new IglooPieces.IglooPiece(manager, STRUCTURE_LOCATION_IGLOO, pos, rotation, 0));
    }

    public static class IglooPiece extends TemplateStructurePiece {
        public IglooPiece(StructureManager manager, ResourceLocation identifier, BlockPos pos, Rotation rotation, int yOffset) {
            super(StructurePieceType.IGLOO, 0, manager, identifier, identifier.toString(), makeSettings(rotation, identifier), makePosition(identifier, pos, yOffset));
        }

        public IglooPiece(StructureManager manager, CompoundTag nbt) {
            super(StructurePieceType.IGLOO, nbt, manager, (identifier) -> {
                return makeSettings(Rotation.valueOf(nbt.getString("Rot")), identifier);
            });
        }

        private static StructurePlaceSettings makeSettings(Rotation rotation, ResourceLocation identifier) {
            return (new StructurePlaceSettings()).setRotation(rotation).setMirror(Mirror.NONE).setRotationPivot(IglooPieces.PIVOTS.get(identifier)).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        }

        private static BlockPos makePosition(ResourceLocation identifier, BlockPos pos, int yOffset) {
            return pos.offset(IglooPieces.OFFSETS.get(identifier)).below(yOffset);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putString("Rot", this.placeSettings.getRotation().name());
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPos pos, ServerLevelAccessor world, Random random, BoundingBox boundingBox) {
            if ("chest".equals(metadata)) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                BlockEntity blockEntity = world.getBlockEntity(pos.below());
                if (blockEntity instanceof ChestBlockEntity) {
                    ((ChestBlockEntity)blockEntity).setLootTable(BuiltInLootTables.IGLOO_CHEST, random.nextLong());
                }

            }
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            ResourceLocation resourceLocation = new ResourceLocation(this.templateName);
            StructurePlaceSettings structurePlaceSettings = makeSettings(this.placeSettings.getRotation(), resourceLocation);
            BlockPos blockPos = IglooPieces.OFFSETS.get(resourceLocation);
            BlockPos blockPos2 = this.templatePosition.offset(StructureTemplate.calculateRelativePosition(structurePlaceSettings, new BlockPos(3 - blockPos.getX(), 0, -blockPos.getZ())));
            int i = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockPos2.getX(), blockPos2.getZ());
            BlockPos blockPos3 = this.templatePosition;
            this.templatePosition = this.templatePosition.offset(0, i - 90 - 1, 0);
            super.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pos);
            if (resourceLocation.equals(IglooPieces.STRUCTURE_LOCATION_IGLOO)) {
                BlockPos blockPos4 = this.templatePosition.offset(StructureTemplate.calculateRelativePosition(structurePlaceSettings, new BlockPos(3, 0, 5)));
                BlockState blockState = world.getBlockState(blockPos4.below());
                if (!blockState.isAir() && !blockState.is(Blocks.LADDER)) {
                    world.setBlock(blockPos4, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
                }
            }

            this.templatePosition = blockPos3;
        }
    }
}
