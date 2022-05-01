package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

public class SubStringSource {
    private final String plainText;
    private final List<Style> charStyles;
    private final Int2IntFunction reverseCharModifier;

    private SubStringSource(String string, List<Style> styles, Int2IntFunction reverser) {
        this.plainText = string;
        this.charStyles = ImmutableList.copyOf(styles);
        this.reverseCharModifier = reverser;
    }

    public String getPlainText() {
        return this.plainText;
    }

    public List<FormattedCharSequence> substring(int start, int length, boolean reverse) {
        if (length == 0) {
            return ImmutableList.of();
        } else {
            List<FormattedCharSequence> list = Lists.newArrayList();
            Style style = this.charStyles.get(start);
            int i = start;

            for(int j = 1; j < length; ++j) {
                int k = start + j;
                Style style2 = this.charStyles.get(k);
                if (!style2.equals(style)) {
                    String string = this.plainText.substring(i, k);
                    list.add(reverse ? FormattedCharSequence.backward(string, style, this.reverseCharModifier) : FormattedCharSequence.forward(string, style));
                    style = style2;
                    i = k;
                }
            }

            if (i < start + length) {
                String string2 = this.plainText.substring(i, start + length);
                list.add(reverse ? FormattedCharSequence.backward(string2, style, this.reverseCharModifier) : FormattedCharSequence.forward(string2, style));
            }

            return reverse ? Lists.reverse(list) : list;
        }
    }

    public static SubStringSource create(FormattedText visitable) {
        return create(visitable, (codePoint) -> {
            return codePoint;
        }, (string) -> {
            return string;
        });
    }

    public static SubStringSource create(FormattedText visitable, Int2IntFunction reverser, UnaryOperator<String> shaper) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Style> list = Lists.newArrayList();
        visitable.visit((style, text) -> {
            StringDecomposer.iterateFormatted(text, style, (charIndex, stylex, codePoint) -> {
                stringBuilder.appendCodePoint(codePoint);
                int i = Character.charCount(codePoint);

                for(int j = 0; j < i; ++j) {
                    list.add(stylex);
                }

                return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        return new SubStringSource(shaper.apply(stringBuilder.toString()), list, reverser);
    }
}
