package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class NearestItemSensor extends Sensor<Mob> {
    private static final long XZ_RANGE = 8L;
    private static final long Y_RANGE = 4L;
    public static final int MAX_DISTANCE_TO_WANTED_ITEM = 9;

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
    }

    @Override
    protected void doTick(ServerLevel world, Mob entity) {
        Brain<?> brain = entity.getBrain();
        List<ItemEntity> list = world.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(8.0D, 4.0D, 8.0D), (itemEntity) -> {
            return itemEntity.closerThan(entity, 9.0D) && entity.wantsToPickUp(itemEntity.getItem()); // Paper - move predicate into getEntities
        });
        list.sort((e1, e2) -> Double.compare(entity.distanceToSqr(e1), entity.distanceToSqr(e2))); // better to take the sort perf hit than using line of sight more than we need to.
        // Paper start - remove streams
        // Paper start - remove streams in favour of lists
        ItemEntity nearest = null;
        for (int i = 0; i < list.size(); i++) {
            ItemEntity entityItem = list.get(i);
            if (entity.hasLineOfSight(entityItem)) {
                nearest = entityItem;
                break;
            }
        }
        // Paper end - remove streams
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, Optional.ofNullable(nearest));
        // Paper end
    }
}
