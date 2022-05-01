package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;

abstract class CombiningPredicate implements BlockPredicate {
    protected final List<BlockPredicate> predicates;

    protected CombiningPredicate(List<BlockPredicate> predicates) {
        this.predicates = predicates;
    }

    public static <T extends CombiningPredicate> Codec<T> codec(Function<List<BlockPredicate>, T> combiner) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter((predicate) -> {
                return predicate.predicates;
            })).apply(instance, combiner);
        });
    }
}
