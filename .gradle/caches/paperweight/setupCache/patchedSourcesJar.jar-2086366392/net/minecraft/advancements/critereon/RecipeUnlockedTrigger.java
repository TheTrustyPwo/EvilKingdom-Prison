package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeUnlockedTrigger extends SimpleCriterionTrigger<RecipeUnlockedTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("recipe_unlocked");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public RecipeUnlockedTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "recipe"));
        return new RecipeUnlockedTrigger.TriggerInstance(composite, resourceLocation);
    }

    public void trigger(ServerPlayer player, Recipe<?> recipe) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(recipe);
        });
    }

    public static RecipeUnlockedTrigger.TriggerInstance unlocked(ResourceLocation id) {
        return new RecipeUnlockedTrigger.TriggerInstance(EntityPredicate.Composite.ANY, id);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final ResourceLocation recipe;

        public TriggerInstance(EntityPredicate.Composite player, ResourceLocation recipe) {
            super(RecipeUnlockedTrigger.ID, player);
            this.recipe = recipe;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.addProperty("recipe", this.recipe.toString());
            return jsonObject;
        }

        public boolean matches(Recipe<?> recipe) {
            return this.recipe.equals(recipe.getId());
        }
    }
}
