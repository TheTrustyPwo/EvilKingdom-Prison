package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class InteractWithDoor extends Behavior<LivingEntity> {
    private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
    private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = 2.0D;
    private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = 2.0D;
    @Nullable
    private Node lastCheckedNode;
    private int remainingCooldown;

    public InteractWithDoor() {
        super(ImmutableMap.of(MemoryModuleType.PATH, MemoryStatus.VALUE_PRESENT, MemoryModuleType.DOORS_TO_CLOSE, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        Path path = entity.getBrain().getMemory(MemoryModuleType.PATH).get();
        if (!path.notStarted() && !path.isDone()) {
            if (!Objects.equals(this.lastCheckedNode, path.getNextNode())) {
                this.remainingCooldown = 20;
                return true;
            } else {
                if (this.remainingCooldown > 0) {
                    --this.remainingCooldown;
                }

                return this.remainingCooldown == 0;
            }
        } else {
            return false;
        }
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        Path path = entity.getBrain().getMemory(MemoryModuleType.PATH).get();
        this.lastCheckedNode = path.getNextNode();
        Node node = path.getPreviousNode();
        Node node2 = path.getNextNode();
        BlockPos blockPos = node.asBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.is(BlockTags.WOODEN_DOORS, (state) -> {
            return state.getBlock() instanceof DoorBlock;
        })) {
            DoorBlock doorBlock = (DoorBlock)blockState.getBlock();
            if (!doorBlock.isOpen(blockState)) {
                doorBlock.setOpen(entity, world, blockState, blockPos, true);
            }

            this.rememberDoorToClose(world, entity, blockPos);
        }

        BlockPos blockPos2 = node2.asBlockPos();
        BlockState blockState2 = world.getBlockState(blockPos2);
        if (blockState2.is(BlockTags.WOODEN_DOORS, (state) -> {
            return state.getBlock() instanceof DoorBlock;
        })) {
            DoorBlock doorBlock2 = (DoorBlock)blockState2.getBlock();
            if (!doorBlock2.isOpen(blockState2)) {
                doorBlock2.setOpen(entity, world, blockState2, blockPos2, true);
                this.rememberDoorToClose(world, entity, blockPos2);
            }
        }

        closeDoorsThatIHaveOpenedOrPassedThrough(world, entity, node, node2);
    }

    public static void closeDoorsThatIHaveOpenedOrPassedThrough(ServerLevel world, LivingEntity entity, @Nullable Node lastNode, @Nullable Node currentNode) {
        Brain<?> brain = entity.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.DOORS_TO_CLOSE)) {
            Iterator<GlobalPos> iterator = brain.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get().iterator();

            while(iterator.hasNext()) {
                GlobalPos globalPos = iterator.next();
                BlockPos blockPos = globalPos.pos();
                if ((lastNode == null || !lastNode.asBlockPos().equals(blockPos)) && (currentNode == null || !currentNode.asBlockPos().equals(blockPos))) {
                    if (isDoorTooFarAway(world, entity, globalPos)) {
                        iterator.remove();
                    } else {
                        BlockState blockState = world.getBlockState(blockPos);
                        if (!blockState.is(BlockTags.WOODEN_DOORS, (state) -> {
                            return state.getBlock() instanceof DoorBlock;
                        })) {
                            iterator.remove();
                        } else {
                            DoorBlock doorBlock = (DoorBlock)blockState.getBlock();
                            if (!doorBlock.isOpen(blockState)) {
                                iterator.remove();
                            } else if (areOtherMobsComingThroughDoor(world, entity, blockPos)) {
                                iterator.remove();
                            } else {
                                doorBlock.setOpen(entity, world, blockState, blockPos, false);
                                iterator.remove();
                            }
                        }
                    }
                }
            }
        }

    }

    private static boolean areOtherMobsComingThroughDoor(ServerLevel world, LivingEntity entity, BlockPos pos) {
        Brain<?> brain = entity.getBrain();
        return !brain.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES) ? false : brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get().stream().filter((livingEntity2) -> {
            return livingEntity2.getType() == entity.getType();
        }).filter((livingEntity) -> {
            return pos.closerToCenterThan(livingEntity.position(), 2.0D);
        }).anyMatch((livingEntity) -> {
            return isMobComingThroughDoor(world, livingEntity, pos);
        });
    }

    private static boolean isMobComingThroughDoor(ServerLevel world, LivingEntity entity, BlockPos pos) {
        if (!entity.getBrain().hasMemoryValue(MemoryModuleType.PATH)) {
            return false;
        } else {
            Path path = entity.getBrain().getMemory(MemoryModuleType.PATH).get();
            if (path.isDone()) {
                return false;
            } else {
                Node node = path.getPreviousNode();
                if (node == null) {
                    return false;
                } else {
                    Node node2 = path.getNextNode();
                    return pos.equals(node.asBlockPos()) || pos.equals(node2.asBlockPos());
                }
            }
        }
    }

    private static boolean isDoorTooFarAway(ServerLevel world, LivingEntity entity, GlobalPos doorPos) {
        return doorPos.dimension() != world.dimension() || !doorPos.pos().closerToCenterThan(entity.position(), 2.0D);
    }

    private void rememberDoorToClose(ServerLevel world, LivingEntity entity, BlockPos pos) {
        Brain<?> brain = entity.getBrain();
        GlobalPos globalPos = GlobalPos.of(world.dimension(), pos);
        if (brain.getMemory(MemoryModuleType.DOORS_TO_CLOSE).isPresent()) {
            brain.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get().add(globalPos);
        } else {
            brain.setMemory(MemoryModuleType.DOORS_TO_CLOSE, Sets.newHashSet(globalPos));
        }

    }
}
