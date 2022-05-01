package net.minecraft.world.level.levelgen.carver;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class CarvingContext extends WorldGenerationContext {
    private final NoiseBasedChunkGenerator generator;
    private final RegistryAccess registryAccess;
    private final NoiseChunk noiseChunk;

    public CarvingContext(NoiseBasedChunkGenerator chunkGenerator, RegistryAccess registryManager, LevelHeightAccessor heightLimitView, NoiseChunk chunkNoiseSampler, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level) { // Paper
        super(chunkGenerator, heightLimitView, level); // Paper
        this.generator = chunkGenerator;
        this.registryAccess = registryManager;
        this.noiseChunk = chunkNoiseSampler;
    }

    /** @deprecated */
    @Deprecated
    public Optional<BlockState> topMaterial(Function<BlockPos, Holder<Biome>> posToBiome, ChunkAccess chunk, BlockPos pos, boolean hasFluid) {
        return this.generator.topMaterial(this, posToBiome, chunk, this.noiseChunk, pos, hasFluid);
    }

    /** @deprecated */
    @Deprecated
    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }
}
