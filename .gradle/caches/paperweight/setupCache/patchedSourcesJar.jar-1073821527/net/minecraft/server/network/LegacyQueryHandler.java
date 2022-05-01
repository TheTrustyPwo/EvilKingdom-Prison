package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public class LegacyQueryHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int FAKE_PROTOCOL_VERSION = 127;
    private final ServerConnectionListener serverConnectionListener;

    public LegacyQueryHandler(ServerConnectionListener networkIo) {
        this.serverConnectionListener = networkIo;
    }

    public void channelRead(ChannelHandlerContext channelHandlerContext, Object object) {
        ByteBuf byteBuf = (ByteBuf)object;
        byteBuf.markReaderIndex();
        boolean bl = true;

        try {
            if (byteBuf.readUnsignedByte() == 254) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress)channelHandlerContext.channel().remoteAddress();
                MinecraftServer minecraftServer = this.serverConnectionListener.getServer();
                int i = byteBuf.readableBytes();
                switch(i) {
                case 0:
                    LOGGER.debug("Ping: (<1.3.x) from {}:{}", inetSocketAddress.getAddress(), inetSocketAddress.getPort());
                    String string = String.format("%s\u00a7%d\u00a7%d", minecraftServer.getMotd(), minecraftServer.getPlayerCount(), minecraftServer.getMaxPlayers());
                    this.sendFlushAndClose(channelHandlerContext, this.createReply(string));
                    break;
                case 1:
                    if (byteBuf.readUnsignedByte() != 1) {
                        return;
                    }

                    LOGGER.debug("Ping: (1.4-1.5.x) from {}:{}", inetSocketAddress.getAddress(), inetSocketAddress.getPort());
                    String string2 = String.format("\u00a71\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d", 127, minecraftServer.getServerVersion(), minecraftServer.getMotd(), minecraftServer.getPlayerCount(), minecraftServer.getMaxPlayers());
                    this.sendFlushAndClose(channelHandlerContext, this.createReply(string2));
                    break;
                default:
                    boolean bl2 = byteBuf.readUnsignedByte() == 1;
                    bl2 &= byteBuf.readUnsignedByte() == 250;
                    bl2 &= "MC|PingHost".equals(new String(byteBuf.readBytes(byteBuf.readShort() * 2).array(), StandardCharsets.UTF_16BE));
                    int j = byteBuf.readUnsignedShort();
                    bl2 &= byteBuf.readUnsignedByte() >= 73;
                    bl2 &= 3 + byteBuf.readBytes(byteBuf.readShort() * 2).array().length + 4 == j;
                    bl2 &= byteBuf.readInt() <= 65535;
                    bl2 &= byteBuf.readableBytes() == 0;
                    if (!bl2) {
                        return;
                    }

                    LOGGER.debug("Ping: (1.6) from {}:{}", inetSocketAddress.getAddress(), inetSocketAddress.getPort());
                    String string3 = String.format("\u00a71\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d", 127, minecraftServer.getServerVersion(), minecraftServer.getMotd(), minecraftServer.getPlayerCount(), minecraftServer.getMaxPlayers());
                    ByteBuf byteBuf2 = this.createReply(string3);

                    try {
                        this.sendFlushAndClose(channelHandlerContext, byteBuf2);
                    } finally {
                        byteBuf2.release();
                    }
                }

                byteBuf.release();
                bl = false;
                return;
            }
        } catch (RuntimeException var21) {
            return;
        } finally {
            if (bl) {
                byteBuf.resetReaderIndex();
                channelHandlerContext.channel().pipeline().remove("legacy_query");
                channelHandlerContext.fireChannelRead(object);
            }

        }

    }

    private void sendFlushAndClose(ChannelHandlerContext ctx, ByteBuf buf) {
        ctx.pipeline().firstContext().writeAndFlush(buf).addListener(ChannelFutureListener.CLOSE);
    }

    private ByteBuf createReply(String s) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(255);
        char[] cs = s.toCharArray();
        byteBuf.writeShort(cs.length);

        for(char c : cs) {
            byteBuf.writeChar(c);
        }

        return byteBuf;
    }
}
