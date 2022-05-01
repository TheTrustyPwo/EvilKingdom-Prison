package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockBreakAckPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class ServerPlayerGameMode {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected ServerLevel level;
    protected final ServerPlayer player;
    private GameType gameModeForPlayer = GameType.DEFAULT_MODE;
    @Nullable
    private GameType previousGameModeForPlayer;
    private boolean isDestroyingBlock;
    private int destroyProgressStart;
    private BlockPos destroyPos = BlockPos.ZERO;
    private int gameTicks;
    private boolean hasDelayedDestroy;
    private BlockPos delayedDestroyPos = BlockPos.ZERO;
    private int delayedTickStart;
    private int lastSentState = -1;

    public ServerPlayerGameMode(ServerPlayer player) {
        this.player = player;
        this.level = player.getLevel();
    }

    public boolean changeGameModeForPlayer(GameType gameMode) {
        if (gameMode == this.gameModeForPlayer) {
            return false;
        } else {
            this.setGameModeForPlayer(gameMode, this.gameModeForPlayer);
            return true;
        }
    }

    protected void setGameModeForPlayer(GameType gameMode, @Nullable GameType previousGameMode) {
        this.previousGameModeForPlayer = previousGameMode;
        this.gameModeForPlayer = gameMode;
        gameMode.updatePlayerAbilities(this.player.getAbilities());
        this.player.onUpdateAbilities();
        this.player.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_GAME_MODE, this.player));
        this.level.updateSleepingPlayerList();
    }

    public GameType getGameModeForPlayer() {
        return this.gameModeForPlayer;
    }

    @Nullable
    public GameType getPreviousGameModeForPlayer() {
        return this.previousGameModeForPlayer;
    }

    public boolean isSurvival() {
        return this.gameModeForPlayer.isSurvival();
    }

    public boolean isCreative() {
        return this.gameModeForPlayer.isCreative();
    }

    public void tick() {
        ++this.gameTicks;
        if (this.hasDelayedDestroy) {
            BlockState blockState = this.level.getBlockState(this.delayedDestroyPos);
            if (blockState.isAir()) {
                this.hasDelayedDestroy = false;
            } else {
                float f = this.incrementDestroyProgress(blockState, this.delayedDestroyPos, this.delayedTickStart);
                if (f >= 1.0F) {
                    this.hasDelayedDestroy = false;
                    this.destroyBlock(this.delayedDestroyPos);
                }
            }
        } else if (this.isDestroyingBlock) {
            BlockState blockState2 = this.level.getBlockState(this.destroyPos);
            if (blockState2.isAir()) {
                this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                this.lastSentState = -1;
                this.isDestroyingBlock = false;
            } else {
                this.incrementDestroyProgress(blockState2, this.destroyPos, this.destroyProgressStart);
            }
        }

    }

    private float incrementDestroyProgress(BlockState state, BlockPos pos, int i) {
        int j = this.gameTicks - i;
        float f = state.getDestroyProgress(this.player, this.player.level, pos) * (float)(j + 1);
        int k = (int)(f * 10.0F);
        if (k != this.lastSentState) {
            this.level.destroyBlockProgress(this.player.getId(), pos, k);
            this.lastSentState = k;
        }

        return f;
    }

    public void handleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int worldHeight) {
        double d = this.player.getX() - ((double)pos.getX() + 0.5D);
        double e = this.player.getY() - ((double)pos.getY() + 0.5D) + 1.5D;
        double f = this.player.getZ() - ((double)pos.getZ() + 0.5D);
        double g = d * d + e * e + f * f;
        if (g > 36.0D) {
            BlockState blockState;
            if (this.player.level.getServer() != null && this.player.chunkPosition().getChessboardDistance(new ChunkPos(pos)) < this.player.level.getServer().getPlayerList().getViewDistance()) {
                blockState = this.level.getBlockState(pos);
            } else {
                blockState = Blocks.AIR.defaultBlockState();
            }

            this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, blockState, action, false, "too far"));
        } else if (pos.getY() >= worldHeight) {
            this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, this.level.getBlockState(pos), action, false, "too high"));
        } else {
            if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                if (!this.level.mayInteract(this.player, pos)) {
                    this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, this.level.getBlockState(pos), action, false, "may not interact"));
                    return;
                }

                if (this.isCreative()) {
                    this.destroyAndAck(pos, action, "creative destroy");
                    return;
                }

                if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
                    this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, this.level.getBlockState(pos), action, false, "block action restricted"));
                    return;
                }

                this.destroyProgressStart = this.gameTicks;
                float h = 1.0F;
                BlockState blockState3 = this.level.getBlockState(pos);
                if (!blockState3.isAir()) {
                    blockState3.attack(this.level, pos, this.player);
                    h = blockState3.getDestroyProgress(this.player, this.player.level, pos);
                }

                if (!blockState3.isAir() && h >= 1.0F) {
                    this.destroyAndAck(pos, action, "insta mine");
                } else {
                    if (this.isDestroyingBlock) {
                        this.player.connection.send(new ClientboundBlockBreakAckPacket(this.destroyPos, this.level.getBlockState(this.destroyPos), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, false, "abort destroying since another started (client insta mine, server disagreed)"));
                    }

                    this.isDestroyingBlock = true;
                    this.destroyPos = pos.immutable();
                    int i = (int)(h * 10.0F);
                    this.level.destroyBlockProgress(this.player.getId(), pos, i);
                    this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, this.level.getBlockState(pos), action, true, "actual start of destroying"));
                    this.lastSentState = i;
                }
            } else if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                if (pos.equals(this.destroyPos)) {
                    int j = this.gameTicks - this.destroyProgressStart;
                    BlockState blockState4 = this.level.getBlockState(pos);
                    if (!blockState4.isAir()) {
                        float k = blockState4.getDestroyProgress(this.player, this.player.level, pos) * (float)(j + 1);
                        if (k >= 0.7F) {
                            this.isDestroyingBlock = false;
                            this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                            this.destroyAndAck(pos, action, "destroyed");
                            return;
                        }

                        if (!this.hasDelayedDestroy) {
                            this.isDestroyingBlock = false;
                            this.hasDelayedDestroy = true;
                            this.delayedDestroyPos = pos;
                            this.delayedTickStart = this.destroyProgressStart;
                        }
                    }
                }

                this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, this.level.getBlockState(pos), action, true, "stopped destroying"));
            } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
                this.isDestroyingBlock = false;
                if (!Objects.equals(this.destroyPos, pos)) {
                    LOGGER.warn("Mismatch in destroy block pos: {} {}", this.destroyPos, pos);
                    this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                    this.player.connection.send(new ClientboundBlockBreakAckPacket(this.destroyPos, this.level.getBlockState(this.destroyPos), action, true, "aborted mismatched destroying"));
                }

                this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, this.level.getBlockState(pos), action, true, "aborted destroying"));
            }

        }
    }

    public void destroyAndAck(BlockPos pos, ServerboundPlayerActionPacket.Action action, String reason) {
        if (this.destroyBlock(pos)) {
            this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, this.level.getBlockState(pos), action, true, reason));
        } else {
            this.player.connection.send(new ClientboundBlockBreakAckPacket(pos, this.level.getBlockState(pos), action, false, reason));
        }

    }

    public boolean destroyBlock(BlockPos pos) {
        BlockState blockState = this.level.getBlockState(pos);
        if (!this.player.getMainHandItem().getItem().canAttackBlock(blockState, this.level, pos, this.player)) {
            return false;
        } else {
            BlockEntity blockEntity = this.level.getBlockEntity(pos);
            Block block = blockState.getBlock();
            if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks()) {
                this.level.sendBlockUpdated(pos, blockState, blockState, 3);
                return false;
            } else if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
                return false;
            } else {
                block.playerWillDestroy(this.level, pos, blockState, this.player);
                boolean bl = this.level.removeBlock(pos, false);
                if (bl) {
                    block.destroy(this.level, pos, blockState);
                }

                if (this.isCreative()) {
                    return true;
                } else {
                    ItemStack itemStack = this.player.getMainHandItem();
                    ItemStack itemStack2 = itemStack.copy();
                    boolean bl2 = this.player.hasCorrectToolForDrops(blockState);
                    itemStack.mineBlock(this.level, blockState, pos, this.player);
                    if (bl && bl2) {
                        block.playerDestroy(this.level, this.player, pos, blockState, blockEntity, itemStack2);
                    }

                    return true;
                }
            }
        }
    }

    public InteractionResult useItem(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand) {
        if (this.gameModeForPlayer == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        } else if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.PASS;
        } else {
            int i = stack.getCount();
            int j = stack.getDamageValue();
            InteractionResultHolder<ItemStack> interactionResultHolder = stack.use(world, player, hand);
            ItemStack itemStack = interactionResultHolder.getObject();
            if (itemStack == stack && itemStack.getCount() == i && itemStack.getUseDuration() <= 0 && itemStack.getDamageValue() == j) {
                return interactionResultHolder.getResult();
            } else if (interactionResultHolder.getResult() == InteractionResult.FAIL && itemStack.getUseDuration() > 0 && !player.isUsingItem()) {
                return interactionResultHolder.getResult();
            } else {
                player.setItemInHand(hand, itemStack);
                if (this.isCreative()) {
                    itemStack.setCount(i);
                    if (itemStack.isDamageableItem() && itemStack.getDamageValue() != j) {
                        itemStack.setDamageValue(j);
                    }
                }

                if (itemStack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }

                if (!player.isUsingItem()) {
                    player.inventoryMenu.sendAllDataToRemote();
                }

                return interactionResultHolder.getResult();
            }
        }
    }

    public InteractionResult useItemOn(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider menuProvider = blockState.getMenuProvider(world, blockPos);
            if (menuProvider != null) {
                player.openMenu(menuProvider);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        } else {
            boolean bl = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
            boolean bl2 = player.isSecondaryUseActive() && bl;
            ItemStack itemStack = stack.copy();
            if (!bl2) {
                InteractionResult interactionResult = blockState.use(world, player, hand, hitResult);
                if (interactionResult.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockPos, itemStack);
                    return interactionResult;
                }
            }

            if (!stack.isEmpty() && !player.getCooldowns().isOnCooldown(stack.getItem())) {
                UseOnContext useOnContext = new UseOnContext(player, hand, hitResult);
                InteractionResult interactionResult2;
                if (this.isCreative()) {
                    int i = stack.getCount();
                    interactionResult2 = stack.useOn(useOnContext);
                    stack.setCount(i);
                } else {
                    interactionResult2 = stack.useOn(useOnContext);
                }

                if (interactionResult2.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockPos, itemStack);
                }

                return interactionResult2;
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    public void setLevel(ServerLevel world) {
        this.level = world;
    }
}
