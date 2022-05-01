package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HarvestFarmland extends Behavior<Villager> {

    private static final int HARVEST_DURATION = 200;
    public static final float SPEED_MODIFIER = 0.5F;
    @Nullable
    private BlockPos aboveFarmlandPos;
    private long nextOkStartTime;
    private int timeWorkedSoFar;
    private final List<BlockPos> validFarmlandAroundVillager = Lists.newArrayList();

    public HarvestFarmland() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SECONDARY_JOB_SITE, MemoryStatus.VALUE_PRESENT));
    }

    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        if (!world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        } else if (entity.getVillagerData().getProfession() != VillagerProfession.FARMER) {
            return false;
        } else {
            BlockPos.MutableBlockPos blockposition_mutableblockposition = entity.blockPosition().mutable();

            this.validFarmlandAroundVillager.clear();

            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    for (int k = -1; k <= 1; ++k) {
                        blockposition_mutableblockposition.set(entity.getX() + (double) i, entity.getY() + (double) j, entity.getZ() + (double) k);
                        if (this.validPos(blockposition_mutableblockposition, world)) {
                            this.validFarmlandAroundVillager.add(new BlockPos(blockposition_mutableblockposition));
                        }
                    }
                }
            }

            this.aboveFarmlandPos = this.getValidFarmland(world);
            return this.aboveFarmlandPos != null;
        }
    }

    @Nullable
    private BlockPos getValidFarmland(ServerLevel world) {
        return this.validFarmlandAroundVillager.isEmpty() ? null : (BlockPos) this.validFarmlandAroundVillager.get(world.getRandom().nextInt(this.validFarmlandAroundVillager.size()));
    }

    private boolean validPos(BlockPos pos, ServerLevel world) {
        BlockState iblockdata = world.getBlockState(pos);
        Block block = iblockdata.getBlock();
        Block block1 = world.getBlockState(pos.below()).getBlock();

        return block instanceof CropBlock && ((CropBlock) block).isMaxAge(iblockdata) || iblockdata.isAir() && block1 instanceof FarmBlock;
    }

    protected void start(ServerLevel world, Villager entity, long time) {
        if (time > this.nextOkStartTime && this.aboveFarmlandPos != null) {
            entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, (new BlockPosTracker(this.aboveFarmlandPos))); // CraftBukkit - decompile error
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, (new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, 1))); // CraftBukkit - decompile error
        }

    }

    protected void stop(ServerLevel worldserver, Villager entityvillager, long i) {
        entityvillager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        entityvillager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.timeWorkedSoFar = 0;
        this.nextOkStartTime = i + 40L;
    }

    protected void tick(ServerLevel worldserver, Villager entityvillager, long i) {
        if (this.aboveFarmlandPos == null || this.aboveFarmlandPos.closerToCenterThan(entityvillager.position(), 1.0D)) {
            if (this.aboveFarmlandPos != null && i > this.nextOkStartTime) {
                BlockState iblockdata = worldserver.getBlockState(this.aboveFarmlandPos);
                Block block = iblockdata.getBlock();
                Block block1 = worldserver.getBlockState(this.aboveFarmlandPos.below()).getBlock();

                if (block instanceof CropBlock && ((CropBlock) block).isMaxAge(iblockdata)) {
                    // CraftBukkit start
                    if (!org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityChangeBlockEvent(entityvillager, this.aboveFarmlandPos, Blocks.AIR.defaultBlockState()).isCancelled()) {
                        worldserver.destroyBlock(this.aboveFarmlandPos, true, entityvillager);
                    }
                    // CraftBukkit end
                }

                if (iblockdata.isAir() && block1 instanceof FarmBlock && entityvillager.hasFarmSeeds()) {
                    SimpleContainer inventorysubcontainer = entityvillager.getInventory();

                    for (int j = 0; j < inventorysubcontainer.getContainerSize(); ++j) {
                        ItemStack itemstack = inventorysubcontainer.getItem(j);
                        boolean flag = false;

                        if (!itemstack.isEmpty()) {
                            // CraftBukkit start
                            Block planted = null;
                            if (itemstack.is(Items.WHEAT_SEEDS)) {
                                planted = Blocks.WHEAT;
                                flag = true;
                            } else if (itemstack.is(Items.POTATO)) {
                                planted = Blocks.POTATOES;
                                flag = true;
                            } else if (itemstack.is(Items.CARROT)) {
                                planted = Blocks.CARROTS;
                                flag = true;
                            } else if (itemstack.is(Items.BEETROOT_SEEDS)) {
                                planted = Blocks.BEETROOTS;
                                flag = true;
                            }

                            if (planted != null && !org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityChangeBlockEvent(entityvillager, this.aboveFarmlandPos, planted.defaultBlockState()).isCancelled()) {
                                worldserver.setBlock(this.aboveFarmlandPos, planted.defaultBlockState(), 3);
                            } else {
                                flag = false;
                            }
                            // CraftBukkit end
                        }

                        if (flag) {
                            worldserver.playSound((Player) null, (double) this.aboveFarmlandPos.getX(), (double) this.aboveFarmlandPos.getY(), (double) this.aboveFarmlandPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                            itemstack.shrink(1);
                            if (itemstack.isEmpty()) {
                                inventorysubcontainer.setItem(j, ItemStack.EMPTY);
                            }
                            break;
                        }
                    }
                }

                if (block instanceof CropBlock && !((CropBlock) block).isMaxAge(iblockdata)) {
                    this.validFarmlandAroundVillager.remove(this.aboveFarmlandPos);
                    this.aboveFarmlandPos = this.getValidFarmland(worldserver);
                    if (this.aboveFarmlandPos != null) {
                        this.nextOkStartTime = i + 20L;
                        entityvillager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, (new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5F, 1))); // CraftBukkit - decompile error
                        entityvillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, (new BlockPosTracker(this.aboveFarmlandPos))); // CraftBukkit - decompile error
                    }
                }
            }

            ++this.timeWorkedSoFar;
        }
    }

    protected boolean canStillUse(ServerLevel worldserver, Villager entityvillager, long i) {
        return this.timeWorkedSoFar < 200;
    }
}
