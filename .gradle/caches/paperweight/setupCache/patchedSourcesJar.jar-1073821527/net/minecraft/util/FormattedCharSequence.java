package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import net.minecraft.network.chat.Style;

@FunctionalInterface
public interface FormattedCharSequence {
    FormattedCharSequence EMPTY = (formattedCharSink) -> {
        return true;
    };

    boolean accept(FormattedCharSink visitor);

    static FormattedCharSequence codepoint(int codePoint, Style style) {
        return (visitor) -> {
            return visitor.accept(0, style, codePoint);
        };
    }

    static FormattedCharSequence forward(String string, Style style) {
        return string.isEmpty() ? EMPTY : (visitor) -> {
            return StringDecomposer.iterate(string, style, visitor);
        };
    }

    static FormattedCharSequence forward(String string, Style style, Int2IntFunction codePointMapper) {
        return string.isEmpty() ? EMPTY : (visitor) -> {
            return StringDecomposer.iterate(string, style, decorateOutput(visitor, codePointMapper));
        };
    }

    static FormattedCharSequence backward(String string, Style style) {
        return string.isEmpty() ? EMPTY : (visitor) -> {
            return StringDecomposer.iterateBackwards(string, style, visitor);
        };
    }

    static FormattedCharSequence backward(String string, Style style, Int2IntFunction codePointMapper) {
        return string.isEmpty() ? EMPTY : (visitor) -> {
            return StringDecomposer.iterateBackwards(string, style, decorateOutput(visitor, codePointMapper));
        };
    }

    static FormattedCharSink decorateOutput(FormattedCharSink visitor, Int2IntFunction codePointMapper) {
        return (charIndex, style, charPoint) -> {
            return visitor.accept(charIndex, style, codePointMapper.apply(Integer.valueOf(charPoint)));
        };
    }

    static FormattedCharSequence composite() {
        return EMPTY;
    }

    static FormattedCharSequence composite(FormattedCharSequence text) {
        return text;
    }

    static FormattedCharSequence composite(FormattedCharSequence first, FormattedCharSequence second) {
        return fromPair(first, second);
    }

    static FormattedCharSequence composite(FormattedCharSequence... texts) {
        return fromList(ImmutableList.copyOf(texts));
    }

    static FormattedCharSequence composite(List<FormattedCharSequence> texts) {
        int i = texts.size();
        switch(i) {
        case 0:
            return EMPTY;
        case 1:
            return texts.get(0);
        case 2:
            return fromPair(texts.get(0), texts.get(1));
        default:
            return fromList(ImmutableList.copyOf(texts));
        }
    }

    static FormattedCharSequence fromPair(FormattedCharSequence text1, FormattedCharSequence text2) {
        return (visitor) -> {
            return text1.accept(visitor) && text2.accept(visitor);
        };
    }

    static FormattedCharSequence fromList(List<FormattedCharSequence> texts) {
        return (visitor) -> {
            for(FormattedCharSequence formattedCharSequence : texts) {
                if (!formattedCharSequence.accept(visitor)) {
                    return false;
                }
            }

            return true;
        };
    }
}
