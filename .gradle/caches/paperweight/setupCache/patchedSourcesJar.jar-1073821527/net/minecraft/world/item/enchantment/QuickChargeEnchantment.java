package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class QuickChargeEnchantment extends Enchantment {
    public QuickChargeEnchantment(Enchantment.Rarity weight, EquipmentSlot... slot) {
        super(weight, EnchantmentCategory.CROSSBOW, slot);
    }

    @Override
    public int getMinCost(int level) {
        return 12 + (level - 1) * 20;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
