package net.minecraft.world.level.block.entity;

import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartHopper;
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
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
// CraftBukkit end

public class HopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {

    public static final int MOVE_ITEM_SPEED = 8;
    public static final int HOPPER_CONTAINER_SIZE = 5;
    private NonNullList<ItemStack> items;
    private int cooldownTime;
    private long tickedGameTime;

    // CraftBukkit start - add fields and methods
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    private int maxStack = MAX_STACK;

    public List<ItemStack> getContents() {
        return this.items;
    }

    public void onOpen(CraftHumanEntity who) {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who) {
        this.transaction.remove(who);
    }

    public List<HumanEntity> getViewers() {
        return this.transaction;
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStack;
    }

    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }
    // CraftBukkit end

    public HopperBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.HOPPER, pos, state);
        this.items = NonNullList.withSize(5, ItemStack.EMPTY);
        this.cooldownTime = -1;
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
        this.unpackLootTable((Player) null);
        return ContainerHelper.removeItem(this.getItems(), slot, amount);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.unpackLootTable((Player) null);
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
            // Spigot start
            boolean result = HopperBlockEntity.tryMoveItems(world, pos, state, blockEntity, () -> {
                return HopperBlockEntity.suckInItems(world, blockEntity);
            });
            if (!result && blockEntity.level.spigotConfig.hopperCheck > 1) {
                blockEntity.setCooldown(blockEntity.level.spigotConfig.hopperCheck);
            }
            // Spigot end
        }

    }

    private static boolean tryMoveItems(Level world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleansupplier) {
        if (world.isClientSide) {
            return false;
        } else {
            if (!blockEntity.isOnCooldown() && (Boolean) state.getValue(HopperBlock.ENABLED)) {
                boolean flag = false;

                if (!blockEntity.isEmpty()) {
                    flag = HopperBlockEntity.ejectItems(world, pos, state, (Container) blockEntity, blockEntity); // CraftBukkit
                }

                if (!blockEntity.inventoryFull()) {
                    flag |= booleansupplier.getAsBoolean();
                }

                if (flag) {
                    blockEntity.setCooldown(world.spigotConfig.hopperTransfer); // Spigot
                    setChanged(world, pos, state);
                    return true;
                }
            }

            return false;
        }
    }

    private boolean inventoryFull() {
        Iterator iterator = this.items.iterator();

        ItemStack itemstack;

        do {
            if (!iterator.hasNext()) {
                return true;
            }

            itemstack = (ItemStack) iterator.next();
        } while (!itemstack.isEmpty() && itemstack.getCount() == itemstack.getMaxStackSize());

        return false;
    }
    // Paper start - Optimize Hoppers
    private static boolean skipPullModeEventFire = false;
    private static boolean skipPushModeEventFire = false;
    public static boolean skipHopperEvents = false;

    private static boolean hopperPush(Level level, BlockPos pos, Container destination, Direction enumdirection, HopperBlockEntity hopper) {
        skipPushModeEventFire = skipHopperEvents;
        boolean foundItem = false;
        for (int i = 0; i < hopper.getContainerSize(); ++i) {
            ItemStack item = hopper.getItem(i);
            if (!item.isEmpty()) {
                foundItem = true;
                ItemStack origItemStack = item;
                ItemStack itemstack = origItemStack;

                final int origCount = origItemStack.getCount();
                final int moved = Math.min(level.spigotConfig.hopperAmount, origCount);
                origItemStack.setCount(moved);

                // We only need to fire the event once to give protection plugins a chance to cancel this event
                // Because nothing uses getItem, every event call should end up the same result.
                if (!skipPushModeEventFire) {
                    itemstack = callPushMoveEvent(destination, itemstack, hopper);
                    if (itemstack == null) { // cancelled
                        origItemStack.setCount(origCount);
                        return false;
                    }
                }
                final ItemStack itemstack2 = addItem(hopper, destination, itemstack, enumdirection);
                final int remaining = itemstack2.getCount();
                if (remaining != moved) {
                    origItemStack = origItemStack.cloneItemStack(true);
                    origItemStack.setCount(origCount);
                    if (!origItemStack.isEmpty()) {
                        origItemStack.setCount(origCount - moved + remaining);
                    }
                    hopper.setItem(i, origItemStack);
                    destination.setChanged();
                    return true;
                }
                origItemStack.setCount(origCount);
            }
        }
        if (foundItem && level.paperConfig.cooldownHopperWhenFull) { // Inventory was full - cooldown
            hopper.setCooldown(level.spigotConfig.hopperTransfer);
        }
        return false;
    }

    private static boolean hopperPull(Level level, Hopper ihopper, Container iinventory, ItemStack origItemStack, int i) {
        ItemStack itemstack = origItemStack;
        final int origCount = origItemStack.getCount();
        final int moved = Math.min(level.spigotConfig.hopperAmount, origCount);
        itemstack.setCount(moved);

        if (!skipPullModeEventFire) {
            itemstack = callPullMoveEvent(ihopper, iinventory, itemstack);
            if (itemstack == null) { // cancelled
                origItemStack.setCount(origCount);
                // Drastically improve performance by returning true.
                // No plugin could of relied on the behavior of false as the other call
                // site for IMIE did not exhibit the same behavior
                return true;
            }
        }

        final ItemStack itemstack2 = addItem(iinventory, ihopper, itemstack, null);
        final int remaining = itemstack2.getCount();
        if (remaining != moved) {
            origItemStack = origItemStack.cloneItemStack(true);
            origItemStack.setCount(origCount);
            if (!origItemStack.isEmpty()) {
                origItemStack.setCount(origCount - moved + remaining);
            }
            IGNORE_TILE_UPDATES = true;
            iinventory.setItem(i, origItemStack);
            IGNORE_TILE_UPDATES = false;
            iinventory.setChanged();
            return true;
        }
        origItemStack.setCount(origCount);

        if (level.paperConfig.cooldownHopperWhenFull) {
            cooldownHopper(ihopper);
        }

        return false;
    }

    private static ItemStack callPushMoveEvent(Container iinventory, ItemStack itemstack, HopperBlockEntity hopper) {
        Inventory destinationInventory = getInventory(iinventory);
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(hopper.getOwner(false).getInventory(),
            CraftItemStack.asCraftMirror(itemstack), destinationInventory, true);
        boolean result = event.callEvent();
        if (!event.calledGetItem && !event.calledSetItem) {
            skipPushModeEventFire = true;
        }
        if (!result) {
            cooldownHopper(hopper);
            return null;
        }

        if (event.calledSetItem) {
            return CraftItemStack.asNMSCopy(event.getItem());
        } else {
            return itemstack;
        }
    }

    private static ItemStack callPullMoveEvent(Hopper hopper, Container iinventory, ItemStack itemstack) {
        Inventory sourceInventory = getInventory(iinventory);
        Inventory destination = getInventory(hopper);

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(sourceInventory,
            // Mirror is safe as we no plugins ever use this item
            CraftItemStack.asCraftMirror(itemstack), destination, false);
        boolean result = event.callEvent();
        if (!event.calledGetItem && !event.calledSetItem) {
            skipPullModeEventFire = true;
        }
        if (!result) {
            cooldownHopper(hopper);
            return null;
        }

        if (event.calledSetItem) {
            return CraftItemStack.asNMSCopy(event.getItem());
        } else {
            return itemstack;
        }
    }

    private static Inventory getInventory(Container iinventory) {
        Inventory sourceInventory;// Have to special case large chests as they work oddly
        if (iinventory instanceof CompoundContainer) {
            sourceInventory = new org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryDoubleChest((CompoundContainer) iinventory);
        } else if (iinventory instanceof BlockEntity) {
            sourceInventory = ((BlockEntity) iinventory).getOwner(false).getInventory();
        } else {
            sourceInventory = iinventory.getOwner().getInventory();
        }
        return sourceInventory;
    }

    private static void cooldownHopper(Hopper hopper) {
        if (hopper instanceof HopperBlockEntity blockEntity) {
            blockEntity.setCooldown(blockEntity.getLevel().spigotConfig.hopperTransfer);
        } else if (hopper instanceof MinecartHopper blockEntity) {
            blockEntity.setCooldown(blockEntity.getLevel().spigotConfig.hopperTransfer / 2);
        }
    }
    // Paper end

    private static boolean ejectItems(Level world, BlockPos blockposition, BlockState iblockdata, Container iinventory, HopperBlockEntity hopper) { // CraftBukkit
        Container iinventory1 = HopperBlockEntity.getAttachedContainer(world, blockposition, iblockdata);

        if (iinventory1 == null) {
            return false;
        } else {
            Direction enumdirection = ((Direction) iblockdata.getValue(HopperBlock.FACING)).getOpposite();

            if (HopperBlockEntity.isFullContainer(iinventory1, enumdirection)) {
                return false;
            } else {
                return hopperPush(world, blockposition, iinventory1, enumdirection, hopper); /* // Paper - disable rest
                for (int i = 0; i < iinventory.getContainerSize(); ++i) {
                    if (!iinventory.getItem(i).isEmpty()) {
                        ItemStack itemstack = iinventory.getItem(i).copy();
                        // ItemStack itemstack1 = addItem(iinventory, iinventory1, iinventory.removeItem(i, 1), enumdirection);

                        // CraftBukkit start - Call event when pushing items into other inventories
                        CraftItemStack oitemstack = CraftItemStack.asCraftMirror(iinventory.removeItem(i, world.spigotConfig.hopperAmount)); // Spigot

                        Inventory destinationInventory;
                        // Have to special case large chests as they work oddly
                        if (iinventory1 instanceof CompoundContainer) {
                            destinationInventory = new org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryDoubleChest((CompoundContainer) iinventory1);
                        } else {
                            destinationInventory = iinventory1.getOwner().getInventory();
                        }

                        InventoryMoveItemEvent event = new InventoryMoveItemEvent(iinventory.getOwner().getInventory(), oitemstack.clone(), destinationInventory, true);
                        world.getCraftServer().getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            hopper.setItem(i, itemstack);
                            hopper.setCooldown(world.spigotConfig.hopperTransfer); // Spigot
                            return false;
                        }
                        int origCount = event.getItem().getAmount(); // Spigot
                        ItemStack itemstack1 = HopperBlockEntity.addItem(iinventory, iinventory1, CraftItemStack.asNMSCopy(event.getItem()), enumdirection);
                        // CraftBukkit end

                        if (itemstack1.isEmpty()) {
                            iinventory1.setChanged();
                            return true;
                        }

                        itemstack.shrink(origCount - itemstack1.getCount()); // Spigot
                        iinventory.setItem(i, itemstack);
                    }
                }

                return false;*/ // Paper - end commenting out replaced block for Hopper Optimizations
            }
        }
    }

    private static IntStream getSlots(Container inventory, Direction side) {
        return inventory instanceof WorldlyContainer ? IntStream.of(((WorldlyContainer) inventory).getSlotsForFace(side)) : IntStream.range(0, inventory.getContainerSize());
    }

    private static boolean isFullContainer(Container inventory, Direction direction) {
        return allMatch(inventory, direction, STACK_SIZE_TEST); // Paper - no streams
    }

    private static boolean isEmptyContainer(Container inv, Direction facing) {
        // Paper start
        return allMatch(inv, facing, IS_EMPTY_TEST);
    }
    private static boolean allMatch(Container iinventory, Direction enumdirection, java.util.function.BiPredicate<ItemStack, Integer> test) {
        if (iinventory instanceof WorldlyContainer) {
            for (int i : ((WorldlyContainer) iinventory).getSlotsForFace(enumdirection)) {
                if (!test.test(iinventory.getItem(i), i)) {
                    return false;
                }
            }
        } else {
            int size = iinventory.getContainerSize();
            for (int i = 0; i < size; i++) {
                if (!test.test(iinventory.getItem(i), i)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean anyMatch(Container iinventory, Direction enumdirection, java.util.function.BiPredicate<ItemStack, Integer> test) {
        if (iinventory instanceof WorldlyContainer) {
            for (int i : ((WorldlyContainer) iinventory).getSlotsForFace(enumdirection)) {
                if (test.test(iinventory.getItem(i), i)) {
                    return true;
                }
            }
        } else {
            int size = iinventory.getContainerSize();
            for (int i = 0; i < size; i++) {
                if (test.test(iinventory.getItem(i), i)) {
                    return true;
                }
            }
        }
        return true;
    }
    private static final java.util.function.BiPredicate<ItemStack, Integer> STACK_SIZE_TEST = (itemstack, i) -> itemstack.getCount() >= itemstack.getMaxStackSize();
    private static final java.util.function.BiPredicate<ItemStack, Integer> IS_EMPTY_TEST = (itemstack, i) -> itemstack.isEmpty();
    // Paper end

    public static boolean suckInItems(Level world, Hopper hopper) {
        Container iinventory = HopperBlockEntity.getSourceContainer(world, hopper);

        if (iinventory != null) {
            Direction enumdirection = Direction.DOWN;

            // Paper start - optimize hoppers and remove streams
            skipPullModeEventFire = skipHopperEvents;
            return !HopperBlockEntity.isEmptyContainer(iinventory, enumdirection) && anyMatch(iinventory, enumdirection, (item, i) -> {
                // Logic copied from below to avoid extra getItem calls
                if (!item.isEmpty() && canTakeItemFromContainer(iinventory, item, i, enumdirection)) {
                    return hopperPull(world, hopper, iinventory, item, i);
                } else {
                    return false;
                }
                // Paper end
            });
        } else {
            Iterator iterator = HopperBlockEntity.getItemsAtAndAbove(world, hopper).iterator();

            ItemEntity entityitem;

            do {
                if (!iterator.hasNext()) {
                    return false;
                }

                entityitem = (ItemEntity) iterator.next();
            } while (!HopperBlockEntity.addItem(hopper, entityitem));

            return true;
        }
    }

    // Paper - method unused as logic is inlined above
    private static boolean a(Hopper ihopper, Container iinventory, int i, Direction enumdirection, Level world) { // Spigot
        ItemStack itemstack = iinventory.getItem(i);

        if (!itemstack.isEmpty() && HopperBlockEntity.canTakeItemFromContainer(iinventory, itemstack, i, enumdirection)) { // If this logic changes, update above. this is left inused incase reflective plugins
            return hopperPull(world, ihopper, iinventory, itemstack, i); /* // Paper - disable rest
            ItemStack itemstack1 = itemstack.copy();
            // ItemStack itemstack2 = addItem(iinventory, ihopper, iinventory.removeItem(i, 1), (EnumDirection) null);
            // CraftBukkit start - Call event on collection of items from inventories into the hopper
            CraftItemStack oitemstack = CraftItemStack.asCraftMirror(iinventory.removeItem(i, world.spigotConfig.hopperAmount)); // Spigot

            Inventory sourceInventory;
            // Have to special case large chests as they work oddly
            if (iinventory instanceof CompoundContainer) {
                sourceInventory = new org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryDoubleChest((CompoundContainer) iinventory);
            } else {
                sourceInventory = iinventory.getOwner().getInventory();
            }

            InventoryMoveItemEvent event = new InventoryMoveItemEvent(sourceInventory, oitemstack.clone(), ihopper.getOwner().getInventory(), false);

            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                iinventory.setItem(i, itemstack1);

                if (ihopper instanceof HopperBlockEntity) {
                    ((HopperBlockEntity) ihopper).setCooldown(world.spigotConfig.hopperTransfer); // Spigot
                } else if (ihopper instanceof MinecartHopper) {
                    ((MinecartHopper) ihopper).setCooldown(world.spigotConfig.hopperTransfer / 2); // Spigot
                }
                return false;
            }
            int origCount = event.getItem().getAmount(); // Spigot
            ItemStack itemstack2 = HopperBlockEntity.addItem(iinventory, ihopper, CraftItemStack.asNMSCopy(event.getItem()), null);
            // CraftBukkit end

            if (itemstack2.isEmpty()) {
                iinventory.setChanged();
                return true;
            }

            itemstack1.shrink(origCount - itemstack2.getCount()); // Spigot
            iinventory.setItem(i, itemstack1);*/ // Paper - end commenting out replaced block for Hopper Optimizations
        }

        return false;
    }

    public static boolean addItem(Container inventory, ItemEntity itemEntity) {
        boolean flag = false;
        // CraftBukkit start
        InventoryPickupItemEvent event = new InventoryPickupItemEvent(getInventory(inventory), (org.bukkit.entity.Item) itemEntity.getBukkitEntity()); // Paper - use getInventory() to avoid snapshot creation
        itemEntity.level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        // CraftBukkit end
        ItemStack itemstack = itemEntity.getItem().copy();
        ItemStack itemstack1 = HopperBlockEntity.addItem((Container) null, inventory, itemstack, (Direction) null);

        if (itemstack1.isEmpty()) {
            flag = true;
            itemEntity.discard();
        } else {
            itemEntity.setItem(itemstack1);
        }

        return flag;
    }

    public static ItemStack addItem(@Nullable Container from, Container to, ItemStack stack, @Nullable Direction side) {
        if (to instanceof WorldlyContainer && side != null) {
            WorldlyContainer iworldinventory = (WorldlyContainer) to;
            int[] aint = iworldinventory.getSlotsForFace(side);

            for (int i = 0; i < aint.length && !stack.isEmpty(); ++i) {
                stack = HopperBlockEntity.tryMoveInItem(from, to, stack, aint[i], side);
            }
        } else {
            int j = to.getContainerSize();

            for (int k = 0; k < j && !stack.isEmpty(); ++k) {
                stack = HopperBlockEntity.tryMoveInItem(from, to, stack, k, side);
            }
        }

        return stack;
    }

    private static boolean canPlaceItemInContainer(Container inventory, ItemStack stack, int slot, @Nullable Direction side) {
        return !inventory.canPlaceItem(slot, stack) ? false : !(inventory instanceof WorldlyContainer) || ((WorldlyContainer) inventory).canPlaceItemThroughFace(slot, stack, side);
    }

    private static boolean canTakeItemFromContainer(Container inv, ItemStack stack, int slot, Direction facing) {
        return !(inv instanceof WorldlyContainer) || ((WorldlyContainer) inv).canTakeItemThroughFace(slot, stack, facing);
    }

    private static ItemStack tryMoveInItem(@Nullable Container from, Container to, ItemStack stack, int slot, @Nullable Direction side) {
        ItemStack itemstack1 = to.getItem(slot);

        if (HopperBlockEntity.canPlaceItemInContainer(to, stack, slot, side)) {
            boolean flag = false;
            boolean flag1 = to.isEmpty();

            if (itemstack1.isEmpty()) {
                // Spigot start - SPIGOT-6693, InventorySubcontainer#setItem
                ItemStack leftover = ItemStack.EMPTY; // Paper
                if (!stack.isEmpty() && stack.getCount() > to.getMaxStackSize()) {
                    leftover = stack; // Paper
                    stack = stack.split(to.getMaxStackSize());
                }
                // Spigot end
                IGNORE_TILE_UPDATES = true; // Paper
                to.setItem(slot, stack);
                IGNORE_TILE_UPDATES = false; // Paper
                stack = leftover; // Paper
                flag = true;
            } else if (HopperBlockEntity.canMergeItems(itemstack1, stack)) {
                int j = Math.min(stack.getMaxStackSize(), to.getMaxStackSize()) - itemstack1.getCount(); // Paper
                int k = Math.min(stack.getCount(), j);

                stack.shrink(k);
                itemstack1.grow(k);
                flag = k > 0;
            }

            if (flag) {
                if (flag1 && to instanceof HopperBlockEntity) {
                    HopperBlockEntity tileentityhopper = (HopperBlockEntity) to;

                    if (!tileentityhopper.isOnCustomCooldown()) {
                        byte b0 = 0;

                        if (from instanceof HopperBlockEntity) {
                            HopperBlockEntity tileentityhopper1 = (HopperBlockEntity) from;

                            if (tileentityhopper.tickedGameTime >= tileentityhopper1.tickedGameTime) {
                                b0 = 1;
                            }
                        }

                        tileentityhopper.setCooldown(tileentityhopper.level.spigotConfig.hopperTransfer - b0); // Spigot
                    }
                }

                to.setChanged();
            }
        }

        return stack;
    }

    @Nullable
    private static Container getAttachedContainer(Level world, BlockPos pos, BlockState state) {
        Direction enumdirection = (Direction) state.getValue(HopperBlock.FACING);

        return HopperBlockEntity.getContainerAt(world, pos.relative(enumdirection));
    }

    @Nullable
    private static Container getSourceContainer(Level world, Hopper hopper) {
        return HopperBlockEntity.getContainerAt(world, hopper.getLevelX(), hopper.getLevelY() + 1.0D, hopper.getLevelZ());
    }

    public static List<ItemEntity> getItemsAtAndAbove(Level world, Hopper hopper) {
        // Paper start - Optimize item suck in. remove streams, restore 1.12 checks. Seriously checking the bowl?!
        double d0 = hopper.getLevelX();
        double d1 = hopper.getLevelY();
        double d2 = hopper.getLevelZ();
        AABB bb = new AABB(d0 - 0.5D, d1, d2 - 0.5D, d0 + 0.5D, d1 + 1.5D, d2 + 0.5D);
        return world.getEntitiesOfClass(ItemEntity.class, bb, Entity::isAlive);
        // Paper end
    }

    @Nullable
    public static Container getContainerAt(Level world, BlockPos pos) {
        return HopperBlockEntity.getContainerAt(world, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, true); // Paper
    }

    public static Container getContainerAt(Level world, double x, double y, double z) { return getContainerAt(world, x, y, z, false); } // Paper - overload to default false
    @Nullable
    private static Container getContainerAt(Level world, double x, double y, double z, boolean optimizeEntities) {
        Object object = null;
        BlockPos blockposition = new BlockPos(x, y, z);
        if ( !world.hasChunkAt( blockposition ) ) return null; // Spigot
        BlockState iblockdata = world.getBlockState(blockposition);
        Block block = iblockdata.getBlock();

        if (block instanceof WorldlyContainerHolder) {
            object = ((WorldlyContainerHolder) block).getContainer(iblockdata, world, blockposition);
        } else if (iblockdata.hasBlockEntity()) {
            BlockEntity tileentity = world.getBlockEntity(blockposition);

            if (tileentity instanceof Container) {
                object = (Container) tileentity;
                if (object instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    object = ChestBlock.getContainer((ChestBlock) block, iblockdata, world, blockposition, true);
                }
            }
        }

        if (object == null && (!optimizeEntities || !world.paperConfig.hoppersIgnoreOccludingBlocks || !org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers.getMaterial(block).isOccluding())) { // Paper
            List<Entity> list = world.getEntities((Entity) null, new AABB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntitySelector.CONTAINER_ENTITY_SELECTOR);

            if (!list.isEmpty()) {
                object = (Container) list.get(world.random.nextInt(list.size()));
            }
        }

        return (Container) object;
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        return !first.is(second.getItem()) ? false : (first.getDamageValue() != second.getDamageValue() ? false : (first.getCount() > first.getMaxStackSize() ? false : ItemStack.tagMatches(first, second)));
    }

    @Override
    public double getLevelX() {
        return (double) this.worldPosition.getX() + 0.5D;
    }

    @Override
    public double getLevelY() {
        return (double) this.worldPosition.getY() + 0.5D;
    }

    @Override
    public double getLevelZ() {
        return (double) this.worldPosition.getZ() + 0.5D;
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
        if (entity instanceof ItemEntity && Shapes.joinIsNotEmpty(Shapes.create(entity.getBoundingBox().move((double) (-pos.getX()), (double) (-pos.getY()), (double) (-pos.getZ()))), blockEntity.getSuckShape(), BooleanOp.AND)) {
            HopperBlockEntity.tryMoveItems(world, pos, state, blockEntity, () -> {
                return HopperBlockEntity.addItem(blockEntity, (ItemEntity) entity);
            });
        }

    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory playerInventory) {
        return new HopperMenu(syncId, playerInventory, this);
    }
}
