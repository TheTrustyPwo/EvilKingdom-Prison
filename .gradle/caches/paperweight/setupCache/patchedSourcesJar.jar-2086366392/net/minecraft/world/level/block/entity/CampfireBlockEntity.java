package net.minecraft.world.level.block.entity;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
// CraftBukkit start
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.event.block.BlockCookEvent;
// CraftBukkit end

public class CampfireBlockEntity extends BlockEntity implements Clearable {

    private static final int BURN_COOL_SPEED = 2;
    private static final int NUM_SLOTS = 4;
    private final NonNullList<ItemStack> items;
    public final int[] cookingProgress;
    public final int[] cookingTime;
    public final boolean[] stopCooking; // Paper

    public CampfireBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.CAMPFIRE, pos, state);
        this.items = NonNullList.withSize(4, ItemStack.EMPTY);
        this.cookingProgress = new int[4];
        this.cookingTime = new int[4];
        this.stopCooking = new boolean[4]; // Paper
    }

    public static void cookTick(Level world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        boolean flag = false;

        for (int i = 0; i < campfire.items.size(); ++i) {
            ItemStack itemstack = (ItemStack) campfire.items.get(i);

            if (!itemstack.isEmpty()) {
                flag = true;
                if (!campfire.stopCooking[i]) { // Paper
                int j = campfire.cookingProgress[i]++;
                } // Paper

                if (campfire.cookingProgress[i] >= campfire.cookingTime[i]) {
                    SimpleContainer inventorysubcontainer = new SimpleContainer(new ItemStack[]{itemstack});
                    // Paper start
                    Optional<CampfireCookingRecipe> recipe = world.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, inventorysubcontainer, world);
                    ItemStack itemstack1 = (ItemStack) recipe.map((recipecampfire) -> {
                        // Paper end
                        return recipecampfire.assemble(inventorysubcontainer);
                    }).orElse(itemstack);

                    // CraftBukkit start - fire BlockCookEvent
                    CraftItemStack source = CraftItemStack.asCraftMirror(itemstack);
                    org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemstack1);

                    BlockCookEvent blockCookEvent = new BlockCookEvent(CraftBlock.at(world, pos), source, result, (org.bukkit.inventory.CookingRecipe<?>) recipe.map(CampfireCookingRecipe::toBukkitRecipe).orElse(null)); // Paper
                    world.getCraftServer().getPluginManager().callEvent(blockCookEvent);

                    if (blockCookEvent.isCancelled()) {
                        return;
                    }

                    result = blockCookEvent.getResult();
                    itemstack1 = CraftItemStack.asNMSCopy(result);
                    // CraftBukkit end
                    // Paper start
                    net.minecraft.world.entity.item.ItemEntity droppedItem = new net.minecraft.world.entity.item.ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, itemstack1.split(world.random.nextInt(21) + 10));
                    droppedItem.setDeltaMovement(world.random.nextGaussian() * 0.05D, world.random.nextGaussian() * 0.05D + 0.2D, world.random.nextGaussian() * 0.05D);
                    world.addFreshEntity(droppedItem);
                    // Paper end
                    campfire.items.set(i, ItemStack.EMPTY);
                    world.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }

        if (flag) {
            setChanged(world, pos, state);
        }

    }

    public static void cooldownTick(Level world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        boolean flag = false;

        for (int i = 0; i < campfire.items.size(); ++i) {
            if (campfire.cookingProgress[i] > 0) {
                flag = true;
                campfire.cookingProgress[i] = Mth.clamp(campfire.cookingProgress[i] - 2, (int) 0, campfire.cookingTime[i]);
            }
        }

        if (flag) {
            setChanged(world, pos, state);
        }

    }

    public static void particleTick(Level world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        Random random = world.random;
        int i;

        if (random.nextFloat() < 0.11F) {
            for (i = 0; i < random.nextInt(2) + 2; ++i) {
                CampfireBlock.makeParticles(world, pos, (Boolean) state.getValue(CampfireBlock.SIGNAL_FIRE), false);
            }
        }

        i = ((Direction) state.getValue(CampfireBlock.FACING)).get2DDataValue();

        for (int j = 0; j < campfire.items.size(); ++j) {
            if (!((ItemStack) campfire.items.get(j)).isEmpty() && random.nextFloat() < 0.2F) {
                Direction enumdirection = Direction.from2DDataValue(Math.floorMod(j + i, 4));
                float f = 0.3125F;
                double d0 = (double) pos.getX() + 0.5D - (double) ((float) enumdirection.getStepX() * 0.3125F) + (double) ((float) enumdirection.getClockWise().getStepX() * 0.3125F);
                double d1 = (double) pos.getY() + 0.5D;
                double d2 = (double) pos.getZ() + 0.5D - (double) ((float) enumdirection.getStepZ() * 0.3125F) + (double) ((float) enumdirection.getClockWise().getStepZ() * 0.3125F);

                for (int k = 0; k < 4; ++k) {
                    world.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0D, 5.0E-4D, 0.0D);
                }
            }
        }

    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.items.clear();
        ContainerHelper.loadAllItems(nbt, this.items);
        int[] aint;

        if (nbt.contains("CookingTimes", 11)) {
            aint = nbt.getIntArray("CookingTimes");
            System.arraycopy(aint, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, aint.length));
        }

        if (nbt.contains("CookingTotalTimes", 11)) {
            aint = nbt.getIntArray("CookingTotalTimes");
            System.arraycopy(aint, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, aint.length));
        }

        // Paper start
        if (nbt.contains("Paper.StopCooking", org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers.NBT.TAG_BYTE_ARRAY)) {
            byte[] abyte = nbt.getByteArray("Paper.StopCooking");
            boolean[] cookingState = new boolean[4];
            for (int index = 0; index < abyte.length; index++) {
                cookingState[index] = abyte[index] == 1;
            }
            System.arraycopy(cookingState, 0, this.stopCooking, 0, Math.min(this.stopCooking.length, abyte.length));
        }
        // Paper end
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, this.items, true);
        nbt.putIntArray("CookingTimes", this.cookingProgress);
        nbt.putIntArray("CookingTotalTimes", this.cookingTime);
        // Paper start
        byte[] cookingState = new byte[4];
        for (int index = 0; index < cookingState.length; index++) {
            cookingState[index] = (byte) (this.stopCooking[index] ? 1 : 0);
        }
        nbt.putByteArray("Paper.StopCooking", cookingState);
        // Paper end
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbttagcompound = new CompoundTag();

        ContainerHelper.saveAllItems(nbttagcompound, this.items, true);
        return nbttagcompound;
    }

    public Optional<CampfireCookingRecipe> getCookableRecipe(ItemStack item) {
        return this.items.stream().noneMatch(ItemStack::isEmpty) ? Optional.empty() : this.level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SimpleContainer(new ItemStack[]{item}), this.level);
    }

    public boolean placeFood(ItemStack item, int cookTime) {
        for (int j = 0; j < this.items.size(); ++j) {
            ItemStack itemstack1 = (ItemStack) this.items.get(j);

            if (itemstack1.isEmpty()) {
                this.cookingTime[j] = cookTime;
                this.cookingProgress[j] = 0;
                this.items.set(j, item.split(1));
                this.markUpdated();
                return true;
            }
        }

        return false;
    }

    private void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    public void dowse() {
        if (this.level != null) {
            this.markUpdated();
        }

    }
}
