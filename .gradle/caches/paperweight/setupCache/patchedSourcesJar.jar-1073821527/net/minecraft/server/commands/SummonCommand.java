package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SummonCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.summon.failed"));
    private static final SimpleCommandExceptionType ERROR_DUPLICATE_UUID = new SimpleCommandExceptionType(new TranslatableComponent("commands.summon.failed.uuid"));
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(new TranslatableComponent("commands.summon.invalidPosition"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("summon").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("entity", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES).executes((context) -> {
            return spawnEntity(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "entity"), context.getSource().getPosition(), new CompoundTag(), true);
        }).then(Commands.argument("pos", Vec3Argument.vec3()).executes((context) -> {
            return spawnEntity(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "entity"), Vec3Argument.getVec3(context, "pos"), new CompoundTag(), true);
        }).then(Commands.argument("nbt", CompoundTagArgument.compoundTag()).executes((context) -> {
            return spawnEntity(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "entity"), Vec3Argument.getVec3(context, "pos"), CompoundTagArgument.getCompoundTag(context, "nbt"), false);
        })))));
    }

    private static int spawnEntity(CommandSourceStack source, ResourceLocation entity, Vec3 pos, CompoundTag nbt, boolean initialize) throws CommandSyntaxException {
        BlockPos blockPos = new BlockPos(pos);
        if (!Level.isInSpawnableBounds(blockPos)) {
            throw INVALID_POSITION.create();
        } else {
            CompoundTag compoundTag = nbt.copy();
            compoundTag.putString("id", entity.toString());
            ServerLevel serverLevel = source.getLevel();
            Entity entity2 = EntityType.loadEntityRecursive(compoundTag, serverLevel, (entityx) -> {
                entityx.moveTo(pos.x, pos.y, pos.z, entityx.getYRot(), entityx.getXRot());
                return entityx;
            });
            if (entity2 == null) {
                throw ERROR_FAILED.create();
            } else {
                if (initialize && entity2 instanceof Mob) {
                    ((Mob)entity2).finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(entity2.blockPosition()), MobSpawnType.COMMAND, (SpawnGroupData)null, (CompoundTag)null);
                }

                if (!serverLevel.tryAddFreshEntityWithPassengers(entity2)) {
                    throw ERROR_DUPLICATE_UUID.create();
                } else {
                    source.sendSuccess(new TranslatableComponent("commands.summon.success", entity2.getDisplayName()), true);
                    return 1;
                }
            }
        }
    }
}
