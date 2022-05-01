package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public final class BlockLightEngine extends LayerLightEngine<BlockLightSectionStorage.BlockDataLayerStorageMap, BlockLightSectionStorage> {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final MutableInt mutableInt = new MutableInt(); // Paper

    public BlockLightEngine(LightChunkGetter chunkProvider) {
        super(chunkProvider, LightLayer.BLOCK, new BlockLightSectionStorage(chunkProvider));
    }

    private int getLightEmission(long blockPos) {
        int i = BlockPos.getX(blockPos);
        int j = BlockPos.getY(blockPos);
        int k = BlockPos.getZ(blockPos);
        BlockGetter blockGetter = this.chunkSource.getChunkForLighting(SectionPos.blockToSectionCoord(i), SectionPos.blockToSectionCoord(k));
        return blockGetter != null ? blockGetter.getLightEmission(this.pos.set(i, j, k)) : 0;
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        if (targetId == Long.MAX_VALUE) {
            return 15;
        } else if (sourceId == Long.MAX_VALUE) {
            return level + 15 - this.getLightEmission(targetId);
        } else if (level >= 15) {
            return level;
        } else {
            int i = Integer.signum(BlockPos.getX(targetId) - BlockPos.getX(sourceId));
            int j = Integer.signum(BlockPos.getY(targetId) - BlockPos.getY(sourceId));
            int k = Integer.signum(BlockPos.getZ(targetId) - BlockPos.getZ(sourceId));
            Direction direction = Direction.fromNormal(i, j, k);
            if (direction == null) {
                return 15;
            } else {
                //MutableInt mutableint = new MutableInt(); // Paper - share mutableint, single threaded
                BlockState blockState = this.getStateAndOpacity(targetId, mutableInt);
                if (mutableInt.getValue() >= 15) {
                    return 15;
                } else {
                    BlockState blockState2 = this.getStateAndOpacity(sourceId, (MutableInt)null);
                    VoxelShape voxelShape = this.getShape(blockState2, sourceId, direction);
                    VoxelShape voxelShape2 = this.getShape(blockState, targetId, direction.getOpposite());
                    return Shapes.faceShapeOccludes(voxelShape, voxelShape2) ? 15 : level + Math.max(1, mutableInt.getValue());
                }
            }
        }
    }

    @Override
    protected void checkNeighborsAfterUpdate(long id, int level, boolean decrease) {
        long l = SectionPos.blockToSection(id);

        for(Direction direction : DIRECTIONS) {
            long m = BlockPos.offset(id, direction);
            long n = SectionPos.blockToSection(m);
            if (l == n || this.storage.storingLightForSection(n)) {
                this.checkNeighbor(id, m, level, decrease);
            }
        }

    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        int i = maxLevel;
        if (Long.MAX_VALUE != excludedId) {
            int j = this.computeLevelFromNeighbor(Long.MAX_VALUE, id, 0);
            if (maxLevel > j) {
                i = j;
            }

            if (i == 0) {
                return i;
            }
        }

        long l = SectionPos.blockToSection(id);
        DataLayer dataLayer = this.storage.getDataLayer(l, true);

        for(Direction direction : DIRECTIONS) {
            long m = BlockPos.offset(id, direction);
            if (m != excludedId) {
                long n = SectionPos.blockToSection(m);
                DataLayer dataLayer2;
                if (l == n) {
                    dataLayer2 = dataLayer;
                } else {
                    dataLayer2 = this.storage.getDataLayer(n, true);
                }

                if (dataLayer2 != null) {
                    int k = this.computeLevelFromNeighbor(m, id, this.getLevel(dataLayer2, m));
                    if (i > k) {
                        i = k;
                    }

                    if (i == 0) {
                        return i;
                    }
                }
            }
        }

        return i;
    }

    @Override
    public void onBlockEmissionIncrease(BlockPos pos, int level) {
        this.storage.runAllUpdates();
        this.checkEdge(Long.MAX_VALUE, pos.asLong(), 15 - level, true);
    }
}
