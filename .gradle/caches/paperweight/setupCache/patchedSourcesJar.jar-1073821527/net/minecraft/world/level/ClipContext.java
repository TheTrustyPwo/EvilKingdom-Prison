package net.minecraft.world.level;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClipContext {
    private final Vec3 from;
    private final Vec3 to;
    private final ClipContext.Block block;
    private final ClipContext.Fluid fluid;
    private final CollisionContext collisionContext;

    public ClipContext(Vec3 start, Vec3 end, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity) {
        this.from = start;
        this.to = end;
        this.block = shapeType;
        this.fluid = fluidHandling;
        this.collisionContext = CollisionContext.of(entity);
    }

    public Vec3 getTo() {
        return this.to;
    }

    public Vec3 getFrom() {
        return this.from;
    }

    public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
        return this.block.get(state, world, pos, this.collisionContext);
    }

    public VoxelShape getFluidShape(FluidState state, BlockGetter world, BlockPos pos) {
        return this.fluid.canPick(state) ? state.getShape(world, pos) : Shapes.empty();
    }

    public static enum Block implements ClipContext.ShapeGetter {
        COLLIDER(BlockBehaviour.BlockStateBase::getCollisionShape),
        OUTLINE(BlockBehaviour.BlockStateBase::getShape),
        VISUAL(BlockBehaviour.BlockStateBase::getVisualShape),
        FALLDAMAGE_RESETTING((state, world, pos, context) -> {
            return state.is(BlockTags.FALL_DAMAGE_RESETTING) ? Shapes.block() : Shapes.empty();
        });

        private final ClipContext.ShapeGetter shapeGetter;

        private Block(ClipContext.ShapeGetter provider) {
            this.shapeGetter = provider;
        }

        @Override
        public VoxelShape get(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
            return this.shapeGetter.get(state, world, pos, context);
        }
    }

    public static enum Fluid {
        NONE((state) -> {
            return false;
        }),
        SOURCE_ONLY(FluidState::isSource),
        ANY((state) -> {
            return !state.isEmpty();
        }),
        WATER((state) -> {
            return state.is(FluidTags.WATER);
        });

        private final Predicate<FluidState> canPick;

        private Fluid(Predicate<FluidState> predicate) {
            this.canPick = predicate;
        }

        public boolean canPick(FluidState state) {
            return this.canPick.test(state);
        }
    }

    public interface ShapeGetter {
        VoxelShape get(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context);
    }
}
