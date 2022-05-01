package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

public class FireworkStarItem extends Item {
    public FireworkStarItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        CompoundTag compoundTag = stack.getTagElement("Explosion");
        if (compoundTag != null) {
            appendHoverText(compoundTag, tooltip);
        }

    }

    public static void appendHoverText(CompoundTag nbt, List<Component> tooltip) {
        FireworkRocketItem.Shape shape = FireworkRocketItem.Shape.byId(nbt.getByte("Type"));
        tooltip.add((new TranslatableComponent("item.minecraft.firework_star.shape." + shape.getName())).withStyle(ChatFormatting.GRAY));
        int[] is = nbt.getIntArray("Colors");
        if (is.length > 0) {
            tooltip.add(appendColors((new TextComponent("")).withStyle(ChatFormatting.GRAY), is));
        }

        int[] js = nbt.getIntArray("FadeColors");
        if (js.length > 0) {
            tooltip.add(appendColors((new TranslatableComponent("item.minecraft.firework_star.fade_to")).append(" ").withStyle(ChatFormatting.GRAY), js));
        }

        if (nbt.getBoolean("Trail")) {
            tooltip.add((new TranslatableComponent("item.minecraft.firework_star.trail")).withStyle(ChatFormatting.GRAY));
        }

        if (nbt.getBoolean("Flicker")) {
            tooltip.add((new TranslatableComponent("item.minecraft.firework_star.flicker")).withStyle(ChatFormatting.GRAY));
        }

    }

    private static Component appendColors(MutableComponent line, int[] colors) {
        for(int i = 0; i < colors.length; ++i) {
            if (i > 0) {
                line.append(", ");
            }

            line.append(getColorName(colors[i]));
        }

        return line;
    }

    private static Component getColorName(int color) {
        DyeColor dyeColor = DyeColor.byFireworkColor(color);
        return dyeColor == null ? new TranslatableComponent("item.minecraft.firework_star.custom_color") : new TranslatableComponent("item.minecraft.firework_star." + dyeColor.getName());
    }
}
