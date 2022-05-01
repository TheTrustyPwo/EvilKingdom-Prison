package net.minecraft.world.item;

public class BookItem extends Item {
    public BookItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
