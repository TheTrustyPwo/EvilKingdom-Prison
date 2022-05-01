package net.minecraft.world.level.lighting;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public abstract class LayerLightEngine<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> extends DynamicGraphMinFixedPoint implements LayerLightEventListener {
    public static final long SELF_SOURCE = Long.MAX_VALUE;
    private static final Direction[] DIRECTIONS = Direction.values();
    protected final LightChunkGetter chunkSource;
    protected final LightLayer layer;
    protected final S storage;
    private boolean runningLightUpdates;
    protected final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private static final int CACHE_SIZE = 2;
    private final long[] lastChunkPos = new long[2];
    private final BlockGetter[] lastChunk = new BlockGetter[2];

    public LayerLightEngine(LightChunkGetter chunkProvider, LightLayer type, S lightStorage) {
        super(16, 256, 8192);
        this.chunkSource = chunkProvider;
        this.layer = type;
        this.storage = lightStorage;
        this.clearCache();
    }

    @Override
    protected void checkNode(long id) {
        this.storage.runAllUpdates();
        if (this.storage.storingLightForSection(SectionPos.blockToSection(id))) {
            super.checkNode(id);
        }

    }

    @Nullable
    private BlockGetter getChunk(int chunkX, int chunkZ) {
        long l = ChunkPos.asLong(chunkX, chunkZ);

        for(int i = 0; i < 2; ++i) {
            if (l == this.lastChunkPos[i]) {
                return this.lastChunk[i];
            }
        }

        BlockGetter blockGetter = this.chunkSource.getChunkForLighting(chunkX, chunkZ);

        for(int j = 1; j > 0; --j) {
            this.lastChunkPos[j] = this.lastChunkPos[j - 1];
            this.lastChunk[j] = this.lastChunk[j - 1];
        }

        this.lastChunkPos[0] = l;
        this.lastChunk[0] = blockGetter;
        return blockGetter;
    }

    private void clearCache() {
        Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
        Arrays.fill(this.lastChunk, (Object)null);
    }

    protected BlockState getStateAndOpacity(long pos, @Nullable MutableInt mutableInt) {
        if (pos == Long.MAX_VALUE) {
            if (mutableInt != null) {
                mutableInt.setValue(0);
            }

            return Blocks.AIR.defaultBlockState();
        } else {
            int i = SectionPos.blockToSectionCoord(BlockPos.getX(pos));
            int j = SectionPos.blockToSectionCoord(BlockPos.getZ(pos));
            BlockGetter blockGetter = this.getChunk(i, j);
            if (blockGetter == null) {
                if (mutableInt != null) {
                    mutableInt.setValue(16);
                }

                return Blocks.BEDROCK.defaultBlockState();
            } else {
                this.pos.set(pos);
                BlockState blockState = blockGetter.getBlockState(this.pos);
                boolean bl = blockState.canOcclude() && blockState.useShapeForLightOcclusion();
                if (mutableInt != null) {
                    mutableInt.setValue(blockState.getLightBlock(this.chunkSource.getLevel(), this.pos));
                }

                return bl ? blockState : Blocks.AIR.defaultBlockState();
            }
        }
    }

    protected VoxelShape getShape(BlockState world, long pos, Direction facing) {
        return world.canOcclude() ? world.getFaceOcclusionShape(this.chunkSource.getLevel(), this.pos.set(pos), facing) : Shapes.empty();
    }

    public static int getLightBlockInto(BlockGetter world, BlockState state1, BlockPos pos1, BlockState state2, BlockPos pos2, Direction direction, int opacity2) {
        boolean bl = state1.canOcclude() && state1.useShapeForLightOcclusion();
        boolean bl2 = state2.canOcclude() && state2.useShapeForLightOcclusion();
        if (!bl && !bl2) {
            return opacity2;
        } else {
            VoxelShape voxelShape = bl ? state1.getOcclusionShape(world, pos1) : Shapes.empty();
            VoxelShape voxelShape2 = bl2 ? state2.getOcclusionShape(world, pos2) : Shapes.empty();
            return Shapes.mergedFaceOccludes(voxelShape, voxelShape2, direction) ? 16 : opacity2;
        }
    }

    @Override
    protected boolean isSource(long id) {
        return id == Long.MAX_VALUE;
    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        return 0;
    }

    @Override
    protected int getLevel(long id) {
        return id == Long.MAX_VALUE ? 0 : 15 - this.storage.getStoredLevel(id);
    }

    protected int getLevel(DataLayer section, long blockPos) {
        return 15 - section.get(SectionPos.sectionRelative(BlockPos.getX(blockPos)), SectionPos.sectionRelative(BlockPos.getY(blockPos)), SectionPos.sectionRelative(BlockPos.getZ(blockPos)));
    }

    @Override
    protected void setLevel(long id, int level) {
        this.storage.setStoredLevel(id, Math.min(15, 15 - level));
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        return 0;
    }

    @Override
    public boolean hasLightWork() {
        return this.hasWork() || this.storage.hasWork() || this.storage.hasInconsistencies();
    }

    @Override
    public int runUpdates(int i, boolean doSkylight, boolean skipEdgeLightPropagation) {
        if (!this.runningLightUpdates) {
            if (this.storage.hasWork()) {
                i = this.storage.runUpdates(i);
                if (i == 0) {
                    return i;
                }
            }

            this.storage.markNewInconsistencies(this, doSkylight, skipEdgeLightPropagation);
        }

        this.runningLightUpdates = true;
        if (this.hasWork()) {
            i = this.runUpdates(i);
            this.clearCache();
            if (i == 0) {
                return i;
            }
        }

        this.runningLightUpdates = false;
        this.storage.swapSectionMap();
        return i;
    }

    protected void queueSectionData(long sectionPos, @Nullable DataLayer lightArray, boolean nonEdge) {
        this.storage.queueSectionData(sectionPos, lightArray, nonEdge);
    }

    @Nullable
    @Override
    public DataLayer getDataLayerData(SectionPos pos) {
        return this.storage.getDataLayerData(pos.asLong());
    }

    @Override
    public int getLightValue(BlockPos pos) {
        return this.storage.getLightValue(pos.asLong());
    }

    public String getDebugData(long sectionPos) {
        return "" + this.storage.getLevel(sectionPos);
    }

    @Override
    public void checkBlock(BlockPos pos) {
        long l = pos.asLong();
        this.checkNode(l);

        for(Direction direction : DIRECTIONS) {
            this.checkNode(BlockPos.offset(l, direction));
        }

    }

    @Override
    public void onBlockEmissionIncrease(BlockPos pos, int level) {
    }

    @Override
    public void updateSectionStatus(SectionPos pos, boolean notReady) {
        this.storage.updateSectionStatus(pos.asLong(), notReady);
    }

    @Override
    public void enableLightSources(ChunkPos pos, boolean retainData) {
        long l = SectionPos.getZeroNode(SectionPos.asLong(pos.x, 0, pos.z));
        this.storage.enableLightSources(l, retainData);
    }

    public void retainData(ChunkPos pos, boolean retainData) {
        long l = SectionPos.getZeroNode(SectionPos.asLong(pos.x, 0, pos.z));
        this.storage.retainData(l, retainData);
    }
}
