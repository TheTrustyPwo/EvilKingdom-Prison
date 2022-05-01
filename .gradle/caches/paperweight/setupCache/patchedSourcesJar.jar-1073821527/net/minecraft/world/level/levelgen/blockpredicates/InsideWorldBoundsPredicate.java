package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;

public class InsideWorldBoundsPredicate implements BlockPredicate {
    public static final Codec<InsideWorldBoundsPredicate> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Vec3i.offsetCodec(16).optionalFieldOf("offset", BlockPos.ZERO).forGetter((predicate) -> {
            return predicate.offset;
        })).apply(instance, InsideWorldBoundsPredicate::new);
    });
    private final Vec3i offset;

    public InsideWorldBoundsPredicate(Vec3i offset) {
        this.offset = offset;
    }

    @Override
    public boolean test(WorldGenLevel worldGenLevel, BlockPos blockPos) {
        return !worldGenLevel.isOutsideBuildHeight(blockPos.offset(this.offset));
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.INSIDE_WORLD_BOUNDS;
    }
}
