package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShulkerBoxBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int COLUMNS = 9;
    public static final int ROWS = 3;
    public static final int CONTAINER_SIZE = 27;
    public static final int EVENT_SET_OPEN_COUNT = 1;
    public static final int OPENING_TICK_LENGTH = 10;
    public static final float MAX_LID_HEIGHT = 0.5F;
    public static final float MAX_LID_ROTATION = 270.0F;
    public static final String ITEMS_TAG = "Items";
    private static final int[] SLOTS = IntStream.range(0, 27).toArray();
    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(27, ItemStack.EMPTY);
    public int openCount;
    private ShulkerBoxBlockEntity.AnimationStatus animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
    private float progress;
    private float progressOld;
    @Nullable
    private final DyeColor color;

    public ShulkerBoxBlockEntity(@Nullable DyeColor color, BlockPos pos, BlockState state) {
        super(BlockEntityType.SHULKER_BOX, pos, state);
        this.color = color;
    }

    public ShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.SHULKER_BOX, pos, state);
        this.color = ShulkerBoxBlock.getColorFromBlock(state.getBlock());
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ShulkerBoxBlockEntity blockEntity) {
        blockEntity.updateAnimation(world, pos, state);
    }

    private void updateAnimation(Level world, BlockPos pos, BlockState state) {
        this.progressOld = this.progress;
        switch(this.animationStatus) {
        case CLOSED:
            this.progress = 0.0F;
            break;
        case OPENING:
            this.progress += 0.1F;
            if (this.progress >= 1.0F) {
                this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENED;
                this.progress = 1.0F;
                doNeighborUpdates(world, pos, state);
            }

            this.moveCollidedEntities(world, pos, state);
            break;
        case CLOSING:
            this.progress -= 0.1F;
            if (this.progress <= 0.0F) {
                this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
                this.progress = 0.0F;
                doNeighborUpdates(world, pos, state);
            }
            break;
        case OPENED:
            this.progress = 1.0F;
        }

    }

    public ShulkerBoxBlockEntity.AnimationStatus getAnimationStatus() {
        return this.animationStatus;
    }

    public AABB getBoundingBox(BlockState state) {
        return Shulker.getProgressAabb(state.getValue(ShulkerBoxBlock.FACING), 0.5F * this.getProgress(1.0F));
    }

    private void moveCollidedEntities(Level world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof ShulkerBoxBlock) {
            Direction direction = state.getValue(ShulkerBoxBlock.FACING);
            AABB aABB = Shulker.getProgressDeltaAabb(direction, this.progressOld, this.progress).move(pos);
            List<Entity> list = world.getEntities((Entity)null, aABB);
            if (!list.isEmpty()) {
                for(int i = 0; i < list.size(); ++i) {
                    Entity entity = list.get(i);
                    if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
                        entity.move(MoverType.SHULKER_BOX, new Vec3((aABB.getXsize() + 0.01D) * (double)direction.getStepX(), (aABB.getYsize() + 0.01D) * (double)direction.getStepY(), (aABB.getZsize() + 0.01D) * (double)direction.getStepZ()));
                    }
                }

            }
        }
    }

    @Override
    public int getContainerSize() {
        return this.itemStacks.size();
    }

    @Override
    public boolean triggerEvent(int type, int data) {
        if (type == 1) {
            this.openCount = data;
            if (data == 0) {
                this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSING;
                doNeighborUpdates(this.getLevel(), this.worldPosition, this.getBlockState());
            }

            if (data == 1) {
                this.animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENING;
                doNeighborUpdates(this.getLevel(), this.worldPosition, this.getBlockState());
            }

            return true;
        } else {
            return super.triggerEvent(type, data);
        }
    }

    private static void doNeighborUpdates(Level world, BlockPos pos, BlockState state) {
        state.updateNeighbourShapes(world, pos, 3);
    }

    @Override
    public void startOpen(Player player) {
        if (!player.isSpectator()) {
            if (this.openCount < 0) {
                this.openCount = 0;
            }

            ++this.openCount;
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
            if (this.openCount == 1) {
                this.level.gameEvent(player, GameEvent.CONTAINER_OPEN, this.worldPosition);
                this.level.playSound((Player)null, this.worldPosition, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
            }
        }

    }

    @Override
    public void stopOpen(Player player) {
        if (!player.isSpectator()) {
            --this.openCount;
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
            if (this.openCount <= 0) {
                this.level.gameEvent(player, GameEvent.CONTAINER_CLOSE, this.worldPosition);
                this.level.playSound((Player)null, this.worldPosition, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
            }
        }

    }

    @Override
    protected Component getDefaultName() {
        return new TranslatableComponent("container.shulkerBox");
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.loadFromTag(nbt);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (!this.trySaveLootTable(nbt)) {
            ContainerHelper.saveAllItems(nbt, this.itemStacks, false);
        }

    }

    public void loadFromTag(CompoundTag nbt) {
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt) && nbt.contains("Items", 9)) {
            ContainerHelper.loadAllItems(nbt, this.itemStacks);
        }

    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.itemStacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.itemStacks = list;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return !(Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    public float getProgress(float delta) {
        return Mth.lerp(delta, this.progressOld, this.progress);
    }

    @Nullable
    public DyeColor getColor() {
        return this.color;
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new ShulkerBoxMenu(syncId, playerInventory, this);
    }

    public boolean isClosed() {
        return this.animationStatus == ShulkerBoxBlockEntity.AnimationStatus.CLOSED;
    }

    public static enum AnimationStatus {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING;
    }
}
