package net.minecraft.world.level.levelgen.synth;

public class NoiseUtils {
    public static double biasTowardsExtreme(double d, double e) {
        return d + Math.sin(Math.PI * d) * e / Math.PI;
    }

    public static void parityNoiseOctaveConfigString(StringBuilder builder, double originX, double originY, double originZ, byte[] permutations) {
        builder.append(String.format("xo=%.3f, yo=%.3f, zo=%.3f, p0=%d, p255=%d", (float)originX, (float)originY, (float)originZ, permutations[0], permutations[255]));
    }

    public static void parityNoiseOctaveConfigString(StringBuilder builder, double originX, double originY, double originZ, int[] permutations) {
        builder.append(String.format("xo=%.3f, yo=%.3f, zo=%.3f, p0=%d, p255=%d", (float)originX, (float)originY, (float)originZ, permutations[0], permutations[255]));
    }
}
