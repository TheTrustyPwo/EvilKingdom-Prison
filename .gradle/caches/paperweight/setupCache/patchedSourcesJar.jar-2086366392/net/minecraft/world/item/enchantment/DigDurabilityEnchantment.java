package net.minecraft.world.item.enchantment;

import java.util.Random;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class DigDurabilityEnchantment extends Enchantment {
    protected DigDurabilityEnchantment(Enchantment.Rarity weight, EquipmentSlot... slotTypes) {
        super(weight, EnchantmentCategory.BREAKABLE, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.isDamageableItem() ? true : super.canEnchant(stack);
    }

    public static boolean shouldIgnoreDurabilityDrop(ItemStack item, int level, Random random) {
        if (item.getItem() instanceof ArmorItem && random.nextFloat() < 0.6F) {
            return false;
        } else {
            return random.nextInt(level + 1) > 0;
        }
    }
}
