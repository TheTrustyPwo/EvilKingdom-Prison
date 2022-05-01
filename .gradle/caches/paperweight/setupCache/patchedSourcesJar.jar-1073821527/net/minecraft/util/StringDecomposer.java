package net.minecraft.util;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class StringDecomposer {
    private static final char REPLACEMENT_CHAR = '\ufffd';
    private static final Optional<Object> STOP_ITERATION = Optional.of(Unit.INSTANCE);

    private static boolean feedChar(Style style, FormattedCharSink visitor, int index, char c) {
        return Character.isSurrogate(c) ? visitor.accept(index, style, 65533) : visitor.accept(index, style, c);
    }

    public static boolean iterate(String text, Style style, FormattedCharSink visitor) {
        int i = text.length();

        for(int j = 0; j < i; ++j) {
            char c = text.charAt(j);
            if (Character.isHighSurrogate(c)) {
                if (j + 1 >= i) {
                    if (!visitor.accept(j, style, 65533)) {
                        return false;
                    }
                    break;
                }

                char d = text.charAt(j + 1);
                if (Character.isLowSurrogate(d)) {
                    if (!visitor.accept(j, style, Character.toCodePoint(c, d))) {
                        return false;
                    }

                    ++j;
                } else if (!visitor.accept(j, style, 65533)) {
                    return false;
                }
            } else if (!feedChar(style, visitor, j, c)) {
                return false;
            }
        }

        return true;
    }

    public static boolean iterateBackwards(String text, Style style, FormattedCharSink visitor) {
        int i = text.length();

        for(int j = i - 1; j >= 0; --j) {
            char c = text.charAt(j);
            if (Character.isLowSurrogate(c)) {
                if (j - 1 < 0) {
                    if (!visitor.accept(0, style, 65533)) {
                        return false;
                    }
                    break;
                }

                char d = text.charAt(j - 1);
                if (Character.isHighSurrogate(d)) {
                    --j;
                    if (!visitor.accept(j, style, Character.toCodePoint(d, c))) {
                        return false;
                    }
                } else if (!visitor.accept(j, style, 65533)) {
                    return false;
                }
            } else if (!feedChar(style, visitor, j, c)) {
                return false;
            }
        }

        return true;
    }

    public static boolean iterateFormatted(String text, Style style, FormattedCharSink visitor) {
        return iterateFormatted(text, 0, style, visitor);
    }

    public static boolean iterateFormatted(String text, int startIndex, Style style, FormattedCharSink visitor) {
        return iterateFormatted(text, startIndex, style, style, visitor);
    }

    public static boolean iterateFormatted(String text, int startIndex, Style startingStyle, Style resetStyle, FormattedCharSink visitor) {
        int i = text.length();
        Style style = startingStyle;

        for(int j = startIndex; j < i; ++j) {
            char c = text.charAt(j);
            if (c == 167) {
                if (j + 1 >= i) {
                    break;
                }

                char d = text.charAt(j + 1);
                ChatFormatting chatFormatting = ChatFormatting.getByCode(d);
                if (chatFormatting != null) {
                    style = chatFormatting == ChatFormatting.RESET ? resetStyle : style.applyLegacyFormat(chatFormatting);
                }

                ++j;
            } else if (Character.isHighSurrogate(c)) {
                if (j + 1 >= i) {
                    if (!visitor.accept(j, style, 65533)) {
                        return false;
                    }
                    break;
                }

                char e = text.charAt(j + 1);
                if (Character.isLowSurrogate(e)) {
                    if (!visitor.accept(j, style, Character.toCodePoint(c, e))) {
                        return false;
                    }

                    ++j;
                } else if (!visitor.accept(j, style, 65533)) {
                    return false;
                }
            } else if (!feedChar(style, visitor, j, c)) {
                return false;
            }
        }

        return true;
    }

    public static boolean iterateFormatted(FormattedText text, Style style, FormattedCharSink visitor) {
        return !text.visit((stylex, string) -> {
            return iterateFormatted(string, 0, stylex, visitor) ? Optional.empty() : STOP_ITERATION;
        }, style).isPresent();
    }

    public static String filterBrokenSurrogates(String text) {
        StringBuilder stringBuilder = new StringBuilder();
        iterate(text, Style.EMPTY, (index, style, codePoint) -> {
            stringBuilder.appendCodePoint(codePoint);
            return true;
        });
        return stringBuilder.toString();
    }

    public static String getPlainText(FormattedText text) {
        StringBuilder stringBuilder = new StringBuilder();
        iterateFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            stringBuilder.appendCodePoint(codePoint);
            return true;
        });
        return stringBuilder.toString();
    }
}
