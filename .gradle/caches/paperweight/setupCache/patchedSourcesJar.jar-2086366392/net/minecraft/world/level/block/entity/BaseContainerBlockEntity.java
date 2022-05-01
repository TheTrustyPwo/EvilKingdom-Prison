package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseContainerBlockEntity extends BlockEntity implements Container, MenuProvider, Nameable {

    public LockCode lockKey;
    public Component name;

    protected BaseContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.lockKey = LockCode.NO_LOCK;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.lockKey = LockCode.fromTag(nbt);
        if (nbt.contains("CustomName", 8)) {
            this.name = net.minecraft.server.MCUtil.getBaseComponentFromNbt("CustomName", nbt); // Paper - Catch ParseException
        }

    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        this.lockKey.addToTag(nbt);
        if (this.name != null) {
            nbt.putString("CustomName", Component.Serializer.toJson(this.name));
        }

    }

    public void setCustomName(Component customName) {
        this.name = customName;
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : this.getDefaultName();
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    protected abstract Component getDefaultName();

    public boolean canOpen(Player player) {
        return BaseContainerBlockEntity.canUnlock(player, this.lockKey, this.getDisplayName());
    }

    public static boolean canUnlock(Player player, LockCode lock, Component containerName) {
        if (!player.isSpectator() && !lock.unlocksWith(player.getMainHandItem())) {
            player.displayClientMessage(new TranslatableComponent("container.isLocked", new Object[]{containerName}), true);
            player.playNotifySound(SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
            return false;
        } else {
            return true;
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return this.canOpen(player) ? this.createMenu(syncId, inv) : null;
    }

    protected abstract AbstractContainerMenu createMenu(int syncId, Inventory playerInventory);

    // CraftBukkit start
    @Override
    public org.bukkit.Location getLocation() {
        if (level == null) return null;
        return new org.bukkit.Location(level.getWorld(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }
    // CraftBukkit end
}
