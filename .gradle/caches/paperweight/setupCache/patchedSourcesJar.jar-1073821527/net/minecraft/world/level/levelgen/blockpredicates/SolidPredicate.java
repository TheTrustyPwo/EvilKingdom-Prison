package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

public class SolidPredicate extends StateTestingPredicate {
    public static final Codec<SolidPredicate> CODEC = RecordCodecBuilder.create((instance) -> {
        return stateTestingCodec(instance).apply(instance, SolidPredicate::new);
    });

    public SolidPredicate(Vec3i offset) {
        super(offset);
    }

    @Override
    protected boolean test(BlockState state) {
        return state.getMaterial().isSolid();
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.SOLID;
    }
}
