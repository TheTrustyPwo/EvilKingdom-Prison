package net.minecraft.network;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


import io.netty.util.concurrent.AbstractEventExecutor; // Paper
public class Connection extends SimpleChannelInboundHandler<Packet<?>> {

    private static final float AVERAGE_PACKETS_SMOOTHING = 0.75F;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Marker ROOT_MARKER = MarkerFactory.getMarker("NETWORK");
    public static final Marker PACKET_MARKER = (Marker) Util.make(MarkerFactory.getMarker("NETWORK_PACKETS"), (marker) -> {
        marker.add(Connection.ROOT_MARKER);
    });
    public static final Marker PACKET_RECEIVED_MARKER = (Marker) Util.make(MarkerFactory.getMarker("PACKET_RECEIVED"), (marker) -> {
        marker.add(Connection.PACKET_MARKER);
    });
    public static final Marker PACKET_SENT_MARKER = (Marker) Util.make(MarkerFactory.getMarker("PACKET_SENT"), (marker) -> {
        marker.add(Connection.PACKET_MARKER);
    });
    public static final AttributeKey<ConnectionProtocol> ATTRIBUTE_PROTOCOL = AttributeKey.valueOf("protocol");
    public static final LazyLoadedValue<NioEventLoopGroup> NETWORK_WORKER_GROUP = new LazyLoadedValue<>(() -> {
        return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).setUncaughtExceptionHandler(new net.minecraft.DefaultUncaughtExceptionHandlerWithName(LOGGER)).build()); // Paper
    });
    public static final LazyLoadedValue<EpollEventLoopGroup> NETWORK_EPOLL_WORKER_GROUP = new LazyLoadedValue<>(() -> {
        return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).setUncaughtExceptionHandler(new net.minecraft.DefaultUncaughtExceptionHandlerWithName(LOGGER)).build()); // Paper
    });
    public static final LazyLoadedValue<DefaultEventLoopGroup> LOCAL_WORKER_GROUP = new LazyLoadedValue<>(() -> {
        return new DefaultEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).setUncaughtExceptionHandler(new net.minecraft.DefaultUncaughtExceptionHandlerWithName(LOGGER)).build()); // Paper
    });
    private final PacketFlow receiving;
    private final Queue<Connection.PacketHolder> queue = Queues.newConcurrentLinkedQueue();
    public Channel channel;
    public SocketAddress address;
    // Spigot Start
    public java.util.UUID spoofedUUID;
    public com.mojang.authlib.properties.Property[] spoofedProfile;
    public boolean preparing = true;
    // Spigot End
    private PacketListener packetListener;
    private Component disconnectedReason;
    private boolean encrypted;
    private boolean disconnectionHandled;
    private int receivedPackets;
    private int sentPackets;
    private float averageReceivedPackets;
    private float averageSentPackets;
    private int tickCount;
    private boolean handlingFault;
    // Paper start - NetworkClient implementation
    public int protocolVersion;
    public java.net.InetSocketAddress virtualHost;
    private static boolean enableExplicitFlush = Boolean.getBoolean("paper.explicit-flush");
    // Optimize network
    public boolean isPending = true;
    public boolean queueImmunity = false;
    public ConnectionProtocol protocol;
    // Paper end
    // Paper start - add pending task queue
    private final Queue<Runnable> pendingTasks = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public void execute(final Runnable run) {
        if (this.channel == null || !this.channel.isRegistered()) {
            run.run();
            return;
        }
        final boolean queue = !this.queue.isEmpty();
        if (!queue) {
            this.channel.eventLoop().execute(run);
        } else {
            this.pendingTasks.add(run);
            if (this.queue.isEmpty()) {
                // something flushed async, dump tasks now
                Runnable r;
                while ((r = this.pendingTasks.poll()) != null) {
                    this.channel.eventLoop().execute(r);
                }
            }
        }
    }
    // Paper end - add pending task queue

    // Paper start - allow controlled flushing
    volatile boolean canFlush = true;
    private final java.util.concurrent.atomic.AtomicInteger packetWrites = new java.util.concurrent.atomic.AtomicInteger();
    private int flushPacketsStart;
    private final Object flushLock = new Object();

    public void disableAutomaticFlush() {
        synchronized (this.flushLock) {
            this.flushPacketsStart = this.packetWrites.get(); // must be volatile and before canFlush = false
            this.canFlush = false;
        }
    }

    public void enableAutomaticFlush() {
        synchronized (this.flushLock) {
            this.canFlush = true;
            if (this.packetWrites.get() != this.flushPacketsStart) { // must be after canFlush = true
                this.flush(); // only make the flush call if we need to
            }
        }
    }

    private final void flush() {
        if (this.channel.eventLoop().inEventLoop()) {
            this.channel.flush();
        } else {
            this.channel.eventLoop().execute(() -> {
                this.channel.flush();
            });
        }
    }
    // Paper end - allow controlled flushing
    // Paper start - packet limiter
    protected final Object PACKET_LIMIT_LOCK = new Object();
    protected final io.papermc.paper.util.IntervalledCounter allPacketCounts = com.destroystokyo.paper.PaperConfig.allPacketsLimit != null ? new io.papermc.paper.util.IntervalledCounter(
            (long)(com.destroystokyo.paper.PaperConfig.allPacketsLimit.packetLimitInterval * 1.0e9)
    ) : null;
    protected final java.util.Map<Class<? extends net.minecraft.network.protocol.Packet<?>>, io.papermc.paper.util.IntervalledCounter> packetSpecificLimits = new java.util.HashMap<>();

    private boolean stopReadingPackets;
    private void killForPacketSpam() {
        this.sendPacket(new ClientboundDisconnectPacket(org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage.fromString(com.destroystokyo.paper.PaperConfig.kickMessage, true)[0]), (future) -> {
            this.disconnect(org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage.fromString(com.destroystokyo.paper.PaperConfig.kickMessage, true)[0]);
        });
        this.setReadOnly();
        this.stopReadingPackets = true;
    }
    // Paper end - packet limiter

    public Connection(PacketFlow side) {
        this.receiving = side;
    }

    public void channelActive(ChannelHandlerContext channelhandlercontext) throws Exception {
        super.channelActive(channelhandlercontext);
        this.channel = channelhandlercontext.channel();
        this.address = this.channel.remoteAddress();
        // Spigot Start
        this.preparing = false;
        // Spigot End

        try {
            this.setProtocol(ConnectionProtocol.HANDSHAKING);
        } catch (Throwable throwable) {
            Connection.LOGGER.error(LogUtils.FATAL_MARKER, "Failed to change protocol to handshake", throwable);
        }

    }

    public void setProtocol(ConnectionProtocol state) {
        protocol = state; // Paper
        this.channel.attr(Connection.ATTRIBUTE_PROTOCOL).set(state);
        this.channel.config().setAutoRead(true);
        Connection.LOGGER.debug("Enabled auto read");
    }

    public void channelInactive(ChannelHandlerContext channelhandlercontext) {
        this.disconnect(new TranslatableComponent("disconnect.endOfStream"));
    }

    public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) {
        // Paper start
        if (throwable instanceof io.netty.handler.codec.EncoderException && throwable.getCause() instanceof PacketEncoder.PacketTooLargeException) {
            if (((PacketEncoder.PacketTooLargeException) throwable.getCause()).getPacket().packetTooLarge(this)) {
                return;
            } else {
                throwable = throwable.getCause();
            }
        }
        // Paper end
        if (throwable instanceof SkipPacketException) {
            Connection.LOGGER.debug("Skipping packet due to errors", throwable.getCause());
        } else {
            boolean flag = !this.handlingFault;

            this.handlingFault = true;
            if (this.channel.isOpen()) {
                net.minecraft.server.level.ServerPlayer player = this.getPlayer(); // Paper
                if (throwable instanceof TimeoutException) {
                    Connection.LOGGER.debug("Timeout", throwable);
                    if (player != null) player.quitReason = org.bukkit.event.player.PlayerQuitEvent.QuitReason.TIMED_OUT; // Paper
                    this.disconnect(new TranslatableComponent("disconnect.timeout"));
                } else {
                    TranslatableComponent chatmessage = new TranslatableComponent("disconnect.genericReason", new Object[]{"Internal Exception: " + throwable});

                    if (player != null) player.quitReason = org.bukkit.event.player.PlayerQuitEvent.QuitReason.ERRONEOUS_STATE; // Paper
                    if (flag) {
                        Connection.LOGGER.debug("Failed to sent packet", throwable);
                        ConnectionProtocol enumprotocol = this.getCurrentProtocol();
                        Packet<?> packet = enumprotocol == ConnectionProtocol.LOGIN ? new ClientboundLoginDisconnectPacket(chatmessage) : new ClientboundDisconnectPacket(chatmessage);

                        this.send((Packet) packet, (future) -> {
                            this.disconnect(chatmessage);
                        });
                        this.setReadOnly();
                    } else {
                        Connection.LOGGER.debug("Double fault", throwable);
                        this.disconnect(chatmessage);
                    }
                }

            }
        }
        if (net.minecraft.server.MinecraftServer.getServer().isDebugging()) throwable.printStackTrace(); // Spigot
    }

    protected void channelRead0(ChannelHandlerContext channelhandlercontext, Packet<?> packet) {
        if (this.channel.isOpen()) {
            // Paper start - packet limiter
            if (this.stopReadingPackets) {
                return;
            }
            if (this.allPacketCounts != null ||
                    com.destroystokyo.paper.PaperConfig.packetSpecificLimits.containsKey(packet.getClass())) {
                long time = System.nanoTime();
                synchronized (PACKET_LIMIT_LOCK) {
                    if (this.allPacketCounts != null) {
                        this.allPacketCounts.updateAndAdd(1, time);
                        if (this.allPacketCounts.getRate() >= com.destroystokyo.paper.PaperConfig.allPacketsLimit.maxPacketRate) {
                            this.killForPacketSpam();
                            return;
                        }
                    }

                    for (Class<?> check = packet.getClass(); check != Object.class; check = check.getSuperclass()) {
                        com.destroystokyo.paper.PaperConfig.PacketLimit packetSpecificLimit =
                            com.destroystokyo.paper.PaperConfig.packetSpecificLimits.get(check);
                        if (packetSpecificLimit == null) {
                            continue;
                        }
                        io.papermc.paper.util.IntervalledCounter counter = this.packetSpecificLimits.computeIfAbsent((Class)check, (clazz) -> {
                            return new io.papermc.paper.util.IntervalledCounter((long)(packetSpecificLimit.packetLimitInterval * 1.0e9));
                        });
                        counter.updateAndAdd(1, time);
                        if (counter.getRate() >= packetSpecificLimit.maxPacketRate) {
                            switch (packetSpecificLimit.violateAction) {
                                case DROP:
                                    return;
                                case KICK:
                                    this.killForPacketSpam();
                                    return;
                            }
                        }
                    }
                }
            }
            // Paper end - packet limiter
            try {
                Connection.genericsFtw(packet, this.packetListener);
            } catch (RunningOnDifferentThreadException cancelledpackethandleexception) {
                ;
            } catch (RejectedExecutionException rejectedexecutionexception) {
                this.disconnect(new TranslatableComponent("multiplayer.disconnect.server_shutdown"));
            } catch (ClassCastException classcastexception) {
                Connection.LOGGER.error("Received {} that couldn't be processed", packet.getClass(), classcastexception);
                this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_packet"));
            }

            ++this.receivedPackets;
        }

    }

    private static <T extends PacketListener> void genericsFtw(Packet<T> packet, PacketListener listener) {
        packet.handle((T) listener); // CraftBukkit - decompile error
    }

    public void setListener(PacketListener listener) {
        Validate.notNull(listener, "packetListener", new Object[0]);
        this.packetListener = listener;
    }
    // Paper start
    public net.minecraft.server.level.ServerPlayer getPlayer() {
        if (packetListener instanceof ServerGamePacketListenerImpl) {
            return ((ServerGamePacketListenerImpl) packetListener).player;
        } else {
            return null;
        }
    }
    private static class InnerUtil { // Attempt to hide these methods from ProtocolLib so it doesn't accidently pick them up.
        private static java.util.List<Packet> buildExtraPackets(Packet packet) {
            java.util.List<Packet> extra = packet.getExtraPackets();
            if (extra == null || extra.isEmpty()) {
                return null;
            }
            java.util.List<Packet> ret = new java.util.ArrayList<>(1 + extra.size());
            buildExtraPackets0(extra, ret);
            return ret;
        }

        private static void buildExtraPackets0(java.util.List<Packet> extraPackets, java.util.List<Packet> into) {
            for (Packet extra : extraPackets) {
                into.add(extra);
                java.util.List<Packet> extraExtra = extra.getExtraPackets();
                if (extraExtra != null && !extraExtra.isEmpty()) {
                    buildExtraPackets0(extraExtra, into);
                }
            }
        }
        // Paper start
        private static boolean canSendImmediate(Connection networkManager, Packet<?> packet) {
            return networkManager.isPending || networkManager.protocol != ConnectionProtocol.PLAY ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundKeepAlivePacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundChatPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundClearTitlesPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundBossEventPacket;
        }
        // Paper end
    }
    // Paper end

    public void send(Packet<?> packet) {
        this.send(packet, (GenericFutureListener) null);
    }

    public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        // Paper start - handle oversized packets better
        boolean connected = this.isConnected();
        if (!connected && !preparing) {
            return; // Do nothing
        }
        packet.onPacketDispatch(getPlayer());
        if (connected && (InnerUtil.canSendImmediate(this, packet) || (
            net.minecraft.server.MCUtil.isMainThread() && packet.isReady() && this.queue.isEmpty() &&
            (packet.getExtraPackets() == null || packet.getExtraPackets().isEmpty())
        ))) {
            this.writePacket(packet, callback, null); // Paper
            return;
        }
        // write the packets to the queue, then flush - antixray hooks there already
        java.util.List<Packet> extraPackets = InnerUtil.buildExtraPackets(packet);
        boolean hasExtraPackets = extraPackets != null && !extraPackets.isEmpty();
        if (!hasExtraPackets) {
            this.queue.add(new Connection.PacketHolder(packet, callback));
        } else {
            java.util.List<Connection.PacketHolder> packets = new java.util.ArrayList<>(1 + extraPackets.size());
            packets.add(new Connection.PacketHolder(packet, null)); // delay the future listener until the end of the extra packets

            for (int i = 0, len = extraPackets.size(); i < len;) {
                Packet extra = extraPackets.get(i);
                boolean end = ++i == len;
                packets.add(new Connection.PacketHolder(extra, end ? callback : null)); // append listener to the end
            }
            this.queue.addAll(packets); // atomic
        }
        this.flushQueue();
        // Paper end
    }

    private void sendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        // Paper start - add flush parameter
        this.writePacket(packet, callback, Boolean.TRUE);
    }
    private void writePacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, Boolean flushConditional) {
        this.packetWrites.getAndIncrement(); // must be befeore using canFlush
        boolean effectiveFlush = flushConditional == null ? this.canFlush : flushConditional.booleanValue();
        final boolean flush = effectiveFlush || packet instanceof net.minecraft.network.protocol.game.ClientboundKeepAlivePacket || packet instanceof ClientboundDisconnectPacket; // no delay for certain packets
        // Paper end - add flush parameter
        ConnectionProtocol enumprotocol = ConnectionProtocol.getProtocolForPacket(packet);
        ConnectionProtocol enumprotocol1 = this.getCurrentProtocol();

        ++this.sentPackets;
        if (enumprotocol1 != enumprotocol) {
            Connection.LOGGER.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop()) {
            this.doSendPacket(packet, callback, enumprotocol, enumprotocol1, flush); // Paper - add flush parameter
        } else {
            // Paper start - optimise packets that are not flushed
            // note: since the type is not dynamic here, we need to actually copy the old executor code
            // into two branches. On conflict, just re-copy - no changes were made inside the executor code.
            if (!flush) {
                AbstractEventExecutor.LazyRunnable run = () -> {
                    this.doSendPacket(packet, callback, enumprotocol, enumprotocol1, flush); // Paper - add flush parameter
                };
                this.channel.eventLoop().execute(run);
            } else { // Paper end - optimise packets that are not flushed
            this.channel.eventLoop().execute(() -> {
                this.doSendPacket(packet, callback, enumprotocol, enumprotocol1, flush); // Paper - add flush parameter // Paper - diff on change
            });
            } // Paper
        }

    }

    private void doSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, ConnectionProtocol packetState, ConnectionProtocol currentState) {
        // Paper start - add flush parameter
        this.doSendPacket(packet, callback, packetState, currentState, true);
    }
    private void doSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, ConnectionProtocol packetState, ConnectionProtocol currentState, boolean flush) {
        // Paper end - add flush parameter
        if (packetState != currentState) {
            this.setProtocol(packetState);
        }

        // Paper start
        net.minecraft.server.level.ServerPlayer player = getPlayer();
        if (!isConnected()) {
            packet.onPacketDispatchFinish(player, null);
            return;
        }

        try {
            // Paper end
        ChannelFuture channelfuture = flush ? this.channel.writeAndFlush(packet) : this.channel.write(packet); // Paper - add flush parameter

        if (callback != null) {
            channelfuture.addListener(callback);
        }
        // Paper start
        if (packet.hasFinishListener()) {
            channelfuture.addListener((ChannelFutureListener) channelFuture -> packet.onPacketDispatchFinish(player, channelFuture));
        }
        // Paper end

        channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        // Paper start
        } catch (Exception e) {
            LOGGER.error("NetworkException: " + player, e);
            disconnect(new net.minecraft.network.chat.TranslatableComponent("disconnect.genericReason", "Internal Exception: " + e.getMessage()));
            packet.onPacketDispatchFinish(player, null);
        }
        // Paper end
    }

    private ConnectionProtocol getCurrentProtocol() {
        return (ConnectionProtocol) this.channel.attr(Connection.ATTRIBUTE_PROTOCOL).get();
    }

    // Paper start - rewrite this to be safer if ran off main thread
    private boolean flushQueue() { // void -> boolean
        if (!isConnected()) {
            return true;
        }
        if (net.minecraft.server.MCUtil.isMainThread()) {
            return processQueue();
        } else if (isPending) {
            // Should only happen during login/status stages
            synchronized (this.queue) {
                return this.processQueue();
            }
        }
        return false;
    }
    private boolean processQueue() {
        try { // Paper - add pending task queue
        if (this.queue.isEmpty()) return true;
        // Paper start - make only one flush call per sendPacketQueue() call
        final boolean needsFlush = this.canFlush;
        boolean hasWrotePacket = false;
        // Paper end - make only one flush call per sendPacketQueue() call
        // If we are on main, we are safe here in that nothing else should be processing queue off main anymore
        // But if we are not on main due to login/status, the parent is synchronized on packetQueue
        java.util.Iterator<PacketHolder> iterator = this.queue.iterator();
        while (iterator.hasNext()) {
            PacketHolder queued = iterator.next(); // poll -> peek

            // Fix NPE (Spigot bug caused by handleDisconnection())
            if (false && queued == null) { // Paper - diff on change, this logic is redundant: iterator guarantees ret of an element - on change, hook the flush logic here
                return true;
            }

            Packet<?> packet = queued.packet;
            if (!packet.isReady()) {
                // Paper start - make only one flush call per sendPacketQueue() call
                if (hasWrotePacket && (needsFlush || this.canFlush)) {
                    this.flush();
                }
                // Paper end - make only one flush call per sendPacketQueue() call
                return false;
            } else {
                iterator.remove();
                this.writePacket(packet, queued.listener, (!iterator.hasNext() && (needsFlush || this.canFlush)) ? Boolean.TRUE : Boolean.FALSE); // Paper - make only one flush call per sendPacketQueue() call
                hasWrotePacket = true; // Paper - make only one flush call per sendPacketQueue() call
            }
        }
        return true;
        } finally { // Paper start - add pending task queue
            Runnable r;
            while ((r = this.pendingTasks.poll()) != null) {
                this.channel.eventLoop().execute(r);
            }
        } // Paper end - add pending task queue
    }
    // Paper end

    private static final int MAX_PER_TICK = com.destroystokyo.paper.PaperConfig.maxJoinsPerTick; // Paper
    private static int joinAttemptsThisTick; // Paper
    private static int currTick; // Paper
    public void tick() {
        this.flushQueue();
        // Paper start
        if (currTick != MinecraftServer.currentTick) {
            currTick = MinecraftServer.currentTick;
            joinAttemptsThisTick = 0;
        }
        // Paper end
        if (this.packetListener instanceof ServerLoginPacketListenerImpl) {
            if ( ((ServerLoginPacketListenerImpl) this.packetListener).state != ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT // Paper
                     || (joinAttemptsThisTick++ < MAX_PER_TICK)) { // Paper - limit the number of joins which can be processed each tick
            ((ServerLoginPacketListenerImpl) this.packetListener).tick();
            } // Paper
        }

        if (this.packetListener instanceof ServerGamePacketListenerImpl) {
            // Paper start - detailed watchdog information
            net.minecraft.network.protocol.PacketUtils.packetProcessing.push(this.packetListener);
            try {
            // Paper end - detailed watchdog information
            ((ServerGamePacketListenerImpl) this.packetListener).tick();
            } finally { // Paper start - detailed watchdog information
                net.minecraft.network.protocol.PacketUtils.packetProcessing.pop();
            } // Paper start - detailed watchdog information
        }

        if (!this.isConnected() && !this.disconnectionHandled) {
            this.handleDisconnection();
        }

        if (this.channel != null) {
            if (enableExplicitFlush) this.channel.eventLoop().execute(() -> this.channel.flush()); // Paper - we don't need to explicit flush here, but allow opt in incase issues are found to a better version
        }

        if (this.tickCount++ % 20 == 0) {
            this.tickSecond();
        }

    }

    protected void tickSecond() {
        this.averageSentPackets = Mth.lerp(0.75F, (float) this.sentPackets, this.averageSentPackets);
        this.averageReceivedPackets = Mth.lerp(0.75F, (float) this.receivedPackets, this.averageReceivedPackets);
        this.sentPackets = 0;
        this.receivedPackets = 0;
    }

    public SocketAddress getRemoteAddress() {
        return this.address;
    }

    // Paper start
    public void clearPacketQueue() {
        net.minecraft.server.level.ServerPlayer player = getPlayer();
        queue.forEach(queuedPacket -> {
            Packet<?> packet = queuedPacket.packet;
            if (packet.hasFinishListener()) {
                packet.onPacketDispatchFinish(player, null);
            }
        });
        queue.clear();
    }
    // Paper end
    public void disconnect(Component disconnectReason) {
        // Spigot Start
        this.preparing = false;
        clearPacketQueue(); // Paper
        // Spigot End
        if (this.channel.isOpen()) {
            this.channel.close(); // We can't wait as this may be called from an event loop.
            this.disconnectedReason = disconnectReason;
        }

    }

    public boolean isMemoryConnection() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public PacketFlow getReceiving() {
        return this.receiving;
    }

    public PacketFlow getSending() {
        return this.receiving.getOpposite();
    }

    public static Connection connectToServer(InetSocketAddress address, boolean useEpoll) {
        final Connection networkmanager = new Connection(PacketFlow.CLIENTBOUND);
        Class oclass;
        LazyLoadedValue lazyinitvar;

        if (Epoll.isAvailable() && useEpoll) {
            oclass = EpollSocketChannel.class;
            lazyinitvar = Connection.NETWORK_EPOLL_WORKER_GROUP;
        } else {
            oclass = NioSocketChannel.class;
            lazyinitvar = Connection.NETWORK_WORKER_GROUP;
        }

        ((Bootstrap) ((Bootstrap) ((Bootstrap) (new Bootstrap()).group((EventLoopGroup) lazyinitvar.get())).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException channelexception) {
                    ;
                }

                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("splitter", new Varint21FrameDecoder()).addLast("decoder", new PacketDecoder(PacketFlow.CLIENTBOUND)).addLast("prepender", new Varint21LengthFieldPrepender()).addLast("encoder", new PacketEncoder(PacketFlow.SERVERBOUND)).addLast("packet_handler", networkmanager);
            }
        })).channel(oclass)).connect(address.getAddress(), address.getPort()).syncUninterruptibly();
        return networkmanager;
    }

    public static Connection connectToLocalServer(SocketAddress address) {
        final Connection networkmanager = new Connection(PacketFlow.CLIENTBOUND);

        ((Bootstrap) ((Bootstrap) ((Bootstrap) (new Bootstrap()).group((EventLoopGroup) Connection.LOCAL_WORKER_GROUP.get())).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast("packet_handler", networkmanager);
            }
        })).channel(LocalChannel.class)).connect(address).syncUninterruptibly();
        return networkmanager;
    }

    // Paper start
//    public void setEncryptionKey(Cipher decryptionCipher, Cipher encryptionCipher) {
//        this.encrypted = true;
//        this.channel.pipeline().addBefore("splitter", "decrypt", new CipherDecoder(decryptionCipher));
//        this.channel.pipeline().addBefore("prepender", "encrypt", new CipherEncoder(encryptionCipher));
//    }

    public void setupEncryption(javax.crypto.SecretKey key) throws net.minecraft.util.CryptException {
        if (!this.encrypted) {
            try {
                com.velocitypowered.natives.encryption.VelocityCipher decryption = com.velocitypowered.natives.util.Natives.cipher.get().forDecryption(key);
                com.velocitypowered.natives.encryption.VelocityCipher encryption = com.velocitypowered.natives.util.Natives.cipher.get().forEncryption(key);

                this.encrypted = true;
                this.channel.pipeline().addBefore("splitter", "decrypt", new CipherDecoder(decryption));
                this.channel.pipeline().addBefore("prepender", "encrypt", new CipherEncoder(encryption));
            } catch (java.security.GeneralSecurityException e) {
                throw new net.minecraft.util.CryptException(e);
            }
        }
    }
    // Paper end

    public boolean isEncrypted() {
        return this.encrypted;
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean isConnecting() {
        return this.channel == null;
    }

    public PacketListener getPacketListener() {
        return this.packetListener;
    }

    @Nullable
    public Component getDisconnectedReason() {
        return this.disconnectedReason;
    }

    public void setReadOnly() {
        this.channel.config().setAutoRead(false);
    }

    public void setupCompression(int compressionThreshold, boolean rejectsBadPackets) {
        if (compressionThreshold >= 0) {
            com.velocitypowered.natives.compression.VelocityCompressor compressor = com.velocitypowered.natives.util.Natives.compress.get().create(-1); // Paper
            if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
                ((CompressionDecoder) this.channel.pipeline().get("decompress")).setThreshold(compressionThreshold, rejectsBadPackets);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new CompressionDecoder(compressor, compressionThreshold, rejectsBadPackets)); // Paper
            }

            if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
                ((CompressionEncoder) this.channel.pipeline().get("compress")).setThreshold(compressionThreshold);
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new CompressionEncoder(compressor, compressionThreshold)); // Paper
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
                this.channel.pipeline().remove("compress");
            }
        }

    }

    public void handleDisconnection() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (this.disconnectionHandled) {
                //Connection.LOGGER.warn("handleDisconnection() called twice"); // Paper - Do not log useless message
            } else {
                this.disconnectionHandled = true;
                if (this.getDisconnectedReason() != null) {
                    this.getPacketListener().onDisconnect(this.getDisconnectedReason());
                } else if (this.getPacketListener() != null) {
                    this.getPacketListener().onDisconnect(new TranslatableComponent("multiplayer.disconnect.generic"));
                }
                clearPacketQueue(); // Paper
                // Paper start - Add PlayerConnectionCloseEvent
                final PacketListener packetListener = this.getPacketListener();
                if (packetListener instanceof ServerGamePacketListenerImpl) {
                    /* Player was logged in */
                    final ServerGamePacketListenerImpl playerConnection = (ServerGamePacketListenerImpl) packetListener;
                    new com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent(playerConnection.player.getUUID(),
                        playerConnection.player.getScoreboardName(), ((java.net.InetSocketAddress)address).getAddress(), false).callEvent();
                } else if (packetListener instanceof ServerLoginPacketListenerImpl) {
                    /* Player is login stage */
                    final ServerLoginPacketListenerImpl loginListener = (ServerLoginPacketListenerImpl) packetListener;
                    switch (loginListener.state) {
                        case READY_TO_ACCEPT:
                        case DELAY_ACCEPT:
                        case ACCEPTED:
                            final com.mojang.authlib.GameProfile profile = loginListener.gameProfile; /* Should be non-null at this stage */
                            new com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent(profile.getId(), profile.getName(),
                                ((java.net.InetSocketAddress)address).getAddress(), false).callEvent();
                    }
                }
                // Paper end
            }

        }
    }

    public float getAverageReceivedPackets() {
        return this.averageReceivedPackets;
    }

    public float getAverageSentPackets() {
        return this.averageSentPackets;
    }

    private static class PacketHolder {

        final Packet<?> packet;
        @Nullable
        final GenericFutureListener<? extends Future<? super Void>> listener;

        public PacketHolder(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
            this.packet = packet;
            this.listener = callback;
        }
    }

    // Spigot Start
    public SocketAddress getRawAddress()
    {
        // Paper start - this can be nullable in the case of a Unix domain socket, so if it is, fake something
        if (this.channel.remoteAddress() == null) {
            return new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 0);
        }
        // Paper end
        return this.channel.remoteAddress();
    }
    // Spigot End
}
