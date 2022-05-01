package net.minecraft.server.level;

import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class PlayerRespawnLogic {
    @Nullable
    protected static BlockPos getOverworldRespawnPos(ServerLevel world, int x, int z) {
        boolean bl = world.dimensionType().hasCeiling();
        LevelChunk levelChunk = world.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        int i = bl ? world.getChunkSource().getGenerator().getSpawnHeight(world) : levelChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
        if (i < world.getMinBuildHeight()) {
            return null;
        } else {
            int j = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
            if (j <= i && j > levelChunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15)) {
                return null;
            } else {
                BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

                for(int k = i + 1; k >= world.getMinBuildHeight(); --k) {
                    mutableBlockPos.set(x, k, z);
                    BlockState blockState = world.getBlockState(mutableBlockPos);
                    if (!blockState.getFluidState().isEmpty()) {
                        break;
                    }

                    if (Block.isFaceFull(blockState.getCollisionShape(world, mutableBlockPos), Direction.UP)) {
                        return mutableBlockPos.above().immutable();
                    }
                }

                return null;
            }
        }
    }

    @Nullable
    public static BlockPos getSpawnPosInChunk(ServerLevel world, ChunkPos chunkPos) {
        if (SharedConstants.debugVoidTerrain(chunkPos)) {
            return null;
        } else {
            for(int i = chunkPos.getMinBlockX(); i <= chunkPos.getMaxBlockX(); ++i) {
                for(int j = chunkPos.getMinBlockZ(); j <= chunkPos.getMaxBlockZ(); ++j) {
                    BlockPos blockPos = getOverworldRespawnPos(world, i, j);
                    if (blockPos != null) {
                        return blockPos;
                    }
                }
            }

            return null;
        }
    }
}
