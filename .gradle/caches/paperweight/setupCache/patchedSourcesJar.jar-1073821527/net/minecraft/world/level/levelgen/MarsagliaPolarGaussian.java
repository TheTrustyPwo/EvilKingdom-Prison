package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;

public class MarsagliaPolarGaussian {
    public final RandomSource randomSource;
    private double nextNextGaussian;
    private boolean haveNextNextGaussian;

    public MarsagliaPolarGaussian(RandomSource baseRandom) {
        this.randomSource = baseRandom;
    }

    public void reset() {
        this.haveNextNextGaussian = false;
    }

    public double nextGaussian() {
        if (this.haveNextNextGaussian) {
            this.haveNextNextGaussian = false;
            return this.nextNextGaussian;
        } else {
            while(true) {
                double d = 2.0D * this.randomSource.nextDouble() - 1.0D;
                double e = 2.0D * this.randomSource.nextDouble() - 1.0D;
                double f = Mth.square(d) + Mth.square(e);
                if (!(f >= 1.0D)) {
                    if (f != 0.0D) {
                        double g = Math.sqrt(-2.0D * Math.log(f) / f);
                        this.nextNextGaussian = e * g;
                        this.haveNextNextGaussian = true;
                        return d * g;
                    }
                }
            }
        }
    }
}
