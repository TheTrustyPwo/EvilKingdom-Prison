package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;
// CraftBukkit end

public class CraftingMenu extends RecipeBookMenu<CraftingContainer> {

    public static final int RESULT_SLOT = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_END = 10;
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;
    public final CraftingContainer craftSlots;
    public final ResultContainer resultSlots;
    public final ContainerLevelAccess access;
    private final Player player;
    // CraftBukkit start
    private CraftInventoryView bukkitEntity = null;
    // CraftBukkit end

    public CraftingMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    public CraftingMenu(int syncId, Inventory playerInventory, ContainerLevelAccess context) {
        super(MenuType.CRAFTING, syncId);
        // CraftBukkit start - Switched order of IInventory construction and stored player
        this.resultSlots = new ResultContainer();
        this.craftSlots = new CraftingContainer(this, 3, 3, playerInventory.player); // CraftBukkit - pass player
        this.craftSlots.resultInventory = this.resultSlots;
        // CraftBukkit end
        this.access = context;
        this.player = playerInventory.player;
        this.addSlot(new ResultSlot(playerInventory.player, this.craftSlots, this.resultSlots, 0, 124, 35));

        int j;
        int k;

        for (j = 0; j < 3; ++j) {
            for (k = 0; k < 3; ++k) {
                this.addSlot(new Slot(this.craftSlots, k + j * 3, 30 + k * 18, 17 + j * 18));
            }
        }

        for (j = 0; j < 3; ++j) {
            for (k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + j * 9 + 9, 8 + k * 18, 84 + j * 18));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 142));
        }

    }

    protected static void slotChangedCraftingGrid(AbstractContainerMenu handler, Level world, Player player, CraftingContainer craftingInventory, ResultContainer resultInventory) {
        if (!world.isClientSide) {
            ServerPlayer entityplayer = (ServerPlayer) player;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<CraftingRecipe> optional = world.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInventory, world);

            if (optional.isPresent()) {
                CraftingRecipe recipecrafting = (CraftingRecipe) optional.get();

                if (resultInventory.setRecipeUsed(world, entityplayer, recipecrafting)) {
                    itemstack = recipecrafting.assemble(craftingInventory);
                }
            }
            itemstack = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPreCraftEvent(craftingInventory, resultInventory, itemstack, handler.getBukkitView(), optional.orElse(null) instanceof RepairItemRecipe); // CraftBukkit

            resultInventory.setItem(0, itemstack);
            handler.setRemoteSlot(0, itemstack);
            entityplayer.connection.send(new ClientboundContainerSetSlotPacket(handler.containerId, handler.incrementStateId(), 0, itemstack));
        }
    }

    @Override
    public void slotsChanged(Container inventory) {
        this.access.execute((world, blockposition) -> {
            CraftingMenu.slotChangedCraftingGrid(this, world, this.player, this.craftSlots, this.resultSlots);
        });
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents finder) {
        this.craftSlots.fillStackedContents(finder);
    }

    @Override
    public void clearCraftingContent() {
        this.craftSlots.clearContent();
        this.resultSlots.clearContent();
    }

    @Override
    public boolean recipeMatches(Recipe<? super CraftingContainer> recipe) {
        return recipe.matches(this.craftSlots, this.player.level);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((world, blockposition) -> {
            this.clearContainer(player, this.craftSlots);
        });
    }

    @Override
    public boolean stillValid(Player player) {
        if (!this.checkReachable) return true; // CraftBukkit
        return stillValid(this.access, player, Blocks.CRAFTING_TABLE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();

            itemstack = itemstack1.copy();
            if (index == 0) {
                this.access.execute((world, blockposition) -> {
                    itemstack1.getItem().onCraftedBy(itemstack1, world, player);
                });
                if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index >= 10 && index < 46) {
                if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
                    if (index < 37) {
                        if (!this.moveItemStackTo(itemstack1, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack1, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(itemstack1, 10, 46, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
            if (index == 0) {
                player.drop(itemstack1, false);
            }
        }

        return itemstack;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public int getResultSlotIndex() {
        return 0;
    }

    @Override
    public int getGridWidth() {
        return this.craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return 10;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int index) {
        return index != this.getResultSlotIndex();
    }

    // CraftBukkit start
    @Override
    public CraftInventoryView getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftSlots, this.resultSlots);
        this.bukkitEntity = new CraftInventoryView(this.player.getBukkitEntity(), inventory, this);
        return this.bukkitEntity;
    }
    // CraftBukkit end
}
