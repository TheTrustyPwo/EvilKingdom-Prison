package net.minecraft.world.level.levelgen.feature;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

public class DripstoneUtils {
    protected static double getDripstoneHeight(double radius, double scale, double heightScale, double bluntness) {
        if (radius < bluntness) {
            radius = bluntness;
        }

        double d = 0.384D;
        double e = radius / scale * 0.384D;
        double f = 0.75D * Math.pow(e, 1.3333333333333333D);
        double g = Math.pow(e, 0.6666666666666666D);
        double h = 0.3333333333333333D * Math.log(e);
        double i = heightScale * (f - g - h);
        i = Math.max(i, 0.0D);
        return i / 0.384D * scale;
    }

    protected static boolean isCircleMostlyEmbeddedInStone(WorldGenLevel world, BlockPos pos, int height) {
        if (isEmptyOrWaterOrLava(world, pos)) {
            return false;
        } else {
            float f = 6.0F;
            float g = 6.0F / (float)height;

            for(float h = 0.0F; h < ((float)Math.PI * 2F); h += g) {
                int i = (int)(Mth.cos(h) * (float)height);
                int j = (int)(Mth.sin(h) * (float)height);
                if (isEmptyOrWaterOrLava(world, pos.offset(i, 0, j))) {
                    return false;
                }
            }

            return true;
        }
    }

    protected static boolean isEmptyOrWater(LevelAccessor world, BlockPos pos) {
        return world.isStateAtPosition(pos, DripstoneUtils::isEmptyOrWater);
    }

    protected static boolean isEmptyOrWaterOrLava(LevelAccessor world, BlockPos pos) {
        return world.isStateAtPosition(pos, DripstoneUtils::isEmptyOrWaterOrLava);
    }

    protected static void buildBaseToTipColumn(Direction direction, int height, boolean merge, Consumer<BlockState> callback) {
        if (height >= 3) {
            callback.accept(createPointedDripstone(direction, DripstoneThickness.BASE));

            for(int i = 0; i < height - 3; ++i) {
                callback.accept(createPointedDripstone(direction, DripstoneThickness.MIDDLE));
            }
        }

        if (height >= 2) {
            callback.accept(createPointedDripstone(direction, DripstoneThickness.FRUSTUM));
        }

        if (height >= 1) {
            callback.accept(createPointedDripstone(direction, merge ? DripstoneThickness.TIP_MERGE : DripstoneThickness.TIP));
        }

    }

    protected static void growPointedDripstone(LevelAccessor world, BlockPos pos, Direction direction, int height, boolean merge) {
        if (isDripstoneBase(world.getBlockState(pos.relative(direction.getOpposite())))) {
            BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
            buildBaseToTipColumn(direction, height, merge, (state) -> {
                if (state.is(Blocks.POINTED_DRIPSTONE)) {
                    state = state.setValue(PointedDripstoneBlock.WATERLOGGED, Boolean.valueOf(world.isWaterAt(mutableBlockPos)));
                }

                world.setBlock(mutableBlockPos, state, 2);
                mutableBlockPos.move(direction);
            });
        }
    }

    protected static boolean placeDripstoneBlockIfPossible(LevelAccessor world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(BlockTags.DRIPSTONE_REPLACEABLE)) {
            world.setBlock(pos, Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 2);
            return true;
        } else {
            return false;
        }
    }

    private static BlockState createPointedDripstone(Direction direction, DripstoneThickness thickness) {
        return Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION, direction).setValue(PointedDripstoneBlock.THICKNESS, thickness);
    }

    public static boolean isDripstoneBaseOrLava(BlockState state) {
        return isDripstoneBase(state) || state.is(Blocks.LAVA);
    }

    public static boolean isDripstoneBase(BlockState state) {
        return state.is(Blocks.DRIPSTONE_BLOCK) || state.is(BlockTags.DRIPSTONE_REPLACEABLE);
    }

    public static boolean isEmptyOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    public static boolean isNeitherEmptyNorWater(BlockState state) {
        return !state.isAir() && !state.is(Blocks.WATER);
    }

    public static boolean isEmptyOrWaterOrLava(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }
}
