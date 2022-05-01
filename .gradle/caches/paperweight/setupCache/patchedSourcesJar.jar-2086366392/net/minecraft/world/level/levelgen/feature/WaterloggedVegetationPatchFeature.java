package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class WaterloggedVegetationPatchFeature extends VegetationPatchFeature {
    public WaterloggedVegetationPatchFeature(Codec<VegetationPatchConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected Set<BlockPos> placeGroundPatch(WorldGenLevel world, VegetationPatchConfiguration config, Random random, BlockPos pos, Predicate<BlockState> replaceable, int radiusX, int radiusZ) {
        Set<BlockPos> set = super.placeGroundPatch(world, config, random, pos, replaceable, radiusX, radiusZ);
        Set<BlockPos> set2 = new HashSet<>();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(BlockPos blockPos : set) {
            if (!isExposed(world, set, blockPos, mutableBlockPos)) {
                set2.add(blockPos);
            }
        }

        for(BlockPos blockPos2 : set2) {
            world.setBlock(blockPos2, Blocks.WATER.defaultBlockState(), 2);
        }

        return set2;
    }

    private static boolean isExposed(WorldGenLevel world, Set<BlockPos> positions, BlockPos pos, BlockPos.MutableBlockPos mutablePos) {
        return isExposedDirection(world, pos, mutablePos, Direction.NORTH) || isExposedDirection(world, pos, mutablePos, Direction.EAST) || isExposedDirection(world, pos, mutablePos, Direction.SOUTH) || isExposedDirection(world, pos, mutablePos, Direction.WEST) || isExposedDirection(world, pos, mutablePos, Direction.DOWN);
    }

    private static boolean isExposedDirection(WorldGenLevel world, BlockPos pos, BlockPos.MutableBlockPos mutablePos, Direction direction) {
        mutablePos.setWithOffset(pos, direction);
        return !world.getBlockState(mutablePos).isFaceSturdy(world, mutablePos, direction.getOpposite());
    }

    @Override
    protected boolean placeVegetation(WorldGenLevel world, VegetationPatchConfiguration config, ChunkGenerator generator, Random random, BlockPos pos) {
        if (super.placeVegetation(world, config, generator, random, pos.below())) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && !blockState.getValue(BlockStateProperties.WATERLOGGED)) {
                world.setBlock(pos, blockState.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true)), 2);
            }

            return true;
        } else {
            return false;
        }
    }
}
