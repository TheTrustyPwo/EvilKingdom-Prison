package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockMaterialPredicate;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.material.Material;

public class WitherSkullBlock extends SkullBlock {
    @Nullable
    private static BlockPattern witherPatternFull;
    @Nullable
    private static BlockPattern witherPatternBase;

    protected WitherSkullBlock(BlockBehaviour.Properties settings) {
        super(SkullBlock.Types.WITHER_SKELETON, settings);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SkullBlockEntity) {
            checkSpawn(world, pos, (SkullBlockEntity)blockEntity);
        }

    }

    public static void checkSpawn(Level world, BlockPos pos, SkullBlockEntity blockEntity) {
        if (!world.isClientSide) {
            BlockState blockState = blockEntity.getBlockState();
            boolean bl = blockState.is(Blocks.WITHER_SKELETON_SKULL) || blockState.is(Blocks.WITHER_SKELETON_WALL_SKULL);
            if (bl && pos.getY() >= world.getMinBuildHeight() && world.getDifficulty() != Difficulty.PEACEFUL) {
                BlockPattern blockPattern = getOrCreateWitherFull();
                BlockPattern.BlockPatternMatch blockPatternMatch = blockPattern.find(world, pos);
                if (blockPatternMatch != null) {
                    for(int i = 0; i < blockPattern.getWidth(); ++i) {
                        for(int j = 0; j < blockPattern.getHeight(); ++j) {
                            BlockInWorld blockInWorld = blockPatternMatch.getBlock(i, j, 0);
                            world.setBlock(blockInWorld.getPos(), Blocks.AIR.defaultBlockState(), 2);
                            world.levelEvent(2001, blockInWorld.getPos(), Block.getId(blockInWorld.getState()));
                        }
                    }

                    WitherBoss witherBoss = EntityType.WITHER.create(world);
                    BlockPos blockPos = blockPatternMatch.getBlock(1, 2, 0).getPos();
                    witherBoss.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.55D, (double)blockPos.getZ() + 0.5D, blockPatternMatch.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F, 0.0F);
                    witherBoss.yBodyRot = blockPatternMatch.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F;
                    witherBoss.makeInvulnerable();

                    for(ServerPlayer serverPlayer : world.getEntitiesOfClass(ServerPlayer.class, witherBoss.getBoundingBox().inflate(50.0D))) {
                        CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, witherBoss);
                    }

                    world.addFreshEntity(witherBoss);

                    for(int k = 0; k < blockPattern.getWidth(); ++k) {
                        for(int l = 0; l < blockPattern.getHeight(); ++l) {
                            world.blockUpdated(blockPatternMatch.getBlock(k, l, 0).getPos(), Blocks.AIR);
                        }
                    }

                }
            }
        }
    }

    public static boolean canSpawnMob(Level world, BlockPos pos, ItemStack stack) {
        if (stack.is(Items.WITHER_SKELETON_SKULL) && pos.getY() >= world.getMinBuildHeight() + 2 && world.getDifficulty() != Difficulty.PEACEFUL && !world.isClientSide) {
            return getOrCreateWitherBase().find(world, pos) != null;
        } else {
            return false;
        }
    }

    private static BlockPattern getOrCreateWitherFull() {
        if (witherPatternFull == null) {
            witherPatternFull = BlockPatternBuilder.start().aisle("^^^", "###", "~#~").where('#', (pos) -> {
                return pos.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS);
            }).where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL).or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL)))).where('~', BlockInWorld.hasState(BlockMaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return witherPatternFull;
    }

    private static BlockPattern getOrCreateWitherBase() {
        if (witherPatternBase == null) {
            witherPatternBase = BlockPatternBuilder.start().aisle("   ", "###", "~#~").where('#', (pos) -> {
                return pos.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS);
            }).where('~', BlockInWorld.hasState(BlockMaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return witherPatternBase;
    }
}
