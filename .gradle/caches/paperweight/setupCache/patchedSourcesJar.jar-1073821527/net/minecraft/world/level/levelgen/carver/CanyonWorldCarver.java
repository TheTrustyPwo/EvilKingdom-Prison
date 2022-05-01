package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CanyonWorldCarver extends WorldCarver<CanyonCarverConfiguration> {
    public CanyonWorldCarver(Codec<CanyonCarverConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean isStartChunk(CanyonCarverConfiguration config, Random random) {
        return random.nextFloat() <= config.probability;
    }

    @Override
    public boolean carve(CarvingContext context, CanyonCarverConfiguration config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> posToBiome, Random random, Aquifer aquiferSampler, ChunkPos pos, CarvingMask mask) {
        int i = (this.getRange() * 2 - 1) * 16;
        double d = (double)pos.getBlockX(random.nextInt(16));
        int j = config.y.sample(random, context);
        double e = (double)pos.getBlockZ(random.nextInt(16));
        float f = random.nextFloat() * ((float)Math.PI * 2F);
        float g = config.verticalRotation.sample(random);
        double h = (double)config.yScale.sample(random);
        float k = config.shape.thickness.sample(random);
        int l = (int)((float)i * config.shape.distanceFactor.sample(random));
        int m = 0;
        this.doCarve(context, config, chunk, posToBiome, random.nextLong(), aquiferSampler, d, (double)j, e, k, f, g, 0, l, h, mask);
        return true;
    }

    private void doCarve(CarvingContext context, CanyonCarverConfiguration config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> posToBiome, long seed, Aquifer aquiferSampler, double x, double y, double z, float width, float yaw, float pitch, int branchStartIndex, int branchCount, double yawPitchRatio, CarvingMask mask) {
        Random random = new Random(seed);
        float[] fs = this.initWidthFactors(context, config, random);
        float f = 0.0F;
        float g = 0.0F;

        for(int i = branchStartIndex; i < branchCount; ++i) {
            double d = 1.5D + (double)(Mth.sin((float)i * (float)Math.PI / (float)branchCount) * width);
            double e = d * yawPitchRatio;
            d *= (double)config.shape.horizontalRadiusFactor.sample(random);
            e = this.updateVerticalRadius(config, random, e, (float)branchCount, (float)i);
            float h = Mth.cos(pitch);
            float j = Mth.sin(pitch);
            x += (double)(Mth.cos(yaw) * h);
            y += (double)j;
            z += (double)(Mth.sin(yaw) * h);
            pitch *= 0.7F;
            pitch += g * 0.05F;
            yaw += f * 0.05F;
            g *= 0.8F;
            f *= 0.5F;
            g += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            f += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (random.nextInt(4) != 0) {
                if (!canReach(chunk.getPos(), x, z, i, branchCount, width)) {
                    return;
                }

                this.carveEllipsoid(context, config, chunk, posToBiome, aquiferSampler, x, y, z, d, e, mask, (contextx, scaledRelativeX, scaledRelativeY, scaledRelativeZ, yx) -> {
                    return this.shouldSkip(contextx, fs, scaledRelativeX, scaledRelativeY, scaledRelativeZ, yx);
                });
            }
        }

    }

    private float[] initWidthFactors(CarvingContext context, CanyonCarverConfiguration config, Random random) {
        int i = context.getGenDepth();
        float[] fs = new float[i];
        float f = 1.0F;

        for(int j = 0; j < i; ++j) {
            if (j == 0 || random.nextInt(config.shape.widthSmoothness) == 0) {
                f = 1.0F + random.nextFloat() * random.nextFloat();
            }

            fs[j] = f * f;
        }

        return fs;
    }

    private double updateVerticalRadius(CanyonCarverConfiguration config, Random random, double pitch, float branchCount, float branchIndex) {
        float f = 1.0F - Mth.abs(0.5F - branchIndex / branchCount) * 2.0F;
        float g = config.shape.verticalRadiusDefaultFactor + config.shape.verticalRadiusCenterFactor * f;
        return (double)g * pitch * (double)Mth.randomBetween(random, 0.75F, 1.0F);
    }

    private boolean shouldSkip(CarvingContext context, float[] horizontalStretchFactors, double scaledRelativeX, double scaledRelativeY, double scaledRelativeZ, int y) {
        int i = y - context.getMinGenY();
        return (scaledRelativeX * scaledRelativeX + scaledRelativeZ * scaledRelativeZ) * (double)horizontalStretchFactors[i - 1] + scaledRelativeY * scaledRelativeY / 6.0D >= 1.0D;
    }
}
