package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class RandomSpreadFoliagePlacer extends FoliagePlacer {
    public static final Codec<RandomSpreadFoliagePlacer> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).and(instance.group(IntProvider.codec(1, 512).fieldOf("foliage_height").forGetter((placer) -> {
            return placer.foliageHeight;
        }), Codec.intRange(0, 256).fieldOf("leaf_placement_attempts").forGetter((placer) -> {
            return placer.leafPlacementAttempts;
        }))).apply(instance, RandomSpreadFoliagePlacer::new);
    });
    private final IntProvider foliageHeight;
    private final int leafPlacementAttempts;

    public RandomSpreadFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider foliageHeight, int leafPlacementAttempts) {
        super(radius, offset);
        this.foliageHeight = foliageHeight;
        this.leafPlacementAttempts = leafPlacementAttempts;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return FoliagePlacerType.RANDOM_SPREAD_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, TreeConfiguration config, int trunkHeight, FoliagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        BlockPos blockPos = treeNode.pos();
        BlockPos.MutableBlockPos mutableBlockPos = blockPos.mutable();

        for(int i = 0; i < this.leafPlacementAttempts; ++i) {
            mutableBlockPos.setWithOffset(blockPos, random.nextInt(radius) - random.nextInt(radius), random.nextInt(foliageHeight) - random.nextInt(foliageHeight), random.nextInt(radius) - random.nextInt(radius));
            tryPlaceLeaf(world, replacer, random, config, mutableBlockPos);
        }

    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, TreeConfiguration config) {
        return this.foliageHeight.sample(random);
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return false;
    }
}
