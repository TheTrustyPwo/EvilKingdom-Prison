package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.slf4j.Logger;

public class ServerPlaceRecipe<C extends Container> implements PlaceRecipe<Integer> {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final StackedContents stackedContents = new StackedContents();
    protected Inventory inventory;
    protected RecipeBookMenu<C> menu;

    public ServerPlaceRecipe(RecipeBookMenu<C> handler) {
        this.menu = handler;
    }

    public void recipeClicked(ServerPlayer entity, @Nullable Recipe<C> recipe, boolean craftAll) {
        if (recipe != null && entity.getRecipeBook().contains(recipe)) {
            this.inventory = entity.getInventory();
            if (this.testClearGrid() || entity.isCreative()) {
                this.stackedContents.clear();
                entity.getInventory().fillStackedContents(this.stackedContents);
                this.menu.fillCraftSlotsStackedContents(this.stackedContents);
                if (this.stackedContents.canCraft(recipe, (IntList)null)) {
                    this.handleRecipeClicked(recipe, craftAll);
                } else {
                    this.clearGrid(true);
                    entity.connection.send(new ClientboundPlaceGhostRecipePacket(entity.containerMenu.containerId, recipe));
                }

                entity.getInventory().setChanged();
            }
        }
    }

    protected void clearGrid(boolean bl) {
        for(int i = 0; i < this.menu.getSize(); ++i) {
            if (this.menu.shouldMoveToInventory(i)) {
                ItemStack itemStack = this.menu.getSlot(i).getItem().copy();
                this.inventory.placeItemBackInInventory(itemStack, false);
                this.menu.getSlot(i).set(itemStack);
            }
        }

        this.menu.clearCraftingContent();
    }

    protected void handleRecipeClicked(Recipe<C> recipe, boolean craftAll) {
        boolean bl = this.menu.recipeMatches(recipe);
        int i = this.stackedContents.getBiggestCraftableStack(recipe, (IntList)null);
        if (bl) {
            for(int j = 0; j < this.menu.getGridHeight() * this.menu.getGridWidth() + 1; ++j) {
                if (j != this.menu.getResultSlotIndex()) {
                    ItemStack itemStack = this.menu.getSlot(j).getItem();
                    if (!itemStack.isEmpty() && Math.min(i, itemStack.getMaxStackSize()) < itemStack.getCount() + 1) {
                        return;
                    }
                }
            }
        }

        int k = this.getStackSize(craftAll, i, bl);
        IntList intList = new IntArrayList();
        if (this.stackedContents.canCraft(recipe, intList, k)) {
            int l = k;

            for(int m : intList) {
                int n = StackedContents.fromStackingIndex(m).getMaxStackSize();
                if (n < l) {
                    l = n;
                }
            }

            if (this.stackedContents.canCraft(recipe, intList, l)) {
                this.clearGrid(false);
                this.placeRecipe(this.menu.getGridWidth(), this.menu.getGridHeight(), this.menu.getResultSlotIndex(), recipe, intList.iterator(), l);
            }
        }

    }

    @Override
    public void addItemToSlot(Iterator<Integer> inputs, int slot, int amount, int gridX, int gridY) {
        Slot slot2 = this.menu.getSlot(slot);
        ItemStack itemStack = StackedContents.fromStackingIndex(inputs.next());
        if (!itemStack.isEmpty()) {
            for(int i = 0; i < amount; ++i) {
                this.moveItemToGrid(slot2, itemStack);
            }
        }

    }

    protected int getStackSize(boolean craftAll, int limit, boolean recipeInCraftingSlots) {
        int i = 1;
        if (craftAll) {
            i = limit;
        } else if (recipeInCraftingSlots) {
            i = 64;

            for(int j = 0; j < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; ++j) {
                if (j != this.menu.getResultSlotIndex()) {
                    ItemStack itemStack = this.menu.getSlot(j).getItem();
                    if (!itemStack.isEmpty() && i > itemStack.getCount()) {
                        i = itemStack.getCount();
                    }
                }
            }

            if (i < 64) {
                ++i;
            }
        }

        return i;
    }

    protected void moveItemToGrid(Slot slot, ItemStack stack) {
        int i = this.inventory.findSlotMatchingUnusedItem(stack);
        if (i != -1) {
            ItemStack itemStack = this.inventory.getItem(i).copy();
            if (!itemStack.isEmpty()) {
                if (itemStack.getCount() > 1) {
                    this.inventory.removeItem(i, 1);
                } else {
                    this.inventory.removeItemNoUpdate(i);
                }

                itemStack.setCount(1);
                if (slot.getItem().isEmpty()) {
                    slot.set(itemStack);
                } else {
                    slot.getItem().grow(1);
                }

            }
        }
    }

    private boolean testClearGrid() {
        List<ItemStack> list = Lists.newArrayList();
        int i = this.getAmountOfFreeSlotsInInventory();

        for(int j = 0; j < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; ++j) {
            if (j != this.menu.getResultSlotIndex()) {
                ItemStack itemStack = this.menu.getSlot(j).getItem().copy();
                if (!itemStack.isEmpty()) {
                    int k = this.inventory.getSlotWithRemainingSpace(itemStack);
                    if (k == -1 && list.size() <= i) {
                        for(ItemStack itemStack2 : list) {
                            if (itemStack2.sameItem(itemStack) && itemStack2.getCount() != itemStack2.getMaxStackSize() && itemStack2.getCount() + itemStack.getCount() <= itemStack2.getMaxStackSize()) {
                                itemStack2.grow(itemStack.getCount());
                                itemStack.setCount(0);
                                break;
                            }
                        }

                        if (!itemStack.isEmpty()) {
                            if (list.size() >= i) {
                                return false;
                            }

                            list.add(itemStack);
                        }
                    } else if (k == -1) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private int getAmountOfFreeSlotsInInventory() {
        int i = 0;

        for(ItemStack itemStack : this.inventory.items) {
            if (itemStack.isEmpty()) {
                ++i;
            }
        }

        return i;
    }
}
