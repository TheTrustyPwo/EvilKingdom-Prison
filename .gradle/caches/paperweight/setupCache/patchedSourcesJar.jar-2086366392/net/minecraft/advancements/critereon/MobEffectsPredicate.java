package net.minecraft.advancements.critereon;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class MobEffectsPredicate {
    public static final MobEffectsPredicate ANY = new MobEffectsPredicate(Collections.emptyMap());
    private final Map<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> effects;

    public MobEffectsPredicate(Map<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> effects) {
        this.effects = effects;
    }

    public static MobEffectsPredicate effects() {
        return new MobEffectsPredicate(Maps.newLinkedHashMap());
    }

    public MobEffectsPredicate and(MobEffect statusEffect) {
        this.effects.put(statusEffect, new MobEffectsPredicate.MobEffectInstancePredicate());
        return this;
    }

    public MobEffectsPredicate and(MobEffect statusEffect, MobEffectsPredicate.MobEffectInstancePredicate data) {
        this.effects.put(statusEffect, data);
        return this;
    }

    public boolean matches(Entity entity) {
        if (this == ANY) {
            return true;
        } else {
            return entity instanceof LivingEntity ? this.matches(((LivingEntity)entity).getActiveEffectsMap()) : false;
        }
    }

    public boolean matches(LivingEntity livingEntity) {
        return this == ANY ? true : this.matches(livingEntity.getActiveEffectsMap());
    }

    public boolean matches(Map<MobEffect, MobEffectInstance> effects) {
        if (this == ANY) {
            return true;
        } else {
            for(Entry<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> entry : this.effects.entrySet()) {
                MobEffectInstance mobEffectInstance = effects.get(entry.getKey());
                if (!entry.getValue().matches(mobEffectInstance)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static MobEffectsPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "effects");
            Map<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> map = Maps.newLinkedHashMap();

            for(Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                ResourceLocation resourceLocation = new ResourceLocation(entry.getKey());
                MobEffect mobEffect = Registry.MOB_EFFECT.getOptional(resourceLocation).orElseThrow(() -> {
                    return new JsonSyntaxException("Unknown effect '" + resourceLocation + "'");
                });
                MobEffectsPredicate.MobEffectInstancePredicate mobEffectInstancePredicate = MobEffectsPredicate.MobEffectInstancePredicate.fromJson(GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey()));
                map.put(mobEffect, mobEffectInstancePredicate);
            }

            return new MobEffectsPredicate(map);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();

            for(Entry<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> entry : this.effects.entrySet()) {
                jsonObject.add(Registry.MOB_EFFECT.getKey(entry.getKey()).toString(), entry.getValue().serializeToJson());
            }

            return jsonObject;
        }
    }

    public static class MobEffectInstancePredicate {
        private final MinMaxBounds.Ints amplifier;
        private final MinMaxBounds.Ints duration;
        @Nullable
        private final Boolean ambient;
        @Nullable
        private final Boolean visible;

        public MobEffectInstancePredicate(MinMaxBounds.Ints amplifier, MinMaxBounds.Ints duration, @Nullable Boolean ambient, @Nullable Boolean visible) {
            this.amplifier = amplifier;
            this.duration = duration;
            this.ambient = ambient;
            this.visible = visible;
        }

        public MobEffectInstancePredicate() {
            this(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, (Boolean)null, (Boolean)null);
        }

        public boolean matches(@Nullable MobEffectInstance statusEffectInstance) {
            if (statusEffectInstance == null) {
                return false;
            } else if (!this.amplifier.matches(statusEffectInstance.getAmplifier())) {
                return false;
            } else if (!this.duration.matches(statusEffectInstance.getDuration())) {
                return false;
            } else if (this.ambient != null && this.ambient != statusEffectInstance.isAmbient()) {
                return false;
            } else {
                return this.visible == null || this.visible == statusEffectInstance.isVisible();
            }
        }

        public JsonElement serializeToJson() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("amplifier", this.amplifier.serializeToJson());
            jsonObject.add("duration", this.duration.serializeToJson());
            jsonObject.addProperty("ambient", this.ambient);
            jsonObject.addProperty("visible", this.visible);
            return jsonObject;
        }

        public static MobEffectsPredicate.MobEffectInstancePredicate fromJson(JsonObject json) {
            MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(json.get("amplifier"));
            MinMaxBounds.Ints ints2 = MinMaxBounds.Ints.fromJson(json.get("duration"));
            Boolean boolean_ = json.has("ambient") ? GsonHelper.getAsBoolean(json, "ambient") : null;
            Boolean boolean2 = json.has("visible") ? GsonHelper.getAsBoolean(json, "visible") : null;
            return new MobEffectsPredicate.MobEffectInstancePredicate(ints, ints2, boolean_, boolean2);
        }
    }
}
