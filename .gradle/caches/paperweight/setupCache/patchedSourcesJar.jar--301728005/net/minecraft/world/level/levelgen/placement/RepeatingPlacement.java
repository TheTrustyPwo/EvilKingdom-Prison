package net.minecraft.world.level.levelgen.placement;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;

public abstract class RepeatingPlacement extends PlacementModifier {
    protected abstract int count(Random random, BlockPos pos);

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, Random random, BlockPos pos) {
        return IntStream.range(0, this.count(random, pos)).mapToObj((i) -> {
            return pos;
        });
    }
}
