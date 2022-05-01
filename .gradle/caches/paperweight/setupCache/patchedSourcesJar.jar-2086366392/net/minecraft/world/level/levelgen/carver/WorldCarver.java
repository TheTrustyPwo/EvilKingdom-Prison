package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;

public abstract class WorldCarver<C extends CarverConfiguration> {
    public static final WorldCarver<CaveCarverConfiguration> CAVE = register("cave", new CaveWorldCarver(CaveCarverConfiguration.CODEC));
    public static final WorldCarver<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", new NetherWorldCarver(CaveCarverConfiguration.CODEC));
    public static final WorldCarver<CanyonCarverConfiguration> CANYON = register("canyon", new CanyonWorldCarver(CanyonCarverConfiguration.CODEC));
    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
    protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
    protected static final FluidState WATER = Fluids.WATER.defaultFluidState();
    protected static final FluidState LAVA = Fluids.LAVA.defaultFluidState();
    protected Set<Block> replaceableBlocks = ImmutableSet.of(Blocks.WATER, Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.GRASS_BLOCK, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.MYCELIUM, Blocks.SNOW, Blocks.PACKED_ICE, Blocks.DEEPSLATE, Blocks.CALCITE, Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.TUFF, Blocks.GRANITE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK);
    protected Set<Fluid> liquids = ImmutableSet.of(Fluids.WATER);
    private final Codec<ConfiguredWorldCarver<C>> configuredCodec;

    private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(String name, F carver) {
        return Registry.register(Registry.CARVER, name, carver);
    }

    public WorldCarver(Codec<C> configCodec) {
        this.configuredCodec = configCodec.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config).codec();
    }

    public ConfiguredWorldCarver<C> configured(C config) {
        return new ConfiguredWorldCarver<>(this, config);
    }

    public Codec<ConfiguredWorldCarver<C>> configuredCodec() {
        return this.configuredCodec;
    }

    public int getRange() {
        return 4;
    }

    protected boolean carveEllipsoid(CarvingContext context, C config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> posToBiome, Aquifer aquiferSampler, double d, double e, double f, double g, double h, CarvingMask mask, WorldCarver.CarveSkipChecker skipPredicate) {
        ChunkPos chunkPos = chunk.getPos();
        double i = (double)chunkPos.getMiddleBlockX();
        double j = (double)chunkPos.getMiddleBlockZ();
        double k = 16.0D + g * 2.0D;
        if (!(Math.abs(d - i) > k) && !(Math.abs(f - j) > k)) {
            int l = chunkPos.getMinBlockX();
            int m = chunkPos.getMinBlockZ();
            int n = Math.max(Mth.floor(d - g) - l - 1, 0);
            int o = Math.min(Mth.floor(d + g) - l, 15);
            int p = Math.max(Mth.floor(e - h) - 1, context.getMinGenY() + 1);
            int q = chunk.isUpgrading() ? 0 : 7;
            int r = Math.min(Mth.floor(e + h) + 1, context.getMinGenY() + context.getGenDepth() - 1 - q);
            int s = Math.max(Mth.floor(f - g) - m - 1, 0);
            int t = Math.min(Mth.floor(f + g) - m, 15);
            boolean bl = false;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();

            for(int u = n; u <= o; ++u) {
                int v = chunkPos.getBlockX(u);
                double w = ((double)v + 0.5D - d) / g;

                for(int x = s; x <= t; ++x) {
                    int y = chunkPos.getBlockZ(x);
                    double z = ((double)y + 0.5D - f) / g;
                    if (!(w * w + z * z >= 1.0D)) {
                        MutableBoolean mutableBoolean = new MutableBoolean(false);

                        for(int aa = r; aa > p; --aa) {
                            double ab = ((double)aa - 0.5D - e) / h;
                            if (!skipPredicate.shouldSkip(context, w, ab, z, aa) && (!mask.get(u, aa, x) || isDebugEnabled(config))) {
                                mask.set(u, aa, x);
                                mutableBlockPos.set(v, aa, y);
                                bl |= this.carveBlock(context, config, chunk, posToBiome, mask, mutableBlockPos, mutableBlockPos2, aquiferSampler, mutableBoolean);
                            }
                        }
                    }
                }
            }

            return bl;
        } else {
            return false;
        }
    }

    protected boolean carveBlock(CarvingContext context, C config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> posToBiome, CarvingMask mask, BlockPos.MutableBlockPos mutableBlockPos, BlockPos.MutableBlockPos mutableBlockPos2, Aquifer aquiferSampler, MutableBoolean mutableBoolean) {
        BlockState blockState = chunk.getBlockState(mutableBlockPos);
        if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.MYCELIUM)) {
            mutableBoolean.setTrue();
        }

        if (!this.canReplaceBlock(blockState) && !isDebugEnabled(config)) {
            return false;
        } else {
            BlockState blockState2 = this.getCarveState(context, config, mutableBlockPos, aquiferSampler);
            if (blockState2 == null) {
                return false;
            } else {
                chunk.setBlockState(mutableBlockPos, blockState2, false);
                if (aquiferSampler.shouldScheduleFluidUpdate() && !blockState2.getFluidState().isEmpty()) {
                    chunk.markPosForPostprocessing(mutableBlockPos);
                }

                if (mutableBoolean.isTrue()) {
                    mutableBlockPos2.setWithOffset(mutableBlockPos, Direction.DOWN);
                    if (chunk.getBlockState(mutableBlockPos2).is(Blocks.DIRT)) {
                        context.topMaterial(posToBiome, chunk, mutableBlockPos2, !blockState2.getFluidState().isEmpty()).ifPresent((state) -> {
                            chunk.setBlockState(mutableBlockPos2, state, false);
                            if (!state.getFluidState().isEmpty()) {
                                chunk.markPosForPostprocessing(mutableBlockPos2);
                            }

                        });
                    }
                }

                return true;
            }
        }
    }

    @Nullable
    private BlockState getCarveState(CarvingContext context, C config, BlockPos pos, Aquifer sampler) {
        if (pos.getY() <= config.lavaLevel.resolveY(context)) {
            return LAVA.createLegacyBlock();
        } else {
            BlockState blockState = sampler.computeSubstance(new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ()), 0.0D);
            if (blockState == null) {
                return isDebugEnabled(config) ? config.debugSettings.getBarrierState() : null;
            } else {
                return isDebugEnabled(config) ? getDebugState(config, blockState) : blockState;
            }
        }
    }

    private static BlockState getDebugState(CarverConfiguration config, BlockState state) {
        if (state.is(Blocks.AIR)) {
            return config.debugSettings.getAirState();
        } else if (state.is(Blocks.WATER)) {
            BlockState blockState = config.debugSettings.getWaterState();
            return blockState.hasProperty(BlockStateProperties.WATERLOGGED) ? blockState.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true)) : blockState;
        } else {
            return state.is(Blocks.LAVA) ? config.debugSettings.getLavaState() : state;
        }
    }

    public abstract boolean carve(CarvingContext context, C config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> posToBiome, Random random, Aquifer aquiferSampler, ChunkPos pos, CarvingMask mask);

    public abstract boolean isStartChunk(C config, Random random);

    protected boolean canReplaceBlock(BlockState state) {
        return this.replaceableBlocks.contains(state.getBlock());
    }

    protected static boolean canReach(ChunkPos pos, double x, double z, int branchIndex, int branchCount, float baseWidth) {
        double d = (double)pos.getMiddleBlockX();
        double e = (double)pos.getMiddleBlockZ();
        double f = x - d;
        double g = z - e;
        double h = (double)(branchCount - branchIndex);
        double i = (double)(baseWidth + 2.0F + 16.0F);
        return f * f + g * g - h * h <= i * i;
    }

    private static boolean isDebugEnabled(CarverConfiguration config) {
        return config.debugSettings.isDebugMode();
    }

    public interface CarveSkipChecker {
        boolean shouldSkip(CarvingContext context, double scaledRelativeX, double scaledRelativeY, double scaledRelativeZ, int y);
    }
}
