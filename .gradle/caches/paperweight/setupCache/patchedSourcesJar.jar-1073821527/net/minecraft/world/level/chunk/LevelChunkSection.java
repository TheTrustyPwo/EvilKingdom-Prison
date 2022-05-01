package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LevelChunkSection {
    public static final int SECTION_WIDTH = 16;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_SIZE = 4096;
    public static final int BIOME_CONTAINER_BITS = 2;
    private final int bottomBlockY;
    private short nonEmptyBlockCount;
    private short tickingBlockCount;
    private short tickingFluidCount;
    public final PalettedContainer<BlockState> states;
    private final PalettedContainer<Holder<Biome>> biomes;

    public LevelChunkSection(int chunkPos, PalettedContainer<BlockState> blockStateContainer, PalettedContainer<Holder<Biome>> biomeContainer) {
        this.bottomBlockY = getBottomBlockY(chunkPos);
        this.states = blockStateContainer;
        this.biomes = biomeContainer;
        this.recalcBlockCounts();
    }

    public LevelChunkSection(int chunkPos, Registry<Biome> biomeRegistry) {
        this.bottomBlockY = getBottomBlockY(chunkPos);
        this.states = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
        this.biomes = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
    }

    public static int getBottomBlockY(int chunkPos) {
        return chunkPos << 4;
    }

    public BlockState getBlockState(int x, int y, int z) {
        return this.states.get(x, y, z);
    }

    public FluidState getFluidState(int x, int y, int z) {
        return this.states.get(x, y, z).getFluidState();
    }

    public void acquire() {
        this.states.acquire();
    }

    public void release() {
        this.states.release();
    }

    public BlockState setBlockState(int x, int y, int z, BlockState state) {
        return this.setBlockState(x, y, z, state, true);
    }

    public BlockState setBlockState(int x, int y, int z, BlockState state, boolean lock) {
        BlockState blockState;
        if (lock) {
            blockState = this.states.getAndSet(x, y, z, state);
        } else {
            blockState = this.states.getAndSetUnchecked(x, y, z, state);
        }

        FluidState fluidState = blockState.getFluidState();
        FluidState fluidState2 = state.getFluidState();
        if (!blockState.isAir()) {
            --this.nonEmptyBlockCount;
            if (blockState.isRandomlyTicking()) {
                --this.tickingBlockCount;
            }
        }

        if (!fluidState.isEmpty()) {
            --this.tickingFluidCount;
        }

        if (!state.isAir()) {
            ++this.nonEmptyBlockCount;
            if (state.isRandomlyTicking()) {
                ++this.tickingBlockCount;
            }
        }

        if (!fluidState2.isEmpty()) {
            ++this.tickingFluidCount;
        }

        return blockState;
    }

    public boolean hasOnlyAir() {
        return this.nonEmptyBlockCount == 0;
    }

    public boolean isRandomlyTicking() {
        return this.isRandomlyTickingBlocks() || this.isRandomlyTickingFluids();
    }

    public boolean isRandomlyTickingBlocks() {
        return this.tickingBlockCount > 0;
    }

    public boolean isRandomlyTickingFluids() {
        return this.tickingFluidCount > 0;
    }

    public int bottomBlockY() {
        return this.bottomBlockY;
    }

    public void recalcBlockCounts() {
        class BlockCounter implements PalettedContainer.CountConsumer<BlockState> {
            public int nonEmptyBlockCount;
            public int tickingBlockCount;
            public int tickingFluidCount;

            @Override
            public void accept(BlockState blockState, int i) {
                FluidState fluidState = blockState.getFluidState();
                if (!blockState.isAir()) {
                    this.nonEmptyBlockCount += i;
                    if (blockState.isRandomlyTicking()) {
                        this.tickingBlockCount += i;
                    }
                }

                if (!fluidState.isEmpty()) {
                    this.nonEmptyBlockCount += i;
                    if (fluidState.isRandomlyTicking()) {
                        this.tickingFluidCount += i;
                    }
                }

            }
        }

        BlockCounter lv = new BlockCounter();
        this.states.count(lv);
        this.nonEmptyBlockCount = (short)lv.nonEmptyBlockCount;
        this.tickingBlockCount = (short)lv.tickingBlockCount;
        this.tickingFluidCount = (short)lv.tickingFluidCount;
    }

    public PalettedContainer<BlockState> getStates() {
        return this.states;
    }

    public PalettedContainer<Holder<Biome>> getBiomes() {
        return this.biomes;
    }

    public void read(FriendlyByteBuf buf) {
        this.nonEmptyBlockCount = buf.readShort();
        this.states.read(buf);
        this.biomes.read(buf);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeShort(this.nonEmptyBlockCount);
        this.states.write(buf);
        this.biomes.write(buf);
    }

    public int getSerializedSize() {
        return 2 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
    }

    public boolean maybeHas(Predicate<BlockState> predicate) {
        return this.states.maybeHas(predicate);
    }

    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        return this.biomes.get(x, y, z);
    }

    public void fillBiomesFromNoise(BiomeResolver biomeSupplier, Climate.Sampler sampler, int x, int z) {
        PalettedContainer<Holder<Biome>> palettedContainer = this.getBiomes();
        palettedContainer.acquire();

        try {
            int i = QuartPos.fromBlock(this.bottomBlockY());
            int j = 4;

            for(int k = 0; k < 4; ++k) {
                for(int l = 0; l < 4; ++l) {
                    for(int m = 0; m < 4; ++m) {
                        palettedContainer.getAndSetUnchecked(k, l, m, biomeSupplier.getNoiseBiome(x + k, i + l, z + m, sampler));
                    }
                }
            }
        } finally {
            palettedContainer.release();
        }

    }
}
