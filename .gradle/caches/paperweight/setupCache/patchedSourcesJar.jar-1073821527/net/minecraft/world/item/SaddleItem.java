package net.minecraft.world.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.player.Player;

public class SaddleItem extends Item {
    public SaddleItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Saddleable && entity.isAlive()) {
            Saddleable saddleable = (Saddleable)entity;
            if (!saddleable.isSaddled() && saddleable.isSaddleable()) {
                if (!user.level.isClientSide) {
                    saddleable.equipSaddle(SoundSource.NEUTRAL);
                    stack.shrink(1);
                }

                return InteractionResult.sidedSuccess(user.level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }
}
