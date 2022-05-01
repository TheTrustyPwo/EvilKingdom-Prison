package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.configurations.PointedDripstoneConfiguration;

public class PointedDripstoneFeature extends Feature<PointedDripstoneConfiguration> {
    public PointedDripstoneFeature(Codec<PointedDripstoneConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<PointedDripstoneConfiguration> context) {
        LevelAccessor levelAccessor = context.level();
        BlockPos blockPos = context.origin();
        Random random = context.random();
        PointedDripstoneConfiguration pointedDripstoneConfiguration = context.config();
        Optional<Direction> optional = getTipDirection(levelAccessor, blockPos, random);
        if (optional.isEmpty()) {
            return false;
        } else {
            BlockPos blockPos2 = blockPos.relative(optional.get().getOpposite());
            createPatchOfDripstoneBlocks(levelAccessor, random, blockPos2, pointedDripstoneConfiguration);
            int i = random.nextFloat() < pointedDripstoneConfiguration.chanceOfTallerDripstone && DripstoneUtils.isEmptyOrWater(levelAccessor.getBlockState(blockPos.relative(optional.get()))) ? 2 : 1;
            DripstoneUtils.growPointedDripstone(levelAccessor, blockPos, optional.get(), i, false);
            return true;
        }
    }

    private static Optional<Direction> getTipDirection(LevelAccessor world, BlockPos pos, Random random) {
        boolean bl = DripstoneUtils.isDripstoneBase(world.getBlockState(pos.above()));
        boolean bl2 = DripstoneUtils.isDripstoneBase(world.getBlockState(pos.below()));
        if (bl && bl2) {
            return Optional.of(random.nextBoolean() ? Direction.DOWN : Direction.UP);
        } else if (bl) {
            return Optional.of(Direction.DOWN);
        } else {
            return bl2 ? Optional.of(Direction.UP) : Optional.empty();
        }
    }

    private static void createPatchOfDripstoneBlocks(LevelAccessor world, Random random, BlockPos pos, PointedDripstoneConfiguration config) {
        DripstoneUtils.placeDripstoneBlockIfPossible(world, pos);

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            if (!(random.nextFloat() > config.chanceOfDirectionalSpread)) {
                BlockPos blockPos = pos.relative(direction);
                DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos);
                if (!(random.nextFloat() > config.chanceOfSpreadRadius2)) {
                    BlockPos blockPos2 = blockPos.relative(Direction.getRandom(random));
                    DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos2);
                    if (!(random.nextFloat() > config.chanceOfSpreadRadius3)) {
                        BlockPos blockPos3 = blockPos2.relative(Direction.getRandom(random));
                        DripstoneUtils.placeDripstoneBlockIfPossible(world, blockPos3);
                    }
                }
            }
        }

    }
}
