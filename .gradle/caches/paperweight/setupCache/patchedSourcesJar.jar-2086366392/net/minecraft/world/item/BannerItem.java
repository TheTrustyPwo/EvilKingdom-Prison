package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
    private static final String PATTERN_PREFIX = "block.minecraft.banner.";

    public BannerItem(Block standingBlock, Block wallBlock, Item.Properties settings) {
        super(standingBlock, wallBlock, settings);
        Validate.isInstanceOf(AbstractBannerBlock.class, standingBlock);
        Validate.isInstanceOf(AbstractBannerBlock.class, wallBlock);
    }

    public static void appendHoverTextFromBannerBlockEntityTag(ItemStack stack, List<Component> tooltip) {
        CompoundTag compoundTag = BlockItem.getBlockEntityData(stack);
        if (compoundTag != null && compoundTag.contains("Patterns")) {
            ListTag listTag = compoundTag.getList("Patterns", 10);

            for(int i = 0; i < listTag.size() && i < 6; ++i) {
                CompoundTag compoundTag2 = listTag.getCompound(i);
                DyeColor dyeColor = DyeColor.byId(compoundTag2.getInt("Color"));
                BannerPattern bannerPattern = BannerPattern.byHash(compoundTag2.getString("Pattern"));
                if (bannerPattern != null) {
                    tooltip.add((new TranslatableComponent("block.minecraft.banner." + bannerPattern.getFilename() + "." + dyeColor.getName())).withStyle(ChatFormatting.GRAY));
                }
            }

        }
    }

    public DyeColor getColor() {
        return ((AbstractBannerBlock)this.getBlock()).getColor();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        appendHoverTextFromBannerBlockEntityTag(stack, tooltip);
    }
}
