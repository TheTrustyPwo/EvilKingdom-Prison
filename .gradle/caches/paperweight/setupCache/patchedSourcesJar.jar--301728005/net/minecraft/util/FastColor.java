package net.minecraft.util;

public class FastColor {
    public static class ARGB32 {
        public static int alpha(int argb) {
            return argb >>> 24;
        }

        public static int red(int argb) {
            return argb >> 16 & 255;
        }

        public static int green(int argb) {
            return argb >> 8 & 255;
        }

        public static int blue(int argb) {
            return argb & 255;
        }

        public static int color(int alpha, int red, int green, int blue) {
            return alpha << 24 | red << 16 | green << 8 | blue;
        }

        public static int multiply(int first, int second) {
            return color(alpha(first) * alpha(second) / 255, red(first) * red(second) / 255, green(first) * green(second) / 255, blue(first) * blue(second) / 255);
        }
    }
}
