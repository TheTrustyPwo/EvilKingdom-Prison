package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class JunglePyramidPiece extends ScatteredFeaturePiece {
    public static final int WIDTH = 12;
    public static final int DEPTH = 15;
    private boolean placedMainChest;
    private boolean placedHiddenChest;
    private boolean placedTrap1;
    private boolean placedTrap2;
    private static final JunglePyramidPiece.MossStoneSelector STONE_SELECTOR = new JunglePyramidPiece.MossStoneSelector();

    public JunglePyramidPiece(Random random, int x, int z) {
        super(StructurePieceType.JUNGLE_PYRAMID_PIECE, x, 64, z, 12, 10, 15, getRandomHorizontalDirection(random));
    }

    public JunglePyramidPiece(CompoundTag nbt) {
        super(StructurePieceType.JUNGLE_PYRAMID_PIECE, nbt);
        this.placedMainChest = nbt.getBoolean("placedMainChest");
        this.placedHiddenChest = nbt.getBoolean("placedHiddenChest");
        this.placedTrap1 = nbt.getBoolean("placedTrap1");
        this.placedTrap2 = nbt.getBoolean("placedTrap2");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        super.addAdditionalSaveData(context, nbt);
        nbt.putBoolean("placedMainChest", this.placedMainChest);
        nbt.putBoolean("placedHiddenChest", this.placedHiddenChest);
        nbt.putBoolean("placedTrap1", this.placedTrap1);
        nbt.putBoolean("placedTrap2", this.placedTrap2);
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        if (this.updateAverageGroundHeight(world, chunkBox, 0)) {
            this.generateBox(world, chunkBox, 0, -4, 0, this.width - 1, 0, this.depth - 1, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 2, 1, 2, 9, 2, 2, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 2, 1, 12, 9, 2, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 2, 1, 3, 2, 2, 11, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 9, 1, 3, 9, 2, 11, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 1, 3, 1, 10, 6, 1, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 1, 3, 13, 10, 6, 13, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 1, 3, 2, 1, 6, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 10, 3, 2, 10, 6, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 2, 3, 2, 9, 3, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 2, 6, 2, 9, 6, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 3, 7, 3, 8, 7, 11, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 8, 4, 7, 8, 10, false, random, STONE_SELECTOR);
            this.generateAirBox(world, chunkBox, 3, 1, 3, 8, 2, 11);
            this.generateAirBox(world, chunkBox, 4, 3, 6, 7, 3, 9);
            this.generateAirBox(world, chunkBox, 2, 4, 2, 9, 5, 12);
            this.generateAirBox(world, chunkBox, 4, 6, 5, 7, 6, 9);
            this.generateAirBox(world, chunkBox, 5, 7, 6, 6, 7, 8);
            this.generateAirBox(world, chunkBox, 5, 1, 2, 6, 2, 2);
            this.generateAirBox(world, chunkBox, 5, 2, 12, 6, 2, 12);
            this.generateAirBox(world, chunkBox, 5, 5, 1, 6, 5, 1);
            this.generateAirBox(world, chunkBox, 5, 5, 13, 6, 5, 13);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 1, 5, 5, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 10, 5, 5, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 1, 5, 9, chunkBox);
            this.placeBlock(world, Blocks.AIR.defaultBlockState(), 10, 5, 9, chunkBox);

            for(int i = 0; i <= 14; i += 14) {
                this.generateBox(world, chunkBox, 2, 4, i, 2, 5, i, false, random, STONE_SELECTOR);
                this.generateBox(world, chunkBox, 4, 4, i, 4, 5, i, false, random, STONE_SELECTOR);
                this.generateBox(world, chunkBox, 7, 4, i, 7, 5, i, false, random, STONE_SELECTOR);
                this.generateBox(world, chunkBox, 9, 4, i, 9, 5, i, false, random, STONE_SELECTOR);
            }

            this.generateBox(world, chunkBox, 5, 6, 0, 6, 6, 0, false, random, STONE_SELECTOR);

            for(int j = 0; j <= 11; j += 11) {
                for(int k = 2; k <= 12; k += 2) {
                    this.generateBox(world, chunkBox, j, 4, k, j, 5, k, false, random, STONE_SELECTOR);
                }

                this.generateBox(world, chunkBox, j, 6, 5, j, 6, 5, false, random, STONE_SELECTOR);
                this.generateBox(world, chunkBox, j, 6, 9, j, 6, 9, false, random, STONE_SELECTOR);
            }

            this.generateBox(world, chunkBox, 2, 7, 2, 2, 9, 2, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 9, 7, 2, 9, 9, 2, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 2, 7, 12, 2, 9, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 9, 7, 12, 9, 9, 12, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 9, 4, 4, 9, 4, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 7, 9, 4, 7, 9, 4, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 9, 10, 4, 9, 10, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 7, 9, 10, 7, 9, 10, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 5, 9, 7, 6, 9, 7, false, random, STONE_SELECTOR);
            BlockState blockState = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
            BlockState blockState2 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
            BlockState blockState3 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            BlockState blockState4 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            this.placeBlock(world, blockState4, 5, 9, 6, chunkBox);
            this.placeBlock(world, blockState4, 6, 9, 6, chunkBox);
            this.placeBlock(world, blockState3, 5, 9, 8, chunkBox);
            this.placeBlock(world, blockState3, 6, 9, 8, chunkBox);
            this.placeBlock(world, blockState4, 4, 0, 0, chunkBox);
            this.placeBlock(world, blockState4, 5, 0, 0, chunkBox);
            this.placeBlock(world, blockState4, 6, 0, 0, chunkBox);
            this.placeBlock(world, blockState4, 7, 0, 0, chunkBox);
            this.placeBlock(world, blockState4, 4, 1, 8, chunkBox);
            this.placeBlock(world, blockState4, 4, 2, 9, chunkBox);
            this.placeBlock(world, blockState4, 4, 3, 10, chunkBox);
            this.placeBlock(world, blockState4, 7, 1, 8, chunkBox);
            this.placeBlock(world, blockState4, 7, 2, 9, chunkBox);
            this.placeBlock(world, blockState4, 7, 3, 10, chunkBox);
            this.generateBox(world, chunkBox, 4, 1, 9, 4, 1, 9, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 7, 1, 9, 7, 1, 9, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 1, 10, 7, 2, 10, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 5, 4, 5, 6, 4, 5, false, random, STONE_SELECTOR);
            this.placeBlock(world, blockState, 4, 4, 5, chunkBox);
            this.placeBlock(world, blockState2, 7, 4, 5, chunkBox);

            for(int l = 0; l < 4; ++l) {
                this.placeBlock(world, blockState3, 5, 0 - l, 6 + l, chunkBox);
                this.placeBlock(world, blockState3, 6, 0 - l, 6 + l, chunkBox);
                this.generateAirBox(world, chunkBox, 5, 0 - l, 7 + l, 6, 0 - l, 9 + l);
            }

            this.generateAirBox(world, chunkBox, 1, -3, 12, 10, -1, 13);
            this.generateAirBox(world, chunkBox, 1, -3, 1, 3, -1, 13);
            this.generateAirBox(world, chunkBox, 1, -3, 1, 9, -1, 5);

            for(int m = 1; m <= 13; m += 2) {
                this.generateBox(world, chunkBox, 1, -3, m, 1, -2, m, false, random, STONE_SELECTOR);
            }

            for(int n = 2; n <= 12; n += 2) {
                this.generateBox(world, chunkBox, 1, -1, n, 3, -1, n, false, random, STONE_SELECTOR);
            }

            this.generateBox(world, chunkBox, 2, -2, 1, 5, -2, 1, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 7, -2, 1, 9, -2, 1, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 6, -3, 1, 6, -3, 1, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 6, -1, 1, 6, -1, 1, false, random, STONE_SELECTOR);
            this.placeBlock(world, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.EAST).setValue(TripWireHookBlock.ATTACHED, Boolean.valueOf(true)), 1, -3, 8, chunkBox);
            this.placeBlock(world, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.WEST).setValue(TripWireHookBlock.ATTACHED, Boolean.valueOf(true)), 4, -3, 8, chunkBox);
            this.placeBlock(world, Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.EAST, Boolean.valueOf(true)).setValue(TripWireBlock.WEST, Boolean.valueOf(true)).setValue(TripWireBlock.ATTACHED, Boolean.valueOf(true)), 2, -3, 8, chunkBox);
            this.placeBlock(world, Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.EAST, Boolean.valueOf(true)).setValue(TripWireBlock.WEST, Boolean.valueOf(true)).setValue(TripWireBlock.ATTACHED, Boolean.valueOf(true)), 3, -3, 8, chunkBox);
            BlockState blockState5 = Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE);
            this.placeBlock(world, blockState5, 5, -3, 7, chunkBox);
            this.placeBlock(world, blockState5, 5, -3, 6, chunkBox);
            this.placeBlock(world, blockState5, 5, -3, 5, chunkBox);
            this.placeBlock(world, blockState5, 5, -3, 4, chunkBox);
            this.placeBlock(world, blockState5, 5, -3, 3, chunkBox);
            this.placeBlock(world, blockState5, 5, -3, 2, chunkBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE), 5, -3, 1, chunkBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE), 4, -3, 1, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3, -3, 1, chunkBox);
            if (!this.placedTrap1) {
                this.placedTrap1 = this.createDispenser(world, chunkBox, random, 3, -2, 1, Direction.NORTH, BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER);
            }

            this.placeBlock(world, Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, Boolean.valueOf(true)), 3, -2, 2, chunkBox);
            this.placeBlock(world, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.NORTH).setValue(TripWireHookBlock.ATTACHED, Boolean.valueOf(true)), 7, -3, 1, chunkBox);
            this.placeBlock(world, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.SOUTH).setValue(TripWireHookBlock.ATTACHED, Boolean.valueOf(true)), 7, -3, 5, chunkBox);
            this.placeBlock(world, Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, Boolean.valueOf(true)).setValue(TripWireBlock.SOUTH, Boolean.valueOf(true)).setValue(TripWireBlock.ATTACHED, Boolean.valueOf(true)), 7, -3, 2, chunkBox);
            this.placeBlock(world, Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, Boolean.valueOf(true)).setValue(TripWireBlock.SOUTH, Boolean.valueOf(true)).setValue(TripWireBlock.ATTACHED, Boolean.valueOf(true)), 7, -3, 3, chunkBox);
            this.placeBlock(world, Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, Boolean.valueOf(true)).setValue(TripWireBlock.SOUTH, Boolean.valueOf(true)).setValue(TripWireBlock.ATTACHED, Boolean.valueOf(true)), 7, -3, 4, chunkBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE), 8, -3, 6, chunkBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE), 9, -3, 6, chunkBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.UP), 9, -3, 5, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 9, -3, 4, chunkBox);
            this.placeBlock(world, blockState5, 9, -2, 4, chunkBox);
            if (!this.placedTrap2) {
                this.placedTrap2 = this.createDispenser(world, chunkBox, random, 9, -2, 3, Direction.WEST, BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER);
            }

            this.placeBlock(world, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, Boolean.valueOf(true)), 8, -1, 3, chunkBox);
            this.placeBlock(world, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, Boolean.valueOf(true)), 8, -2, 3, chunkBox);
            if (!this.placedMainChest) {
                this.placedMainChest = this.createChest(world, chunkBox, random, 8, -3, 3, BuiltInLootTables.JUNGLE_TEMPLE);
            }

            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 9, -3, 2, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8, -3, 1, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 4, -3, 5, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 5, -2, 5, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 5, -1, 5, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 6, -3, 5, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 7, -2, 5, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 7, -1, 5, chunkBox);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8, -3, 5, chunkBox);
            this.generateBox(world, chunkBox, 9, -1, 1, 9, -1, 5, false, random, STONE_SELECTOR);
            this.generateAirBox(world, chunkBox, 8, -3, 8, 10, -1, 10);
            this.placeBlock(world, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 8, -2, 11, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 9, -2, 11, chunkBox);
            this.placeBlock(world, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 10, -2, 11, chunkBox);
            BlockState blockState6 = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACING, Direction.NORTH).setValue(LeverBlock.FACE, AttachFace.WALL);
            this.placeBlock(world, blockState6, 8, -2, 12, chunkBox);
            this.placeBlock(world, blockState6, 9, -2, 12, chunkBox);
            this.placeBlock(world, blockState6, 10, -2, 12, chunkBox);
            this.generateBox(world, chunkBox, 8, -3, 8, 8, -3, 10, false, random, STONE_SELECTOR);
            this.generateBox(world, chunkBox, 10, -3, 8, 10, -3, 10, false, random, STONE_SELECTOR);
            this.placeBlock(world, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 10, -2, 9, chunkBox);
            this.placeBlock(world, blockState5, 8, -2, 9, chunkBox);
            this.placeBlock(world, blockState5, 8, -2, 10, chunkBox);
            this.placeBlock(world, Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE), 10, -1, 9, chunkBox);
            this.placeBlock(world, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 9, -2, 8, chunkBox);
            this.placeBlock(world, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), 10, -2, 8, chunkBox);
            this.placeBlock(world, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), 10, -1, 8, chunkBox);
            this.placeBlock(world, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH), 10, -2, 10, chunkBox);
            if (!this.placedHiddenChest) {
                this.placedHiddenChest = this.createChest(world, chunkBox, random, 9, -3, 10, BuiltInLootTables.JUNGLE_TEMPLE);
            }

        }
    }

    static class MossStoneSelector extends StructurePiece.BlockSelector {
        @Override
        public void next(Random random, int x, int y, int z, boolean placeBlock) {
            if (random.nextFloat() < 0.4F) {
                this.next = Blocks.COBBLESTONE.defaultBlockState();
            } else {
                this.next = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            }

        }
    }
}
