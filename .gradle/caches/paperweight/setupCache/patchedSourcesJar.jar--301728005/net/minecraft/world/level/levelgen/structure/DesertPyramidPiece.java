package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertPyramidPiece extends ScatteredFeaturePiece {
    public static final int WIDTH = 21;
    public static final int DEPTH = 21;
    private final boolean[] hasPlacedChest = new boolean[4];

    public DesertPyramidPiece(Random random, int x, int z) {
        super(StructurePieceType.DESERT_PYRAMID_PIECE, x, 64, z, 21, 15, 21, getRandomHorizontalDirection(random));
    }

    public DesertPyramidPiece(CompoundTag nbt) {
        super(StructurePieceType.DESERT_PYRAMID_PIECE, nbt);
        this.hasPlacedChest[0] = nbt.getBoolean("hasPlacedChest0");
        this.hasPlacedChest[1] = nbt.getBoolean("hasPlacedChest1");
        this.hasPlacedChest[2] = nbt.getBoolean("hasPlacedChest2");
        this.hasPlacedChest[3] = nbt.getBoolean("hasPlacedChest3");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        super.addAdditionalSaveData(context, nbt);
        nbt.putBoolean("hasPlacedChest0", this.hasPlacedChest[0]);
        nbt.putBoolean("hasPlacedChest1", this.hasPlacedChest[1]);
        nbt.putBoolean("hasPlacedChest2", this.hasPlacedChest[2]);
        nbt.putBoolean("hasPlacedChest3", this.hasPlacedChest[3]);
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        if (this.updateHeightPositionToLowestGroundHeight(world, -random.nextInt(3))) {
            this.generateBox(world, chunkBox, 0, -4, 0, this.width - 1, 0, this.depth - 1, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);

            for(int i = 1; i <= 9; ++i) {
                this.generateBox(world, chunkBox, i, i, i, this.width - 1 - i, i, this.depth - 1 - i, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
                this.generateBox(world, chunkBox, i + 1, i, i + 1, this.width - 2 - i, i, this.depth - 2 - i, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            }

            for(int j = 0; j < this.width; ++j) {
                for(int k = 0; k < this.depth; ++k) {
                    int l = -5;
                    this.fillColumnDown(world, Blocks.SANDSTONE.defaultBlockState(), j, -5, k, chunkBox);
                }
            }

            BlockState blockState = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            BlockState blockState2 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            BlockState blockState3 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
            BlockState blockState4 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 9, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 10, 1, 3, 10, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.placeBlock(world, blockState, 2, 10, 0, chunkBox);
            this.placeBlock(world, blockState2, 2, 10, 4, chunkBox);
            this.placeBlock(world, blockState3, 0, 10, 2, chunkBox);
            this.placeBlock(world, blockState4, 4, 10, 2, chunkBox);
            this.generateBox(world, chunkBox, this.width - 5, 0, 0, this.width - 1, 9, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, this.width - 4, 10, 1, this.width - 2, 10, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.placeBlock(world, blockState, this.width - 3, 10, 0, chunkBox);
            this.placeBlock(world, blockState2, this.width - 3, 10, 4, chunkBox);
            this.placeBlock(world, blockState3, this.width - 5, 10, 2, chunkBox);
            this.placeBlock(world, blockState4, this.width - 1, 10, 2, chunkBox);
            this.generateBox(world, chunkBox, 8, 0, 0, 12, 4, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 9, 1, 0, 11, 3, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 1, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 2, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 3, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, 3, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 3, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 2, 1, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 1, 1, chunkBox);
            this.generateBox(world, chunkBox, 4, 1, 1, 8, 3, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 1, 2, 8, 2, 2, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 12, 1, 1, 16, 3, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 12, 1, 2, 16, 2, 2, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 4, 5, this.width - 6, 4, this.depth - 6, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 9, 4, 9, 11, 4, 11, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 1, 8, 8, 3, 8, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 12, 1, 8, 12, 3, 8, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 1, 12, 8, 3, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 12, 1, 12, 12, 3, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 1, 5, 4, 4, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, this.width - 5, 1, 5, this.width - 2, 4, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 7, 9, 6, 7, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, this.width - 7, 7, 9, this.width - 7, 7, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 5, 9, 5, 7, 11, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, this.width - 6, 5, 9, this.width - 6, 7, 11, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 5, 5, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 5, 6, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 6, 6, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), this.width - 6, 5, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), this.width - 6, 6, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), this.width - 7, 6, 10, chunkBox);
            this.generateBox(world, chunkBox, 2, 4, 4, 2, 6, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, this.width - 3, 4, 4, this.width - 3, 6, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(world, blockState, 2, 4, 5, chunkBox);
            this.placeBlock(world, blockState, 2, 3, 4, chunkBox);
            this.placeBlock(world, blockState, this.width - 3, 4, 5, chunkBox);
            this.placeBlock(world, blockState, this.width - 3, 3, 4, chunkBox);
            this.generateBox(world, chunkBox, 1, 1, 3, 2, 2, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, this.width - 3, 1, 3, this.width - 2, 2, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.placeBlock(world, Blocks.SANDSTONE.defaultBlockState(), 1, 1, 2, chunkBox);
            this.placeBlock(world, Blocks.SANDSTONE.defaultBlockState(), this.width - 2, 1, 2, chunkBox);
            this.placeBlock(world, Blocks.SANDSTONE_SLAB.defaultBlockState(), 1, 2, 2, chunkBox);
            this.placeBlock(world, Blocks.SANDSTONE_SLAB.defaultBlockState(), this.width - 2, 2, 2, chunkBox);
            this.placeBlock(world, blockState4, 2, 1, 2, chunkBox);
            this.placeBlock(world, blockState3, this.width - 3, 1, 2, chunkBox);
            this.generateBox(world, chunkBox, 4, 3, 5, 4, 3, 17, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, this.width - 5, 3, 5, this.width - 5, 3, 17, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 3, 1, 5, 4, 2, 16, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, this.width - 6, 1, 5, this.width - 5, 2, 16, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);

            for(int m = 5; m <= 17; m += 2) {
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 4, 1, m, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 4, 2, m, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), this.width - 5, 1, m, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), this.width - 5, 2, m, chunkBox);
            }

            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 7, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 8, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 0, 9, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 0, 9, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 8, 0, 10, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 12, 0, 10, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 7, 0, 10, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 13, 0, 10, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 0, 11, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 0, 11, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 12, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 13, chunkBox);
            this.placeBlock(world, Blocks.BLUE_TERRACOTTA.defaultBlockState(), 10, 0, 10, chunkBox);

            for(int n = 0; n <= this.width - 1; n += this.width - 1) {
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 2, 1, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 2, 2, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 2, 3, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 3, 1, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 3, 2, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 3, 3, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 4, 1, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), n, 4, 2, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 4, 3, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 5, 1, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 5, 2, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 5, 3, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 6, 1, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), n, 6, 2, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 6, 3, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 7, 1, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 7, 2, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), n, 7, 3, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 8, 1, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 8, 2, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), n, 8, 3, chunkBox);
            }

            for(int o = 2; o <= this.width - 3; o += this.width - 3 - 2) {
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o - 1, 2, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o, 2, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o + 1, 2, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o - 1, 3, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o, 3, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o + 1, 3, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o - 1, 4, 0, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), o, 4, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o + 1, 4, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o - 1, 5, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o, 5, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o + 1, 5, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o - 1, 6, 0, chunkBox);
                this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), o, 6, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o + 1, 6, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o - 1, 7, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o, 7, 0, chunkBox);
                this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), o + 1, 7, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o - 1, 8, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o, 8, 0, chunkBox);
                this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), o + 1, 8, 0, chunkBox);
            }

            this.generateBox(world, chunkBox, 8, 4, 0, 12, 6, 0, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 8, 6, 0, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 12, 6, 0, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 5, 0, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, 5, 0, chunkBox);
            this.placeBlock(world, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 5, 0, chunkBox);
            this.generateBox(world, chunkBox, 8, -14, 8, 12, -11, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, -10, 8, 12, -10, 12, Blocks.CHISELED_SANDSTONE.defaultBlockState(), Blocks.CHISELED_SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, -9, 8, 12, -9, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, -8, 8, 12, -1, 12, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 9, -11, 9, 11, -1, 11, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(world, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 10, -11, 10, chunkBox);
            this.generateBox(world, chunkBox, 9, -13, 9, 11, -13, 11, Blocks.TNT.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 8, -11, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 8, -10, 10, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 7, -10, 10, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 7, -11, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 12, -11, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 12, -10, 10, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 13, -10, 10, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 13, -11, 10, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 10, -11, 8, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 10, -10, 8, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, -10, 7, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, -11, 7, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 10, -11, 12, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 10, -10, 12, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, -10, 13, chunkBox);
            this.placeBlock(world, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, -11, 13, chunkBox);

            for(Direction direction : Direction.Plane.HORIZONTAL) {
                if (!this.hasPlacedChest[direction.get2DDataValue()]) {
                    int p = direction.getStepX() * 2;
                    int q = direction.getStepZ() * 2;
                    this.hasPlacedChest[direction.get2DDataValue()] = this.createChest(world, chunkBox, random, 10 + p, -11, 10 + q, BuiltInLootTables.DESERT_PYRAMID);
                }
            }

        }
    }
}
