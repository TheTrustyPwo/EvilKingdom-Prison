package net.minecraft.world.level.levelgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseRouterData {
    private static final float ORE_THICKNESS = 0.08F;
    private static final double VEININESS_FREQUENCY = 1.5D;
    private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5D;
    private static final double SURFACE_DENSITY_THRESHOLD = 1.5625D;
    private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0D);
    private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
    private static final ResourceKey<DensityFunction> ZERO = createKey("zero");
    private static final ResourceKey<DensityFunction> Y = createKey("y");
    private static final ResourceKey<DensityFunction> SHIFT_X = createKey("shift_x");
    private static final ResourceKey<DensityFunction> SHIFT_Z = createKey("shift_z");
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE = createKey("overworld/base_3d_noise");
    private static final ResourceKey<DensityFunction> CONTINENTS = createKey("overworld/continents");
    private static final ResourceKey<DensityFunction> EROSION = createKey("overworld/erosion");
    private static final ResourceKey<DensityFunction> RIDGES = createKey("overworld/ridges");
    private static final ResourceKey<DensityFunction> FACTOR = createKey("overworld/factor");
    private static final ResourceKey<DensityFunction> DEPTH = createKey("overworld/depth");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE = createKey("overworld/sloped_cheese");
    private static final ResourceKey<DensityFunction> CONTINENTS_LARGE = createKey("overworld_large_biomes/continents");
    private static final ResourceKey<DensityFunction> EROSION_LARGE = createKey("overworld_large_biomes/erosion");
    private static final ResourceKey<DensityFunction> FACTOR_LARGE = createKey("overworld_large_biomes/factor");
    private static final ResourceKey<DensityFunction> DEPTH_LARGE = createKey("overworld_large_biomes/depth");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE_LARGE = createKey("overworld_large_biomes/sloped_cheese");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE_END = createKey("end/sloped_cheese");
    private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = createKey("overworld/caves/spaghetti_roughness_function");
    private static final ResourceKey<DensityFunction> ENTRANCES = createKey("overworld/caves/entrances");
    private static final ResourceKey<DensityFunction> NOODLE = createKey("overworld/caves/noodle");
    private static final ResourceKey<DensityFunction> PILLARS = createKey("overworld/caves/pillars");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = createKey("overworld/caves/spaghetti_2d_thickness_modulator");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D = createKey("overworld/caves/spaghetti_2d");

    protected static NoiseRouterWithOnlyNoises overworld(NoiseSettings noiseSettings, boolean bl) {
        return overworldWithNewCaves(noiseSettings, bl);
    }

    private static ResourceKey<DensityFunction> createKey(String id) {
        return ResourceKey.create(Registry.DENSITY_FUNCTION_REGISTRY, new ResourceLocation(id));
    }

    public static Holder<? extends DensityFunction> bootstrap() {
        register(ZERO, DensityFunctions.zero());
        int i = DimensionType.MIN_Y * 2;
        int j = DimensionType.MAX_Y * 2;
        register(Y, DensityFunctions.yClampedGradient(i, j, (double)i, (double)j));
        DensityFunction densityFunction = register(SHIFT_X, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftA(getNoise(Noises.SHIFT)))));
        DensityFunction densityFunction2 = register(SHIFT_Z, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftB(getNoise(Noises.SHIFT)))));
        register(BASE_3D_NOISE, BlendedNoise.UNSEEDED);
        DensityFunction densityFunction3 = register(CONTINENTS, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.CONTINENTALNESS))));
        DensityFunction densityFunction4 = register(EROSION, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.EROSION))));
        DensityFunction densityFunction5 = register(RIDGES, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.RIDGE))));
        DensityFunction densityFunction6 = DensityFunctions.noise(getNoise(Noises.JAGGED), 1500.0D, 0.0D);
        DensityFunction densityFunction7 = splineWithBlending(densityFunction3, densityFunction4, densityFunction5, DensityFunctions.TerrainShaperSpline.SplineType.OFFSET, -0.81D, 2.5D, DensityFunctions.blendOffset());
        DensityFunction densityFunction8 = register(FACTOR, splineWithBlending(densityFunction3, densityFunction4, densityFunction5, DensityFunctions.TerrainShaperSpline.SplineType.FACTOR, 0.0D, 8.0D, BLENDING_FACTOR));
        DensityFunction densityFunction9 = register(DEPTH, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D), densityFunction7));
        register(SLOPED_CHEESE, slopedCheese(densityFunction3, densityFunction4, densityFunction5, densityFunction8, densityFunction9, densityFunction6));
        DensityFunction densityFunction10 = register(CONTINENTS_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.CONTINENTALNESS_LARGE))));
        DensityFunction densityFunction11 = register(EROSION_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.EROSION_LARGE))));
        DensityFunction densityFunction12 = splineWithBlending(densityFunction10, densityFunction11, densityFunction5, DensityFunctions.TerrainShaperSpline.SplineType.OFFSET, -0.81D, 2.5D, DensityFunctions.blendOffset());
        DensityFunction densityFunction13 = register(FACTOR_LARGE, splineWithBlending(densityFunction10, densityFunction11, densityFunction5, DensityFunctions.TerrainShaperSpline.SplineType.FACTOR, 0.0D, 8.0D, BLENDING_FACTOR));
        DensityFunction densityFunction14 = register(DEPTH_LARGE, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D), densityFunction12));
        register(SLOPED_CHEESE_LARGE, slopedCheese(densityFunction10, densityFunction11, densityFunction5, densityFunction13, densityFunction14, densityFunction6));
        register(SLOPED_CHEESE_END, DensityFunctions.add(DensityFunctions.endIslands(0L), getFunction(BASE_3D_NOISE)));
        register(SPAGHETTI_ROUGHNESS_FUNCTION, spaghettiRoughnessFunction());
        register(SPAGHETTI_2D_THICKNESS_MODULATOR, DensityFunctions.cacheOnce(DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_2D_THICKNESS), 2.0D, 1.0D, -0.6D, -1.3D)));
        register(SPAGHETTI_2D, spaghetti2D());
        register(ENTRANCES, entrances());
        register(NOODLE, noodle());
        register(PILLARS, pillars());
        return BuiltinRegistries.DENSITY_FUNCTION.holders().iterator().next();
    }

    private static DensityFunction register(ResourceKey<DensityFunction> resourceKey, DensityFunction densityFunction) {
        return new DensityFunctions.HolderHolder(BuiltinRegistries.register(BuiltinRegistries.DENSITY_FUNCTION, resourceKey, densityFunction));
    }

    private static Holder<NormalNoise.NoiseParameters> getNoise(ResourceKey<NormalNoise.NoiseParameters> resourceKey) {
        return BuiltinRegistries.NOISE.getHolderOrThrow(resourceKey);
    }

    private static DensityFunction getFunction(ResourceKey<DensityFunction> resourceKey) {
        return new DensityFunctions.HolderHolder(BuiltinRegistries.DENSITY_FUNCTION.getHolderOrThrow(resourceKey));
    }

    private static DensityFunction slopedCheese(DensityFunction densityFunction, DensityFunction densityFunction2, DensityFunction densityFunction3, DensityFunction densityFunction4, DensityFunction densityFunction5, DensityFunction densityFunction6) {
        DensityFunction densityFunction7 = splineWithBlending(densityFunction, densityFunction2, densityFunction3, DensityFunctions.TerrainShaperSpline.SplineType.JAGGEDNESS, 0.0D, 1.28D, BLENDING_JAGGEDNESS);
        DensityFunction densityFunction8 = DensityFunctions.mul(densityFunction7, densityFunction6.halfNegative());
        DensityFunction densityFunction9 = noiseGradientDensity(densityFunction4, DensityFunctions.add(densityFunction5, densityFunction8));
        return DensityFunctions.add(densityFunction9, getFunction(BASE_3D_NOISE));
    }

    private static DensityFunction spaghettiRoughnessFunction() {
        DensityFunction densityFunction = DensityFunctions.noise(getNoise(Noises.SPAGHETTI_ROUGHNESS));
        DensityFunction densityFunction2 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_ROUGHNESS_MODULATOR), 0.0D, -0.1D);
        return DensityFunctions.cacheOnce(DensityFunctions.mul(densityFunction2, DensityFunctions.add(densityFunction.abs(), DensityFunctions.constant(-0.4D))));
    }

    private static DensityFunction entrances() {
        DensityFunction densityFunction = DensityFunctions.cacheOnce(DensityFunctions.noise(getNoise(Noises.SPAGHETTI_3D_RARITY), 2.0D, 1.0D));
        DensityFunction densityFunction2 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_3D_THICKNESS), -0.065D, -0.088D);
        DensityFunction densityFunction3 = DensityFunctions.weirdScaledSampler(densityFunction, getNoise(Noises.SPAGHETTI_3D_1), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
        DensityFunction densityFunction4 = DensityFunctions.weirdScaledSampler(densityFunction, getNoise(Noises.SPAGHETTI_3D_2), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
        DensityFunction densityFunction5 = DensityFunctions.add(DensityFunctions.max(densityFunction3, densityFunction4), densityFunction2).clamp(-1.0D, 1.0D);
        DensityFunction densityFunction6 = getFunction(SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction densityFunction7 = DensityFunctions.noise(getNoise(Noises.CAVE_ENTRANCE), 0.75D, 0.5D);
        DensityFunction densityFunction8 = DensityFunctions.add(DensityFunctions.add(densityFunction7, DensityFunctions.constant(0.37D)), DensityFunctions.yClampedGradient(-10, 30, 0.3D, 0.0D));
        return DensityFunctions.cacheOnce(DensityFunctions.min(densityFunction8, DensityFunctions.add(densityFunction6, densityFunction5)));
    }

    private static DensityFunction noodle() {
        DensityFunction densityFunction = getFunction(Y);
        int i = -64;
        int j = -60;
        int k = 320;
        DensityFunction densityFunction2 = yLimitedInterpolatable(densityFunction, DensityFunctions.noise(getNoise(Noises.NOODLE), 1.0D, 1.0D), -60, 320, -1);
        DensityFunction densityFunction3 = yLimitedInterpolatable(densityFunction, DensityFunctions.mappedNoise(getNoise(Noises.NOODLE_THICKNESS), 1.0D, 1.0D, -0.05D, -0.1D), -60, 320, 0);
        double d = 2.6666666666666665D;
        DensityFunction densityFunction4 = yLimitedInterpolatable(densityFunction, DensityFunctions.noise(getNoise(Noises.NOODLE_RIDGE_A), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
        DensityFunction densityFunction5 = yLimitedInterpolatable(densityFunction, DensityFunctions.noise(getNoise(Noises.NOODLE_RIDGE_B), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
        DensityFunction densityFunction6 = DensityFunctions.mul(DensityFunctions.constant(1.5D), DensityFunctions.max(densityFunction4.abs(), densityFunction5.abs()));
        return DensityFunctions.rangeChoice(densityFunction2, -1000000.0D, 0.0D, DensityFunctions.constant(64.0D), DensityFunctions.add(densityFunction3, densityFunction6));
    }

    private static DensityFunction pillars() {
        double d = 25.0D;
        double e = 0.3D;
        DensityFunction densityFunction = DensityFunctions.noise(getNoise(Noises.PILLAR), 25.0D, 0.3D);
        DensityFunction densityFunction2 = DensityFunctions.mappedNoise(getNoise(Noises.PILLAR_RARENESS), 0.0D, -2.0D);
        DensityFunction densityFunction3 = DensityFunctions.mappedNoise(getNoise(Noises.PILLAR_THICKNESS), 0.0D, 1.1D);
        DensityFunction densityFunction4 = DensityFunctions.add(DensityFunctions.mul(densityFunction, DensityFunctions.constant(2.0D)), densityFunction2);
        return DensityFunctions.cacheOnce(DensityFunctions.mul(densityFunction4, densityFunction3.cube()));
    }

    private static DensityFunction spaghetti2D() {
        DensityFunction densityFunction = DensityFunctions.noise(getNoise(Noises.SPAGHETTI_2D_MODULATOR), 2.0D, 1.0D);
        DensityFunction densityFunction2 = DensityFunctions.weirdScaledSampler(densityFunction, getNoise(Noises.SPAGHETTI_2D), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE2);
        DensityFunction densityFunction3 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_2D_ELEVATION), 0.0D, (double)Math.floorDiv(-64, 8), 8.0D);
        DensityFunction densityFunction4 = getFunction(SPAGHETTI_2D_THICKNESS_MODULATOR);
        DensityFunction densityFunction5 = DensityFunctions.add(densityFunction3, DensityFunctions.yClampedGradient(-64, 320, 8.0D, -40.0D)).abs();
        DensityFunction densityFunction6 = DensityFunctions.add(densityFunction5, densityFunction4).cube();
        double d = 0.083D;
        DensityFunction densityFunction7 = DensityFunctions.add(densityFunction2, DensityFunctions.mul(DensityFunctions.constant(0.083D), densityFunction4));
        return DensityFunctions.max(densityFunction7, densityFunction6).clamp(-1.0D, 1.0D);
    }

    private static DensityFunction underground(DensityFunction densityFunction) {
        DensityFunction densityFunction2 = getFunction(SPAGHETTI_2D);
        DensityFunction densityFunction3 = getFunction(SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction densityFunction4 = DensityFunctions.noise(getNoise(Noises.CAVE_LAYER), 8.0D);
        DensityFunction densityFunction5 = DensityFunctions.mul(DensityFunctions.constant(4.0D), densityFunction4.square());
        DensityFunction densityFunction6 = DensityFunctions.noise(getNoise(Noises.CAVE_CHEESE), 0.6666666666666666D);
        DensityFunction densityFunction7 = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27D), densityFunction6).clamp(-1.0D, 1.0D), DensityFunctions.add(DensityFunctions.constant(1.5D), DensityFunctions.mul(DensityFunctions.constant(-0.64D), densityFunction)).clamp(0.0D, 0.5D));
        DensityFunction densityFunction8 = DensityFunctions.add(densityFunction5, densityFunction7);
        DensityFunction densityFunction9 = DensityFunctions.min(DensityFunctions.min(densityFunction8, getFunction(ENTRANCES)), DensityFunctions.add(densityFunction2, densityFunction3));
        DensityFunction densityFunction10 = getFunction(PILLARS);
        DensityFunction densityFunction11 = DensityFunctions.rangeChoice(densityFunction10, -1000000.0D, 0.03D, DensityFunctions.constant(-1000000.0D), densityFunction10);
        return DensityFunctions.max(densityFunction9, densityFunction11);
    }

    private static DensityFunction postProcess(NoiseSettings noiseSettings, DensityFunction densityFunction) {
        DensityFunction densityFunction2 = DensityFunctions.slide(noiseSettings, densityFunction);
        DensityFunction densityFunction3 = DensityFunctions.blendDensity(densityFunction2);
        return DensityFunctions.mul(DensityFunctions.interpolated(densityFunction3), DensityFunctions.constant(0.64D)).squeeze();
    }

    private static NoiseRouterWithOnlyNoises overworldWithNewCaves(NoiseSettings noiseSettings, boolean bl) {
        DensityFunction densityFunction = DensityFunctions.noise(getNoise(Noises.AQUIFER_BARRIER), 0.5D);
        DensityFunction densityFunction2 = DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67D);
        DensityFunction densityFunction3 = DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143D);
        DensityFunction densityFunction4 = DensityFunctions.noise(getNoise(Noises.AQUIFER_LAVA));
        DensityFunction densityFunction5 = getFunction(SHIFT_X);
        DensityFunction densityFunction6 = getFunction(SHIFT_Z);
        DensityFunction densityFunction7 = DensityFunctions.shiftedNoise2d(densityFunction5, densityFunction6, 0.25D, getNoise(bl ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE));
        DensityFunction densityFunction8 = DensityFunctions.shiftedNoise2d(densityFunction5, densityFunction6, 0.25D, getNoise(bl ? Noises.VEGETATION_LARGE : Noises.VEGETATION));
        DensityFunction densityFunction9 = getFunction(bl ? FACTOR_LARGE : FACTOR);
        DensityFunction densityFunction10 = getFunction(bl ? DEPTH_LARGE : DEPTH);
        DensityFunction densityFunction11 = noiseGradientDensity(DensityFunctions.cache2d(densityFunction9), densityFunction10);
        DensityFunction densityFunction12 = getFunction(bl ? SLOPED_CHEESE_LARGE : SLOPED_CHEESE);
        DensityFunction densityFunction13 = DensityFunctions.min(densityFunction12, DensityFunctions.mul(DensityFunctions.constant(5.0D), getFunction(ENTRANCES)));
        DensityFunction densityFunction14 = DensityFunctions.rangeChoice(densityFunction12, -1000000.0D, 1.5625D, densityFunction13, underground(densityFunction12));
        DensityFunction densityFunction15 = DensityFunctions.min(postProcess(noiseSettings, densityFunction14), getFunction(NOODLE));
        DensityFunction densityFunction16 = getFunction(Y);
        int i = noiseSettings.minY();
        int j = Stream.of(OreVeinifier.VeinType.values()).mapToInt((veinType) -> {
            return veinType.minY;
        }).min().orElse(i);
        int k = Stream.of(OreVeinifier.VeinType.values()).mapToInt((veinType) -> {
            return veinType.maxY;
        }).max().orElse(i);
        DensityFunction densityFunction17 = yLimitedInterpolatable(densityFunction16, DensityFunctions.noise(getNoise(Noises.ORE_VEININESS), 1.5D, 1.5D), j, k, 0);
        float f = 4.0F;
        DensityFunction densityFunction18 = yLimitedInterpolatable(densityFunction16, DensityFunctions.noise(getNoise(Noises.ORE_VEIN_A), 4.0D, 4.0D), j, k, 0).abs();
        DensityFunction densityFunction19 = yLimitedInterpolatable(densityFunction16, DensityFunctions.noise(getNoise(Noises.ORE_VEIN_B), 4.0D, 4.0D), j, k, 0).abs();
        DensityFunction densityFunction20 = DensityFunctions.add(DensityFunctions.constant((double)-0.08F), DensityFunctions.max(densityFunction18, densityFunction19));
        DensityFunction densityFunction21 = DensityFunctions.noise(getNoise(Noises.ORE_GAP));
        return new NoiseRouterWithOnlyNoises(densityFunction, densityFunction2, densityFunction3, densityFunction4, densityFunction7, densityFunction8, getFunction(bl ? CONTINENTS_LARGE : CONTINENTS), getFunction(bl ? EROSION_LARGE : EROSION), getFunction(bl ? DEPTH_LARGE : DEPTH), getFunction(RIDGES), densityFunction11, densityFunction15, densityFunction17, densityFunction20, densityFunction21);
    }

    private static NoiseRouterWithOnlyNoises noNewCaves(NoiseSettings noiseSettings) {
        DensityFunction densityFunction = getFunction(SHIFT_X);
        DensityFunction densityFunction2 = getFunction(SHIFT_Z);
        DensityFunction densityFunction3 = DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.TEMPERATURE));
        DensityFunction densityFunction4 = DensityFunctions.shiftedNoise2d(densityFunction, densityFunction2, 0.25D, getNoise(Noises.VEGETATION));
        DensityFunction densityFunction5 = noiseGradientDensity(DensityFunctions.cache2d(getFunction(FACTOR)), getFunction(DEPTH));
        DensityFunction densityFunction6 = postProcess(noiseSettings, getFunction(SLOPED_CHEESE));
        return new NoiseRouterWithOnlyNoises(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), densityFunction3, densityFunction4, getFunction(CONTINENTS), getFunction(EROSION), getFunction(DEPTH), getFunction(RIDGES), densityFunction5, densityFunction6, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
    }

    protected static NoiseRouterWithOnlyNoises overworldWithoutCaves(NoiseSettings noiseSettings) {
        return noNewCaves(noiseSettings);
    }

    protected static NoiseRouterWithOnlyNoises nether(NoiseSettings noiseSettings) {
        return noNewCaves(noiseSettings);
    }

    protected static NoiseRouterWithOnlyNoises end(NoiseSettings noiseSettings) {
        DensityFunction densityFunction = DensityFunctions.cache2d(DensityFunctions.endIslands(0L));
        DensityFunction densityFunction2 = postProcess(noiseSettings, getFunction(SLOPED_CHEESE_END));
        return new NoiseRouterWithOnlyNoises(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), densityFunction, densityFunction2, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
    }

    private static NormalNoise seedNoise(PositionalRandomFactory positionalRandomFactory, Registry<NormalNoise.NoiseParameters> registry, Holder<NormalNoise.NoiseParameters> holder) {
        return Noises.instantiate(positionalRandomFactory, holder.unwrapKey().flatMap(registry::getHolder).orElse(holder));
    }

    public static NoiseRouter createNoiseRouter(NoiseSettings noiseSettings, long l, Registry<NormalNoise.NoiseParameters> registry, WorldgenRandom.Algorithm algorithm, NoiseRouterWithOnlyNoises noiseRouterWithOnlyNoises) {
        boolean bl = algorithm == WorldgenRandom.Algorithm.LEGACY;
        PositionalRandomFactory positionalRandomFactory = algorithm.newInstance(l).forkPositional();
        Map<DensityFunction, DensityFunction> map = new HashMap<>();
        DensityFunction.Visitor visitor = (densityFunction) -> {
            if (densityFunction instanceof DensityFunctions.Noise) {
                DensityFunctions.Noise noise = (DensityFunctions.Noise)densityFunction;
                Holder<NormalNoise.NoiseParameters> holder = noise.noiseData();
                return new DensityFunctions.Noise(holder, seedNoise(positionalRandomFactory, registry, holder), noise.xzScale(), noise.yScale());
            } else if (densityFunction instanceof DensityFunctions.ShiftNoise) {
                DensityFunctions.ShiftNoise shiftNoise = (DensityFunctions.ShiftNoise)densityFunction;
                Holder<NormalNoise.NoiseParameters> holder2 = shiftNoise.noiseData();
                NormalNoise normalNoise;
                if (bl) {
                    normalNoise = NormalNoise.create(positionalRandomFactory.fromHashOf(Noises.SHIFT.location()), new NormalNoise.NoiseParameters(0, 0.0D));
                } else {
                    normalNoise = seedNoise(positionalRandomFactory, registry, holder2);
                }

                return shiftNoise.withNewNoise(normalNoise);
            } else if (densityFunction instanceof DensityFunctions.ShiftedNoise) {
                DensityFunctions.ShiftedNoise shiftedNoise = (DensityFunctions.ShiftedNoise)densityFunction;
                if (bl) {
                    Holder<NormalNoise.NoiseParameters> holder3 = shiftedNoise.noiseData();
                    if (Objects.equals(holder3.unwrapKey(), Optional.of(Noises.TEMPERATURE))) {
                        NormalNoise normalNoise3 = NormalNoise.createLegacyNetherBiome(algorithm.newInstance(l), new NormalNoise.NoiseParameters(-7, 1.0D, 1.0D));
                        return new DensityFunctions.ShiftedNoise(shiftedNoise.shiftX(), shiftedNoise.shiftY(), shiftedNoise.shiftZ(), shiftedNoise.xzScale(), shiftedNoise.yScale(), holder3, normalNoise3);
                    }

                    if (Objects.equals(holder3.unwrapKey(), Optional.of(Noises.VEGETATION))) {
                        NormalNoise normalNoise4 = NormalNoise.createLegacyNetherBiome(algorithm.newInstance(l + 1L), new NormalNoise.NoiseParameters(-7, 1.0D, 1.0D));
                        return new DensityFunctions.ShiftedNoise(shiftedNoise.shiftX(), shiftedNoise.shiftY(), shiftedNoise.shiftZ(), shiftedNoise.xzScale(), shiftedNoise.yScale(), holder3, normalNoise4);
                    }
                }

                Holder<NormalNoise.NoiseParameters> holder4 = shiftedNoise.noiseData();
                return new DensityFunctions.ShiftedNoise(shiftedNoise.shiftX(), shiftedNoise.shiftY(), shiftedNoise.shiftZ(), shiftedNoise.xzScale(), shiftedNoise.yScale(), holder4, seedNoise(positionalRandomFactory, registry, holder4));
            } else if (densityFunction instanceof DensityFunctions.WeirdScaledSampler) {
                DensityFunctions.WeirdScaledSampler weirdScaledSampler = (DensityFunctions.WeirdScaledSampler)densityFunction;
                return new DensityFunctions.WeirdScaledSampler(weirdScaledSampler.input(), weirdScaledSampler.noiseData(), seedNoise(positionalRandomFactory, registry, weirdScaledSampler.noiseData()), weirdScaledSampler.rarityValueMapper());
            } else if (densityFunction instanceof BlendedNoise) {
                return bl ? new BlendedNoise(algorithm.newInstance(l), noiseSettings.noiseSamplingSettings(), noiseSettings.getCellWidth(), noiseSettings.getCellHeight()) : new BlendedNoise(positionalRandomFactory.fromHashOf(new ResourceLocation("terrain")), noiseSettings.noiseSamplingSettings(), noiseSettings.getCellWidth(), noiseSettings.getCellHeight());
            } else if (densityFunction instanceof DensityFunctions.EndIslandDensityFunction) {
                return new DensityFunctions.EndIslandDensityFunction(l);
            } else if (densityFunction instanceof DensityFunctions.TerrainShaperSpline) {
                DensityFunctions.TerrainShaperSpline terrainShaperSpline = (DensityFunctions.TerrainShaperSpline)densityFunction;
                TerrainShaper terrainShaper = noiseSettings.terrainShaper();
                return new DensityFunctions.TerrainShaperSpline(terrainShaperSpline.continentalness(), terrainShaperSpline.erosion(), terrainShaperSpline.weirdness(), terrainShaper, terrainShaperSpline.spline(), terrainShaperSpline.minValue(), terrainShaperSpline.maxValue());
            } else if (densityFunction instanceof DensityFunctions.Slide) {
                DensityFunctions.Slide slide = (DensityFunctions.Slide)densityFunction;
                return new DensityFunctions.Slide(noiseSettings, slide.input());
            } else {
                return densityFunction;
            }
        };
        DensityFunction.Visitor visitor2 = (densityFunction) -> {
            return map.computeIfAbsent(densityFunction, visitor);
        };
        NoiseRouterWithOnlyNoises noiseRouterWithOnlyNoises2 = noiseRouterWithOnlyNoises.mapAll(visitor2);
        PositionalRandomFactory positionalRandomFactory2 = positionalRandomFactory.fromHashOf(new ResourceLocation("aquifer")).forkPositional();
        PositionalRandomFactory positionalRandomFactory3 = positionalRandomFactory.fromHashOf(new ResourceLocation("ore")).forkPositional();
        return new NoiseRouter(noiseRouterWithOnlyNoises2.barrierNoise(), noiseRouterWithOnlyNoises2.fluidLevelFloodednessNoise(), noiseRouterWithOnlyNoises2.fluidLevelSpreadNoise(), noiseRouterWithOnlyNoises2.lavaNoise(), positionalRandomFactory2, positionalRandomFactory3, noiseRouterWithOnlyNoises2.temperature(), noiseRouterWithOnlyNoises2.vegetation(), noiseRouterWithOnlyNoises2.continents(), noiseRouterWithOnlyNoises2.erosion(), noiseRouterWithOnlyNoises2.depth(), noiseRouterWithOnlyNoises2.ridges(), noiseRouterWithOnlyNoises2.initialDensityWithoutJaggedness(), noiseRouterWithOnlyNoises2.finalDensity(), noiseRouterWithOnlyNoises2.veinToggle(), noiseRouterWithOnlyNoises2.veinRidged(), noiseRouterWithOnlyNoises2.veinGap(), (new OverworldBiomeBuilder()).spawnTarget());
    }

    private static DensityFunction splineWithBlending(DensityFunction densityFunction, DensityFunction densityFunction2, DensityFunction densityFunction3, DensityFunctions.TerrainShaperSpline.SplineType splineType, double d, double e, DensityFunction densityFunction4) {
        DensityFunction densityFunction5 = DensityFunctions.terrainShaperSpline(densityFunction, densityFunction2, densityFunction3, splineType, d, e);
        DensityFunction densityFunction6 = DensityFunctions.lerp(DensityFunctions.blendAlpha(), densityFunction4, densityFunction5);
        return DensityFunctions.flatCache(DensityFunctions.cache2d(densityFunction6));
    }

    private static DensityFunction noiseGradientDensity(DensityFunction densityFunction, DensityFunction densityFunction2) {
        DensityFunction densityFunction3 = DensityFunctions.mul(densityFunction2, densityFunction);
        return DensityFunctions.mul(DensityFunctions.constant(4.0D), densityFunction3.quarterNegative());
    }

    private static DensityFunction yLimitedInterpolatable(DensityFunction densityFunction, DensityFunction densityFunction2, int i, int j, int k) {
        return DensityFunctions.interpolated(DensityFunctions.rangeChoice(densityFunction, (double)i, (double)(j + 1), densityFunction2, DensityFunctions.constant((double)k)));
    }

    protected static double applySlide(NoiseSettings noiseSettings, double d, double e) {
        double f = (double)((int)e / noiseSettings.getCellHeight() - noiseSettings.getMinCellY());
        d = noiseSettings.topSlideSettings().applySlide(d, (double)noiseSettings.getCellCountY() - f);
        return noiseSettings.bottomSlideSettings().applySlide(d, f);
    }

    protected static double computePreliminarySurfaceLevelScanning(NoiseSettings noiseSettings, DensityFunction densityFunction, int i, int j) {
        for(int k = noiseSettings.getMinCellY() + noiseSettings.getCellCountY(); k >= noiseSettings.getMinCellY(); --k) {
            int l = k * noiseSettings.getCellHeight();
            double d = -0.703125D;
            double e = densityFunction.compute(new DensityFunction.SinglePointContext(i, l, j)) + -0.703125D;
            double f = Mth.clamp(e, -64.0D, 64.0D);
            f = applySlide(noiseSettings, f, (double)l);
            if (f > 0.390625D) {
                return (double)l;
            }
        }

        return 2.147483647E9D;
    }

    protected static final class QuantizedSpaghettiRarity {
        protected static double getSphaghettiRarity2D(double value) {
            if (value < -0.75D) {
                return 0.5D;
            } else if (value < -0.5D) {
                return 0.75D;
            } else if (value < 0.5D) {
                return 1.0D;
            } else {
                return value < 0.75D ? 2.0D : 3.0D;
            }
        }

        protected static double getSpaghettiRarity3D(double value) {
            if (value < -0.5D) {
                return 0.75D;
            } else if (value < 0.0D) {
                return 1.0D;
            } else {
                return value < 0.5D ? 1.5D : 2.0D;
            }
        }
    }
}
