package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class RandomizedIntStateProvider extends BlockStateProvider {
    public static final Codec<RandomizedIntStateProvider> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockStateProvider.CODEC.fieldOf("source").forGetter((randomizedIntStateProvider) -> {
            return randomizedIntStateProvider.source;
        }), Codec.STRING.fieldOf("property").forGetter((randomizedIntStateProvider) -> {
            return randomizedIntStateProvider.propertyName;
        }), IntProvider.CODEC.fieldOf("values").forGetter((randomizedIntStateProvider) -> {
            return randomizedIntStateProvider.values;
        })).apply(instance, RandomizedIntStateProvider::new);
    });
    private final BlockStateProvider source;
    private final String propertyName;
    @Nullable
    private IntegerProperty property;
    private final IntProvider values;

    public RandomizedIntStateProvider(BlockStateProvider source, IntegerProperty property, IntProvider values) {
        this.source = source;
        this.property = property;
        this.propertyName = property.getName();
        this.values = values;
        Collection<Integer> collection = property.getPossibleValues();

        for(int i = values.getMinValue(); i <= values.getMaxValue(); ++i) {
            if (!collection.contains(i)) {
                throw new IllegalArgumentException("Property value out of range: " + property.getName() + ": " + i);
            }
        }

    }

    public RandomizedIntStateProvider(BlockStateProvider source, String propertyName, IntProvider values) {
        this.source = source;
        this.propertyName = propertyName;
        this.values = values;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.RANDOMIZED_INT_STATE_PROVIDER;
    }

    @Override
    public BlockState getState(Random random, BlockPos pos) {
        BlockState blockState = this.source.getState(random, pos);
        if (this.property == null || !blockState.hasProperty(this.property)) {
            this.property = findProperty(blockState, this.propertyName);
        }

        return blockState.setValue(this.property, Integer.valueOf(this.values.sample(random)));
    }

    private static IntegerProperty findProperty(BlockState state, String propertyName) {
        Collection<Property<?>> collection = state.getProperties();
        Optional<IntegerProperty> optional = collection.stream().filter((property) -> {
            return property.getName().equals(propertyName);
        }).filter((property) -> {
            return property instanceof IntegerProperty;
        }).map((property) -> {
            return (IntegerProperty)property;
        }).findAny();
        return optional.orElseThrow(() -> {
            return new IllegalArgumentException("Illegal property: " + propertyName);
        });
    }
}
