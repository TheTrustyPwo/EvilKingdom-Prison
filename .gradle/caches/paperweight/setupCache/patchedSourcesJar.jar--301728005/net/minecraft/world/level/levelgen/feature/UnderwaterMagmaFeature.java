package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.UnderwaterMagmaConfiguration;
import net.minecraft.world.phys.AABB;

public class UnderwaterMagmaFeature extends Feature<UnderwaterMagmaConfiguration> {
    public UnderwaterMagmaFeature(Codec<UnderwaterMagmaConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<UnderwaterMagmaConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        UnderwaterMagmaConfiguration underwaterMagmaConfiguration = context.config();
        Random random = context.random();
        OptionalInt optionalInt = getFloorY(worldGenLevel, blockPos, underwaterMagmaConfiguration);
        if (!optionalInt.isPresent()) {
            return false;
        } else {
            BlockPos blockPos2 = blockPos.atY(optionalInt.getAsInt());
            Vec3i vec3i = new Vec3i(underwaterMagmaConfiguration.placementRadiusAroundFloor, underwaterMagmaConfiguration.placementRadiusAroundFloor, underwaterMagmaConfiguration.placementRadiusAroundFloor);
            AABB aABB = new AABB(blockPos2.subtract(vec3i), blockPos2.offset(vec3i));
            return BlockPos.betweenClosedStream(aABB).filter((pos) -> {
                return random.nextFloat() < underwaterMagmaConfiguration.placementProbabilityPerValidPosition;
            }).filter((pos) -> {
                return this.isValidPlacement(worldGenLevel, pos);
            }).mapToInt((pos) -> {
                worldGenLevel.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                return 1;
            }).sum() > 0;
        }
    }

    private static OptionalInt getFloorY(WorldGenLevel world, BlockPos pos, UnderwaterMagmaConfiguration config) {
        Predicate<BlockState> predicate = (state) -> {
            return state.is(Blocks.WATER);
        };
        Predicate<BlockState> predicate2 = (state) -> {
            return !state.is(Blocks.WATER);
        };
        Optional<Column> optional = Column.scan(world, pos, config.floorSearchRange, predicate, predicate2);
        return optional.map(Column::getFloor).orElseGet(OptionalInt::empty);
    }

    private boolean isValidPlacement(WorldGenLevel world, BlockPos pos) {
        if (!this.isWaterOrAir(world, pos) && !this.isWaterOrAir(world, pos.below())) {
            for(Direction direction : Direction.Plane.HORIZONTAL) {
                if (this.isWaterOrAir(world, pos.relative(direction))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isWaterOrAir(LevelAccessor world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.is(Blocks.WATER) || blockState.isAir();
    }
}
