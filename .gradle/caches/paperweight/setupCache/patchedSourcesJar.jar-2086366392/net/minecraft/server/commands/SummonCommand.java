package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
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

    public SummonCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.Commands.literal("summon").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("entity", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES).executes((commandcontext) -> {
            return SummonCommand.spawnEntity((CommandSourceStack) commandcontext.getSource(), EntitySummonArgument.getSummonableEntity(commandcontext, "entity"), ((CommandSourceStack) commandcontext.getSource()).getPosition(), new CompoundTag(), true);
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("pos", Vec3Argument.vec3()).executes((commandcontext) -> {
            return SummonCommand.spawnEntity((CommandSourceStack) commandcontext.getSource(), EntitySummonArgument.getSummonableEntity(commandcontext, "entity"), Vec3Argument.getVec3(commandcontext, "pos"), new CompoundTag(), true);
        })).then(net.minecraft.commands.Commands.argument("nbt", CompoundTagArgument.compoundTag()).executes((commandcontext) -> {
            return SummonCommand.spawnEntity((CommandSourceStack) commandcontext.getSource(), EntitySummonArgument.getSummonableEntity(commandcontext, "entity"), Vec3Argument.getVec3(commandcontext, "pos"), CompoundTagArgument.getCompoundTag(commandcontext, "nbt"), false);
        })))));
    }

    private static int spawnEntity(CommandSourceStack source, ResourceLocation entity, Vec3 pos, CompoundTag nbt, boolean initialize) throws CommandSyntaxException {
        BlockPos blockposition = new BlockPos(pos);

        if (!Level.isInSpawnableBounds(blockposition)) {
            throw SummonCommand.INVALID_POSITION.create();
        } else {
            CompoundTag nbttagcompound1 = nbt.copy();

            nbttagcompound1.putString("id", entity.toString());
            ServerLevel worldserver = source.getLevel();
            Entity entity1 = EntityType.loadEntityRecursive(nbttagcompound1, worldserver, (loadedEntity) -> { // Paper - remap fix
                loadedEntity.moveTo(pos.x, pos.y, pos.z, loadedEntity.getYRot(), loadedEntity.getXRot()); // Paper - remap fix
                return loadedEntity; // Paper - remap fix
            });

            if (entity1 == null) {
                throw SummonCommand.ERROR_FAILED.create();
            } else {
                if (initialize && entity1 instanceof Mob) {
                    ((Mob) entity1).finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(entity1.blockPosition()), MobSpawnType.COMMAND, (SpawnGroupData) null, (CompoundTag) null);
                }

                if (!worldserver.tryAddFreshEntityWithPassengers(entity1, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.COMMAND)) { // CraftBukkit - pass a spawn reason of "COMMAND"
                    throw SummonCommand.ERROR_DUPLICATE_UUID.create();
                } else {
                    source.sendSuccess(new TranslatableComponent("commands.summon.success", new Object[]{entity1.getDisplayName()}), true);
                    return 1;
                }
            }
        }
    }
}
