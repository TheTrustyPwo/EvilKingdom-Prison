package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantContainer implements Container {
    private final Merchant merchant;
    private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
    @Nullable
    private MerchantOffer activeOffer;
    public int selectionHint;
    private int futureXp;

    public MerchantContainer(Merchant merchant) {
        this.merchant = merchant;
    }

    @Override
    public int getContainerSize() {
        return this.itemStacks.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemStack : this.itemStacks) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.itemStacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack itemStack = this.itemStacks.get(slot);
        if (slot == 2 && !itemStack.isEmpty()) {
            return ContainerHelper.removeItem(this.itemStacks, slot, itemStack.getCount());
        } else {
            ItemStack itemStack2 = ContainerHelper.removeItem(this.itemStacks, slot, amount);
            if (!itemStack2.isEmpty() && this.isPaymentSlot(slot)) {
                this.updateSellItem();
            }

            return itemStack2;
        }
    }

    private boolean isPaymentSlot(int slot) {
        return slot == 0 || slot == 1;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.itemStacks, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.itemStacks.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        if (this.isPaymentSlot(slot)) {
            this.updateSellItem();
        }

    }

    @Override
    public boolean stillValid(Player player) {
        return this.merchant.getTradingPlayer() == player;
    }

    @Override
    public void setChanged() {
        this.updateSellItem();
    }

    public void updateSellItem() {
        this.activeOffer = null;
        ItemStack itemStack;
        ItemStack itemStack2;
        if (this.itemStacks.get(0).isEmpty()) {
            itemStack = this.itemStacks.get(1);
            itemStack2 = ItemStack.EMPTY;
        } else {
            itemStack = this.itemStacks.get(0);
            itemStack2 = this.itemStacks.get(1);
        }

        if (itemStack.isEmpty()) {
            this.setItem(2, ItemStack.EMPTY);
            this.futureXp = 0;
        } else {
            MerchantOffers merchantOffers = this.merchant.getOffers();
            if (!merchantOffers.isEmpty()) {
                MerchantOffer merchantOffer = merchantOffers.getRecipeFor(itemStack, itemStack2, this.selectionHint);
                if (merchantOffer == null || merchantOffer.isOutOfStock()) {
                    this.activeOffer = merchantOffer;
                    merchantOffer = merchantOffers.getRecipeFor(itemStack2, itemStack, this.selectionHint);
                }

                if (merchantOffer != null && !merchantOffer.isOutOfStock()) {
                    this.activeOffer = merchantOffer;
                    this.setItem(2, merchantOffer.assemble());
                    this.futureXp = merchantOffer.getXp();
                } else {
                    this.setItem(2, ItemStack.EMPTY);
                    this.futureXp = 0;
                }
            }

            this.merchant.notifyTradeUpdated(this.getItem(2));
        }
    }

    @Nullable
    public MerchantOffer getActiveOffer() {
        return this.activeOffer;
    }

    public void setSelectionHint(int index) {
        this.selectionHint = index;
        this.updateSellItem();
    }

    @Override
    public void clearContent() {
        this.itemStacks.clear();
    }

    public int getFutureXp() {
        return this.futureXp;
    }
}
