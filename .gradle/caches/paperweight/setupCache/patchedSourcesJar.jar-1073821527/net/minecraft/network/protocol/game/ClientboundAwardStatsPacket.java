package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;

public class ClientboundAwardStatsPacket implements Packet<ClientGamePacketListener> {
    private final Object2IntMap<Stat<?>> stats;

    public ClientboundAwardStatsPacket(Object2IntMap<Stat<?>> stats) {
        this.stats = stats;
    }

    public ClientboundAwardStatsPacket(FriendlyByteBuf buf) {
        this.stats = buf.readMap(Object2IntOpenHashMap::new, (bufx) -> {
            int i = bufx.readVarInt();
            int j = bufx.readVarInt();
            return readStatCap(Registry.STAT_TYPE.byId(i), j);
        }, FriendlyByteBuf::readVarInt);
    }

    private static <T> Stat<T> readStatCap(StatType<T> statType, int id) {
        return statType.get(statType.getRegistry().byId(id));
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleAwardStats(this);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeMap(this.stats, (bufx, stat) -> {
            bufx.writeVarInt(Registry.STAT_TYPE.getId(stat.getType()));
            bufx.writeVarInt(this.getStatIdCap(stat));
        }, FriendlyByteBuf::writeVarInt);
    }

    private <T> int getStatIdCap(Stat<T> stat) {
        return stat.getType().getRegistry().getId(stat.getValue());
    }

    public Map<Stat<?>, Integer> getStats() {
        return this.stats;
    }
}
