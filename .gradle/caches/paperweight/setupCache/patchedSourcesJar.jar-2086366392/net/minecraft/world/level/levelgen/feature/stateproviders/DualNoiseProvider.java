package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class DualNoiseProvider extends NoiseProvider {
    public static final Codec<DualNoiseProvider> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(InclusiveRange.codec(Codec.INT, 1, 64).fieldOf("variety").forGetter((dualNoiseProvider) -> {
            return dualNoiseProvider.variety;
        }), NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("slow_noise").forGetter((dualNoiseProvider) -> {
            return dualNoiseProvider.slowNoiseParameters;
        }), ExtraCodecs.POSITIVE_FLOAT.fieldOf("slow_scale").forGetter((dualNoiseProvider) -> {
            return dualNoiseProvider.slowScale;
        })).and(noiseProviderCodec(instance)).apply(instance, DualNoiseProvider::new);
    });
    private final InclusiveRange<Integer> variety;
    private final NormalNoise.NoiseParameters slowNoiseParameters;
    private final float slowScale;
    private final NormalNoise slowNoise;

    public DualNoiseProvider(InclusiveRange<Integer> variety, NormalNoise.NoiseParameters slowNoiseParameters, float slowScale, long seed, NormalNoise.NoiseParameters noiseParameters, float scale, List<BlockState> states) {
        super(seed, noiseParameters, scale, states);
        this.variety = variety;
        this.slowNoiseParameters = slowNoiseParameters;
        this.slowScale = slowScale;
        this.slowNoise = NormalNoise.create(new WorldgenRandom(new LegacyRandomSource(seed)), slowNoiseParameters);
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.DUAL_NOISE_PROVIDER;
    }

    @Override
    public BlockState getState(Random random, BlockPos pos) {
        double d = this.getSlowNoiseValue(pos);
        int i = (int)Mth.clampedMap(d, -1.0D, 1.0D, (double)this.variety.minInclusive().intValue(), (double)(this.variety.maxInclusive() + 1));
        List<BlockState> list = Lists.newArrayListWithCapacity(i);

        for(int j = 0; j < i; ++j) {
            list.add(this.getRandomState(this.states, this.getSlowNoiseValue(pos.offset(j * '\ud511', 0, j * '\u85ba'))));
        }

        return this.getRandomState(list, pos, (double)this.scale);
    }

    protected double getSlowNoiseValue(BlockPos pos) {
        return this.slowNoise.getValue((double)((float)pos.getX() * this.slowScale), (double)((float)pos.getY() * this.slowScale), (double)((float)pos.getZ() * this.slowScale));
    }
}
