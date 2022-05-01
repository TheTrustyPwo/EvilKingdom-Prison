package net.minecraft.world.item;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class DebugStickItem extends Item {
    public DebugStickItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
        if (!world.isClientSide) {
            this.handleInteraction(miner, state, world, pos, false, miner.getItemInHand(InteractionHand.MAIN_HAND));
        }

        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (!level.isClientSide && player != null) {
            BlockPos blockPos = context.getClickedPos();
            if (!this.handleInteraction(player, level.getBlockState(blockPos), level, blockPos, true, context.getItemInHand())) {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean handleInteraction(Player player, BlockState state, LevelAccessor world, BlockPos pos, boolean update, ItemStack stack) {
        if (!player.canUseGameMasterBlocks()) {
            return false;
        } else {
            Block block = state.getBlock();
            StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
            Collection<Property<?>> collection = stateDefinition.getProperties();
            String string = Registry.BLOCK.getKey(block).toString();
            if (collection.isEmpty()) {
                message(player, new TranslatableComponent(this.getDescriptionId() + ".empty", string));
                return false;
            } else {
                CompoundTag compoundTag = stack.getOrCreateTagElement("DebugProperty");
                String string2 = compoundTag.getString(string);
                Property<?> property = stateDefinition.getProperty(string2);
                if (update) {
                    if (property == null) {
                        property = collection.iterator().next();
                    }

                    BlockState blockState = cycleState(state, property, player.isSecondaryUseActive());
                    world.setBlock(pos, blockState, 18);
                    message(player, new TranslatableComponent(this.getDescriptionId() + ".update", property.getName(), getNameHelper(blockState, property)));
                } else {
                    property = getRelative(collection, property, player.isSecondaryUseActive());
                    String string3 = property.getName();
                    compoundTag.putString(string, string3);
                    message(player, new TranslatableComponent(this.getDescriptionId() + ".select", string3, getNameHelper(state, property)));
                }

                return true;
            }
        }
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean inverse) {
        return state.setValue(property, getRelative(property.getPossibleValues(), state.getValue(property), inverse));
    }

    private static <T> T getRelative(Iterable<T> elements, @Nullable T current, boolean inverse) {
        return (T)(inverse ? Util.findPreviousInIterable(elements, current) : Util.findNextInIterable(elements, current));
    }

    private static void message(Player player, Component message) {
        ((ServerPlayer)player).sendMessage(message, ChatType.GAME_INFO, Util.NIL_UUID);
    }

    private static <T extends Comparable<T>> String getNameHelper(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
