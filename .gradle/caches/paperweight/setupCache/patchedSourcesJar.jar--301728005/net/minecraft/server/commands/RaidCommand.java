package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;

public class RaidCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("raid").requires((source) -> {
            return source.hasPermission(3);
        }).then(Commands.literal("start").then(Commands.argument("omenlvl", IntegerArgumentType.integer(0)).executes((context) -> {
            return start(context.getSource(), IntegerArgumentType.getInteger(context, "omenlvl"));
        }))).then(Commands.literal("stop").executes((context) -> {
            return stop(context.getSource());
        })).then(Commands.literal("check").executes((context) -> {
            return check(context.getSource());
        })).then(Commands.literal("sound").then(Commands.argument("type", ComponentArgument.textComponent()).executes((context) -> {
            return playSound(context.getSource(), ComponentArgument.getComponent(context, "type"));
        }))).then(Commands.literal("spawnleader").executes((context) -> {
            return spawnLeader(context.getSource());
        })).then(Commands.literal("setomen").then(Commands.argument("level", IntegerArgumentType.integer(0)).executes((context) -> {
            return setBadOmenLevel(context.getSource(), IntegerArgumentType.getInteger(context, "level"));
        }))).then(Commands.literal("glow").executes((context) -> {
            return glow(context.getSource());
        })));
    }

    private static int glow(CommandSourceStack source) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            for(Raider raider : raid.getAllRaiders()) {
                raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1000, 1));
            }
        }

        return 1;
    }

    private static int setBadOmenLevel(CommandSourceStack source, int level) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            int i = raid.getMaxBadOmenLevel();
            if (level > i) {
                source.sendFailure(new TextComponent("Sorry, the max bad omen level you can set is " + i));
            } else {
                int j = raid.getBadOmenLevel();
                raid.setBadOmenLevel(level);
                source.sendSuccess(new TextComponent("Changed village's bad omen level from " + j + " to " + level), false);
            }
        } else {
            source.sendFailure(new TextComponent("No raid found here"));
        }

        return 1;
    }

    private static int spawnLeader(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("Spawned a raid captain"), false);
        Raider raider = EntityType.PILLAGER.create(source.getLevel());
        raider.setPatrolLeader(true);
        raider.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
        raider.setPos(source.getPosition().x, source.getPosition().y, source.getPosition().z);
        raider.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(new BlockPos(source.getPosition())), MobSpawnType.COMMAND, (SpawnGroupData)null, (CompoundTag)null);
        source.getLevel().addFreshEntityWithPassengers(raider);
        return 1;
    }

    private static int playSound(CommandSourceStack source, Component type) {
        if (type != null && type.getString().equals("local")) {
            source.getLevel().playSound((Player)null, new BlockPos(source.getPosition().add(5.0D, 0.0D, 0.0D)), SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 2.0F, 1.0F);
        }

        return 1;
    }

    private static int start(CommandSourceStack source, int level) throws CommandSyntaxException {
        ServerPlayer serverPlayer = source.getPlayerOrException();
        BlockPos blockPos = serverPlayer.blockPosition();
        if (serverPlayer.getLevel().isRaided(blockPos)) {
            source.sendFailure(new TextComponent("Raid already started close by"));
            return -1;
        } else {
            Raids raids = serverPlayer.getLevel().getRaids();
            Raid raid = raids.createOrExtendRaid(serverPlayer);
            if (raid != null) {
                raid.setBadOmenLevel(level);
                raids.setDirty();
                source.sendSuccess(new TextComponent("Created a raid in your local village"), false);
            } else {
                source.sendFailure(new TextComponent("Failed to create a raid in your local village"));
            }

            return 1;
        }
    }

    private static int stop(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer serverPlayer = source.getPlayerOrException();
        BlockPos blockPos = serverPlayer.blockPosition();
        Raid raid = serverPlayer.getLevel().getRaidAt(blockPos);
        if (raid != null) {
            raid.stop();
            source.sendSuccess(new TextComponent("Stopped raid"), false);
            return 1;
        } else {
            source.sendFailure(new TextComponent("No raid here"));
            return -1;
        }
    }

    private static int check(CommandSourceStack source) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Found a started raid! ");
            source.sendSuccess(new TextComponent(stringBuilder.toString()), false);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Num groups spawned: ");
            stringBuilder.append(raid.getGroupsSpawned());
            stringBuilder.append(" Bad omen level: ");
            stringBuilder.append(raid.getBadOmenLevel());
            stringBuilder.append(" Num mobs: ");
            stringBuilder.append(raid.getTotalRaidersAlive());
            stringBuilder.append(" Raid health: ");
            stringBuilder.append(raid.getHealthOfLivingRaiders());
            stringBuilder.append(" / ");
            stringBuilder.append(raid.getTotalHealth());
            source.sendSuccess(new TextComponent(stringBuilder.toString()), false);
            return 1;
        } else {
            source.sendFailure(new TextComponent("Found no started raids"));
            return 0;
        }
    }

    @Nullable
    private static Raid getRaid(ServerPlayer player) {
        return player.getLevel().getRaidAt(player.blockPosition());
    }
}
