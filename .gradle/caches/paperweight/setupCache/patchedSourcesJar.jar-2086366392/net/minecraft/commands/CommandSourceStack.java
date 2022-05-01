package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import com.mojang.brigadier.tree.CommandNode; // CraftBukkit

public class CommandSourceStack implements SharedSuggestionProvider, com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource { // Paper

    public static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(new TranslatableComponent("permissions.requires.player"));
    public static final SimpleCommandExceptionType ERROR_NOT_ENTITY = new SimpleCommandExceptionType(new TranslatableComponent("permissions.requires.entity"));
    public final CommandSource source;
    private final Vec3 worldPosition;
    private final ServerLevel level;
    private final int permissionLevel;
    private final String textName;
    private final Component displayName;
    private final MinecraftServer server;
    private final boolean silent;
    @Nullable
    private final Entity entity;
    @Nullable
    private final ResultConsumer<CommandSourceStack> consumer;
    private final EntityAnchorArgument.Anchor anchor;
    private final Vec2 rotation;
    public java.util.Map<Thread, CommandNode> currentCommand = new java.util.concurrent.ConcurrentHashMap<>(); // CraftBukkit // Paper

    public CommandSourceStack(CommandSource output, Vec3 pos, Vec2 rot, ServerLevel world, int level, String name, Component displayName, MinecraftServer server, @Nullable Entity entity) {
        this(output, pos, rot, world, level, name, displayName, server, entity, false, (commandcontext, flag, j) -> {
        }, EntityAnchorArgument.Anchor.FEET);
    }

    protected CommandSourceStack(CommandSource output, Vec3 pos, Vec2 rot, ServerLevel world, int level, String name, Component displayName, MinecraftServer server, @Nullable Entity entity, boolean silent, @Nullable ResultConsumer<CommandSourceStack> consumer, EntityAnchorArgument.Anchor entityAnchor) {
        this.source = output;
        this.worldPosition = pos;
        this.level = world;
        this.silent = silent;
        this.entity = entity;
        this.permissionLevel = level;
        this.textName = name;
        this.displayName = displayName;
        this.server = server;
        this.consumer = consumer;
        this.anchor = entityAnchor;
        this.rotation = rot;
    }

    public CommandSourceStack withSource(CommandSource output) {
        return this.source == output ? this : new CommandSourceStack(output, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandSourceStack withEntity(Entity entity) {
        return this.entity == entity ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, entity.getName().getString(), entity.getDisplayName(), this.server, entity, this.silent, this.consumer, this.anchor);
    }

    public CommandSourceStack withPosition(Vec3 position) {
        return this.worldPosition.equals(position) ? this : new CommandSourceStack(this.source, position, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandSourceStack withRotation(Vec2 rotation) {
        return this.rotation.equals(rotation) ? this : new CommandSourceStack(this.source, this.worldPosition, rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandSourceStack withCallback(ResultConsumer<CommandSourceStack> consumer) {
        return Objects.equals(this.consumer, consumer) ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, consumer, this.anchor);
    }

    public CommandSourceStack withCallback(ResultConsumer<CommandSourceStack> consumer, BinaryOperator<ResultConsumer<CommandSourceStack>> merger) {
        ResultConsumer<CommandSourceStack> resultconsumer1 = (ResultConsumer) merger.apply(this.consumer, consumer);

        return this.withCallback(resultconsumer1);
    }

    public CommandSourceStack withSuppressedOutput() {
        return !this.silent && !this.source.alwaysAccepts() ? new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, true, this.consumer, this.anchor) : this;
    }

    public CommandSourceStack withPermission(int level) {
        return level == this.permissionLevel ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, level, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandSourceStack withMaximumPermission(int level) {
        return level <= this.permissionLevel ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, level, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandSourceStack withAnchor(EntityAnchorArgument.Anchor anchor) {
        return anchor == this.anchor ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, anchor);
    }

    public CommandSourceStack withLevel(ServerLevel world) {
        if (world == this.level) {
            return this;
        } else {
            double d0 = DimensionType.getTeleportationScale(this.level.dimensionType(), world.dimensionType());
            Vec3 vec3d = new Vec3(this.worldPosition.x * d0, this.worldPosition.y, this.worldPosition.z * d0);

            return new CommandSourceStack(this.source, vec3d, this.rotation, world, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
        }
    }

    public CommandSourceStack facing(Entity entity, EntityAnchorArgument.Anchor anchor) {
        return this.facing(anchor.apply(entity));
    }

    public CommandSourceStack facing(Vec3 position) {
        Vec3 vec3d1 = this.anchor.apply(this);
        double d0 = position.x - vec3d1.x;
        double d1 = position.y - vec3d1.y;
        double d2 = position.z - vec3d1.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float f = Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 57.2957763671875D)));
        float f1 = Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875D) - 90.0F);

        return this.withRotation(new Vec2(f, f1));
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public String getTextName() {
        return this.textName;
    }

    // Paper start
    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return getEntity() != null ? getEntity().getBukkitEntity() : null;
    }

    @Override
    public org.bukkit.World getBukkitWorld() {
        return getLevel() != null ? getLevel().getWorld() : null;
    }

    @Override
    public org.bukkit.Location getBukkitLocation() {
        Vec3 pos = getPosition();
        org.bukkit.World world = getBukkitWorld();
        return world != null && pos != null ? new org.bukkit.Location(world, pos.x, pos.y, pos.z) : null;
    }
    // Paper end

    @Override
    public boolean hasPermission(int level) {
        // CraftBukkit start
        // Paper start - fix concurrency issue
        CommandNode currentCommand = this.currentCommand.get(Thread.currentThread());
        if (currentCommand != null) {
            return this.hasPermission(level, org.bukkit.craftbukkit.v1_18_R2.command.VanillaCommandWrapper.getPermission(currentCommand));
            // Paper end
        }
        // CraftBukkit end

        return this.permissionLevel >= level;
    }

    // CraftBukkit start
    public boolean hasPermission(int i, String bukkitPermission) {
        // World is null when loading functions
        return ((this.getLevel() == null || !this.getLevel().getCraftServer().ignoreVanillaPermissions) && this.permissionLevel >= i) || this.getBukkitSender().hasPermission(bukkitPermission);
    }
    // CraftBukkit end

    public Vec3 getPosition() {
        return this.worldPosition;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    @Nullable
    public Entity getEntity() {
        return this.entity;
    }

    public Entity getEntityOrException() throws CommandSyntaxException {
        if (this.entity == null) {
            throw CommandSourceStack.ERROR_NOT_ENTITY.create();
        } else {
            return this.entity;
        }
    }

    public ServerPlayer getPlayerOrException() throws CommandSyntaxException {
        if (!(this.entity instanceof ServerPlayer)) {
            throw CommandSourceStack.ERROR_NOT_PLAYER.create();
        } else {
            return (ServerPlayer) this.entity;
        }
    }

    public Vec2 getRotation() {
        return this.rotation;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public EntityAnchorArgument.Anchor getAnchor() {
        return this.anchor;
    }

    public void sendSuccess(Component message, boolean broadcastToOps) {
        if (this.source.acceptsSuccess() && !this.silent) {
            this.source.sendMessage(message, Util.NIL_UUID);
        }

        if (broadcastToOps && this.source.shouldInformAdmins() && !this.silent) {
            this.broadcastToAdmins(message);
        }

    }

    private void broadcastToAdmins(Component message) {
        MutableComponent ichatmutablecomponent = (new TranslatableComponent("chat.type.admin", new Object[]{this.getDisplayName(), message})).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});

        if (this.server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            Iterator iterator = this.server.getPlayerList().getPlayers().iterator();

            while (iterator.hasNext()) {
                ServerPlayer entityplayer = (ServerPlayer) iterator.next();

                if (entityplayer != this.source && entityplayer.getBukkitEntity().hasPermission("minecraft.admin.command_feedback")) { // CraftBukkit
                    entityplayer.sendMessage(ichatmutablecomponent, Util.NIL_UUID);
                }
            }
        }

        if (this.source != this.server && this.server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS) && !org.spigotmc.SpigotConfig.silentCommandBlocks) { // Spigot
            this.server.sendMessage(ichatmutablecomponent, Util.NIL_UUID);
        }

    }

    public void sendFailure(Component message) {
        if (this.source.acceptsFailure() && !this.silent) {
            this.source.sendMessage((new TextComponent("")).append(message).withStyle(ChatFormatting.RED), Util.NIL_UUID);
        }

    }

    public void onCommandComplete(CommandContext<CommandSourceStack> context, boolean success, int result) {
        if (this.consumer != null) {
            this.consumer.onCommandComplete(context, success, result);
        }

    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        return Lists.newArrayList(this.server.getPlayerNames());
    }

    @Override
    public Collection<String> getAllTeams() {
        return this.server.getScoreboard().getTeamNames();
    }

    @Override
    public Collection<ResourceLocation> getAvailableSoundEvents() {
        return Registry.SOUND_EVENT.keySet();
    }

    @Override
    public Stream<ResourceLocation> getRecipeNames() {
        return this.server.getRecipeManager().getRecipeIds();
    }

    @Override
    public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> context) {
        return Suggestions.empty();
    }

    @Override
    public CompletableFuture<Suggestions> suggestRegistryElements(ResourceKey<? extends Registry<?>> registryRef, SharedSuggestionProvider.ElementSuggestionType suggestedIdType, SuggestionsBuilder builder, CommandContext<?> context) {
        return (CompletableFuture) this.registryAccess().registry(registryRef).map((iregistry) -> {
            this.suggestRegistryElements(iregistry, suggestedIdType, builder);
            return builder.buildFuture();
        }).orElseGet(Suggestions::empty);
    }

    @Override
    public Set<ResourceKey<Level>> levels() {
        return this.server.levelKeys();
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.server.registryAccess();
    }

    // CraftBukkit start
    public org.bukkit.command.CommandSender getBukkitSender() {
        return this.source.getBukkitSender(this);
    }
    // CraftBukkit end
    // Paper start - override getSelectedEntities
    @Override
    public Collection<String> getSelectedEntities() {
        if (com.destroystokyo.paper.PaperConfig.fixTargetSelectorTagCompletion && this.source instanceof ServerPlayer player) {
            double pickDistance = player.gameMode.getGameModeForPlayer().isCreative() ? 5.0F : 4.5F;
            Vec3 min = player.getEyePosition(1.0F);
            Vec3 viewVector = player.getViewVector(1.0F);
            Vec3 max = min.add(viewVector.x * pickDistance, viewVector.y * pickDistance, viewVector.z * pickDistance);
            net.minecraft.world.phys.AABB aabb = player.getBoundingBox().expandTowards(viewVector.scale(pickDistance)).inflate(1.0D, 1.0D, 1.0D);
            pickDistance = player.gameMode.getGameModeForPlayer().isCreative() ? 6.0F : pickDistance;
            net.minecraft.world.phys.EntityHitResult hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, min, max, aabb, (e) -> !e.isSpectator() && e.isPickable(), pickDistance);
            return hitResult != null ? java.util.Collections.singletonList(hitResult.getEntity().getStringUUID()) : SharedSuggestionProvider.super.getSelectedEntities();
        }
        return SharedSuggestionProvider.super.getSelectedEntities();
    }
    // Paper end
}
