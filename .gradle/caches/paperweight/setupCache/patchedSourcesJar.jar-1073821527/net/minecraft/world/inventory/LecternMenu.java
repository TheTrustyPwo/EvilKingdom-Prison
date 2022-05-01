package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LecternMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 1;
    private static final int SLOT_COUNT = 1;
    public static final int BUTTON_PREV_PAGE = 1;
    public static final int BUTTON_NEXT_PAGE = 2;
    public static final int BUTTON_TAKE_BOOK = 3;
    public static final int BUTTON_PAGE_JUMP_RANGE_START = 100;
    private final Container lectern;
    private final ContainerData lecternData;

    public LecternMenu(int syncId) {
        this(syncId, new SimpleContainer(1), new SimpleContainerData(1));
    }

    public LecternMenu(int syncId, Container inventory, ContainerData propertyDelegate) {
        super(MenuType.LECTERN, syncId);
        checkContainerSize(inventory, 1);
        checkContainerDataCount(propertyDelegate, 1);
        this.lectern = inventory;
        this.lecternData = propertyDelegate;
        this.addSlot(new Slot(inventory, 0, 0, 0) {
            @Override
            public void setChanged() {
                super.setChanged();
                LecternMenu.this.slotsChanged(this.container);
            }
        });
        this.addDataSlots(propertyDelegate);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 100) {
            int i = id - 100;
            this.setData(0, i);
            return true;
        } else {
            switch(id) {
            case 1:
                int k = this.lecternData.get(0);
                this.setData(0, k - 1);
                return true;
            case 2:
                int j = this.lecternData.get(0);
                this.setData(0, j + 1);
                return true;
            case 3:
                if (!player.mayBuild()) {
                    return false;
                }

                ItemStack itemStack = this.lectern.removeItemNoUpdate(0);
                this.lectern.setChanged();
                if (!player.getInventory().add(itemStack)) {
                    player.drop(itemStack, false);
                }

                return true;
            default:
                return false;
            }
        }
    }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        this.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.lectern.stillValid(player);
    }

    public ItemStack getBook() {
        return this.lectern.getItem(0);
    }

    public int getPage() {
        return this.lecternData.get(0);
    }
}
