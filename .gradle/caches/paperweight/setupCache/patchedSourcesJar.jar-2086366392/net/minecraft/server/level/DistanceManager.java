package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap; // Paper
public abstract class DistanceManager {

    static final Logger LOGGER = LogUtils.getLogger();
    private static final int ENTITY_TICKING_RANGE = 2;
    static final int PLAYER_TICKET_LEVEL = 33 + ChunkStatus.getDistance(ChunkStatus.FULL) - 2;
    private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
    private static final int ENTITY_TICKING_LEVEL_THRESHOLD = 32;
    private static final int BLOCK_TICKING_LEVEL_THRESHOLD = 33;
    final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap();
    public final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap();
    //private final DistanceManager.ChunkTicketTracker ticketTracker = new DistanceManager.ChunkTicketTracker(); // Paper - replace ticket level propagator
    public static final int MOB_SPAWN_RANGE = 8; // private final ChunkMapDistance.b f = new ChunkMapDistance.b(8); // Paper - no longer used
    //private final TickingTracker tickingTicketsTracker = new TickingTracker(); // Paper - no longer used
    //private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(33); // Paper - no longer used
    // Paper start use a queue, but still keep unique requirement
    public final java.util.Queue<ChunkHolder> pendingChunkUpdates = new java.util.ArrayDeque<ChunkHolder>() {
        @Override
        public boolean add(ChunkHolder o) {
            if (o.isUpdateQueued) return true;
            o.isUpdateQueued = true;
            return super.add(o);
        }
    };
    // Paper end
    final ChunkTaskPriorityQueueSorter ticketThrottler;
    final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> ticketThrottlerInput;
    final ProcessorHandle<ChunkTaskPriorityQueueSorter.Release> ticketThrottlerReleaser;
    final LongSet ticketsToRelease = new LongOpenHashSet();
    final Executor mainThreadExecutor;
    private long ticketTickCounter;
    private int simulationDistance = 10;
    private final ChunkMap chunkMap; // Paper

    protected DistanceManager(Executor workerExecutor, Executor mainThreadExecutor, ChunkMap chunkMap) {
        Objects.requireNonNull(mainThreadExecutor);
        ProcessorHandle<Runnable> mailbox = ProcessorHandle.of("player ticket throttler", mainThreadExecutor::execute);
        ChunkTaskPriorityQueueSorter chunktaskqueuesorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(mailbox), workerExecutor, 4);

        this.ticketThrottler = chunktaskqueuesorter;
        this.ticketThrottlerInput = chunktaskqueuesorter.getProcessor(mailbox, true);
        this.ticketThrottlerReleaser = chunktaskqueuesorter.getReleaseProcessor(mailbox);
        this.mainThreadExecutor = mainThreadExecutor;
        this.chunkMap = chunkMap; // Paper
    }

    // Paper start - replace ticket level propagator
    protected final Long2IntLinkedOpenHashMap ticketLevelUpdates = new Long2IntLinkedOpenHashMap() {
        @Override
        protected void rehash(int newN) {
            // no downsizing allowed
            if (newN < this.n) {
                return;
            }
            super.rehash(newN);
        }
    };
    protected final io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D ticketLevelPropagator = new io.papermc.paper.util.misc.Delayed8WayDistancePropagator2D(
            (long coordinate, byte oldLevel, byte newLevel) -> {
                DistanceManager.this.ticketLevelUpdates.putAndMoveToLast(coordinate, convertBetweenTicketLevels(newLevel));
            }
    );
    // function for converting between ticket levels and propagator levels and vice versa
    // the problem is the ticket level propagator will propagate from a set source down to zero, whereas mojang expects
    // levels to propagate from a set value up to a maximum value. so we need to convert the levels we put into the propagator
    // and the levels we get out of the propagator

    // this maps so that GOLDEN_TICKET + 1 will be 0 in the propagator, GOLDEN_TICKET will be 1, and so on
    // we need GOLDEN_TICKET+1 as 0 because anything >= GOLDEN_TICKET+1 should be unloaded
    public static int convertBetweenTicketLevels(final int level) {
        return ChunkMap.MAX_CHUNK_DISTANCE - level + 1;
    }

    protected final int getPropagatedTicketLevel(final long coordinate) {
        return convertBetweenTicketLevels(this.ticketLevelPropagator.getLevel(coordinate));
    }

    protected final void updateTicketLevel(final long coordinate, final int ticketLevel) {
        if (ticketLevel > ChunkMap.MAX_CHUNK_DISTANCE) {
            this.ticketLevelPropagator.removeSource(coordinate);
        } else {
            this.ticketLevelPropagator.setSource(coordinate, convertBetweenTicketLevels(ticketLevel));
        }
    }
    // Paper end - replace ticket level propagator

    protected void purgeStaleTickets() {
        ++this.ticketTickCounter;
        ObjectIterator objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();
        // Paper start - use optimised removeIf
        long[] currChunk = new long[1];
        long ticketCounter = DistanceManager.this.ticketTickCounter;
        java.util.function.Predicate<Ticket<?>> removeIf = (ticket) -> {
            final boolean ret = ticket.timedOut(ticketCounter);
            if (ret) {
                //this.tickingTicketsTracker.removeTicket(currChunk[0], ticket); // Paper - no longer used
            }
            return ret;
        };
        // Paper end - use optimised removeIf

        while (objectiterator.hasNext()) {
            Entry<SortedArraySet<Ticket<?>>> entry = (Entry) objectiterator.next();
            // Paper start - use optimised removeIf
            Iterator<Ticket<?>> iterator = null;
            currChunk[0] = entry.getLongKey();
            boolean flag = entry.getValue().removeIf(removeIf);

            while (false && iterator.hasNext()) {
                // Paper end - use optimised removeIf
                Ticket<?> ticket = (Ticket) iterator.next();

                if (ticket.timedOut(this.ticketTickCounter)) {
                    iterator.remove();
                    flag = true;
                    //this.tickingTicketsTracker.removeTicket(entry.getLongKey(), ticket); // Paper - no longer used
                }
            }

            if (flag) {
                this.updateTicketLevel(entry.getLongKey(), getTicketLevelAt(entry.getValue())); // Paper - replace ticket level propagator
            }

            if (((SortedArraySet) entry.getValue()).isEmpty()) {
                objectiterator.remove();
            }
        }

    }

    private static int getTicketLevelAt(SortedArraySet<Ticket<?>> tickets) {
        org.spigotmc.AsyncCatcher.catchOp("ChunkMapDistance::getTicketLevelAt"); // Paper
        return !tickets.isEmpty() ? ((Ticket) tickets.first()).getTicketLevel() : ChunkMap.MAX_CHUNK_DISTANCE + 1;
    }

    protected abstract boolean isChunkToRemove(long pos);

    @Nullable
    protected abstract ChunkHolder getChunk(long pos);

    @Nullable
    protected abstract ChunkHolder updateChunkScheduling(long pos, int level, @Nullable ChunkHolder holder, int k);

    protected long ticketLevelUpdateCount; // Paper - replace ticket level propagator
    public boolean runAllUpdates(ChunkMap chunkStorage) {
        //this.f.a(); // Paper - no longer used
        //this.tickingTicketsTracker.runAllUpdates(); // Paper - no longer used
        org.spigotmc.AsyncCatcher.catchOp("DistanceManagerTick"); // Paper
        //this.playerTicketManager.runAllUpdates(); // Paper - no longer used
        boolean flag = this.ticketLevelPropagator.propagateUpdates(); // Paper - replace ticket level propagator

        if (flag) {
            ;
        }

        // Paper start - replace level propagator
        ticket_update_loop:
        while (!this.ticketLevelUpdates.isEmpty()) {
            flag = true;

            boolean oldPolling = this.pollingPendingChunkUpdates;
            this.pollingPendingChunkUpdates = true;
            try {
                for (java.util.Iterator<Long2IntMap.Entry> iterator = this.ticketLevelUpdates.long2IntEntrySet().fastIterator(); iterator.hasNext();) {
                    Long2IntMap.Entry entry = iterator.next();
                    long key = entry.getLongKey();
                    int newLevel = entry.getIntValue();
                    ChunkHolder chunk = this.getChunk(key);

                    if (chunk == null && newLevel > ChunkMap.MAX_CHUNK_DISTANCE) {
                        // not loaded and it shouldn't be loaded!
                        continue;
                    }

                    int currentLevel = chunk == null ? ChunkMap.MAX_CHUNK_DISTANCE + 1 : chunk.getTicketLevel();

                    if (currentLevel == newLevel) {
                        // nothing to do
                        continue;
                    }

                    this.updateChunkScheduling(key, newLevel, chunk, currentLevel);
                }

                long recursiveCheck = ++this.ticketLevelUpdateCount;
                while (!this.ticketLevelUpdates.isEmpty()) {
                    long key = this.ticketLevelUpdates.firstLongKey();
                    int newLevel = this.ticketLevelUpdates.removeFirstInt();
                    ChunkHolder chunk = this.getChunk(key);

                    if (chunk == null) {
                        if (newLevel <= ChunkMap.MAX_CHUNK_DISTANCE) {
                            throw new IllegalStateException("Expected chunk holder to be created");
                        }
                        // not loaded and it shouldn't be loaded!
                        continue;
                    }

                    int currentLevel = chunk.oldTicketLevel;

                    if (currentLevel == newLevel) {
                        // nothing to do
                        continue;
                    }

                    chunk.updateFutures(chunkStorage, this.mainThreadExecutor);
                    if (recursiveCheck != this.ticketLevelUpdateCount) {
                        // back to the start, we must create player chunks and update the ticket level fields before
                        // processing the actual level updates
                        continue ticket_update_loop;
                    }
                }

                for (;;) {
                    if (recursiveCheck != this.ticketLevelUpdateCount) {
                        continue ticket_update_loop;
                    }
                    ChunkHolder pendingUpdate = this.pendingChunkUpdates.poll();
                    if (pendingUpdate == null) {
                        break;
                    }

                    pendingUpdate.updateFutures(chunkStorage, this.mainThreadExecutor);
                }
            } finally {
                this.pollingPendingChunkUpdates = oldPolling;
            }
        }

        return flag;
        // Paper end - replace level propagator
    }
    boolean pollingPendingChunkUpdates = false; // Paper - Chunk priority

    boolean addTicket(long i, Ticket<?> ticket) { // CraftBukkit - void -> boolean
        org.spigotmc.AsyncCatcher.catchOp("ChunkMapDistance::addTicket"); // Paper
        SortedArraySet<Ticket<?>> arraysetsorted = this.getTickets(i);
        int j = DistanceManager.getTicketLevelAt(arraysetsorted);
        Ticket<?> ticket1 = (Ticket) arraysetsorted.addOrGet(ticket);

        ticket1.setCreatedTick(this.ticketTickCounter);
        if (ticket.getTicketLevel() < j) {
            this.updateTicketLevel(i, ticket.getTicketLevel()); // Paper - replace ticket level propagator
        }

        return ticket == ticket1; // CraftBukkit
    }

    boolean removeTicket(long i, Ticket<?> ticket) { // CraftBukkit - void -> boolean
        org.spigotmc.AsyncCatcher.catchOp("ChunkMapDistance::removeTicket"); // Paper
        SortedArraySet<Ticket<?>> arraysetsorted = this.getTickets(i);
        int oldLevel = getTicketLevelAt(arraysetsorted); // Paper

        boolean removed = false; // CraftBukkit
        if (arraysetsorted.remove(ticket)) {
            removed = true; // CraftBukkit
            // Paper start - delay chunk unloads for player tickets
            long delayChunkUnloadsBy = chunkMap.level.paperConfig.delayChunkUnloadsBy;
            if (ticket.getType() == TicketType.PLAYER && delayChunkUnloadsBy > 0) {
                boolean hasPlayer = false;
                for (Ticket<?> ticket1 : arraysetsorted) {
                    if (ticket1.getType() == TicketType.PLAYER) {
                        hasPlayer = true;
                        break;
                    }
                }
                ChunkHolder playerChunk = chunkMap.getUpdatingChunkIfPresent(i);
                if (!hasPlayer && playerChunk != null && playerChunk.isFullChunkReady()) {
                    Ticket<Long> delayUnload = new Ticket<Long>(TicketType.DELAY_UNLOAD, 33, i);
                    delayUnload.delayUnloadBy = delayChunkUnloadsBy;
                    delayUnload.setCreatedTick(this.ticketTickCounter);
                    arraysetsorted.remove(delayUnload);
                    // refresh ticket
                    arraysetsorted.add(delayUnload);
                }
            }
            // Paper end
        }

        if (arraysetsorted.isEmpty()) {
            this.tickets.remove(i);
        }

        // Paper start - Chunk priority
        int newLevel = getTicketLevelAt(arraysetsorted);
        if (newLevel > oldLevel) {
            this.updateTicketLevel(i, newLevel); // Paper // Paper - replace ticket level propagator
        }
        // Paper end
        return removed; // CraftBukkit
    }

    public <T> void addTicket(TicketType<T> type, ChunkPos pos, int level, T argument) {
        this.addTicket(pos.toLong(), new Ticket<>(type, level, argument));
    }

    public <T> void removeTicket(TicketType<T> type, ChunkPos pos, int level, T argument) {
        Ticket<T> ticket = new Ticket<>(type, level, argument);

        this.removeTicket(pos.toLong(), ticket);
    }

    public <T> void addRegionTicket(TicketType<T> type, ChunkPos pos, int radius, T argument) {
        // CraftBukkit start
        this.addRegionTicketAtDistance(type, pos, radius, argument);
    }

    public <T> boolean addRegionTicketAtDistance(TicketType<T> tickettype, ChunkPos chunkcoordintpair, int i, T t0) {
        // CraftBukkit end
        Ticket<T> ticket = new Ticket<>(tickettype, 33 - i, t0);
        long j = chunkcoordintpair.toLong();

        boolean added = this.addTicket(j, ticket); // CraftBukkit
        //this.tickingTicketsTracker.addTicket(j, ticket); // Paper - no longer used
        return added; // CraftBukkit
    }

    public <T> void removeRegionTicket(TicketType<T> type, ChunkPos pos, int radius, T argument) {
        // CraftBukkit start
        this.removeRegionTicketAtDistance(type, pos, radius, argument);
    }

    public <T> boolean removeRegionTicketAtDistance(TicketType<T> tickettype, ChunkPos chunkcoordintpair, int i, T t0) {
        // CraftBukkit end
        Ticket<T> ticket = new Ticket<>(tickettype, 33 - i, t0);
        long j = chunkcoordintpair.toLong();

        boolean removed = this.removeTicket(j, ticket); // CraftBukkit
        //this.tickingTicketsTracker.removeTicket(j, ticket); // Paper - no longer used
        return removed; // CraftBukkit
    }

    private SortedArraySet<Ticket<?>> getTickets(long position) {
        return (SortedArraySet) this.tickets.computeIfAbsent(position, (j) -> {
            return SortedArraySet.create(4);
        });
    }

    // Paper start - Chunk priority
    public static final int PRIORITY_TICKET_LEVEL = ChunkMap.MAX_CHUNK_DISTANCE;
    public static final int URGENT_PRIORITY = 29;
    public boolean delayDistanceManagerTick = false;
    public boolean markUrgent(ChunkPos coords) {
        return addPriorityTicket(coords, TicketType.URGENT, URGENT_PRIORITY);
    }
    public boolean markHighPriority(ChunkPos coords, int priority) {
        priority = Math.min(URGENT_PRIORITY - 1, Math.max(1, priority));
        return addPriorityTicket(coords, TicketType.PRIORITY, priority);
    }

    public void markAreaHighPriority(ChunkPos center, int priority, int radius) {
        delayDistanceManagerTick = true;
        priority = Math.min(URGENT_PRIORITY - 1, Math.max(1, priority));
        int finalPriority = priority;
        net.minecraft.server.MCUtil.getSpiralOutChunks(center.getWorldPosition(), radius).forEach(coords -> {
            addPriorityTicket(coords, TicketType.PRIORITY, finalPriority);
        });
        delayDistanceManagerTick = false;
        chunkMap.level.getChunkSource().runDistanceManagerUpdates();
    }

    public void clearAreaPriorityTickets(ChunkPos center, int radius) {
        delayDistanceManagerTick = true;
        net.minecraft.server.MCUtil.getSpiralOutChunks(center.getWorldPosition(), radius).forEach(coords -> {
            this.removeTicket(coords.toLong(), new Ticket<ChunkPos>(TicketType.PRIORITY, PRIORITY_TICKET_LEVEL, coords));
        });
        delayDistanceManagerTick = false;
        chunkMap.level.getChunkSource().runDistanceManagerUpdates();
    }

    private boolean addPriorityTicket(ChunkPos coords, TicketType<ChunkPos> ticketType, int priority) {
        org.spigotmc.AsyncCatcher.catchOp("ChunkMapDistance::addPriorityTicket");
        long pair = coords.toLong();
        ChunkHolder chunk = chunkMap.getUpdatingChunkIfPresent(pair);
        if ((chunk != null && chunk.isFullChunkReady())) {
            return false;
        }

        boolean success;
        if (!(success = updatePriorityTicket(coords, ticketType, priority))) {
            Ticket<ChunkPos> ticket = new Ticket<ChunkPos>(ticketType, PRIORITY_TICKET_LEVEL, coords);
            ticket.priority = priority;
            success = this.addTicket(pair, ticket);
        } else {
            if (chunk == null) {
                chunk = chunkMap.getUpdatingChunkIfPresent(pair);
            }
            chunkMap.queueHolderUpdate(chunk);
        }

        //chunkMap.world.getWorld().spawnParticle(priority <= 15 ? org.bukkit.Particle.EXPLOSION_HUGE : org.bukkit.Particle.EXPLOSION_NORMAL, chunkMap.world.getWorld().getPlayers(), null, coords.x << 4, 70, coords.z << 4, 2, 0, 0, 0, 1, null, true);

        chunkMap.level.getChunkSource().runDistanceManagerUpdates();

        return success;
    }

    private boolean updatePriorityTicket(ChunkPos coords, TicketType<ChunkPos> type, int priority) {
        SortedArraySet<Ticket<?>> tickets = this.tickets.get(coords.toLong());
        if (tickets == null) {
            return false;
        }
        for (Ticket<?> ticket : tickets) {
            if (ticket.getType() == type) {
                // We only support increasing, not decreasing, too complicated
                ticket.setCreatedTick(this.ticketTickCounter);
                ticket.priority = Math.max(ticket.priority, priority);
                return true;
            }
        }

        return false;
    }

    public int getChunkPriority(ChunkPos coords) {
        org.spigotmc.AsyncCatcher.catchOp("ChunkMapDistance::getChunkPriority");
        SortedArraySet<Ticket<?>> tickets = this.tickets.get(coords.toLong());
        if (tickets == null) {
            return 0;
        }
        for (Ticket<?> ticket : tickets) {
            if (ticket.getType() == TicketType.URGENT) {
                return URGENT_PRIORITY;
            }
        }
        for (Ticket<?> ticket : tickets) {
            if (ticket.getType() == TicketType.PRIORITY && ticket.priority > 0) {
                return ticket.priority;
            }
        }
        return 0;
    }

    public void clearPriorityTickets(ChunkPos coords) {
        org.spigotmc.AsyncCatcher.catchOp("ChunkMapDistance::clearPriority");
        this.removeTicket(coords.toLong(), new Ticket<ChunkPos>(TicketType.PRIORITY, PRIORITY_TICKET_LEVEL, coords));
    }

    public void clearUrgent(ChunkPos coords) {
        org.spigotmc.AsyncCatcher.catchOp("ChunkMapDistance::clearUrgent");
        this.removeTicket(coords.toLong(), new Ticket<ChunkPos>(TicketType.URGENT, PRIORITY_TICKET_LEVEL, coords));
    }
    // Paper end

    protected void updateChunkForced(ChunkPos pos, boolean forced) {
        Ticket<ChunkPos> ticket = new Ticket<>(TicketType.FORCED, 31, pos);
        long i = pos.toLong();

        if (forced) {
            this.addTicket(i, ticket);
            //this.tickingTicketsTracker.addTicket(i, ticket); // Paper - no longer used
        } else {
            this.removeTicket(i, ticket);
            //this.tickingTicketsTracker.removeTicket(i, ticket); // Paper - no longer used
        }

    }

    public void addPlayer(SectionPos pos, ServerPlayer player) {
        ChunkPos chunkcoordintpair = pos.chunk();
        long i = chunkcoordintpair.toLong();

        ((ObjectSet) this.playersPerChunk.computeIfAbsent(i, (j) -> {
            return new ObjectOpenHashSet();
        })).add(player);
        //this.f.update(i, 0, true); // Paper - no longer used
        //this.playerTicketManager.update(i, 0, true); // Paper - no longer used
        //this.tickingTicketsTracker.addTicket(TicketType.PLAYER, chunkcoordintpair, this.getPlayerTicketLevel(), chunkcoordintpair); // Paper - no longer used
    }

    public void removePlayer(SectionPos pos, ServerPlayer player) {
        ChunkPos chunkcoordintpair = pos.chunk();
        long i = chunkcoordintpair.toLong();
        ObjectSet<ServerPlayer> objectset = (ObjectSet) this.playersPerChunk.get(i);
        if (objectset == null) return; // CraftBukkit - SPIGOT-6208

        if (objectset != null) objectset.remove(player); // Paper - some state corruption happens here, don't crash, clean up gracefully.
        if (objectset == null || objectset.isEmpty()) { // Paper
            this.playersPerChunk.remove(i);
            //this.f.update(i, Integer.MAX_VALUE, false); // Paper - no longer used
            //this.playerTicketManager.update(i, Integer.MAX_VALUE, false); // Paper - no longer used
            //this.tickingTicketsTracker.removeTicket(TicketType.PLAYER, chunkcoordintpair, this.getPlayerTicketLevel(), chunkcoordintpair); // Paper - no longer used
        }

    }

    private int getPlayerTicketLevel() {
        return Math.max(0, 31 - this.simulationDistance);
    }

    public boolean inEntityTickingRange(long chunkPos) {
        // Paper start - replace player chunk loader system
        ChunkHolder holder = this.chunkMap.getVisibleChunkIfPresent(chunkPos);
        return holder != null && holder.isEntityTickingReady();
        // Paper end - replace player chunk loader system
    }

    public boolean inBlockTickingRange(long chunkPos) {
        // Paper start - replace player chunk loader system
        ChunkHolder holder = this.chunkMap.getVisibleChunkIfPresent(chunkPos);
        return holder != null && holder.isTickingReady();
        // Paper end - replace player chunk loader system
    }

    protected String getTicketDebugString(long pos) {
        SortedArraySet<Ticket<?>> arraysetsorted = (SortedArraySet) this.tickets.get(pos);

        return arraysetsorted != null && !arraysetsorted.isEmpty() ? ((Ticket) arraysetsorted.first()).toString() : "no_ticket";
    }

    protected void updatePlayerTickets(int viewDistance) {
        this.chunkMap.playerChunkManager.setTargetNoTickViewDistance(viewDistance); // Paper - route to player chunk manager
    }

    public void updateSimulationDistance(int simulationDistance) {
        this.chunkMap.playerChunkManager.setTargetTickViewDistance(simulationDistance); // Paper - route to player chunk manager
    }

    // Paper start
    public int getSimulationDistance() {
        return this.chunkMap.playerChunkManager.getTargetTickViewDistance(); // Paper - route to player chunk manager
    }
    // Paper end

    public int getNaturalSpawnChunkCount() {
        // Paper start - use distance map to implement
        // note: this is the spawn chunk count
        return this.chunkMap.playerChunkTickRangeMap.size();
        // Paper end - use distance map to implement
    }

    public boolean hasPlayersNearby(long chunkPos) {
        // Paper start - use distance map to implement
        // note: this is the is spawn chunk method
        return this.chunkMap.playerChunkTickRangeMap.getObjectsInRange(chunkPos) != null;
        // Paper end - use distance map to implement
    }

    public String getDebugStatus() {
        return this.ticketThrottler.getDebugStatus();
    }

    private void dumpTickets(String path) {
        try {
            FileOutputStream fileoutputstream = new FileOutputStream(new File(path));

            try {
                ObjectIterator objectiterator = this.tickets.long2ObjectEntrySet().iterator();

                while (objectiterator.hasNext()) {
                    Entry<SortedArraySet<Ticket<?>>> entry = (Entry) objectiterator.next();
                    ChunkPos chunkcoordintpair = new ChunkPos(entry.getLongKey());
                    Iterator iterator = ((SortedArraySet) entry.getValue()).iterator();

                    while (iterator.hasNext()) {
                        Ticket<?> ticket = (Ticket) iterator.next();

                        fileoutputstream.write((chunkcoordintpair.x + "\t" + chunkcoordintpair.z + "\t" + ticket.getType() + "\t" + ticket.getTicketLevel() + "\t\n").getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (Throwable throwable) {
                try {
                    fileoutputstream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }

                throw throwable;
            }

            fileoutputstream.close();
        } catch (IOException ioexception) {
            DistanceManager.LOGGER.error("Failed to dump tickets to {}", path, ioexception);
        }

    }

    // Paper - replace player chunk loader

    public void removeTicketsOnClosing() {
        ImmutableSet<TicketType<?>> immutableset = ImmutableSet.of(TicketType.UNKNOWN, TicketType.POST_TELEPORT, TicketType.LIGHT, TicketType.FUTURE_AWAIT, TicketType.ASYNC_LOAD, TicketType.REQUIRED_LOAD, TicketType.CHUNK_RELIGHT, ca.spottedleaf.starlight.common.light.StarLightInterface.CHUNK_WORK_TICKET); // Paper - add additional tickets to preserve
        ObjectIterator objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();

        while (objectiterator.hasNext()) {
            Entry<SortedArraySet<Ticket<?>>> entry = (Entry) objectiterator.next();
            Iterator<Ticket<?>> iterator = ((SortedArraySet) entry.getValue()).iterator();
            boolean flag = false;

            while (iterator.hasNext()) {
                Ticket<?> ticket = (Ticket) iterator.next();

                if (!immutableset.contains(ticket.getType())) {
                    iterator.remove();
                    flag = true;
                    // this.tickingTicketsTracker.removeTicket(entry.getLongKey(), ticket); // Paper - no longer used
                }
            }

            if (flag) {
                this.updateTicketLevel(entry.getLongKey(), getTicketLevelAt(entry.getValue())); // Paper - replace ticket level propagator
                // this.ticketTracker.update(entry.getLongKey(), DistanceManager.getTicketLevelAt((SortedArraySet) entry.getValue()), false); // Paper - no longer used
            }

            if (((SortedArraySet) entry.getValue()).isEmpty()) {
                objectiterator.remove();
            }
        }

    }

    public boolean hasTickets() {
        return !this.tickets.isEmpty();
    }

    // CraftBukkit start
    public <T> void removeAllTicketsFor(TicketType<T> ticketType, int ticketLevel, T ticketIdentifier) {
        Ticket<T> target = new Ticket<>(ticketType, ticketLevel, ticketIdentifier);

        for (java.util.Iterator<Entry<SortedArraySet<Ticket<?>>>> iterator = this.tickets.long2ObjectEntrySet().fastIterator(); iterator.hasNext();) {
            Entry<SortedArraySet<Ticket<?>>> entry = iterator.next();
            SortedArraySet<Ticket<?>> tickets = entry.getValue();
            if (tickets.remove(target)) {
                // copied from removeTicket
                this.updateTicketLevel(entry.getLongKey(), getTicketLevelAt(tickets)); // Paper - replace ticket level propagator

                // can't use entry after it's removed
                if (tickets.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }
    // CraftBukkit end

    /* Paper - replace old loader system
    private class ChunkTicketTracker extends ChunkTracker {

        public ChunkTicketTracker() {
            super(ChunkMap.MAX_CHUNK_DISTANCE + 2, 16, 256);
        }

        @Override
        protected int getLevelFromSource(long id) {
            SortedArraySet<Ticket<?>> arraysetsorted = (SortedArraySet) DistanceManager.this.tickets.get(id);

            return arraysetsorted == null ? Integer.MAX_VALUE : (arraysetsorted.isEmpty() ? Integer.MAX_VALUE : ((Ticket) arraysetsorted.first()).getTicketLevel());
        }

        @Override
        protected int getLevel(long id) {
            if (!DistanceManager.this.isChunkToRemove(id)) {
                ChunkHolder playerchunk = DistanceManager.this.getChunk(id);

                if (playerchunk != null) {
                    return playerchunk.getTicketLevel();
                }
            }

            return ChunkMap.MAX_CHUNK_DISTANCE + 1;
        }

        @Override
        protected void setLevel(long id, int level) {
            ChunkHolder playerchunk = DistanceManager.this.getChunk(id);
            int k = playerchunk == null ? ChunkMap.MAX_CHUNK_DISTANCE + 1 : playerchunk.getTicketLevel();

            if (k != level) {
                playerchunk = DistanceManager.this.updateChunkScheduling(id, level, playerchunk, k);
                if (playerchunk != null) {
                    DistanceManager.this.pendingChunkUpdates.add(playerchunk);
                }

            }
        }

        public int runDistanceUpdates(int distance) {
            return this.runUpdates(distance);
        }
    }

    private class FixedPlayerDistanceChunkTracker extends ChunkTracker {

        protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
        protected final int maxDistance;

        protected FixedPlayerDistanceChunkTracker(int i) {
            super(i + 2, 16, 256);
            this.maxDistance = i;
            this.chunks.defaultReturnValue((byte) (i + 2));
        }

        @Override
        protected int getLevel(long id) {
            return this.chunks.get(id);
        }

        @Override
        protected void setLevel(long id, int level) {
            byte b0;

            if (level > this.maxDistance) {
                b0 = this.chunks.remove(id);
            } else {
                b0 = this.chunks.put(id, (byte) level);
            }

            this.onLevelChange(id, b0, level);
        }

        protected void onLevelChange(long pos, int oldDistance, int distance) {}

        @Override
        protected int getLevelFromSource(long id) {
            return this.havePlayer(id) ? 0 : Integer.MAX_VALUE;
        }

        private boolean havePlayer(long chunkPos) {
            ObjectSet<ServerPlayer> objectset = (ObjectSet) DistanceManager.this.playersPerChunk.get(chunkPos);

            return objectset != null && !objectset.isEmpty();
        }

        public void runAllUpdates() {
            this.runUpdates(Integer.MAX_VALUE);
        }

        private void dumpChunks(String path) {
            try {
                FileOutputStream fileoutputstream = new FileOutputStream(new File(path));

                try {
                    ObjectIterator objectiterator = this.chunks.long2ByteEntrySet().iterator();

                    while (objectiterator.hasNext()) {
                        it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry it_unimi_dsi_fastutil_longs_long2bytemap_entry = (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry) objectiterator.next();
                        ChunkPos chunkcoordintpair = new ChunkPos(it_unimi_dsi_fastutil_longs_long2bytemap_entry.getLongKey());
                        String s1 = Byte.toString(it_unimi_dsi_fastutil_longs_long2bytemap_entry.getByteValue());

                        fileoutputstream.write((chunkcoordintpair.x + "\t" + chunkcoordintpair.z + "\t" + s1 + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Throwable throwable) {
                    try {
                        fileoutputstream.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }

                    throw throwable;
                }

                fileoutputstream.close();
            } catch (IOException ioexception) {
                DistanceManager.LOGGER.error("Failed to dump chunks to {}", path, ioexception);
            }

        }
    }

    private class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {

        private int viewDistance = 0;
        private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
        private final LongSet toUpdate = new LongOpenHashSet();

        protected PlayerTicketTracker(int i) {
            super(i);
            this.queueLevels.defaultReturnValue(i + 2);
        }

        @Override
        protected void onLevelChange(long pos, int oldDistance, int distance) {
            this.toUpdate.add(pos);
        }

        public void updateViewDistance(int watchDistance) {
            ObjectIterator objectiterator = this.chunks.long2ByteEntrySet().iterator();

            while (objectiterator.hasNext()) {
                it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry it_unimi_dsi_fastutil_longs_long2bytemap_entry = (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry) objectiterator.next();
                byte b0 = it_unimi_dsi_fastutil_longs_long2bytemap_entry.getByteValue();
                long j = it_unimi_dsi_fastutil_longs_long2bytemap_entry.getLongKey();

                this.onLevelChange(j, b0, this.haveTicketFor(b0), b0 <= watchDistance - 2);
            }

            this.viewDistance = watchDistance;
        }

        private void onLevelChange(long pos, int distance, boolean oldWithinViewDistance, boolean withinViewDistance) {
            if (oldWithinViewDistance != withinViewDistance) {
                Ticket<?> ticket = new Ticket<>(TicketType.PLAYER, DistanceManager.PLAYER_TICKET_LEVEL, new ChunkPos(pos));

                if (withinViewDistance) {
                    DistanceManager.this.ticketThrottlerInput.tell(ChunkTaskPriorityQueueSorter.message(() -> {
                        DistanceManager.this.mainThreadExecutor.execute(() -> {
                            if (this.haveTicketFor(this.getLevel(pos))) {
                                DistanceManager.this.addTicket(pos, ticket);
                                DistanceManager.this.ticketsToRelease.add(pos);
                            } else {
                                DistanceManager.this.ticketThrottlerReleaser.tell(ChunkTaskPriorityQueueSorter.release(() -> {
                                }, pos, false));
                            }

                        });
                    }, pos, () -> {
                        return distance;
                    }));
                } else {
                    DistanceManager.this.ticketThrottlerReleaser.tell(ChunkTaskPriorityQueueSorter.release(() -> {
                        DistanceManager.this.mainThreadExecutor.execute(() -> {
                            DistanceManager.this.removeTicket(pos, ticket);
                        });
                    }, pos, true));
                }
            }

        }

        @Override
        public void runAllUpdates() {
            super.runAllUpdates();
            if (!this.toUpdate.isEmpty()) {
                LongIterator longiterator = this.toUpdate.iterator();

                while (longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    int j = this.queueLevels.get(i);
                    int k = this.getLevel(i);

                    if (j != k) {
                        DistanceManager.this.ticketThrottler.onLevelChange(new ChunkPos(i), () -> {
                            return this.queueLevels.get(i);
                        }, k, (l) -> {
                            if (l >= this.queueLevels.defaultReturnValue()) {
                                this.queueLevels.remove(i);
                            } else {
                                this.queueLevels.put(i, l);
                            }

                        });
                        this.onLevelChange(i, k, this.haveTicketFor(j), this.haveTicketFor(k));
                    }
                }

                this.toUpdate.clear();
            }

        }

        private boolean haveTicketFor(int distance) {
            return distance <= this.viewDistance - 2;
        }
    }
     */ // Paper - replace old loader system
}
