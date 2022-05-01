package net.minecraft.world.item;

import java.util.stream.Stream;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ItemUtils {
    public static InteractionResultHolder<ItemStack> startUsingInstantly(Level world, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public static ItemStack createFilledResult(ItemStack inputStack, Player player, ItemStack outputStack, boolean creativeOverride) {
        boolean bl = player.getAbilities().instabuild;
        if (creativeOverride && bl) {
            if (!player.getInventory().contains(outputStack)) {
                player.getInventory().add(outputStack);
            }

            return inputStack;
        } else {
            if (!bl) {
                inputStack.shrink(1);
            }

            if (inputStack.isEmpty()) {
                return outputStack;
            } else {
                if (!player.getInventory().add(outputStack)) {
                    player.drop(outputStack, false);
                }

                return inputStack;
            }
        }
    }

    public static ItemStack createFilledResult(ItemStack inputStack, Player player, ItemStack outputStack) {
        return createFilledResult(inputStack, player, outputStack, true);
    }

    public static void onContainerDestroyed(ItemEntity itemEntity, Stream<ItemStack> contents) {
        Level level = itemEntity.level;
        if (!level.isClientSide) {
            contents.forEach((stack) -> {
                level.addFreshEntity(new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), stack));
            });
        }
    }
}
