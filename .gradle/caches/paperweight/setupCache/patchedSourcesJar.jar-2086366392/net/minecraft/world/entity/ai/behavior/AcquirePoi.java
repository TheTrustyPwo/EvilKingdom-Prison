package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;

public class AcquirePoi extends Behavior<PathfinderMob> {
    private static final int BATCH_SIZE = 5;
    private static final int RATE = 20;
    public static final int SCAN_RANGE = 48;
    private final PoiType poiType;
    private final MemoryModuleType<GlobalPos> memoryToAcquire;
    private final boolean onlyIfAdult;
    private final Optional<Byte> onPoiAcquisitionEvent;
    private long nextScheduledStart;
    private final Long2ObjectMap<AcquirePoi.JitteredLinearRetry> batchCache = new Long2ObjectOpenHashMap<>();

    public AcquirePoi(PoiType poiType, MemoryModuleType<GlobalPos> moduleType, MemoryModuleType<GlobalPos> targetMemoryModuleType, boolean onlyRunIfChild, Optional<Byte> entityStatus) {
        super(constructEntryConditionMap(moduleType, targetMemoryModuleType));
        this.poiType = poiType;
        this.memoryToAcquire = targetMemoryModuleType;
        this.onlyIfAdult = onlyRunIfChild;
        this.onPoiAcquisitionEvent = entityStatus;
    }

    public AcquirePoi(PoiType poiType, MemoryModuleType<GlobalPos> moduleType, boolean onlyRunIfChild, Optional<Byte> entityStatus) {
        this(poiType, moduleType, moduleType, onlyRunIfChild, entityStatus);
    }

    private static ImmutableMap<MemoryModuleType<?>, MemoryStatus> constructEntryConditionMap(MemoryModuleType<GlobalPos> firstModule, MemoryModuleType<GlobalPos> secondModule) {
        Builder<MemoryModuleType<?>, MemoryStatus> builder = ImmutableMap.builder();
        builder.put(firstModule, MemoryStatus.VALUE_ABSENT);
        if (secondModule != firstModule) {
            builder.put(secondModule, MemoryStatus.VALUE_ABSENT);
        }

        return builder.build();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, PathfinderMob entity) {
        if (this.onlyIfAdult && entity.isBaby()) {
            return false;
        } else if (this.nextScheduledStart == 0L) {
            this.nextScheduledStart = entity.level.getGameTime() + (long)world.random.nextInt(20);
            return false;
        } else {
            return world.getGameTime() >= this.nextScheduledStart;
        }
    }

    @Override
    protected void start(ServerLevel world, PathfinderMob entity, long time) {
        this.nextScheduledStart = time + 20L + (long)world.getRandom().nextInt(20);
        PoiManager poiManager = world.getPoiManager();
        this.batchCache.long2ObjectEntrySet().removeIf((entry) -> {
            return !entry.getValue().isStillValid(time);
        });
        Predicate<BlockPos> predicate = (blockPos) -> {
            AcquirePoi.JitteredLinearRetry jitteredLinearRetry = this.batchCache.get(blockPos.asLong());
            if (jitteredLinearRetry == null) {
                return true;
            } else if (!jitteredLinearRetry.shouldRetry(time)) {
                return false;
            } else {
                jitteredLinearRetry.markAttempt(time);
                return true;
            }
        };
        // Paper start - optimise POI access
        java.util.List<BlockPos> poiposes = new java.util.ArrayList<>();
        io.papermc.paper.util.PoiAccess.findNearestPoiPositions(poiManager, this.poiType.getPredicate(), predicate, entity.blockPosition(), 48, 48*48, PoiManager.Occupancy.HAS_SPACE, false, 5, poiposes);
        Set<BlockPos> set = new java.util.HashSet<>(poiposes);
        // Paper end - optimise POI access
        Path path = entity.getNavigation().createPath(set, this.poiType.getValidRange());
        if (path != null && path.canReach()) {
            BlockPos blockPos = path.getTarget();
            poiManager.getType(blockPos).ifPresent((poiType) -> {
                poiManager.take(this.poiType.getPredicate(), (blockPos2) -> {
                    return blockPos2.equals(blockPos);
                }, blockPos, 1);
                entity.getBrain().setMemory(this.memoryToAcquire, GlobalPos.of(world.dimension(), blockPos));
                this.onPoiAcquisitionEvent.ifPresent((byte_) -> {
                    world.broadcastEntityEvent(entity, byte_);
                });
                this.batchCache.clear();
                DebugPackets.sendPoiTicketCountPacket(world, blockPos);
            });
        } else {
            for(BlockPos blockPos2 : set) {
                this.batchCache.computeIfAbsent(blockPos2.asLong(), (m) -> {
                    return new AcquirePoi.JitteredLinearRetry(entity.level.random, time);
                });
            }
        }

    }

    static class JitteredLinearRetry {
        private static final int MIN_INTERVAL_INCREASE = 40;
        private static final int MAX_INTERVAL_INCREASE = 80;
        private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
        private final Random random;
        private long previousAttemptTimestamp;
        private long nextScheduledAttemptTimestamp;
        private int currentDelay;

        JitteredLinearRetry(Random random, long time) {
            this.random = random;
            this.markAttempt(time);
        }

        public void markAttempt(long time) {
            this.previousAttemptTimestamp = time;
            int i = this.currentDelay + this.random.nextInt(40) + 40;
            this.currentDelay = Math.min(i, 400);
            this.nextScheduledAttemptTimestamp = time + (long)this.currentDelay;
        }

        public boolean isStillValid(long time) {
            return time - this.previousAttemptTimestamp < 400L;
        }

        public boolean shouldRetry(long time) {
            return time >= this.nextScheduledAttemptTimestamp;
        }

        @Override
        public String toString() {
            return "RetryMarker{, previousAttemptAt=" + this.previousAttemptTimestamp + ", nextScheduledAttemptAt=" + this.nextScheduledAttemptTimestamp + ", currentDelay=" + this.currentDelay + "}";
        }
    }
}
