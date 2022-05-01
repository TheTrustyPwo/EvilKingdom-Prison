package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec2;

public class WorldBorderCommand {
    private static final SimpleCommandExceptionType ERROR_SAME_CENTER = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.center.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_SIZE = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.set.failed.nochange"));
    private static final SimpleCommandExceptionType ERROR_TOO_SMALL = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.set.failed.small"));
    private static final SimpleCommandExceptionType ERROR_TOO_BIG = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.set.failed.big", 5.9999968E7D));
    private static final SimpleCommandExceptionType ERROR_TOO_FAR_OUT = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.set.failed.far", 2.9999984E7D));
    private static final SimpleCommandExceptionType ERROR_SAME_WARNING_TIME = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.warning.time.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_WARNING_DISTANCE = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.warning.distance.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_BUFFER = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.damage.buffer.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_AMOUNT = new SimpleCommandExceptionType(new TranslatableComponent("commands.worldborder.damage.amount.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("worldborder").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("add").then(Commands.argument("distance", DoubleArgumentType.doubleArg(-5.9999968E7D, 5.9999968E7D)).executes((context) -> {
            return setSize(context.getSource(), context.getSource().getLevel().getWorldBorder().getSize() + DoubleArgumentType.getDouble(context, "distance"), 0L);
        }).then(Commands.argument("time", IntegerArgumentType.integer(0)).executes((context) -> {
            return setSize(context.getSource(), context.getSource().getLevel().getWorldBorder().getSize() + DoubleArgumentType.getDouble(context, "distance"), context.getSource().getLevel().getWorldBorder().getLerpRemainingTime() + (long)IntegerArgumentType.getInteger(context, "time") * 1000L);
        })))).then(Commands.literal("set").then(Commands.argument("distance", DoubleArgumentType.doubleArg(-5.9999968E7D, 5.9999968E7D)).executes((context) -> {
            return setSize(context.getSource(), DoubleArgumentType.getDouble(context, "distance"), 0L);
        }).then(Commands.argument("time", IntegerArgumentType.integer(0)).executes((context) -> {
            return setSize(context.getSource(), DoubleArgumentType.getDouble(context, "distance"), (long)IntegerArgumentType.getInteger(context, "time") * 1000L);
        })))).then(Commands.literal("center").then(Commands.argument("pos", Vec2Argument.vec2()).executes((context) -> {
            return setCenter(context.getSource(), Vec2Argument.getVec2(context, "pos"));
        }))).then(Commands.literal("damage").then(Commands.literal("amount").then(Commands.argument("damagePerBlock", FloatArgumentType.floatArg(0.0F)).executes((context) -> {
            return setDamageAmount(context.getSource(), FloatArgumentType.getFloat(context, "damagePerBlock"));
        }))).then(Commands.literal("buffer").then(Commands.argument("distance", FloatArgumentType.floatArg(0.0F)).executes((context) -> {
            return setDamageBuffer(context.getSource(), FloatArgumentType.getFloat(context, "distance"));
        })))).then(Commands.literal("get").executes((context) -> {
            return getSize(context.getSource());
        })).then(Commands.literal("warning").then(Commands.literal("distance").then(Commands.argument("distance", IntegerArgumentType.integer(0)).executes((context) -> {
            return setWarningDistance(context.getSource(), IntegerArgumentType.getInteger(context, "distance"));
        }))).then(Commands.literal("time").then(Commands.argument("time", IntegerArgumentType.integer(0)).executes((context) -> {
            return setWarningTime(context.getSource(), IntegerArgumentType.getInteger(context, "time"));
        })))));
    }

    private static int setDamageBuffer(CommandSourceStack source, float distance) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getDamageSafeZone() == (double)distance) {
            throw ERROR_SAME_DAMAGE_BUFFER.create();
        } else {
            worldBorder.setDamageSafeZone((double)distance);
            source.sendSuccess(new TranslatableComponent("commands.worldborder.damage.buffer.success", String.format(Locale.ROOT, "%.2f", distance)), true);
            return (int)distance;
        }
    }

    private static int setDamageAmount(CommandSourceStack source, float damagePerBlock) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getDamagePerBlock() == (double)damagePerBlock) {
            throw ERROR_SAME_DAMAGE_AMOUNT.create();
        } else {
            worldBorder.setDamagePerBlock((double)damagePerBlock);
            source.sendSuccess(new TranslatableComponent("commands.worldborder.damage.amount.success", String.format(Locale.ROOT, "%.2f", damagePerBlock)), true);
            return (int)damagePerBlock;
        }
    }

    private static int setWarningTime(CommandSourceStack source, int time) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getWarningTime() == time) {
            throw ERROR_SAME_WARNING_TIME.create();
        } else {
            worldBorder.setWarningTime(time);
            source.sendSuccess(new TranslatableComponent("commands.worldborder.warning.time.success", time), true);
            return time;
        }
    }

    private static int setWarningDistance(CommandSourceStack source, int distance) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getWarningBlocks() == distance) {
            throw ERROR_SAME_WARNING_DISTANCE.create();
        } else {
            worldBorder.setWarningBlocks(distance);
            source.sendSuccess(new TranslatableComponent("commands.worldborder.warning.distance.success", distance), true);
            return distance;
        }
    }

    private static int getSize(CommandSourceStack source) {
        double d = source.getServer().overworld().getWorldBorder().getSize();
        source.sendSuccess(new TranslatableComponent("commands.worldborder.get", String.format(Locale.ROOT, "%.0f", d)), false);
        return Mth.floor(d + 0.5D);
    }

    private static int setCenter(CommandSourceStack source, Vec2 pos) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        if (worldBorder.getCenterX() == (double)pos.x && worldBorder.getCenterZ() == (double)pos.y) {
            throw ERROR_SAME_CENTER.create();
        } else if (!((double)Math.abs(pos.x) > 2.9999984E7D) && !((double)Math.abs(pos.y) > 2.9999984E7D)) {
            worldBorder.setCenter((double)pos.x, (double)pos.y);
            source.sendSuccess(new TranslatableComponent("commands.worldborder.center.success", String.format(Locale.ROOT, "%.2f", pos.x), String.format("%.2f", pos.y)), true);
            return 0;
        } else {
            throw ERROR_TOO_FAR_OUT.create();
        }
    }

    private static int setSize(CommandSourceStack source, double distance, long time) throws CommandSyntaxException {
        WorldBorder worldBorder = source.getServer().overworld().getWorldBorder();
        double d = worldBorder.getSize();
        if (d == distance) {
            throw ERROR_SAME_SIZE.create();
        } else if (distance < 1.0D) {
            throw ERROR_TOO_SMALL.create();
        } else if (distance > 5.9999968E7D) {
            throw ERROR_TOO_BIG.create();
        } else {
            if (time > 0L) {
                worldBorder.lerpSizeBetween(d, distance, time);
                if (distance > d) {
                    source.sendSuccess(new TranslatableComponent("commands.worldborder.set.grow", String.format(Locale.ROOT, "%.1f", distance), Long.toString(time / 1000L)), true);
                } else {
                    source.sendSuccess(new TranslatableComponent("commands.worldborder.set.shrink", String.format(Locale.ROOT, "%.1f", distance), Long.toString(time / 1000L)), true);
                }
            } else {
                worldBorder.setSize(distance);
                source.sendSuccess(new TranslatableComponent("commands.worldborder.set.immediate", String.format(Locale.ROOT, "%.1f", distance)), true);
            }

            return (int)(distance - d);
        }
    }
}
