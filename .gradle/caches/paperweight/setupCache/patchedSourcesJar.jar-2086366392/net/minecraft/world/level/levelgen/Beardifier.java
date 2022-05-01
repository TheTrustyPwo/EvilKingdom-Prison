package net.minecraft.world.level.levelgen;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.NoiseEffect;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Beardifier implements DensityFunctions.BeardifierOrMarker {
    public static final int BEARD_KERNEL_RADIUS = 12;
    private static final int BEARD_KERNEL_SIZE = 24;
    private static final float[] BEARD_KERNEL = Util.make(new float[13824], (array) -> {
        for(int i = 0; i < 24; ++i) {
            for(int j = 0; j < 24; ++j) {
                for(int k = 0; k < 24; ++k) {
                    array[i * 24 * 24 + j * 24 + k] = (float)computeBeardContribution(j - 12, k - 12, i - 12);
                }
            }
        }

    });
    private final ObjectList<StructurePiece> rigids;
    private final ObjectList<JigsawJunction> junctions;
    private final ObjectListIterator<StructurePiece> pieceIterator;
    private final ObjectListIterator<JigsawJunction> junctionIterator;

    protected Beardifier(StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        this.junctions = new ObjectArrayList<>(32);
        this.rigids = new ObjectArrayList<>(10);
        // Paper start - replace for each
        for (StructureStart start : structureAccessor.startsForFeature(SectionPos.bottomOf(chunk), (configuredStructureFeature) -> {
            return configuredStructureFeature.adaptNoise;
        })) { // Paper end
            for(StructurePiece structurePiece : start.getPieces()) {
                if (structurePiece.isCloseToChunk(chunkPos, 12)) {
                    if (structurePiece instanceof PoolElementStructurePiece) {
                        PoolElementStructurePiece poolElementStructurePiece = (PoolElementStructurePiece)structurePiece;
                        StructureTemplatePool.Projection projection = poolElementStructurePiece.getElement().getProjection();
                        if (projection == StructureTemplatePool.Projection.RIGID) {
                            this.rigids.add(poolElementStructurePiece);
                        }

                        for(JigsawJunction jigsawJunction : poolElementStructurePiece.getJunctions()) {
                            int k = jigsawJunction.getSourceX();
                            int l = jigsawJunction.getSourceZ();
                            if (k > i - 12 && l > j - 12 && k < i + 15 + 12 && l < j + 15 + 12) {
                                this.junctions.add(jigsawJunction);
                            }
                        }
                    } else {
                        this.rigids.add(structurePiece);
                    }
                }
            }

        } // Paper
        this.pieceIterator = this.rigids.iterator();
        this.junctionIterator = this.junctions.iterator();
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        int i = pos.blockX();
        int j = pos.blockY();
        int k = pos.blockZ();
        double d = 0.0D;

        while(this.pieceIterator.hasNext()) {
            StructurePiece structurePiece = this.pieceIterator.next();
            BoundingBox boundingBox = structurePiece.getBoundingBox();
            int l = Math.max(0, Math.max(boundingBox.minX() - i, i - boundingBox.maxX()));
            int m = j - (boundingBox.minY() + (structurePiece instanceof PoolElementStructurePiece ? ((PoolElementStructurePiece)structurePiece).getGroundLevelDelta() : 0));
            int n = Math.max(0, Math.max(boundingBox.minZ() - k, k - boundingBox.maxZ()));
            NoiseEffect noiseEffect = structurePiece.getNoiseEffect();
            if (noiseEffect == NoiseEffect.BURY) {
                d += getBuryContribution(l, m, n);
            } else if (noiseEffect == NoiseEffect.BEARD) {
                d += getBeardContribution(l, m, n) * 0.8D;
            }
        }

        this.pieceIterator.back(this.rigids.size());

        while(this.junctionIterator.hasNext()) {
            JigsawJunction jigsawJunction = this.junctionIterator.next();
            int o = i - jigsawJunction.getSourceX();
            int p = j - jigsawJunction.getSourceGroundY();
            int q = k - jigsawJunction.getSourceZ();
            d += getBeardContribution(o, p, q) * 0.4D;
        }

        this.junctionIterator.back(this.junctions.size());
        return d;
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    private static double getBuryContribution(int x, int y, int z) {
        double d = Mth.length((double)x, (double)y / 2.0D, (double)z);
        return Mth.clampedMap(d, 0.0D, 6.0D, 1.0D, 0.0D);
    }

    private static double getBeardContribution(int x, int y, int z) {
        int i = x + 12;
        int j = y + 12;
        int k = z + 12;
        if (i >= 0 && i < 24) {
            if (j >= 0 && j < 24) {
                return k >= 0 && k < 24 ? (double)BEARD_KERNEL[k * 24 * 24 + i * 24 + j] : 0.0D;
            } else {
                return 0.0D;
            }
        } else {
            return 0.0D;
        }
    }

    private static double computeBeardContribution(int x, int y, int z) {
        double d = (double)(x * x + z * z);
        double e = (double)y + 0.5D;
        double f = e * e;
        double g = Math.pow(Math.E, -(f / 16.0D + d / 16.0D));
        double h = -e * Mth.fastInvSqrt(f / 2.0D + d / 2.0D) / 2.0D;
        return h * g;
    }
}
