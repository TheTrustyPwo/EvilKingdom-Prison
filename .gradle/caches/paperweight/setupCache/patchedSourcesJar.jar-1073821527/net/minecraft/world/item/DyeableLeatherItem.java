package net.minecraft.world.item;

import java.util.List;
import net.minecraft.nbt.CompoundTag;

public interface DyeableLeatherItem {
    String TAG_COLOR = "color";
    String TAG_DISPLAY = "display";
    int DEFAULT_LEATHER_COLOR = 10511680;

    default boolean hasCustomColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTagElement("display");
        return compoundTag != null && compoundTag.contains("color", 99);
    }

    default int getColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTagElement("display");
        return compoundTag != null && compoundTag.contains("color", 99) ? compoundTag.getInt("color") : 10511680;
    }

    default void clearColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTagElement("display");
        if (compoundTag != null && compoundTag.contains("color")) {
            compoundTag.remove("color");
        }

    }

    default void setColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt("color", color);
    }

    static ItemStack dyeArmor(ItemStack stack, List<DyeItem> colors) {
        ItemStack itemStack = ItemStack.EMPTY;
        int[] is = new int[3];
        int i = 0;
        int j = 0;
        DyeableLeatherItem dyeableLeatherItem = null;
        Item item = stack.getItem();
        if (item instanceof DyeableLeatherItem) {
            dyeableLeatherItem = (DyeableLeatherItem)item;
            itemStack = stack.copy();
            itemStack.setCount(1);
            if (dyeableLeatherItem.hasCustomColor(stack)) {
                int k = dyeableLeatherItem.getColor(itemStack);
                float f = (float)(k >> 16 & 255) / 255.0F;
                float g = (float)(k >> 8 & 255) / 255.0F;
                float h = (float)(k & 255) / 255.0F;
                i += (int)(Math.max(f, Math.max(g, h)) * 255.0F);
                is[0] += (int)(f * 255.0F);
                is[1] += (int)(g * 255.0F);
                is[2] += (int)(h * 255.0F);
                ++j;
            }

            for(DyeItem dyeItem : colors) {
                float[] fs = dyeItem.getDyeColor().getTextureDiffuseColors();
                int l = (int)(fs[0] * 255.0F);
                int m = (int)(fs[1] * 255.0F);
                int n = (int)(fs[2] * 255.0F);
                i += Math.max(l, Math.max(m, n));
                is[0] += l;
                is[1] += m;
                is[2] += n;
                ++j;
            }
        }

        if (dyeableLeatherItem == null) {
            return ItemStack.EMPTY;
        } else {
            int o = is[0] / j;
            int p = is[1] / j;
            int q = is[2] / j;
            float r = (float)i / (float)j;
            float s = (float)Math.max(o, Math.max(p, q));
            o = (int)((float)o * r / s);
            p = (int)((float)p * r / s);
            q = (int)((float)q * r / s);
            int var26 = (o << 8) + p;
            var26 = (var26 << 8) + q;
            dyeableLeatherItem.setColor(itemStack, var26);
            return itemStack;
        }
    }
}
