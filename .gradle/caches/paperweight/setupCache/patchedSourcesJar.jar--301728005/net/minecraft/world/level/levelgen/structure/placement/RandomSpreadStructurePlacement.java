package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public record RandomSpreadStructurePlacement(int spacing, int separation, RandomSpreadType spreadType, int salt, Vec3i locateOffset) implements StructurePlacement {
    public static final Codec<RandomSpreadStructurePlacement> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing), Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation), RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(RandomSpreadStructurePlacement::salt), Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(RandomSpreadStructurePlacement::locateOffset)).apply(instance, RandomSpreadStructurePlacement::new);
    }).flatXmap((placement) -> {
        return placement.spacing <= placement.separation ? DataResult.error("Spacing has to be larger than separation") : DataResult.success(placement);
    }, DataResult::success).codec();

    public RandomSpreadStructurePlacement(int spacing, int separation, RandomSpreadType spreadType, int salt) {
        this(spacing, separation, spreadType, salt, Vec3i.ZERO);
    }

    public ChunkPos getPotentialFeatureChunk(long seed, int x, int z) {
        int i = this.spacing();
        int j = this.separation();
        int k = Math.floorDiv(x, i);
        int l = Math.floorDiv(z, i);
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureWithSalt(seed, k, l, this.salt());
        int m = i - j;
        int n = this.spreadType().evaluate(worldgenRandom, m);
        int o = this.spreadType().evaluate(worldgenRandom, m);
        return new ChunkPos(k * i + n, l * i + o);
    }

    @Override
    public boolean isFeatureChunk(ChunkGenerator chunkGenerator, long l, int i, int j) {
        ChunkPos chunkPos = this.getPotentialFeatureChunk(l, i, j);
        return chunkPos.x == i && chunkPos.z == j;
    }

    @Override
    public StructurePlacementType<?> type() {
        return StructurePlacementType.RANDOM_SPREAD;
    }
}
