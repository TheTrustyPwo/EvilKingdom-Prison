package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public abstract class TrunkPlacer {
    public static final Codec<TrunkPlacer> CODEC = Registry.TRUNK_PLACER_TYPES.byNameCodec().dispatch(TrunkPlacer::type, TrunkPlacerType::codec);
    private static final int MAX_BASE_HEIGHT = 32;
    private static final int MAX_RAND = 24;
    public static final int MAX_HEIGHT = 80;
    protected final int baseHeight;
    protected final int heightRandA;
    protected final int heightRandB;

    protected static <P extends TrunkPlacer> P3<Mu<P>, Integer, Integer, Integer> trunkPlacerParts(Instance<P> instance) {
        return instance.group(Codec.intRange(0, 32).fieldOf("base_height").forGetter((placer) -> {
            return placer.baseHeight;
        }), Codec.intRange(0, 24).fieldOf("height_rand_a").forGetter((placer) -> {
            return placer.heightRandA;
        }), Codec.intRange(0, 24).fieldOf("height_rand_b").forGetter((placer) -> {
            return placer.heightRandB;
        }));
    }

    public TrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        this.baseHeight = baseHeight;
        this.heightRandA = firstRandomHeight;
        this.heightRandB = secondRandomHeight;
    }

    protected abstract TrunkPlacerType<?> type();

    public abstract List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, int height, BlockPos startPos, TreeConfiguration config);

    public int getTreeHeight(Random random) {
        return this.baseHeight + random.nextInt(this.heightRandA + 1) + random.nextInt(this.heightRandB + 1);
    }

    private static boolean isDirt(LevelSimulatedReader world, BlockPos pos) {
        return world.isStateAtPosition(pos, (state) -> {
            return Feature.isDirt(state) && !state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.MYCELIUM);
        });
    }

    protected static void setDirtAt(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, BlockPos pos, TreeConfiguration config) {
        if (config.forceDirt || !isDirt(world, pos)) {
            replacer.accept(pos, config.dirtProvider.getState(random, pos));
        }

    }

    protected static boolean placeLog(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, BlockPos pos, TreeConfiguration config) {
        return placeLog(world, replacer, random, pos, config, Function.identity());
    }

    protected static boolean placeLog(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, BlockPos pos, TreeConfiguration config, Function<BlockState, BlockState> stateProvider) {
        if (TreeFeature.validTreePos(world, pos)) {
            replacer.accept(pos, stateProvider.apply(config.trunkProvider.getState(random, pos)));
            return true;
        } else {
            return false;
        }
    }

    protected static void placeLogIfFree(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, BlockPos.MutableBlockPos pos, TreeConfiguration config) {
        if (TreeFeature.isFree(world, pos)) {
            placeLog(world, replacer, random, pos, config);
        }

    }
}
