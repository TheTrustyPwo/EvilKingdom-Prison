package net.minecraft.world.entity;

import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface SlotAccess {
    SlotAccess NULL = new SlotAccess() {
        @Override
        public ItemStack get() {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean set(ItemStack stack) {
            return false;
        }
    };

    static SlotAccess forContainer(Container inventory, int index, Predicate<ItemStack> stackFilter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return inventory.getItem(index);
            }

            @Override
            public boolean set(ItemStack stack) {
                if (!stackFilter.test(stack)) {
                    return false;
                } else {
                    inventory.setItem(index, stack);
                    return true;
                }
            }
        };
    }

    static SlotAccess forContainer(Container inventory, int index) {
        return forContainer(inventory, index, (stack) -> {
            return true;
        });
    }

    static SlotAccess forEquipmentSlot(LivingEntity entity, EquipmentSlot slot, Predicate<ItemStack> filter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return entity.getItemBySlot(slot);
            }

            @Override
            public boolean set(ItemStack stack) {
                if (!filter.test(stack)) {
                    return false;
                } else {
                    entity.setItemSlot(slot, stack);
                    return true;
                }
            }
        };
    }

    static SlotAccess forEquipmentSlot(LivingEntity entity, EquipmentSlot slot) {
        return forEquipmentSlot(entity, slot, (stack) -> {
            return true;
        });
    }

    ItemStack get();

    boolean set(ItemStack stack);
}
