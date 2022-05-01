package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EmptyMapItem extends ComplexItem {
    public EmptyMapItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (world.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        } else {
            if (!user.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            user.awardStat(Stats.ITEM_USED.get(this));
            user.level.playSound((Player)null, user, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, user.getSoundSource(), 1.0F, 1.0F);
            ItemStack itemStack2 = MapItem.create(world, user.getBlockX(), user.getBlockZ(), (byte)0, true, false);
            if (itemStack.isEmpty()) {
                return InteractionResultHolder.consume(itemStack2);
            } else {
                if (!user.getInventory().add(itemStack2.copy())) {
                    user.drop(itemStack2, false);
                }

                return InteractionResultHolder.consume(itemStack);
            }
        }
    }
}
