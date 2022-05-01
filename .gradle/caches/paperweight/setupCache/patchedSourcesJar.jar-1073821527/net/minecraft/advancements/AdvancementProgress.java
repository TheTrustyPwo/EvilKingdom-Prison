package net.minecraft.advancements;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;

public class AdvancementProgress implements Comparable<AdvancementProgress> {
    final Map<String, CriterionProgress> criteria;
    private String[][] requirements = new String[0][];

    private AdvancementProgress(Map<String, CriterionProgress> criteriaProgresses) {
        this.criteria = criteriaProgresses;
    }

    public AdvancementProgress() {
        this.criteria = Maps.newHashMap();
    }

    public void update(Map<String, Criterion> criteria, String[][] requirements) {
        Set<String> set = criteria.keySet();
        this.criteria.entrySet().removeIf((progress) -> {
            return !set.contains(progress.getKey());
        });

        for(String string : set) {
            if (!this.criteria.containsKey(string)) {
                this.criteria.put(string, new CriterionProgress());
            }
        }

        this.requirements = requirements;
    }

    public boolean isDone() {
        if (this.requirements.length == 0) {
            return false;
        } else {
            for(String[] strings : this.requirements) {
                boolean bl = false;

                for(String string : strings) {
                    CriterionProgress criterionProgress = this.getCriterion(string);
                    if (criterionProgress != null && criterionProgress.isDone()) {
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

    public boolean hasProgress() {
        for(CriterionProgress criterionProgress : this.criteria.values()) {
            if (criterionProgress.isDone()) {
                return true;
            }
        }

        return false;
    }

    public boolean grantProgress(String name) {
        CriterionProgress criterionProgress = this.criteria.get(name);
        if (criterionProgress != null && !criterionProgress.isDone()) {
            criterionProgress.grant();
            return true;
        } else {
            return false;
        }
    }

    public boolean revokeProgress(String name) {
        CriterionProgress criterionProgress = this.criteria.get(name);
        if (criterionProgress != null && criterionProgress.isDone()) {
            criterionProgress.revoke();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "AdvancementProgress{criteria=" + this.criteria + ", requirements=" + Arrays.deepToString(this.requirements) + "}";
    }

    public void serializeToNetwork(FriendlyByteBuf buf) {
        buf.writeMap(this.criteria, FriendlyByteBuf::writeUtf, (bufx, progresses) -> {
            progresses.serializeToNetwork(bufx);
        });
    }

    public static AdvancementProgress fromNetwork(FriendlyByteBuf buf) {
        Map<String, CriterionProgress> map = buf.readMap(FriendlyByteBuf::readUtf, CriterionProgress::fromNetwork);
        return new AdvancementProgress(map);
    }

    @Nullable
    public CriterionProgress getCriterion(String name) {
        return this.criteria.get(name);
    }

    public float getPercent() {
        if (this.criteria.isEmpty()) {
            return 0.0F;
        } else {
            float f = (float)this.requirements.length;
            float g = (float)this.countCompletedRequirements();
            return g / f;
        }
    }

    @Nullable
    public String getProgressText() {
        if (this.criteria.isEmpty()) {
            return null;
        } else {
            int i = this.requirements.length;
            if (i <= 1) {
                return null;
            } else {
                int j = this.countCompletedRequirements();
                return j + "/" + i;
            }
        }
    }

    private int countCompletedRequirements() {
        int i = 0;

        for(String[] strings : this.requirements) {
            boolean bl = false;

            for(String string : strings) {
                CriterionProgress criterionProgress = this.getCriterion(string);
                if (criterionProgress != null && criterionProgress.isDone()) {
                    bl = true;
                    break;
                }
            }

            if (bl) {
                ++i;
            }
        }

        return i;
    }

    public Iterable<String> getRemainingCriteria() {
        List<String> list = Lists.newArrayList();

        for(Entry<String, CriterionProgress> entry : this.criteria.entrySet()) {
            if (!entry.getValue().isDone()) {
                list.add(entry.getKey());
            }
        }

        return list;
    }

    public Iterable<String> getCompletedCriteria() {
        List<String> list = Lists.newArrayList();

        for(Entry<String, CriterionProgress> entry : this.criteria.entrySet()) {
            if (entry.getValue().isDone()) {
                list.add(entry.getKey());
            }
        }

        return list;
    }

    @Nullable
    public Date getFirstProgressDate() {
        Date date = null;

        for(CriterionProgress criterionProgress : this.criteria.values()) {
            if (criterionProgress.isDone() && (date == null || criterionProgress.getObtained().before(date))) {
                date = criterionProgress.getObtained();
            }
        }

        return date;
    }

    @Override
    public int compareTo(AdvancementProgress advancementProgress) {
        Date date = this.getFirstProgressDate();
        Date date2 = advancementProgress.getFirstProgressDate();
        if (date == null && date2 != null) {
            return 1;
        } else if (date != null && date2 == null) {
            return -1;
        } else {
            return date == null && date2 == null ? 0 : date.compareTo(date2);
        }
    }

    public static class Serializer implements JsonDeserializer<AdvancementProgress>, JsonSerializer<AdvancementProgress> {
        @Override
        public JsonElement serialize(AdvancementProgress advancementProgress, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            JsonObject jsonObject2 = new JsonObject();

            for(Entry<String, CriterionProgress> entry : advancementProgress.criteria.entrySet()) {
                CriterionProgress criterionProgress = entry.getValue();
                if (criterionProgress.isDone()) {
                    jsonObject2.add(entry.getKey(), criterionProgress.serializeToJson());
                }
            }

            if (!jsonObject2.entrySet().isEmpty()) {
                jsonObject.add("criteria", jsonObject2);
            }

            jsonObject.addProperty("done", advancementProgress.isDone());
            return jsonObject;
        }

        @Override
        public AdvancementProgress deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "advancement");
            JsonObject jsonObject2 = GsonHelper.getAsJsonObject(jsonObject, "criteria", new JsonObject());
            AdvancementProgress advancementProgress = new AdvancementProgress();

            for(Entry<String, JsonElement> entry : jsonObject2.entrySet()) {
                String string = entry.getKey();
                advancementProgress.criteria.put(string, CriterionProgress.fromJson(GsonHelper.convertToString(entry.getValue(), string)));
            }

            return advancementProgress;
        }
    }
}
