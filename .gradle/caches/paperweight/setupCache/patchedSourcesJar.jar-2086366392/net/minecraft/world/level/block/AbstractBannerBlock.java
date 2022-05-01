package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractBannerBlock extends BaseEntityBlock {
    private final DyeColor color;

    protected AbstractBannerBlock(DyeColor color, BlockBehaviour.Properties settings) {
        super(settings);
        this.color = color;
    }

    @Override
    public boolean isPossibleToRespawnInThis() {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BannerBlockEntity(pos, state, this.color);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClientSide) {
            world.getBlockEntity(pos, BlockEntityType.BANNER).ifPresent((blockEntity) -> {
                blockEntity.fromItem(itemStack);
            });
        } else if (itemStack.hasCustomHoverName()) {
            world.getBlockEntity(pos, BlockEntityType.BANNER).ifPresent((blockEntity) -> {
                blockEntity.setCustomName(itemStack.getHoverName());
            });
        }

    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof BannerBlockEntity ? ((BannerBlockEntity)blockEntity).getItem() : super.getCloneItemStack(world, pos, state);
    }

    public DyeColor getColor() {
        return this.color;
    }
}
