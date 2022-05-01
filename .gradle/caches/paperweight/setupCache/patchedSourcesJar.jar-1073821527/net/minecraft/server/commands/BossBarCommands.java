package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class BossBarCommands {
    private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType((name) -> {
        return new TranslatableComponent("commands.bossbar.create.failed", name);
    });
    private static final DynamicCommandExceptionType ERROR_DOESNT_EXIST = new DynamicCommandExceptionType((name) -> {
        return new TranslatableComponent("commands.bossbar.unknown", name);
    });
    private static final SimpleCommandExceptionType ERROR_NO_PLAYER_CHANGE = new SimpleCommandExceptionType(new TranslatableComponent("commands.bossbar.set.players.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_NAME_CHANGE = new SimpleCommandExceptionType(new TranslatableComponent("commands.bossbar.set.name.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_COLOR_CHANGE = new SimpleCommandExceptionType(new TranslatableComponent("commands.bossbar.set.color.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_STYLE_CHANGE = new SimpleCommandExceptionType(new TranslatableComponent("commands.bossbar.set.style.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_VALUE_CHANGE = new SimpleCommandExceptionType(new TranslatableComponent("commands.bossbar.set.value.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_MAX_CHANGE = new SimpleCommandExceptionType(new TranslatableComponent("commands.bossbar.set.max.unchanged"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_HIDDEN = new SimpleCommandExceptionType(new TranslatableComponent("commands.bossbar.set.visibility.unchanged.hidden"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_VISIBLE = new SimpleCommandExceptionType(new TranslatableComponent("commands.bossbar.set.visibility.unchanged.visible"));
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_BOSS_BAR = (context, builder) -> {
        return SharedSuggestionProvider.suggestResource(context.getSource().getServer().getCustomBossEvents().getIds(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bossbar").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("add").then(Commands.argument("id", ResourceLocationArgument.id()).then(Commands.argument("name", ComponentArgument.textComponent()).executes((context) -> {
            return createBar(context.getSource(), ResourceLocationArgument.getId(context, "id"), ComponentArgument.getComponent(context, "name"));
        })))).then(Commands.literal("remove").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(SUGGEST_BOSS_BAR).executes((context) -> {
            return removeBar(context.getSource(), getBossBar(context));
        }))).then(Commands.literal("list").executes((context) -> {
            return listBars(context.getSource());
        })).then(Commands.literal("set").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(SUGGEST_BOSS_BAR).then(Commands.literal("name").then(Commands.argument("name", ComponentArgument.textComponent()).executes((context) -> {
            return setName(context.getSource(), getBossBar(context), ComponentArgument.getComponent(context, "name"));
        }))).then(Commands.literal("color").then(Commands.literal("pink").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossEvent.BossBarColor.PINK);
        })).then(Commands.literal("blue").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossEvent.BossBarColor.BLUE);
        })).then(Commands.literal("red").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossEvent.BossBarColor.RED);
        })).then(Commands.literal("green").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossEvent.BossBarColor.GREEN);
        })).then(Commands.literal("yellow").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossEvent.BossBarColor.YELLOW);
        })).then(Commands.literal("purple").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossEvent.BossBarColor.PURPLE);
        })).then(Commands.literal("white").executes((context) -> {
            return setColor(context.getSource(), getBossBar(context), BossEvent.BossBarColor.WHITE);
        }))).then(Commands.literal("style").then(Commands.literal("progress").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossEvent.BossBarOverlay.PROGRESS);
        })).then(Commands.literal("notched_6").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossEvent.BossBarOverlay.NOTCHED_6);
        })).then(Commands.literal("notched_10").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossEvent.BossBarOverlay.NOTCHED_10);
        })).then(Commands.literal("notched_12").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossEvent.BossBarOverlay.NOTCHED_12);
        })).then(Commands.literal("notched_20").executes((context) -> {
            return setStyle(context.getSource(), getBossBar(context), BossEvent.BossBarOverlay.NOTCHED_20);
        }))).then(Commands.literal("value").then(Commands.argument("value", IntegerArgumentType.integer(0)).executes((context) -> {
            return setValue(context.getSource(), getBossBar(context), IntegerArgumentType.getInteger(context, "value"));
        }))).then(Commands.literal("max").then(Commands.argument("max", IntegerArgumentType.integer(1)).executes((context) -> {
            return setMax(context.getSource(), getBossBar(context), IntegerArgumentType.getInteger(context, "max"));
        }))).then(Commands.literal("visible").then(Commands.argument("visible", BoolArgumentType.bool()).executes((context) -> {
            return setVisible(context.getSource(), getBossBar(context), BoolArgumentType.getBool(context, "visible"));
        }))).then(Commands.literal("players").executes((context) -> {
            return setPlayers(context.getSource(), getBossBar(context), Collections.emptyList());
        }).then(Commands.argument("targets", EntityArgument.players()).executes((context) -> {
            return setPlayers(context.getSource(), getBossBar(context), EntityArgument.getOptionalPlayers(context, "targets"));
        }))))).then(Commands.literal("get").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(SUGGEST_BOSS_BAR).then(Commands.literal("value").executes((context) -> {
            return getValue(context.getSource(), getBossBar(context));
        })).then(Commands.literal("max").executes((context) -> {
            return getMax(context.getSource(), getBossBar(context));
        })).then(Commands.literal("visible").executes((context) -> {
            return getVisible(context.getSource(), getBossBar(context));
        })).then(Commands.literal("players").executes((context) -> {
            return getPlayers(context.getSource(), getBossBar(context));
        })))));
    }

    private static int getValue(CommandSourceStack source, CustomBossEvent bossBar) {
        source.sendSuccess(new TranslatableComponent("commands.bossbar.get.value", bossBar.getDisplayName(), bossBar.getValue()), true);
        return bossBar.getValue();
    }

    private static int getMax(CommandSourceStack source, CustomBossEvent bossBar) {
        source.sendSuccess(new TranslatableComponent("commands.bossbar.get.max", bossBar.getDisplayName(), bossBar.getMax()), true);
        return bossBar.getMax();
    }

    private static int getVisible(CommandSourceStack source, CustomBossEvent bossBar) {
        if (bossBar.isVisible()) {
            source.sendSuccess(new TranslatableComponent("commands.bossbar.get.visible.visible", bossBar.getDisplayName()), true);
            return 1;
        } else {
            source.sendSuccess(new TranslatableComponent("commands.bossbar.get.visible.hidden", bossBar.getDisplayName()), true);
            return 0;
        }
    }

    private static int getPlayers(CommandSourceStack source, CustomBossEvent bossBar) {
        if (bossBar.getPlayers().isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.bossbar.get.players.none", bossBar.getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.bossbar.get.players.some", bossBar.getDisplayName(), bossBar.getPlayers().size(), ComponentUtils.formatList(bossBar.getPlayers(), Player::getDisplayName)), true);
        }

        return bossBar.getPlayers().size();
    }

    private static int setVisible(CommandSourceStack source, CustomBossEvent bossBar, boolean visible) throws CommandSyntaxException {
        if (bossBar.isVisible() == visible) {
            if (visible) {
                throw ERROR_ALREADY_VISIBLE.create();
            } else {
                throw ERROR_ALREADY_HIDDEN.create();
            }
        } else {
            bossBar.setVisible(visible);
            if (visible) {
                source.sendSuccess(new TranslatableComponent("commands.bossbar.set.visible.success.visible", bossBar.getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.bossbar.set.visible.success.hidden", bossBar.getDisplayName()), true);
            }

            return 0;
        }
    }

    private static int setValue(CommandSourceStack source, CustomBossEvent bossBar, int value) throws CommandSyntaxException {
        if (bossBar.getValue() == value) {
            throw ERROR_NO_VALUE_CHANGE.create();
        } else {
            bossBar.setValue(value);
            source.sendSuccess(new TranslatableComponent("commands.bossbar.set.value.success", bossBar.getDisplayName(), value), true);
            return value;
        }
    }

    private static int setMax(CommandSourceStack source, CustomBossEvent bossBar, int value) throws CommandSyntaxException {
        if (bossBar.getMax() == value) {
            throw ERROR_NO_MAX_CHANGE.create();
        } else {
            bossBar.setMax(value);
            source.sendSuccess(new TranslatableComponent("commands.bossbar.set.max.success", bossBar.getDisplayName(), value), true);
            return value;
        }
    }

    private static int setColor(CommandSourceStack source, CustomBossEvent bossBar, BossEvent.BossBarColor color) throws CommandSyntaxException {
        if (bossBar.getColor().equals(color)) {
            throw ERROR_NO_COLOR_CHANGE.create();
        } else {
            bossBar.setColor(color);
            source.sendSuccess(new TranslatableComponent("commands.bossbar.set.color.success", bossBar.getDisplayName()), true);
            return 0;
        }
    }

    private static int setStyle(CommandSourceStack source, CustomBossEvent bossBar, BossEvent.BossBarOverlay style) throws CommandSyntaxException {
        if (bossBar.getOverlay().equals(style)) {
            throw ERROR_NO_STYLE_CHANGE.create();
        } else {
            bossBar.setOverlay(style);
            source.sendSuccess(new TranslatableComponent("commands.bossbar.set.style.success", bossBar.getDisplayName()), true);
            return 0;
        }
    }

    private static int setName(CommandSourceStack source, CustomBossEvent bossBar, Component name) throws CommandSyntaxException {
        Component component = ComponentUtils.updateForEntity(source, name, (Entity)null, 0);
        if (bossBar.getName().equals(component)) {
            throw ERROR_NO_NAME_CHANGE.create();
        } else {
            bossBar.setName(component);
            source.sendSuccess(new TranslatableComponent("commands.bossbar.set.name.success", bossBar.getDisplayName()), true);
            return 0;
        }
    }

    private static int setPlayers(CommandSourceStack source, CustomBossEvent bossBar, Collection<ServerPlayer> players) throws CommandSyntaxException {
        boolean bl = bossBar.setPlayers(players);
        if (!bl) {
            throw ERROR_NO_PLAYER_CHANGE.create();
        } else {
            if (bossBar.getPlayers().isEmpty()) {
                source.sendSuccess(new TranslatableComponent("commands.bossbar.set.players.success.none", bossBar.getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.bossbar.set.players.success.some", bossBar.getDisplayName(), players.size(), ComponentUtils.formatList(players, Player::getDisplayName)), true);
            }

            return bossBar.getPlayers().size();
        }
    }

    private static int listBars(CommandSourceStack source) {
        Collection<CustomBossEvent> collection = source.getServer().getCustomBossEvents().getEvents();
        if (collection.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.bossbar.list.bars.none"), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.bossbar.list.bars.some", collection.size(), ComponentUtils.formatList(collection, CustomBossEvent::getDisplayName)), false);
        }

        return collection.size();
    }

    private static int createBar(CommandSourceStack source, ResourceLocation name, Component displayName) throws CommandSyntaxException {
        CustomBossEvents customBossEvents = source.getServer().getCustomBossEvents();
        if (customBossEvents.get(name) != null) {
            throw ERROR_ALREADY_EXISTS.create(name.toString());
        } else {
            CustomBossEvent customBossEvent = customBossEvents.create(name, ComponentUtils.updateForEntity(source, displayName, (Entity)null, 0));
            source.sendSuccess(new TranslatableComponent("commands.bossbar.create.success", customBossEvent.getDisplayName()), true);
            return customBossEvents.getEvents().size();
        }
    }

    private static int removeBar(CommandSourceStack source, CustomBossEvent bossBar) {
        CustomBossEvents customBossEvents = source.getServer().getCustomBossEvents();
        bossBar.removeAllPlayers();
        customBossEvents.remove(bossBar);
        source.sendSuccess(new TranslatableComponent("commands.bossbar.remove.success", bossBar.getDisplayName()), true);
        return customBossEvents.getEvents().size();
    }

    public static CustomBossEvent getBossBar(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation resourceLocation = ResourceLocationArgument.getId(context, "id");
        CustomBossEvent customBossEvent = context.getSource().getServer().getCustomBossEvents().get(resourceLocation);
        if (customBossEvent == null) {
            throw ERROR_DOESNT_EXIST.create(resourceLocation.toString());
        } else {
            return customBossEvent;
        }
    }
}
