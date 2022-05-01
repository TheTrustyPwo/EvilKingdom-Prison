package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class OreVeinifier {
    private static final float VEININESS_THRESHOLD = 0.4F;
    private static final int EDGE_ROUNDOFF_BEGIN = 20;
    private static final double MAX_EDGE_ROUNDOFF = 0.2D;
    private static final float VEIN_SOLIDNESS = 0.7F;
    private static final float MIN_RICHNESS = 0.1F;
    private static final float MAX_RICHNESS = 0.3F;
    private static final float MAX_RICHNESS_THRESHOLD = 0.6F;
    private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;
    private static final float SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.3F;

    private OreVeinifier() {
    }

    protected static NoiseChunk.BlockStateFiller create(DensityFunction veinToggle, DensityFunction veinRidged, DensityFunction veinGap, PositionalRandomFactory randomDeriver) {
        BlockState blockState = null;
        return (pos) -> {
            double d = veinToggle.compute(pos);
            int i = pos.blockY();
            OreVeinifier.VeinType veinType = d > 0.0D ? OreVeinifier.VeinType.COPPER : OreVeinifier.VeinType.IRON;
            double e = Math.abs(d);
            int j = veinType.maxY - i;
            int k = i - veinType.minY;
            if (k >= 0 && j >= 0) {
                int l = Math.min(j, k);
                double f = Mth.clampedMap((double)l, 0.0D, 20.0D, -0.2D, 0.0D);
                if (e + f < (double)0.4F) {
                    return blockState;
                } else {
                    RandomSource randomSource = randomDeriver.at(pos.blockX(), i, pos.blockZ());
                    if (randomSource.nextFloat() > 0.7F) {
                        return blockState;
                    } else if (veinRidged.compute(pos) >= 0.0D) {
                        return blockState;
                    } else {
                        double g = Mth.clampedMap(e, (double)0.4F, (double)0.6F, (double)0.1F, (double)0.3F);
                        if ((double)randomSource.nextFloat() < g && veinGap.compute(pos) > (double)-0.3F) {
                            return randomSource.nextFloat() < 0.02F ? veinType.rawOreBlock : veinType.ore;
                        } else {
                            return veinType.filler;
                        }
                    }
                }
            } else {
                return blockState;
            }
        };
    }

    protected static enum VeinType {
        COPPER(Blocks.COPPER_ORE.defaultBlockState(), Blocks.RAW_COPPER_BLOCK.defaultBlockState(), Blocks.GRANITE.defaultBlockState(), 0, 50),
        IRON(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), Blocks.RAW_IRON_BLOCK.defaultBlockState(), Blocks.TUFF.defaultBlockState(), -60, -8);

        final BlockState ore;
        final BlockState rawOreBlock;
        final BlockState filler;
        protected final int minY;
        protected final int maxY;

        private VeinType(BlockState ore, BlockState rawOreBlock, BlockState stone, int minY, int maxY) {
            this.ore = ore;
            this.rawOreBlock = rawOreBlock;
            this.filler = stone;
            this.minY = minY;
            this.maxY = maxY;
        }
    }
}
