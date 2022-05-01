package net.minecraft.world.item;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BoneMealItem extends Item {
    public static final int GRASS_SPREAD_WIDTH = 3;
    public static final int GRASS_SPREAD_HEIGHT = 1;
    public static final int GRASS_COUNT_MULTIPLIER = 3;

    public BoneMealItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockPos blockPos2 = blockPos.relative(context.getClickedFace());
        if (growCrop(context.getItemInHand(), level, blockPos)) {
            if (!level.isClientSide) {
                level.levelEvent(1505, blockPos, 0);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            BlockState blockState = level.getBlockState(blockPos);
            boolean bl = blockState.isFaceSturdy(level, blockPos, context.getClickedFace());
            if (bl && growWaterPlant(context.getItemInHand(), level, blockPos2, context.getClickedFace())) {
                if (!level.isClientSide) {
                    level.levelEvent(1505, blockPos2, 0);
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    public static boolean growCrop(ItemStack stack, Level world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof BonemealableBlock) {
            BonemealableBlock bonemealableBlock = (BonemealableBlock)blockState.getBlock();
            if (bonemealableBlock.isValidBonemealTarget(world, pos, blockState, world.isClientSide)) {
                if (world instanceof ServerLevel) {
                    if (bonemealableBlock.isBonemealSuccess(world, world.random, pos, blockState)) {
                        bonemealableBlock.performBonemeal((ServerLevel)world, world.random, pos, blockState);
                    }

                    stack.shrink(1);
                }

                return true;
            }
        }

        return false;
    }

    public static boolean growWaterPlant(ItemStack stack, Level world, BlockPos blockPos, @Nullable Direction facing) {
        if (world.getBlockState(blockPos).is(Blocks.WATER) && world.getFluidState(blockPos).getAmount() == 8) {
            if (!(world instanceof ServerLevel)) {
                return true;
            } else {
                Random random = world.getRandom();

                label78:
                for(int i = 0; i < 128; ++i) {
                    BlockPos blockPos2 = blockPos;
                    BlockState blockState = Blocks.SEAGRASS.defaultBlockState();

                    for(int j = 0; j < i / 16; ++j) {
                        blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                        if (world.getBlockState(blockPos2).isCollisionShapeFullBlock(world, blockPos2)) {
                            continue label78;
                        }
                    }

                    Holder<Biome> holder = world.getBiome(blockPos2);
                    if (holder.is(Biomes.WARM_OCEAN)) {
                        if (i == 0 && facing != null && facing.getAxis().isHorizontal()) {
                            blockState = Registry.BLOCK.getTag(BlockTags.WALL_CORALS).flatMap((blocks) -> {
                                return blocks.getRandomElement(world.random);
                            }).map((blockEntry) -> {
                                return blockEntry.value().defaultBlockState();
                            }).orElse(blockState);
                            if (blockState.hasProperty(BaseCoralWallFanBlock.FACING)) {
                                blockState = blockState.setValue(BaseCoralWallFanBlock.FACING, facing);
                            }
                        } else if (random.nextInt(4) == 0) {
                            blockState = Registry.BLOCK.getTag(BlockTags.UNDERWATER_BONEMEALS).flatMap((blocks) -> {
                                return blocks.getRandomElement(world.random);
                            }).map((blockEntry) -> {
                                return blockEntry.value().defaultBlockState();
                            }).orElse(blockState);
                        }
                    }

                    if (blockState.is(BlockTags.WALL_CORALS, (state) -> {
                        return state.hasProperty(BaseCoralWallFanBlock.FACING);
                    })) {
                        for(int k = 0; !blockState.canSurvive(world, blockPos2) && k < 4; ++k) {
                            blockState = blockState.setValue(BaseCoralWallFanBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
                        }
                    }

                    if (blockState.canSurvive(world, blockPos2)) {
                        BlockState blockState2 = world.getBlockState(blockPos2);
                        if (blockState2.is(Blocks.WATER) && world.getFluidState(blockPos2).getAmount() == 8) {
                            world.setBlock(blockPos2, blockState, 3);
                        } else if (blockState2.is(Blocks.SEAGRASS) && random.nextInt(10) == 0) {
                            ((BonemealableBlock)Blocks.SEAGRASS).performBonemeal((ServerLevel)world, random, blockPos2, blockState2);
                        }
                    }
                }

                stack.shrink(1);
                return true;
            }
        } else {
            return false;
        }
    }

    public static void addGrowthParticles(LevelAccessor world, BlockPos pos, int count) {
        if (count == 0) {
            count = 15;
        }

        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isAir()) {
            double d = 0.5D;
            double e;
            if (blockState.is(Blocks.WATER)) {
                count *= 3;
                e = 1.0D;
                d = 3.0D;
            } else if (blockState.isSolidRender(world, pos)) {
                pos = pos.above();
                count *= 3;
                d = 3.0D;
                e = 1.0D;
            } else {
                e = blockState.getShape(world, pos).max(Direction.Axis.Y);
            }

            world.addParticle(ParticleTypes.HAPPY_VILLAGER, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
            Random random = world.getRandom();

            for(int i = 0; i < count; ++i) {
                double h = random.nextGaussian() * 0.02D;
                double j = random.nextGaussian() * 0.02D;
                double k = random.nextGaussian() * 0.02D;
                double l = 0.5D - d;
                double m = (double)pos.getX() + l + random.nextDouble() * d * 2.0D;
                double n = (double)pos.getY() + random.nextDouble() * e;
                double o = (double)pos.getZ() + l + random.nextDouble() * d * 2.0D;
                if (!world.getBlockState((new BlockPos(m, n, o)).below()).isAir()) {
                    world.addParticle(ParticleTypes.HAPPY_VILLAGER, m, n, o, h, j, k);
                }
            }

        }
    }
}
