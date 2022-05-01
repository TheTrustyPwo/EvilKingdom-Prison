package net.minecraft.world.inventory;

import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Slot {
    public final int slot;
    public final Container container;
    public int index;
    public final int x;
    public final int y;

    public Slot(Container inventory, int index, int x, int y) {
        this.container = inventory;
        this.slot = index;
        this.x = x;
        this.y = y;
    }

    public void onQuickCraft(ItemStack newItem, ItemStack original) {
        int i = original.getCount() - newItem.getCount();
        if (i > 0) {
            this.onQuickCraft(original, i);
        }

    }

    protected void onQuickCraft(ItemStack stack, int amount) {
    }

    protected void onSwapCraft(int amount) {
    }

    protected void checkTakeAchievements(ItemStack stack) {
    }

    public void onTake(Player player, ItemStack stack) {
        this.setChanged();
    }

    public boolean mayPlace(ItemStack stack) {
        return true;
    }

    public ItemStack getItem() {
        return this.container.getItem(this.slot);
    }

    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    public void set(ItemStack stack) {
        this.container.setItem(this.slot, stack);
        this.setChanged();
    }

    public void setChanged() {
        this.container.setChanged();
    }

    public int getMaxStackSize() {
        return this.container.getMaxStackSize();
    }

    public int getMaxStackSize(ItemStack stack) {
        return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
    }

    @Nullable
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return null;
    }

    public ItemStack remove(int amount) {
        return this.container.removeItem(this.slot, amount);
    }

    public boolean mayPickup(Player playerEntity) {
        return true;
    }

    public boolean isActive() {
        return true;
    }

    public Optional<ItemStack> tryRemove(int min, int max, Player player) {
        if (!this.mayPickup(player)) {
            return Optional.empty();
        } else if (!this.allowModification(player) && max < this.getItem().getCount()) {
            return Optional.empty();
        } else {
            min = Math.min(min, max);
            ItemStack itemStack = this.remove(min);
            if (itemStack.isEmpty()) {
                return Optional.empty();
            } else {
                if (this.getItem().isEmpty()) {
                    this.set(ItemStack.EMPTY);
                }

                return Optional.of(itemStack);
            }
        }
    }

    public ItemStack safeTake(int min, int max, Player player) {
        Optional<ItemStack> optional = this.tryRemove(min, max, player);
        optional.ifPresent((stack) -> {
            this.onTake(player, stack);
        });
        return optional.orElse(ItemStack.EMPTY);
    }

    public ItemStack safeInsert(ItemStack stack) {
        return this.safeInsert(stack, stack.getCount());
    }

    public ItemStack safeInsert(ItemStack stack, int count) {
        if (!stack.isEmpty() && this.mayPlace(stack)) {
            ItemStack itemStack = this.getItem();
            int i = Math.min(Math.min(count, stack.getCount()), this.getMaxStackSize(stack) - itemStack.getCount());
            if (itemStack.isEmpty()) {
                this.set(stack.split(i));
            } else if (ItemStack.isSameItemSameTags(itemStack, stack)) {
                stack.shrink(i);
                itemStack.grow(i);
                this.set(itemStack);
            }

            return stack;
        } else {
            return stack;
        }
    }

    public boolean allowModification(Player player) {
        return this.mayPickup(player) && this.mayPlace(this.getItem());
    }

    public int getContainerSlot() {
        return this.slot;
    }
}
