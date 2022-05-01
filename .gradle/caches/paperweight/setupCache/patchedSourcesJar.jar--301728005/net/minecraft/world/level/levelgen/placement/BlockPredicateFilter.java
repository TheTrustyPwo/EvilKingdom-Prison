package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public class BlockPredicateFilter extends PlacementFilter {
    public static final Codec<BlockPredicateFilter> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockPredicate.CODEC.fieldOf("predicate").forGetter((blockPredicateFilter) -> {
            return blockPredicateFilter.predicate;
        })).apply(instance, BlockPredicateFilter::new);
    });
    private final BlockPredicate predicate;

    private BlockPredicateFilter(BlockPredicate predicate) {
        this.predicate = predicate;
    }

    public static BlockPredicateFilter forPredicate(BlockPredicate predicate) {
        return new BlockPredicateFilter(predicate);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, Random random, BlockPos pos) {
        return this.predicate.test(context.getLevel(), pos);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.BLOCK_PREDICATE_FILTER;
    }
}
