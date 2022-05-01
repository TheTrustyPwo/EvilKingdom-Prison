package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

public class HeightRangePlacement extends PlacementModifier {
    public static final Codec<HeightRangePlacement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(HeightProvider.CODEC.fieldOf("height").forGetter((heightRangePlacement) -> {
            return heightRangePlacement.height;
        })).apply(instance, HeightRangePlacement::new);
    });
    private final HeightProvider height;

    private HeightRangePlacement(HeightProvider height) {
        this.height = height;
    }

    public static HeightRangePlacement of(HeightProvider height) {
        return new HeightRangePlacement(height);
    }

    public static HeightRangePlacement uniform(VerticalAnchor minOffset, VerticalAnchor maxOffset) {
        return of(UniformHeight.of(minOffset, maxOffset));
    }

    public static HeightRangePlacement triangle(VerticalAnchor minOffset, VerticalAnchor maxOffset) {
        return of(TrapezoidHeight.of(minOffset, maxOffset));
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, Random random, BlockPos pos) {
        return Stream.of(pos.atY(this.height.sample(random, context)));
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.HEIGHT_RANGE;
    }
}
