package net.minecraft.world.level.block;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.Wearable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockMaterialPredicate;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;

public class CarvedPumpkinBlock extends HorizontalDirectionalBlock implements Wearable {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    @Nullable
    private BlockPattern snowGolemBase;
    @Nullable
    private BlockPattern snowGolemFull;
    @Nullable
    private BlockPattern ironGolemBase;
    @Nullable
    private BlockPattern ironGolemFull;
    private static final Predicate<BlockState> PUMPKINS_PREDICATE = (state) -> {
        return state != null && (state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN));
    };

    protected CarvedPumpkinBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.trySpawnGolem(world, pos);
        }
    }

    public boolean canSpawnGolem(LevelReader world, BlockPos pos) {
        return this.getOrCreateSnowGolemBase().find(world, pos) != null || this.getOrCreateIronGolemBase().find(world, pos) != null;
    }

    private void trySpawnGolem(Level world, BlockPos pos) {
        BlockPattern.BlockPatternMatch blockPatternMatch = this.getOrCreateSnowGolemFull().find(world, pos);
        if (blockPatternMatch != null) {
            for(int i = 0; i < this.getOrCreateSnowGolemFull().getHeight(); ++i) {
                BlockInWorld blockInWorld = blockPatternMatch.getBlock(0, i, 0);
                world.setBlock(blockInWorld.getPos(), Blocks.AIR.defaultBlockState(), 2);
                world.levelEvent(2001, blockInWorld.getPos(), Block.getId(blockInWorld.getState()));
            }

            SnowGolem snowGolem = EntityType.SNOW_GOLEM.create(world);
            BlockPos blockPos = blockPatternMatch.getBlock(0, 2, 0).getPos();
            snowGolem.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.05D, (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
            world.addFreshEntity(snowGolem);

            for(ServerPlayer serverPlayer : world.getEntitiesOfClass(ServerPlayer.class, snowGolem.getBoundingBox().inflate(5.0D))) {
                CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, snowGolem);
            }

            for(int j = 0; j < this.getOrCreateSnowGolemFull().getHeight(); ++j) {
                BlockInWorld blockInWorld2 = blockPatternMatch.getBlock(0, j, 0);
                world.blockUpdated(blockInWorld2.getPos(), Blocks.AIR);
            }
        } else {
            blockPatternMatch = this.getOrCreateIronGolemFull().find(world, pos);
            if (blockPatternMatch != null) {
                for(int k = 0; k < this.getOrCreateIronGolemFull().getWidth(); ++k) {
                    for(int l = 0; l < this.getOrCreateIronGolemFull().getHeight(); ++l) {
                        BlockInWorld blockInWorld3 = blockPatternMatch.getBlock(k, l, 0);
                        world.setBlock(blockInWorld3.getPos(), Blocks.AIR.defaultBlockState(), 2);
                        world.levelEvent(2001, blockInWorld3.getPos(), Block.getId(blockInWorld3.getState()));
                    }
                }

                BlockPos blockPos2 = blockPatternMatch.getBlock(1, 2, 0).getPos();
                IronGolem ironGolem = EntityType.IRON_GOLEM.create(world);
                ironGolem.setPlayerCreated(true);
                ironGolem.moveTo((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.05D, (double)blockPos2.getZ() + 0.5D, 0.0F, 0.0F);
                world.addFreshEntity(ironGolem);

                for(ServerPlayer serverPlayer2 : world.getEntitiesOfClass(ServerPlayer.class, ironGolem.getBoundingBox().inflate(5.0D))) {
                    CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer2, ironGolem);
                }

                for(int m = 0; m < this.getOrCreateIronGolemFull().getWidth(); ++m) {
                    for(int n = 0; n < this.getOrCreateIronGolemFull().getHeight(); ++n) {
                        BlockInWorld blockInWorld4 = blockPatternMatch.getBlock(m, n, 0);
                        world.blockUpdated(blockInWorld4.getPos(), Blocks.AIR);
                    }
                }
            }
        }

    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private BlockPattern getOrCreateSnowGolemBase() {
        if (this.snowGolemBase == null) {
            this.snowGolemBase = BlockPatternBuilder.start().aisle(" ", "#", "#").where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))).build();
        }

        return this.snowGolemBase;
    }

    private BlockPattern getOrCreateSnowGolemFull() {
        if (this.snowGolemFull == null) {
            this.snowGolemFull = BlockPatternBuilder.start().aisle("^", "#", "#").where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE)).where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))).build();
        }

        return this.snowGolemFull;
    }

    private BlockPattern getOrCreateIronGolemBase() {
        if (this.ironGolemBase == null) {
            this.ironGolemBase = BlockPatternBuilder.start().aisle("~ ~", "###", "~#~").where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK))).where('~', BlockInWorld.hasState(BlockMaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return this.ironGolemBase;
    }

    private BlockPattern getOrCreateIronGolemFull() {
        if (this.ironGolemFull == null) {
            this.ironGolemFull = BlockPatternBuilder.start().aisle("~^~", "###", "~#~").where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE)).where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK))).where('~', BlockInWorld.hasState(BlockMaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return this.ironGolemFull;
    }
}
