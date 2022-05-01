package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

public class HeightmapPlacement extends PlacementModifier {
    public static final Codec<HeightmapPlacement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Heightmap.Types.CODEC.fieldOf("heightmap").forGetter((heightmapPlacement) -> {
            return heightmapPlacement.heightmap;
        })).apply(instance, HeightmapPlacement::new);
    });
    private final Heightmap.Types heightmap;

    private HeightmapPlacement(Heightmap.Types heightmap) {
        this.heightmap = heightmap;
    }

    public static HeightmapPlacement onHeightmap(Heightmap.Types heightmap) {
        return new HeightmapPlacement(heightmap);
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, Random random, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getZ();
        int k = context.getHeight(this.heightmap, i, j);
        return k > context.getMinBuildHeight() ? Stream.of(new BlockPos(i, k, j)) : Stream.of();
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.HEIGHTMAP;
    }
}
