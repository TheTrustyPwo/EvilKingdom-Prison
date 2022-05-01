package net.minecraft.world.item.context;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BlockPlaceContext extends UseOnContext {
    private final BlockPos relativePos;
    protected boolean replaceClicked = true;

    public BlockPlaceContext(Player player, InteractionHand hand, ItemStack stack, BlockHitResult hitResult) {
        this(player.level, player, hand, stack, hitResult);
    }

    public BlockPlaceContext(UseOnContext context) {
        this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), context.getHitResult());
    }

    public BlockPlaceContext(Level world, @Nullable Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        super(world, player, hand, stack, hit);
        this.relativePos = hit.getBlockPos().relative(hit.getDirection());
        this.replaceClicked = world.getBlockState(hit.getBlockPos()).canBeReplaced(this);
    }

    public static BlockPlaceContext at(BlockPlaceContext context, BlockPos pos, Direction side) {
        return new BlockPlaceContext(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), new BlockHitResult(new Vec3((double)pos.getX() + 0.5D + (double)side.getStepX() * 0.5D, (double)pos.getY() + 0.5D + (double)side.getStepY() * 0.5D, (double)pos.getZ() + 0.5D + (double)side.getStepZ() * 0.5D), side, pos, false));
    }

    @Override
    public BlockPos getClickedPos() {
        return this.replaceClicked ? super.getClickedPos() : this.relativePos;
    }

    public boolean canPlace() {
        return this.replaceClicked || this.getLevel().getBlockState(this.getClickedPos()).canBeReplaced(this);
    }

    public boolean replacingClickedOnBlock() {
        return this.replaceClicked;
    }

    public Direction getNearestLookingDirection() {
        return Direction.orderedByNearest(this.getPlayer())[0];
    }

    public Direction getNearestLookingVerticalDirection() {
        return Direction.getFacingAxis(this.getPlayer(), Direction.Axis.Y);
    }

    public Direction[] getNearestLookingDirections() {
        Direction[] directions = Direction.orderedByNearest(this.getPlayer());
        if (this.replaceClicked) {
            return directions;
        } else {
            Direction direction = this.getClickedFace();

            int i;
            for(i = 0; i < directions.length && directions[i] != direction.getOpposite(); ++i) {
            }

            if (i > 0) {
                System.arraycopy(directions, 0, directions, 1, i);
                directions[0] = direction.getOpposite();
            }

            return directions;
        }
    }
}
