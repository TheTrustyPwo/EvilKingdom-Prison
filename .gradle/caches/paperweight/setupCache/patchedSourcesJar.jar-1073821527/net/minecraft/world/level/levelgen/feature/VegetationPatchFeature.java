package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class VegetationPatchFeature extends Feature<VegetationPatchConfiguration> {
    public VegetationPatchFeature(Codec<VegetationPatchConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<VegetationPatchConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        VegetationPatchConfiguration vegetationPatchConfiguration = context.config();
        Random random = context.random();
        BlockPos blockPos = context.origin();
        Predicate<BlockState> predicate = (state) -> {
            return state.is(vegetationPatchConfiguration.replaceable);
        };
        int i = vegetationPatchConfiguration.xzRadius.sample(random) + 1;
        int j = vegetationPatchConfiguration.xzRadius.sample(random) + 1;
        Set<BlockPos> set = this.placeGroundPatch(worldGenLevel, vegetationPatchConfiguration, random, blockPos, predicate, i, j);
        this.distributeVegetation(context, worldGenLevel, vegetationPatchConfiguration, random, set, i, j);
        return !set.isEmpty();
    }

    protected Set<BlockPos> placeGroundPatch(WorldGenLevel world, VegetationPatchConfiguration config, Random random, BlockPos pos, Predicate<BlockState> replaceable, int radiusX, int radiusZ) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        BlockPos.MutableBlockPos mutableBlockPos2 = mutableBlockPos.mutable();
        Direction direction = config.surface.getDirection();
        Direction direction2 = direction.getOpposite();
        Set<BlockPos> set = new HashSet<>();

        for(int i = -radiusX; i <= radiusX; ++i) {
            boolean bl = i == -radiusX || i == radiusX;

            for(int j = -radiusZ; j <= radiusZ; ++j) {
                boolean bl2 = j == -radiusZ || j == radiusZ;
                boolean bl3 = bl || bl2;
                boolean bl4 = bl && bl2;
                boolean bl5 = bl3 && !bl4;
                if (!bl4 && (!bl5 || config.extraEdgeColumnChance != 0.0F && !(random.nextFloat() > config.extraEdgeColumnChance))) {
                    mutableBlockPos.setWithOffset(pos, i, 0, j);

                    for(int k = 0; world.isStateAtPosition(mutableBlockPos, BlockBehaviour.BlockStateBase::isAir) && k < config.verticalRange; ++k) {
                        mutableBlockPos.move(direction);
                    }

                    for(int var25 = 0; world.isStateAtPosition(mutableBlockPos, (state) -> {
                        return !state.isAir();
                    }) && var25 < config.verticalRange; ++var25) {
                        mutableBlockPos.move(direction2);
                    }

                    mutableBlockPos2.setWithOffset(mutableBlockPos, config.surface.getDirection());
                    BlockState blockState = world.getBlockState(mutableBlockPos2);
                    if (world.isEmptyBlock(mutableBlockPos) && blockState.isFaceSturdy(world, mutableBlockPos2, config.surface.getDirection().getOpposite())) {
                        int l = config.depth.sample(random) + (config.extraBottomBlockChance > 0.0F && random.nextFloat() < config.extraBottomBlockChance ? 1 : 0);
                        BlockPos blockPos = mutableBlockPos2.immutable();
                        boolean bl6 = this.placeGround(world, config, replaceable, random, mutableBlockPos2, l);
                        if (bl6) {
                            set.add(blockPos);
                        }
                    }
                }
            }
        }

        return set;
    }

    protected void distributeVegetation(FeaturePlaceContext<VegetationPatchConfiguration> context, WorldGenLevel world, VegetationPatchConfiguration config, Random random, Set<BlockPos> positions, int radiusX, int radiusZ) {
        for(BlockPos blockPos : positions) {
            if (config.vegetationChance > 0.0F && random.nextFloat() < config.vegetationChance) {
                this.placeVegetation(world, config, context.chunkGenerator(), random, blockPos);
            }
        }

    }

    protected boolean placeVegetation(WorldGenLevel world, VegetationPatchConfiguration config, ChunkGenerator generator, Random random, BlockPos pos) {
        return config.vegetationFeature.value().place(world, generator, random, pos.relative(config.surface.getDirection().getOpposite()));
    }

    protected boolean placeGround(WorldGenLevel world, VegetationPatchConfiguration config, Predicate<BlockState> replaceable, Random random, BlockPos.MutableBlockPos pos, int depth) {
        for(int i = 0; i < depth; ++i) {
            BlockState blockState = config.groundState.getState(random, pos);
            BlockState blockState2 = world.getBlockState(pos);
            if (!blockState.is(blockState2.getBlock())) {
                if (!replaceable.test(blockState2)) {
                    return i != 0;
                }

                world.setBlock(pos, blockState, 2);
                pos.move(config.surface.getDirection());
            }
        }

        return true;
    }
}
