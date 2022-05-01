package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.storage.loot.LootContext;

public class CuredZombieVillagerTrigger extends SimpleCriterionTrigger<CuredZombieVillagerTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("cured_zombie_villager");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public CuredZombieVillagerTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "zombie", deserializationContext);
        EntityPredicate.Composite composite3 = EntityPredicate.Composite.fromJson(jsonObject, "villager", deserializationContext);
        return new CuredZombieVillagerTrigger.TriggerInstance(composite, composite2, composite3);
    }

    public void trigger(ServerPlayer player, Zombie zombie, Villager villager) {
        LootContext lootContext = EntityPredicate.createContext(player, zombie);
        LootContext lootContext2 = EntityPredicate.createContext(player, villager);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, lootContext2);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final EntityPredicate.Composite zombie;
        private final EntityPredicate.Composite villager;

        public TriggerInstance(EntityPredicate.Composite player, EntityPredicate.Composite zombie, EntityPredicate.Composite villager) {
            super(CuredZombieVillagerTrigger.ID, player);
            this.zombie = zombie;
            this.villager = villager;
        }

        public static CuredZombieVillagerTrigger.TriggerInstance curedZombieVillager() {
            return new CuredZombieVillagerTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY);
        }

        public boolean matches(LootContext zombieContext, LootContext villagerContext) {
            if (!this.zombie.matches(zombieContext)) {
                return false;
            } else {
                return this.villager.matches(villagerContext);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("zombie", this.zombie.toJson(predicateSerializer));
            jsonObject.add("villager", this.villager.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
