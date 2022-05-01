package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class UseBonemeal extends Behavior<Villager> {
    private static final int BONEMEALING_DURATION = 80;
    private long nextWorkCycleTime;
    private long lastBonemealingSession;
    private int timeWorkedSoFar;
    private Optional<BlockPos> cropPos = Optional.empty();

    public UseBonemeal() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        if (entity.tickCount % 10 == 0 && (this.lastBonemealingSession == 0L || this.lastBonemealingSession + 160L <= (long)entity.tickCount)) {
            if (entity.getInventory().countItem(Items.BONE_MEAL) <= 0) {
                return false;
            } else {
                this.cropPos = this.pickNextTarget(world, entity);
                return this.cropPos.isPresent();
            }
        } else {
            return false;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Villager villager, long l) {
        return this.timeWorkedSoFar < 80 && this.cropPos.isPresent();
    }

    private Optional<BlockPos> pickNextTarget(ServerLevel world, Villager entity) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        Optional<BlockPos> optional = Optional.empty();
        int i = 0;

        for(int j = -1; j <= 1; ++j) {
            for(int k = -1; k <= 1; ++k) {
                for(int l = -1; l <= 1; ++l) {
                    mutableBlockPos.setWithOffset(entity.blockPosition(), j, k, l);
                    if (this.validPos(mutableBlockPos, world)) {
                        ++i;
                        if (world.random.nextInt(i) == 0) {
                            optional = Optional.of(mutableBlockPos.immutable());
                        }
                    }
                }
            }
        }

        return optional;
    }

    private boolean validPos(BlockPos pos, ServerLevel world) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        return block instanceof CropBlock && !((CropBlock)block).isMaxAge(blockState);
    }

    @Override
    protected void start(ServerLevel serverLevel, Villager villager, long l) {
        this.setCurrentCropAsTarget(villager);
        villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE_MEAL));
        this.nextWorkCycleTime = l;
        this.timeWorkedSoFar = 0;
    }

    private void setCurrentCropAsTarget(Villager villager) {
        this.cropPos.ifPresent((pos) -> {
            BlockPosTracker blockPosTracker = new BlockPosTracker(pos);
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, blockPosTracker);
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(blockPosTracker, 0.5F, 1));
        });
    }

    @Override
    protected void stop(ServerLevel world, Villager entity, long time) {
        entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.lastBonemealingSession = (long)entity.tickCount;
    }

    @Override
    protected void tick(ServerLevel world, Villager entity, long time) {
        BlockPos blockPos = this.cropPos.get();
        if (time >= this.nextWorkCycleTime && blockPos.closerToCenterThan(entity.position(), 1.0D)) {
            ItemStack itemStack = ItemStack.EMPTY;
            SimpleContainer simpleContainer = entity.getInventory();
            int i = simpleContainer.getContainerSize();

            for(int j = 0; j < i; ++j) {
                ItemStack itemStack2 = simpleContainer.getItem(j);
                if (itemStack2.is(Items.BONE_MEAL)) {
                    itemStack = itemStack2;
                    break;
                }
            }

            if (!itemStack.isEmpty() && BoneMealItem.growCrop(itemStack, world, blockPos)) {
                world.levelEvent(1505, blockPos, 0);
                this.cropPos = this.pickNextTarget(world, entity);
                this.setCurrentCropAsTarget(entity);
                this.nextWorkCycleTime = time + 40L;
            }

            ++this.timeWorkedSoFar;
        }
    }
}
