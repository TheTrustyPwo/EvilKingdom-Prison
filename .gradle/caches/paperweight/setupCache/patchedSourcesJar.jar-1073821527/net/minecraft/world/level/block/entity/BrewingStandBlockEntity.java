package net.minecraft.world.level.block.entity;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BrewingStandBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int[] SLOTS_FOR_UP = new int[]{3};
    private static final int[] SLOTS_FOR_DOWN = new int[]{0, 1, 2, 3};
    private static final int[] SLOTS_FOR_SIDES = new int[]{0, 1, 2, 4};
    public static final int FUEL_USES = 20;
    public static final int DATA_BREW_TIME = 0;
    public static final int DATA_FUEL_USES = 1;
    public static final int NUM_DATA_VALUES = 2;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    public int brewTime;
    private boolean[] lastPotionCount;
    private Item ingredient;
    public int fuel;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            switch(index) {
            case 0:
                return BrewingStandBlockEntity.this.brewTime;
            case 1:
                return BrewingStandBlockEntity.this.fuel;
            default:
                return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch(index) {
            case 0:
                BrewingStandBlockEntity.this.brewTime = value;
                break;
            case 1:
                BrewingStandBlockEntity.this.fuel = value;
            }

        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public BrewingStandBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.BREWING_STAND, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return new TranslatableComponent("container.brewing");
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity) {
        ItemStack itemStack = blockEntity.items.get(4);
        if (blockEntity.fuel <= 0 && itemStack.is(Items.BLAZE_POWDER)) {
            blockEntity.fuel = 20;
            itemStack.shrink(1);
            setChanged(world, pos, state);
        }

        boolean bl = isBrewable(blockEntity.items);
        boolean bl2 = blockEntity.brewTime > 0;
        ItemStack itemStack2 = blockEntity.items.get(3);
        if (bl2) {
            --blockEntity.brewTime;
            boolean bl3 = blockEntity.brewTime == 0;
            if (bl3 && bl) {
                doBrew(world, pos, blockEntity.items);
                setChanged(world, pos, state);
            } else if (!bl || !itemStack2.is(blockEntity.ingredient)) {
                blockEntity.brewTime = 0;
                setChanged(world, pos, state);
            }
        } else if (bl && blockEntity.fuel > 0) {
            --blockEntity.fuel;
            blockEntity.brewTime = 400;
            blockEntity.ingredient = itemStack2.getItem();
            setChanged(world, pos, state);
        }

        boolean[] bls = blockEntity.getPotionBits();
        if (!Arrays.equals(bls, blockEntity.lastPotionCount)) {
            blockEntity.lastPotionCount = bls;
            BlockState blockState = state;
            if (!(state.getBlock() instanceof BrewingStandBlock)) {
                return;
            }

            for(int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; ++i) {
                blockState = blockState.setValue(BrewingStandBlock.HAS_BOTTLE[i], Boolean.valueOf(bls[i]));
            }

            world.setBlock(pos, blockState, 2);
        }

    }

    private boolean[] getPotionBits() {
        boolean[] bls = new boolean[3];

        for(int i = 0; i < 3; ++i) {
            if (!this.items.get(i).isEmpty()) {
                bls[i] = true;
            }
        }

        return bls;
    }

    private static boolean isBrewable(NonNullList<ItemStack> slots) {
        ItemStack itemStack = slots.get(3);
        if (itemStack.isEmpty()) {
            return false;
        } else if (!PotionBrewing.isIngredient(itemStack)) {
            return false;
        } else {
            for(int i = 0; i < 3; ++i) {
                ItemStack itemStack2 = slots.get(i);
                if (!itemStack2.isEmpty() && PotionBrewing.hasMix(itemStack2, itemStack)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static void doBrew(Level world, BlockPos pos, NonNullList<ItemStack> slots) {
        ItemStack itemStack = slots.get(3);

        for(int i = 0; i < 3; ++i) {
            slots.set(i, PotionBrewing.mix(itemStack, slots.get(i)));
        }

        itemStack.shrink(1);
        if (itemStack.getItem().hasCraftingRemainingItem()) {
            ItemStack itemStack2 = new ItemStack(itemStack.getItem().getCraftingRemainingItem());
            if (itemStack.isEmpty()) {
                itemStack = itemStack2;
            } else {
                Containers.dropItemStack(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), itemStack2);
            }
        }

        slots.set(3, itemStack);
        world.levelEvent(1035, pos, 0);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, this.items);
        this.brewTime = nbt.getShort("BrewTime");
        this.fuel = nbt.getByte("Fuel");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putShort("BrewTime", (short)this.brewTime);
        ContainerHelper.saveAllItems(nbt, this.items);
        nbt.putByte("Fuel", (byte)this.fuel);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.items.size()) {
            this.items.set(slot, stack);
        }

    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return !(player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) > 64.0D);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 3) {
            return PotionBrewing.isIngredient(stack);
        } else if (slot == 4) {
            return stack.is(Items.BLAZE_POWDER);
        } else {
            return (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.GLASS_BOTTLE)) && this.getItem(slot).isEmpty();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return SLOTS_FOR_UP;
        } else {
            return side == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 3 ? stack.is(Items.GLASS_BOTTLE) : true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new BrewingStandMenu(syncId, playerInventory, this, this.dataAccess);
    }
}
