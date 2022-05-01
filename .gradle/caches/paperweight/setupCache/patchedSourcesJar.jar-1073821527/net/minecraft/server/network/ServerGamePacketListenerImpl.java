package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class ServerGamePacketListenerImpl implements ServerPlayerConnection, ServerGamePacketListener {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int LATENCY_CHECK_INTERVAL = 15000;
    public final Connection connection;
    private final MinecraftServer server;
    public ServerPlayer player;
    private int tickCount;
    private long keepAliveTime;
    private boolean keepAlivePending;
    private long keepAliveChallenge;
    private int chatSpamTickCount;
    private int dropSpamTickCount;
    private double firstGoodX;
    private double firstGoodY;
    private double firstGoodZ;
    private double lastGoodX;
    private double lastGoodY;
    private double lastGoodZ;
    @Nullable
    private Entity lastVehicle;
    private double vehicleFirstGoodX;
    private double vehicleFirstGoodY;
    private double vehicleFirstGoodZ;
    private double vehicleLastGoodX;
    private double vehicleLastGoodY;
    private double vehicleLastGoodZ;
    @Nullable
    private Vec3 awaitingPositionFromClient;
    private int awaitingTeleport;
    private int awaitingTeleportTime;
    private boolean clientIsFloating;
    private int aboveGroundTickCount;
    private boolean clientVehicleIsFloating;
    private int aboveGroundVehicleTickCount;
    private int receivedMovePacketCount;
    private int knownMovePacketCount;

    public ServerGamePacketListenerImpl(MinecraftServer server, Connection connection, ServerPlayer player) {
        this.server = server;
        this.connection = connection;
        connection.setListener(this);
        this.player = player;
        player.connection = this;
        this.keepAliveTime = Util.getMillis();
        player.getTextFilter().join();
    }

    public void tick() {
        this.resetPosition();
        this.player.xo = this.player.getX();
        this.player.yo = this.player.getY();
        this.player.zo = this.player.getZ();
        this.player.doTick();
        this.player.absMoveTo(this.firstGoodX, this.firstGoodY, this.firstGoodZ, this.player.getYRot(), this.player.getXRot());
        ++this.tickCount;
        this.knownMovePacketCount = this.receivedMovePacketCount;
        if (this.clientIsFloating && !this.player.isSleeping() && !this.player.isPassenger()) {
            if (++this.aboveGroundTickCount > 80) {
                LOGGER.warn("{} was kicked for floating too long!", (Object)this.player.getName().getString());
                this.disconnect(new TranslatableComponent("multiplayer.disconnect.flying"));
                return;
            }
        } else {
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
        }

        this.lastVehicle = this.player.getRootVehicle();
        if (this.lastVehicle != this.player && this.lastVehicle.getControllingPassenger() == this.player) {
            this.vehicleFirstGoodX = this.lastVehicle.getX();
            this.vehicleFirstGoodY = this.lastVehicle.getY();
            this.vehicleFirstGoodZ = this.lastVehicle.getZ();
            this.vehicleLastGoodX = this.lastVehicle.getX();
            this.vehicleLastGoodY = this.lastVehicle.getY();
            this.vehicleLastGoodZ = this.lastVehicle.getZ();
            if (this.clientVehicleIsFloating && this.player.getRootVehicle().getControllingPassenger() == this.player) {
                if (++this.aboveGroundVehicleTickCount > 80) {
                    LOGGER.warn("{} was kicked for floating a vehicle too long!", (Object)this.player.getName().getString());
                    this.disconnect(new TranslatableComponent("multiplayer.disconnect.flying"));
                    return;
                }
            } else {
                this.clientVehicleIsFloating = false;
                this.aboveGroundVehicleTickCount = 0;
            }
        } else {
            this.lastVehicle = null;
            this.clientVehicleIsFloating = false;
            this.aboveGroundVehicleTickCount = 0;
        }

        this.server.getProfiler().push("keepAlive");
        long l = Util.getMillis();
        if (l - this.keepAliveTime >= 15000L) {
            if (this.keepAlivePending) {
                this.disconnect(new TranslatableComponent("disconnect.timeout"));
            } else {
                this.keepAlivePending = true;
                this.keepAliveTime = l;
                this.keepAliveChallenge = l;
                this.send(new ClientboundKeepAlivePacket(this.keepAliveChallenge));
            }
        }

        this.server.getProfiler().pop();
        if (this.chatSpamTickCount > 0) {
            --this.chatSpamTickCount;
        }

        if (this.dropSpamTickCount > 0) {
            --this.dropSpamTickCount;
        }

        if (this.player.getLastActionTime() > 0L && this.server.getPlayerIdleTimeout() > 0 && Util.getMillis() - this.player.getLastActionTime() > (long)(this.server.getPlayerIdleTimeout() * 1000 * 60)) {
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.idling"));
        }

    }

    public void resetPosition() {
        this.firstGoodX = this.player.getX();
        this.firstGoodY = this.player.getY();
        this.firstGoodZ = this.player.getZ();
        this.lastGoodX = this.player.getX();
        this.lastGoodY = this.player.getY();
        this.lastGoodZ = this.player.getZ();
    }

    @Override
    public Connection getConnection() {
        return this.connection;
    }

    private boolean isSingleplayerOwner() {
        return this.server.isSingleplayerOwner(this.player.getGameProfile());
    }

    public void disconnect(Component reason) {
        this.connection.send(new ClientboundDisconnectPacket(reason), (future) -> {
            this.connection.disconnect(reason);
        });
        this.connection.setReadOnly();
        this.server.executeBlocking(this.connection::handleDisconnection);
    }

    private <T, R> void filterTextPacket(T text, Consumer<R> consumer, BiFunction<TextFilter, T, CompletableFuture<R>> backingFilterer) {
        BlockableEventLoop<?> blockableEventLoop = this.player.getLevel().getServer();
        Consumer<R> consumer2 = (object2) -> {
            if (this.getConnection().isConnected()) {
                try {
                    consumer.accept(object2);
                } catch (Exception var5) {
                    LOGGER.error("Failed to handle chat packet {}, suppressing error", text, var5);
                }
            } else {
                LOGGER.debug("Ignoring packet due to disconnection");
            }

        };
        backingFilterer.apply(this.player.getTextFilter(), text).thenAcceptAsync(consumer2, blockableEventLoop);
    }

    private void filterTextPacket(String text, Consumer<TextFilter.FilteredText> consumer) {
        this.filterTextPacket(text, consumer, TextFilter::processStreamMessage);
    }

    private void filterTextPacket(List<String> texts, Consumer<List<TextFilter.FilteredText>> consumer) {
        this.filterTextPacket(texts, consumer, TextFilter::processMessageBundle);
    }

    @Override
    public void handlePlayerInput(ServerboundPlayerInputPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.setPlayerInput(packet.getXxa(), packet.getZza(), packet.isJumping(), packet.isShiftKeyDown());
    }

    private static boolean containsInvalidValues(double x, double y, double z, float yaw, float pitch) {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || !Floats.isFinite(pitch) || !Floats.isFinite(yaw);
    }

    private static double clampHorizontal(double d) {
        return Mth.clamp(d, -3.0E7D, 3.0E7D);
    }

    private static double clampVertical(double d) {
        return Mth.clamp(d, -2.0E7D, 2.0E7D);
    }

    @Override
    public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (containsInvalidValues(packet.getX(), packet.getY(), packet.getZ(), packet.getYRot(), packet.getXRot())) {
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_vehicle_movement"));
        } else {
            Entity entity = this.player.getRootVehicle();
            if (entity != this.player && entity.getControllingPassenger() == this.player && entity == this.lastVehicle) {
                ServerLevel serverLevel = this.player.getLevel();
                double d = entity.getX();
                double e = entity.getY();
                double f = entity.getZ();
                double g = clampHorizontal(packet.getX());
                double h = clampVertical(packet.getY());
                double i = clampHorizontal(packet.getZ());
                float j = Mth.wrapDegrees(packet.getYRot());
                float k = Mth.wrapDegrees(packet.getXRot());
                double l = g - this.vehicleFirstGoodX;
                double m = h - this.vehicleFirstGoodY;
                double n = i - this.vehicleFirstGoodZ;
                double o = entity.getDeltaMovement().lengthSqr();
                double p = l * l + m * m + n * n;
                if (p - o > 100.0D && !this.isSingleplayerOwner()) {
                    LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", entity.getName().getString(), this.player.getName().getString(), l, m, n);
                    this.connection.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }

                boolean bl = serverLevel.noCollision(entity, entity.getBoundingBox().deflate(0.0625D));
                l = g - this.vehicleLastGoodX;
                m = h - this.vehicleLastGoodY - 1.0E-6D;
                n = i - this.vehicleLastGoodZ;
                boolean bl2 = entity.verticalCollisionBelow;
                entity.move(MoverType.PLAYER, new Vec3(l, m, n));
                l = g - entity.getX();
                m = h - entity.getY();
                if (m > -0.5D || m < 0.5D) {
                    m = 0.0D;
                }

                n = i - entity.getZ();
                p = l * l + m * m + n * n;
                boolean bl3 = false;
                if (p > 0.0625D) {
                    bl3 = true;
                    LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", entity.getName().getString(), this.player.getName().getString(), Math.sqrt(p));
                }

                entity.absMoveTo(g, h, i, j, k);
                boolean bl4 = serverLevel.noCollision(entity, entity.getBoundingBox().deflate(0.0625D));
                if (bl && (bl3 || !bl4)) {
                    entity.absMoveTo(d, e, f, j, k);
                    this.connection.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }

                this.player.getLevel().getChunkSource().move(this.player);
                this.player.checkMovementStatistics(this.player.getX() - d, this.player.getY() - e, this.player.getZ() - f);
                this.clientVehicleIsFloating = m >= -0.03125D && !bl2 && !this.server.isFlightAllowed() && !entity.isNoGravity() && this.noBlocksAround(entity);
                this.vehicleLastGoodX = entity.getX();
                this.vehicleLastGoodY = entity.getY();
                this.vehicleLastGoodZ = entity.getZ();
            }

        }
    }

    private boolean noBlocksAround(Entity entity) {
        return entity.level.getBlockStates(entity.getBoundingBox().inflate(0.0625D).expandTowards(0.0D, -0.55D, 0.0D)).allMatch(BlockBehaviour.BlockStateBase::isAir);
    }

    @Override
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (packet.getId() == this.awaitingTeleport) {
            this.player.absMoveTo(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
            this.lastGoodX = this.awaitingPositionFromClient.x;
            this.lastGoodY = this.awaitingPositionFromClient.y;
            this.lastGoodZ = this.awaitingPositionFromClient.z;
            if (this.player.isChangingDimension()) {
                this.player.hasChangedDimension();
            }

            this.awaitingPositionFromClient = null;
        }

    }

    @Override
    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.server.getRecipeManager().byKey(packet.getRecipe()).ifPresent(this.player.getRecipeBook()::removeHighlight);
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.getRecipeBook().setBookSetting(packet.getBookType(), packet.isOpen(), packet.isFiltering());
    }

    @Override
    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (packet.getAction() == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
            ResourceLocation resourceLocation = packet.getTab();
            Advancement advancement = this.server.getAdvancements().getAdvancement(resourceLocation);
            if (advancement != null) {
                this.player.getAdvancements().setSelectedTab(advancement);
            }
        }

    }

    @Override
    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        StringReader stringReader = new StringReader(packet.getCommand());
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        ParseResults<CommandSourceStack> parseResults = this.server.getCommands().getDispatcher().parse(stringReader, this.player.createCommandSourceStack());
        this.server.getCommands().getDispatcher().getCompletionSuggestions(parseResults).thenAccept((suggestions) -> {
            this.connection.send(new ClientboundCommandSuggestionsPacket(packet.getId(), suggestions));
        });
    }

    @Override
    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (!this.server.isCommandBlockEnabled()) {
            this.player.sendMessage(new TranslatableComponent("advMode.notEnabled"), Util.NIL_UUID);
        } else if (!this.player.canUseGameMasterBlocks()) {
            this.player.sendMessage(new TranslatableComponent("advMode.notAllowed"), Util.NIL_UUID);
        } else {
            BaseCommandBlock baseCommandBlock = null;
            CommandBlockEntity commandBlockEntity = null;
            BlockPos blockPos = packet.getPos();
            BlockEntity blockEntity = this.player.level.getBlockEntity(blockPos);
            if (blockEntity instanceof CommandBlockEntity) {
                commandBlockEntity = (CommandBlockEntity)blockEntity;
                baseCommandBlock = commandBlockEntity.getCommandBlock();
            }

            String string = packet.getCommand();
            boolean bl = packet.isTrackOutput();
            if (baseCommandBlock != null) {
                CommandBlockEntity.Mode mode = commandBlockEntity.getMode();
                BlockState blockState = this.player.level.getBlockState(blockPos);
                Direction direction = blockState.getValue(CommandBlock.FACING);
                BlockState blockState2;
                switch(packet.getMode()) {
                case SEQUENCE:
                    blockState2 = Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState();
                    break;
                case AUTO:
                    blockState2 = Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState();
                    break;
                case REDSTONE:
                default:
                    blockState2 = Blocks.COMMAND_BLOCK.defaultBlockState();
                }

                BlockState blockState5 = blockState2.setValue(CommandBlock.FACING, direction).setValue(CommandBlock.CONDITIONAL, Boolean.valueOf(packet.isConditional()));
                if (blockState5 != blockState) {
                    this.player.level.setBlock(blockPos, blockState5, 2);
                    blockEntity.setBlockState(blockState5);
                    this.player.level.getChunkAt(blockPos).setBlockEntity(blockEntity);
                }

                baseCommandBlock.setCommand(string);
                baseCommandBlock.setTrackOutput(bl);
                if (!bl) {
                    baseCommandBlock.setLastOutput((Component)null);
                }

                commandBlockEntity.setAutomatic(packet.isAutomatic());
                if (mode != packet.getMode()) {
                    commandBlockEntity.onModeSwitch();
                }

                baseCommandBlock.onUpdated();
                if (!StringUtil.isNullOrEmpty(string)) {
                    this.player.sendMessage(new TranslatableComponent("advMode.setCommand.success", string), Util.NIL_UUID);
                }
            }

        }
    }

    @Override
    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (!this.server.isCommandBlockEnabled()) {
            this.player.sendMessage(new TranslatableComponent("advMode.notEnabled"), Util.NIL_UUID);
        } else if (!this.player.canUseGameMasterBlocks()) {
            this.player.sendMessage(new TranslatableComponent("advMode.notAllowed"), Util.NIL_UUID);
        } else {
            BaseCommandBlock baseCommandBlock = packet.getCommandBlock(this.player.level);
            if (baseCommandBlock != null) {
                baseCommandBlock.setCommand(packet.getCommand());
                baseCommandBlock.setTrackOutput(packet.isTrackOutput());
                if (!packet.isTrackOutput()) {
                    baseCommandBlock.setLastOutput((Component)null);
                }

                baseCommandBlock.onUpdated();
                this.player.sendMessage(new TranslatableComponent("advMode.setCommand.success", packet.getCommand()), Util.NIL_UUID);
            }

        }
    }

    @Override
    public void handlePickItem(ServerboundPickItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.getInventory().pickSlot(packet.getSlot());
        this.player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, this.player.getInventory().selected, this.player.getInventory().getItem(this.player.getInventory().selected)));
        this.player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, packet.getSlot(), this.player.getInventory().getItem(packet.getSlot())));
        this.player.connection.send(new ClientboundSetCarriedItemPacket(this.player.getInventory().selected));
    }

    @Override
    public void handleRenameItem(ServerboundRenameItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.containerMenu instanceof AnvilMenu) {
            AnvilMenu anvilMenu = (AnvilMenu)this.player.containerMenu;
            String string = SharedConstants.filterText(packet.getName());
            if (string.length() <= 50) {
                anvilMenu.setItemName(string);
            }
        }

    }

    @Override
    public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.containerMenu instanceof BeaconMenu) {
            ((BeaconMenu)this.player.containerMenu).updateEffects(packet.getPrimary(), packet.getSecondary());
        }

    }

    @Override
    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockPos = packet.getPos();
            BlockState blockState = this.player.level.getBlockState(blockPos);
            BlockEntity blockEntity = this.player.level.getBlockEntity(blockPos);
            if (blockEntity instanceof StructureBlockEntity) {
                StructureBlockEntity structureBlockEntity = (StructureBlockEntity)blockEntity;
                structureBlockEntity.setMode(packet.getMode());
                structureBlockEntity.setStructureName(packet.getName());
                structureBlockEntity.setStructurePos(packet.getOffset());
                structureBlockEntity.setStructureSize(packet.getSize());
                structureBlockEntity.setMirror(packet.getMirror());
                structureBlockEntity.setRotation(packet.getRotation());
                structureBlockEntity.setMetaData(packet.getData());
                structureBlockEntity.setIgnoreEntities(packet.isIgnoreEntities());
                structureBlockEntity.setShowAir(packet.isShowAir());
                structureBlockEntity.setShowBoundingBox(packet.isShowBoundingBox());
                structureBlockEntity.setIntegrity(packet.getIntegrity());
                structureBlockEntity.setSeed(packet.getSeed());
                if (structureBlockEntity.hasStructureName()) {
                    String string = structureBlockEntity.getStructureName();
                    if (packet.getUpdateType() == StructureBlockEntity.UpdateType.SAVE_AREA) {
                        if (structureBlockEntity.saveStructure()) {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.save_success", string), false);
                        } else {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.save_failure", string), false);
                        }
                    } else if (packet.getUpdateType() == StructureBlockEntity.UpdateType.LOAD_AREA) {
                        if (!structureBlockEntity.isStructureLoadable()) {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.load_not_found", string), false);
                        } else if (structureBlockEntity.loadStructure(this.player.getLevel())) {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.load_success", string), false);
                        } else {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.load_prepare", string), false);
                        }
                    } else if (packet.getUpdateType() == StructureBlockEntity.UpdateType.SCAN_AREA) {
                        if (structureBlockEntity.detectSize()) {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.size_success", string), false);
                        } else {
                            this.player.displayClientMessage(new TranslatableComponent("structure_block.size_failure"), false);
                        }
                    }
                } else {
                    this.player.displayClientMessage(new TranslatableComponent("structure_block.invalid_structure_name", packet.getName()), false);
                }

                structureBlockEntity.setChanged();
                this.player.level.sendBlockUpdated(blockPos, blockState, blockState, 3);
            }

        }
    }

    @Override
    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockPos = packet.getPos();
            BlockState blockState = this.player.level.getBlockState(blockPos);
            BlockEntity blockEntity = this.player.level.getBlockEntity(blockPos);
            if (blockEntity instanceof JigsawBlockEntity) {
                JigsawBlockEntity jigsawBlockEntity = (JigsawBlockEntity)blockEntity;
                jigsawBlockEntity.setName(packet.getName());
                jigsawBlockEntity.setTarget(packet.getTarget());
                jigsawBlockEntity.setPool(packet.getPool());
                jigsawBlockEntity.setFinalState(packet.getFinalState());
                jigsawBlockEntity.setJoint(packet.getJoint());
                jigsawBlockEntity.setChanged();
                this.player.level.sendBlockUpdated(blockPos, blockState, blockState, 3);
            }

        }
    }

    @Override
    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockPos = packet.getPos();
            BlockEntity blockEntity = this.player.level.getBlockEntity(blockPos);
            if (blockEntity instanceof JigsawBlockEntity) {
                JigsawBlockEntity jigsawBlockEntity = (JigsawBlockEntity)blockEntity;
                jigsawBlockEntity.generate(this.player.getLevel(), packet.levels(), packet.keepJigsaws());
            }

        }
    }

    @Override
    public void handleSelectTrade(ServerboundSelectTradePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        int i = packet.getItem();
        AbstractContainerMenu abstractContainerMenu = this.player.containerMenu;
        if (abstractContainerMenu instanceof MerchantMenu) {
            MerchantMenu merchantMenu = (MerchantMenu)abstractContainerMenu;
            merchantMenu.setSelectionHint(i);
            merchantMenu.tryMoveItems(i);
        }

    }

    @Override
    public void handleEditBook(ServerboundEditBookPacket packet) {
        int i = packet.getSlot();
        if (Inventory.isHotbarSlot(i) || i == 40) {
            List<String> list = Lists.newArrayList();
            Optional<String> optional = packet.getTitle();
            optional.ifPresent(list::add);
            packet.getPages().stream().limit(100L).forEach(list::add);
            this.filterTextPacket(list, optional.isPresent() ? (listx) -> {
                this.signBook(listx.get(0), listx.subList(1, listx.size()), i);
            } : (listx) -> {
                this.updateBookContents(listx, i);
            });
        }
    }

    private void updateBookContents(List<TextFilter.FilteredText> pages, int slotId) {
        ItemStack itemStack = this.player.getInventory().getItem(slotId);
        if (itemStack.is(Items.WRITABLE_BOOK)) {
            this.updateBookPages(pages, UnaryOperator.identity(), itemStack);
        }
    }

    private void signBook(TextFilter.FilteredText title, List<TextFilter.FilteredText> pages, int slotId) {
        ItemStack itemStack = this.player.getInventory().getItem(slotId);
        if (itemStack.is(Items.WRITABLE_BOOK)) {
            ItemStack itemStack2 = new ItemStack(Items.WRITTEN_BOOK);
            CompoundTag compoundTag = itemStack.getTag();
            if (compoundTag != null) {
                itemStack2.setTag(compoundTag.copy());
            }

            itemStack2.addTagElement("author", StringTag.valueOf(this.player.getName().getString()));
            if (this.player.isTextFilteringEnabled()) {
                itemStack2.addTagElement("title", StringTag.valueOf(title.getFiltered()));
            } else {
                itemStack2.addTagElement("filtered_title", StringTag.valueOf(title.getFiltered()));
                itemStack2.addTagElement("title", StringTag.valueOf(title.getRaw()));
            }

            this.updateBookPages(pages, (string) -> {
                return Component.Serializer.toJson(new TextComponent(string));
            }, itemStack2);
            this.player.getInventory().setItem(slotId, itemStack2);
        }
    }

    private void updateBookPages(List<TextFilter.FilteredText> messages, UnaryOperator<String> postProcessor, ItemStack book) {
        ListTag listTag = new ListTag();
        if (this.player.isTextFilteringEnabled()) {
            messages.stream().map((message) -> {
                return StringTag.valueOf(postProcessor.apply(message.getFiltered()));
            }).forEach(listTag::add);
        } else {
            CompoundTag compoundTag = new CompoundTag();
            int i = 0;

            for(int j = messages.size(); i < j; ++i) {
                TextFilter.FilteredText filteredText = messages.get(i);
                String string = filteredText.getRaw();
                listTag.add(StringTag.valueOf(postProcessor.apply(string)));
                String string2 = filteredText.getFiltered();
                if (!string.equals(string2)) {
                    compoundTag.putString(String.valueOf(i), postProcessor.apply(string2));
                }
            }

            if (!compoundTag.isEmpty()) {
                book.addTagElement("filtered_pages", compoundTag);
            }
        }

        book.addTagElement("pages", listTag);
    }

    @Override
    public void handleEntityTagQuery(ServerboundEntityTagQuery packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.hasPermissions(2)) {
            Entity entity = this.player.getLevel().getEntity(packet.getEntityId());
            if (entity != null) {
                CompoundTag compoundTag = entity.saveWithoutId(new CompoundTag());
                this.player.connection.send(new ClientboundTagQueryPacket(packet.getTransactionId(), compoundTag));
            }

        }
    }

    @Override
    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQuery packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.hasPermissions(2)) {
            BlockEntity blockEntity = this.player.getLevel().getBlockEntity(packet.getPos());
            CompoundTag compoundTag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;
            this.player.connection.send(new ClientboundTagQueryPacket(packet.getTransactionId(), compoundTag));
        }
    }

    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (containsInvalidValues(packet.getX(0.0D), packet.getY(0.0D), packet.getZ(0.0D), packet.getYRot(0.0F), packet.getXRot(0.0F))) {
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_player_movement"));
        } else {
            ServerLevel serverLevel = this.player.getLevel();
            if (!this.player.wonGame) {
                if (this.tickCount == 0) {
                    this.resetPosition();
                }

                if (this.awaitingPositionFromClient != null) {
                    if (this.tickCount - this.awaitingTeleportTime > 20) {
                        this.awaitingTeleportTime = this.tickCount;
                        this.teleport(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
                    }

                } else {
                    this.awaitingTeleportTime = this.tickCount;
                    double d = clampHorizontal(packet.getX(this.player.getX()));
                    double e = clampVertical(packet.getY(this.player.getY()));
                    double f = clampHorizontal(packet.getZ(this.player.getZ()));
                    float g = Mth.wrapDegrees(packet.getYRot(this.player.getYRot()));
                    float h = Mth.wrapDegrees(packet.getXRot(this.player.getXRot()));
                    if (this.player.isPassenger()) {
                        this.player.absMoveTo(this.player.getX(), this.player.getY(), this.player.getZ(), g, h);
                        this.player.getLevel().getChunkSource().move(this.player);
                    } else {
                        double i = this.player.getX();
                        double j = this.player.getY();
                        double k = this.player.getZ();
                        double l = this.player.getY();
                        double m = d - this.firstGoodX;
                        double n = e - this.firstGoodY;
                        double o = f - this.firstGoodZ;
                        double p = this.player.getDeltaMovement().lengthSqr();
                        double q = m * m + n * n + o * o;
                        if (this.player.isSleeping()) {
                            if (q > 1.0D) {
                                this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), g, h);
                            }

                        } else {
                            ++this.receivedMovePacketCount;
                            int r = this.receivedMovePacketCount - this.knownMovePacketCount;
                            if (r > 5) {
                                LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getName().getString(), r);
                                r = 1;
                            }

                            if (!this.player.isChangingDimension() && (!this.player.getLevel().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) || !this.player.isFallFlying())) {
                                float s = this.player.isFallFlying() ? 300.0F : 100.0F;
                                if (q - p > (double)(s * (float)r) && !this.isSingleplayerOwner()) {
                                    LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getName().getString(), m, n, o);
                                    this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
                                    return;
                                }
                            }

                            AABB aABB = this.player.getBoundingBox();
                            m = d - this.lastGoodX;
                            n = e - this.lastGoodY;
                            o = f - this.lastGoodZ;
                            boolean bl = n > 0.0D;
                            if (this.player.isOnGround() && !packet.isOnGround() && bl) {
                                this.player.jumpFromGround();
                            }

                            boolean bl2 = this.player.verticalCollisionBelow;
                            this.player.move(MoverType.PLAYER, new Vec3(m, n, o));
                            m = d - this.player.getX();
                            n = e - this.player.getY();
                            if (n > -0.5D || n < 0.5D) {
                                n = 0.0D;
                            }

                            o = f - this.player.getZ();
                            q = m * m + n * n + o * o;
                            boolean bl3 = false;
                            if (!this.player.isChangingDimension() && q > 0.0625D && !this.player.isSleeping() && !this.player.gameMode.isCreative() && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                                bl3 = true;
                                LOGGER.warn("{} moved wrongly!", (Object)this.player.getName().getString());
                            }

                            this.player.absMoveTo(d, e, f, g, h);
                            if (this.player.noPhysics || this.player.isSleeping() || (!bl3 || !serverLevel.noCollision(this.player, aABB)) && !this.isPlayerCollidingWithAnythingNew(serverLevel, aABB)) {
                                this.clientIsFloating = n >= -0.03125D && !bl2 && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR && !this.server.isFlightAllowed() && !this.player.getAbilities().mayfly && !this.player.hasEffect(MobEffects.LEVITATION) && !this.player.isFallFlying() && !this.player.isAutoSpinAttack() && this.noBlocksAround(this.player);
                                this.player.getLevel().getChunkSource().move(this.player);
                                this.player.doCheckFallDamage(this.player.getY() - l, packet.isOnGround());
                                this.player.setOnGround(packet.isOnGround());
                                if (bl) {
                                    this.player.resetFallDistance();
                                }

                                this.player.checkMovementStatistics(this.player.getX() - i, this.player.getY() - j, this.player.getZ() - k);
                                this.lastGoodX = this.player.getX();
                                this.lastGoodY = this.player.getY();
                                this.lastGoodZ = this.player.getZ();
                            } else {
                                this.teleport(i, j, k, g, h);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isPlayerCollidingWithAnythingNew(LevelReader world, AABB box) {
        Iterable<VoxelShape> iterable = world.getCollisions(this.player, this.player.getBoundingBox().deflate((double)1.0E-5F));
        VoxelShape voxelShape = Shapes.create(box.deflate((double)1.0E-5F));

        for(VoxelShape voxelShape2 : iterable) {
            if (!Shapes.joinIsNotEmpty(voxelShape2, voxelShape, BooleanOp.AND)) {
                return true;
            }
        }

        return false;
    }

    public void dismount(double x, double y, double z, float yaw, float pitch) {
        this.teleport(x, y, z, yaw, pitch, Collections.emptySet(), true);
    }

    public void teleport(double x, double y, double z, float yaw, float pitch) {
        this.teleport(x, y, z, yaw, pitch, Collections.emptySet(), false);
    }

    public void teleport(double x, double y, double z, float yaw, float pitch, Set<ClientboundPlayerPositionPacket.RelativeArgument> flags) {
        this.teleport(x, y, z, yaw, pitch, flags, false);
    }

    public void teleport(double x, double y, double z, float yaw, float pitch, Set<ClientboundPlayerPositionPacket.RelativeArgument> flags, boolean shouldDismount) {
        double d = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.X) ? this.player.getX() : 0.0D;
        double e = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.Y) ? this.player.getY() : 0.0D;
        double f = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.Z) ? this.player.getZ() : 0.0D;
        float g = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT) ? this.player.getYRot() : 0.0F;
        float h = flags.contains(ClientboundPlayerPositionPacket.RelativeArgument.X_ROT) ? this.player.getXRot() : 0.0F;
        this.awaitingPositionFromClient = new Vec3(x, y, z);
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }

        this.awaitingTeleportTime = this.tickCount;
        this.player.absMoveTo(x, y, z, yaw, pitch);
        this.player.connection.send(new ClientboundPlayerPositionPacket(x - d, y - e, z - f, yaw - g, pitch - h, flags, this.awaitingTeleport, shouldDismount));
    }

    @Override
    public void handlePlayerAction(ServerboundPlayerActionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        BlockPos blockPos = packet.getPos();
        this.player.resetLastActionTime();
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        switch(action) {
        case SWAP_ITEM_WITH_OFFHAND:
            if (!this.player.isSpectator()) {
                ItemStack itemStack = this.player.getItemInHand(InteractionHand.OFF_HAND);
                this.player.setItemInHand(InteractionHand.OFF_HAND, this.player.getItemInHand(InteractionHand.MAIN_HAND));
                this.player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
                this.player.stopUsingItem();
            }

            return;
        case DROP_ITEM:
            if (!this.player.isSpectator()) {
                this.player.drop(false);
            }

            return;
        case DROP_ALL_ITEMS:
            if (!this.player.isSpectator()) {
                this.player.drop(true);
            }

            return;
        case RELEASE_USE_ITEM:
            this.player.releaseUsingItem();
            return;
        case START_DESTROY_BLOCK:
        case ABORT_DESTROY_BLOCK:
        case STOP_DESTROY_BLOCK:
            this.player.gameMode.handleBlockBreakAction(blockPos, action, packet.getDirection(), this.player.level.getMaxBuildHeight());
            return;
        default:
            throw new IllegalArgumentException("Invalid player action");
        }
    }

    private static boolean wasBlockPlacementAttempt(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            Item item = stack.getItem();
            return (item instanceof BlockItem || item instanceof BucketItem) && !player.getCooldowns().isOnCooldown(item);
        }
    }

    @Override
    public void handleUseItemOn(ServerboundUseItemOnPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        ServerLevel serverLevel = this.player.getLevel();
        InteractionHand interactionHand = packet.getHand();
        ItemStack itemStack = this.player.getItemInHand(interactionHand);
        BlockHitResult blockHitResult = packet.getHitResult();
        Vec3 vec3 = blockHitResult.getLocation();
        BlockPos blockPos = blockHitResult.getBlockPos();
        Vec3 vec32 = vec3.subtract(Vec3.atCenterOf(blockPos));
        if (this.player.level.getServer() != null && this.player.chunkPosition().getChessboardDistance(new ChunkPos(blockPos)) < this.player.level.getServer().getPlayerList().getViewDistance()) {
            double d = 1.0000001D;
            if (Math.abs(vec32.x()) < 1.0000001D && Math.abs(vec32.y()) < 1.0000001D && Math.abs(vec32.z()) < 1.0000001D) {
                Direction direction = blockHitResult.getDirection();
                this.player.resetLastActionTime();
                int i = this.player.level.getMaxBuildHeight();
                if (blockPos.getY() < i) {
                    if (this.awaitingPositionFromClient == null && this.player.distanceToSqr((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D) < 64.0D && serverLevel.mayInteract(this.player, blockPos)) {
                        InteractionResult interactionResult = this.player.gameMode.useItemOn(this.player, serverLevel, itemStack, interactionHand, blockHitResult);
                        if (direction == Direction.UP && !interactionResult.consumesAction() && blockPos.getY() >= i - 1 && wasBlockPlacementAttempt(this.player, itemStack)) {
                            Component component = (new TranslatableComponent("build.tooHigh", i - 1)).withStyle(ChatFormatting.RED);
                            this.player.sendMessage(component, ChatType.GAME_INFO, Util.NIL_UUID);
                        } else if (interactionResult.shouldSwing()) {
                            this.player.swing(interactionHand, true);
                        }
                    }
                } else {
                    Component component2 = (new TranslatableComponent("build.tooHigh", i - 1)).withStyle(ChatFormatting.RED);
                    this.player.sendMessage(component2, ChatType.GAME_INFO, Util.NIL_UUID);
                }

                this.player.connection.send(new ClientboundBlockUpdatePacket(serverLevel, blockPos));
                this.player.connection.send(new ClientboundBlockUpdatePacket(serverLevel, blockPos.relative(direction)));
            } else {
                LOGGER.warn("Ignoring UseItemOnPacket from {}: Location {} too far away from hit block {}.", this.player.getGameProfile().getName(), vec3, blockPos);
            }
        } else {
            LOGGER.warn("Ignoring UseItemOnPacket from {}: hit position {} too far away from player {}.", this.player.getGameProfile().getName(), blockPos, this.player.blockPosition());
        }
    }

    @Override
    public void handleUseItem(ServerboundUseItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        ServerLevel serverLevel = this.player.getLevel();
        InteractionHand interactionHand = packet.getHand();
        ItemStack itemStack = this.player.getItemInHand(interactionHand);
        this.player.resetLastActionTime();
        if (!itemStack.isEmpty()) {
            InteractionResult interactionResult = this.player.gameMode.useItem(this.player, serverLevel, itemStack, interactionHand);
            if (interactionResult.shouldSwing()) {
                this.player.swing(interactionHand, true);
            }

        }
    }

    @Override
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.isSpectator()) {
            for(ServerLevel serverLevel : this.server.getAllLevels()) {
                Entity entity = packet.getEntity(serverLevel);
                if (entity != null) {
                    this.player.teleportTo(serverLevel, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                    return;
                }
            }
        }

    }

    @Override
    public void handleResourcePackResponse(ServerboundResourcePackPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (packet.getAction() == ServerboundResourcePackPacket.Action.DECLINED && this.server.isResourcePackRequired()) {
            LOGGER.info("Disconnecting {} due to resource pack rejection", (Object)this.player.getName());
            this.disconnect(new TranslatableComponent("multiplayer.requiredTexturePrompt.disconnect"));
        }

    }

    @Override
    public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        Entity entity = this.player.getVehicle();
        if (entity instanceof Boat) {
            ((Boat)entity).setPaddleState(packet.getLeft(), packet.getRight());
        }

    }

    @Override
    public void handlePong(ServerboundPongPacket packet) {
    }

    @Override
    public void onDisconnect(Component reason) {
        LOGGER.info("{} lost connection: {}", this.player.getName().getString(), reason.getString());
        this.server.invalidateStatus();
        this.server.getPlayerList().broadcastMessage((new TranslatableComponent("multiplayer.player.left", this.player.getDisplayName())).withStyle(ChatFormatting.YELLOW), ChatType.SYSTEM, Util.NIL_UUID);
        this.player.disconnect();
        this.server.getPlayerList().remove(this.player);
        this.player.getTextFilter().leave();
        if (this.isSingleplayerOwner()) {
            LOGGER.info("Stopping singleplayer server as player logged out");
            this.server.halt(false);
        }

    }

    @Override
    public void send(Packet<?> packet) {
        this.send(packet, (GenericFutureListener<? extends Future<? super Void>>)null);
    }

    public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener) {
        try {
            this.connection.send(packet, listener);
        } catch (Throwable var6) {
            CrashReport crashReport = CrashReport.forThrowable(var6, "Sending packet");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Packet being sent");
            crashReportCategory.setDetail("Packet class", () -> {
                return packet.getClass().getCanonicalName();
            });
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (packet.getSlot() >= 0 && packet.getSlot() < Inventory.getSelectionSize()) {
            if (this.player.getInventory().selected != packet.getSlot() && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                this.player.stopUsingItem();
            }

            this.player.getInventory().selected = packet.getSlot();
            this.player.resetLastActionTime();
        } else {
            LOGGER.warn("{} tried to set an invalid carried item", (Object)this.player.getName().getString());
        }
    }

    @Override
    public void handleChat(ServerboundChatPacket packet) {
        String string = StringUtils.normalizeSpace(packet.getMessage());

        for(int i = 0; i < string.length(); ++i) {
            if (!SharedConstants.isAllowedChatCharacter(string.charAt(i))) {
                this.disconnect(new TranslatableComponent("multiplayer.disconnect.illegal_characters"));
                return;
            }
        }

        if (string.startsWith("/")) {
            PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
            this.handleChat(TextFilter.FilteredText.passThrough(string));
        } else {
            this.filterTextPacket(string, this::handleChat);
        }

    }

    private void handleChat(TextFilter.FilteredText message) {
        if (this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            this.send(new ClientboundChatPacket((new TranslatableComponent("chat.disabled.options")).withStyle(ChatFormatting.RED), ChatType.SYSTEM, Util.NIL_UUID));
        } else {
            this.player.resetLastActionTime();
            String string = message.getRaw();
            if (string.startsWith("/")) {
                this.handleCommand(string);
            } else {
                String string2 = message.getFiltered();
                Component component = string2.isEmpty() ? null : new TranslatableComponent("chat.type.text", this.player.getDisplayName(), string2);
                Component component2 = new TranslatableComponent("chat.type.text", this.player.getDisplayName(), string);
                this.server.getPlayerList().broadcastMessage(component2, (player) -> {
                    return this.player.shouldFilterMessageTo(player) ? component : component2;
                }, ChatType.CHAT, this.player.getUUID());
            }

            this.chatSpamTickCount += 20;
            if (this.chatSpamTickCount > 200 && !this.server.getPlayerList().isOp(this.player.getGameProfile())) {
                this.disconnect(new TranslatableComponent("disconnect.spam"));
            }

        }
    }

    private void handleCommand(String input) {
        this.server.getCommands().performCommand(this.player.createCommandSourceStack(), input);
    }

    @Override
    public void handleAnimate(ServerboundSwingPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.resetLastActionTime();
        this.player.swing(packet.getHand());
    }

    @Override
    public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.resetLastActionTime();
        switch(packet.getAction()) {
        case PRESS_SHIFT_KEY:
            this.player.setShiftKeyDown(true);
            break;
        case RELEASE_SHIFT_KEY:
            this.player.setShiftKeyDown(false);
            break;
        case START_SPRINTING:
            this.player.setSprinting(true);
            break;
        case STOP_SPRINTING:
            this.player.setSprinting(false);
            break;
        case STOP_SLEEPING:
            if (this.player.isSleeping()) {
                this.player.stopSleepInBed(false, true);
                this.awaitingPositionFromClient = this.player.position();
            }
            break;
        case START_RIDING_JUMP:
            if (this.player.getVehicle() instanceof PlayerRideableJumping) {
                PlayerRideableJumping playerRideableJumping = (PlayerRideableJumping)this.player.getVehicle();
                int i = packet.getData();
                if (playerRideableJumping.canJump() && i > 0) {
                    playerRideableJumping.handleStartJump(i);
                }
            }
            break;
        case STOP_RIDING_JUMP:
            if (this.player.getVehicle() instanceof PlayerRideableJumping) {
                PlayerRideableJumping playerRideableJumping2 = (PlayerRideableJumping)this.player.getVehicle();
                playerRideableJumping2.handleStopJump();
            }
            break;
        case OPEN_INVENTORY:
            if (this.player.getVehicle() instanceof AbstractHorse) {
                ((AbstractHorse)this.player.getVehicle()).openInventory(this.player);
            }
            break;
        case START_FALL_FLYING:
            if (!this.player.tryToStartFallFlying()) {
                this.player.stopFallFlying();
            }
            break;
        default:
            throw new IllegalArgumentException("Invalid client command!");
        }

    }

    @Override
    public void handleInteract(ServerboundInteractPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        ServerLevel serverLevel = this.player.getLevel();
        final Entity entity = packet.getTarget(serverLevel);
        this.player.resetLastActionTime();
        this.player.setShiftKeyDown(packet.isUsingSecondaryAction());
        if (entity != null) {
            if (!serverLevel.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                return;
            }

            double d = 36.0D;
            if (this.player.distanceToSqr(entity) < 36.0D) {
                packet.dispatch(new ServerboundInteractPacket.Handler() {
                    private void performInteraction(InteractionHand hand, ServerGamePacketListenerImpl.EntityInteraction action) {
                        ItemStack itemStack = ServerGamePacketListenerImpl.this.player.getItemInHand(hand).copy();
                        InteractionResult interactionResult = action.run(ServerGamePacketListenerImpl.this.player, entity, hand);
                        if (interactionResult.consumesAction()) {
                            CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(ServerGamePacketListenerImpl.this.player, itemStack, entity);
                            if (interactionResult.shouldSwing()) {
                                ServerGamePacketListenerImpl.this.player.swing(hand, true);
                            }
                        }

                    }

                    @Override
                    public void onInteraction(InteractionHand hand) {
                        this.performInteraction(hand, Player::interactOn);
                    }

                    @Override
                    public void onInteraction(InteractionHand hand, Vec3 pos) {
                        this.performInteraction(hand, (player, entityx, handx) -> {
                            return entityx.interactAt(player, pos, handx);
                        });
                    }

                    @Override
                    public void onAttack() {
                        if (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb) && !(entity instanceof AbstractArrow) && entity != ServerGamePacketListenerImpl.this.player) {
                            ServerGamePacketListenerImpl.this.player.attack(entity);
                        } else {
                            ServerGamePacketListenerImpl.this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_entity_attacked"));
                            ServerGamePacketListenerImpl.LOGGER.warn("Player {} tried to attack an invalid entity", (Object)ServerGamePacketListenerImpl.this.player.getName().getString());
                        }
                    }
                });
            }
        }

    }

    @Override
    public void handleClientCommand(ServerboundClientCommandPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.resetLastActionTime();
        ServerboundClientCommandPacket.Action action = packet.getAction();
        switch(action) {
        case PERFORM_RESPAWN:
            if (this.player.wonGame) {
                this.player.wonGame = false;
                this.player = this.server.getPlayerList().respawn(this.player, true);
                CriteriaTriggers.CHANGED_DIMENSION.trigger(this.player, Level.END, Level.OVERWORLD);
            } else {
                if (this.player.getHealth() > 0.0F) {
                    return;
                }

                this.player = this.server.getPlayerList().respawn(this.player, false);
                if (this.server.isHardcore()) {
                    this.player.setGameMode(GameType.SPECTATOR);
                    this.player.getLevel().getGameRules().getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS).set(false, this.server);
                }
            }
            break;
        case REQUEST_STATS:
            this.player.getStats().sendStats(this.player);
        }

    }

    @Override
    public void handleContainerClose(ServerboundContainerClosePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.doCloseContainer();
    }

    @Override
    public void handleContainerClick(ServerboundContainerClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == packet.getContainerId()) {
            if (this.player.isSpectator()) {
                this.player.containerMenu.sendAllDataToRemote();
            } else {
                int i = packet.getSlotNum();
                if (!this.player.containerMenu.isValidSlotIndex(i)) {
                    LOGGER.debug("Player {} clicked invalid slot index: {}, available slots: {}", this.player.getName(), i, this.player.containerMenu.slots.size());
                } else {
                    boolean bl = packet.getStateId() != this.player.containerMenu.getStateId();
                    this.player.containerMenu.suppressRemoteUpdates();
                    this.player.containerMenu.clicked(i, packet.getButtonNum(), packet.getClickType(), this.player);

                    for(Entry<ItemStack> entry : Int2ObjectMaps.fastIterable(packet.getChangedSlots())) {
                        this.player.containerMenu.setRemoteSlotNoCopy(entry.getIntKey(), entry.getValue());
                    }

                    this.player.containerMenu.setRemoteCarried(packet.getCarriedItem());
                    this.player.containerMenu.resumeRemoteUpdates();
                    if (bl) {
                        this.player.containerMenu.broadcastFullState();
                    } else {
                        this.player.containerMenu.broadcastChanges();
                    }

                }
            }
        }
    }

    @Override
    public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.resetLastActionTime();
        if (!this.player.isSpectator() && this.player.containerMenu.containerId == packet.getContainerId() && this.player.containerMenu instanceof RecipeBookMenu) {
            this.server.getRecipeManager().byKey(packet.getRecipe()).ifPresent((recipe) -> {
                ((RecipeBookMenu)this.player.containerMenu).handlePlacement(packet.isShiftDown(), recipe, this.player);
            });
        }
    }

    @Override
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == packet.getContainerId() && !this.player.isSpectator()) {
            boolean bl = this.player.containerMenu.clickMenuButton(this.player, packet.getButtonId());
            if (bl) {
                this.player.containerMenu.broadcastChanges();
            }
        }

    }

    @Override
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.gameMode.isCreative()) {
            boolean bl = packet.getSlotNum() < 0;
            ItemStack itemStack = packet.getItem();
            CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
            if (!itemStack.isEmpty() && compoundTag != null && compoundTag.contains("x") && compoundTag.contains("y") && compoundTag.contains("z")) {
                BlockPos blockPos = BlockEntity.getPosFromTag(compoundTag);
                BlockEntity blockEntity = this.player.level.getBlockEntity(blockPos);
                if (blockEntity != null) {
                    blockEntity.saveToItem(itemStack);
                }
            }

            boolean bl2 = packet.getSlotNum() >= 1 && packet.getSlotNum() <= 45;
            boolean bl3 = itemStack.isEmpty() || itemStack.getDamageValue() >= 0 && itemStack.getCount() <= 64 && !itemStack.isEmpty();
            if (bl2 && bl3) {
                this.player.inventoryMenu.getSlot(packet.getSlotNum()).set(itemStack);
                this.player.inventoryMenu.broadcastChanges();
            } else if (bl && bl3 && this.dropSpamTickCount < 200) {
                this.dropSpamTickCount += 20;
                this.player.drop(itemStack, true);
            }
        }

    }

    @Override
    public void handleSignUpdate(ServerboundSignUpdatePacket packet) {
        List<String> list = Stream.of(packet.getLines()).map(ChatFormatting::stripFormatting).collect(Collectors.toList());
        this.filterTextPacket(list, (listx) -> {
            this.updateSignText(packet, listx);
        });
    }

    private void updateSignText(ServerboundSignUpdatePacket packet, List<TextFilter.FilteredText> signText) {
        this.player.resetLastActionTime();
        ServerLevel serverLevel = this.player.getLevel();
        BlockPos blockPos = packet.getPos();
        if (serverLevel.hasChunkAt(blockPos)) {
            BlockState blockState = serverLevel.getBlockState(blockPos);
            BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
            if (!(blockEntity instanceof SignBlockEntity)) {
                return;
            }

            SignBlockEntity signBlockEntity = (SignBlockEntity)blockEntity;
            if (!signBlockEntity.isEditable() || !this.player.getUUID().equals(signBlockEntity.getPlayerWhoMayEdit())) {
                LOGGER.warn("Player {} just tried to change non-editable sign", (Object)this.player.getName().getString());
                return;
            }

            for(int i = 0; i < signText.size(); ++i) {
                TextFilter.FilteredText filteredText = signText.get(i);
                if (this.player.isTextFilteringEnabled()) {
                    signBlockEntity.setMessage(i, new TextComponent(filteredText.getFiltered()));
                } else {
                    signBlockEntity.setMessage(i, new TextComponent(filteredText.getRaw()), new TextComponent(filteredText.getFiltered()));
                }
            }

            signBlockEntity.setChanged();
            serverLevel.sendBlockUpdated(blockPos, blockState, blockState, 3);
        }

    }

    @Override
    public void handleKeepAlive(ServerboundKeepAlivePacket packet) {
        if (this.keepAlivePending && packet.getId() == this.keepAliveChallenge) {
            int i = (int)(Util.getMillis() - this.keepAliveTime);
            this.player.latency = (this.player.latency * 3 + i) / 4;
            this.keepAlivePending = false;
        } else if (!this.isSingleplayerOwner()) {
            this.disconnect(new TranslatableComponent("disconnect.timeout"));
        }

    }

    @Override
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.getAbilities().flying = packet.isFlying() && this.player.getAbilities().mayfly;
    }

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        this.player.updateOptions(packet);
    }

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {
    }

    @Override
    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.hasPermissions(2) || this.isSingleplayerOwner()) {
            this.server.setDifficulty(packet.getDifficulty(), false);
        }
    }

    @Override
    public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.getLevel());
        if (this.player.hasPermissions(2) || this.isSingleplayerOwner()) {
            this.server.setDifficultyLocked(packet.isLocked());
        }
    }

    @Override
    public ServerPlayer getPlayer() {
        return this.player;
    }

    @FunctionalInterface
    interface EntityInteraction {
        InteractionResult run(ServerPlayer player, Entity entity, InteractionHand hand);
    }
}
