package net.minecraft.world.phys.shapes;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;

public class EntityCollisionContext implements CollisionContext {
    protected static final CollisionContext EMPTY = new EntityCollisionContext(false, -Double.MAX_VALUE, ItemStack.EMPTY, (fluidState) -> {
        return false;
    }, (Entity)null) {
        @Override
        public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
            return defaultValue;
        }
    };
    private final boolean descending;
    private final double entityBottom;
    private final ItemStack heldItem;
    private final Predicate<FluidState> canStandOnFluid;
    @Nullable
    private final Entity entity;

    protected EntityCollisionContext(boolean descending, double minY, ItemStack heldItem, Predicate<FluidState> walkOnFluidPredicate, @Nullable Entity entity) {
        this.descending = descending;
        this.entityBottom = minY;
        this.heldItem = heldItem;
        this.canStandOnFluid = walkOnFluidPredicate;
        this.entity = entity;
    }

    /** @deprecated */
    @Deprecated
    protected EntityCollisionContext(Entity entity) {
        this(entity.isDescending(), entity.getY(), entity instanceof LivingEntity ? ((LivingEntity)entity).getMainHandItem() : ItemStack.EMPTY, entity instanceof LivingEntity ? ((LivingEntity)entity)::canStandOnFluid : (fluidState) -> {
            return false;
        }, entity);
    }

    @Override
    public boolean isHoldingItem(Item item) {
        return this.heldItem.is(item);
    }

    @Override
    public boolean canStandOnFluid(FluidState state, FluidState fluidState) {
        return this.canStandOnFluid.test(fluidState) && !state.getType().isSame(fluidState.getType());
    }

    @Override
    public boolean isDescending() {
        return this.descending;
    }

    @Override
    public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
        return this.entityBottom > (double)pos.getY() + shape.max(Direction.Axis.Y) - (double)1.0E-5F;
    }

    @Nullable
    public Entity getEntity() {
        return this.entity;
    }
}
