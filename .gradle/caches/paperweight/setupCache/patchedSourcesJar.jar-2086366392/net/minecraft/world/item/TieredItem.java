package net.minecraft.world.item;

public class TieredItem extends Item {
    private final Tier tier;

    public TieredItem(Tier material, Item.Properties settings) {
        super(settings.defaultDurability(material.getUses()));
        this.tier = material;
    }

    public Tier getTier() {
        return this.tier;
    }

    @Override
    public int getEnchantmentValue() {
        return this.tier.getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return this.tier.getRepairIngredient().test(ingredient) || super.isValidRepairItem(stack, ingredient);
    }
}
