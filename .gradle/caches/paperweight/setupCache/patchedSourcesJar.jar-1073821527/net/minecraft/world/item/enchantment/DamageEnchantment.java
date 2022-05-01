package net.minecraft.world.item.enchantment;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;

public class DamageEnchantment extends Enchantment {
    public static final int ALL = 0;
    public static final int UNDEAD = 1;
    public static final int ARTHROPODS = 2;
    private static final String[] NAMES = new String[]{"all", "undead", "arthropods"};
    private static final int[] MIN_COST = new int[]{1, 5, 5};
    private static final int[] LEVEL_COST = new int[]{11, 8, 8};
    private static final int[] LEVEL_COST_SPAN = new int[]{20, 20, 20};
    public final int type;

    public DamageEnchantment(Enchantment.Rarity weight, int typeIndex, EquipmentSlot... slots) {
        super(weight, EnchantmentCategory.WEAPON, slots);
        this.type = typeIndex;
    }

    @Override
    public int getMinCost(int level) {
        return MIN_COST[this.type] + (level - 1) * LEVEL_COST[this.type];
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + LEVEL_COST_SPAN[this.type];
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public float getDamageBonus(int level, MobType group) {
        if (this.type == 0) {
            return 1.0F + (float)Math.max(0, level - 1) * 0.5F;
        } else if (this.type == 1 && group == MobType.UNDEAD) {
            return (float)level * 2.5F;
        } else {
            return this.type == 2 && group == MobType.ARTHROPOD ? (float)level * 2.5F : 0.0F;
        }
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        return !(other instanceof DamageEnchantment);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof AxeItem ? true : super.canEnchant(stack);
    }

    @Override
    public void doPostAttack(LivingEntity user, Entity target, int level) {
        if (target instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)target;
            if (this.type == 2 && level > 0 && livingEntity.getMobType() == MobType.ARTHROPOD) {
                int i = 20 + user.getRandom().nextInt(10 * level);
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, i, 3));
            }
        }

    }
}
