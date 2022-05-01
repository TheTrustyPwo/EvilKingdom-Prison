package net.minecraft.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class NameTagItem extends Item {
    public NameTagItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if (stack.hasCustomHoverName() && !(entity instanceof Player)) {
            if (!user.level.isClientSide && entity.isAlive()) {
                entity.setCustomName(stack.getHoverName());
                if (entity instanceof Mob) {
                    ((Mob)entity).setPersistenceRequired();
                }

                stack.shrink(1);
            }

            return InteractionResult.sidedSuccess(user.level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }
}
