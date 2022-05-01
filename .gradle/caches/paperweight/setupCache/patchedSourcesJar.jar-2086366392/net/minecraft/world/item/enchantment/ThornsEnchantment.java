package net.minecraft.world.item.enchantment;

import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class ThornsEnchantment extends Enchantment {
    private static final float CHANCE_PER_LEVEL = 0.15F;

    public ThornsEnchantment(Enchantment.Rarity weight, EquipmentSlot... slotTypes) {
        super(weight, EnchantmentCategory.ARMOR_CHEST, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 10 + 20 * (level - 1);
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
        return stack.getItem() instanceof ArmorItem ? true : super.canEnchant(stack);
    }

    @Override
    public void doPostHurt(LivingEntity user, Entity attacker, int level) {
        Random random = user.getRandom();
        Entry<EquipmentSlot, ItemStack> entry = EnchantmentHelper.getRandomItemWith(Enchantments.THORNS, user);
        if (shouldHit(level, random)) {
            if (attacker != null) {
                attacker.hurt(DamageSource.thorns(user), (float)getDamage(level, random));
            }

            if (entry != null) {
                entry.getValue().hurtAndBreak(2, user, (entity) -> {
                    entity.broadcastBreakEvent(entry.getKey());
                });
            }
        }

    }

    public static boolean shouldHit(int level, Random random) {
        if (level <= 0) {
            return false;
        } else {
            return random.nextFloat() < 0.15F * (float)level;
        }
    }

    public static int getDamage(int level, Random random) {
        return level > 10 ? level - 10 : 1 + random.nextInt(4);
    }
}
