package net.minecraft.world.entity.vehicle;

import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
// CraftBukkit start
import java.util.List;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
// CraftBukkit end

public abstract class AbstractMinecartContainer extends AbstractMinecart implements Container, MenuProvider {

    private NonNullList<ItemStack> itemStacks;
    @Nullable
    public ResourceLocation lootTable;
    public long lootTableSeed;

    // CraftBukkit start
    { this.lootableData = new com.destroystokyo.paper.loottable.PaperLootableInventoryData(new com.destroystokyo.paper.loottable.PaperMinecartLootableInventory(this)); } // Paper
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    private int maxStack = MAX_STACK;

    public List<ItemStack> getContents() {
        return this.itemStacks;
    }

    public void onOpen(CraftHumanEntity who) {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who) {
        this.transaction.remove(who);
    }

    public List<HumanEntity> getViewers() {
        return this.transaction;
    }

    public InventoryHolder getOwner() {
        org.bukkit.entity.Entity cart = getBukkitEntity();
        if(cart instanceof InventoryHolder) return (InventoryHolder) cart;
        return null;
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStack;
    }

    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location getLocation() {
        return getBukkitEntity().getLocation();
    }
    // CraftBukkit end

    protected AbstractMinecartContainer(EntityType<?> type, Level world) {
        super(type, world);
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY); // CraftBukkit - SPIGOT-3513
    }

    protected AbstractMinecartContainer(EntityType<?> type, double x, double y, double z, Level world) {
        super(type, world, x, y, z);
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY); // CraftBukkit - SPIGOT-3513
    }

    @Override
    public void destroy(DamageSource damageSource) {
        super.destroy(damageSource);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            Containers.dropContents(this.level, (Entity) this, (Container) this);
            if (!this.level.isClientSide) {
                Entity entity = damageSource.getDirectEntity();

                if (entity != null && entity.getType() == EntityType.PLAYER) {
                    PiglinAi.angerNearbyPiglins((Player) entity, true);
                }
            }
        }

    }

    @Override
    public boolean isEmpty() {
        Iterator iterator = this.itemStacks.iterator();

        ItemStack itemstack;

        do {
            if (!iterator.hasNext()) {
                return true;
            }

            itemstack = (ItemStack) iterator.next();
        } while (itemstack.isEmpty());

        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        this.unpackLootTable((Player) null);
        return (ItemStack) this.itemStacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        this.unpackLootTable((Player) null);
        return ContainerHelper.removeItem(this.itemStacks, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        this.unpackLootTable((Player) null);
        ItemStack itemstack = (ItemStack) this.itemStacks.get(slot);

        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.itemStacks.set(slot, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.unpackLootTable((Player) null);
        this.itemStacks.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

    }

    @Override
    public SlotAccess getSlot(final int mappedIndex) {
        return mappedIndex >= 0 && mappedIndex < this.getContainerSize() ? new SlotAccess() {
            @Override
            public ItemStack get() {
                return AbstractMinecartContainer.this.getItem(mappedIndex);
            }

            @Override
            public boolean set(ItemStack stack) {
                AbstractMinecartContainer.this.setItem(mappedIndex, stack);
                return true;
            }
        } : super.getSlot(mappedIndex);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return this.isRemoved() ? false : player.distanceToSqr((Entity) this) <= 64.0D;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level.isClientSide && reason.shouldDestroy()) {
            Containers.dropContents(this.level, (Entity) this, (Container) this);
        }

        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        this.lootableData.saveNbt(nbt); // Paper
        if (this.lootTable != null) {
            nbt.putString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                nbt.putLong("LootTableSeed", this.lootTableSeed);
            }
        } if (true) { // Paper - Always save the items, Table may stick around
            ContainerHelper.saveAllItems(nbt, this.itemStacks);
        }

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.lootableData.loadNbt(nbt); // Paper
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (nbt.contains("LootTable", 8)) {
            this.lootTable = new ResourceLocation(nbt.getString("LootTable"));
            this.lootTableSeed = nbt.getLong("LootTableSeed");
        } if (true) { // Paper - always load the items, table may still remain
            ContainerHelper.loadAllItems(nbt, this.itemStacks);
        }

    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        player.openMenu(this);
        if (!player.level.isClientSide) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, (Entity) player);
            PiglinAi.angerNearbyPiglins(player, true);
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void applyNaturalSlowdown() {
        float f = 0.98F;

        if (this.lootTable == null) {
            int i = 15 - AbstractContainerMenu.getRedstoneSignalFromContainer(this);

            f += (float) i * 0.001F;
        }

        if (this.isInWater()) {
            f *= 0.95F;
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply((double) f, 0.0D, (double) f));
    }

    public void unpackLootTable(@Nullable Player player) {
        if (this.lootableData.shouldReplenish(player) && this.level.getServer() != null) { // Paper
            LootTable loottable = this.level.getServer().getLootTables().get(this.lootTable);

            if (player instanceof ServerPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer) player, this.lootTable);
            }

            //this.lootTable = null; // Paper
            this.lootableData.processRefill(player); // Paper
            LootContext.Builder loottableinfo_builder = (new LootContext.Builder((ServerLevel) this.level)).withParameter(LootContextParams.ORIGIN, this.position()).withOptionalRandomSeed(this.lootTableSeed);

            if (player != null) {
                loottableinfo_builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
            }

            loottable.fill(this, loottableinfo_builder.create(LootContextParamSets.CHEST));
        }

    }

    @Override
    public void clearContent() {
        this.unpackLootTable((Player) null);
        this.itemStacks.clear();
    }

    public void setLootTable(ResourceLocation id, long lootSeed) {
        this.lootTable = id;
        this.lootTableSeed = lootSeed;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        if (this.lootTable != null && player.isSpectator()) {
            return null;
        } else {
            this.unpackLootTable(inv.player);
            return this.createMenu(syncId, inv);
        }
    }

    protected abstract AbstractContainerMenu createMenu(int syncId, Inventory playerInventory);
}
