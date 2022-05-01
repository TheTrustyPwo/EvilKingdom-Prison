package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class AxisAlignedLinearPosTest extends PosRuleTest {
    public static final Codec<AxisAlignedLinearPosTest> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("min_chance").orElse(0.0F).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.minChance;
        }), Codec.FLOAT.fieldOf("max_chance").orElse(0.0F).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.maxChance;
        }), Codec.INT.fieldOf("min_dist").orElse(0).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.minDist;
        }), Codec.INT.fieldOf("max_dist").orElse(0).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.maxDist;
        }), Direction.Axis.CODEC.fieldOf("axis").orElse(Direction.Axis.Y).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.axis;
        })).apply(instance, AxisAlignedLinearPosTest::new);
    });
    private final float minChance;
    private final float maxChance;
    private final int minDist;
    private final int maxDist;
    private final Direction.Axis axis;

    public AxisAlignedLinearPosTest(float minChance, float maxChance, int minDistance, int maxDistance, Direction.Axis axis) {
        if (minDistance >= maxDistance) {
            throw new IllegalArgumentException("Invalid range: [" + minDistance + "," + maxDistance + "]");
        } else {
            this.minChance = minChance;
            this.maxChance = maxChance;
            this.minDist = minDistance;
            this.maxDist = maxDistance;
            this.axis = axis;
        }
    }

    @Override
    public boolean test(BlockPos blockPos, BlockPos blockPos2, BlockPos pivot, Random random) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, this.axis);
        float f = (float)Math.abs((blockPos2.getX() - pivot.getX()) * direction.getStepX());
        float g = (float)Math.abs((blockPos2.getY() - pivot.getY()) * direction.getStepY());
        float h = (float)Math.abs((blockPos2.getZ() - pivot.getZ()) * direction.getStepZ());
        int i = (int)(f + g + h);
        float j = random.nextFloat();
        return j <= Mth.clampedLerp(this.minChance, this.maxChance, Mth.inverseLerp((float)i, (float)this.minDist, (float)this.maxDist));
    }

    @Override
    protected PosRuleTestType<?> getType() {
        return PosRuleTestType.AXIS_ALIGNED_LINEAR_POS_TEST;
    }
}
