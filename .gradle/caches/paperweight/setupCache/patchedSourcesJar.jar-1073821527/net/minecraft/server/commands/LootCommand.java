package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LootCommand {
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_LOOT_TABLE = (context, builder) -> {
        LootTables lootTables = context.getSource().getServer().getLootTables();
        return SharedSuggestionProvider.suggestResource(lootTables.getIds(), builder);
    };
    private static final DynamicCommandExceptionType ERROR_NO_HELD_ITEMS = new DynamicCommandExceptionType((entityName) -> {
        return new TranslatableComponent("commands.drop.no_held_items", entityName);
    });
    private static final DynamicCommandExceptionType ERROR_NO_LOOT_TABLE = new DynamicCommandExceptionType((entityName) -> {
        return new TranslatableComponent("commands.drop.no_loot_table", entityName);
    });

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(addTargets(Commands.literal("loot").requires((source) -> {
            return source.hasPermission(2);
        }), (builder, constructor) -> {
            return builder.then(Commands.literal("fish").then(Commands.argument("loot_table", ResourceLocationArgument.id()).suggests(SUGGEST_LOOT_TABLE).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((context) -> {
                return dropFishingLoot(context, ResourceLocationArgument.getId(context, "loot_table"), BlockPosArgument.getLoadedBlockPos(context, "pos"), ItemStack.EMPTY, constructor);
            }).then(Commands.argument("tool", ItemArgument.item()).executes((context) -> {
                return dropFishingLoot(context, ResourceLocationArgument.getId(context, "loot_table"), BlockPosArgument.getLoadedBlockPos(context, "pos"), ItemArgument.getItem(context, "tool").createItemStack(1, false), constructor);
            })).then(Commands.literal("mainhand").executes((context) -> {
                return dropFishingLoot(context, ResourceLocationArgument.getId(context, "loot_table"), BlockPosArgument.getLoadedBlockPos(context, "pos"), getSourceHandItem(context.getSource(), EquipmentSlot.MAINHAND), constructor);
            })).then(Commands.literal("offhand").executes((context) -> {
                return dropFishingLoot(context, ResourceLocationArgument.getId(context, "loot_table"), BlockPosArgument.getLoadedBlockPos(context, "pos"), getSourceHandItem(context.getSource(), EquipmentSlot.OFFHAND), constructor);
            }))))).then(Commands.literal("loot").then(Commands.argument("loot_table", ResourceLocationArgument.id()).suggests(SUGGEST_LOOT_TABLE).executes((context) -> {
                return dropChestLoot(context, ResourceLocationArgument.getId(context, "loot_table"), constructor);
            }))).then(Commands.literal("kill").then(Commands.argument("target", EntityArgument.entity()).executes((context) -> {
                return dropKillLoot(context, EntityArgument.getEntity(context, "target"), constructor);
            }))).then(Commands.literal("mine").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((context) -> {
                return dropBlockLoot(context, BlockPosArgument.getLoadedBlockPos(context, "pos"), ItemStack.EMPTY, constructor);
            }).then(Commands.argument("tool", ItemArgument.item()).executes((context) -> {
                return dropBlockLoot(context, BlockPosArgument.getLoadedBlockPos(context, "pos"), ItemArgument.getItem(context, "tool").createItemStack(1, false), constructor);
            })).then(Commands.literal("mainhand").executes((context) -> {
                return dropBlockLoot(context, BlockPosArgument.getLoadedBlockPos(context, "pos"), getSourceHandItem(context.getSource(), EquipmentSlot.MAINHAND), constructor);
            })).then(Commands.literal("offhand").executes((context) -> {
                return dropBlockLoot(context, BlockPosArgument.getLoadedBlockPos(context, "pos"), getSourceHandItem(context.getSource(), EquipmentSlot.OFFHAND), constructor);
            }))));
        }));
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addTargets(T rootArgument, LootCommand.TailProvider sourceConstructor) {
        return rootArgument.then(Commands.literal("replace").then(Commands.literal("entity").then(Commands.argument("entities", EntityArgument.entities()).then(sourceConstructor.construct(Commands.argument("slot", SlotArgument.slot()), (context, stacks, messageSender) -> {
            return entityReplace(EntityArgument.getEntities(context, "entities"), SlotArgument.getSlot(context, "slot"), stacks.size(), stacks, messageSender);
        }).then(sourceConstructor.construct(Commands.argument("count", IntegerArgumentType.integer(0)), (context, stacks, messageSender) -> {
            return entityReplace(EntityArgument.getEntities(context, "entities"), SlotArgument.getSlot(context, "slot"), IntegerArgumentType.getInteger(context, "count"), stacks, messageSender);
        }))))).then(Commands.literal("block").then(Commands.argument("targetPos", BlockPosArgument.blockPos()).then(sourceConstructor.construct(Commands.argument("slot", SlotArgument.slot()), (context, stacks, messageSender) -> {
            return blockReplace(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "targetPos"), SlotArgument.getSlot(context, "slot"), stacks.size(), stacks, messageSender);
        }).then(sourceConstructor.construct(Commands.argument("count", IntegerArgumentType.integer(0)), (context, stacks, messageSender) -> {
            return blockReplace(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "targetPos"), IntegerArgumentType.getInteger(context, "slot"), IntegerArgumentType.getInteger(context, "count"), stacks, messageSender);
        })))))).then(Commands.literal("insert").then(sourceConstructor.construct(Commands.argument("targetPos", BlockPosArgument.blockPos()), (context, stacks, messageSender) -> {
            return blockDistribute(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "targetPos"), stacks, messageSender);
        }))).then(Commands.literal("give").then(sourceConstructor.construct(Commands.argument("players", EntityArgument.players()), (context, stacks, messageSender) -> {
            return playerGive(EntityArgument.getPlayers(context, "players"), stacks, messageSender);
        }))).then(Commands.literal("spawn").then(sourceConstructor.construct(Commands.argument("targetPos", Vec3Argument.vec3()), (context, stacks, messageSender) -> {
            return dropInWorld(context.getSource(), Vec3Argument.getVec3(context, "targetPos"), stacks, messageSender);
        })));
    }

    private static Container getContainer(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof Container)) {
            throw ItemCommands.ERROR_TARGET_NOT_A_CONTAINER.create(pos.getX(), pos.getY(), pos.getZ());
        } else {
            return (Container)blockEntity;
        }
    }

    private static int blockDistribute(CommandSourceStack source, BlockPos targetPos, List<ItemStack> stacks, LootCommand.Callback messageSender) throws CommandSyntaxException {
        Container container = getContainer(source, targetPos);
        List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

        for(ItemStack itemStack : stacks) {
            if (distributeToContainer(container, itemStack.copy())) {
                container.setChanged();
                list.add(itemStack);
            }
        }

        messageSender.accept(list);
        return list.size();
    }

    private static boolean distributeToContainer(Container inventory, ItemStack stack) {
        boolean bl = false;

        for(int i = 0; i < inventory.getContainerSize() && !stack.isEmpty(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (inventory.canPlaceItem(i, stack)) {
                if (itemStack.isEmpty()) {
                    inventory.setItem(i, stack);
                    bl = true;
                    break;
                }

                if (canMergeItems(itemStack, stack)) {
                    int j = stack.getMaxStackSize() - itemStack.getCount();
                    int k = Math.min(stack.getCount(), j);
                    stack.shrink(k);
                    itemStack.grow(k);
                    bl = true;
                }
            }
        }

        return bl;
    }

    private static int blockReplace(CommandSourceStack source, BlockPos targetPos, int slot, int stackCount, List<ItemStack> stacks, LootCommand.Callback messageSender) throws CommandSyntaxException {
        Container container = getContainer(source, targetPos);
        int i = container.getContainerSize();
        if (slot >= 0 && slot < i) {
            List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

            for(int j = 0; j < stackCount; ++j) {
                int k = slot + j;
                ItemStack itemStack = j < stacks.size() ? stacks.get(j) : ItemStack.EMPTY;
                if (container.canPlaceItem(k, itemStack)) {
                    container.setItem(k, itemStack);
                    list.add(itemStack);
                }
            }

            messageSender.accept(list);
            return list.size();
        } else {
            throw ItemCommands.ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
        }
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        return first.is(second.getItem()) && first.getDamageValue() == second.getDamageValue() && first.getCount() <= first.getMaxStackSize() && Objects.equals(first.getTag(), second.getTag());
    }

    private static int playerGive(Collection<ServerPlayer> players, List<ItemStack> stacks, LootCommand.Callback messageSender) throws CommandSyntaxException {
        List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

        for(ItemStack itemStack : stacks) {
            for(ServerPlayer serverPlayer : players) {
                if (serverPlayer.getInventory().add(itemStack.copy())) {
                    list.add(itemStack);
                }
            }
        }

        messageSender.accept(list);
        return list.size();
    }

    private static void setSlots(Entity entity, List<ItemStack> stacks, int slot, int stackCount, List<ItemStack> addedStacks) {
        for(int i = 0; i < stackCount; ++i) {
            ItemStack itemStack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            SlotAccess slotAccess = entity.getSlot(slot + i);
            if (slotAccess != SlotAccess.NULL && slotAccess.set(itemStack.copy())) {
                addedStacks.add(itemStack);
            }
        }

    }

    private static int entityReplace(Collection<? extends Entity> targets, int slot, int stackCount, List<ItemStack> stacks, LootCommand.Callback messageSender) throws CommandSyntaxException {
        List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

        for(Entity entity : targets) {
            if (entity instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)entity;
                setSlots(entity, stacks, slot, stackCount, list);
                serverPlayer.containerMenu.broadcastChanges();
            } else {
                setSlots(entity, stacks, slot, stackCount, list);
            }
        }

        messageSender.accept(list);
        return list.size();
    }

    private static int dropInWorld(CommandSourceStack source, Vec3 pos, List<ItemStack> stacks, LootCommand.Callback messageSender) throws CommandSyntaxException {
        ServerLevel serverLevel = source.getLevel();
        stacks.forEach((stack) -> {
            ItemEntity itemEntity = new ItemEntity(serverLevel, pos.x, pos.y, pos.z, stack.copy());
            itemEntity.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(itemEntity);
        });
        messageSender.accept(stacks);
        return stacks.size();
    }

    private static void callback(CommandSourceStack source, List<ItemStack> stacks) {
        if (stacks.size() == 1) {
            ItemStack itemStack = stacks.get(0);
            source.sendSuccess(new TranslatableComponent("commands.drop.success.single", itemStack.getCount(), itemStack.getDisplayName()), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.drop.success.multiple", stacks.size()), false);
        }

    }

    private static void callback(CommandSourceStack source, List<ItemStack> stacks, ResourceLocation lootTable) {
        if (stacks.size() == 1) {
            ItemStack itemStack = stacks.get(0);
            source.sendSuccess(new TranslatableComponent("commands.drop.success.single_with_table", itemStack.getCount(), itemStack.getDisplayName(), lootTable), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.drop.success.multiple_with_table", stacks.size(), lootTable), false);
        }

    }

    private static ItemStack getSourceHandItem(CommandSourceStack source, EquipmentSlot slot) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        if (entity instanceof LivingEntity) {
            return ((LivingEntity)entity).getItemBySlot(slot);
        } else {
            throw ERROR_NO_HELD_ITEMS.create(entity.getDisplayName());
        }
    }

    private static int dropBlockLoot(CommandContext<CommandSourceStack> context, BlockPos pos, ItemStack stack, LootCommand.DropConsumer constructor) throws CommandSyntaxException {
        CommandSourceStack commandSourceStack = context.getSource();
        ServerLevel serverLevel = commandSourceStack.getLevel();
        BlockState blockState = serverLevel.getBlockState(pos);
        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
        LootContext.Builder builder = (new LootContext.Builder(serverLevel)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.BLOCK_STATE, blockState).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity).withOptionalParameter(LootContextParams.THIS_ENTITY, commandSourceStack.getEntity()).withParameter(LootContextParams.TOOL, stack);
        List<ItemStack> list = blockState.getDrops(builder);
        return constructor.accept(context, list, (stacks) -> {
            callback(commandSourceStack, stacks, blockState.getBlock().getLootTable());
        });
    }

    private static int dropKillLoot(CommandContext<CommandSourceStack> context, Entity entity, LootCommand.DropConsumer constructor) throws CommandSyntaxException {
        if (!(entity instanceof LivingEntity)) {
            throw ERROR_NO_LOOT_TABLE.create(entity.getDisplayName());
        } else {
            ResourceLocation resourceLocation = ((LivingEntity)entity).getLootTable();
            CommandSourceStack commandSourceStack = context.getSource();
            LootContext.Builder builder = new LootContext.Builder(commandSourceStack.getLevel());
            Entity entity2 = commandSourceStack.getEntity();
            if (entity2 instanceof Player) {
                builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, (Player)entity2);
            }

            builder.withParameter(LootContextParams.DAMAGE_SOURCE, DamageSource.MAGIC);
            builder.withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, entity2);
            builder.withOptionalParameter(LootContextParams.KILLER_ENTITY, entity2);
            builder.withParameter(LootContextParams.THIS_ENTITY, entity);
            builder.withParameter(LootContextParams.ORIGIN, commandSourceStack.getPosition());
            LootTable lootTable = commandSourceStack.getServer().getLootTables().get(resourceLocation);
            List<ItemStack> list = lootTable.getRandomItems(builder.create(LootContextParamSets.ENTITY));
            return constructor.accept(context, list, (stacks) -> {
                callback(commandSourceStack, stacks, resourceLocation);
            });
        }
    }

    private static int dropChestLoot(CommandContext<CommandSourceStack> context, ResourceLocation lootTable, LootCommand.DropConsumer constructor) throws CommandSyntaxException {
        CommandSourceStack commandSourceStack = context.getSource();
        LootContext.Builder builder = (new LootContext.Builder(commandSourceStack.getLevel())).withOptionalParameter(LootContextParams.THIS_ENTITY, commandSourceStack.getEntity()).withParameter(LootContextParams.ORIGIN, commandSourceStack.getPosition());
        return drop(context, lootTable, builder.create(LootContextParamSets.CHEST), constructor);
    }

    private static int dropFishingLoot(CommandContext<CommandSourceStack> context, ResourceLocation lootTable, BlockPos pos, ItemStack stack, LootCommand.DropConsumer constructor) throws CommandSyntaxException {
        CommandSourceStack commandSourceStack = context.getSource();
        LootContext lootContext = (new LootContext.Builder(commandSourceStack.getLevel())).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, stack).withOptionalParameter(LootContextParams.THIS_ENTITY, commandSourceStack.getEntity()).create(LootContextParamSets.FISHING);
        return drop(context, lootTable, lootContext, constructor);
    }

    private static int drop(CommandContext<CommandSourceStack> context, ResourceLocation lootTable, LootContext lootContext, LootCommand.DropConsumer constructor) throws CommandSyntaxException {
        CommandSourceStack commandSourceStack = context.getSource();
        LootTable lootTable2 = commandSourceStack.getServer().getLootTables().get(lootTable);
        List<ItemStack> list = lootTable2.getRandomItems(lootContext);
        return constructor.accept(context, list, (stacks) -> {
            callback(commandSourceStack, stacks);
        });
    }

    @FunctionalInterface
    interface Callback {
        void accept(List<ItemStack> items) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface DropConsumer {
        int accept(CommandContext<CommandSourceStack> context, List<ItemStack> items, LootCommand.Callback messageSender) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface TailProvider {
        ArgumentBuilder<CommandSourceStack, ?> construct(ArgumentBuilder<CommandSourceStack, ?> builder, LootCommand.DropConsumer target);
    }
}
