package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ADVANCEMENTS = (context, builder) -> {
        Collection<Advancement> collection = context.getSource().getServer().getAdvancements().getAllAdvancements();
        return SharedSuggestionProvider.suggestResource(collection.stream().map(Advancement::getId), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("advancement").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("grant").then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("only").then(Commands.argument("advancement", ResourceLocationArgument.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.GRANT, getAdvancements(ResourceLocationArgument.getAdvancement(context, "advancement"), AdvancementCommands.Mode.ONLY));
        }).then(Commands.argument("criterion", StringArgumentType.greedyString()).suggests((context, builder) -> {
            return SharedSuggestionProvider.suggest(ResourceLocationArgument.getAdvancement(context, "advancement").getCriteria().keySet(), builder);
        }).executes((context) -> {
            return performCriterion(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.GRANT, ResourceLocationArgument.getAdvancement(context, "advancement"), StringArgumentType.getString(context, "criterion"));
        })))).then(Commands.literal("from").then(Commands.argument("advancement", ResourceLocationArgument.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.GRANT, getAdvancements(ResourceLocationArgument.getAdvancement(context, "advancement"), AdvancementCommands.Mode.FROM));
        }))).then(Commands.literal("until").then(Commands.argument("advancement", ResourceLocationArgument.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.GRANT, getAdvancements(ResourceLocationArgument.getAdvancement(context, "advancement"), AdvancementCommands.Mode.UNTIL));
        }))).then(Commands.literal("through").then(Commands.argument("advancement", ResourceLocationArgument.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.GRANT, getAdvancements(ResourceLocationArgument.getAdvancement(context, "advancement"), AdvancementCommands.Mode.THROUGH));
        }))).then(Commands.literal("everything").executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.GRANT, context.getSource().getServer().getAdvancements().getAllAdvancements());
        })))).then(Commands.literal("revoke").then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("only").then(Commands.argument("advancement", ResourceLocationArgument.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.REVOKE, getAdvancements(ResourceLocationArgument.getAdvancement(context, "advancement"), AdvancementCommands.Mode.ONLY));
        }).then(Commands.argument("criterion", StringArgumentType.greedyString()).suggests((context, builder) -> {
            return SharedSuggestionProvider.suggest(ResourceLocationArgument.getAdvancement(context, "advancement").getCriteria().keySet(), builder);
        }).executes((context) -> {
            return performCriterion(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.REVOKE, ResourceLocationArgument.getAdvancement(context, "advancement"), StringArgumentType.getString(context, "criterion"));
        })))).then(Commands.literal("from").then(Commands.argument("advancement", ResourceLocationArgument.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.REVOKE, getAdvancements(ResourceLocationArgument.getAdvancement(context, "advancement"), AdvancementCommands.Mode.FROM));
        }))).then(Commands.literal("until").then(Commands.argument("advancement", ResourceLocationArgument.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.REVOKE, getAdvancements(ResourceLocationArgument.getAdvancement(context, "advancement"), AdvancementCommands.Mode.UNTIL));
        }))).then(Commands.literal("through").then(Commands.argument("advancement", ResourceLocationArgument.id()).suggests(SUGGEST_ADVANCEMENTS).executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.REVOKE, getAdvancements(ResourceLocationArgument.getAdvancement(context, "advancement"), AdvancementCommands.Mode.THROUGH));
        }))).then(Commands.literal("everything").executes((context) -> {
            return perform(context.getSource(), EntityArgument.getPlayers(context, "targets"), AdvancementCommands.Action.REVOKE, context.getSource().getServer().getAdvancements().getAllAdvancements());
        })))));
    }

    private static int perform(CommandSourceStack source, Collection<ServerPlayer> targets, AdvancementCommands.Action operation, Collection<Advancement> selection) {
        int i = 0;

        for(ServerPlayer serverPlayer : targets) {
            i += operation.perform(serverPlayer, selection);
        }

        if (i == 0) {
            if (selection.size() == 1) {
                if (targets.size() == 1) {
                    throw new CommandRuntimeException(new TranslatableComponent(operation.getKey() + ".one.to.one.failure", selection.iterator().next().getChatComponent(), targets.iterator().next().getDisplayName()));
                } else {
                    throw new CommandRuntimeException(new TranslatableComponent(operation.getKey() + ".one.to.many.failure", selection.iterator().next().getChatComponent(), targets.size()));
                }
            } else if (targets.size() == 1) {
                throw new CommandRuntimeException(new TranslatableComponent(operation.getKey() + ".many.to.one.failure", selection.size(), targets.iterator().next().getDisplayName()));
            } else {
                throw new CommandRuntimeException(new TranslatableComponent(operation.getKey() + ".many.to.many.failure", selection.size(), targets.size()));
            }
        } else {
            if (selection.size() == 1) {
                if (targets.size() == 1) {
                    source.sendSuccess(new TranslatableComponent(operation.getKey() + ".one.to.one.success", selection.iterator().next().getChatComponent(), targets.iterator().next().getDisplayName()), true);
                } else {
                    source.sendSuccess(new TranslatableComponent(operation.getKey() + ".one.to.many.success", selection.iterator().next().getChatComponent(), targets.size()), true);
                }
            } else if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent(operation.getKey() + ".many.to.one.success", selection.size(), targets.iterator().next().getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent(operation.getKey() + ".many.to.many.success", selection.size(), targets.size()), true);
            }

            return i;
        }
    }

    private static int performCriterion(CommandSourceStack source, Collection<ServerPlayer> targets, AdvancementCommands.Action operation, Advancement advancement, String criterion) {
        int i = 0;
        if (!advancement.getCriteria().containsKey(criterion)) {
            throw new CommandRuntimeException(new TranslatableComponent("commands.advancement.criterionNotFound", advancement.getChatComponent(), criterion));
        } else {
            for(ServerPlayer serverPlayer : targets) {
                if (operation.performCriterion(serverPlayer, advancement, criterion)) {
                    ++i;
                }
            }

            if (i == 0) {
                if (targets.size() == 1) {
                    throw new CommandRuntimeException(new TranslatableComponent(operation.getKey() + ".criterion.to.one.failure", criterion, advancement.getChatComponent(), targets.iterator().next().getDisplayName()));
                } else {
                    throw new CommandRuntimeException(new TranslatableComponent(operation.getKey() + ".criterion.to.many.failure", criterion, advancement.getChatComponent(), targets.size()));
                }
            } else {
                if (targets.size() == 1) {
                    source.sendSuccess(new TranslatableComponent(operation.getKey() + ".criterion.to.one.success", criterion, advancement.getChatComponent(), targets.iterator().next().getDisplayName()), true);
                } else {
                    source.sendSuccess(new TranslatableComponent(operation.getKey() + ".criterion.to.many.success", criterion, advancement.getChatComponent(), targets.size()), true);
                }

                return i;
            }
        }
    }

    private static List<Advancement> getAdvancements(Advancement advancement, AdvancementCommands.Mode selection) {
        List<Advancement> list = Lists.newArrayList();
        if (selection.parents) {
            for(Advancement advancement2 = advancement.getParent(); advancement2 != null; advancement2 = advancement2.getParent()) {
                list.add(advancement2);
            }
        }

        list.add(advancement);
        if (selection.children) {
            addChildren(advancement, list);
        }

        return list;
    }

    private static void addChildren(Advancement parent, List<Advancement> childList) {
        for(Advancement advancement : parent.getChildren()) {
            childList.add(advancement);
            addChildren(advancement, childList);
        }

    }

    static enum Action {
        GRANT("grant") {
            @Override
            protected boolean perform(ServerPlayer player, Advancement advancement) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (advancementProgress.isDone()) {
                    return false;
                } else {
                    for(String string : advancementProgress.getRemainingCriteria()) {
                        player.getAdvancements().award(advancement, string);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(ServerPlayer player, Advancement advancement, String criterion) {
                return player.getAdvancements().award(advancement, criterion);
            }
        },
        REVOKE("revoke") {
            @Override
            protected boolean perform(ServerPlayer player, Advancement advancement) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (!advancementProgress.hasProgress()) {
                    return false;
                } else {
                    for(String string : advancementProgress.getCompletedCriteria()) {
                        player.getAdvancements().revoke(advancement, string);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(ServerPlayer player, Advancement advancement, String criterion) {
                return player.getAdvancements().revoke(advancement, criterion);
            }
        };

        private final String key;

        Action(String name) {
            this.key = "commands.advancement." + name;
        }

        public int perform(ServerPlayer player, Iterable<Advancement> advancements) {
            int i = 0;

            for(Advancement advancement : advancements) {
                if (this.perform(player, advancement)) {
                    ++i;
                }
            }

            return i;
        }

        protected abstract boolean perform(ServerPlayer player, Advancement advancement);

        protected abstract boolean performCriterion(ServerPlayer player, Advancement advancement, String criterion);

        protected String getKey() {
            return this.key;
        }
    }

    static enum Mode {
        ONLY(false, false),
        THROUGH(true, true),
        FROM(false, true),
        UNTIL(true, false),
        EVERYTHING(true, true);

        final boolean parents;
        final boolean children;

        private Mode(boolean before, boolean after) {
            this.parents = before;
            this.children = after;
        }
    }
}
