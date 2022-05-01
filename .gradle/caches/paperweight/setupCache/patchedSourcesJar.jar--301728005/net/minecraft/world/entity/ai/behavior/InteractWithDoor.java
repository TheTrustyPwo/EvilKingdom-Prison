package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        Path pathentity = (Path) entity.getBrain().getMemory(MemoryModuleType.PATH).get();

        if (!pathentity.notStarted() && !pathentity.isDone()) {
            if (!Objects.equals(this.lastCheckedNode, pathentity.getNextNode())) {
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
        Path pathentity = (Path) entity.getBrain().getMemory(MemoryModuleType.PATH).get();

        this.lastCheckedNode = pathentity.getNextNode();
        Node pathpoint = pathentity.getPreviousNode();
        Node pathpoint1 = pathentity.getNextNode();
        BlockPos blockposition = pathpoint.asBlockPos();
        BlockState iblockdata = world.getBlockState(blockposition);

        if (iblockdata.is(BlockTags.WOODEN_DOORS, (blockbase_blockdata) -> {
            return blockbase_blockdata.getBlock() instanceof DoorBlock;
        })) {
            DoorBlock blockdoor = (DoorBlock) iblockdata.getBlock();

            if (!blockdoor.isOpen(iblockdata)) {
                // CraftBukkit start - entities opening doors
                org.bukkit.event.entity.EntityInteractEvent event = new org.bukkit.event.entity.EntityInteractEvent(entity.getBukkitEntity(), org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock.at(entity.level, blockposition));
                entity.level.getCraftServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                // CraftBukkit end
                blockdoor.setOpen(entity, world, iblockdata, blockposition, true);
            }

            this.rememberDoorToClose(world, entity, blockposition);
        }

        BlockPos blockposition1 = pathpoint1.asBlockPos();
        BlockState iblockdata1 = world.getBlockState(blockposition1);

        if (iblockdata1.is(BlockTags.WOODEN_DOORS, (blockbase_blockdata) -> {
            return blockbase_blockdata.getBlock() instanceof DoorBlock;
        })) {
            DoorBlock blockdoor1 = (DoorBlock) iblockdata1.getBlock();

            if (!blockdoor1.isOpen(iblockdata1)) {
                // CraftBukkit start - entities opening doors
                org.bukkit.event.entity.EntityInteractEvent event = new org.bukkit.event.entity.EntityInteractEvent(entity.getBukkitEntity(), org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock.at(entity.level, blockposition1));
                entity.level.getCraftServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                // CraftBukkit end
                blockdoor1.setOpen(entity, world, iblockdata1, blockposition1, true);
                this.rememberDoorToClose(world, entity, blockposition1);
            }
        }

        InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough(world, entity, pathpoint, pathpoint1);
    }

    public static void closeDoorsThatIHaveOpenedOrPassedThrough(ServerLevel world, LivingEntity entity, @Nullable Node lastNode, @Nullable Node currentNode) {
        Brain<?> behaviorcontroller = entity.getBrain();

        if (behaviorcontroller.hasMemoryValue(MemoryModuleType.DOORS_TO_CLOSE)) {
            Iterator iterator = ((Set) behaviorcontroller.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get()).iterator();

            while (iterator.hasNext()) {
                GlobalPos globalpos = (GlobalPos) iterator.next();
                BlockPos blockposition = globalpos.pos();

                if ((lastNode == null || !lastNode.asBlockPos().equals(blockposition)) && (currentNode == null || !currentNode.asBlockPos().equals(blockposition))) {
                    if (InteractWithDoor.isDoorTooFarAway(world, entity, globalpos)) {
                        iterator.remove();
                    } else {
                        BlockState iblockdata = world.getBlockState(blockposition);

                        if (!iblockdata.is(BlockTags.WOODEN_DOORS, (blockbase_blockdata) -> {
                            return blockbase_blockdata.getBlock() instanceof DoorBlock;
                        })) {
                            iterator.remove();
                        } else {
                            DoorBlock blockdoor = (DoorBlock) iblockdata.getBlock();

                            if (!blockdoor.isOpen(iblockdata)) {
                                iterator.remove();
                            } else if (InteractWithDoor.areOtherMobsComingThroughDoor(world, entity, blockposition)) {
                                iterator.remove();
                            } else {
                                blockdoor.setOpen(entity, world, iblockdata, blockposition, false);
                                iterator.remove();
                            }
                        }
                    }
                }
            }
        }

    }

    private static boolean areOtherMobsComingThroughDoor(ServerLevel world, LivingEntity entity, BlockPos pos) {
        Brain<?> behaviorcontroller = entity.getBrain();

        return !behaviorcontroller.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES) ? false : (behaviorcontroller.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get()).stream().filter((entityliving1) -> { // CraftBukkit - decompile error
            return entityliving1.getType() == entity.getType();
        }).filter((entityliving1) -> {
            return pos.closerToCenterThan(entityliving1.position(), 2.0D);
        }).anyMatch((entityliving1) -> {
            return InteractWithDoor.isMobComingThroughDoor(world, entityliving1, pos);
        });
    }

    private static boolean isMobComingThroughDoor(ServerLevel world, LivingEntity entity, BlockPos pos) {
        if (!entity.getBrain().hasMemoryValue(MemoryModuleType.PATH)) {
            return false;
        } else {
            Path pathentity = (Path) entity.getBrain().getMemory(MemoryModuleType.PATH).get();

            if (pathentity.isDone()) {
                return false;
            } else {
                Node pathpoint = pathentity.getPreviousNode();

                if (pathpoint == null) {
                    return false;
                } else {
                    Node pathpoint1 = pathentity.getNextNode();

                    return pos.equals(pathpoint.asBlockPos()) || pos.equals(pathpoint1.asBlockPos());
                }
            }
        }
    }

    private static boolean isDoorTooFarAway(ServerLevel world, LivingEntity entity, GlobalPos doorPos) {
        return doorPos.dimension() != world.dimension() || !doorPos.pos().closerToCenterThan(entity.position(), 2.0D);
    }

    private void rememberDoorToClose(ServerLevel world, LivingEntity entity, BlockPos pos) {
        Brain<?> behaviorcontroller = entity.getBrain();
        GlobalPos globalpos = GlobalPos.of(world.dimension(), pos);

        if (behaviorcontroller.getMemory(MemoryModuleType.DOORS_TO_CLOSE).isPresent()) {
            ((Set) behaviorcontroller.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get()).add(globalpos);
        } else {
            behaviorcontroller.setMemory(MemoryModuleType.DOORS_TO_CLOSE, Sets.newHashSet(new GlobalPos[]{globalpos})); // CraftBukkit - decompile error
        }

    }
}
