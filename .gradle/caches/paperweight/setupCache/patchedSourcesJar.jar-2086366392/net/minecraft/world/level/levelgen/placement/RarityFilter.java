package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;

public class RarityFilter extends PlacementFilter {
    public static final Codec<RarityFilter> CODEC = ExtraCodecs.POSITIVE_INT.fieldOf("chance").xmap(RarityFilter::new, (rarityFilter) -> {
        return rarityFilter.chance;
    }).codec();
    private final int chance;

    private RarityFilter(int chance) {
        this.chance = chance;
    }

    public static RarityFilter onAverageOnceEvery(int chance) {
        return new RarityFilter(chance);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, Random random, BlockPos pos) {
        return random.nextFloat() < 1.0F / (float)this.chance;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.RARITY_FILTER;
    }
}
