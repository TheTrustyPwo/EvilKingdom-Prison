package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;

public class SpringConfiguration implements FeatureConfiguration {
    public static final Codec<SpringConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(FluidState.CODEC.fieldOf("state").forGetter((config) -> {
            return config.state;
        }), Codec.BOOL.fieldOf("requires_block_below").orElse(true).forGetter((config) -> {
            return config.requiresBlockBelow;
        }), Codec.INT.fieldOf("rock_count").orElse(4).forGetter((config) -> {
            return config.rockCount;
        }), Codec.INT.fieldOf("hole_count").orElse(1).forGetter((config) -> {
            return config.holeCount;
        }), RegistryCodecs.homogeneousList(Registry.BLOCK_REGISTRY).fieldOf("valid_blocks").forGetter((config) -> {
            return config.validBlocks;
        })).apply(instance, SpringConfiguration::new);
    });
    public final FluidState state;
    public final boolean requiresBlockBelow;
    public final int rockCount;
    public final int holeCount;
    public final HolderSet<Block> validBlocks;

    public SpringConfiguration(FluidState state, boolean requiresBlockBelow, int rockCount, int holeCount, HolderSet<Block> validBlocks) {
        this.state = state;
        this.requiresBlockBelow = requiresBlockBelow;
        this.rockCount = rockCount;
        this.holeCount = holeCount;
        this.validBlocks = validBlocks;
    }
}
