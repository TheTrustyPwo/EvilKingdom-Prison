package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.papermc.paper.adventure.PaperAdventure; // Paper
import java.io.IOException;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PacketFlow flow;

    public PacketEncoder(PacketFlow side) {
        this.flow = side;
    }

    protected void encode(ChannelHandlerContext channelHandlerContext, Packet<?> packet, ByteBuf byteBuf) throws Exception {
        ConnectionProtocol connectionProtocol = channelHandlerContext.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
        if (connectionProtocol == null) {
            throw new RuntimeException("ConnectionProtocol unknown: " + packet);
        } else {
            Integer integer = connectionProtocol.getPacketId(this.flow, packet);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(Connection.PACKET_SENT_MARKER, "OUT: [{}:{}] {}", channelHandlerContext.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get(), integer, packet.getClass().getName());
            }

            if (integer == null) {
                throw new IOException("Can't serialize unregistered packet");
            } else {
                FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(byteBuf);
                friendlyByteBuf.writeVarInt(integer);
                friendlyByteBuf.adventure$locale = channelHandlerContext.channel().attr(PaperAdventure.LOCALE_ATTRIBUTE).get(); // Paper

                try {
                    int i = friendlyByteBuf.writerIndex();
                    packet.write(friendlyByteBuf);
                    int j = friendlyByteBuf.writerIndex() - i;
                    if (j > 8388608) {
                        throw new IllegalArgumentException("Packet too big (is " + j + ", should be less than 8388608): " + packet);
                    } else {
                        int k = channelHandlerContext.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get().getId();
                        JvmProfiler.INSTANCE.onPacketSent(k, integer, channelHandlerContext.channel().remoteAddress(), j);
                    }
                } catch (Throwable var10) {
                    LOGGER.error("Packet encoding of packet ID {} threw (skippable? {})", integer, packet.isSkippable(), var10); // Paper - Give proper error message
                    if (packet.isSkippable()) {
                        throw new SkipPacketException(var10);
                    } else {
                        throw var10;
                    }
                }

                // Paper start
                int packetLength = friendlyByteBuf.readableBytes();
                if (packetLength > MAX_PACKET_SIZE) {
                    throw new PacketTooLargeException(packet, packetLength);
                }
                // Paper end
            }
        }
    }

    // Paper start
    private static int MAX_PACKET_SIZE = 2097152;

    public static class PacketTooLargeException extends RuntimeException {
        private final Packet<?> packet;

        PacketTooLargeException(Packet<?> packet, int packetLength) {
            super("PacketTooLarge - " + packet.getClass().getSimpleName() + " is " + packetLength + ". Max is " + MAX_PACKET_SIZE);
            this.packet = packet;
        }

        public Packet<?> getPacket() {
            return packet;
        }
    }
    // Paper end
}
