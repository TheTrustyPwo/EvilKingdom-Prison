package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;

public class NearestBedSensor extends Sensor<Mob> {
    private static final int CACHE_TIMEOUT = 40;
    private static final int BATCH_SIZE = 5;
    private static final int RATE = 20;
    private final Long2LongMap batchCache = new Long2LongOpenHashMap();
    private int triedCount;
    private long lastUpdate;

    public NearestBedSensor() {
        super(20);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_BED);
    }

    @Override
    protected void doTick(ServerLevel world, Mob entity) {
        if (entity.isBaby()) {
            this.triedCount = 0;
            this.lastUpdate = world.getGameTime() + (long)world.getRandom().nextInt(20);
            PoiManager poiManager = world.getPoiManager();
            Predicate<BlockPos> predicate = (pos) -> {
                long l = pos.asLong();
                if (this.batchCache.containsKey(l)) {
                    return false;
                } else if (++this.triedCount >= 5) {
                    return false;
                } else {
                    this.batchCache.put(l, this.lastUpdate + 40L);
                    return true;
                }
            };
            // Paper start - optimise POI access
            java.util.List<BlockPos> poiposes = new java.util.ArrayList<>();
            // don't ask me why it's unbounded. ask mojang.
            io.papermc.paper.util.PoiAccess.findAnyPoiPositions(poiManager, PoiType.HOME.getPredicate(), predicate, entity.blockPosition(), 48, PoiManager.Occupancy.ANY, false, Integer.MAX_VALUE, poiposes);
            Path path = entity.getNavigation().createPath(new java.util.HashSet<>(poiposes), PoiType.HOME.getValidRange());
            // Paper end - optimise POI access
            if (path != null && path.canReach()) {
                BlockPos blockPos = path.getTarget();
                Optional<PoiType> optional = poiManager.getType(blockPos);
                if (optional.isPresent()) {
                    entity.getBrain().setMemory(MemoryModuleType.NEAREST_BED, blockPos);
                }
            } else if (this.triedCount < 5) {
                this.batchCache.long2LongEntrySet().removeIf((entry) -> {
                    return entry.getLongValue() < this.lastUpdate;
                });
            }

        }
    }
}
