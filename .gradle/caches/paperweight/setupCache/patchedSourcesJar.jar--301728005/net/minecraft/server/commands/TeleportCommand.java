package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
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
// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.event.entity.EntityTeleportEvent;
// CraftBukkit end

public class TeleportCommand {

    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(new TranslatableComponent("commands.teleport.invalidPosition"));

    public TeleportCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalcommandnode = dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.Commands.literal("teleport").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.Commands.argument("location", Vec3Argument.vec3()).executes((commandcontext) -> {
            return TeleportCommand.teleportToPos((CommandSourceStack) commandcontext.getSource(), Collections.singleton(((CommandSourceStack) commandcontext.getSource()).getEntityOrException()), ((CommandSourceStack) commandcontext.getSource()).getLevel(), Vec3Argument.getCoordinates(commandcontext, "location"), WorldCoordinates.current(), (TeleportCommand.LookAt) null);
        }))).then(net.minecraft.commands.Commands.argument("destination", EntityArgument.entity()).executes((commandcontext) -> {
            return TeleportCommand.teleportToEntity((CommandSourceStack) commandcontext.getSource(), Collections.singleton(((CommandSourceStack) commandcontext.getSource()).getEntityOrException()), EntityArgument.getEntity(commandcontext, "destination"));
        }))).then(((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("targets", EntityArgument.entities()).then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("location", Vec3Argument.vec3()).executes((commandcontext) -> {
            return TeleportCommand.teleportToPos((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), ((CommandSourceStack) commandcontext.getSource()).getLevel(), Vec3Argument.getCoordinates(commandcontext, "location"), (Coordinates) null, (TeleportCommand.LookAt) null);
        })).then(net.minecraft.commands.Commands.argument("rotation", RotationArgument.rotation()).executes((commandcontext) -> {
            return TeleportCommand.teleportToPos((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), ((CommandSourceStack) commandcontext.getSource()).getLevel(), Vec3Argument.getCoordinates(commandcontext, "location"), RotationArgument.getRotation(commandcontext, "rotation"), (TeleportCommand.LookAt) null);
        }))).then(((LiteralArgumentBuilder) net.minecraft.commands.Commands.literal("facing").then(net.minecraft.commands.Commands.literal("entity").then(((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("facingEntity", EntityArgument.entity()).executes((commandcontext) -> {
            return TeleportCommand.teleportToPos((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), ((CommandSourceStack) commandcontext.getSource()).getLevel(), Vec3Argument.getCoordinates(commandcontext, "location"), (Coordinates) null, new TeleportCommand.LookAt(EntityArgument.getEntity(commandcontext, "facingEntity"), EntityAnchorArgument.Anchor.FEET));
        })).then(net.minecraft.commands.Commands.argument("facingAnchor", EntityAnchorArgument.anchor()).executes((commandcontext) -> {
            return TeleportCommand.teleportToPos((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), ((CommandSourceStack) commandcontext.getSource()).getLevel(), Vec3Argument.getCoordinates(commandcontext, "location"), (Coordinates) null, new TeleportCommand.LookAt(EntityArgument.getEntity(commandcontext, "facingEntity"), EntityAnchorArgument.getAnchor(commandcontext, "facingAnchor")));
        }))))).then(net.minecraft.commands.Commands.argument("facingLocation", Vec3Argument.vec3()).executes((commandcontext) -> {
            return TeleportCommand.teleportToPos((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), ((CommandSourceStack) commandcontext.getSource()).getLevel(), Vec3Argument.getCoordinates(commandcontext, "location"), (Coordinates) null, new TeleportCommand.LookAt(Vec3Argument.getVec3(commandcontext, "facingLocation")));
        }))))).then(net.minecraft.commands.Commands.argument("destination", EntityArgument.entity()).executes((commandcontext) -> {
            return TeleportCommand.teleportToEntity((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), EntityArgument.getEntity(commandcontext, "destination"));
        }))));

        dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.Commands.literal("tp").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).redirect(literalcommandnode));
    }

    private static int teleportToEntity(CommandSourceStack source, Collection<? extends Entity> targets, Entity destination) throws CommandSyntaxException {
        Iterator iterator = targets.iterator();

        while (iterator.hasNext()) {
            Entity entity1 = (Entity) iterator.next();

            TeleportCommand.performTeleport(source, entity1, (ServerLevel) destination.level, destination.getX(), destination.getY(), destination.getZ(), EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class), destination.getYRot(), destination.getXRot(), (TeleportCommand.LookAt) null);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.teleport.success.entity.single", new Object[]{((Entity) targets.iterator().next()).getDisplayName(), destination.getDisplayName()}), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.teleport.success.entity.multiple", new Object[]{targets.size(), destination.getDisplayName()}), true);
        }

        return targets.size();
    }

    private static int teleportToPos(CommandSourceStack source, Collection<? extends Entity> targets, ServerLevel world, Coordinates location, @Nullable Coordinates rotation, @Nullable TeleportCommand.LookAt facingLocation) throws CommandSyntaxException {
        Vec3 vec3d = location.getPosition(source);
        Vec2 vec2f = rotation == null ? null : rotation.getRotation(source);
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

        Iterator iterator = targets.iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();

            if (rotation == null) {
                TeleportCommand.performTeleport(source, entity, world, vec3d.x, vec3d.y, vec3d.z, set, entity.getYRot(), entity.getXRot(), facingLocation);
            } else {
                TeleportCommand.performTeleport(source, entity, world, vec3d.x, vec3d.y, vec3d.z, set, vec2f.y, vec2f.x, facingLocation);
            }
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.teleport.success.location.single", new Object[]{((Entity) targets.iterator().next()).getDisplayName(), TeleportCommand.formatDouble(vec3d.x), TeleportCommand.formatDouble(vec3d.y), TeleportCommand.formatDouble(vec3d.z)}), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.teleport.success.location.multiple", new Object[]{targets.size(), TeleportCommand.formatDouble(vec3d.x), TeleportCommand.formatDouble(vec3d.y), TeleportCommand.formatDouble(vec3d.z)}), true);
        }

        return targets.size();
    }

    private static String formatDouble(double d) {
        return String.format(Locale.ROOT, "%f", d);
    }

    private static void performTeleport(CommandSourceStack source, Entity target, ServerLevel world, double x, double y, double z, Set<ClientboundPlayerPositionPacket.RelativeArgument> movementFlags, float yaw, float pitch, @Nullable TeleportCommand.LookAt facingLocation) throws CommandSyntaxException {
        BlockPos blockposition = new BlockPos(x, y, z);

        if (!Level.isInSpawnableBounds(blockposition)) {
            throw TeleportCommand.INVALID_POSITION.create();
        } else {
            float f2 = Mth.wrapDegrees(yaw);
            float f3 = Mth.wrapDegrees(pitch);

            if (target instanceof ServerPlayer) {
                ChunkPos chunkcoordintpair = new ChunkPos(new BlockPos(x, y, z));

                world.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkcoordintpair, 1, target.getId());
                target.stopRiding();
                if (((ServerPlayer) target).isSleeping()) {
                    ((ServerPlayer) target).stopSleepInBed(true, true);
                }

                if (world == target.level) {
                    ((ServerPlayer) target).connection.teleport(x, y, z, f2, f3, movementFlags, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND); // CraftBukkit
                } else {
                    ((ServerPlayer) target).teleportTo(world, x, y, z, f2, f3, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.COMMAND); // CraftBukkit
                }

                target.setYHeadRot(f2);
            } else {
                float f4 = Mth.clamp(f3, -90.0F, 90.0F);
                // CraftBukkit start - Teleport event
                Location to = new Location(world.getWorld(), x, y, z, f2, f4);
                EntityTeleportEvent event = new EntityTeleportEvent(target.getBukkitEntity(), target.getBukkitEntity().getLocation(), to);
                world.getCraftServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }

                x = to.getX();
                y = to.getY();
                z = to.getZ();
                f2 = to.getYaw();
                f4 = to.getPitch();
                world = ((CraftWorld) to.getWorld()).getHandle();
                // CraftBukkit end

                if (world == target.level) {
                    target.moveTo(x, y, z, f2, f4);
                    target.setYHeadRot(f2);
                } else {
                    target.unRide();
                    Entity entity1 = target;

                    target = target.getType().create(world);
                    if (target == null) {
                        return;
                    }

                    target.restoreFrom(entity1);
                    target.moveTo(x, y, z, f2, f4);
                    target.setYHeadRot(f2);
                    entity1.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
                    world.addDuringTeleport(target);
                }
            }

            if (facingLocation != null) {
                facingLocation.perform(source, target);
            }

            if (!(target instanceof LivingEntity) || !((LivingEntity) target).isFallFlying()) {
                target.setDeltaMovement(target.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
                target.setOnGround(true);
            }

            if (target instanceof PathfinderMob) {
                ((PathfinderMob) target).getNavigation().stop();
            }

        }
    }

    private static class LookAt {

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
                    ((ServerPlayer) entity).lookAt(source.getAnchor(), this.entity, this.anchor);
                } else {
                    entity.lookAt(source.getAnchor(), this.position);
                }
            } else {
                entity.lookAt(source.getAnchor(), this.position);
            }

        }
    }
}
