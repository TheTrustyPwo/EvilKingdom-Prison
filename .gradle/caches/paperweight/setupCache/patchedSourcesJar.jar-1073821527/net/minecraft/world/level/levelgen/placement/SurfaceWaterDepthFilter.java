package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceWaterDepthFilter extends PlacementFilter {
    public static final Codec<SurfaceWaterDepthFilter> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("max_water_depth").forGetter((surfaceWaterDepthFilter) -> {
            return surfaceWaterDepthFilter.maxWaterDepth;
        })).apply(instance, SurfaceWaterDepthFilter::new);
    });
    private final int maxWaterDepth;

    private SurfaceWaterDepthFilter(int maxWaterDepth) {
        this.maxWaterDepth = maxWaterDepth;
    }

    public static SurfaceWaterDepthFilter forMaxDepth(int maxWaterDepth) {
        return new SurfaceWaterDepthFilter(maxWaterDepth);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, Random random, BlockPos pos) {
        int i = context.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ());
        int j = context.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        return j - i <= this.maxWaterDepth;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.SURFACE_WATER_DEPTH_FILTER;
    }
}
