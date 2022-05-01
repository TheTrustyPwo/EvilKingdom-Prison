package net.minecraft.world.inventory;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class CartographyTableMenu extends AbstractContainerMenu {
    public static final int MAP_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final ContainerLevelAccess access;
    long lastSoundTime;
    public final Container container = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            CartographyTableMenu.this.slotsChanged(this);
            super.setChanged();
        }
    };
    private final ResultContainer resultContainer = new ResultContainer() {
        @Override
        public void setChanged() {
            CartographyTableMenu.this.slotsChanged(this);
            super.setChanged();
        }
    };

    public CartographyTableMenu(int syncId, Inventory inventory) {
        this(syncId, inventory, ContainerLevelAccess.NULL);
    }

    public CartographyTableMenu(int syncId, Inventory inventory, ContainerLevelAccess context) {
        super(MenuType.CARTOGRAPHY_TABLE, syncId);
        this.access = context;
        this.addSlot(new Slot(this.container, 0, 15, 15) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.FILLED_MAP);
            }
        });
        this.addSlot(new Slot(this.container, 1, 15, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.PAPER) || stack.is(Items.MAP) || stack.is(Items.GLASS_PANE);
            }
        });
        this.addSlot(new Slot(this.resultContainer, 2, 145, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                CartographyTableMenu.this.slots.get(0).remove(1);
                CartographyTableMenu.this.slots.get(1).remove(1);
                stack.getItem().onCraftedBy(stack, player.level, player);
                context.execute((world, pos) -> {
                    long l = world.getGameTime();
                    if (CartographyTableMenu.this.lastSoundTime != l) {
                        world.playSound((Player)null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        CartographyTableMenu.this.lastSoundTime = l;
                    }

                });
                super.onTake(player, stack);
            }
        });

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inventory, k, 8 + k * 18, 142));
        }

    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Blocks.CARTOGRAPHY_TABLE);
    }

    @Override
    public void slotsChanged(Container inventory) {
        ItemStack itemStack = this.container.getItem(0);
        ItemStack itemStack2 = this.container.getItem(1);
        ItemStack itemStack3 = this.resultContainer.getItem(2);
        if (itemStack3.isEmpty() || !itemStack.isEmpty() && !itemStack2.isEmpty()) {
            if (!itemStack.isEmpty() && !itemStack2.isEmpty()) {
                this.setupResultSlot(itemStack, itemStack2, itemStack3);
            }
        } else {
            this.resultContainer.removeItemNoUpdate(2);
        }

    }

    private void setupResultSlot(ItemStack map, ItemStack item, ItemStack oldResult) {
        this.access.execute((world, pos) -> {
            MapItemSavedData mapItemSavedData = MapItem.getSavedData(map, world);
            if (mapItemSavedData != null) {
                ItemStack itemStack4;
                if (item.is(Items.PAPER) && !mapItemSavedData.locked && mapItemSavedData.scale < 4) {
                    itemStack4 = map.copy();
                    itemStack4.setCount(1);
                    itemStack4.getOrCreateTag().putInt("map_scale_direction", 1);
                    this.broadcastChanges();
                } else if (item.is(Items.GLASS_PANE) && !mapItemSavedData.locked) {
                    itemStack4 = map.copy();
                    itemStack4.setCount(1);
                    itemStack4.getOrCreateTag().putBoolean("map_to_lock", true);
                    this.broadcastChanges();
                } else {
                    if (!item.is(Items.MAP)) {
                        this.resultContainer.removeItemNoUpdate(2);
                        this.broadcastChanges();
                        return;
                    }

                    itemStack4 = map.copy();
                    itemStack4.setCount(2);
                    this.broadcastChanges();
                }

                if (!ItemStack.matches(itemStack4, oldResult)) {
                    this.resultContainer.setItem(2, itemStack4);
                    this.broadcastChanges();
                }

            }
        });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index == 2) {
                itemStack2.getItem().onCraftedBy(itemStack2, player.level, player);
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index != 1 && index != 0) {
                if (itemStack2.is(Items.FILLED_MAP)) {
                    if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!itemStack2.is(Items.PAPER) && !itemStack2.is(Items.MAP) && !itemStack2.is(Items.GLASS_PANE)) {
                    if (index >= 3 && index < 30) {
                        if (!this.moveItemStackTo(itemStack2, 30, 39, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 30 && index < 39 && !this.moveItemStackTo(itemStack2, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
            this.broadcastChanges();
        }

        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultContainer.removeItemNoUpdate(2);
        this.access.execute((world, pos) -> {
            this.clearContainer(player, this.container);
        });
    }
}
