package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** @deprecated */
@Deprecated
public class CountOnEveryLayerPlacement extends PlacementModifier {
    public static final Codec<CountOnEveryLayerPlacement> CODEC = IntProvider.codec(0, 256).fieldOf("count").xmap(CountOnEveryLayerPlacement::new, (countOnEveryLayerPlacement) -> {
        return countOnEveryLayerPlacement.count;
    }).codec();
    private final IntProvider count;

    private CountOnEveryLayerPlacement(IntProvider count) {
        this.count = count;
    }

    public static CountOnEveryLayerPlacement of(IntProvider count) {
        return new CountOnEveryLayerPlacement(count);
    }

    public static CountOnEveryLayerPlacement of(int count) {
        return of(ConstantInt.of(count));
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, Random random, BlockPos pos) {
        Builder<BlockPos> builder = Stream.builder();
        int i = 0;

        boolean bl;
        do {
            bl = false;

            for(int j = 0; j < this.count.sample(random); ++j) {
                int k = random.nextInt(16) + pos.getX();
                int l = random.nextInt(16) + pos.getZ();
                int m = context.getHeight(Heightmap.Types.MOTION_BLOCKING, k, l);
                int n = findOnGroundYPosition(context, k, m, l, i);
                if (n != Integer.MAX_VALUE) {
                    builder.add(new BlockPos(k, n, l));
                    bl = true;
                }
            }

            ++i;
        } while(bl);

        return builder.build();
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.COUNT_ON_EVERY_LAYER;
    }

    private static int findOnGroundYPosition(PlacementContext context, int x, int y, int z, int targetY) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(x, y, z);
        int i = 0;
        BlockState blockState = context.getBlockState(mutableBlockPos);

        for(int j = y; j >= context.getMinBuildHeight() + 1; --j) {
            mutableBlockPos.setY(j - 1);
            BlockState blockState2 = context.getBlockState(mutableBlockPos);
            if (!isEmpty(blockState2) && isEmpty(blockState) && !blockState2.is(Blocks.BEDROCK)) {
                if (i == targetY) {
                    return mutableBlockPos.getY() + 1;
                }

                ++i;
            }

            blockState = blockState2;
        }

        return Integer.MAX_VALUE;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }
}
