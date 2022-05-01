package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RootSystemConfiguration implements FeatureConfiguration {
    public static final Codec<RootSystemConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(PlacedFeature.CODEC.fieldOf("feature").forGetter((config) -> {
            return config.treeFeature;
        }), Codec.intRange(1, 64).fieldOf("required_vertical_space_for_tree").forGetter((config) -> {
            return config.requiredVerticalSpaceForTree;
        }), Codec.intRange(1, 64).fieldOf("root_radius").forGetter((config) -> {
            return config.rootRadius;
        }), TagKey.hashedCodec(Registry.BLOCK_REGISTRY).fieldOf("root_replaceable").forGetter((config) -> {
            return config.rootReplaceable;
        }), BlockStateProvider.CODEC.fieldOf("root_state_provider").forGetter((config) -> {
            return config.rootStateProvider;
        }), Codec.intRange(1, 256).fieldOf("root_placement_attempts").forGetter((config) -> {
            return config.rootPlacementAttempts;
        }), Codec.intRange(1, 4096).fieldOf("root_column_max_height").forGetter((config) -> {
            return config.rootColumnMaxHeight;
        }), Codec.intRange(1, 64).fieldOf("hanging_root_radius").forGetter((config) -> {
            return config.hangingRootRadius;
        }), Codec.intRange(0, 16).fieldOf("hanging_roots_vertical_span").forGetter((config) -> {
            return config.hangingRootsVerticalSpan;
        }), BlockStateProvider.CODEC.fieldOf("hanging_root_state_provider").forGetter((config) -> {
            return config.hangingRootStateProvider;
        }), Codec.intRange(1, 256).fieldOf("hanging_root_placement_attempts").forGetter((config) -> {
            return config.hangingRootPlacementAttempts;
        }), Codec.intRange(1, 64).fieldOf("allowed_vertical_water_for_tree").forGetter((config) -> {
            return config.allowedVerticalWaterForTree;
        }), BlockPredicate.CODEC.fieldOf("allowed_tree_position").forGetter((config) -> {
            return config.allowedTreePosition;
        })).apply(instance, RootSystemConfiguration::new);
    });
    public final Holder<PlacedFeature> treeFeature;
    public final int requiredVerticalSpaceForTree;
    public final int rootRadius;
    public final TagKey<Block> rootReplaceable;
    public final BlockStateProvider rootStateProvider;
    public final int rootPlacementAttempts;
    public final int rootColumnMaxHeight;
    public final int hangingRootRadius;
    public final int hangingRootsVerticalSpan;
    public final BlockStateProvider hangingRootStateProvider;
    public final int hangingRootPlacementAttempts;
    public final int allowedVerticalWaterForTree;
    public final BlockPredicate allowedTreePosition;

    public RootSystemConfiguration(Holder<PlacedFeature> feature, int requiredVerticalSpaceForTree, int rootRadius, TagKey<Block> rootReplaceable, BlockStateProvider rootStateProvider, int rootPlacementAttempts, int maxRootColumnHeight, int hangingRootRadius, int hangingRootVerticalSpan, BlockStateProvider hangingRootStateProvider, int hangingRootPlacementAttempts, int allowedVerticalWaterForTree, BlockPredicate predicate) {
        this.treeFeature = feature;
        this.requiredVerticalSpaceForTree = requiredVerticalSpaceForTree;
        this.rootRadius = rootRadius;
        this.rootReplaceable = rootReplaceable;
        this.rootStateProvider = rootStateProvider;
        this.rootPlacementAttempts = rootPlacementAttempts;
        this.rootColumnMaxHeight = maxRootColumnHeight;
        this.hangingRootRadius = hangingRootRadius;
        this.hangingRootsVerticalSpan = hangingRootVerticalSpan;
        this.hangingRootStateProvider = hangingRootStateProvider;
        this.hangingRootPlacementAttempts = hangingRootPlacementAttempts;
        this.allowedVerticalWaterForTree = allowedVerticalWaterForTree;
        this.allowedTreePosition = predicate;
    }
}
