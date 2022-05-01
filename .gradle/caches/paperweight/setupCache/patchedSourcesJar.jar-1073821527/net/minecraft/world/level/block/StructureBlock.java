package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.phys.BlockHitResult;

public class StructureBlock extends BaseEntityBlock implements GameMasterBlock {
    public static final EnumProperty<StructureMode> MODE = BlockStateProperties.STRUCTUREBLOCK_MODE;

    protected StructureBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, StructureMode.LOAD));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructureBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof StructureBlockEntity) {
            return ((StructureBlockEntity)blockEntity).usedBy(player) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClientSide) {
            if (placer != null) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof StructureBlockEntity) {
                    ((StructureBlockEntity)blockEntity).createdBy(placer);
                }
            }

        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (world instanceof ServerLevel) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StructureBlockEntity) {
                StructureBlockEntity structureBlockEntity = (StructureBlockEntity)blockEntity;
                boolean bl = world.hasNeighborSignal(pos);
                boolean bl2 = structureBlockEntity.isPowered();
                if (bl && !bl2) {
                    structureBlockEntity.setPowered(true);
                    this.trigger((ServerLevel)world, structureBlockEntity);
                } else if (!bl && bl2) {
                    structureBlockEntity.setPowered(false);
                }

            }
        }
    }

    private void trigger(ServerLevel world, StructureBlockEntity blockEntity) {
        switch(blockEntity.getMode()) {
        case SAVE:
            blockEntity.saveStructure(false);
            break;
        case LOAD:
            blockEntity.loadStructure(world, false);
            break;
        case CORNER:
            blockEntity.unloadStructure();
        case DATA:
        }

    }
}
