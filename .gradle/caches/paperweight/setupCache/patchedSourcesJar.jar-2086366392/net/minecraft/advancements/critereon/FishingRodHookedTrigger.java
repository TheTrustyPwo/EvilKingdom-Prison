package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class FishingRodHookedTrigger extends SimpleCriterionTrigger<FishingRodHookedTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("fishing_rod_hooked");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public FishingRodHookedTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("rod"));
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "entity", deserializationContext);
        ItemPredicate itemPredicate2 = ItemPredicate.fromJson(jsonObject.get("item"));
        return new FishingRodHookedTrigger.TriggerInstance(composite, itemPredicate, composite2, itemPredicate2);
    }

    public void trigger(ServerPlayer player, ItemStack rod, FishingHook bobber, Collection<ItemStack> fishingLoots) {
        LootContext lootContext = EntityPredicate.createContext(player, (Entity)(bobber.getHookedIn() != null ? bobber.getHookedIn() : bobber));
        this.trigger(player, (conditions) -> {
            return conditions.matches(rod, lootContext, fishingLoots);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final ItemPredicate rod;
        private final EntityPredicate.Composite entity;
        private final ItemPredicate item;

        public TriggerInstance(EntityPredicate.Composite player, ItemPredicate rod, EntityPredicate.Composite hookedEntity, ItemPredicate caughtItem) {
            super(FishingRodHookedTrigger.ID, player);
            this.rod = rod;
            this.entity = hookedEntity;
            this.item = caughtItem;
        }

        public static FishingRodHookedTrigger.TriggerInstance fishedItem(ItemPredicate rod, EntityPredicate bobber, ItemPredicate item) {
            return new FishingRodHookedTrigger.TriggerInstance(EntityPredicate.Composite.ANY, rod, EntityPredicate.Composite.wrap(bobber), item);
        }

        public boolean matches(ItemStack rod, LootContext hookedEntityContext, Collection<ItemStack> fishingLoots) {
            if (!this.rod.matches(rod)) {
                return false;
            } else if (!this.entity.matches(hookedEntityContext)) {
                return false;
            } else {
                if (this.item != ItemPredicate.ANY) {
                    boolean bl = false;
                    Entity entity = hookedEntityContext.getParamOrNull(LootContextParams.THIS_ENTITY);
                    if (entity instanceof ItemEntity) {
                        ItemEntity itemEntity = (ItemEntity)entity;
                        if (this.item.matches(itemEntity.getItem())) {
                            bl = true;
                        }
                    }

                    for(ItemStack itemStack : fishingLoots) {
                        if (this.item.matches(itemStack)) {
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
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("rod", this.rod.serializeToJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            jsonObject.add("item", this.item.serializeToJson());
            return jsonObject;
        }
    }
}
