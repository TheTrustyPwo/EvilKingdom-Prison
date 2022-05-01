package net.minecraft.util;

public class SmoothDouble {
    private double targetValue;
    private double remainingValue;
    private double lastAmount;

    public double getNewDeltaValue(double original, double smoother) {
        this.targetValue += original;
        double d = this.targetValue - this.remainingValue;
        double e = Mth.lerp(0.5D, this.lastAmount, d);
        double f = Math.signum(d);
        if (f * d > f * this.lastAmount) {
            d = e;
        }

        this.lastAmount = e;
        this.remainingValue += d * smoother;
        return d * smoother;
    }

    public void reset() {
        this.targetValue = 0.0D;
        this.remainingValue = 0.0D;
        this.lastAmount = 0.0D;
    }
}
