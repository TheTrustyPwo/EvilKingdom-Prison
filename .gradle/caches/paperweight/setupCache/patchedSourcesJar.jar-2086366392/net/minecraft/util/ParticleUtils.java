package net.minecraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ParticleUtils {
    public static void spawnParticlesOnBlockFaces(Level world, BlockPos pos, ParticleOptions effect, UniformInt range) {
        for(Direction direction : Direction.values()) {
            int i = range.sample(world.random);

            for(int j = 0; j < i; ++j) {
                spawnParticleOnFace(world, pos, direction, effect);
            }
        }

    }

    public static void spawnParticlesAlongAxis(Direction.Axis axis, Level world, BlockPos pos, double variance, ParticleOptions effect, UniformInt range) {
        Vec3 vec3 = Vec3.atCenterOf(pos);
        boolean bl = axis == Direction.Axis.X;
        boolean bl2 = axis == Direction.Axis.Y;
        boolean bl3 = axis == Direction.Axis.Z;
        int i = range.sample(world.random);

        for(int j = 0; j < i; ++j) {
            double d = vec3.x + Mth.nextDouble(world.random, -1.0D, 1.0D) * (bl ? 0.5D : variance);
            double e = vec3.y + Mth.nextDouble(world.random, -1.0D, 1.0D) * (bl2 ? 0.5D : variance);
            double f = vec3.z + Mth.nextDouble(world.random, -1.0D, 1.0D) * (bl3 ? 0.5D : variance);
            double g = bl ? Mth.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
            double h = bl2 ? Mth.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
            double k = bl3 ? Mth.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
            world.addParticle(effect, d, e, f, g, h, k);
        }

    }

    public static void spawnParticleOnFace(Level world, BlockPos pos, Direction direction, ParticleOptions effect) {
        Vec3 vec3 = Vec3.atCenterOf(pos);
        int i = direction.getStepX();
        int j = direction.getStepY();
        int k = direction.getStepZ();
        double d = vec3.x + (i == 0 ? Mth.nextDouble(world.random, -0.5D, 0.5D) : (double)i * 0.55D);
        double e = vec3.y + (j == 0 ? Mth.nextDouble(world.random, -0.5D, 0.5D) : (double)j * 0.55D);
        double f = vec3.z + (k == 0 ? Mth.nextDouble(world.random, -0.5D, 0.5D) : (double)k * 0.55D);
        double g = i == 0 ? Mth.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
        double h = j == 0 ? Mth.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
        double l = k == 0 ? Mth.nextDouble(world.random, -1.0D, 1.0D) : 0.0D;
        world.addParticle(effect, d, e, f, g, h, l);
    }
}
