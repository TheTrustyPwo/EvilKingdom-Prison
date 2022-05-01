package net.minecraft.world.inventory;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class MerchantResultSlot extends Slot {
    private final MerchantContainer slots;
    private final Player player;
    private int removeCount;
    private final Merchant merchant;

    public MerchantResultSlot(Player player, Merchant merchant, MerchantContainer merchantInventory, int index, int x, int y) {
        super(merchantInventory, index, x, y);
        this.player = player;
        this.merchant = merchant;
        this.slots = merchantInventory;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }

        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.removeCount += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        stack.onCraftedBy(this.player.level, this.player, this.removeCount);
        this.removeCount = 0;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        MerchantOffer merchantOffer = this.slots.getActiveOffer();
        if (merchantOffer != null) {
            ItemStack itemStack = this.slots.getItem(0);
            ItemStack itemStack2 = this.slots.getItem(1);
            if (merchantOffer.take(itemStack, itemStack2) || merchantOffer.take(itemStack2, itemStack)) {
                this.merchant.notifyTrade(merchantOffer);
                player.awardStat(Stats.TRADED_WITH_VILLAGER);
                this.slots.setItem(0, itemStack);
                this.slots.setItem(1, itemStack2);
            }

            this.merchant.overrideXp(this.merchant.getVillagerXp() + merchantOffer.getXp());
        }

    }
}
