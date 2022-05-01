package net.minecraft.world.item.trading;

import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;

public interface Merchant {
    void setTradingPlayer(@Nullable Player customer);

    @Nullable
    Player getTradingPlayer();

    MerchantOffers getOffers();

    void overrideOffers(MerchantOffers offers);

    void notifyTrade(MerchantOffer offer);

    void notifyTradeUpdated(ItemStack stack);

    int getVillagerXp();

    void overrideXp(int experience);

    boolean showProgressBar();

    SoundEvent getNotifyTradeSound();

    default boolean canRestock() {
        return false;
    }

    default void openTradingScreen(Player player, Component test, int levelProgress) {
        OptionalInt optionalInt = player.openMenu(new SimpleMenuProvider((syncId, playerInventory, playerx) -> {
            return new MerchantMenu(syncId, playerInventory, this);
        }, test));
        if (optionalInt.isPresent()) {
            MerchantOffers merchantOffers = this.getOffers();
            if (!merchantOffers.isEmpty()) {
                player.sendMerchantOffers(optionalInt.getAsInt(), merchantOffers, levelProgress, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
            }
        }

    }

    boolean isClientSide();
}
