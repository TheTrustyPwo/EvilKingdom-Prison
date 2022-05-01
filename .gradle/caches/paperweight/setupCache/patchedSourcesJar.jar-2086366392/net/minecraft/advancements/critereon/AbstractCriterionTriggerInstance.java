package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractCriterionTriggerInstance implements CriterionTriggerInstance {
    private final ResourceLocation criterion;
    private final EntityPredicate.Composite player;

    public AbstractCriterionTriggerInstance(ResourceLocation id, EntityPredicate.Composite playerPredicate) {
        this.criterion = id;
        this.player = playerPredicate;
    }

    @Override
    public ResourceLocation getCriterion() {
        return this.criterion;
    }

    protected EntityPredicate.Composite getPlayerPredicate() {
        return this.player;
    }

    @Override
    public JsonObject serializeToJson(SerializationContext predicateSerializer) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("player", this.player.toJson(predicateSerializer));
        return jsonObject;
    }

    @Override
    public String toString() {
        return "AbstractCriterionInstance{criterion=" + this.criterion + "}";
    }
}
