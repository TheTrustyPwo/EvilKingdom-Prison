package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GiveCommand {
    public static final int MAX_ALLOWED_ITEMSTACKS = 100;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("give").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("item", ItemArgument.item()).executes((context) -> {
            return giveItem(context.getSource(), ItemArgument.getItem(context, "item"), EntityArgument.getPlayers(context, "targets"), 1);
        }).then(Commands.argument("count", IntegerArgumentType.integer(1)).executes((context) -> {
            return giveItem(context.getSource(), ItemArgument.getItem(context, "item"), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "count"));
        })))));
    }

    private static int giveItem(CommandSourceStack source, ItemInput item, Collection<ServerPlayer> targets, int count) throws CommandSyntaxException {
        int i = item.getItem().getMaxStackSize();
        int j = i * 100;
        if (count > j) {
            source.sendFailure(new TranslatableComponent("commands.give.failed.toomanyitems", j, item.createItemStack(count, false).getDisplayName()));
            return 0;
        } else {
            for(ServerPlayer serverPlayer : targets) {
                int k = count;

                while(k > 0) {
                    int l = Math.min(i, k);
                    k -= l;
                    ItemStack itemStack = item.createItemStack(l, false);
                    boolean bl = serverPlayer.getInventory().add(itemStack);
                    if (bl && itemStack.isEmpty()) {
                        itemStack.setCount(1);
                        ItemEntity itemEntity2 = serverPlayer.drop(itemStack, false);
                        if (itemEntity2 != null) {
                            itemEntity2.makeFakeItem();
                        }

                        serverPlayer.level.playSound((Player)null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((serverPlayer.getRandom().nextFloat() - serverPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                        serverPlayer.containerMenu.broadcastChanges();
                    } else {
                        ItemEntity itemEntity = serverPlayer.drop(itemStack, false);
                        if (itemEntity != null) {
                            itemEntity.setNoPickUpDelay();
                            itemEntity.setOwner(serverPlayer.getUUID());
                        }
                    }
                }
            }

            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.give.success.single", count, item.createItemStack(count, false).getDisplayName(), targets.iterator().next().getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.give.success.single", count, item.createItemStack(count, false).getDisplayName(), targets.size()), true);
            }

            return targets.size();
        }
    }
}
