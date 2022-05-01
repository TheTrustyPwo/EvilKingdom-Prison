package net.minecraft.util;

import net.minecraft.world.phys.Vec3;

public class CubicSampler {
    private static final int GAUSSIAN_SAMPLE_RADIUS = 2;
    private static final int GAUSSIAN_SAMPLE_BREADTH = 6;
    private static final double[] GAUSSIAN_SAMPLE_KERNEL = new double[]{0.0D, 1.0D, 4.0D, 6.0D, 4.0D, 1.0D, 0.0D};

    private CubicSampler() {
    }

    public static Vec3 gaussianSampleVec3(Vec3 pos, CubicSampler.Vec3Fetcher rgbFetcher) {
        int i = Mth.floor(pos.x());
        int j = Mth.floor(pos.y());
        int k = Mth.floor(pos.z());
        double d = pos.x() - (double)i;
        double e = pos.y() - (double)j;
        double f = pos.z() - (double)k;
        double g = 0.0D;
        Vec3 vec3 = Vec3.ZERO;

        for(int l = 0; l < 6; ++l) {
            double h = Mth.lerp(d, GAUSSIAN_SAMPLE_KERNEL[l + 1], GAUSSIAN_SAMPLE_KERNEL[l]);
            int m = i - 2 + l;

            for(int n = 0; n < 6; ++n) {
                double o = Mth.lerp(e, GAUSSIAN_SAMPLE_KERNEL[n + 1], GAUSSIAN_SAMPLE_KERNEL[n]);
                int p = j - 2 + n;

                for(int q = 0; q < 6; ++q) {
                    double r = Mth.lerp(f, GAUSSIAN_SAMPLE_KERNEL[q + 1], GAUSSIAN_SAMPLE_KERNEL[q]);
                    int s = k - 2 + q;
                    double t = h * o * r;
                    g += t;
                    vec3 = vec3.add(rgbFetcher.fetch(m, p, s).scale(t));
                }
            }
        }

        return vec3.scale(1.0D / g);
    }

    @FunctionalInterface
    public interface Vec3Fetcher {
        Vec3 fetch(int x, int y, int z);
    }
}
