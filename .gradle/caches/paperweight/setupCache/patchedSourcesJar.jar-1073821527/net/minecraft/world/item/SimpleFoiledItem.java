package net.minecraft.world.item;

public class SimpleFoiledItem extends Item {
    public SimpleFoiledItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
