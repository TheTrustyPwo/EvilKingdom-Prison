package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledByCrossbowTrigger extends SimpleCriterionTrigger<KilledByCrossbowTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("killed_by_crossbow");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public KilledByCrossbowTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        EntityPredicate.Composite[] composites = EntityPredicate.Composite.fromJsonArray(jsonObject, "victims", deserializationContext);
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("unique_entity_types"));
        return new KilledByCrossbowTrigger.TriggerInstance(composite, composites, ints);
    }

    public void trigger(ServerPlayer player, Collection<Entity> piercingKilledEntities) {
        List<LootContext> list = Lists.newArrayList();
        Set<EntityType<?>> set = Sets.newHashSet();

        for(Entity entity : piercingKilledEntities) {
            set.add(entity.getType());
            list.add(EntityPredicate.createContext(player, entity));
        }

        this.trigger(player, (conditions) -> {
            return conditions.matches(list, set.size());
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final EntityPredicate.Composite[] victims;
        private final MinMaxBounds.Ints uniqueEntityTypes;

        public TriggerInstance(EntityPredicate.Composite player, EntityPredicate.Composite[] victims, MinMaxBounds.Ints uniqueEntityTypes) {
            super(KilledByCrossbowTrigger.ID, player);
            this.victims = victims;
            this.uniqueEntityTypes = uniqueEntityTypes;
        }

        public static KilledByCrossbowTrigger.TriggerInstance crossbowKilled(EntityPredicate.Builder... victimPredicates) {
            EntityPredicate.Composite[] composites = new EntityPredicate.Composite[victimPredicates.length];

            for(int i = 0; i < victimPredicates.length; ++i) {
                EntityPredicate.Builder builder = victimPredicates[i];
                composites[i] = EntityPredicate.Composite.wrap(builder.build());
            }

            return new KilledByCrossbowTrigger.TriggerInstance(EntityPredicate.Composite.ANY, composites, MinMaxBounds.Ints.ANY);
        }

        public static KilledByCrossbowTrigger.TriggerInstance crossbowKilled(MinMaxBounds.Ints uniqueEntityTypes) {
            EntityPredicate.Composite[] composites = new EntityPredicate.Composite[0];
            return new KilledByCrossbowTrigger.TriggerInstance(EntityPredicate.Composite.ANY, composites, uniqueEntityTypes);
        }

        public boolean matches(Collection<LootContext> victimContexts, int uniqueEntityTypeCount) {
            if (this.victims.length > 0) {
                List<LootContext> list = Lists.newArrayList(victimContexts);

                for(EntityPredicate.Composite composite : this.victims) {
                    boolean bl = false;
                    Iterator<LootContext> iterator = list.iterator();

                    while(iterator.hasNext()) {
                        LootContext lootContext = iterator.next();
                        if (composite.matches(lootContext)) {
                            iterator.remove();
                            bl = true;
                            break;
                        }
                    }

                    if (!bl) {
                        return false;
                    }
                }
            }

            return this.uniqueEntityTypes.matches(uniqueEntityTypeCount);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("victims", EntityPredicate.Composite.toJson(this.victims, predicateSerializer));
            jsonObject.add("unique_entity_types", this.uniqueEntityTypes.serializeToJson());
            return jsonObject;
        }
    }
}
