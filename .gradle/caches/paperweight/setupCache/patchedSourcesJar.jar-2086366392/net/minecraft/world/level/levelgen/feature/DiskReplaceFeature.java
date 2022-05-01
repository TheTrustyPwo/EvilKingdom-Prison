package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class DiskReplaceFeature extends BaseDiskFeature {
    public DiskReplaceFeature(Codec<DiskConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DiskConfiguration> context) {
        return !context.level().getFluidState(context.origin()).is(FluidTags.WATER) ? false : super.place(context);
    }
}
