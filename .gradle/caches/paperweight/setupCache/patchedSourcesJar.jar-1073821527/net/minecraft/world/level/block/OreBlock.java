package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class OreBlock extends Block {
    private final UniformInt xpRange;

    public OreBlock(BlockBehaviour.Properties settings) {
        this(settings, UniformInt.of(0, 0));
    }

    public OreBlock(BlockBehaviour.Properties settings, UniformInt experienceDropped) {
        super(settings);
        this.xpRange = experienceDropped;
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack stack) {
        super.spawnAfterBreak(state, world, pos, stack);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack) == 0) {
            int i = this.xpRange.sample(world.random);
            if (i > 0) {
                this.popExperience(world, pos, i);
            }
        }

    }
}
