package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class WeightedListHeight extends HeightProvider {
    public static final Codec<WeightedListHeight> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(SimpleWeightedRandomList.wrappedCodec(HeightProvider.CODEC).fieldOf("distribution").forGetter((weightedListHeight) -> {
            return weightedListHeight.distribution;
        })).apply(instance, WeightedListHeight::new);
    });
    private final SimpleWeightedRandomList<HeightProvider> distribution;

    public WeightedListHeight(SimpleWeightedRandomList<HeightProvider> weightedList) {
        this.distribution = weightedList;
    }

    @Override
    public int sample(Random random, WorldGenerationContext context) {
        return this.distribution.getRandomValue(random).orElseThrow(IllegalStateException::new).sample(random, context);
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.WEIGHTED_LIST;
    }
}
