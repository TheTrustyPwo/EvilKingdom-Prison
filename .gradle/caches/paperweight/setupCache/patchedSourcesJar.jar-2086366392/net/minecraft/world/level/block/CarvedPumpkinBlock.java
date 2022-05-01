package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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

// CraftBukkit start
import org.bukkit.craftbukkit.v1_18_R2.util.BlockStateListPopulator;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
// CraftBukkit end

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
    private static final Predicate<BlockState> PUMPKINS_PREDICATE = (iblockdata) -> {
        return iblockdata != null && (iblockdata.is(Blocks.CARVED_PUMPKIN) || iblockdata.is(Blocks.JACK_O_LANTERN));
    };

    protected CarvedPumpkinBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState) ((BlockState) this.stateDefinition.any()).setValue(CarvedPumpkinBlock.FACING, Direction.NORTH));
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
        BlockPattern.BlockPatternMatch shapedetector_shapedetectorcollection = this.getOrCreateSnowGolemFull().find(world, pos);
        int i;
        Iterator iterator;
        ServerPlayer entityplayer;
        int j;

        BlockStateListPopulator blockList = new BlockStateListPopulator(world); // CraftBukkit - Use BlockStateListPopulator
        if (shapedetector_shapedetectorcollection != null) {
            for (i = 0; i < this.getOrCreateSnowGolemFull().getHeight(); ++i) {
                BlockInWorld shapedetectorblock = shapedetector_shapedetectorcollection.getBlock(0, i, 0);

                blockList.setBlock(shapedetectorblock.getPos(), Blocks.AIR.defaultBlockState(), 2); // CraftBukkit
                // world.levelEvent(2001, shapedetectorblock.getPos(), Block.getId(shapedetectorblock.getState())); // CraftBukkit
            }

            SnowGolem entitysnowman = (SnowGolem) EntityType.SNOW_GOLEM.create(world);
            BlockPos blockposition1 = shapedetector_shapedetectorcollection.getBlock(0, 2, 0).getPos();

            entitysnowman.moveTo((double) blockposition1.getX() + 0.5D, (double) blockposition1.getY() + 0.05D, (double) blockposition1.getZ() + 0.5D, 0.0F, 0.0F);
            // CraftBukkit start
            if (!world.addFreshEntity(entitysnowman, SpawnReason.BUILD_SNOWMAN)) {
                return;
            }
            for (BlockPos pos1 : blockList.getBlocks()) {
                world.levelEvent(2001, pos1, Block.getId(world.getBlockState(pos1)));
            }
            blockList.updateList();
            // CraftBukkit end
            iterator = world.getEntitiesOfClass(ServerPlayer.class, entitysnowman.getBoundingBox().inflate(5.0D)).iterator();

            while (iterator.hasNext()) {
                entityplayer = (ServerPlayer) iterator.next();
                CriteriaTriggers.SUMMONED_ENTITY.trigger(entityplayer, (Entity) entitysnowman);
            }

            for (j = 0; j < this.getOrCreateSnowGolemFull().getHeight(); ++j) {
                BlockInWorld shapedetectorblock1 = shapedetector_shapedetectorcollection.getBlock(0, j, 0);

                world.blockUpdated(shapedetectorblock1.getPos(), Blocks.AIR);
            }
        } else {
            shapedetector_shapedetectorcollection = this.getOrCreateIronGolemFull().find(world, pos);
            if (shapedetector_shapedetectorcollection != null) {
                for (i = 0; i < this.getOrCreateIronGolemFull().getWidth(); ++i) {
                    for (int k = 0; k < this.getOrCreateIronGolemFull().getHeight(); ++k) {
                        BlockInWorld shapedetectorblock2 = shapedetector_shapedetectorcollection.getBlock(i, k, 0);

                        blockList.setBlock(shapedetectorblock2.getPos(), Blocks.AIR.defaultBlockState(), 2); // CraftBukkit
                        // world.levelEvent(2001, shapedetectorblock2.getPos(), Block.getId(shapedetectorblock2.getState())); // CraftBukkit
                    }
                }

                BlockPos blockposition2 = shapedetector_shapedetectorcollection.getBlock(1, 2, 0).getPos();
                IronGolem entityirongolem = (IronGolem) EntityType.IRON_GOLEM.create(world);

                entityirongolem.setPlayerCreated(true);
                entityirongolem.moveTo((double) blockposition2.getX() + 0.5D, (double) blockposition2.getY() + 0.05D, (double) blockposition2.getZ() + 0.5D, 0.0F, 0.0F);
                // CraftBukkit start
                if (!world.addFreshEntity(entityirongolem, SpawnReason.BUILD_IRONGOLEM)) {
                    return;
                }
                for (BlockPos pos2 : blockList.getBlocks()) {
                    world.levelEvent(2001, pos2, Block.getId(world.getBlockState(pos2)));
                }
                blockList.updateList();
                // CraftBukkit end
                iterator = world.getEntitiesOfClass(ServerPlayer.class, entityirongolem.getBoundingBox().inflate(5.0D)).iterator();

                while (iterator.hasNext()) {
                    entityplayer = (ServerPlayer) iterator.next();
                    CriteriaTriggers.SUMMONED_ENTITY.trigger(entityplayer, (Entity) entityirongolem);
                }

                for (j = 0; j < this.getOrCreateIronGolemFull().getWidth(); ++j) {
                    for (int l = 0; l < this.getOrCreateIronGolemFull().getHeight(); ++l) {
                        BlockInWorld shapedetectorblock3 = shapedetector_shapedetectorcollection.getBlock(j, l, 0);

                        world.blockUpdated(shapedetectorblock3.getPos(), Blocks.AIR);
                    }
                }
            }
        }

    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return (BlockState) this.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CarvedPumpkinBlock.FACING);
    }

    private BlockPattern getOrCreateSnowGolemBase() {
        if (this.snowGolemBase == null) {
            this.snowGolemBase = BlockPatternBuilder.start().aisle(" ", "#", "#").where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))).build();
        }

        return this.snowGolemBase;
    }

    private BlockPattern getOrCreateSnowGolemFull() {
        if (this.snowGolemFull == null) {
            this.snowGolemFull = BlockPatternBuilder.start().aisle("^", "#", "#").where('^', BlockInWorld.hasState(CarvedPumpkinBlock.PUMPKINS_PREDICATE)).where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))).build();
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
            this.ironGolemFull = BlockPatternBuilder.start().aisle("~^~", "###", "~#~").where('^', BlockInWorld.hasState(CarvedPumpkinBlock.PUMPKINS_PREDICATE)).where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK))).where('~', BlockInWorld.hasState(BlockMaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return this.ironGolemFull;
    }
}
