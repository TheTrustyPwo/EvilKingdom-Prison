package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TeleportCommand {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(new TranslatableComponent("commands.teleport.invalidPosition"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(Commands.literal("teleport").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("location", Vec3Argument.vec3()).executes((context) -> {
            return teleportToPos(context.getSource(), Collections.singleton(context.getSource().getEntityOrException()), context.getSource().getLevel(), Vec3Argument.getCoordinates(context, "location"), WorldCoordinates.current(), (TeleportCommand.LookAt)null);
        })).then(Commands.argument("destination", EntityArgument.entity()).executes((context) -> {
            return teleportToEntity(context.getSource(), Collections.singleton(context.getSource().getEntityOrException()), EntityArgument.getEntity(context, "destination"));
        })).then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("location", Vec3Argument.vec3()).executes((context) -> {
            return teleportToPos(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getSource().getLevel(), Vec3Argument.getCoordinates(context, "location"), (Coordinates)null, (TeleportCommand.LookAt)null);
        }).then(Commands.argument("rotation", RotationArgument.rotation()).executes((context) -> {
            return teleportToPos(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getSource().getLevel(), Vec3Argument.getCoordinates(context, "location"), RotationArgument.getRotation(context, "rotation"), (TeleportCommand.LookAt)null);
        })).then(Commands.literal("facing").then(Commands.literal("entity").then(Commands.argument("facingEntity", EntityArgument.entity()).executes((context) -> {
            return teleportToPos(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getSource().getLevel(), Vec3Argument.getCoordinates(context, "location"), (Coordinates)null, new TeleportCommand.LookAt(EntityArgument.getEntity(context, "facingEntity"), EntityAnchorArgument.Anchor.FEET));
        }).then(Commands.argument("facingAnchor", EntityAnchorArgument.anchor()).executes((context) -> {
            return teleportToPos(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getSource().getLevel(), Vec3Argument.getCoordinates(context, "location"), (Coordinates)null, new TeleportCommand.LookAt(EntityArgument.getEntity(context, "facingEntity"), EntityAnchorArgument.getAnchor(context, "facingAnchor")));
        })))).then(Commands.argument("facingLocation", Vec3Argument.vec3()).executes((context) -> {
            return teleportToPos(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getSource().getLevel(), Vec3Argument.getCoordinates(context, "location"), (Coordinates)null, new TeleportCommand.LookAt(Vec3Argument.getVec3(context, "facingLocation")));
        })))).then(Commands.argument("destination", EntityArgument.entity()).executes((context) -> {
            return teleportToEntity(context.getSource(), EntityArgument.getEntities(context, "targets"), EntityArgument.getEntity(context, "destination"));
        }))));
        dispatcher.register(Commands.literal("tp").requires((source) -> {
            return source.hasPermission(2);
        }).redirect(literalCommandNode));
    }

    private static int teleportToEntity(CommandSourceStack source, Collection<? extends Entity> targets, Entity destination) throws CommandSyntaxException {
        for(Entity entity : targets) {
            performTeleport(source, entity, (ServerLevel)destination.level, destination.getX(), destination.getY(), destination.getZ(), EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class), destination.getYRot(), destination.getXRot(), (TeleportCommand.LookAt)null);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.teleport.success.entity.single", targets.iterator().next().getDisplayName(), destination.getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.teleport.success.entity.multiple", targets.size(), destination.getDisplayName()), true);
        }

        return targets.size();
    }

    private static int teleportToPos(CommandSourceStack source, Collection<? extends Entity> targets, ServerLevel world, Coordinates location, @Nullable Coordinates rotation, @Nullable TeleportCommand.LookAt facingLocation) throws CommandSyntaxException {
        Vec3 vec3 = location.getPosition(source);
        Vec2 vec2 = rotation == null ? null : rotation.getRotation(source);
        Set<ClientboundPlayerPositionPacket.RelativeArgument> set = EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class);
        if (location.isXRelative()) {
            set.add(ClientboundPlayerPositionPacket.RelativeArgument.X);
        }

        if (location.isYRelative()) {
            set.add(ClientboundPlayerPositionPacket.RelativeArgument.Y);
        }

        if (location.isZRelative()) {
            set.add(ClientboundPlayerPositionPacket.RelativeArgument.Z);
        }

        if (rotation == null) {
            set.add(ClientboundPlayerPositionPacket.RelativeArgument.X_ROT);
            set.add(ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT);
        } else {
            if (rotation.isXRelative()) {
                set.add(ClientboundPlayerPositionPacket.RelativeArgument.X_ROT);
            }

            if (rotation.isYRelative()) {
                set.add(ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT);
            }
        }

        for(Entity entity : targets) {
            if (rotation == null) {
                performTeleport(source, entity, world, vec3.x, vec3.y, vec3.z, set, entity.getYRot(), entity.getXRot(), facingLocation);
            } else {
                performTeleport(source, entity, world, vec3.x, vec3.y, vec3.z, set, vec2.y, vec2.x, facingLocation);
            }
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.teleport.success.location.single", targets.iterator().next().getDisplayName(), formatDouble(vec3.x), formatDouble(vec3.y), formatDouble(vec3.z)), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.teleport.success.location.multiple", targets.size(), formatDouble(vec3.x), formatDouble(vec3.y), formatDouble(vec3.z)), true);
        }

        return targets.size();
    }

    private static String formatDouble(double d) {
        return String.format(Locale.ROOT, "%f", d);
    }

    private static void performTeleport(CommandSourceStack source, Entity target, ServerLevel world, double x, double y, double z, Set<ClientboundPlayerPositionPacket.RelativeArgument> movementFlags, float yaw, float pitch, @Nullable TeleportCommand.LookAt facingLocation) throws CommandSyntaxException {
        BlockPos blockPos = new BlockPos(x, y, z);
        if (!Level.isInSpawnableBounds(blockPos)) {
            throw INVALID_POSITION.create();
        } else {
            float f = Mth.wrapDegrees(yaw);
            float g = Mth.wrapDegrees(pitch);
            if (target instanceof ServerPlayer) {
                ChunkPos chunkPos = new ChunkPos(new BlockPos(x, y, z));
                world.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, target.getId());
                target.stopRiding();
                if (((ServerPlayer)target).isSleeping()) {
                    ((ServerPlayer)target).stopSleepInBed(true, true);
                }

                if (world == target.level) {
                    ((ServerPlayer)target).connection.teleport(x, y, z, f, g, movementFlags);
                } else {
                    ((ServerPlayer)target).teleportTo(world, x, y, z, f, g);
                }

                target.setYHeadRot(f);
            } else {
                float h = Mth.clamp(g, -90.0F, 90.0F);
                if (world == target.level) {
                    target.moveTo(x, y, z, f, h);
                    target.setYHeadRot(f);
                } else {
                    target.unRide();
                    Entity entity = target;
                    target = target.getType().create(world);
                    if (target == null) {
                        return;
                    }

                    target.restoreFrom(entity);
                    target.moveTo(x, y, z, f, h);
                    target.setYHeadRot(f);
                    entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
                    world.addDuringTeleport(target);
                }
            }

            if (facingLocation != null) {
                facingLocation.perform(source, target);
            }

            if (!(target instanceof LivingEntity) || !((LivingEntity)target).isFallFlying()) {
                target.setDeltaMovement(target.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
                target.setOnGround(true);
            }

            if (target instanceof PathfinderMob) {
                ((PathfinderMob)target).getNavigation().stop();
            }

        }
    }

    static class LookAt {
        private final Vec3 position;
        private final Entity entity;
        private final EntityAnchorArgument.Anchor anchor;

        public LookAt(Entity target, EntityAnchorArgument.Anchor targetAnchor) {
            this.entity = target;
            this.anchor = targetAnchor;
            this.position = targetAnchor.apply(target);
        }

        public LookAt(Vec3 targetPos) {
            this.entity = null;
            this.position = targetPos;
            this.anchor = null;
        }

        public void perform(CommandSourceStack source, Entity entity) {
            if (this.entity != null) {
                if (entity instanceof ServerPlayer) {
                    ((ServerPlayer)entity).lookAt(source.getAnchor(), this.entity, this.anchor);
                } else {
                    entity.lookAt(source.getAnchor(), this.position);
                }
            } else {
                entity.lookAt(source.getAnchor(), this.position);
            }

        }
    }
}
