package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.core.QuartPos;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.dimension.DimensionType;

public record NoiseSettings(int minY, int height, NoiseSamplingSettings noiseSamplingSettings, NoiseSlider topSlideSettings, NoiseSlider bottomSlideSettings, int noiseSizeHorizontal, int noiseSizeVertical, TerrainShaper terrainShaper) {
    public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("min_y").forGetter(NoiseSettings::minY), Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(NoiseSettings::height), NoiseSamplingSettings.CODEC.fieldOf("sampling").forGetter(NoiseSettings::noiseSamplingSettings), NoiseSlider.CODEC.fieldOf("top_slide").forGetter(NoiseSettings::topSlideSettings), NoiseSlider.CODEC.fieldOf("bottom_slide").forGetter(NoiseSettings::bottomSlideSettings), Codec.intRange(1, 4).fieldOf("size_horizontal").forGetter(NoiseSettings::noiseSizeHorizontal), Codec.intRange(1, 4).fieldOf("size_vertical").forGetter(NoiseSettings::noiseSizeVertical), TerrainShaper.CODEC.fieldOf("terrain_shaper").forGetter(NoiseSettings::terrainShaper)).apply(instance, NoiseSettings::new);
    }).comapFlatMap(NoiseSettings::guardY, Function.identity());
    static final NoiseSettings NETHER_NOISE_SETTINGS = create(0, 128, new NoiseSamplingSettings(1.0D, 3.0D, 80.0D, 60.0D), new NoiseSlider(0.9375D, 3, 0), new NoiseSlider(2.5D, 4, -1), 1, 2, TerrainProvider.nether());
    static final NoiseSettings END_NOISE_SETTINGS = create(0, 128, new NoiseSamplingSettings(2.0D, 1.0D, 80.0D, 160.0D), new NoiseSlider(-23.4375D, 64, -46), new NoiseSlider(-0.234375D, 7, 1), 2, 1, TerrainProvider.end());
    static final NoiseSettings CAVES_NOISE_SETTINGS = create(-64, 192, new NoiseSamplingSettings(1.0D, 3.0D, 80.0D, 60.0D), new NoiseSlider(0.9375D, 3, 0), new NoiseSlider(2.5D, 4, -1), 1, 2, TerrainProvider.caves());
    static final NoiseSettings FLOATING_ISLANDS_NOISE_SETTINGS = create(0, 256, new NoiseSamplingSettings(2.0D, 1.0D, 80.0D, 160.0D), new NoiseSlider(-23.4375D, 64, -46), new NoiseSlider(-0.234375D, 7, 1), 2, 1, TerrainProvider.floatingIslands());

    private static DataResult<NoiseSettings> guardY(NoiseSettings config) {
        if (config.minY() + config.height() > DimensionType.MAX_Y + 1) {
            return DataResult.error("min_y + height cannot be higher than: " + (DimensionType.MAX_Y + 1));
        } else if (config.height() % 16 != 0) {
            return DataResult.error("height has to be a multiple of 16");
        } else {
            return config.minY() % 16 != 0 ? DataResult.error("min_y has to be a multiple of 16") : DataResult.success(config);
        }
    }

    public static NoiseSettings create(int minimumY, int height, NoiseSamplingSettings sampling, NoiseSlider topSlide, NoiseSlider bottomSlide, int horizontalSize, int verticalSize, TerrainShaper terrainShaper) {
        NoiseSettings noiseSettings = new NoiseSettings(minimumY, height, sampling, topSlide, bottomSlide, horizontalSize, verticalSize, terrainShaper);
        guardY(noiseSettings).error().ifPresent((partialResult) -> {
            throw new IllegalStateException(partialResult.message());
        });
        return noiseSettings;
    }

    static NoiseSettings overworldNoiseSettings(boolean bl) {
        return create(-64, 384, new NoiseSamplingSettings(1.0D, 1.0D, 80.0D, 160.0D), new NoiseSlider(-0.078125D, 2, bl ? 0 : 8), new NoiseSlider(bl ? 0.4D : 0.1171875D, 3, 0), 1, 2, TerrainProvider.overworld(bl));
    }

    public int getCellHeight() {
        return QuartPos.toBlock(this.noiseSizeVertical());
    }

    public int getCellWidth() {
        return QuartPos.toBlock(this.noiseSizeHorizontal());
    }

    public int getCellCountY() {
        return this.height() / this.getCellHeight();
    }

    public int getMinCellY() {
        return Mth.intFloorDiv(this.minY(), this.getCellHeight());
    }
}
