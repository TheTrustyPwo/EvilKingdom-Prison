package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceRelativeThresholdFilter extends PlacementFilter {
    public static final Codec<SurfaceRelativeThresholdFilter> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Heightmap.Types.CODEC.fieldOf("heightmap").forGetter((surfaceRelativeThresholdFilter) -> {
            return surfaceRelativeThresholdFilter.heightmap;
        }), Codec.INT.optionalFieldOf("min_inclusive", Integer.valueOf(Integer.MIN_VALUE)).forGetter((surfaceRelativeThresholdFilter) -> {
            return surfaceRelativeThresholdFilter.minInclusive;
        }), Codec.INT.optionalFieldOf("max_inclusive", Integer.valueOf(Integer.MAX_VALUE)).forGetter((surfaceRelativeThresholdFilter) -> {
            return surfaceRelativeThresholdFilter.maxInclusive;
        })).apply(instance, SurfaceRelativeThresholdFilter::new);
    });
    private final Heightmap.Types heightmap;
    private final int minInclusive;
    private final int maxInclusive;

    private SurfaceRelativeThresholdFilter(Heightmap.Types heightmap, int min, int max) {
        this.heightmap = heightmap;
        this.minInclusive = min;
        this.maxInclusive = max;
    }

    public static SurfaceRelativeThresholdFilter of(Heightmap.Types heightmap, int min, int max) {
        return new SurfaceRelativeThresholdFilter(heightmap, min, max);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, Random random, BlockPos pos) {
        long l = (long)context.getHeight(this.heightmap, pos.getX(), pos.getZ());
        long m = l + (long)this.minInclusive;
        long n = l + (long)this.maxInclusive;
        return m <= (long)pos.getY() && (long)pos.getY() <= n;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.SURFACE_RELATIVE_THRESHOLD_FILTER;
    }
}
