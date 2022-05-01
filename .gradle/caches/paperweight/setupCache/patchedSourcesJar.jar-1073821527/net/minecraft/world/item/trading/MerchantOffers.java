package net.minecraft.world.item.trading;

import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class MerchantOffers extends ArrayList<MerchantOffer> {
    public MerchantOffers() {
    }

    public MerchantOffers(CompoundTag nbt) {
        ListTag listTag = nbt.getList("Recipes", 10);

        for(int i = 0; i < listTag.size(); ++i) {
            this.add(new MerchantOffer(listTag.getCompound(i)));
        }

    }

    @Nullable
    public MerchantOffer getRecipeFor(ItemStack firstBuyItem, ItemStack secondBuyItem, int index) {
        if (index > 0 && index < this.size()) {
            MerchantOffer merchantOffer = this.get(index);
            return merchantOffer.satisfiedBy(firstBuyItem, secondBuyItem) ? merchantOffer : null;
        } else {
            for(int i = 0; i < this.size(); ++i) {
                MerchantOffer merchantOffer2 = this.get(i);
                if (merchantOffer2.satisfiedBy(firstBuyItem, secondBuyItem)) {
                    return merchantOffer2;
                }
            }

            return null;
        }
    }

    public void writeToStream(FriendlyByteBuf buf) {
        buf.writeByte((byte)(this.size() & 255));

        for(int i = 0; i < this.size(); ++i) {
            MerchantOffer merchantOffer = this.get(i);
            buf.writeItem(merchantOffer.getBaseCostA());
            buf.writeItem(merchantOffer.getResult());
            ItemStack itemStack = merchantOffer.getCostB();
            buf.writeBoolean(!itemStack.isEmpty());
            if (!itemStack.isEmpty()) {
                buf.writeItem(itemStack);
            }

            buf.writeBoolean(merchantOffer.isOutOfStock());
            buf.writeInt(merchantOffer.getUses());
            buf.writeInt(merchantOffer.getMaxUses());
            buf.writeInt(merchantOffer.getXp());
            buf.writeInt(merchantOffer.getSpecialPriceDiff());
            buf.writeFloat(merchantOffer.getPriceMultiplier());
            buf.writeInt(merchantOffer.getDemand());
        }

    }

    public static MerchantOffers createFromStream(FriendlyByteBuf buf) {
        MerchantOffers merchantOffers = new MerchantOffers();
        int i = buf.readByte() & 255;

        for(int j = 0; j < i; ++j) {
            ItemStack itemStack = buf.readItem();
            ItemStack itemStack2 = buf.readItem();
            ItemStack itemStack3 = ItemStack.EMPTY;
            if (buf.readBoolean()) {
                itemStack3 = buf.readItem();
            }

            boolean bl = buf.readBoolean();
            int k = buf.readInt();
            int l = buf.readInt();
            int m = buf.readInt();
            int n = buf.readInt();
            float f = buf.readFloat();
            int o = buf.readInt();
            MerchantOffer merchantOffer = new MerchantOffer(itemStack, itemStack3, itemStack2, k, l, m, f, o);
            if (bl) {
                merchantOffer.setToOutOfStock();
            }

            merchantOffer.setSpecialPriceDiff(n);
            merchantOffers.add(merchantOffer);
        }

        return merchantOffers;
    }

    public CompoundTag createTag() {
        CompoundTag compoundTag = new CompoundTag();
        ListTag listTag = new ListTag();

        for(int i = 0; i < this.size(); ++i) {
            MerchantOffer merchantOffer = this.get(i);
            listTag.add(merchantOffer.createTag());
        }

        compoundTag.put("Recipes", listTag);
        return compoundTag;
    }
}
