package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

public class EnchantedBookItem extends Item {
    public static final String TAG_STORED_ENCHANTMENTS = "StoredEnchantments";

    public EnchantedBookItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    public static ListTag getEnchantments(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null ? compoundTag.getList("StoredEnchantments", 10) : new ListTag();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        ItemStack.appendEnchantmentNames(tooltip, getEnchantments(stack));
    }

    public static void addEnchantment(ItemStack stack, EnchantmentInstance entry) {
        ListTag listTag = getEnchantments(stack);
        boolean bl = true;
        ResourceLocation resourceLocation = EnchantmentHelper.getEnchantmentId(entry.enchantment);

        for(int i = 0; i < listTag.size(); ++i) {
            CompoundTag compoundTag = listTag.getCompound(i);
            ResourceLocation resourceLocation2 = EnchantmentHelper.getEnchantmentId(compoundTag);
            if (resourceLocation2 != null && resourceLocation2.equals(resourceLocation)) {
                if (EnchantmentHelper.getEnchantmentLevel(compoundTag) < entry.level) {
                    EnchantmentHelper.setEnchantmentLevel(compoundTag, entry.level);
                }

                bl = false;
                break;
            }
        }

        if (bl) {
            listTag.add(EnchantmentHelper.storeEnchantment(resourceLocation, entry.level));
        }

        stack.getOrCreateTag().put("StoredEnchantments", listTag);
    }

    public static ItemStack createForEnchantment(EnchantmentInstance info) {
        ItemStack itemStack = new ItemStack(Items.ENCHANTED_BOOK);
        addEnchantment(itemStack, info);
        return itemStack;
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (group == CreativeModeTab.TAB_SEARCH) {
            for(Enchantment enchantment : Registry.ENCHANTMENT) {
                if (enchantment.category != null) {
                    for(int i = enchantment.getMinLevel(); i <= enchantment.getMaxLevel(); ++i) {
                        stacks.add(createForEnchantment(new EnchantmentInstance(enchantment, i)));
                    }
                }
            }
        } else if (group.getEnchantmentCategories().length != 0) {
            for(Enchantment enchantment2 : Registry.ENCHANTMENT) {
                if (group.hasEnchantmentCategory(enchantment2.category)) {
                    stacks.add(createForEnchantment(new EnchantmentInstance(enchantment2, enchantment2.getMaxLevel())));
                }
            }
        }

    }
}
