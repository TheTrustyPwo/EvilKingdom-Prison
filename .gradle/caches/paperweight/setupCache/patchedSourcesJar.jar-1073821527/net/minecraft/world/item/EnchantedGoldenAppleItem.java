package net.minecraft.world.item;

public class EnchantedGoldenAppleItem extends Item {
    public EnchantedGoldenAppleItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
