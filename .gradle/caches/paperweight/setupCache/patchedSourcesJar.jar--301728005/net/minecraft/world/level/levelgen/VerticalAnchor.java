package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.dimension.DimensionType;

public interface VerticalAnchor {
    Codec<VerticalAnchor> CODEC = ExtraCodecs.xor(VerticalAnchor.Absolute.CODEC, ExtraCodecs.xor(VerticalAnchor.AboveBottom.CODEC, VerticalAnchor.BelowTop.CODEC)).xmap(VerticalAnchor::merge, VerticalAnchor::split);
    VerticalAnchor BOTTOM = aboveBottom(0);
    VerticalAnchor TOP = belowTop(0);

    static VerticalAnchor absolute(int offset) {
        return new VerticalAnchor.Absolute(offset);
    }

    static VerticalAnchor aboveBottom(int offset) {
        return new VerticalAnchor.AboveBottom(offset);
    }

    static VerticalAnchor belowTop(int offset) {
        return new VerticalAnchor.BelowTop(offset);
    }

    static VerticalAnchor bottom() {
        return BOTTOM;
    }

    static VerticalAnchor top() {
        return TOP;
    }

    private static VerticalAnchor merge(Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> either) {
        return either.map(Function.identity(), (eitherx) -> {
            return eitherx.map(Function.identity(), Function.identity());
        });
    }

    private static Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> split(VerticalAnchor yOffset) {
        return yOffset instanceof VerticalAnchor.Absolute ? Either.left((VerticalAnchor.Absolute)yOffset) : Either.right(yOffset instanceof VerticalAnchor.AboveBottom ? Either.left((VerticalAnchor.AboveBottom)yOffset) : Either.right((VerticalAnchor.BelowTop)yOffset));
    }

    int resolveY(WorldGenerationContext context);

    public static record AboveBottom(int offset) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.AboveBottom> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("above_bottom").xmap(VerticalAnchor.AboveBottom::new, VerticalAnchor.AboveBottom::offset).codec();

        @Override
        public int resolveY(WorldGenerationContext context) {
            return context.getMinGenY() + this.offset;
        }

        @Override
        public String toString() {
            return this.offset + " above bottom";
        }
    }

    public static record Absolute(int y) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.Absolute> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("absolute").xmap(VerticalAnchor.Absolute::new, VerticalAnchor.Absolute::y).codec();

        @Override
        public int resolveY(WorldGenerationContext context) {
            return this.y;
        }

        @Override
        public String toString() {
            return this.y + " absolute";
        }
    }

    public static record BelowTop(int offset) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.BelowTop> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("below_top").xmap(VerticalAnchor.BelowTop::new, VerticalAnchor.BelowTop::offset).codec();

        @Override
        public int resolveY(WorldGenerationContext context) {
            return context.getGenDepth() - 1 + context.getMinGenY() - this.offset;
        }

        @Override
        public String toString() {
            return this.offset + " below top";
        }
    }
}
