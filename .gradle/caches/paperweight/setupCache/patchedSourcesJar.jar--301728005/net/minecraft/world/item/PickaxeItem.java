package net.minecraft.world.item;

import net.minecraft.tags.BlockTags;

public class PickaxeItem extends DiggerItem {
    protected PickaxeItem(Tier material, int attackDamage, float attackSpeed, Item.Properties settings) {
        super((float)attackDamage, attackSpeed, material, BlockTags.MINEABLE_WITH_PICKAXE, settings);
    }
}
