package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

public abstract class AbstractContainerMenu {
    private static Logger LOGGER = LogUtils.getLogger();
    public static final int SLOT_CLICKED_OUTSIDE = -999;
    public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
    public static final int QUICKCRAFT_TYPE_GREEDY = 1;
    public static final int QUICKCRAFT_TYPE_CLONE = 2;
    public static final int QUICKCRAFT_HEADER_START = 0;
    public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
    public static final int QUICKCRAFT_HEADER_END = 2;
    public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
    public NonNullList<ItemStack> lastSlots = NonNullList.create();
    public NonNullList<Slot> slots = NonNullList.create();
    public List<DataSlot> dataSlots = Lists.newArrayList();
    private ItemStack carried = ItemStack.EMPTY;
    public NonNullList<ItemStack> remoteSlots = NonNullList.create();
    public IntList remoteDataSlots = new IntArrayList();
    private ItemStack remoteCarried = ItemStack.EMPTY;
    private int stateId;
    @Nullable
    private final MenuType<?> menuType;
    public final int containerId;
    private int quickcraftType = -1;
    private int quickcraftStatus;
    private final Set<Slot> quickcraftSlots = Sets.newHashSet();
    private final List<ContainerListener> containerListeners = Lists.newArrayList();
    @Nullable
    private ContainerSynchronizer synchronizer;
    private boolean suppressRemoteUpdates;

    protected AbstractContainerMenu(@Nullable MenuType<?> type, int syncId) {
        this.menuType = type;
        this.containerId = syncId;
    }

    protected static boolean stillValid(ContainerLevelAccess context, Player player, Block block) {
        return context.evaluate((world, pos) -> {
            return !world.getBlockState(pos).is(block) ? false : player.distanceToSqr((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    public MenuType<?> getType() {
        if (this.menuType == null) {
            throw new UnsupportedOperationException("Unable to construct this menu by type");
        } else {
            return this.menuType;
        }
    }

    protected static void checkContainerSize(Container inventory, int expectedSize) {
        int i = inventory.getContainerSize();
        if (i < expectedSize) {
            throw new IllegalArgumentException("Container size " + i + " is smaller than expected " + expectedSize);
        }
    }

    protected static void checkContainerDataCount(ContainerData data, int expectedCount) {
        int i = data.getCount();
        if (i < expectedCount) {
            throw new IllegalArgumentException("Container data count " + i + " is smaller than expected " + expectedCount);
        }
    }

    public boolean isValidSlotIndex(int slot) {
        return slot == -1 || slot == -999 || slot < this.slots.size();
    }

    protected Slot addSlot(Slot slot) {
        slot.index = this.slots.size();
        this.slots.add(slot);
        this.lastSlots.add(ItemStack.EMPTY);
        this.remoteSlots.add(ItemStack.EMPTY);
        return slot;
    }

    protected DataSlot addDataSlot(DataSlot property) {
        this.dataSlots.add(property);
        this.remoteDataSlots.add(0);
        return property;
    }

    protected void addDataSlots(ContainerData propertyDelegate) {
        for(int i = 0; i < propertyDelegate.getCount(); ++i) {
            this.addDataSlot(DataSlot.forContainer(propertyDelegate, i));
        }

    }

    public void addSlotListener(ContainerListener listener) {
        if (!this.containerListeners.contains(listener)) {
            this.containerListeners.add(listener);
            this.broadcastChanges();
        }
    }

    public void setSynchronizer(ContainerSynchronizer handler) {
        this.synchronizer = handler;
        this.sendAllDataToRemote();
    }

    public void sendAllDataToRemote() {
        int i = 0;

        for(int j = this.slots.size(); i < j; ++i) {
            this.remoteSlots.set(i, this.slots.get(i).getItem().copy());
        }

        this.remoteCarried = this.getCarried().copy();
        i = 0;

        for(int l = this.dataSlots.size(); i < l; ++i) {
            this.remoteDataSlots.set(i, this.dataSlots.get(i).get());
        }

        if (this.synchronizer != null) {
            this.synchronizer.sendInitialData(this, this.remoteSlots, this.remoteCarried, this.remoteDataSlots.toIntArray());
        }

    }

    public void removeSlotListener(ContainerListener listener) {
        this.containerListeners.remove(listener);
    }

    public NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> nonNullList = NonNullList.create();

        for(Slot slot : this.slots) {
            nonNullList.add(slot.getItem());
        }

        return nonNullList;
    }

    public void broadcastChanges() {
        for(int i = 0; i < this.slots.size(); ++i) {
            ItemStack itemStack = this.slots.get(i).getItem();
            Supplier<ItemStack> supplier = Suppliers.memoize(itemStack::copy);
            this.triggerSlotListeners(i, itemStack, supplier);
            this.synchronizeSlotToRemote(i, itemStack, supplier);
        }

        this.synchronizeCarriedToRemote();

        for(int j = 0; j < this.dataSlots.size(); ++j) {
            DataSlot dataSlot = this.dataSlots.get(j);
            int k = dataSlot.get();
            if (dataSlot.checkAndClearUpdateFlag()) {
                this.updateDataSlotListeners(j, k);
            }

            this.synchronizeDataSlotToRemote(j, k);
        }

    }

    public void broadcastFullState() {
        for(int i = 0; i < this.slots.size(); ++i) {
            ItemStack itemStack = this.slots.get(i).getItem();
            this.triggerSlotListeners(i, itemStack, itemStack::copy);
        }

        for(int j = 0; j < this.dataSlots.size(); ++j) {
            DataSlot dataSlot = this.dataSlots.get(j);
            if (dataSlot.checkAndClearUpdateFlag()) {
                this.updateDataSlotListeners(j, dataSlot.get());
            }
        }

        this.sendAllDataToRemote();
    }

    private void updateDataSlotListeners(int index, int value) {
        for(ContainerListener containerListener : this.containerListeners) {
            containerListener.dataChanged(this, index, value);
        }

    }

    private void triggerSlotListeners(int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
        ItemStack itemStack = this.lastSlots.get(slot);
        if (!ItemStack.matches(itemStack, stack)) {
            ItemStack itemStack2 = copySupplier.get();
            this.lastSlots.set(slot, itemStack2);

            for(ContainerListener containerListener : this.containerListeners) {
                containerListener.slotChanged(this, slot, itemStack2);
            }
        }

    }

    private void synchronizeSlotToRemote(int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
        if (!this.suppressRemoteUpdates) {
            ItemStack itemStack = this.remoteSlots.get(slot);
            if (!ItemStack.matches(itemStack, stack)) {
                ItemStack itemStack2 = copySupplier.get();
                this.remoteSlots.set(slot, itemStack2);
                if (this.synchronizer != null) {
                    this.synchronizer.sendSlotChange(this, slot, itemStack2);
                }
            }

        }
    }

    private void synchronizeDataSlotToRemote(int id, int value) {
        if (!this.suppressRemoteUpdates) {
            int i = this.remoteDataSlots.getInt(id);
            if (i != value) {
                this.remoteDataSlots.set(id, value);
                if (this.synchronizer != null) {
                    this.synchronizer.sendDataChange(this, id, value);
                }
            }

        }
    }

    private void synchronizeCarriedToRemote() {
        if (!this.suppressRemoteUpdates) {
            if (!ItemStack.matches(this.getCarried(), this.remoteCarried)) {
                this.remoteCarried = this.getCarried().copy();
                if (this.synchronizer != null) {
                    this.synchronizer.sendCarriedChange(this, this.remoteCarried);
                }
            }

        }
    }

    public void setRemoteSlot(int slot, ItemStack stack) {
        this.remoteSlots.set(slot, stack.copy());
    }

    public void setRemoteSlotNoCopy(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.remoteSlots.size()) {
            this.remoteSlots.set(slot, stack);
        } else {
            LOGGER.debug("Incorrect slot index: {} available slots: {}", slot, this.remoteSlots.size());
        }
    }

    public void setRemoteCarried(ItemStack stack) {
        this.remoteCarried = stack.copy();
    }

    public boolean clickMenuButton(Player player, int id) {
        return false;
    }

    public Slot getSlot(int index) {
        return this.slots.get(index);
    }

    public ItemStack quickMoveStack(Player player, int index) {
        return this.slots.get(index).getItem();
    }

    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        try {
            this.doClick(slotIndex, button, actionType, player);
        } catch (Exception var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Container click");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Click info");
            crashReportCategory.setDetail("Menu Type", () -> {
                return this.menuType != null ? Registry.MENU.getKey(this.menuType).toString() : "<no type>";
            });
            crashReportCategory.setDetail("Menu Class", () -> {
                return this.getClass().getCanonicalName();
            });
            crashReportCategory.setDetail("Slot Count", this.slots.size());
            crashReportCategory.setDetail("Slot", slotIndex);
            crashReportCategory.setDetail("Button", button);
            crashReportCategory.setDetail("Type", actionType);
            throw new ReportedException(crashReport);
        }
    }

    private void doClick(int slotIndex, int button, ClickType actionType, Player player) {
        Inventory inventory = player.getInventory();
        if (actionType == ClickType.QUICK_CRAFT) {
            int i = this.quickcraftStatus;
            this.quickcraftStatus = getQuickcraftHeader(button);
            if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.quickcraftStatus == 0) {
                this.quickcraftType = getQuickcraftType(button);
                if (isValidQuickcraftType(this.quickcraftType, player)) {
                    this.quickcraftStatus = 1;
                    this.quickcraftSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.quickcraftStatus == 1) {
                Slot slot = this.slots.get(slotIndex);
                ItemStack itemStack = this.getCarried();
                if (canItemQuickReplace(slot, itemStack, true) && slot.mayPlace(itemStack) && (this.quickcraftType == 2 || itemStack.getCount() > this.quickcraftSlots.size()) && this.canDragTo(slot)) {
                    this.quickcraftSlots.add(slot);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (this.quickcraftSlots.size() == 1) {
                        int j = (this.quickcraftSlots.iterator().next()).index;
                        this.resetQuickCraft();
                        this.doClick(j, this.quickcraftType, ClickType.PICKUP, player);
                        return;
                    }

                    ItemStack itemStack2 = this.getCarried().copy();
                    int k = this.getCarried().getCount();

                    for(Slot slot2 : this.quickcraftSlots) {
                        ItemStack itemStack3 = this.getCarried();
                        if (slot2 != null && canItemQuickReplace(slot2, itemStack3, true) && slot2.mayPlace(itemStack3) && (this.quickcraftType == 2 || itemStack3.getCount() >= this.quickcraftSlots.size()) && this.canDragTo(slot2)) {
                            ItemStack itemStack4 = itemStack2.copy();
                            int l = slot2.hasItem() ? slot2.getItem().getCount() : 0;
                            getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, itemStack4, l);
                            int m = Math.min(itemStack4.getMaxStackSize(), slot2.getMaxStackSize(itemStack4));
                            if (itemStack4.getCount() > m) {
                                itemStack4.setCount(m);
                            }

                            k -= itemStack4.getCount() - l;
                            slot2.set(itemStack4);
                        }
                    }

                    itemStack2.setCount(k);
                    this.setCarried(itemStack2);
                }

                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((actionType == ClickType.PICKUP || actionType == ClickType.QUICK_MOVE) && (button == 0 || button == 1)) {
            ClickAction clickAction = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (slotIndex == -999) {
                if (!this.getCarried().isEmpty()) {
                    if (clickAction == ClickAction.PRIMARY) {
                        player.drop(this.getCarried(), true);
                        this.setCarried(ItemStack.EMPTY);
                    } else {
                        player.drop(this.getCarried().split(1), true);
                    }
                }
            } else if (actionType == ClickType.QUICK_MOVE) {
                if (slotIndex < 0) {
                    return;
                }

                Slot slot3 = this.slots.get(slotIndex);
                if (!slot3.mayPickup(player)) {
                    return;
                }

                for(ItemStack itemStack5 = this.quickMoveStack(player, slotIndex); !itemStack5.isEmpty() && ItemStack.isSame(slot3.getItem(), itemStack5); itemStack5 = this.quickMoveStack(player, slotIndex)) {
                }
            } else {
                if (slotIndex < 0) {
                    return;
                }

                Slot slot4 = this.slots.get(slotIndex);
                ItemStack itemStack6 = slot4.getItem();
                ItemStack itemStack7 = this.getCarried();
                player.updateTutorialInventoryAction(itemStack7, slot4.getItem(), clickAction);
                if (!itemStack7.overrideStackedOnOther(slot4, clickAction, player) && !itemStack6.overrideOtherStackedOnMe(itemStack7, slot4, clickAction, player, this.createCarriedSlotAccess())) {
                    if (itemStack6.isEmpty()) {
                        if (!itemStack7.isEmpty()) {
                            int n = clickAction == ClickAction.PRIMARY ? itemStack7.getCount() : 1;
                            this.setCarried(slot4.safeInsert(itemStack7, n));
                        }
                    } else if (slot4.mayPickup(player)) {
                        if (itemStack7.isEmpty()) {
                            int o = clickAction == ClickAction.PRIMARY ? itemStack6.getCount() : (itemStack6.getCount() + 1) / 2;
                            Optional<ItemStack> optional = slot4.tryRemove(o, Integer.MAX_VALUE, player);
                            optional.ifPresent((stack) -> {
                                this.setCarried(stack);
                                slot4.onTake(player, stack);
                            });
                        } else if (slot4.mayPlace(itemStack7)) {
                            if (ItemStack.isSameItemSameTags(itemStack6, itemStack7)) {
                                int p = clickAction == ClickAction.PRIMARY ? itemStack7.getCount() : 1;
                                this.setCarried(slot4.safeInsert(itemStack7, p));
                            } else if (itemStack7.getCount() <= slot4.getMaxStackSize(itemStack7)) {
                                slot4.set(itemStack7);
                                this.setCarried(itemStack6);
                            }
                        } else if (ItemStack.isSameItemSameTags(itemStack6, itemStack7)) {
                            Optional<ItemStack> optional2 = slot4.tryRemove(itemStack6.getCount(), itemStack7.getMaxStackSize() - itemStack7.getCount(), player);
                            optional2.ifPresent((stack) -> {
                                itemStack7.grow(stack.getCount());
                                slot4.onTake(player, stack);
                            });
                        }
                    }
                }

                slot4.setChanged();
            }
        } else if (actionType == ClickType.SWAP) {
            Slot slot5 = this.slots.get(slotIndex);
            ItemStack itemStack8 = inventory.getItem(button);
            ItemStack itemStack9 = slot5.getItem();
            if (!itemStack8.isEmpty() || !itemStack9.isEmpty()) {
                if (itemStack8.isEmpty()) {
                    if (slot5.mayPickup(player)) {
                        inventory.setItem(button, itemStack9);
                        slot5.onSwapCraft(itemStack9.getCount());
                        slot5.set(ItemStack.EMPTY);
                        slot5.onTake(player, itemStack9);
                    }
                } else if (itemStack9.isEmpty()) {
                    if (slot5.mayPlace(itemStack8)) {
                        int q = slot5.getMaxStackSize(itemStack8);
                        if (itemStack8.getCount() > q) {
                            slot5.set(itemStack8.split(q));
                        } else {
                            inventory.setItem(button, ItemStack.EMPTY);
                            slot5.set(itemStack8);
                        }
                    }
                } else if (slot5.mayPickup(player) && slot5.mayPlace(itemStack8)) {
                    int r = slot5.getMaxStackSize(itemStack8);
                    if (itemStack8.getCount() > r) {
                        slot5.set(itemStack8.split(r));
                        slot5.onTake(player, itemStack9);
                        if (!inventory.add(itemStack9)) {
                            player.drop(itemStack9, true);
                        }
                    } else {
                        inventory.setItem(button, itemStack9);
                        slot5.set(itemStack8);
                        slot5.onTake(player, itemStack9);
                    }
                }
            }
        } else if (actionType == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty() && slotIndex >= 0) {
            Slot slot6 = this.slots.get(slotIndex);
            if (slot6.hasItem()) {
                ItemStack itemStack10 = slot6.getItem().copy();
                itemStack10.setCount(itemStack10.getMaxStackSize());
                this.setCarried(itemStack10);
            }
        } else if (actionType == ClickType.THROW && this.getCarried().isEmpty() && slotIndex >= 0) {
            Slot slot7 = this.slots.get(slotIndex);
            int s = button == 0 ? 1 : slot7.getItem().getCount();
            ItemStack itemStack11 = slot7.safeTake(s, Integer.MAX_VALUE, player);
            player.drop(itemStack11, true);
        } else if (actionType == ClickType.PICKUP_ALL && slotIndex >= 0) {
            Slot slot8 = this.slots.get(slotIndex);
            ItemStack itemStack12 = this.getCarried();
            if (!itemStack12.isEmpty() && (!slot8.hasItem() || !slot8.mayPickup(player))) {
                int t = button == 0 ? 0 : this.slots.size() - 1;
                int u = button == 0 ? 1 : -1;

                for(int v = 0; v < 2; ++v) {
                    for(int w = t; w >= 0 && w < this.slots.size() && itemStack12.getCount() < itemStack12.getMaxStackSize(); w += u) {
                        Slot slot9 = this.slots.get(w);
                        if (slot9.hasItem() && canItemQuickReplace(slot9, itemStack12, true) && slot9.mayPickup(player) && this.canTakeItemForPickAll(itemStack12, slot9)) {
                            ItemStack itemStack13 = slot9.getItem();
                            if (v != 0 || itemStack13.getCount() != itemStack13.getMaxStackSize()) {
                                ItemStack itemStack14 = slot9.safeTake(itemStack13.getCount(), itemStack12.getMaxStackSize() - itemStack12.getCount(), player);
                                itemStack12.grow(itemStack14.getCount());
                            }
                        }
                    }
                }
            }
        }

    }

    private SlotAccess createCarriedSlotAccess() {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return AbstractContainerMenu.this.getCarried();
            }

            @Override
            public boolean set(ItemStack stack) {
                AbstractContainerMenu.this.setCarried(stack);
                return true;
            }
        };
    }

    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return true;
    }

    public void removed(Player player) {
        if (player instanceof ServerPlayer) {
            ItemStack itemStack = this.getCarried();
            if (!itemStack.isEmpty()) {
                if (player.isAlive() && !((ServerPlayer)player).hasDisconnected()) {
                    player.getInventory().placeItemBackInInventory(itemStack);
                } else {
                    player.drop(itemStack, false);
                }

                this.setCarried(ItemStack.EMPTY);
            }
        }

    }

    protected void clearContainer(Player player, Container inventory) {
        if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer)player).hasDisconnected()) {
            for(int i = 0; i < inventory.getContainerSize(); ++i) {
                player.drop(inventory.removeItemNoUpdate(i), false);
            }

        } else {
            for(int j = 0; j < inventory.getContainerSize(); ++j) {
                Inventory inventory2 = player.getInventory();
                if (inventory2.player instanceof ServerPlayer) {
                    inventory2.placeItemBackInInventory(inventory.removeItemNoUpdate(j));
                }
            }

        }
    }

    public void slotsChanged(Container inventory) {
        this.broadcastChanges();
    }

    public void setItem(int slot, int revision, ItemStack stack) {
        this.getSlot(slot).set(stack);
        this.stateId = revision;
    }

    public void initializeContents(int revision, List<ItemStack> stacks, ItemStack cursorStack) {
        for(int i = 0; i < stacks.size(); ++i) {
            this.getSlot(i).set(stacks.get(i));
        }

        this.carried = cursorStack;
        this.stateId = revision;
    }

    public void setData(int id, int value) {
        this.dataSlots.get(id).set(value);
    }

    public abstract boolean stillValid(Player player);

    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
        boolean bl = false;
        int i = startIndex;
        if (fromLast) {
            i = endIndex - 1;
        }

        if (stack.isStackable()) {
            while(!stack.isEmpty()) {
                if (fromLast) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot = this.slots.get(i);
                ItemStack itemStack = slot.getItem();
                if (!itemStack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemStack)) {
                    int j = itemStack.getCount() + stack.getCount();
                    if (j <= stack.getMaxStackSize()) {
                        stack.setCount(0);
                        itemStack.setCount(j);
                        slot.setChanged();
                        bl = true;
                    } else if (itemStack.getCount() < stack.getMaxStackSize()) {
                        stack.shrink(stack.getMaxStackSize() - itemStack.getCount());
                        itemStack.setCount(stack.getMaxStackSize());
                        slot.setChanged();
                        bl = true;
                    }
                }

                if (fromLast) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!stack.isEmpty()) {
            if (fromLast) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while(true) {
                if (fromLast) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot2 = this.slots.get(i);
                ItemStack itemStack2 = slot2.getItem();
                if (itemStack2.isEmpty() && slot2.mayPlace(stack)) {
                    if (stack.getCount() > slot2.getMaxStackSize()) {
                        slot2.set(stack.split(slot2.getMaxStackSize()));
                    } else {
                        slot2.set(stack.split(stack.getCount()));
                    }

                    slot2.setChanged();
                    bl = true;
                    break;
                }

                if (fromLast) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return bl;
    }

    public static int getQuickcraftType(int quickCraftData) {
        return quickCraftData >> 2 & 3;
    }

    public static int getQuickcraftHeader(int quickCraftData) {
        return quickCraftData & 3;
    }

    public static int getQuickcraftMask(int quickCraftStage, int buttonId) {
        return quickCraftStage & 3 | (buttonId & 3) << 2;
    }

    public static boolean isValidQuickcraftType(int stage, Player player) {
        if (stage == 0) {
            return true;
        } else if (stage == 1) {
            return true;
        } else {
            return stage == 2 && player.getAbilities().instabuild;
        }
    }

    protected void resetQuickCraft() {
        this.quickcraftStatus = 0;
        this.quickcraftSlots.clear();
    }

    public static boolean canItemQuickReplace(@Nullable Slot slot, ItemStack stack, boolean allowOverflow) {
        boolean bl = slot == null || !slot.hasItem();
        if (!bl && ItemStack.isSameItemSameTags(stack, slot.getItem())) {
            return slot.getItem().getCount() + (allowOverflow ? 0 : stack.getCount()) <= stack.getMaxStackSize();
        } else {
            return bl;
        }
    }

    public static void getQuickCraftSlotCount(Set<Slot> slots, int mode, ItemStack stack, int stackSize) {
        switch(mode) {
        case 0:
            stack.setCount(Mth.floor((float)stack.getCount() / (float)slots.size()));
            break;
        case 1:
            stack.setCount(1);
            break;
        case 2:
            stack.setCount(stack.getItem().getMaxStackSize());
        }

        stack.grow(stackSize);
    }

    public boolean canDragTo(Slot slot) {
        return true;
    }

    public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity entity) {
        return entity instanceof Container ? getRedstoneSignalFromContainer((Container)entity) : 0;
    }

    public static int getRedstoneSignalFromContainer(@Nullable Container inventory) {
        if (inventory == null) {
            return 0;
        } else {
            int i = 0;
            float f = 0.0F;

            for(int j = 0; j < inventory.getContainerSize(); ++j) {
                ItemStack itemStack = inventory.getItem(j);
                if (!itemStack.isEmpty()) {
                    f += (float)itemStack.getCount() / (float)Math.min(inventory.getMaxStackSize(), itemStack.getMaxStackSize());
                    ++i;
                }
            }

            f /= (float)inventory.getContainerSize();
            return Mth.floor(f * 14.0F) + (i > 0 ? 1 : 0);
        }
    }

    public void setCarried(ItemStack stack) {
        this.carried = stack;
    }

    public ItemStack getCarried() {
        return this.carried;
    }

    public void suppressRemoteUpdates() {
        this.suppressRemoteUpdates = true;
    }

    public void resumeRemoteUpdates() {
        this.suppressRemoteUpdates = false;
    }

    public void transferState(AbstractContainerMenu handler) {
        Table<Container, Integer, Integer> table = HashBasedTable.create();

        for(int i = 0; i < handler.slots.size(); ++i) {
            Slot slot = handler.slots.get(i);
            table.put(slot.container, slot.getContainerSlot(), i);
        }

        for(int j = 0; j < this.slots.size(); ++j) {
            Slot slot2 = this.slots.get(j);
            Integer integer = table.get(slot2.container, slot2.getContainerSlot());
            if (integer != null) {
                this.lastSlots.set(j, handler.lastSlots.get(integer));
                this.remoteSlots.set(j, handler.remoteSlots.get(integer));
            }
        }

    }

    public OptionalInt findSlot(Container inventory, int index) {
        for(int i = 0; i < this.slots.size(); ++i) {
            Slot slot = this.slots.get(i);
            if (slot.container == inventory && index == slot.getContainerSlot()) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    public int getStateId() {
        return this.stateId;
    }

    public int incrementStateId() {
        this.stateId = this.stateId + 1 & 32767;
        return this.stateId;
    }
}
