package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public class FossilFeatureConfiguration implements FeatureConfiguration {
    public static final Codec<FossilFeatureConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(ResourceLocation.CODEC.listOf().fieldOf("fossil_structures").forGetter((config) -> {
            return config.fossilStructures;
        }), ResourceLocation.CODEC.listOf().fieldOf("overlay_structures").forGetter((config) -> {
            return config.overlayStructures;
        }), StructureProcessorType.LIST_CODEC.fieldOf("fossil_processors").forGetter((config) -> {
            return config.fossilProcessors;
        }), StructureProcessorType.LIST_CODEC.fieldOf("overlay_processors").forGetter((config) -> {
            return config.overlayProcessors;
        }), Codec.intRange(0, 7).fieldOf("max_empty_corners_allowed").forGetter((config) -> {
            return config.maxEmptyCornersAllowed;
        })).apply(instance, FossilFeatureConfiguration::new);
    });
    public final List<ResourceLocation> fossilStructures;
    public final List<ResourceLocation> overlayStructures;
    public final Holder<StructureProcessorList> fossilProcessors;
    public final Holder<StructureProcessorList> overlayProcessors;
    public final int maxEmptyCornersAllowed;

    public FossilFeatureConfiguration(List<ResourceLocation> fossilStructures, List<ResourceLocation> overlayStructures, Holder<StructureProcessorList> fossilProcessors, Holder<StructureProcessorList> overlayProcessors, int maxEmptyCorners) {
        if (fossilStructures.isEmpty()) {
            throw new IllegalArgumentException("Fossil structure lists need at least one entry");
        } else if (fossilStructures.size() != overlayStructures.size()) {
            throw new IllegalArgumentException("Fossil structure lists must be equal lengths");
        } else {
            this.fossilStructures = fossilStructures;
            this.overlayStructures = overlayStructures;
            this.fossilProcessors = fossilProcessors;
            this.overlayProcessors = overlayProcessors;
            this.maxEmptyCornersAllowed = maxEmptyCorners;
        }
    }
}
