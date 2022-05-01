package net.minecraft.world;

import javax.annotation.concurrent.Immutable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

@Immutable
public class LockCode {
    public static final LockCode NO_LOCK = new LockCode("");
    public static final String TAG_LOCK = "Lock";
    public final String key;

    public LockCode(String key) {
        this.key = key;
    }

    public boolean unlocksWith(ItemStack stack) {
        return this.key.isEmpty() || !stack.isEmpty() && stack.hasCustomHoverName() && this.key.equals(stack.getHoverName().getString());
    }

    public void addToTag(CompoundTag nbt) {
        if (!this.key.isEmpty()) {
            nbt.putString("Lock", this.key);
        }

    }

    public static LockCode fromTag(CompoundTag nbt) {
        return nbt.contains("Lock", 8) ? new LockCode(nbt.getString("Lock")) : NO_LOCK;
    }
}
