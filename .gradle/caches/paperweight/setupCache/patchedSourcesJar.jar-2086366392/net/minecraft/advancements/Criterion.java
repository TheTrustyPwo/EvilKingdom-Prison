package net.minecraft.advancements;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class Criterion {
    @Nullable
    private final CriterionTriggerInstance trigger;

    public Criterion(CriterionTriggerInstance conditions) {
        this.trigger = conditions;
    }

    public Criterion() {
        this.trigger = null;
    }

    public void serializeToNetwork(FriendlyByteBuf buf) {
    }

    public static Criterion criterionFromJson(JsonObject obj, DeserializationContext predicateDeserializer) {
        ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(obj, "trigger"));
        CriterionTrigger<?> criterionTrigger = CriteriaTriggers.getCriterion(resourceLocation);
        if (criterionTrigger == null) {
            throw new JsonSyntaxException("Invalid criterion trigger: " + resourceLocation);
        } else {
            CriterionTriggerInstance criterionTriggerInstance = criterionTrigger.createInstance(GsonHelper.getAsJsonObject(obj, "conditions", new JsonObject()), predicateDeserializer);
            return new Criterion(criterionTriggerInstance);
        }
    }

    public static Criterion criterionFromNetwork(FriendlyByteBuf buf) {
        return new Criterion();
    }

    public static Map<String, Criterion> criteriaFromJson(JsonObject obj, DeserializationContext predicateDeserializer) {
        Map<String, Criterion> map = Maps.newHashMap();

        for(Entry<String, JsonElement> entry : obj.entrySet()) {
            map.put(entry.getKey(), criterionFromJson(GsonHelper.convertToJsonObject(entry.getValue(), "criterion"), predicateDeserializer));
        }

        return map;
    }

    public static Map<String, Criterion> criteriaFromNetwork(FriendlyByteBuf buf) {
        return buf.readMap(FriendlyByteBuf::readUtf, Criterion::criterionFromNetwork);
    }

    public static void serializeToNetwork(Map<String, Criterion> criteria, FriendlyByteBuf buf) {
        buf.writeMap(criteria, FriendlyByteBuf::writeUtf, (bufx, criterion) -> {
            criterion.serializeToNetwork(bufx);
        });
    }

    @Nullable
    public CriterionTriggerInstance getTrigger() {
        return this.trigger;
    }

    public JsonElement serializeToJson() {
        if (this.trigger == null) {
            throw new JsonSyntaxException("Missing trigger");
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("trigger", this.trigger.getCriterion().toString());
            JsonObject jsonObject2 = this.trigger.serializeToJson(SerializationContext.INSTANCE);
            if (jsonObject2.size() != 0) {
                jsonObject.add("conditions", jsonObject2);
            }

            return jsonObject;
        }
    }
}
