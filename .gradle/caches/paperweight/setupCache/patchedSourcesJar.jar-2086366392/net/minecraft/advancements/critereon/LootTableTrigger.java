package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class LootTableTrigger extends SimpleCriterionTrigger<LootTableTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("player_generates_container_loot");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected LootTableTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "loot_table"));
        return new LootTableTrigger.TriggerInstance(composite, resourceLocation);
    }

    public void trigger(ServerPlayer player, ResourceLocation id) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(id);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final ResourceLocation lootTable;

        public TriggerInstance(EntityPredicate.Composite entity, ResourceLocation lootTable) {
            super(LootTableTrigger.ID, entity);
            this.lootTable = lootTable;
        }

        public static LootTableTrigger.TriggerInstance lootTableUsed(ResourceLocation lootTable) {
            return new LootTableTrigger.TriggerInstance(EntityPredicate.Composite.ANY, lootTable);
        }

        public boolean matches(ResourceLocation lootTable) {
            return this.lootTable.equals(lootTable);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.addProperty("loot_table", this.lootTable.toString());
            return jsonObject;
        }
    }
}
