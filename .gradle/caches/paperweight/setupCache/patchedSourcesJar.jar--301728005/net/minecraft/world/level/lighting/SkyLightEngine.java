package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public final class SkyLightEngine extends LayerLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, SkyLightSectionStorage> {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private final MutableInt mutableInt = new MutableInt(); // Paper

    public SkyLightEngine(LightChunkGetter chunkProvider) {
        super(chunkProvider, LightLayer.SKY, new SkyLightSectionStorage(chunkProvider));
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        if (targetId != Long.MAX_VALUE && sourceId != Long.MAX_VALUE) {
            if (level >= 15) {
                return level;
            } else {
                //MutableInt mutableint = new MutableInt(); // Paper - share mutableint, single threaded
                BlockState blockState = this.getStateAndOpacity(targetId, mutableInt);
                if (mutableInt.getValue() >= 15) {
                    return 15;
                } else {
                    int i = BlockPos.getX(sourceId);
                    int j = BlockPos.getY(sourceId);
                    int k = BlockPos.getZ(sourceId);
                    int l = BlockPos.getX(targetId);
                    int m = BlockPos.getY(targetId);
                    int n = BlockPos.getZ(targetId);
                    int o = Integer.signum(l - i);
                    int p = Integer.signum(m - j);
                    int q = Integer.signum(n - k);
                    Direction direction = Direction.fromNormal(o, p, q);
                    if (direction == null) {
                        throw new IllegalStateException(String.format("Light was spread in illegal direction %d, %d, %d", o, p, q));
                    } else {
                        BlockState blockState2 = this.getStateAndOpacity(sourceId, (MutableInt)null);
                        VoxelShape voxelShape = this.getShape(blockState2, sourceId, direction);
                        VoxelShape voxelShape2 = this.getShape(blockState, targetId, direction.getOpposite());
                        if (Shapes.faceShapeOccludes(voxelShape, voxelShape2)) {
                            return 15;
                        } else {
                            boolean bl = i == l && k == n;
                            boolean bl2 = bl && j > m;
                            return bl2 && level == 0 && mutableInt.getValue() == 0 ? 0 : level + Math.max(1, mutableInt.getValue());
                        }
                    }
                }
            }
        } else {
            return 15;
        }
    }

    @Override
    protected void checkNeighborsAfterUpdate(long id, int level, boolean decrease) {
        long l = SectionPos.blockToSection(id);
        int i = BlockPos.getY(id);
        int j = SectionPos.sectionRelative(i);
        int k = SectionPos.blockToSectionCoord(i);
        int m;
        if (j != 0) {
            m = 0;
        } else {
            int n;
            for(n = 0; !this.storage.storingLightForSection(SectionPos.offset(l, 0, -n - 1, 0)) && this.storage.hasSectionsBelow(k - n - 1); ++n) {
            }

            m = n;
        }

        long p = BlockPos.offset(id, 0, -1 - m * 16, 0);
        long q = SectionPos.blockToSection(p);
        if (l == q || this.storage.storingLightForSection(q)) {
            this.checkNeighbor(id, p, level, decrease);
        }

        long r = BlockPos.offset(id, Direction.UP);
        long s = SectionPos.blockToSection(r);
        if (l == s || this.storage.storingLightForSection(s)) {
            this.checkNeighbor(id, r, level, decrease);
        }

        for(Direction direction : HORIZONTALS) {
            int t = 0;

            while(true) {
                long u = BlockPos.offset(id, direction.getStepX(), -t, direction.getStepZ());
                long v = SectionPos.blockToSection(u);
                if (l == v) {
                    this.checkNeighbor(id, u, level, decrease);
                    break;
                }

                if (this.storage.storingLightForSection(v)) {
                    long w = BlockPos.offset(id, 0, -t, 0);
                    this.checkNeighbor(w, u, level, decrease);
                }

                ++t;
                if (t > m * 16) {
                    break;
                }
            }
        }

    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        int i = maxLevel;
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

                int j;
                if (dataLayer2 != null) {
                    j = this.getLevel(dataLayer2, m);
                } else {
                    if (direction == Direction.DOWN) {
                        continue;
                    }

                    j = 15 - this.storage.getLightValue(m, true);
                }

                int o = this.computeLevelFromNeighbor(m, id, j);
                if (i > o) {
                    i = o;
                }

                if (i == 0) {
                    return i;
                }
            }
        }

        return i;
    }

    @Override
    protected void checkNode(long id) {
        this.storage.runAllUpdates();
        long l = SectionPos.blockToSection(id);
        if (this.storage.storingLightForSection(l)) {
            super.checkNode(id);
        } else {
            for(id = BlockPos.getFlatIndex(id); !this.storage.storingLightForSection(l) && !this.storage.isAboveData(l); id = BlockPos.offset(id, 0, 16, 0)) {
                l = SectionPos.offset(l, Direction.UP);
            }

            if (this.storage.storingLightForSection(l)) {
                super.checkNode(id);
            }
        }

    }

    @Override
    public String getDebugData(long sectionPos) {
        return super.getDebugData(sectionPos) + (this.storage.isAboveData(sectionPos) ? "*" : "");
    }
}
