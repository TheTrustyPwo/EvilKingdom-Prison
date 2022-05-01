package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ClientboundSetEquipmentPacket implements Packet<ClientGamePacketListener> {
    private static final byte CONTINUE_MASK = -128;
    private final int entity;
    private final List<Pair<EquipmentSlot, ItemStack>> slots;

    public ClientboundSetEquipmentPacket(int id, List<Pair<EquipmentSlot, ItemStack>> equipmentList) {
        this.entity = id;
        this.slots = equipmentList;
    }

    public ClientboundSetEquipmentPacket(FriendlyByteBuf buf) {
        this.entity = buf.readVarInt();
        EquipmentSlot[] equipmentSlots = EquipmentSlot.values();
        this.slots = Lists.newArrayList();

        int i;
        do {
            i = buf.readByte();
            EquipmentSlot equipmentSlot = equipmentSlots[i & 127];
            ItemStack itemStack = buf.readItem();
            this.slots.add(Pair.of(equipmentSlot, itemStack));
        } while((i & -128) != 0);

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        int i = this.slots.size();

        for(int j = 0; j < i; ++j) {
            Pair<EquipmentSlot, ItemStack> pair = this.slots.get(j);
            EquipmentSlot equipmentSlot = pair.getFirst();
            boolean bl = j != i - 1;
            int k = equipmentSlot.ordinal();
            buf.writeByte(bl ? k | -128 : k);
            buf.writeItem(pair.getSecond());
        }

    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetEquipment(this);
    }

    public int getEntity() {
        return this.entity;
    }

    public List<Pair<EquipmentSlot, ItemStack>> getSlots() {
        return this.slots;
    }
}
