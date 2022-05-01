package net.minecraft.world.inventory;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView; // CraftBukkit

public class SmithingMenu extends ItemCombinerMenu {

    private final Level level;
    @Nullable
    private UpgradeRecipe selectedRecipe;
    private final List<UpgradeRecipe> recipes;
    // CraftBukkit start
    private CraftInventoryView bukkitEntity;
    // CraftBukkit end

    public SmithingMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    public SmithingMenu(int syncId, Inventory playerInventory, ContainerLevelAccess context) {
        super(MenuType.SMITHING, syncId, playerInventory, context);
        this.level = playerInventory.player.level;
        this.recipes = this.level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING);
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(Blocks.SMITHING_TABLE);
    }

    @Override
    protected boolean mayPickup(Player player, boolean present) {
        return this.selectedRecipe != null && this.selectedRecipe.matches(this.inputSlots, this.level);
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        stack.onCraftedBy(player.level, player, stack.getCount());
        this.resultSlots.awardUsedRecipes(player);
        this.shrinkStackInSlot(0);
        this.shrinkStackInSlot(1);
        this.access.execute((world, blockposition) -> {
            world.levelEvent(1044, blockposition, 0);
        });
    }

    private void shrinkStackInSlot(int slot) {
        ItemStack itemstack = this.inputSlots.getItem(slot);

        itemstack.shrink(1);
        this.inputSlots.setItem(slot, itemstack);
    }

    @Override
    public void createResult() {
        List<UpgradeRecipe> list = this.level.getRecipeManager().getRecipesFor(RecipeType.SMITHING, this.inputSlots, this.level);

        if (list.isEmpty()) {
            org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPrepareSmithingEvent(this.getBukkitView(), ItemStack.EMPTY); // CraftBukkit
        } else {
            this.selectedRecipe = (UpgradeRecipe) list.get(0);
            ItemStack itemstack = this.selectedRecipe.assemble(this.inputSlots);

            this.resultSlots.setRecipeUsed(this.selectedRecipe);
            // CraftBukkit start
            org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPrepareSmithingEvent(this.getBukkitView(), itemstack);
            // CraftBukkit end
        }

        org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPrepareResultEvent(this, 2); // Paper
    }

    @Override
    protected boolean shouldQuickMoveToAdditionalSlot(ItemStack stack) {
        return this.recipes.stream().anyMatch((recipesmithing) -> {
            return recipesmithing.isAdditionIngredient(stack);
        });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    // CraftBukkit start
    @Override
    public CraftInventoryView getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }

        org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventory inventory = new org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventorySmithing(
                access.getLocation(), this.inputSlots, this.resultSlots);
        this.bukkitEntity = new CraftInventoryView(this.player.getBukkitEntity(), inventory, this);
        return this.bukkitEntity;
    }
    // CraftBukkit end
}
