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

public class DarkOakFoliagePlacer extends FoliagePlacer {
    public static final Codec<DarkOakFoliagePlacer> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).apply(instance, DarkOakFoliagePlacer::new);
    });

    public DarkOakFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return FoliagePlacerType.DARK_OAK_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, TreeConfiguration config, int trunkHeight, FoliagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset) {
        BlockPos blockPos = treeNode.pos().above(offset);
        boolean bl = treeNode.doubleTrunk();
        if (bl) {
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 2, -1, bl);
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 3, 0, bl);
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 2, 1, bl);
            if (random.nextBoolean()) {
                this.placeLeavesRow(world, replacer, random, config, blockPos, radius, 2, bl);
            }
        } else {
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 2, -1, bl);
            this.placeLeavesRow(world, replacer, random, config, blockPos, radius + 1, 0, bl);
        }

    }

    @Override
    public int foliageHeight(Random random, int trunkHeight, TreeConfiguration config) {
        return 4;
    }

    @Override
    protected boolean shouldSkipLocationSigned(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return y != 0 || !giantTrunk || dx != -radius && dx < radius || dz != -radius && dz < radius ? super.shouldSkipLocationSigned(random, dx, y, dz, radius, giantTrunk) : true;
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        if (y == -1 && !giantTrunk) {
            return dx == radius && dz == radius;
        } else if (y == 1) {
            return dx + dz > radius * 2 - 2;
        } else {
            return false;
        }
    }
}
