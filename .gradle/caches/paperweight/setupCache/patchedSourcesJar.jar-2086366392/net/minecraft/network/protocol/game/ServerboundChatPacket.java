package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundChatPacket implements Packet<ServerGamePacketListener> {

    private static final int MAX_MESSAGE_LENGTH = 256;
    private final String message;

    public ServerboundChatPacket(String chatMessage) {
        if (chatMessage.length() > 256) {
            chatMessage = chatMessage.substring(0, 256);
        }

        this.message = chatMessage;
    }

    public ServerboundChatPacket(FriendlyByteBuf buf) {
        this.message = org.apache.commons.lang3.StringUtils.normalizeSpace(buf.readUtf(256)); // CraftBukkit - see PlayerConnection
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.message);
    }

    // Spigot Start
    private static final java.util.concurrent.ExecutorService executors = java.util.concurrent.Executors.newCachedThreadPool(
            new com.google.common.util.concurrent.ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "Async Chat Thread - #%d" ).setUncaughtExceptionHandler(new net.minecraft.DefaultUncaughtExceptionHandlerWithName(net.minecraft.server.MinecraftServer.LOGGER)).build() ); // Paper
    public void handle(final ServerGamePacketListener listener) {
        if ( !this.message.startsWith("/") )
        {
            ServerboundChatPacket.executors.execute( new Runnable() // Paper - Use #execute to propagate exceptions up instead of swallowing them
            {

                @Override
                public void run()
                {
                    listener.handleChat( ServerboundChatPacket.this );
                }
            } );
            return;
        }
        // Spigot End
        listener.handleChat(this);
    }

    public String getMessage() {
        return this.message;
    }
}
