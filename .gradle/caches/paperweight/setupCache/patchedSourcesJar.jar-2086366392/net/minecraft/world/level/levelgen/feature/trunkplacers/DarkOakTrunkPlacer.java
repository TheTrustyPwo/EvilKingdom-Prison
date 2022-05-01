package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class DarkOakTrunkPlacer extends TrunkPlacer {
    public static final Codec<DarkOakTrunkPlacer> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).apply(instance, DarkOakTrunkPlacer::new);
    });

    public DarkOakTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.DARK_OAK_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, int height, BlockPos startPos, TreeConfiguration config) {
        List<FoliagePlacer.FoliageAttachment> list = Lists.newArrayList();
        BlockPos blockPos = startPos.below();
        setDirtAt(world, replacer, random, blockPos, config);
        setDirtAt(world, replacer, random, blockPos.east(), config);
        setDirtAt(world, replacer, random, blockPos.south(), config);
        setDirtAt(world, replacer, random, blockPos.south().east(), config);
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int i = height - random.nextInt(4);
        int j = 2 - random.nextInt(3);
        int k = startPos.getX();
        int l = startPos.getY();
        int m = startPos.getZ();
        int n = k;
        int o = m;
        int p = l + height - 1;

        for(int q = 0; q < height; ++q) {
            if (q >= i && j > 0) {
                n += direction.getStepX();
                o += direction.getStepZ();
                --j;
            }

            int r = l + q;
            BlockPos blockPos2 = new BlockPos(n, r, o);
            if (TreeFeature.isAirOrLeaves(world, blockPos2)) {
                placeLog(world, replacer, random, blockPos2, config);
                placeLog(world, replacer, random, blockPos2.east(), config);
                placeLog(world, replacer, random, blockPos2.south(), config);
                placeLog(world, replacer, random, blockPos2.east().south(), config);
            }
        }

        list.add(new FoliagePlacer.FoliageAttachment(new BlockPos(n, p, o), 0, true));

        for(int s = -1; s <= 2; ++s) {
            for(int t = -1; t <= 2; ++t) {
                if ((s < 0 || s > 1 || t < 0 || t > 1) && random.nextInt(3) <= 0) {
                    int u = random.nextInt(3) + 2;

                    for(int v = 0; v < u; ++v) {
                        placeLog(world, replacer, random, new BlockPos(k + s, p - v - 1, m + t), config);
                    }

                    list.add(new FoliagePlacer.FoliageAttachment(new BlockPos(n + s, p, o + t), 0, false));
                }
            }
        }

        return list;
    }
}
