package net.minecraft.network.protocol;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;

public interface Packet<T extends PacketListener> {
    void write(FriendlyByteBuf buf);

    void handle(T listener);

    // Paper start
    /**
     * @param player Null if not at PLAY stage yet
     */
    default void onPacketDispatch(@javax.annotation.Nullable net.minecraft.server.level.ServerPlayer player) {}

    /**
     * @param player Null if not at PLAY stage yet
     * @param future Can be null if packet was cancelled
     */
    default void onPacketDispatchFinish(@javax.annotation.Nullable net.minecraft.server.level.ServerPlayer player, @javax.annotation.Nullable io.netty.channel.ChannelFuture future) {}
    default boolean hasFinishListener() { return false; }
    default boolean isReady() { return true; }
    default java.util.List<Packet> getExtraPackets() { return null; }
    default boolean packetTooLarge(net.minecraft.network.Connection manager) {
        return false;
    }
    // Paper end

    default boolean isSkippable() {
        return false;
    }
}
