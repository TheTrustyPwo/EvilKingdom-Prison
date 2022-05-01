package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ChorusFlowerBlock extends Block {
    public static final int DEAD_AGE = 5;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
    private final ChorusPlantBlock plant;

    protected ChorusFlowerBlock(ChorusPlantBlock plantBlock, BlockBehaviour.Properties settings) {
        super(settings);
        this.plant = plantBlock;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, Integer.valueOf(0)));
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (!state.canSurvive(world, pos)) {
            world.destroyBlock(pos, true);
        }

    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 5;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        BlockPos blockPos = pos.above();
        if (world.isEmptyBlock(blockPos) && blockPos.getY() < world.getMaxBuildHeight()) {
            int i = state.getValue(AGE);
            if (i < 5) {
                boolean bl = false;
                boolean bl2 = false;
                BlockState blockState = world.getBlockState(pos.below());
                if (blockState.is(Blocks.END_STONE)) {
                    bl = true;
                } else if (blockState.is(this.plant)) {
                    int j = 1;

                    for(int k = 0; k < 4; ++k) {
                        BlockState blockState2 = world.getBlockState(pos.below(j + 1));
                        if (!blockState2.is(this.plant)) {
                            if (blockState2.is(Blocks.END_STONE)) {
                                bl2 = true;
                            }
                            break;
                        }

                        ++j;
                    }

                    if (j < 2 || j <= random.nextInt(bl2 ? 5 : 4)) {
                        bl = true;
                    }
                } else if (blockState.isAir()) {
                    bl = true;
                }

                if (bl && allNeighborsEmpty(world, blockPos, (Direction)null) && world.isEmptyBlock(pos.above(2))) {
                    world.setBlock(pos, this.plant.getStateForPlacement(world, pos), 2);
                    this.placeGrownFlower(world, blockPos, i);
                } else if (i < 4) {
                    int l = random.nextInt(4);
                    if (bl2) {
                        ++l;
                    }

                    boolean bl3 = false;

                    for(int m = 0; m < l; ++m) {
                        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                        BlockPos blockPos2 = pos.relative(direction);
                        if (world.isEmptyBlock(blockPos2) && world.isEmptyBlock(blockPos2.below()) && allNeighborsEmpty(world, blockPos2, direction.getOpposite())) {
                            this.placeGrownFlower(world, blockPos2, i + 1);
                            bl3 = true;
                        }
                    }

                    if (bl3) {
                        world.setBlock(pos, this.plant.getStateForPlacement(world, pos), 2);
                    } else {
                        this.placeDeadFlower(world, pos);
                    }
                } else {
                    this.placeDeadFlower(world, pos);
                }

            }
        }
    }

    private void placeGrownFlower(Level world, BlockPos pos, int age) {
        world.setBlock(pos, this.defaultBlockState().setValue(AGE, Integer.valueOf(age)), 2);
        world.levelEvent(1033, pos, 0);
    }

    private void placeDeadFlower(Level world, BlockPos pos) {
        world.setBlock(pos, this.defaultBlockState().setValue(AGE, Integer.valueOf(5)), 2);
        world.levelEvent(1034, pos, 0);
    }

    private static boolean allNeighborsEmpty(LevelReader world, BlockPos pos, @Nullable Direction exceptDirection) {
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != exceptDirection && !world.isEmptyBlock(pos.relative(direction))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction != Direction.UP && !state.canSurvive(world, pos)) {
            world.scheduleTick(pos, this, 1);
        }

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.below());
        if (!blockState.is(this.plant) && !blockState.is(Blocks.END_STONE)) {
            if (!blockState.isAir()) {
                return false;
            } else {
                boolean bl = false;

                for(Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockState blockState2 = world.getBlockState(pos.relative(direction));
                    if (blockState2.is(this.plant)) {
                        if (bl) {
                            return false;
                        }

                        bl = true;
                    } else if (!blockState2.isAir()) {
                        return false;
                    }
                }

                return bl;
            }
        } else {
            return true;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public static void generatePlant(LevelAccessor world, BlockPos pos, Random random, int size) {
        world.setBlock(pos, ((ChorusPlantBlock)Blocks.CHORUS_PLANT).getStateForPlacement(world, pos), 2);
        growTreeRecursive(world, pos, random, pos, size, 0);
    }

    private static void growTreeRecursive(LevelAccessor world, BlockPos pos, Random random, BlockPos rootPos, int size, int layer) {
        ChorusPlantBlock chorusPlantBlock = (ChorusPlantBlock)Blocks.CHORUS_PLANT;
        int i = random.nextInt(4) + 1;
        if (layer == 0) {
            ++i;
        }

        for(int j = 0; j < i; ++j) {
            BlockPos blockPos = pos.above(j + 1);
            if (!allNeighborsEmpty(world, blockPos, (Direction)null)) {
                return;
            }

            world.setBlock(blockPos, chorusPlantBlock.getStateForPlacement(world, blockPos), 2);
            world.setBlock(blockPos.below(), chorusPlantBlock.getStateForPlacement(world, blockPos.below()), 2);
        }

        boolean bl = false;
        if (layer < 4) {
            int k = random.nextInt(4);
            if (layer == 0) {
                ++k;
            }

            for(int l = 0; l < k; ++l) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos blockPos2 = pos.above(i).relative(direction);
                if (Math.abs(blockPos2.getX() - rootPos.getX()) < size && Math.abs(blockPos2.getZ() - rootPos.getZ()) < size && world.isEmptyBlock(blockPos2) && world.isEmptyBlock(blockPos2.below()) && allNeighborsEmpty(world, blockPos2, direction.getOpposite())) {
                    bl = true;
                    world.setBlock(blockPos2, chorusPlantBlock.getStateForPlacement(world, blockPos2), 2);
                    world.setBlock(blockPos2.relative(direction.getOpposite()), chorusPlantBlock.getStateForPlacement(world, blockPos2.relative(direction.getOpposite())), 2);
                    growTreeRecursive(world, blockPos2, random, rootPos, size, layer + 1);
                }
            }
        }

        if (!bl) {
            world.setBlock(pos.above(i), Blocks.CHORUS_FLOWER.defaultBlockState().setValue(AGE, Integer.valueOf(5)), 2);
        }

    }

    @Override
    public void onProjectileHit(Level world, BlockState state, BlockHitResult hit, Projectile projectile) {
        BlockPos blockPos = hit.getBlockPos();
        if (!world.isClientSide && projectile.mayInteract(world, blockPos) && projectile.getType().is(EntityTypeTags.IMPACT_PROJECTILES)) {
            world.destroyBlock(blockPos, true, projectile);
        }

    }
}
