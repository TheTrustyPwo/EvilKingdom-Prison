package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BuriedTreasurePieces {
    public static class BuriedTreasurePiece extends StructurePiece {
        public BuriedTreasurePiece(BlockPos pos) {
            super(StructurePieceType.BURIED_TREASURE_PIECE, 0, new BoundingBox(pos));
        }

        public BuriedTreasurePiece(CompoundTag nbt) {
            super(StructurePieceType.BURIED_TREASURE_PIECE, nbt);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            int i = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.boundingBox.minX(), this.boundingBox.minZ());
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(this.boundingBox.minX(), i, this.boundingBox.minZ());

            while(mutableBlockPos.getY() > world.getMinBuildHeight()) {
                BlockState blockState = world.getBlockState(mutableBlockPos);
                BlockState blockState2 = world.getBlockState(mutableBlockPos.below());
                if (blockState2 == Blocks.SANDSTONE.defaultBlockState() || blockState2 == Blocks.STONE.defaultBlockState() || blockState2 == Blocks.ANDESITE.defaultBlockState() || blockState2 == Blocks.GRANITE.defaultBlockState() || blockState2 == Blocks.DIORITE.defaultBlockState()) {
                    BlockState blockState3 = !blockState.isAir() && !this.isLiquid(blockState) ? blockState : Blocks.SAND.defaultBlockState();

                    for(Direction direction : Direction.values()) {
                        BlockPos blockPos = mutableBlockPos.relative(direction);
                        BlockState blockState4 = world.getBlockState(blockPos);
                        if (blockState4.isAir() || this.isLiquid(blockState4)) {
                            BlockPos blockPos2 = blockPos.below();
                            BlockState blockState5 = world.getBlockState(blockPos2);
                            if ((blockState5.isAir() || this.isLiquid(blockState5)) && direction != Direction.UP) {
                                world.setBlock(blockPos, blockState2, 3);
                            } else {
                                world.setBlock(blockPos, blockState3, 3);
                            }
                        }
                    }

                    this.boundingBox = new BoundingBox(mutableBlockPos);
                    this.createChest(world, chunkBox, random, mutableBlockPos, BuiltInLootTables.BURIED_TREASURE, (BlockState)null);
                    return;
                }

                mutableBlockPos.move(0, -1, 0);
            }

        }

        private boolean isLiquid(BlockState state) {
            return state == Blocks.WATER.defaultBlockState() || state == Blocks.LAVA.defaultBlockState();
        }
    }
}
