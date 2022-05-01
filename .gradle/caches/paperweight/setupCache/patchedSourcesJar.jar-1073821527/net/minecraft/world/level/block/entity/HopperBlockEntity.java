package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

public class HopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {
    public static final int MOVE_ITEM_SPEED = 8;
    public static final int HOPPER_CONTAINER_SIZE = 5;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int cooldownTime = -1;
    private long tickedGameTime;

    public HopperBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.HOPPER, pos, state);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt)) {
            ContainerHelper.loadAllItems(nbt, this.items);
        }

        this.cooldownTime = nbt.getInt("TransferCooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (!this.trySaveLootTable(nbt)) {
            ContainerHelper.saveAllItems(nbt, this.items);
        }

        nbt.putInt("TransferCooldown", this.cooldownTime);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        this.unpackLootTable((Player)null);
        return ContainerHelper.removeItem(this.getItems(), slot, amount);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.unpackLootTable((Player)null);
        this.getItems().set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

    }

    @Override
    protected Component getDefaultName() {
        return new TranslatableComponent("container.hopper");
    }

    public static void pushItemsTick(Level world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity) {
        --blockEntity.cooldownTime;
        blockEntity.tickedGameTime = world.getGameTime();
        if (!blockEntity.isOnCooldown()) {
            blockEntity.setCooldown(0);
            tryMoveItems(world, pos, state, blockEntity, () -> {
                return suckInItems(world, blockEntity);
            });
        }

    }

    private static boolean tryMoveItems(Level world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
        if (world.isClientSide) {
            return false;
        } else {
            if (!blockEntity.isOnCooldown() && state.getValue(HopperBlock.ENABLED)) {
                boolean bl = false;
                if (!blockEntity.isEmpty()) {
                    bl = ejectItems(world, pos, state, blockEntity);
                }

                if (!blockEntity.inventoryFull()) {
                    bl |= booleanSupplier.getAsBoolean();
                }

                if (bl) {
                    blockEntity.setCooldown(8);
                    setChanged(world, pos, state);
                    return true;
                }
            }

            return false;
        }
    }

    private boolean inventoryFull() {
        for(ItemStack itemStack : this.items) {
            if (itemStack.isEmpty() || itemStack.getCount() != itemStack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    private static boolean ejectItems(Level world, BlockPos pos, BlockState state, Container inventory) {
        Container container = getAttachedContainer(world, pos, state);
        if (container == null) {
            return false;
        } else {
            Direction direction = state.getValue(HopperBlock.FACING).getOpposite();
            if (isFullContainer(container, direction)) {
                return false;
            } else {
                for(int i = 0; i < inventory.getContainerSize(); ++i) {
                    if (!inventory.getItem(i).isEmpty()) {
                        ItemStack itemStack = inventory.getItem(i).copy();
                        ItemStack itemStack2 = addItem(inventory, container, inventory.removeItem(i, 1), direction);
                        if (itemStack2.isEmpty()) {
                            container.setChanged();
                            return true;
                        }

                        inventory.setItem(i, itemStack);
                    }
                }

                return false;
            }
        }
    }

    private static IntStream getSlots(Container inventory, Direction side) {
        return inventory instanceof WorldlyContainer ? IntStream.of(((WorldlyContainer)inventory).getSlotsForFace(side)) : IntStream.range(0, inventory.getContainerSize());
    }

    private static boolean isFullContainer(Container inventory, Direction direction) {
        return getSlots(inventory, direction).allMatch((slot) -> {
            ItemStack itemStack = inventory.getItem(slot);
            return itemStack.getCount() >= itemStack.getMaxStackSize();
        });
    }

    private static boolean isEmptyContainer(Container inv, Direction facing) {
        return getSlots(inv, facing).allMatch((slot) -> {
            return inv.getItem(slot).isEmpty();
        });
    }

    public static boolean suckInItems(Level world, Hopper hopper) {
        Container container = getSourceContainer(world, hopper);
        if (container != null) {
            Direction direction = Direction.DOWN;
            return isEmptyContainer(container, direction) ? false : getSlots(container, direction).anyMatch((slot) -> {
                return tryTakeInItemFromSlot(hopper, container, slot, direction);
            });
        } else {
            for(ItemEntity itemEntity : getItemsAtAndAbove(world, hopper)) {
                if (addItem(hopper, itemEntity)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean tryTakeInItemFromSlot(Hopper hopper, Container inventory, int slot, Direction side) {
        ItemStack itemStack = inventory.getItem(slot);
        if (!itemStack.isEmpty() && canTakeItemFromContainer(inventory, itemStack, slot, side)) {
            ItemStack itemStack2 = itemStack.copy();
            ItemStack itemStack3 = addItem(inventory, hopper, inventory.removeItem(slot, 1), (Direction)null);
            if (itemStack3.isEmpty()) {
                inventory.setChanged();
                return true;
            }

            inventory.setItem(slot, itemStack2);
        }

        return false;
    }

    public static boolean addItem(Container inventory, ItemEntity itemEntity) {
        boolean bl = false;
        ItemStack itemStack = itemEntity.getItem().copy();
        ItemStack itemStack2 = addItem((Container)null, inventory, itemStack, (Direction)null);
        if (itemStack2.isEmpty()) {
            bl = true;
            itemEntity.discard();
        } else {
            itemEntity.setItem(itemStack2);
        }

        return bl;
    }

    public static ItemStack addItem(@Nullable Container from, Container to, ItemStack stack, @Nullable Direction side) {
        if (to instanceof WorldlyContainer && side != null) {
            WorldlyContainer worldlyContainer = (WorldlyContainer)to;
            int[] is = worldlyContainer.getSlotsForFace(side);

            for(int i = 0; i < is.length && !stack.isEmpty(); ++i) {
                stack = tryMoveInItem(from, to, stack, is[i], side);
            }
        } else {
            int j = to.getContainerSize();

            for(int k = 0; k < j && !stack.isEmpty(); ++k) {
                stack = tryMoveInItem(from, to, stack, k, side);
            }
        }

        return stack;
    }

    private static boolean canPlaceItemInContainer(Container inventory, ItemStack stack, int slot, @Nullable Direction side) {
        if (!inventory.canPlaceItem(slot, stack)) {
            return false;
        } else {
            return !(inventory instanceof WorldlyContainer) || ((WorldlyContainer)inventory).canPlaceItemThroughFace(slot, stack, side);
        }
    }

    private static boolean canTakeItemFromContainer(Container inv, ItemStack stack, int slot, Direction facing) {
        return !(inv instanceof WorldlyContainer) || ((WorldlyContainer)inv).canTakeItemThroughFace(slot, stack, facing);
    }

    private static ItemStack tryMoveInItem(@Nullable Container from, Container to, ItemStack stack, int slot, @Nullable Direction side) {
        ItemStack itemStack = to.getItem(slot);
        if (canPlaceItemInContainer(to, stack, slot, side)) {
            boolean bl = false;
            boolean bl2 = to.isEmpty();
            if (itemStack.isEmpty()) {
                to.setItem(slot, stack);
                stack = ItemStack.EMPTY;
                bl = true;
            } else if (canMergeItems(itemStack, stack)) {
                int i = stack.getMaxStackSize() - itemStack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.shrink(j);
                itemStack.grow(j);
                bl = j > 0;
            }

            if (bl) {
                if (bl2 && to instanceof HopperBlockEntity) {
                    HopperBlockEntity hopperBlockEntity = (HopperBlockEntity)to;
                    if (!hopperBlockEntity.isOnCustomCooldown()) {
                        int k = 0;
                        if (from instanceof HopperBlockEntity) {
                            HopperBlockEntity hopperBlockEntity2 = (HopperBlockEntity)from;
                            if (hopperBlockEntity.tickedGameTime >= hopperBlockEntity2.tickedGameTime) {
                                k = 1;
                            }
                        }

                        hopperBlockEntity.setCooldown(8 - k);
                    }
                }

                to.setChanged();
            }
        }

        return stack;
    }

    @Nullable
    private static Container getAttachedContainer(Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(HopperBlock.FACING);
        return getContainerAt(world, pos.relative(direction));
    }

    @Nullable
    private static Container getSourceContainer(Level world, Hopper hopper) {
        return getContainerAt(world, hopper.getLevelX(), hopper.getLevelY() + 1.0D, hopper.getLevelZ());
    }

    public static List<ItemEntity> getItemsAtAndAbove(Level world, Hopper hopper) {
        return hopper.getSuckShape().toAabbs().stream().flatMap((box) -> {
            return world.getEntitiesOfClass(ItemEntity.class, box.move(hopper.getLevelX() - 0.5D, hopper.getLevelY() - 0.5D, hopper.getLevelZ() - 0.5D), EntitySelector.ENTITY_STILL_ALIVE).stream();
        }).collect(Collectors.toList());
    }

    @Nullable
    public static Container getContainerAt(Level world, BlockPos pos) {
        return getContainerAt(world, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
    }

    @Nullable
    private static Container getContainerAt(Level world, double x, double y, double z) {
        Container container = null;
        BlockPos blockPos = new BlockPos(x, y, z);
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof WorldlyContainerHolder) {
            container = ((WorldlyContainerHolder)block).getContainer(blockState, world, blockPos);
        } else if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof Container) {
                container = (Container)blockEntity;
                if (container instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    container = ChestBlock.getContainer((ChestBlock)block, blockState, world, blockPos, true);
                }
            }
        }

        if (container == null) {
            List<Entity> list = world.getEntities((Entity)null, new AABB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntitySelector.CONTAINER_ENTITY_SELECTOR);
            if (!list.isEmpty()) {
                container = (Container)list.get(world.random.nextInt(list.size()));
            }
        }

        return container;
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        if (!first.is(second.getItem())) {
            return false;
        } else if (first.getDamageValue() != second.getDamageValue()) {
            return false;
        } else if (first.getCount() > first.getMaxStackSize()) {
            return false;
        } else {
            return ItemStack.tagMatches(first, second);
        }
    }

    @Override
    public double getLevelX() {
        return (double)this.worldPosition.getX() + 0.5D;
    }

    @Override
    public double getLevelY() {
        return (double)this.worldPosition.getY() + 0.5D;
    }

    @Override
    public double getLevelZ() {
        return (double)this.worldPosition.getZ() + 0.5D;
    }

    private void setCooldown(int transferCooldown) {
        this.cooldownTime = transferCooldown;
    }

    private boolean isOnCooldown() {
        return this.cooldownTime > 0;
    }

    private boolean isOnCustomCooldown() {
        return this.cooldownTime > 8;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.items = list;
    }

    public static void entityInside(Level world, BlockPos pos, BlockState state, Entity entity, HopperBlockEntity blockEntity) {
        if (entity instanceof ItemEntity && Shapes.joinIsNotEmpty(Shapes.create(entity.getBoundingBox().move((double)(-pos.getX()), (double)(-pos.getY()), (double)(-pos.getZ()))), blockEntity.getSuckShape(), BooleanOp.AND)) {
            tryMoveItems(world, pos, state, blockEntity, () -> {
                return addItem(blockEntity, (ItemEntity)entity);
            });
        }

    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new HopperMenu(syncId, playerInventory, this);
    }
}
