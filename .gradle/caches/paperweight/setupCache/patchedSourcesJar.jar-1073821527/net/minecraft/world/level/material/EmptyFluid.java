package net.minecraft.world.level.material;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EmptyFluid extends Fluid {
    @Override
    public Item getBucket() {
        return Items.AIR;
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter world, BlockPos pos, Fluid fluid, Direction direction) {
        return true;
    }

    @Override
    public Vec3 getFlow(BlockGetter world, BlockPos pos, FluidState state) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader world) {
        return 0;
    }

    @Override
    protected boolean isEmpty() {
        return true;
    }

    @Override
    protected float getExplosionResistance() {
        return 0.0F;
    }

    @Override
    public float getHeight(FluidState state, BlockGetter world, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return 0.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState state) {
        return false;
    }

    @Override
    public int getAmount(FluidState state) {
        return 0;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter world, BlockPos pos) {
        return Shapes.empty();
    }
}
