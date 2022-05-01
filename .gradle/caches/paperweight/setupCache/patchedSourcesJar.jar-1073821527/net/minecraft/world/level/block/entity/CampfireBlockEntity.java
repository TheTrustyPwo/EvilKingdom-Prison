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
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CampfireBlockEntity extends BlockEntity implements Clearable {
    private static final int BURN_COOL_SPEED = 2;
    private static final int NUM_SLOTS = 4;
    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    public final int[] cookingProgress = new int[4];
    public final int[] cookingTime = new int[4];

    public CampfireBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.CAMPFIRE, pos, state);
    }

    public static void cookTick(Level world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        boolean bl = false;

        for(int i = 0; i < campfire.items.size(); ++i) {
            ItemStack itemStack = campfire.items.get(i);
            if (!itemStack.isEmpty()) {
                bl = true;
                int var10002 = campfire.cookingProgress[i]++;
                if (campfire.cookingProgress[i] >= campfire.cookingTime[i]) {
                    Container container = new SimpleContainer(itemStack);
                    ItemStack itemStack2 = world.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, container, world).map((recipe) -> {
                        return recipe.assemble(container);
                    }).orElse(itemStack);
                    Containers.dropItemStack(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), itemStack2);
                    campfire.items.set(i, ItemStack.EMPTY);
                    world.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }

        if (bl) {
            setChanged(world, pos, state);
        }

    }

    public static void cooldownTick(Level world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        boolean bl = false;

        for(int i = 0; i < campfire.items.size(); ++i) {
            if (campfire.cookingProgress[i] > 0) {
                bl = true;
                campfire.cookingProgress[i] = Mth.clamp(campfire.cookingProgress[i] - 2, 0, campfire.cookingTime[i]);
            }
        }

        if (bl) {
            setChanged(world, pos, state);
        }

    }

    public static void particleTick(Level world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        Random random = world.random;
        if (random.nextFloat() < 0.11F) {
            for(int i = 0; i < random.nextInt(2) + 2; ++i) {
                CampfireBlock.makeParticles(world, pos, state.getValue(CampfireBlock.SIGNAL_FIRE), false);
            }
        }

        int j = state.getValue(CampfireBlock.FACING).get2DDataValue();

        for(int k = 0; k < campfire.items.size(); ++k) {
            if (!campfire.items.get(k).isEmpty() && random.nextFloat() < 0.2F) {
                Direction direction = Direction.from2DDataValue(Math.floorMod(k + j, 4));
                float f = 0.3125F;
                double d = (double)pos.getX() + 0.5D - (double)((float)direction.getStepX() * 0.3125F) + (double)((float)direction.getClockWise().getStepX() * 0.3125F);
                double e = (double)pos.getY() + 0.5D;
                double g = (double)pos.getZ() + 0.5D - (double)((float)direction.getStepZ() * 0.3125F) + (double)((float)direction.getClockWise().getStepZ() * 0.3125F);

                for(int l = 0; l < 4; ++l) {
                    world.addParticle(ParticleTypes.SMOKE, d, e, g, 0.0D, 5.0E-4D, 0.0D);
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
        if (nbt.contains("CookingTimes", 11)) {
            int[] is = nbt.getIntArray("CookingTimes");
            System.arraycopy(is, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, is.length));
        }

        if (nbt.contains("CookingTotalTimes", 11)) {
            int[] js = nbt.getIntArray("CookingTotalTimes");
            System.arraycopy(js, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, js.length));
        }

    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, this.items, true);
        nbt.putIntArray("CookingTimes", this.cookingProgress);
        nbt.putIntArray("CookingTotalTimes", this.cookingTime);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag compoundTag = new CompoundTag();
        ContainerHelper.saveAllItems(compoundTag, this.items, true);
        return compoundTag;
    }

    public Optional<CampfireCookingRecipe> getCookableRecipe(ItemStack item) {
        return this.items.stream().noneMatch(ItemStack::isEmpty) ? Optional.empty() : this.level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SimpleContainer(item), this.level);
    }

    public boolean placeFood(ItemStack item, int cookTime) {
        for(int i = 0; i < this.items.size(); ++i) {
            ItemStack itemStack = this.items.get(i);
            if (itemStack.isEmpty()) {
                this.cookingTime[i] = cookTime;
                this.cookingProgress[i] = 0;
                this.items.set(i, item.split(1));
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
