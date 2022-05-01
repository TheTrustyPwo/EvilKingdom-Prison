package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ChestMenu extends AbstractContainerMenu {
    private static final int SLOTS_PER_ROW = 9;
    private final Container container;
    private final int containerRows;

    private ChestMenu(MenuType<?> type, int syncId, Inventory playerInventory, int rows) {
        this(type, syncId, playerInventory, new SimpleContainer(9 * rows), rows);
    }

    public static ChestMenu oneRow(int syncId, Inventory playerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, syncId, playerInventory, 1);
    }

    public static ChestMenu twoRows(int syncId, Inventory playerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x2, syncId, playerInventory, 2);
    }

    public static ChestMenu threeRows(int syncId, Inventory playerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x3, syncId, playerInventory, 3);
    }

    public static ChestMenu fourRows(int syncId, Inventory playerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x4, syncId, playerInventory, 4);
    }

    public static ChestMenu fiveRows(int syncId, Inventory playerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x5, syncId, playerInventory, 5);
    }

    public static ChestMenu sixRows(int syncId, Inventory playerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x6, syncId, playerInventory, 6);
    }

    public static ChestMenu threeRows(int syncId, Inventory playerInventory, Container inventory) {
        return new ChestMenu(MenuType.GENERIC_9x3, syncId, playerInventory, inventory, 3);
    }

    public static ChestMenu sixRows(int syncId, Inventory playerInventory, Container inventory) {
        return new ChestMenu(MenuType.GENERIC_9x6, syncId, playerInventory, inventory, 6);
    }

    public ChestMenu(MenuType<?> type, int syncId, Inventory playerInventory, Container inventory, int rows) {
        super(type, syncId);
        checkContainerSize(inventory, rows * 9);
        this.container = inventory;
        this.containerRows = rows;
        inventory.startOpen(playerInventory.player);
        int i = (this.containerRows - 4) * 18;

        for(int j = 0; j < this.containerRows; ++j) {
            for(int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inventory, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }

        for(int l = 0; l < 3; ++l) {
            for(int m = 0; m < 9; ++m) {
                this.addSlot(new Slot(playerInventory, m + l * 9 + 9, 8 + m * 18, 103 + l * 18 + i));
            }
        }

        for(int n = 0; n < 9; ++n) {
            this.addSlot(new Slot(playerInventory, n, 8 + n * 18, 161 + i));
        }

    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index < this.containerRows * 9) {
                if (!this.moveItemStackTo(itemStack2, this.containerRows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, this.containerRows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getRowCount() {
        return this.containerRows;
    }
}
