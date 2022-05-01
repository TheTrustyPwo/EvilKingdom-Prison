package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public class OreConfiguration implements FeatureConfiguration {
    public static final Codec<OreConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.list(OreConfiguration.TargetBlockState.CODEC).fieldOf("targets").forGetter((config) -> {
            return config.targetStates;
        }), Codec.intRange(0, 64).fieldOf("size").forGetter((config) -> {
            return config.size;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("discard_chance_on_air_exposure").forGetter((config) -> {
            return config.discardChanceOnAirExposure;
        })).apply(instance, OreConfiguration::new);
    });
    public final List<OreConfiguration.TargetBlockState> targetStates;
    public final int size;
    public final float discardChanceOnAirExposure;

    public OreConfiguration(List<OreConfiguration.TargetBlockState> targets, int size, float discardOnAirChance) {
        this.size = size;
        this.targetStates = targets;
        this.discardChanceOnAirExposure = discardOnAirChance;
    }

    public OreConfiguration(List<OreConfiguration.TargetBlockState> targets, int size) {
        this(targets, size, 0.0F);
    }

    public OreConfiguration(RuleTest test, BlockState state, int size, float discardOnAirChance) {
        this(ImmutableList.of(new OreConfiguration.TargetBlockState(test, state)), size, discardOnAirChance);
    }

    public OreConfiguration(RuleTest test, BlockState state, int size) {
        this(ImmutableList.of(new OreConfiguration.TargetBlockState(test, state)), size, 0.0F);
    }

    public static OreConfiguration.TargetBlockState target(RuleTest test, BlockState state) {
        return new OreConfiguration.TargetBlockState(test, state);
    }

    public static class TargetBlockState {
        public static final Codec<OreConfiguration.TargetBlockState> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(RuleTest.CODEC.fieldOf("target").forGetter((target) -> {
                return target.target;
            }), BlockState.CODEC.fieldOf("state").forGetter((target) -> {
                return target.state;
            })).apply(instance, OreConfiguration.TargetBlockState::new);
        });
        public final RuleTest target;
        public final BlockState state;

        TargetBlockState(RuleTest target, BlockState state) {
            this.target = target;
            this.state = state;
        }
    }
}
