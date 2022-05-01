package net.minecraft.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class StringUtil {
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    private static final Pattern LINE_PATTERN = Pattern.compile("\\r\\n|\\v");
    private static final Pattern LINE_END_PATTERN = Pattern.compile("(?:\\r\\n|\\v)$");

    public static String formatTickDuration(int ticks) {
        int i = ticks / 20;
        int j = i / 60;
        i %= 60;
        return i < 10 ? j + ":0" + i : j + ":" + i;
    }

    public static String stripColor(String text) {
        return STRIP_COLOR_PATTERN.matcher(text).replaceAll("");
    }

    public static boolean isNullOrEmpty(@Nullable String text) {
        return StringUtils.isEmpty(text);
    }

    public static String truncateStringIfNecessary(String text, int maxLength, boolean addEllipsis) {
        if (text.length() <= maxLength) {
            return text;
        } else {
            return addEllipsis && maxLength > 3 ? text.substring(0, maxLength - 3) + "..." : text.substring(0, maxLength);
        }
    }

    public static int lineCount(String text) {
        if (text.isEmpty()) {
            return 0;
        } else {
            Matcher matcher = LINE_PATTERN.matcher(text);

            int i;
            for(i = 1; matcher.find(); ++i) {
            }

            return i;
        }
    }

    public static boolean endsWithNewLine(String text) {
        return LINE_END_PATTERN.matcher(text).find();
    }
}
