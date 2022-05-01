package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;

public class ServerboundSetCreativeModeSlotPacket implements Packet<ServerGamePacketListener> {
    private final int slotNum;
    private final ItemStack itemStack;

    public ServerboundSetCreativeModeSlotPacket(int slot, ItemStack stack) {
        this.slotNum = slot;
        this.itemStack = stack.copy();
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSetCreativeModeSlot(this);
    }

    public ServerboundSetCreativeModeSlotPacket(FriendlyByteBuf buf) {
        this.slotNum = buf.readShort();
        this.itemStack = buf.readItem();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeShort(this.slotNum);
        buf.writeItem(this.itemStack);
    }

    public int getSlotNum() {
        return this.slotNum;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }
}
