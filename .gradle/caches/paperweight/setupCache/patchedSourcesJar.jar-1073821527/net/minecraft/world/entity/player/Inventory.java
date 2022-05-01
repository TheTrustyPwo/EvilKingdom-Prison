package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Inventory implements Container, Nameable {
    public static final int POP_TIME_DURATION = 5;
    public static final int INVENTORY_SIZE = 36;
    private static final int SELECTION_SIZE = 9;
    public static final int SLOT_OFFHAND = 40;
    public static final int NOT_FOUND_INDEX = -1;
    public static final int[] ALL_ARMOR_SLOTS = new int[]{0, 1, 2, 3};
    public static final int[] HELMET_SLOT_ONLY = new int[]{3};
    public final NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
    public final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
    public final NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);
    public final List<NonNullList<ItemStack>> compartments = ImmutableList.of(this.items, this.armor, this.offhand);
    public int selected;
    public final Player player;
    private int timesChanged;

    public Inventory(Player player) {
        this.player = player;
    }

    public ItemStack getSelected() {
        return isHotbarSlot(this.selected) ? this.items.get(this.selected) : ItemStack.EMPTY;
    }

    public static int getSelectionSize() {
        return 9;
    }

    private boolean hasRemainingSpaceForItem(ItemStack existingStack, ItemStack stack) {
        return !existingStack.isEmpty() && ItemStack.isSameItemSameTags(existingStack, stack) && existingStack.isStackable() && existingStack.getCount() < existingStack.getMaxStackSize() && existingStack.getCount() < this.getMaxStackSize();
    }

    public int getFreeSlot() {
        for(int i = 0; i < this.items.size(); ++i) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public void setPickedItem(ItemStack stack) {
        int i = this.findSlotMatchingItem(stack);
        if (isHotbarSlot(i)) {
            this.selected = i;
        } else {
            if (i == -1) {
                this.selected = this.getSuitableHotbarSlot();
                if (!this.items.get(this.selected).isEmpty()) {
                    int j = this.getFreeSlot();
                    if (j != -1) {
                        this.items.set(j, this.items.get(this.selected));
                    }
                }

                this.items.set(this.selected, stack);
            } else {
                this.pickSlot(i);
            }

        }
    }

    public void pickSlot(int slot) {
        this.selected = this.getSuitableHotbarSlot();
        ItemStack itemStack = this.items.get(this.selected);
        this.items.set(this.selected, this.items.get(slot));
        this.items.set(slot, itemStack);
    }

    public static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < 9;
    }

    public int findSlotMatchingItem(ItemStack stack) {
        for(int i = 0; i < this.items.size(); ++i) {
            if (!this.items.get(i).isEmpty() && ItemStack.isSameItemSameTags(stack, this.items.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public int findSlotMatchingUnusedItem(ItemStack stack) {
        for(int i = 0; i < this.items.size(); ++i) {
            ItemStack itemStack = this.items.get(i);
            if (!this.items.get(i).isEmpty() && ItemStack.isSameItemSameTags(stack, this.items.get(i)) && !this.items.get(i).isDamaged() && !itemStack.isEnchanted() && !itemStack.hasCustomHoverName()) {
                return i;
            }
        }

        return -1;
    }

    public int getSuitableHotbarSlot() {
        for(int i = 0; i < 9; ++i) {
            int j = (this.selected + i) % 9;
            if (this.items.get(j).isEmpty()) {
                return j;
            }
        }

        for(int k = 0; k < 9; ++k) {
            int l = (this.selected + k) % 9;
            if (!this.items.get(l).isEnchanted()) {
                return l;
            }
        }

        return this.selected;
    }

    public void swapPaint(double scrollAmount) {
        int i = (int)Math.signum(scrollAmount);

        for(this.selected -= i; this.selected < 0; this.selected += 9) {
        }

        while(this.selected >= 9) {
            this.selected -= 9;
        }

    }

    public int clearOrCountMatchingItems(Predicate<ItemStack> shouldRemove, int maxCount, Container craftingInventory) {
        int i = 0;
        boolean bl = maxCount == 0;
        i += ContainerHelper.clearOrCountMatchingItems(this, shouldRemove, maxCount - i, bl);
        i += ContainerHelper.clearOrCountMatchingItems(craftingInventory, shouldRemove, maxCount - i, bl);
        ItemStack itemStack = this.player.containerMenu.getCarried();
        i += ContainerHelper.clearOrCountMatchingItems(itemStack, shouldRemove, maxCount - i, bl);
        if (itemStack.isEmpty()) {
            this.player.containerMenu.setCarried(ItemStack.EMPTY);
        }

        return i;
    }

    private int addResource(ItemStack stack) {
        int i = this.getSlotWithRemainingSpace(stack);
        if (i == -1) {
            i = this.getFreeSlot();
        }

        return i == -1 ? stack.getCount() : this.addResource(i, stack);
    }

    private int addResource(int slot, ItemStack stack) {
        Item item = stack.getItem();
        int i = stack.getCount();
        ItemStack itemStack = this.getItem(slot);
        if (itemStack.isEmpty()) {
            itemStack = new ItemStack(item, 0);
            if (stack.hasTag()) {
                itemStack.setTag(stack.getTag().copy());
            }

            this.setItem(slot, itemStack);
        }

        int j = i;
        if (i > itemStack.getMaxStackSize() - itemStack.getCount()) {
            j = itemStack.getMaxStackSize() - itemStack.getCount();
        }

        if (j > this.getMaxStackSize() - itemStack.getCount()) {
            j = this.getMaxStackSize() - itemStack.getCount();
        }

        if (j == 0) {
            return i;
        } else {
            i -= j;
            itemStack.grow(j);
            itemStack.setPopTime(5);
            return i;
        }
    }

    public int getSlotWithRemainingSpace(ItemStack stack) {
        if (this.hasRemainingSpaceForItem(this.getItem(this.selected), stack)) {
            return this.selected;
        } else if (this.hasRemainingSpaceForItem(this.getItem(40), stack)) {
            return 40;
        } else {
            for(int i = 0; i < this.items.size(); ++i) {
                if (this.hasRemainingSpaceForItem(this.items.get(i), stack)) {
                    return i;
                }
            }

            return -1;
        }
    }

    public void tick() {
        for(NonNullList<ItemStack> nonNullList : this.compartments) {
            for(int i = 0; i < nonNullList.size(); ++i) {
                if (!nonNullList.get(i).isEmpty()) {
                    nonNullList.get(i).inventoryTick(this.player.level, this.player, i, this.selected == i);
                }
            }
        }

    }

    public boolean add(ItemStack stack) {
        return this.add(-1, stack);
    }

    public boolean add(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            try {
                if (stack.isDamaged()) {
                    if (slot == -1) {
                        slot = this.getFreeSlot();
                    }

                    if (slot >= 0) {
                        this.items.set(slot, stack.copy());
                        this.items.get(slot).setPopTime(5);
                        stack.setCount(0);
                        return true;
                    } else if (this.player.getAbilities().instabuild) {
                        stack.setCount(0);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    int i;
                    do {
                        i = stack.getCount();
                        if (slot == -1) {
                            stack.setCount(this.addResource(stack));
                        } else {
                            stack.setCount(this.addResource(slot, stack));
                        }
                    } while(!stack.isEmpty() && stack.getCount() < i);

                    if (stack.getCount() == i && this.player.getAbilities().instabuild) {
                        stack.setCount(0);
                        return true;
                    } else {
                        return stack.getCount() < i;
                    }
                }
            } catch (Throwable var6) {
                CrashReport crashReport = CrashReport.forThrowable(var6, "Adding item to inventory");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Item being added");
                crashReportCategory.setDetail("Item ID", Item.getId(stack.getItem()));
                crashReportCategory.setDetail("Item data", stack.getDamageValue());
                crashReportCategory.setDetail("Item name", () -> {
                    return stack.getHoverName().getString();
                });
                throw new ReportedException(crashReport);
            }
        }
    }

    public void placeItemBackInInventory(ItemStack stack) {
        this.placeItemBackInInventory(stack, true);
    }

    public void placeItemBackInInventory(ItemStack stack, boolean notifiesClient) {
        while(true) {
            if (!stack.isEmpty()) {
                int i = this.getSlotWithRemainingSpace(stack);
                if (i == -1) {
                    i = this.getFreeSlot();
                }

                if (i != -1) {
                    int j = stack.getMaxStackSize() - this.getItem(i).getCount();
                    if (this.add(i, stack.split(j)) && notifiesClient && this.player instanceof ServerPlayer) {
                        ((ServerPlayer)this.player).connection.send(new ClientboundContainerSetSlotPacket(-2, 0, i, this.getItem(i)));
                    }
                    continue;
                }

                this.player.drop(stack, false);
            }

            return;
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        List<ItemStack> list = null;

        for(NonNullList<ItemStack> nonNullList : this.compartments) {
            if (slot < nonNullList.size()) {
                list = nonNullList;
                break;
            }

            slot -= nonNullList.size();
        }

        return list != null && !list.get(slot).isEmpty() ? ContainerHelper.removeItem(list, slot, amount) : ItemStack.EMPTY;
    }

    public void removeItem(ItemStack stack) {
        for(NonNullList<ItemStack> nonNullList : this.compartments) {
            for(int i = 0; i < nonNullList.size(); ++i) {
                if (nonNullList.get(i) == stack) {
                    nonNullList.set(i, ItemStack.EMPTY);
                    break;
                }
            }
        }

    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        NonNullList<ItemStack> nonNullList = null;

        for(NonNullList<ItemStack> nonNullList2 : this.compartments) {
            if (slot < nonNullList2.size()) {
                nonNullList = nonNullList2;
                break;
            }

            slot -= nonNullList2.size();
        }

        if (nonNullList != null && !nonNullList.get(slot).isEmpty()) {
            ItemStack itemStack = nonNullList.get(slot);
            nonNullList.set(slot, ItemStack.EMPTY);
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        NonNullList<ItemStack> nonNullList = null;

        for(NonNullList<ItemStack> nonNullList2 : this.compartments) {
            if (slot < nonNullList2.size()) {
                nonNullList = nonNullList2;
                break;
            }

            slot -= nonNullList2.size();
        }

        if (nonNullList != null) {
            nonNullList.set(slot, stack);
        }

    }

    public float getDestroySpeed(BlockState block) {
        return this.items.get(this.selected).getDestroySpeed(block);
    }

    public ListTag save(ListTag nbtList) {
        for(int i = 0; i < this.items.size(); ++i) {
            if (!this.items.get(i).isEmpty()) {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putByte("Slot", (byte)i);
                this.items.get(i).save(compoundTag);
                nbtList.add(compoundTag);
            }
        }

        for(int j = 0; j < this.armor.size(); ++j) {
            if (!this.armor.get(j).isEmpty()) {
                CompoundTag compoundTag2 = new CompoundTag();
                compoundTag2.putByte("Slot", (byte)(j + 100));
                this.armor.get(j).save(compoundTag2);
                nbtList.add(compoundTag2);
            }
        }

        for(int k = 0; k < this.offhand.size(); ++k) {
            if (!this.offhand.get(k).isEmpty()) {
                CompoundTag compoundTag3 = new CompoundTag();
                compoundTag3.putByte("Slot", (byte)(k + 150));
                this.offhand.get(k).save(compoundTag3);
                nbtList.add(compoundTag3);
            }
        }

        return nbtList;
    }

    public void load(ListTag nbtList) {
        this.items.clear();
        this.armor.clear();
        this.offhand.clear();

        for(int i = 0; i < nbtList.size(); ++i) {
            CompoundTag compoundTag = nbtList.getCompound(i);
            int j = compoundTag.getByte("Slot") & 255;
            ItemStack itemStack = ItemStack.of(compoundTag);
            if (!itemStack.isEmpty()) {
                if (j >= 0 && j < this.items.size()) {
                    this.items.set(j, itemStack);
                } else if (j >= 100 && j < this.armor.size() + 100) {
                    this.armor.set(j - 100, itemStack);
                } else if (j >= 150 && j < this.offhand.size() + 150) {
                    this.offhand.set(j - 150, itemStack);
                }
            }
        }

    }

    @Override
    public int getContainerSize() {
        return this.items.size() + this.armor.size() + this.offhand.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        for(ItemStack itemStack2 : this.armor) {
            if (!itemStack2.isEmpty()) {
                return false;
            }
        }

        for(ItemStack itemStack3 : this.offhand) {
            if (!itemStack3.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        List<ItemStack> list = null;

        for(NonNullList<ItemStack> nonNullList : this.compartments) {
            if (slot < nonNullList.size()) {
                list = nonNullList;
                break;
            }

            slot -= nonNullList.size();
        }

        return list == null ? ItemStack.EMPTY : list.get(slot);
    }

    @Override
    public Component getName() {
        return new TranslatableComponent("container.inventory");
    }

    public ItemStack getArmor(int slot) {
        return this.armor.get(slot);
    }

    public void hurtArmor(DamageSource damageSource, float amount, int[] slots) {
        if (!(amount <= 0.0F)) {
            amount /= 4.0F;
            if (amount < 1.0F) {
                amount = 1.0F;
            }

            for(int i : slots) {
                ItemStack itemStack = this.armor.get(i);
                if ((!damageSource.isFire() || !itemStack.getItem().isFireResistant()) && itemStack.getItem() instanceof ArmorItem) {
                    itemStack.hurtAndBreak((int)amount, this.player, (player) -> {
                        player.broadcastBreakEvent(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, i));
                    });
                }
            }

        }
    }

    public void dropAll() {
        for(List<ItemStack> list : this.compartments) {
            for(int i = 0; i < list.size(); ++i) {
                ItemStack itemStack = list.get(i);
                if (!itemStack.isEmpty()) {
                    this.player.drop(itemStack, true, false);
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }

    }

    @Override
    public void setChanged() {
        ++this.timesChanged;
    }

    public int getTimesChanged() {
        return this.timesChanged;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.player.isRemoved()) {
            return false;
        } else {
            return !(player.distanceToSqr(this.player) > 64.0D);
        }
    }

    public boolean contains(ItemStack stack) {
        for(List<ItemStack> list : this.compartments) {
            for(ItemStack itemStack : list) {
                if (!itemStack.isEmpty() && itemStack.sameItem(stack)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean contains(TagKey<Item> tag) {
        for(List<ItemStack> list : this.compartments) {
            for(ItemStack itemStack : list) {
                if (!itemStack.isEmpty() && itemStack.is(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void replaceWith(Inventory other) {
        for(int i = 0; i < this.getContainerSize(); ++i) {
            this.setItem(i, other.getItem(i));
        }

        this.selected = other.selected;
    }

    @Override
    public void clearContent() {
        for(List<ItemStack> list : this.compartments) {
            list.clear();
        }

    }

    public void fillStackedContents(StackedContents finder) {
        for(ItemStack itemStack : this.items) {
            finder.accountSimpleStack(itemStack);
        }

    }

    public ItemStack removeFromSelected(boolean entireStack) {
        ItemStack itemStack = this.getSelected();
        return itemStack.isEmpty() ? ItemStack.EMPTY : this.removeItem(this.selected, entireStack ? itemStack.getCount() : 1);
    }
}
