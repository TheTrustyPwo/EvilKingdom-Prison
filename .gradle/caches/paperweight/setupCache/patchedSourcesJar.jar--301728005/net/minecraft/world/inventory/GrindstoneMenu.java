package net.minecraft.world.inventory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryGrindstone;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;
import org.bukkit.entity.Player;
// CraftBukkit end

public class GrindstoneMenu extends AbstractContainerMenu {

    // CraftBukkit start
    private CraftInventoryView bukkitEntity = null;
    private Player player;

    @Override
    public CraftInventoryView getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }

        CraftInventoryGrindstone inventory = new CraftInventoryGrindstone(this.repairSlots, this.resultSlots);
        this.bukkitEntity = new CraftInventoryView(this.player, inventory, this);
        return this.bukkitEntity;
    }
    // CraftBukkit end
    public static final int MAX_NAME_LENGTH = 35;
    public static final int INPUT_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final Container resultSlots;
    final Container repairSlots;
    private final ContainerLevelAccess access;

    public GrindstoneMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    public GrindstoneMenu(int syncId, Inventory playerInventory, final ContainerLevelAccess context) {
        super(MenuType.GRINDSTONE, syncId);
        this.resultSlots = new ResultContainer();
        this.repairSlots = new SimpleContainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                GrindstoneMenu.this.slotsChanged(this);
            }

            // CraftBukkit start
            @Override
            public Location getLocation() {
                return context.getLocation();
            }
            // CraftBukkit end
        };
        this.access = context;
        this.addSlot(new Slot(this.repairSlots, 0, 49, 19) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.isDamageableItem() || stack.is(Items.ENCHANTED_BOOK) || stack.isEnchanted();
            }
        });
        this.addSlot(new Slot(this.repairSlots, 1, 49, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.isDamageableItem() || stack.is(Items.ENCHANTED_BOOK) || stack.isEnchanted();
            }
        });
        this.addSlot(new Slot(this.resultSlots, 2, 129, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(net.minecraft.world.entity.player.Player player, ItemStack stack) {
                context.execute((world, blockposition) -> {
                    if (world instanceof ServerLevel) {
                        ExperienceOrb.award((ServerLevel) world, Vec3.atCenterOf(blockposition), this.getExperienceAmount(world), org.bukkit.entity.ExperienceOrb.SpawnReason.GRINDSTONE, player); // Paper
                    }

                    world.levelEvent(1042, blockposition, 0);
                });
                GrindstoneMenu.this.repairSlots.setItem(0, ItemStack.EMPTY);
                GrindstoneMenu.this.repairSlots.setItem(1, ItemStack.EMPTY);
            }

            private int getExperienceAmount(Level world) {
                byte b0 = 0;
                int j = b0 + this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(0));

                j += this.getExperienceFromItem(GrindstoneMenu.this.repairSlots.getItem(1));
                if (j > 0) {
                    int k = (int) Math.ceil((double) j / 2.0D);

                    return k + world.random.nextInt(k);
                } else {
                    return 0;
                }
            }

            private int getExperienceFromItem(ItemStack stack) {
                int j = 0;
                Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
                Iterator iterator = map.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry<Enchantment, Integer> entry = (Entry) iterator.next();
                    Enchantment enchantment = (Enchantment) entry.getKey();
                    Integer integer = (Integer) entry.getValue();

                    if (!enchantment.isCurse()) {
                        j += enchantment.getMinCost(integer);
                    }
                }

                return j;
            }
        });

        int j;

        for (j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + j * 9 + 9, 8 + k * 18, 84 + j * 18));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 142));
        }

        this.player = (Player) playerInventory.player.getBukkitEntity(); // CraftBukkit
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (inventory == this.repairSlots) {
            this.createResult();
            org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPrepareResultEvent(this, 2); // Paper
        }

    }

    private void createResult() {
        ItemStack itemstack = this.repairSlots.getItem(0);
        ItemStack itemstack1 = this.repairSlots.getItem(1);
        boolean flag = !itemstack.isEmpty() || !itemstack1.isEmpty();
        boolean flag1 = !itemstack.isEmpty() && !itemstack1.isEmpty();

        if (flag) {
            boolean flag2 = !itemstack.isEmpty() && !itemstack.is(Items.ENCHANTED_BOOK) && !itemstack.isEnchanted() || !itemstack1.isEmpty() && !itemstack1.is(Items.ENCHANTED_BOOK) && !itemstack1.isEnchanted();

            if (itemstack.getCount() > 1 || itemstack1.getCount() > 1 || !flag1 && flag2) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                this.broadcastChanges();
                return;
            }

            byte b0 = 1;
            int i;
            ItemStack itemstack2;

            if (flag1) {
                if (!itemstack.is(itemstack1.getItem())) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.broadcastChanges();
                    return;
                }

                Item item = itemstack.getItem();
                int j = item.getMaxDamage() - itemstack.getDamageValue();
                int k = item.getMaxDamage() - itemstack1.getDamageValue();
                int l = j + k + item.getMaxDamage() * 5 / 100;

                i = Math.max(item.getMaxDamage() - l, 0);
                itemstack2 = this.mergeEnchants(itemstack, itemstack1);
                if (!itemstack2.isDamageableItem()) {
                    if (!ItemStack.matches(itemstack, itemstack1) || itemstack2.getMaxStackSize() == 1) { // Paper - add max stack size check
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.broadcastChanges();
                        return;
                    }

                    b0 = 2; // Paper - the problem line for above change, causing over-stacking
                }
            } else {
                boolean flag3 = !itemstack.isEmpty();

                i = flag3 ? itemstack.getDamageValue() : itemstack1.getDamageValue();
                itemstack2 = flag3 ? itemstack : itemstack1;
            }

            this.resultSlots.setItem(0, this.removeNonCurses(itemstack2, i, b0));
        } else {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        }

        this.broadcastChanges();
    }

    private ItemStack mergeEnchants(ItemStack target, ItemStack source) {
        ItemStack itemstack2 = target.copy();
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(source);
        Iterator iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Enchantment, Integer> entry = (Entry) iterator.next();
            Enchantment enchantment = (Enchantment) entry.getKey();

            if (!enchantment.isCurse() || EnchantmentHelper.getItemEnchantmentLevel(enchantment, itemstack2) == 0) {
                itemstack2.enchant(enchantment, (Integer) entry.getValue());
            }
        }

        return itemstack2;
    }

    private ItemStack removeNonCurses(ItemStack item, int damage, int amount) {
        ItemStack itemstack1 = item.copy();

        itemstack1.removeTagKey("Enchantments");
        itemstack1.removeTagKey("StoredEnchantments");
        if (damage > 0) {
            itemstack1.setDamageValue(damage);
        } else {
            itemstack1.removeTagKey("Damage");
        }

        itemstack1.setCount(amount);
        Map<Enchantment, Integer> map = (Map) EnchantmentHelper.getEnchantments(item).entrySet().stream().filter((entry) -> {
            return ((Enchantment) entry.getKey()).isCurse();
        }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        EnchantmentHelper.setEnchantments(map, itemstack1);
        itemstack1.setRepairCost(0);
        if (itemstack1.is(Items.ENCHANTED_BOOK) && map.size() == 0) {
            itemstack1 = new ItemStack(Items.BOOK);
            if (item.hasCustomHoverName()) {
                itemstack1.setHoverName(item.getHoverName());
            }
        }

        for (int k = 0; k < map.size(); ++k) {
            itemstack1.setRepairCost(AnvilMenu.calculateIncreasedRepairCost(itemstack1.getBaseRepairCost()));
        }

        return itemstack1;
    }

    @Override
    public void removed(net.minecraft.world.entity.player.Player player) {
        super.removed(player);
        this.access.execute((world, blockposition) -> {
            this.clearContainer(player, this.repairSlots);
        });
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        if (!this.checkReachable) return true; // CraftBukkit
        return stillValid(this.access, player, Blocks.GRINDSTONE);
    }

    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();

            itemstack = itemstack1.copy();
            ItemStack itemstack2 = this.repairSlots.getItem(0);
            ItemStack itemstack3 = this.repairSlots.getItem(1);

            if (index == 2) {
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index != 0 && index != 1) {
                if (!itemstack2.isEmpty() && !itemstack3.isEmpty()) {
                    if (index >= 3 && index < 30) {
                        if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 30 && index < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
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
        }

        return itemstack;
    }
}
