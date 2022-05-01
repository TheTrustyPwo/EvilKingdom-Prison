package net.minecraft.util;

import java.util.Random;
import java.util.UUID;
import java.util.function.IntPredicate;
import net.minecraft.Util;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.math.NumberUtils;

public class Mth {
    private static final int BIG_ENOUGH_INT = 1024;
    private static final float BIG_ENOUGH_FLOAT = 1024.0F;
    private static final long UUID_VERSION = 61440L;
    private static final long UUID_VERSION_TYPE_4 = 16384L;
    private static final long UUID_VARIANT = -4611686018427387904L;
    private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
    public static final float PI = (float)Math.PI;
    public static final float HALF_PI = ((float)Math.PI / 2F);
    public static final float TWO_PI = ((float)Math.PI * 2F);
    public static final float DEG_TO_RAD = ((float)Math.PI / 180F);
    public static final float RAD_TO_DEG = (180F / (float)Math.PI);
    public static final float EPSILON = 1.0E-5F;
    public static final float SQRT_OF_TWO = sqrt(2.0F);
    private static final float SIN_SCALE = 10430.378F;
    private static final float[] SIN = Util.make(new float[65536], (sineTable) -> {
        for(int i = 0; i < sineTable.length; ++i) {
            sineTable[i] = (float)Math.sin((double)i * Math.PI * 2.0D / 65536.0D);
        }

    });
    private static final Random RANDOM = new Random();
    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};
    private static final double ONE_SIXTH = 0.16666666666666666D;
    private static final int FRAC_EXP = 8;
    private static final int LUT_SIZE = 257;
    private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
    private static final double[] ASIN_TAB = new double[257];
    private static final double[] COS_TAB = new double[257];

    public static float sin(float value) {
        return SIN[(int)(value * 10430.378F) & '\uffff'];
    }

    public static float cos(float value) {
        return SIN[(int)(value * 10430.378F + 16384.0F) & '\uffff'];
    }

    public static float sqrt(float value) {
        return (float)Math.sqrt((double)value);
    }

    public static int floor(float value) {
        int i = (int)value;
        return value < (float)i ? i - 1 : i;
    }

    public static int fastFloor(double value) {
        return (int)(value + 1024.0D) - 1024;
    }

    public static int floor(double value) {
        int i = (int)value;
        return value < (double)i ? i - 1 : i;
    }

    public static long lfloor(double value) {
        long l = (long)value;
        return value < (double)l ? l - 1L : l;
    }

    public static int absFloor(double value) {
        return (int)(value >= 0.0D ? value : -value + 1.0D);
    }

    public static float abs(float value) {
        return Math.abs(value);
    }

    public static int abs(int value) {
        return Math.abs(value);
    }

    public static int ceil(float value) {
        int i = (int)value;
        return value > (float)i ? i + 1 : i;
    }

    public static int ceil(double value) {
        int i = (int)value;
        return value > (double)i ? i + 1 : i;
    }

    public static byte clamp(byte value, byte min, byte max) {
        if (value < min) {
            return min;
        } else {
            return value > max ? max : value;
        }
    }

    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        } else {
            return value > max ? max : value;
        }
    }

    public static long clamp(long value, long min, long max) {
        if (value < min) {
            return min;
        } else {
            return value > max ? max : value;
        }
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        } else {
            return value > max ? max : value;
        }
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        } else {
            return value > max ? max : value;
        }
    }

    public static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0D) {
            return start;
        } else {
            return delta > 1.0D ? end : lerp(delta, start, end);
        }
    }

    public static float clampedLerp(float start, float end, float delta) {
        if (delta < 0.0F) {
            return start;
        } else {
            return delta > 1.0F ? end : lerp(delta, start, end);
        }
    }

    public static double absMax(double a, double b) {
        if (a < 0.0D) {
            a = -a;
        }

        if (b < 0.0D) {
            b = -b;
        }

        return a > b ? a : b;
    }

    public static int intFloorDiv(int dividend, int divisor) {
        return Math.floorDiv(dividend, divisor);
    }

    public static int nextInt(Random random, int min, int max) {
        return min >= max ? min : random.nextInt(max - min + 1) + min;
    }

    public static float nextFloat(Random random, float min, float max) {
        return min >= max ? min : random.nextFloat() * (max - min) + min;
    }

    public static double nextDouble(Random random, double min, double max) {
        return min >= max ? min : random.nextDouble() * (max - min) + min;
    }

    public static double average(long[] array) {
        long l = 0L;

        for(long m : array) {
            l += m;
        }

        return (double)l / (double)array.length;
    }

    public static boolean equal(float a, float b) {
        return Math.abs(b - a) < 1.0E-5F;
    }

    public static boolean equal(double a, double b) {
        return Math.abs(b - a) < (double)1.0E-5F;
    }

    public static int positiveModulo(int dividend, int divisor) {
        return Math.floorMod(dividend, divisor);
    }

    public static float positiveModulo(float dividend, float divisor) {
        return (dividend % divisor + divisor) % divisor;
    }

    public static double positiveModulo(double dividend, double divisor) {
        return (dividend % divisor + divisor) % divisor;
    }

    public static int wrapDegrees(int degrees) {
        int i = degrees % 360;
        if (i >= 180) {
            i -= 360;
        }

        if (i < -180) {
            i += 360;
        }

        return i;
    }

    public static float wrapDegrees(float degrees) {
        float f = degrees % 360.0F;
        if (f >= 180.0F) {
            f -= 360.0F;
        }

        if (f < -180.0F) {
            f += 360.0F;
        }

        return f;
    }

    public static double wrapDegrees(double degrees) {
        double d = degrees % 360.0D;
        if (d >= 180.0D) {
            d -= 360.0D;
        }

        if (d < -180.0D) {
            d += 360.0D;
        }

        return d;
    }

    public static float degreesDifference(float start, float end) {
        return wrapDegrees(end - start);
    }

    public static float degreesDifferenceAbs(float first, float second) {
        return abs(degreesDifference(first, second));
    }

    public static float rotateIfNecessary(float value, float mean, float delta) {
        float f = degreesDifference(value, mean);
        float g = clamp(f, -delta, delta);
        return mean - g;
    }

    public static float approach(float from, float to, float step) {
        step = abs(step);
        return from < to ? clamp(from + step, from, to) : clamp(from - step, to, from);
    }

    public static float approachDegrees(float from, float to, float step) {
        float f = degreesDifference(from, to);
        return approach(from, from + f, step);
    }

    public static int getInt(String string, int fallback) {
        return NumberUtils.toInt(string, fallback);
    }

    public static int getInt(String string, int fallback, int min) {
        return Math.max(min, getInt(string, fallback));
    }

    public static double getDouble(String string, double fallback) {
        try {
            return Double.parseDouble(string);
        } catch (Throwable var4) {
            return fallback;
        }
    }

    public static double getDouble(String string, double fallback, double min) {
        return Math.max(min, getDouble(string, fallback));
    }

    public static int smallestEncompassingPowerOfTwo(int value) {
        int i = value - 1;
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return i + 1;
    }

    public static boolean isPowerOfTwo(int value) {
        return value != 0 && (value & value - 1) == 0;
    }

    public static int ceillog2(int value) {
        value = isPowerOfTwo(value) ? value : smallestEncompassingPowerOfTwo(value);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)((long)value * 125613361L >> 27) & 31];
    }

    public static int log2(int value) {
        return ceillog2(value) - (isPowerOfTwo(value) ? 0 : 1);
    }

    public static int color(float r, float g, float b) {
        return color(floor(r * 255.0F), floor(g * 255.0F), floor(b * 255.0F));
    }

    public static int color(int r, int g, int b) {
        int i = (r << 8) + g;
        return (i << 8) + b;
    }

    public static int colorMultiply(int a, int b) {
        int i = (a & 16711680) >> 16;
        int j = (b & 16711680) >> 16;
        int k = (a & '\uff00') >> 8;
        int l = (b & '\uff00') >> 8;
        int m = (a & 255) >> 0;
        int n = (b & 255) >> 0;
        int o = (int)((float)i * (float)j / 255.0F);
        int p = (int)((float)k * (float)l / 255.0F);
        int q = (int)((float)m * (float)n / 255.0F);
        return a & -16777216 | o << 16 | p << 8 | q;
    }

    public static int colorMultiply(int color, float r, float g, float b) {
        int i = (color & 16711680) >> 16;
        int j = (color & '\uff00') >> 8;
        int k = (color & 255) >> 0;
        int l = (int)((float)i * r);
        int m = (int)((float)j * g);
        int n = (int)((float)k * b);
        return color & -16777216 | l << 16 | m << 8 | n;
    }

    public static float frac(float value) {
        return value - (float)floor(value);
    }

    public static double frac(double value) {
        return value - (double)lfloor(value);
    }

    public static Vec3 catmullRomSplinePos(Vec3 vec3, Vec3 vec32, Vec3 vec33, Vec3 vec34, double d) {
        double e = ((-d + 2.0D) * d - 1.0D) * d * 0.5D;
        double f = ((3.0D * d - 5.0D) * d * d + 2.0D) * 0.5D;
        double g = ((-3.0D * d + 4.0D) * d + 1.0D) * d * 0.5D;
        double h = (d - 1.0D) * d * d * 0.5D;
        return new Vec3(vec3.x * e + vec32.x * f + vec33.x * g + vec34.x * h, vec3.y * e + vec32.y * f + vec33.y * g + vec34.y * h, vec3.z * e + vec32.z * f + vec33.z * g + vec34.z * h);
    }

    public static long getSeed(Vec3i vec) {
        return getSeed(vec.getX(), vec.getY(), vec.getZ());
    }

    public static long getSeed(int x, int y, int z) {
        long l = (long)(x * 3129871) ^ (long)z * 116129781L ^ (long)y;
        l = l * l * 42317861L + l * 11L;
        return l >> 16;
    }

    public static UUID createInsecureUUID(Random random) {
        long l = random.nextLong() & -61441L | 16384L;
        long m = random.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
        return new UUID(l, m);
    }

    public static UUID createInsecureUUID() {
        return createInsecureUUID(RANDOM);
    }

    public static double inverseLerp(double value, double start, double end) {
        return (value - start) / (end - start);
    }

    public static float inverseLerp(float value, float start, float end) {
        return (value - start) / (end - start);
    }

    public static boolean rayIntersectsAABB(Vec3 vec3, Vec3 vec32, AABB aABB) {
        double d = (aABB.minX + aABB.maxX) * 0.5D;
        double e = (aABB.maxX - aABB.minX) * 0.5D;
        double f = vec3.x - d;
        if (Math.abs(f) > e && f * vec32.x >= 0.0D) {
            return false;
        } else {
            double g = (aABB.minY + aABB.maxY) * 0.5D;
            double h = (aABB.maxY - aABB.minY) * 0.5D;
            double i = vec3.y - g;
            if (Math.abs(i) > h && i * vec32.y >= 0.0D) {
                return false;
            } else {
                double j = (aABB.minZ + aABB.maxZ) * 0.5D;
                double k = (aABB.maxZ - aABB.minZ) * 0.5D;
                double l = vec3.z - j;
                if (Math.abs(l) > k && l * vec32.z >= 0.0D) {
                    return false;
                } else {
                    double m = Math.abs(vec32.x);
                    double n = Math.abs(vec32.y);
                    double o = Math.abs(vec32.z);
                    double p = vec32.y * l - vec32.z * i;
                    if (Math.abs(p) > h * o + k * n) {
                        return false;
                    } else {
                        p = vec32.z * f - vec32.x * l;
                        if (Math.abs(p) > e * o + k * m) {
                            return false;
                        } else {
                            p = vec32.x * i - vec32.y * f;
                            return Math.abs(p) < e * n + h * m;
                        }
                    }
                }
            }
        }
    }

    public static double atan2(double y, double x) {
        double d = x * x + y * y;
        if (Double.isNaN(d)) {
            return Double.NaN;
        } else {
            boolean bl = y < 0.0D;
            if (bl) {
                y = -y;
            }

            boolean bl2 = x < 0.0D;
            if (bl2) {
                x = -x;
            }

            boolean bl3 = y > x;
            if (bl3) {
                double e = x;
                x = y;
                y = e;
            }

            double f = fastInvSqrt(d);
            x *= f;
            y *= f;
            double g = FRAC_BIAS + y;
            int i = (int)Double.doubleToRawLongBits(g);
            double h = ASIN_TAB[i];
            double j = COS_TAB[i];
            double k = g - FRAC_BIAS;
            double l = y * j - x * k;
            double m = (6.0D + l * l) * l * 0.16666666666666666D;
            double n = h + m;
            if (bl3) {
                n = (Math.PI / 2D) - n;
            }

            if (bl2) {
                n = Math.PI - n;
            }

            if (bl) {
                n = -n;
            }

            return n;
        }
    }

    public static float fastInvSqrt(float x) {
        float f = 0.5F * x;
        int i = Float.floatToIntBits(x);
        i = 1597463007 - (i >> 1);
        x = Float.intBitsToFloat(i);
        return x * (1.5F - f * x * x);
    }

    public static double fastInvSqrt(double x) {
        double d = 0.5D * x;
        long l = Double.doubleToRawLongBits(x);
        l = 6910469410427058090L - (l >> 1);
        x = Double.longBitsToDouble(l);
        return x * (1.5D - d * x * x);
    }

    public static float fastInvCubeRoot(float x) {
        int i = Float.floatToIntBits(x);
        i = 1419967116 - i / 3;
        float f = Float.intBitsToFloat(i);
        f = 0.6666667F * f + 1.0F / (3.0F * f * f * x);
        return 0.6666667F * f + 1.0F / (3.0F * f * f * x);
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        int i = (int)(hue * 6.0F) % 6;
        float f = hue * 6.0F - (float)i;
        float g = value * (1.0F - saturation);
        float h = value * (1.0F - f * saturation);
        float j = value * (1.0F - (1.0F - f) * saturation);
        float k;
        float l;
        float m;
        switch(i) {
        case 0:
            k = value;
            l = j;
            m = g;
            break;
        case 1:
            k = h;
            l = value;
            m = g;
            break;
        case 2:
            k = g;
            l = value;
            m = j;
            break;
        case 3:
            k = g;
            l = h;
            m = value;
            break;
        case 4:
            k = j;
            l = g;
            m = value;
            break;
        case 5:
            k = value;
            l = g;
            m = h;
            break;
        default:
            throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        }

        int af = clamp((int)(k * 255.0F), 0, 255);
        int ag = clamp((int)(l * 255.0F), 0, 255);
        int ah = clamp((int)(m * 255.0F), 0, 255);
        return af << 16 | ag << 8 | ah;
    }

    public static int murmurHash3Mixer(int value) {
        value ^= value >>> 16;
        value *= -2048144789;
        value ^= value >>> 13;
        value *= -1028477387;
        return value ^ value >>> 16;
    }

    public static long murmurHash3Mixer(long value) {
        value ^= value >>> 33;
        value *= -49064778989728563L;
        value ^= value >>> 33;
        value *= -4265267296055464877L;
        return value ^ value >>> 33;
    }

    public static double[] cumulativeSum(double... values) {
        double d = 0.0D;

        for(double e : values) {
            d += e;
        }

        for(int i = 0; i < values.length; ++i) {
            values[i] /= d;
        }

        for(int j = 0; j < values.length; ++j) {
            values[j] += j == 0 ? 0.0D : values[j - 1];
        }

        return values;
    }

    public static int getRandomForDistributionIntegral(Random random, double[] ds) {
        double d = random.nextDouble();

        for(int i = 0; i < ds.length; ++i) {
            if (d < ds[i]) {
                return i;
            }
        }

        return ds.length;
    }

    public static double[] binNormalDistribution(double d, double e, double f, int i, int j) {
        double[] ds = new double[j - i + 1];
        int k = 0;

        for(int l = i; l <= j; ++l) {
            ds[k] = Math.max(0.0D, d * StrictMath.exp(-((double)l - f) * ((double)l - f) / (2.0D * e * e)));
            ++k;
        }

        return ds;
    }

    public static double[] binBiModalNormalDistribution(double d, double e, double f, double g, double h, double i, int j, int k) {
        double[] ds = new double[k - j + 1];
        int l = 0;

        for(int m = j; m <= k; ++m) {
            ds[l] = Math.max(0.0D, d * StrictMath.exp(-((double)m - f) * ((double)m - f) / (2.0D * e * e)) + g * StrictMath.exp(-((double)m - i) * ((double)m - i) / (2.0D * h * h)));
            ++l;
        }

        return ds;
    }

    public static double[] binLogDistribution(double d, double e, int i, int j) {
        double[] ds = new double[j - i + 1];
        int k = 0;

        for(int l = i; l <= j; ++l) {
            ds[k] = Math.max(d * StrictMath.log((double)l) + e, 0.0D);
            ++k;
        }

        return ds;
    }

    public static int binarySearch(int min, int max, IntPredicate predicate) {
        int i = max - min;

        while(i > 0) {
            int j = i / 2;
            int k = min + j;
            if (predicate.test(k)) {
                i = j;
            } else {
                min = k + 1;
                i -= j + 1;
            }
        }

        return min;
    }

    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static double lerp2(double deltaX, double deltaY, double x0y0, double x1y0, double x0y1, double x1y1) {
        return lerp(deltaY, lerp(deltaX, x0y0, x1y0), lerp(deltaX, x0y1, x1y1));
    }

    public static double lerp3(double deltaX, double deltaY, double deltaZ, double x0y0z0, double x1y0z0, double x0y1z0, double x1y1z0, double x0y0z1, double x1y0z1, double x0y1z1, double x1y1z1) {
        return lerp(deltaZ, lerp2(deltaX, deltaY, x0y0z0, x1y0z0, x0y1z0, x1y1z0), lerp2(deltaX, deltaY, x0y0z1, x1y0z1, x0y1z1, x1y1z1));
    }

    public static double smoothstep(double value) {
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    public static double smoothstepDerivative(double value) {
        return 30.0D * value * value * (value - 1.0D) * (value - 1.0D);
    }

    public static int sign(double value) {
        if (value == 0.0D) {
            return 0;
        } else {
            return value > 0.0D ? 1 : -1;
        }
    }

    public static float rotLerp(float delta, float start, float end) {
        return start + delta * wrapDegrees(end - start);
    }

    public static float diffuseLight(float f, float g, float h) {
        return Math.min(f * f * 0.6F + g * g * ((3.0F + g) / 4.0F) + h * h * 0.8F, 1.0F);
    }

    /** @deprecated */
    @Deprecated
    public static float rotlerp(float start, float end, float delta) {
        float f;
        for(f = end - start; f < -180.0F; f += 360.0F) {
        }

        while(f >= 180.0F) {
            f -= 360.0F;
        }

        return start + delta * f;
    }

    /** @deprecated */
    @Deprecated
    public static float rotWrap(double degrees) {
        while(degrees >= 180.0D) {
            degrees -= 360.0D;
        }

        while(degrees < -180.0D) {
            degrees += 360.0D;
        }

        return (float)degrees;
    }

    public static float triangleWave(float value, float maxDeviation) {
        return (Math.abs(value % maxDeviation - maxDeviation * 0.5F) - maxDeviation * 0.25F) / (maxDeviation * 0.25F);
    }

    public static float square(float n) {
        return n * n;
    }

    public static double square(double n) {
        return n * n;
    }

    public static int square(int n) {
        return n * n;
    }

    public static long square(long n) {
        return n * n;
    }

    public static double clampedMap(double lerpValue, double lerpStart, double lerpEnd, double start, double end) {
        return clampedLerp(start, end, inverseLerp(lerpValue, lerpStart, lerpEnd));
    }

    public static float clampedMap(float lerpValue, float lerpStart, float lerpEnd, float start, float end) {
        return clampedLerp(start, end, inverseLerp(lerpValue, lerpStart, lerpEnd));
    }

    public static double map(double lerpValue, double lerpStart, double lerpEnd, double start, double end) {
        return lerp(inverseLerp(lerpValue, lerpStart, lerpEnd), start, end);
    }

    public static float map(float lerpValue, float lerpStart, float lerpEnd, float start, float end) {
        return lerp(inverseLerp(lerpValue, lerpStart, lerpEnd), start, end);
    }

    public static double wobble(double d) {
        return d + (2.0D * (new Random((long)floor(d * 3000.0D))).nextDouble() - 1.0D) * 1.0E-7D / 2.0D;
    }

    public static int roundToward(int value, int divisor) {
        return positiveCeilDiv(value, divisor) * divisor;
    }

    public static int positiveCeilDiv(int a, int b) {
        return -Math.floorDiv(-a, b);
    }

    public static int randomBetweenInclusive(Random random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public static float randomBetween(Random random, float min, float max) {
        return random.nextFloat() * (max - min) + min;
    }

    public static float normal(Random random, float mean, float deviation) {
        return mean + (float)random.nextGaussian() * deviation;
    }

    public static double lengthSquared(double a, double b) {
        return a * a + b * b;
    }

    public static double length(double a, double b) {
        return Math.sqrt(lengthSquared(a, b));
    }

    public static double lengthSquared(double a, double b, double c) {
        return a * a + b * b + c * c;
    }

    public static double length(double a, double b, double c) {
        return Math.sqrt(lengthSquared(a, b, c));
    }

    public static int quantize(double a, int b) {
        return floor(a / (double)b) * b;
    }

    static {
        for(int i = 0; i < 257; ++i) {
            double d = (double)i / 256.0D;
            double e = Math.asin(d);
            COS_TAB[i] = Math.cos(e);
            ASIN_TAB[i] = e;
        }

    }
}
