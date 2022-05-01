package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public abstract class PlayerList {
    public static final File USERBANLIST_FILE = new File("banned-players.json");
    public static final File IPBANLIST_FILE = new File("banned-ips.json");
    public static final File OPLIST_FILE = new File("ops.json");
    public static final File WHITELIST_FILE = new File("whitelist.json");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEND_PLAYER_INFO_INTERVAL = 600;
    private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    private final MinecraftServer server;
    public final List<ServerPlayer> players = Lists.newArrayList();
    private final Map<UUID, ServerPlayer> playersByUUID = Maps.newHashMap();
    private final UserBanList bans = new UserBanList(USERBANLIST_FILE);
    private final IpBanList ipBans = new IpBanList(IPBANLIST_FILE);
    private final ServerOpList ops = new ServerOpList(OPLIST_FILE);
    private final UserWhiteList whitelist = new UserWhiteList(WHITELIST_FILE);
    private final Map<UUID, ServerStatsCounter> stats = Maps.newHashMap();
    private final Map<UUID, PlayerAdvancements> advancements = Maps.newHashMap();
    public final PlayerDataStorage playerIo;
    private boolean doWhiteList;
    private final RegistryAccess.Frozen registryHolder;
    protected final int maxPlayers;
    private int viewDistance;
    private int simulationDistance;
    private boolean allowCheatsForAllPlayers;
    private static final boolean ALLOW_LOGOUTIVATOR = false;
    private int sendAllPlayerInfoIn;

    public PlayerList(MinecraftServer server, RegistryAccess.Frozen registryManager, PlayerDataStorage saveHandler, int maxPlayers) {
        this.server = server;
        this.registryHolder = registryManager;
        this.maxPlayers = maxPlayers;
        this.playerIo = saveHandler;
    }

    public void placeNewPlayer(Connection connection, ServerPlayer player) {
        GameProfile gameProfile = player.getGameProfile();
        GameProfileCache gameProfileCache = this.server.getProfileCache();
        Optional<GameProfile> optional = gameProfileCache.get(gameProfile.getId());
        String string = optional.map(GameProfile::getName).orElse(gameProfile.getName());
        gameProfileCache.add(gameProfile);
        CompoundTag compoundTag = this.load(player);
        ResourceKey<Level> resourceKey = compoundTag != null ? DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, compoundTag.get("Dimension"))).resultOrPartial(LOGGER::error).orElse(Level.OVERWORLD) : Level.OVERWORLD;
        ServerLevel serverLevel = this.server.getLevel(resourceKey);
        ServerLevel serverLevel2;
        if (serverLevel == null) {
            LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", (Object)resourceKey);
            serverLevel2 = this.server.overworld();
        } else {
            serverLevel2 = serverLevel;
        }

        player.setLevel(serverLevel2);
        String string2 = "local";
        if (connection.getRemoteAddress() != null) {
            string2 = connection.getRemoteAddress().toString();
        }

        LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", player.getName().getString(), string2, player.getId(), player.getX(), player.getY(), player.getZ());
        LevelData levelData = serverLevel2.getLevelData();
        player.loadGameTypes(compoundTag);
        ServerGamePacketListenerImpl serverGamePacketListenerImpl = new ServerGamePacketListenerImpl(this.server, connection, player);
        GameRules gameRules = serverLevel2.getGameRules();
        boolean bl = gameRules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
        boolean bl2 = gameRules.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);
        serverGamePacketListenerImpl.send(new ClientboundLoginPacket(player.getId(), levelData.isHardcore(), player.gameMode.getGameModeForPlayer(), player.gameMode.getPreviousGameModeForPlayer(), this.server.levelKeys(), this.registryHolder, serverLevel2.dimensionTypeRegistration(), serverLevel2.dimension(), BiomeManager.obfuscateSeed(serverLevel2.getSeed()), this.getMaxPlayers(), this.viewDistance, this.simulationDistance, bl2, !bl, serverLevel2.isDebug(), serverLevel2.isFlat()));
        serverGamePacketListenerImpl.send(new ClientboundCustomPayloadPacket(ClientboundCustomPayloadPacket.BRAND, (new FriendlyByteBuf(Unpooled.buffer())).writeUtf(this.getServer().getServerModName())));
        serverGamePacketListenerImpl.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        serverGamePacketListenerImpl.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
        serverGamePacketListenerImpl.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
        serverGamePacketListenerImpl.send(new ClientboundUpdateRecipesPacket(this.server.getRecipeManager().getRecipes()));
        serverGamePacketListenerImpl.send(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registryHolder)));
        this.sendPlayerPermissionLevel(player);
        player.getStats().markAllDirty();
        player.getRecipeBook().sendInitialRecipeBook(player);
        this.updateEntireScoreboard(serverLevel2.getScoreboard(), player);
        this.server.invalidateStatus();
        MutableComponent mutableComponent;
        if (player.getGameProfile().getName().equalsIgnoreCase(string)) {
            mutableComponent = new TranslatableComponent("multiplayer.player.joined", player.getDisplayName());
        } else {
            mutableComponent = new TranslatableComponent("multiplayer.player.joined.renamed", player.getDisplayName(), string);
        }

        this.broadcastMessage(mutableComponent.withStyle(ChatFormatting.YELLOW), ChatType.SYSTEM, Util.NIL_UUID);
        serverGamePacketListenerImpl.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        this.players.add(player);
        this.playersByUUID.put(player.getUUID(), player);
        this.broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, player));

        for(int i = 0; i < this.players.size(); ++i) {
            player.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, this.players.get(i)));
        }

        serverLevel2.addNewPlayer(player);
        this.server.getCustomBossEvents().onPlayerConnect(player);
        this.sendLevelInfo(player, serverLevel2);
        if (!this.server.getResourcePack().isEmpty()) {
            player.sendTexturePack(this.server.getResourcePack(), this.server.getResourcePackHash(), this.server.isResourcePackRequired(), this.server.getResourcePackPrompt());
        }

        for(MobEffectInstance mobEffectInstance : player.getActiveEffects()) {
            serverGamePacketListenerImpl.send(new ClientboundUpdateMobEffectPacket(player.getId(), mobEffectInstance));
        }

        if (compoundTag != null && compoundTag.contains("RootVehicle", 10)) {
            CompoundTag compoundTag2 = compoundTag.getCompound("RootVehicle");
            Entity entity = EntityType.loadEntityRecursive(compoundTag2.getCompound("Entity"), serverLevel2, (vehicle) -> {
                return !serverLevel2.addWithUUID(vehicle) ? null : vehicle;
            });
            if (entity != null) {
                UUID uUID;
                if (compoundTag2.hasUUID("Attach")) {
                    uUID = compoundTag2.getUUID("Attach");
                } else {
                    uUID = null;
                }

                if (entity.getUUID().equals(uUID)) {
                    player.startRiding(entity, true);
                } else {
                    for(Entity entity2 : entity.getIndirectPassengers()) {
                        if (entity2.getUUID().equals(uUID)) {
                            player.startRiding(entity2, true);
                            break;
                        }
                    }
                }

                if (!player.isPassenger()) {
                    LOGGER.warn("Couldn't reattach entity to player");
                    entity.discard();

                    for(Entity entity3 : entity.getIndirectPassengers()) {
                        entity3.discard();
                    }
                }
            }
        }

        player.initInventoryMenu();
    }

    public void updateEntireScoreboard(ServerScoreboard scoreboard, ServerPlayer player) {
        Set<Objective> set = Sets.newHashSet();

        for(PlayerTeam playerTeam : scoreboard.getPlayerTeams()) {
            player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true));
        }

        for(int i = 0; i < 19; ++i) {
            Objective objective = scoreboard.getDisplayObjective(i);
            if (objective != null && !set.contains(objective)) {
                for(Packet<?> packet : scoreboard.getStartTrackingPackets(objective)) {
                    player.connection.send(packet);
                }

                set.add(objective);
            }
        }

    }

    public void addWorldborderListener(ServerLevel world) {
        world.getWorldBorder().addListener(new BorderChangeListener() {
            @Override
            public void onBorderSizeSet(WorldBorder border, double size) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderSizePacket(border));
            }

            @Override
            public void onBorderSizeLerping(WorldBorder border, double fromSize, double toSize, long time) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderLerpSizePacket(border));
            }

            @Override
            public void onBorderCenterSet(WorldBorder border, double centerX, double centerZ) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderCenterPacket(border));
            }

            @Override
            public void onBorderSetWarningTime(WorldBorder border, int warningTime) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDelayPacket(border));
            }

            @Override
            public void onBorderSetWarningBlocks(WorldBorder border, int warningBlockDistance) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDistancePacket(border));
            }

            @Override
            public void onBorderSetDamagePerBlock(WorldBorder border, double damagePerBlock) {
            }

            @Override
            public void onBorderSetDamageSafeZOne(WorldBorder border, double safeZoneRadius) {
            }
        });
    }

    @Nullable
    public CompoundTag load(ServerPlayer player) {
        CompoundTag compoundTag = this.server.getWorldData().getLoadedPlayerTag();
        CompoundTag compoundTag2;
        if (player.getName().getString().equals(this.server.getSingleplayerName()) && compoundTag != null) {
            compoundTag2 = compoundTag;
            player.load(compoundTag);
            LOGGER.debug("loading single player");
        } else {
            compoundTag2 = this.playerIo.load(player);
        }

        return compoundTag2;
    }

    protected void save(ServerPlayer player) {
        this.playerIo.save(player);
        ServerStatsCounter serverStatsCounter = this.stats.get(player.getUUID());
        if (serverStatsCounter != null) {
            serverStatsCounter.save();
        }

        PlayerAdvancements playerAdvancements = this.advancements.get(player.getUUID());
        if (playerAdvancements != null) {
            playerAdvancements.save();
        }

    }

    public void remove(ServerPlayer player) {
        ServerLevel serverLevel = player.getLevel();
        player.awardStat(Stats.LEAVE_GAME);
        this.save(player);
        if (player.isPassenger()) {
            Entity entity = player.getRootVehicle();
            if (entity.hasExactlyOnePlayerPassenger()) {
                LOGGER.debug("Removing player mount");
                player.stopRiding();
                entity.getPassengersAndSelf().forEach((entity) -> {
                    entity.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                });
            }
        }

        player.unRide();
        serverLevel.removePlayerImmediately(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        player.getAdvancements().stopListening();
        this.players.remove(player);
        this.server.getCustomBossEvents().onPlayerDisconnect(player);
        UUID uUID = player.getUUID();
        ServerPlayer serverPlayer = this.playersByUUID.get(uUID);
        if (serverPlayer == player) {
            this.playersByUUID.remove(uUID);
            this.stats.remove(uUID);
            this.advancements.remove(uUID);
        }

        this.broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, player));
    }

    @Nullable
    public Component canPlayerLogin(SocketAddress address, GameProfile profile) {
        if (this.bans.isBanned(profile)) {
            UserBanListEntry userBanListEntry = this.bans.get(profile);
            MutableComponent mutableComponent = new TranslatableComponent("multiplayer.disconnect.banned.reason", userBanListEntry.getReason());
            if (userBanListEntry.getExpires() != null) {
                mutableComponent.append(new TranslatableComponent("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(userBanListEntry.getExpires())));
            }

            return mutableComponent;
        } else if (!this.isWhiteListed(profile)) {
            return new TranslatableComponent("multiplayer.disconnect.not_whitelisted");
        } else if (this.ipBans.isBanned(address)) {
            IpBanListEntry ipBanListEntry = this.ipBans.get(address);
            MutableComponent mutableComponent2 = new TranslatableComponent("multiplayer.disconnect.banned_ip.reason", ipBanListEntry.getReason());
            if (ipBanListEntry.getExpires() != null) {
                mutableComponent2.append(new TranslatableComponent("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ipBanListEntry.getExpires())));
            }

            return mutableComponent2;
        } else {
            return this.players.size() >= this.maxPlayers && !this.canBypassPlayerLimit(profile) ? new TranslatableComponent("multiplayer.disconnect.server_full") : null;
        }
    }

    public ServerPlayer getPlayerForLogin(GameProfile profile) {
        UUID uUID = Player.createPlayerUUID(profile);
        List<ServerPlayer> list = Lists.newArrayList();

        for(int i = 0; i < this.players.size(); ++i) {
            ServerPlayer serverPlayer = this.players.get(i);
            if (serverPlayer.getUUID().equals(uUID)) {
                list.add(serverPlayer);
            }
        }

        ServerPlayer serverPlayer2 = this.playersByUUID.get(profile.getId());
        if (serverPlayer2 != null && !list.contains(serverPlayer2)) {
            list.add(serverPlayer2);
        }

        for(ServerPlayer serverPlayer3 : list) {
            serverPlayer3.connection.disconnect(new TranslatableComponent("multiplayer.disconnect.duplicate_login"));
        }

        return new ServerPlayer(this.server, this.server.overworld(), profile);
    }

    public ServerPlayer respawn(ServerPlayer player, boolean alive) {
        this.players.remove(player);
        player.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        BlockPos blockPos = player.getRespawnPosition();
        float f = player.getRespawnAngle();
        boolean bl = player.isRespawnForced();
        ServerLevel serverLevel = this.server.getLevel(player.getRespawnDimension());
        Optional<Vec3> optional;
        if (serverLevel != null && blockPos != null) {
            optional = Player.findRespawnPositionAndUseSpawnBlock(serverLevel, blockPos, f, bl, alive);
        } else {
            optional = Optional.empty();
        }

        ServerLevel serverLevel2 = serverLevel != null && optional.isPresent() ? serverLevel : this.server.overworld();
        ServerPlayer serverPlayer = new ServerPlayer(this.server, serverLevel2, player.getGameProfile());
        serverPlayer.connection = player.connection;
        serverPlayer.restoreFrom(player, alive);
        serverPlayer.setId(player.getId());
        serverPlayer.setMainArm(player.getMainArm());

        for(String string : player.getTags()) {
            serverPlayer.addTag(string);
        }

        boolean bl2 = false;
        if (optional.isPresent()) {
            BlockState blockState = serverLevel2.getBlockState(blockPos);
            boolean bl3 = blockState.is(Blocks.RESPAWN_ANCHOR);
            Vec3 vec3 = optional.get();
            float h;
            if (!blockState.is(BlockTags.BEDS) && !bl3) {
                h = f;
            } else {
                Vec3 vec32 = Vec3.atBottomCenterOf(blockPos).subtract(vec3).normalize();
                h = (float)Mth.wrapDegrees(Mth.atan2(vec32.z, vec32.x) * (double)(180F / (float)Math.PI) - 90.0D);
            }

            serverPlayer.moveTo(vec3.x, vec3.y, vec3.z, h, 0.0F);
            serverPlayer.setRespawnPosition(serverLevel2.dimension(), blockPos, f, bl, false);
            bl2 = !alive && bl3;
        } else if (blockPos != null) {
            serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }

        while(!serverLevel2.noCollision(serverPlayer) && serverPlayer.getY() < (double)serverLevel2.getMaxBuildHeight()) {
            serverPlayer.setPos(serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ());
        }

        LevelData levelData = serverPlayer.level.getLevelData();
        serverPlayer.connection.send(new ClientboundRespawnPacket(serverPlayer.level.dimensionTypeRegistration(), serverPlayer.level.dimension(), BiomeManager.obfuscateSeed(serverPlayer.getLevel().getSeed()), serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer.gameMode.getPreviousGameModeForPlayer(), serverPlayer.getLevel().isDebug(), serverPlayer.getLevel().isFlat(), alive));
        serverPlayer.connection.teleport(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot());
        serverPlayer.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverLevel2.getSharedSpawnPos(), serverLevel2.getSharedSpawnAngle()));
        serverPlayer.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        serverPlayer.connection.send(new ClientboundSetExperiencePacket(serverPlayer.experienceProgress, serverPlayer.totalExperience, serverPlayer.experienceLevel));
        this.sendLevelInfo(serverPlayer, serverLevel2);
        this.sendPlayerPermissionLevel(serverPlayer);
        serverLevel2.addRespawnedPlayer(serverPlayer);
        this.players.add(serverPlayer);
        this.playersByUUID.put(serverPlayer.getUUID(), serverPlayer);
        serverPlayer.initInventoryMenu();
        serverPlayer.setHealth(serverPlayer.getHealth());
        if (bl2) {
            serverPlayer.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), 1.0F, 1.0F));
        }

        return serverPlayer;
    }

    public void sendPlayerPermissionLevel(ServerPlayer player) {
        GameProfile gameProfile = player.getGameProfile();
        int i = this.server.getProfilePermissions(gameProfile);
        this.sendPlayerPermissionLevel(player, i);
    }

    public void tick() {
        if (++this.sendAllPlayerInfoIn > 600) {
            this.broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_LATENCY, this.players));
            this.sendAllPlayerInfoIn = 0;
        }

    }

    public void broadcastAll(Packet<?> packet) {
        for(ServerPlayer serverPlayer : this.players) {
            serverPlayer.connection.send(packet);
        }

    }

    public void broadcastAll(Packet<?> packet, ResourceKey<Level> dimension) {
        for(ServerPlayer serverPlayer : this.players) {
            if (serverPlayer.level.dimension() == dimension) {
                serverPlayer.connection.send(packet);
            }
        }

    }

    public void broadcastToTeam(Player source, Component message) {
        Team team = source.getTeam();
        if (team != null) {
            for(String string : team.getPlayers()) {
                ServerPlayer serverPlayer = this.getPlayerByName(string);
                if (serverPlayer != null && serverPlayer != source) {
                    serverPlayer.sendMessage(message, source.getUUID());
                }
            }

        }
    }

    public void broadcastToAllExceptTeam(Player source, Component message) {
        Team team = source.getTeam();
        if (team == null) {
            this.broadcastMessage(message, ChatType.SYSTEM, source.getUUID());
        } else {
            for(int i = 0; i < this.players.size(); ++i) {
                ServerPlayer serverPlayer = this.players.get(i);
                if (serverPlayer.getTeam() != team) {
                    serverPlayer.sendMessage(message, source.getUUID());
                }
            }

        }
    }

    public String[] getPlayerNamesArray() {
        String[] strings = new String[this.players.size()];

        for(int i = 0; i < this.players.size(); ++i) {
            strings[i] = this.players.get(i).getGameProfile().getName();
        }

        return strings;
    }

    public UserBanList getBans() {
        return this.bans;
    }

    public IpBanList getIpBans() {
        return this.ipBans;
    }

    public void op(GameProfile profile) {
        this.ops.add(new ServerOpListEntry(profile, this.server.getOperatorUserPermissionLevel(), this.ops.canBypassPlayerLimit(profile)));
        ServerPlayer serverPlayer = this.getPlayer(profile.getId());
        if (serverPlayer != null) {
            this.sendPlayerPermissionLevel(serverPlayer);
        }

    }

    public void deop(GameProfile profile) {
        this.ops.remove(profile);
        ServerPlayer serverPlayer = this.getPlayer(profile.getId());
        if (serverPlayer != null) {
            this.sendPlayerPermissionLevel(serverPlayer);
        }

    }

    private void sendPlayerPermissionLevel(ServerPlayer player, int permissionLevel) {
        if (player.connection != null) {
            byte b;
            if (permissionLevel <= 0) {
                b = 24;
            } else if (permissionLevel >= 4) {
                b = 28;
            } else {
                b = (byte)(24 + permissionLevel);
            }

            player.connection.send(new ClientboundEntityEventPacket(player, b));
        }

        this.server.getCommands().sendCommands(player);
    }

    public boolean isWhiteListed(GameProfile profile) {
        return !this.doWhiteList || this.ops.contains(profile) || this.whitelist.contains(profile);
    }

    public boolean isOp(GameProfile profile) {
        return this.ops.contains(profile) || this.server.isSingleplayerOwner(profile) && this.server.getWorldData().getAllowCommands() || this.allowCheatsForAllPlayers;
    }

    @Nullable
    public ServerPlayer getPlayerByName(String name) {
        for(ServerPlayer serverPlayer : this.players) {
            if (serverPlayer.getGameProfile().getName().equalsIgnoreCase(name)) {
                return serverPlayer;
            }
        }

        return null;
    }

    public void broadcast(@Nullable Player player, double x, double y, double z, double distance, ResourceKey<Level> worldKey, Packet<?> packet) {
        for(int i = 0; i < this.players.size(); ++i) {
            ServerPlayer serverPlayer = this.players.get(i);
            if (serverPlayer != player && serverPlayer.level.dimension() == worldKey) {
                double d = x - serverPlayer.getX();
                double e = y - serverPlayer.getY();
                double f = z - serverPlayer.getZ();
                if (d * d + e * e + f * f < distance * distance) {
                    serverPlayer.connection.send(packet);
                }
            }
        }

    }

    public void saveAll() {
        for(int i = 0; i < this.players.size(); ++i) {
            this.save(this.players.get(i));
        }

    }

    public UserWhiteList getWhiteList() {
        return this.whitelist;
    }

    public String[] getWhiteListNames() {
        return this.whitelist.getUserList();
    }

    public ServerOpList getOps() {
        return this.ops;
    }

    public String[] getOpNames() {
        return this.ops.getUserList();
    }

    public void reloadWhiteList() {
    }

    public void sendLevelInfo(ServerPlayer player, ServerLevel world) {
        WorldBorder worldBorder = this.server.overworld().getWorldBorder();
        player.connection.send(new ClientboundInitializeBorderPacket(worldBorder));
        player.connection.send(new ClientboundSetTimePacket(world.getGameTime(), world.getDayTime(), world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(world.getSharedSpawnPos(), world.getSharedSpawnAngle()));
        if (world.isRaining()) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, world.getRainLevel(1.0F)));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, world.getThunderLevel(1.0F)));
        }

    }

    public void sendAllPlayerInfo(ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        player.resetSentInfo();
        player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public boolean isUsingWhitelist() {
        return this.doWhiteList;
    }

    public void setUsingWhiteList(boolean whitelistEnabled) {
        this.doWhiteList = whitelistEnabled;
    }

    public List<ServerPlayer> getPlayersWithAddress(String ip) {
        List<ServerPlayer> list = Lists.newArrayList();

        for(ServerPlayer serverPlayer : this.players) {
            if (serverPlayer.getIpAddress().equals(ip)) {
                list.add(serverPlayer);
            }
        }

        return list;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public int getSimulationDistance() {
        return this.simulationDistance;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    @Nullable
    public CompoundTag getSingleplayerData() {
        return null;
    }

    public void setAllowCheatsForAllPlayers(boolean cheatsAllowed) {
        this.allowCheatsForAllPlayers = cheatsAllowed;
    }

    public void removeAll() {
        for(int i = 0; i < this.players.size(); ++i) {
            (this.players.get(i)).connection.disconnect(new TranslatableComponent("multiplayer.disconnect.server_shutdown"));
        }

    }

    public void broadcastMessage(Component message, ChatType type, UUID sender) {
        this.server.sendMessage(message, sender);

        for(ServerPlayer serverPlayer : this.players) {
            serverPlayer.sendMessage(message, type, sender);
        }

    }

    public void broadcastMessage(Component serverMessage, Function<ServerPlayer, Component> playerMessageFactory, ChatType type, UUID sender) {
        this.server.sendMessage(serverMessage, sender);

        for(ServerPlayer serverPlayer : this.players) {
            Component component = playerMessageFactory.apply(serverPlayer);
            if (component != null) {
                serverPlayer.sendMessage(component, type, sender);
            }
        }

    }

    public ServerStatsCounter getPlayerStats(Player player) {
        UUID uUID = player.getUUID();
        ServerStatsCounter serverStatsCounter = this.stats.get(uUID);
        if (serverStatsCounter == null) {
            File file = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
            File file2 = new File(file, uUID + ".json");
            if (!file2.exists()) {
                File file3 = new File(file, player.getName().getString() + ".json");
                Path path = file3.toPath();
                if (FileUtil.isPathNormalized(path) && FileUtil.isPathPortable(path) && path.startsWith(file.getPath()) && file3.isFile()) {
                    file3.renameTo(file2);
                }
            }

            serverStatsCounter = new ServerStatsCounter(this.server, file2);
            this.stats.put(uUID, serverStatsCounter);
        }

        return serverStatsCounter;
    }

    public PlayerAdvancements getPlayerAdvancements(ServerPlayer player) {
        UUID uUID = player.getUUID();
        PlayerAdvancements playerAdvancements = this.advancements.get(uUID);
        if (playerAdvancements == null) {
            File file = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).toFile();
            File file2 = new File(file, uUID + ".json");
            playerAdvancements = new PlayerAdvancements(this.server.getFixerUpper(), this, this.server.getAdvancements(), file2, player);
            this.advancements.put(uUID, playerAdvancements);
        }

        playerAdvancements.setPlayer(player);
        return playerAdvancements;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
        this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(viewDistance));

        for(ServerLevel serverLevel : this.server.getAllLevels()) {
            if (serverLevel != null) {
                serverLevel.getChunkSource().setViewDistance(viewDistance);
            }
        }

    }

    public void setSimulationDistance(int simulationDistance) {
        this.simulationDistance = simulationDistance;
        this.broadcastAll(new ClientboundSetSimulationDistancePacket(simulationDistance));

        for(ServerLevel serverLevel : this.server.getAllLevels()) {
            if (serverLevel != null) {
                serverLevel.getChunkSource().setSimulationDistance(simulationDistance);
            }
        }

    }

    public List<ServerPlayer> getPlayers() {
        return this.players;
    }

    @Nullable
    public ServerPlayer getPlayer(UUID uuid) {
        return this.playersByUUID.get(uuid);
    }

    public boolean canBypassPlayerLimit(GameProfile profile) {
        return false;
    }

    public void reloadResources() {
        for(PlayerAdvancements playerAdvancements : this.advancements.values()) {
            playerAdvancements.reload(this.server.getAdvancements());
        }

        this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registryHolder)));
        ClientboundUpdateRecipesPacket clientboundUpdateRecipesPacket = new ClientboundUpdateRecipesPacket(this.server.getRecipeManager().getRecipes());

        for(ServerPlayer serverPlayer : this.players) {
            serverPlayer.connection.send(clientboundUpdateRecipesPacket);
            serverPlayer.getRecipeBook().sendInitialRecipeBook(serverPlayer);
        }

    }

    public boolean isAllowCheatsForAllPlayers() {
        return this.allowCheatsForAllPlayers;
    }
}
