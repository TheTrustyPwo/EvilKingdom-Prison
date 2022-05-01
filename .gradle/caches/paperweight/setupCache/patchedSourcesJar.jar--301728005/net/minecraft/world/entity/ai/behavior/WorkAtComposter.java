package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WorkAtComposter extends WorkAtPoi {

    private static final List<Item> COMPOSTABLE_ITEMS = ImmutableList.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);

    public WorkAtComposter() {}

    @Override
    protected void useWorkstation(ServerLevel world, Villager entity) {
        Optional<GlobalPos> optional = entity.getBrain().getMemory(MemoryModuleType.JOB_SITE);

        if (optional.isPresent()) {
            GlobalPos globalpos = (GlobalPos) optional.get();
            BlockState iblockdata = world.getBlockState(globalpos.pos());

            if (iblockdata.is(Blocks.COMPOSTER)) {
                this.makeBread(entity);
                this.compostItems(world, entity, globalpos, iblockdata);
            }

        }
    }

    private void compostItems(ServerLevel world, Villager entity, GlobalPos pos, BlockState composterState) {
        BlockPos blockposition = pos.pos();

        if ((Integer) composterState.getValue(ComposterBlock.LEVEL) == 8) {
            composterState = ComposterBlock.extractProduce(composterState, world, blockposition, entity); // CraftBukkit
        }

        int i = 20;
        boolean flag = true;
        int[] aint = new int[WorkAtComposter.COMPOSTABLE_ITEMS.size()];
        SimpleContainer inventorysubcontainer = entity.getInventory();
        int j = inventorysubcontainer.getContainerSize();
        BlockState iblockdata1 = composterState;

        for (int k = j - 1; k >= 0 && i > 0; --k) {
            ItemStack itemstack = inventorysubcontainer.getItem(k);
            int l = WorkAtComposter.COMPOSTABLE_ITEMS.indexOf(itemstack.getItem());

            if (l != -1) {
                int i1 = itemstack.getCount();
                int j1 = aint[l] + i1;

                aint[l] = j1;
                int k1 = Math.min(Math.min(j1 - 10, i), i1);

                if (k1 > 0) {
                    i -= k1;

                    for (int l1 = 0; l1 < k1; ++l1) {
                        iblockdata1 = ComposterBlock.insertItem(iblockdata1, world, itemstack, blockposition, entity); // CraftBukkit
                        if ((Integer) iblockdata1.getValue(ComposterBlock.LEVEL) == 7) {
                            this.spawnComposterFillEffects(world, composterState, blockposition, iblockdata1);
                            return;
                        }
                    }
                }
            }
        }

        this.spawnComposterFillEffects(world, composterState, blockposition, iblockdata1);
    }

    private void spawnComposterFillEffects(ServerLevel world, BlockState oldState, BlockPos pos, BlockState newState) {
        world.levelEvent(1500, pos, newState != oldState ? 1 : 0);
    }

    private void makeBread(Villager entity) {
        SimpleContainer inventorysubcontainer = entity.getInventory();

        if (inventorysubcontainer.countItem(Items.BREAD) <= 36) {
            int i = inventorysubcontainer.countItem(Items.WHEAT);
            boolean flag = true;
            boolean flag1 = true;
            int j = Math.min(3, i / 3);

            if (j != 0) {
                int k = j * 3;

                inventorysubcontainer.removeItemType(Items.WHEAT, k);
                ItemStack itemstack = inventorysubcontainer.addItem(new ItemStack(Items.BREAD, j));

                if (!itemstack.isEmpty()) {
                    entity.forceDrops = true; // Paper
                    entity.spawnAtLocation(itemstack, 0.5F);
                    entity.forceDrops = false; // Paper
                }

            }
        }
    }
}
