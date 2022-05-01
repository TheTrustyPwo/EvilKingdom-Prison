package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemInput implements Predicate<ItemStack> {
    private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType((item, maxCount) -> {
        return new TranslatableComponent("arguments.item.overstacked", item, maxCount);
    });
    private final Item item;
    @Nullable
    private final CompoundTag tag;

    public ItemInput(Item item, @Nullable CompoundTag nbt) {
        this.item = item;
        this.tag = nbt;
    }

    public Item getItem() {
        return this.item;
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return itemStack.is(this.item) && NbtUtils.compareNbt(this.tag, itemStack.getTag(), true);
    }

    public ItemStack createItemStack(int amount, boolean checkOverstack) throws CommandSyntaxException {
        ItemStack itemStack = new ItemStack(this.item, amount);
        if (this.tag != null) {
            itemStack.setTag(this.tag);
        }

        if (checkOverstack && amount > itemStack.getMaxStackSize()) {
            throw ERROR_STACK_TOO_BIG.create(Registry.ITEM.getKey(this.item), itemStack.getMaxStackSize());
        } else {
            return itemStack;
        }
    }

    public String serialize() {
        StringBuilder stringBuilder = new StringBuilder(Registry.ITEM.getId(this.item));
        if (this.tag != null) {
            stringBuilder.append((Object)this.tag);
        }

        return stringBuilder.toString();
    }
}
