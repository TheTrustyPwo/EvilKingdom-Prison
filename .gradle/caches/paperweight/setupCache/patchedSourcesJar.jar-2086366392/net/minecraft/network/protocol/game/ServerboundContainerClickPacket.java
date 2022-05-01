package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.function.IntFunction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class ServerboundContainerClickPacket implements Packet<ServerGamePacketListener> {
    private static final int MAX_SLOT_COUNT = 128;
    private final int containerId;
    private final int stateId;
    private final int slotNum;
    private final int buttonNum;
    private final ClickType clickType;
    private final ItemStack carriedItem;
    private final Int2ObjectMap<ItemStack> changedSlots;

    public ServerboundContainerClickPacket(int syncId, int revision, int slot, int button, ClickType actionType, ItemStack stack, Int2ObjectMap<ItemStack> modifiedStacks) {
        this.containerId = syncId;
        this.stateId = revision;
        this.slotNum = slot;
        this.buttonNum = button;
        this.clickType = actionType;
        this.carriedItem = stack;
        this.changedSlots = Int2ObjectMaps.unmodifiable(modifiedStacks);
    }

    public ServerboundContainerClickPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readByte();
        this.stateId = buf.readVarInt();
        this.slotNum = buf.readShort();
        this.buttonNum = buf.readByte();
        this.clickType = buf.readEnum(ClickType.class);
        IntFunction<Int2ObjectOpenHashMap<ItemStack>> intFunction = FriendlyByteBuf.limitValue(Int2ObjectOpenHashMap::new, 128);
        this.changedSlots = Int2ObjectMaps.unmodifiable(buf.readMap(intFunction, (bufx) -> {
            return Integer.valueOf(bufx.readShort());
        }, FriendlyByteBuf::readItem));
        this.carriedItem = buf.readItem();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.containerId);
        buf.writeVarInt(this.stateId);
        buf.writeShort(this.slotNum);
        buf.writeByte(this.buttonNum);
        buf.writeEnum(this.clickType);
        buf.writeMap(this.changedSlots, FriendlyByteBuf::writeShort, FriendlyByteBuf::writeItem);
        buf.writeItem(this.carriedItem);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleContainerClick(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getSlotNum() {
        return this.slotNum;
    }

    public int getButtonNum() {
        return this.buttonNum;
    }

    public ItemStack getCarriedItem() {
        return this.carriedItem;
    }

    public Int2ObjectMap<ItemStack> getChangedSlots() {
        return this.changedSlots;
    }

    public ClickType getClickType() {
        return this.clickType;
    }

    public int getStateId() {
        return this.stateId;
    }
}
