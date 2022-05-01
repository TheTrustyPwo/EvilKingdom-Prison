package net.minecraft.world.item;

import net.minecraft.world.entity.EquipmentSlot;

public class DyeableArmorItem extends ArmorItem implements DyeableLeatherItem {
    public DyeableArmorItem(ArmorMaterial material, EquipmentSlot slot, Item.Properties settings) {
        super(material, slot, settings);
    }
}
