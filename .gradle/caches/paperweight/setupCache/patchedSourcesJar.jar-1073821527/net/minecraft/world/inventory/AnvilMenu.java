package net.minecraft.world.inventory;

import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class AnvilMenu extends ItemCombinerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_COST = false;
    public static final int MAX_NAME_LENGTH = 50;
    private int repairItemCountCost;
    public String itemName;
    public final DataSlot cost = DataSlot.standalone();
    private static final int COST_FAIL = 0;
    private static final int COST_BASE = 1;
    private static final int COST_ADDED_BASE = 1;
    private static final int COST_REPAIR_MATERIAL = 1;
    private static final int COST_REPAIR_SACRIFICE = 2;
    private static final int COST_INCOMPATIBLE_PENALTY = 1;
    private static final int COST_RENAME = 1;

    public AnvilMenu(int syncId, Inventory inventory) {
        this(syncId, inventory, ContainerLevelAccess.NULL);
    }

    public AnvilMenu(int syncId, Inventory inventory, ContainerLevelAccess context) {
        super(MenuType.ANVIL, syncId, inventory, context);
        this.addDataSlot(this.cost);
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(BlockTags.ANVIL);
    }

    @Override
    protected boolean mayPickup(Player player, boolean present) {
        return (player.getAbilities().instabuild || player.experienceLevel >= this.cost.get()) && this.cost.get() > 0;
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-this.cost.get());
        }

        this.inputSlots.setItem(0, ItemStack.EMPTY);
        if (this.repairItemCountCost > 0) {
            ItemStack itemStack = this.inputSlots.getItem(1);
            if (!itemStack.isEmpty() && itemStack.getCount() > this.repairItemCountCost) {
                itemStack.shrink(this.repairItemCountCost);
                this.inputSlots.setItem(1, itemStack);
            } else {
                this.inputSlots.setItem(1, ItemStack.EMPTY);
            }
        } else {
            this.inputSlots.setItem(1, ItemStack.EMPTY);
        }

        this.cost.set(0);
        this.access.execute((world, pos) -> {
            BlockState blockState = world.getBlockState(pos);
            if (!player.getAbilities().instabuild && blockState.is(BlockTags.ANVIL) && player.getRandom().nextFloat() < 0.12F) {
                BlockState blockState2 = AnvilBlock.damage(blockState);
                if (blockState2 == null) {
                    world.removeBlock(pos, false);
                    world.levelEvent(1029, pos, 0);
                } else {
                    world.setBlock(pos, blockState2, 2);
                    world.levelEvent(1030, pos, 0);
                }
            } else {
                world.levelEvent(1030, pos, 0);
            }

        });
    }

    @Override
    public void createResult() {
        ItemStack itemStack = this.inputSlots.getItem(0);
        this.cost.set(1);
        int i = 0;
        int j = 0;
        int k = 0;
        if (itemStack.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
        } else {
            ItemStack itemStack2 = itemStack.copy();
            ItemStack itemStack3 = this.inputSlots.getItem(1);
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemStack2);
            j += itemStack.getBaseRepairCost() + (itemStack3.isEmpty() ? 0 : itemStack3.getBaseRepairCost());
            this.repairItemCountCost = 0;
            if (!itemStack3.isEmpty()) {
                boolean bl = itemStack3.is(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantments(itemStack3).isEmpty();
                if (itemStack2.isDamageableItem() && itemStack2.getItem().isValidRepairItem(itemStack, itemStack3)) {
                    int l = Math.min(itemStack2.getDamageValue(), itemStack2.getMaxDamage() / 4);
                    if (l <= 0) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    int m;
                    for(m = 0; l > 0 && m < itemStack3.getCount(); ++m) {
                        int n = itemStack2.getDamageValue() - l;
                        itemStack2.setDamageValue(n);
                        ++i;
                        l = Math.min(itemStack2.getDamageValue(), itemStack2.getMaxDamage() / 4);
                    }

                    this.repairItemCountCost = m;
                } else {
                    if (!bl && (!itemStack2.is(itemStack3.getItem()) || !itemStack2.isDamageableItem())) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    if (itemStack2.isDamageableItem() && !bl) {
                        int o = itemStack.getMaxDamage() - itemStack.getDamageValue();
                        int p = itemStack3.getMaxDamage() - itemStack3.getDamageValue();
                        int q = p + itemStack2.getMaxDamage() * 12 / 100;
                        int r = o + q;
                        int s = itemStack2.getMaxDamage() - r;
                        if (s < 0) {
                            s = 0;
                        }

                        if (s < itemStack2.getDamageValue()) {
                            itemStack2.setDamageValue(s);
                            i += 2;
                        }
                    }

                    Map<Enchantment, Integer> map2 = EnchantmentHelper.getEnchantments(itemStack3);
                    boolean bl2 = false;
                    boolean bl3 = false;

                    for(Enchantment enchantment : map2.keySet()) {
                        if (enchantment != null) {
                            int t = map.getOrDefault(enchantment, 0);
                            int u = map2.get(enchantment);
                            u = t == u ? u + 1 : Math.max(u, t);
                            boolean bl4 = enchantment.canEnchant(itemStack);
                            if (this.player.getAbilities().instabuild || itemStack.is(Items.ENCHANTED_BOOK)) {
                                bl4 = true;
                            }

                            for(Enchantment enchantment2 : map.keySet()) {
                                if (enchantment2 != enchantment && !enchantment.isCompatibleWith(enchantment2)) {
                                    bl4 = false;
                                    ++i;
                                }
                            }

                            if (!bl4) {
                                bl3 = true;
                            } else {
                                bl2 = true;
                                if (u > enchantment.getMaxLevel()) {
                                    u = enchantment.getMaxLevel();
                                }

                                map.put(enchantment, u);
                                int v = 0;
                                switch(enchantment.getRarity()) {
                                case COMMON:
                                    v = 1;
                                    break;
                                case UNCOMMON:
                                    v = 2;
                                    break;
                                case RARE:
                                    v = 4;
                                    break;
                                case VERY_RARE:
                                    v = 8;
                                }

                                if (bl) {
                                    v = Math.max(1, v / 2);
                                }

                                i += v * u;
                                if (itemStack.getCount() > 1) {
                                    i = 40;
                                }
                            }
                        }
                    }

                    if (bl3 && !bl2) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }
                }
            }

            if (StringUtils.isBlank(this.itemName)) {
                if (itemStack.hasCustomHoverName()) {
                    k = 1;
                    i += k;
                    itemStack2.resetHoverName();
                }
            } else if (!this.itemName.equals(itemStack.getHoverName().getString())) {
                k = 1;
                i += k;
                itemStack2.setHoverName(new TextComponent(this.itemName));
            }

            this.cost.set(j + i);
            if (i <= 0) {
                itemStack2 = ItemStack.EMPTY;
            }

            if (k == i && k > 0 && this.cost.get() >= 40) {
                this.cost.set(39);
            }

            if (this.cost.get() >= 40 && !this.player.getAbilities().instabuild) {
                itemStack2 = ItemStack.EMPTY;
            }

            if (!itemStack2.isEmpty()) {
                int w = itemStack2.getBaseRepairCost();
                if (!itemStack3.isEmpty() && w < itemStack3.getBaseRepairCost()) {
                    w = itemStack3.getBaseRepairCost();
                }

                if (k != i || k == 0) {
                    w = calculateIncreasedRepairCost(w);
                }

                itemStack2.setRepairCost(w);
                EnchantmentHelper.setEnchantments(map, itemStack2);
            }

            this.resultSlots.setItem(0, itemStack2);
            this.broadcastChanges();
        }
    }

    public static int calculateIncreasedRepairCost(int cost) {
        return cost * 2 + 1;
    }

    public void setItemName(String newItemName) {
        this.itemName = newItemName;
        if (this.getSlot(2).hasItem()) {
            ItemStack itemStack = this.getSlot(2).getItem();
            if (StringUtils.isBlank(newItemName)) {
                itemStack.resetHoverName();
            } else {
                itemStack.setHoverName(new TextComponent(this.itemName));
            }
        }

        this.createResult();
    }

    public int getCost() {
        return this.cost.get();
    }
}
