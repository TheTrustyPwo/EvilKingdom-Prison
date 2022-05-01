package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;

public class ReplaceBlockConfiguration implements FeatureConfiguration {
    public static final Codec<ReplaceBlockConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.list(OreConfiguration.TargetBlockState.CODEC).fieldOf("targets").forGetter((config) -> {
            return config.targetStates;
        })).apply(instance, ReplaceBlockConfiguration::new);
    });
    public final List<OreConfiguration.TargetBlockState> targetStates;

    public ReplaceBlockConfiguration(BlockState target, BlockState state) {
        this(ImmutableList.of(OreConfiguration.target(new BlockStateMatchTest(target), state)));
    }

    public ReplaceBlockConfiguration(List<OreConfiguration.TargetBlockState> targets) {
        this.targetStates = targets;
    }
}
