package net.minecraft.world.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

public class ArrowItem extends Item {
    public ArrowItem(Item.Properties settings) {
        super(settings);
    }

    public AbstractArrow createArrow(Level world, ItemStack stack, LivingEntity shooter) {
        Arrow arrow = new Arrow(world, shooter);
        arrow.setEffectsFromItem(stack);
        return arrow;
    }
}
