package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.block.Block;

public class GlowLichenConfiguration implements FeatureConfiguration {
    public static final Codec<GlowLichenConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(1, 64).fieldOf("search_range").orElse(10).forGetter((config) -> {
            return config.searchRange;
        }), Codec.BOOL.fieldOf("can_place_on_floor").orElse(false).forGetter((config) -> {
            return config.canPlaceOnFloor;
        }), Codec.BOOL.fieldOf("can_place_on_ceiling").orElse(false).forGetter((config) -> {
            return config.canPlaceOnCeiling;
        }), Codec.BOOL.fieldOf("can_place_on_wall").orElse(false).forGetter((config) -> {
            return config.canPlaceOnWall;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_spreading").orElse(0.5F).forGetter((config) -> {
            return config.chanceOfSpreading;
        }), RegistryCodecs.homogeneousList(Registry.BLOCK_REGISTRY).fieldOf("can_be_placed_on").forGetter((config) -> {
            return config.canBePlacedOn;
        })).apply(instance, GlowLichenConfiguration::new);
    });
    public final int searchRange;
    public final boolean canPlaceOnFloor;
    public final boolean canPlaceOnCeiling;
    public final boolean canPlaceOnWall;
    public final float chanceOfSpreading;
    public final HolderSet<Block> canBePlacedOn;
    public final List<Direction> validDirections;

    public GlowLichenConfiguration(int searchRange, boolean placeOnFloor, boolean placeOnCeiling, boolean placeOnWalls, float spreadChance, HolderSet<Block> canPlaceOn) {
        this.searchRange = searchRange;
        this.canPlaceOnFloor = placeOnFloor;
        this.canPlaceOnCeiling = placeOnCeiling;
        this.canPlaceOnWall = placeOnWalls;
        this.chanceOfSpreading = spreadChance;
        this.canBePlacedOn = canPlaceOn;
        List<Direction> list = Lists.newArrayList();
        if (placeOnCeiling) {
            list.add(Direction.UP);
        }

        if (placeOnFloor) {
            list.add(Direction.DOWN);
        }

        if (placeOnWalls) {
            Direction.Plane.HORIZONTAL.forEach(list::add);
        }

        this.validDirections = Collections.unmodifiableList(list);
    }
}
