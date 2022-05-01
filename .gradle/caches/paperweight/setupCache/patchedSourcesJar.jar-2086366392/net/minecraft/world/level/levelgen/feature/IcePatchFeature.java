package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class IcePatchFeature extends BaseDiskFeature {
    public IcePatchFeature(Codec<DiskConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DiskConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        Random random = context.random();
        DiskConfiguration diskConfiguration = context.config();

        BlockPos blockPos;
        for(blockPos = context.origin(); worldGenLevel.isEmptyBlock(blockPos) && blockPos.getY() > worldGenLevel.getMinBuildHeight() + 2; blockPos = blockPos.below()) {
        }

        return !worldGenLevel.getBlockState(blockPos).is(Blocks.SNOW_BLOCK) ? false : super.place(new FeaturePlaceContext<>(context.topFeature(), worldGenLevel, context.chunkGenerator(), context.random(), blockPos, context.config()));
    }
}
