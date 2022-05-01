package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("channeled_lightning");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public ChanneledLightningTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        EntityPredicate.Composite[] composites = EntityPredicate.Composite.fromJsonArray(jsonObject, "victims", deserializationContext);
        return new ChanneledLightningTrigger.TriggerInstance(composite, composites);
    }

    public void trigger(ServerPlayer player, Collection<? extends Entity> victims) {
        List<LootContext> list = victims.stream().map((entity) -> {
            return EntityPredicate.createContext(player, entity);
        }).collect(Collectors.toList());
        this.trigger(player, (conditions) -> {
            return conditions.matches(list);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final EntityPredicate.Composite[] victims;

        public TriggerInstance(EntityPredicate.Composite player, EntityPredicate.Composite[] victims) {
            super(ChanneledLightningTrigger.ID, player);
            this.victims = victims;
        }

        public static ChanneledLightningTrigger.TriggerInstance channeledLightning(EntityPredicate... victims) {
            return new ChanneledLightningTrigger.TriggerInstance(EntityPredicate.Composite.ANY, Stream.of(victims).map(EntityPredicate.Composite::wrap).toArray((i) -> {
                return new EntityPredicate.Composite[i];
            }));
        }

        public boolean matches(Collection<? extends LootContext> victims) {
            for(EntityPredicate.Composite composite : this.victims) {
                boolean bl = false;

                for(LootContext lootContext : victims) {
                    if (composite.matches(lootContext)) {
                        bl = true;
                        break;
                    }
                }

                if (!bl) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("victims", EntityPredicate.Composite.toJson(this.victims, predicateSerializer));
            return jsonObject;
        }
    }
}
