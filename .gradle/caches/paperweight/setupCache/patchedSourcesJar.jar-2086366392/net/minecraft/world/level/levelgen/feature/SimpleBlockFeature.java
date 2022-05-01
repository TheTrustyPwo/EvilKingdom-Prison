package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

public class SimpleBlockFeature extends Feature<SimpleBlockConfiguration> {
    public SimpleBlockFeature(Codec<SimpleBlockConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> context) {
        SimpleBlockConfiguration simpleBlockConfiguration = context.config();
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        BlockState blockState = simpleBlockConfiguration.toPlace().getState(context.random(), blockPos);
        if (blockState.canSurvive(worldGenLevel, blockPos)) {
            if (blockState.getBlock() instanceof DoublePlantBlock) {
                if (!worldGenLevel.isEmptyBlock(blockPos.above())) {
                    return false;
                }

                DoublePlantBlock.placeAt(worldGenLevel, blockState, blockPos, 2);
            } else {
                worldGenLevel.setBlock(blockPos, blockState, 2);
            }

            return true;
        } else {
            return false;
        }
    }
}
