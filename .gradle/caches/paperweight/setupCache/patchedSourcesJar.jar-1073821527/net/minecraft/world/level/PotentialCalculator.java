package net.minecraft.world.level;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;

public class PotentialCalculator {
    private final List<PotentialCalculator.PointCharge> charges = Lists.newArrayList();

    public void addCharge(BlockPos pos, double mass) {
        if (mass != 0.0D) {
            this.charges.add(new PotentialCalculator.PointCharge(pos, mass));
        }

    }

    public double getPotentialEnergyChange(BlockPos pos, double mass) {
        if (mass == 0.0D) {
            return 0.0D;
        } else {
            double d = 0.0D;

            for(PotentialCalculator.PointCharge pointCharge : this.charges) {
                d += pointCharge.getPotentialChange(pos);
            }

            return d * mass;
        }
    }

    static class PointCharge {
        private final BlockPos pos;
        private final double charge;

        public PointCharge(BlockPos pos, double mass) {
            this.pos = pos;
            this.charge = mass;
        }

        public double getPotentialChange(BlockPos pos) {
            double d = this.pos.distSqr(pos);
            return d == 0.0D ? Double.POSITIVE_INFINITY : this.charge / Math.sqrt(d);
        }
    }
}
