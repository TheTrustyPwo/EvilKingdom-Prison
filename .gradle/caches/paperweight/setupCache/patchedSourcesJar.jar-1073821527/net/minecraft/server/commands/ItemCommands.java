package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ItemCommands {
    static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType((x, y, z) -> {
        return new TranslatableComponent("commands.item.target.not_a_container", x, y, z);
    });
    private static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType((x, y, z) -> {
        return new TranslatableComponent("commands.item.source.not_a_container", x, y, z);
    });
    static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType((slot) -> {
        return new TranslatableComponent("commands.item.target.no_such_slot", slot);
    });
    private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType((slot) -> {
        return new TranslatableComponent("commands.item.source.no_such_slot", slot);
    });
    private static final DynamicCommandExceptionType ERROR_TARGET_NO_CHANGES = new DynamicCommandExceptionType((slot) -> {
        return new TranslatableComponent("commands.item.target.no_changes", slot);
    });
    private static final Dynamic2CommandExceptionType ERROR_TARGET_NO_CHANGES_KNOWN_ITEM = new Dynamic2CommandExceptionType((itemName, slot) -> {
        return new TranslatableComponent("commands.item.target.no_changed.known_item", itemName, slot);
    });
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MODIFIER = (context, builder) -> {
        ItemModifierManager itemModifierManager = context.getSource().getServer().getItemModifierManager();
        return SharedSuggestionProvider.suggestResource(itemModifierManager.getKeys(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("item").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("replace").then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("slot", SlotArgument.slot()).then(Commands.literal("with").then(Commands.argument("item", ItemArgument.item()).executes((context) -> {
            return setBlockItem(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), SlotArgument.getSlot(context, "slot"), ItemArgument.getItem(context, "item").createItemStack(1, false));
        }).then(Commands.argument("count", IntegerArgumentType.integer(1, 64)).executes((context) -> {
            return setBlockItem(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), SlotArgument.getSlot(context, "slot"), ItemArgument.getItem(context, "item").createItemStack(IntegerArgumentType.getInteger(context, "count"), true));
        })))).then(Commands.literal("from").then(Commands.literal("block").then(Commands.argument("source", BlockPosArgument.blockPos()).then(Commands.argument("sourceSlot", SlotArgument.slot()).executes((context) -> {
            return blockToBlock(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "source"), SlotArgument.getSlot(context, "sourceSlot"), BlockPosArgument.getLoadedBlockPos(context, "pos"), SlotArgument.getSlot(context, "slot"));
        }).then(Commands.argument("modifier", ResourceLocationArgument.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return blockToBlock(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "source"), SlotArgument.getSlot(context, "sourceSlot"), BlockPosArgument.getLoadedBlockPos(context, "pos"), SlotArgument.getSlot(context, "slot"), ResourceLocationArgument.getItemModifier(context, "modifier"));
        }))))).then(Commands.literal("entity").then(Commands.argument("source", EntityArgument.entity()).then(Commands.argument("sourceSlot", SlotArgument.slot()).executes((context) -> {
            return entityToBlock(context.getSource(), EntityArgument.getEntity(context, "source"), SlotArgument.getSlot(context, "sourceSlot"), BlockPosArgument.getLoadedBlockPos(context, "pos"), SlotArgument.getSlot(context, "slot"));
        }).then(Commands.argument("modifier", ResourceLocationArgument.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return entityToBlock(context.getSource(), EntityArgument.getEntity(context, "source"), SlotArgument.getSlot(context, "sourceSlot"), BlockPosArgument.getLoadedBlockPos(context, "pos"), SlotArgument.getSlot(context, "slot"), ResourceLocationArgument.getItemModifier(context, "modifier"));
        }))))))))).then(Commands.literal("entity").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("slot", SlotArgument.slot()).then(Commands.literal("with").then(Commands.argument("item", ItemArgument.item()).executes((context) -> {
            return setEntityItem(context.getSource(), EntityArgument.getEntities(context, "targets"), SlotArgument.getSlot(context, "slot"), ItemArgument.getItem(context, "item").createItemStack(1, false));
        }).then(Commands.argument("count", IntegerArgumentType.integer(1, 64)).executes((context) -> {
            return setEntityItem(context.getSource(), EntityArgument.getEntities(context, "targets"), SlotArgument.getSlot(context, "slot"), ItemArgument.getItem(context, "item").createItemStack(IntegerArgumentType.getInteger(context, "count"), true));
        })))).then(Commands.literal("from").then(Commands.literal("block").then(Commands.argument("source", BlockPosArgument.blockPos()).then(Commands.argument("sourceSlot", SlotArgument.slot()).executes((context) -> {
            return blockToEntities(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "source"), SlotArgument.getSlot(context, "sourceSlot"), EntityArgument.getEntities(context, "targets"), SlotArgument.getSlot(context, "slot"));
        }).then(Commands.argument("modifier", ResourceLocationArgument.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return blockToEntities(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "source"), SlotArgument.getSlot(context, "sourceSlot"), EntityArgument.getEntities(context, "targets"), SlotArgument.getSlot(context, "slot"), ResourceLocationArgument.getItemModifier(context, "modifier"));
        }))))).then(Commands.literal("entity").then(Commands.argument("source", EntityArgument.entity()).then(Commands.argument("sourceSlot", SlotArgument.slot()).executes((context) -> {
            return entityToEntities(context.getSource(), EntityArgument.getEntity(context, "source"), SlotArgument.getSlot(context, "sourceSlot"), EntityArgument.getEntities(context, "targets"), SlotArgument.getSlot(context, "slot"));
        }).then(Commands.argument("modifier", ResourceLocationArgument.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return entityToEntities(context.getSource(), EntityArgument.getEntity(context, "source"), SlotArgument.getSlot(context, "sourceSlot"), EntityArgument.getEntities(context, "targets"), SlotArgument.getSlot(context, "slot"), ResourceLocationArgument.getItemModifier(context, "modifier"));
        })))))))))).then(Commands.literal("modify").then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("slot", SlotArgument.slot()).then(Commands.argument("modifier", ResourceLocationArgument.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return modifyBlockItem(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), SlotArgument.getSlot(context, "slot"), ResourceLocationArgument.getItemModifier(context, "modifier"));
        }))))).then(Commands.literal("entity").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("slot", SlotArgument.slot()).then(Commands.argument("modifier", ResourceLocationArgument.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return modifyEntityItem(context.getSource(), EntityArgument.getEntities(context, "targets"), SlotArgument.getSlot(context, "slot"), ResourceLocationArgument.getItemModifier(context, "modifier"));
        })))))));
    }

    private static int modifyBlockItem(CommandSourceStack source, BlockPos pos, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        Container container = getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);
        if (slot >= 0 && slot < container.getContainerSize()) {
            ItemStack itemStack = applyModifier(source, modifier, container.getItem(slot));
            container.setItem(slot, itemStack);
            source.sendSuccess(new TranslatableComponent("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), itemStack.getDisplayName()), true);
            return 1;
        } else {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
        }
    }

    private static int modifyEntityItem(CommandSourceStack source, Collection<? extends Entity> targets, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        Map<Entity, ItemStack> map = Maps.newHashMapWithExpectedSize(targets.size());

        for(Entity entity : targets) {
            SlotAccess slotAccess = entity.getSlot(slot);
            if (slotAccess != SlotAccess.NULL) {
                ItemStack itemStack = applyModifier(source, modifier, slotAccess.get().copy());
                if (slotAccess.set(itemStack)) {
                    map.put(entity, itemStack);
                    if (entity instanceof ServerPlayer) {
                        ((ServerPlayer)entity).containerMenu.broadcastChanges();
                    }
                }
            }
        }

        if (map.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES.create(slot);
        } else {
            if (map.size() == 1) {
                Entry<Entity, ItemStack> entry = map.entrySet().iterator().next();
                source.sendSuccess(new TranslatableComponent("commands.item.entity.set.success.single", entry.getKey().getDisplayName(), entry.getValue().getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.item.entity.set.success.multiple", map.size()), true);
            }

            return map.size();
        }
    }

    private static int setBlockItem(CommandSourceStack source, BlockPos pos, int slot, ItemStack stack) throws CommandSyntaxException {
        Container container = getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);
        if (slot >= 0 && slot < container.getContainerSize()) {
            container.setItem(slot, stack);
            source.sendSuccess(new TranslatableComponent("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), stack.getDisplayName()), true);
            return 1;
        } else {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
        }
    }

    private static Container getContainer(CommandSourceStack source, BlockPos pos, Dynamic3CommandExceptionType exception) throws CommandSyntaxException {
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof Container)) {
            throw exception.create(pos.getX(), pos.getY(), pos.getZ());
        } else {
            return (Container)blockEntity;
        }
    }

    private static int setEntityItem(CommandSourceStack source, Collection<? extends Entity> targets, int slot, ItemStack stack) throws CommandSyntaxException {
        List<Entity> list = Lists.newArrayListWithCapacity(targets.size());

        for(Entity entity : targets) {
            SlotAccess slotAccess = entity.getSlot(slot);
            if (slotAccess != SlotAccess.NULL && slotAccess.set(stack.copy())) {
                list.add(entity);
                if (entity instanceof ServerPlayer) {
                    ((ServerPlayer)entity).containerMenu.broadcastChanges();
                }
            }
        }

        if (list.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES_KNOWN_ITEM.create(stack.getDisplayName(), slot);
        } else {
            if (list.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.item.entity.set.success.single", list.iterator().next().getDisplayName(), stack.getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.item.entity.set.success.multiple", list.size(), stack.getDisplayName()), true);
            }

            return list.size();
        }
    }

    private static int blockToEntities(CommandSourceStack source, BlockPos sourcePos, int sourceSlot, Collection<? extends Entity> targets, int slot) throws CommandSyntaxException {
        return setEntityItem(source, targets, slot, getBlockItem(source, sourcePos, sourceSlot));
    }

    private static int blockToEntities(CommandSourceStack source, BlockPos sourcePos, int sourceSlot, Collection<? extends Entity> targets, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        return setEntityItem(source, targets, slot, applyModifier(source, modifier, getBlockItem(source, sourcePos, sourceSlot)));
    }

    private static int blockToBlock(CommandSourceStack source, BlockPos sourcePos, int sourceSlot, BlockPos pos, int slot) throws CommandSyntaxException {
        return setBlockItem(source, pos, slot, getBlockItem(source, sourcePos, sourceSlot));
    }

    private static int blockToBlock(CommandSourceStack source, BlockPos sourcePos, int sourceSlot, BlockPos pos, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        return setBlockItem(source, pos, slot, applyModifier(source, modifier, getBlockItem(source, sourcePos, sourceSlot)));
    }

    private static int entityToBlock(CommandSourceStack source, Entity sourceEntity, int sourceSlot, BlockPos pos, int slot) throws CommandSyntaxException {
        return setBlockItem(source, pos, slot, getEntityItem(sourceEntity, sourceSlot));
    }

    private static int entityToBlock(CommandSourceStack source, Entity sourceEntity, int sourceSlot, BlockPos pos, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        return setBlockItem(source, pos, slot, applyModifier(source, modifier, getEntityItem(sourceEntity, sourceSlot)));
    }

    private static int entityToEntities(CommandSourceStack source, Entity sourceEntity, int sourceSlot, Collection<? extends Entity> targets, int slot) throws CommandSyntaxException {
        return setEntityItem(source, targets, slot, getEntityItem(sourceEntity, sourceSlot));
    }

    private static int entityToEntities(CommandSourceStack source, Entity sourceEntity, int sourceSlot, Collection<? extends Entity> targets, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        return setEntityItem(source, targets, slot, applyModifier(source, modifier, getEntityItem(sourceEntity, sourceSlot)));
    }

    private static ItemStack applyModifier(CommandSourceStack source, LootItemFunction modifier, ItemStack stack) {
        ServerLevel serverLevel = source.getLevel();
        LootContext.Builder builder = (new LootContext.Builder(serverLevel)).withParameter(LootContextParams.ORIGIN, source.getPosition()).withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity());
        return modifier.apply(stack, builder.create(LootContextParamSets.COMMAND));
    }

    private static ItemStack getEntityItem(Entity entity, int slotId) throws CommandSyntaxException {
        SlotAccess slotAccess = entity.getSlot(slotId);
        if (slotAccess == SlotAccess.NULL) {
            throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slotId);
        } else {
            return slotAccess.get().copy();
        }
    }

    private static ItemStack getBlockItem(CommandSourceStack source, BlockPos pos, int slotId) throws CommandSyntaxException {
        Container container = getContainer(source, pos, ERROR_SOURCE_NOT_A_CONTAINER);
        if (slotId >= 0 && slotId < container.getContainerSize()) {
            return container.getItem(slotId).copy();
        } else {
            throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slotId);
        }
    }
}
