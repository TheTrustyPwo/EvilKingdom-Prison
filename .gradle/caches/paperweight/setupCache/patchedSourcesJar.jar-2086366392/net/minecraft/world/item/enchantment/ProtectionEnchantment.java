package net.minecraft.world.item.enchantment;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class ProtectionEnchantment extends Enchantment {
    public final ProtectionEnchantment.Type type;

    public ProtectionEnchantment(Enchantment.Rarity weight, ProtectionEnchantment.Type protectionType, EquipmentSlot... slotTypes) {
        super(weight, protectionType == ProtectionEnchantment.Type.FALL ? EnchantmentCategory.ARMOR_FEET : EnchantmentCategory.ARMOR, slotTypes);
        this.type = protectionType;
    }

    @Override
    public int getMinCost(int level) {
        return this.type.getMinCost() + (level - 1) * this.type.getLevelCost();
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + this.type.getLevelCost();
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getDamageProtection(int level, DamageSource source) {
        if (source.isBypassInvul()) {
            return 0;
        } else if (this.type == ProtectionEnchantment.Type.ALL) {
            return level;
        } else if (this.type == ProtectionEnchantment.Type.FIRE && source.isFire()) {
            return level * 2;
        } else if (this.type == ProtectionEnchantment.Type.FALL && source.isFall()) {
            return level * 3;
        } else if (this.type == ProtectionEnchantment.Type.EXPLOSION && source.isExplosion()) {
            return level * 2;
        } else {
            return this.type == ProtectionEnchantment.Type.PROJECTILE && source.isProjectile() ? level * 2 : 0;
        }
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        if (other instanceof ProtectionEnchantment) {
            ProtectionEnchantment protectionEnchantment = (ProtectionEnchantment)other;
            if (this.type == protectionEnchantment.type) {
                return false;
            } else {
                return this.type == ProtectionEnchantment.Type.FALL || protectionEnchantment.type == ProtectionEnchantment.Type.FALL;
            }
        } else {
            return super.checkCompatibility(other);
        }
    }

    public static int getFireAfterDampener(LivingEntity entity, int duration) {
        int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, entity);
        if (i > 0) {
            duration -= Mth.floor((float)duration * (float)i * 0.15F);
        }

        return duration;
    }

    public static double getExplosionKnockbackAfterDampener(LivingEntity entity, double velocity) {
        int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.BLAST_PROTECTION, entity);
        if (i > 0) {
            velocity -= (double)Mth.floor(velocity * (double)((float)i * 0.15F));
        }

        return velocity;
    }

    public static enum Type {
        ALL(1, 11),
        FIRE(10, 8),
        FALL(5, 6),
        EXPLOSION(5, 8),
        PROJECTILE(3, 6);

        private final int minCost;
        private final int levelCost;

        private Type(int basePower, int powerPerLevel) {
            this.minCost = basePower;
            this.levelCost = powerPerLevel;
        }

        public int getMinCost() {
            return this.minCost;
        }

        public int getLevelCost() {
            return this.levelCost;
        }
    }
}
