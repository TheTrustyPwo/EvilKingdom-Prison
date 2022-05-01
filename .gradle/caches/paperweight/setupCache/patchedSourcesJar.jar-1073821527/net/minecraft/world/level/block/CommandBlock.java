package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class CommandBlock extends BaseEntityBlock implements GameMasterBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
    private final boolean automatic;

    public CommandBlock(BlockBehaviour.Properties settings, boolean auto) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, Boolean.valueOf(false)));
        this.automatic = auto;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        CommandBlockEntity commandBlockEntity = new CommandBlockEntity(pos, state);
        commandBlockEntity.setAutomatic(this.automatic);
        return commandBlockEntity;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (!world.isClientSide) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CommandBlockEntity) {
                CommandBlockEntity commandBlockEntity = (CommandBlockEntity)blockEntity;
                boolean bl = world.hasNeighborSignal(pos);
                boolean bl2 = commandBlockEntity.isPowered();
                commandBlockEntity.setPowered(bl);
                if (!bl2 && !commandBlockEntity.isAutomatic() && commandBlockEntity.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
                    if (bl) {
                        commandBlockEntity.markConditionMet();
                        world.scheduleTick(pos, this, 1);
                    }

                }
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockEntity) {
            CommandBlockEntity commandBlockEntity = (CommandBlockEntity)blockEntity;
            BaseCommandBlock baseCommandBlock = commandBlockEntity.getCommandBlock();
            boolean bl = !StringUtil.isNullOrEmpty(baseCommandBlock.getCommand());
            CommandBlockEntity.Mode mode = commandBlockEntity.getMode();
            boolean bl2 = commandBlockEntity.wasConditionMet();
            if (mode == CommandBlockEntity.Mode.AUTO) {
                commandBlockEntity.markConditionMet();
                if (bl2) {
                    this.execute(state, world, pos, baseCommandBlock, bl);
                } else if (commandBlockEntity.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }

                if (commandBlockEntity.isPowered() || commandBlockEntity.isAutomatic()) {
                    world.scheduleTick(pos, this, 1);
                }
            } else if (mode == CommandBlockEntity.Mode.REDSTONE) {
                if (bl2) {
                    this.execute(state, world, pos, baseCommandBlock, bl);
                } else if (commandBlockEntity.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
            }

            world.updateNeighbourForOutputSignal(pos, this);
        }

    }

    private void execute(BlockState state, Level world, BlockPos pos, BaseCommandBlock executor, boolean hasCommand) {
        if (hasCommand) {
            executor.performCommand(world);
        } else {
            executor.setSuccessCount(0);
        }

        executeChain(world, pos, state.getValue(FACING));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockEntity && player.canUseGameMasterBlocks()) {
            player.openCommandBlock((CommandBlockEntity)blockEntity);
            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof CommandBlockEntity ? ((CommandBlockEntity)blockEntity).getCommandBlock().getSuccessCount() : 0;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockEntity) {
            CommandBlockEntity commandBlockEntity = (CommandBlockEntity)blockEntity;
            BaseCommandBlock baseCommandBlock = commandBlockEntity.getCommandBlock();
            if (itemStack.hasCustomHoverName()) {
                baseCommandBlock.setName(itemStack.getHoverName());
            }

            if (!world.isClientSide) {
                if (BlockItem.getBlockEntityData(itemStack) == null) {
                    baseCommandBlock.setTrackOutput(world.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
                    commandBlockEntity.setAutomatic(this.automatic);
                }

                if (commandBlockEntity.getMode() == CommandBlockEntity.Mode.SEQUENCE) {
                    boolean bl = world.hasNeighborSignal(pos);
                    commandBlockEntity.setPowered(bl);
                }
            }

        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONDITIONAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    private static void executeChain(Level world, BlockPos pos, Direction facing) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        GameRules gameRules = world.getGameRules();

        int i;
        BlockState blockState;
        for(i = gameRules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH); i-- > 0; facing = blockState.getValue(FACING)) {
            mutableBlockPos.move(facing);
            blockState = world.getBlockState(mutableBlockPos);
            Block block = blockState.getBlock();
            if (!blockState.is(Blocks.CHAIN_COMMAND_BLOCK)) {
                break;
            }

            BlockEntity blockEntity = world.getBlockEntity(mutableBlockPos);
            if (!(blockEntity instanceof CommandBlockEntity)) {
                break;
            }

            CommandBlockEntity commandBlockEntity = (CommandBlockEntity)blockEntity;
            if (commandBlockEntity.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
                break;
            }

            if (commandBlockEntity.isPowered() || commandBlockEntity.isAutomatic()) {
                BaseCommandBlock baseCommandBlock = commandBlockEntity.getCommandBlock();
                if (commandBlockEntity.markConditionMet()) {
                    if (!baseCommandBlock.performCommand(world)) {
                        break;
                    }

                    world.updateNeighbourForOutputSignal(mutableBlockPos, block);
                } else if (commandBlockEntity.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
            }
        }

        if (i <= 0) {
            int j = Math.max(gameRules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);
            LOGGER.warn("Command Block chain tried to execute more than {} steps!", (int)j);
        }

    }
}
