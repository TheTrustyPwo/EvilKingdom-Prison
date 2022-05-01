package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class BendingTrunkPlacer extends TrunkPlacer {
    public static final Codec<BendingTrunkPlacer> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).and(instance.group(ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_height_for_leaves", 1).forGetter((placer) -> {
            return placer.minHeightForLeaves;
        }), IntProvider.codec(1, 64).fieldOf("bend_length").forGetter((placer) -> {
            return placer.bendLength;
        }))).apply(instance, BendingTrunkPlacer::new);
    });
    private final int minHeightForLeaves;
    private final IntProvider bendLength;

    public BendingTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight, int minHeightForLeaves, IntProvider bendLength) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
        this.minHeightForLeaves = minHeightForLeaves;
        this.bendLength = bendLength;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.BENDING_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, int height, BlockPos startPos, TreeConfiguration config) {
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int i = height - 1;
        BlockPos.MutableBlockPos mutableBlockPos = startPos.mutable();
        BlockPos blockPos = mutableBlockPos.below();
        setDirtAt(world, replacer, random, blockPos, config);
        List<FoliagePlacer.FoliageAttachment> list = Lists.newArrayList();

        for(int j = 0; j <= i; ++j) {
            if (j + 1 >= i + random.nextInt(2)) {
                mutableBlockPos.move(direction);
            }

            if (TreeFeature.validTreePos(world, mutableBlockPos)) {
                placeLog(world, replacer, random, mutableBlockPos, config);
            }

            if (j >= this.minHeightForLeaves) {
                list.add(new FoliagePlacer.FoliageAttachment(mutableBlockPos.immutable(), 0, false));
            }

            mutableBlockPos.move(Direction.UP);
        }

        int k = this.bendLength.sample(random);

        for(int l = 0; l <= k; ++l) {
            if (TreeFeature.validTreePos(world, mutableBlockPos)) {
                placeLog(world, replacer, random, mutableBlockPos, config);
            }

            list.add(new FoliagePlacer.FoliageAttachment(mutableBlockPos.immutable(), 0, false));
            mutableBlockPos.move(direction);
        }

        return list;
    }
}
