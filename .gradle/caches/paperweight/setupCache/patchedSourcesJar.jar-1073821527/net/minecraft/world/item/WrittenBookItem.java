package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WrittenBookItem extends Item {
    public static final int TITLE_LENGTH = 16;
    public static final int TITLE_MAX_LENGTH = 32;
    public static final int PAGE_EDIT_LENGTH = 1024;
    public static final int PAGE_LENGTH = 32767;
    public static final int MAX_PAGES = 100;
    public static final int MAX_GENERATION = 2;
    public static final String TAG_TITLE = "title";
    public static final String TAG_FILTERED_TITLE = "filtered_title";
    public static final String TAG_AUTHOR = "author";
    public static final String TAG_PAGES = "pages";
    public static final String TAG_FILTERED_PAGES = "filtered_pages";
    public static final String TAG_GENERATION = "generation";
    public static final String TAG_RESOLVED = "resolved";

    public WrittenBookItem(Item.Properties settings) {
        super(settings);
    }

    public static boolean makeSureTagIsValid(@Nullable CompoundTag nbt) {
        if (!WritableBookItem.makeSureTagIsValid(nbt)) {
            return false;
        } else if (!nbt.contains("title", 8)) {
            return false;
        } else {
            String string = nbt.getString("title");
            return string.length() > 32 ? false : nbt.contains("author", 8);
        }
    }

    public static int getGeneration(ItemStack stack) {
        return stack.getTag().getInt("generation");
    }

    public static int getPageCount(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null ? compoundTag.getList("pages", 8).size() : 0;
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        if (compoundTag != null) {
            String string = compoundTag.getString("title");
            if (!StringUtil.isNullOrEmpty(string)) {
                return new TextComponent(string);
            }
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        if (stack.hasTag()) {
            CompoundTag compoundTag = stack.getTag();
            String string = compoundTag.getString("author");
            if (!StringUtil.isNullOrEmpty(string)) {
                tooltip.add((new TranslatableComponent("book.byAuthor", string)).withStyle(ChatFormatting.GRAY));
            }

            tooltip.add((new TranslatableComponent("book.generation." + compoundTag.getInt("generation"))).withStyle(ChatFormatting.GRAY));
        }

    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.is(Blocks.LECTERN)) {
            return LecternBlock.tryPlaceBook(context.getPlayer(), level, blockPos, blockState, context.getItemInHand()) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        user.openItemGui(itemStack, hand);
        user.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
    }

    public static boolean resolveBookComponents(ItemStack book, @Nullable CommandSourceStack commandSource, @Nullable Player player) {
        CompoundTag compoundTag = book.getTag();
        if (compoundTag != null && !compoundTag.getBoolean("resolved")) {
            compoundTag.putBoolean("resolved", true);
            if (!makeSureTagIsValid(compoundTag)) {
                return false;
            } else {
                ListTag listTag = compoundTag.getList("pages", 8);

                for(int i = 0; i < listTag.size(); ++i) {
                    listTag.set(i, (Tag)StringTag.valueOf(resolvePage(commandSource, player, listTag.getString(i))));
                }

                if (compoundTag.contains("filtered_pages", 10)) {
                    CompoundTag compoundTag2 = compoundTag.getCompound("filtered_pages");

                    for(String string : compoundTag2.getAllKeys()) {
                        compoundTag2.putString(string, resolvePage(commandSource, player, compoundTag2.getString(string)));
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private static String resolvePage(@Nullable CommandSourceStack commandSource, @Nullable Player player, String text) {
        Component component2;
        try {
            component2 = Component.Serializer.fromJsonLenient(text);
            component2 = ComponentUtils.updateForEntity(commandSource, component2, player, 0);
        } catch (Exception var5) {
            component2 = new TextComponent(text);
        }

        return Component.Serializer.toJson(component2);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
