package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EntityFlagsPredicate {
    public static final EntityFlagsPredicate ANY = (new EntityFlagsPredicate.Builder()).build();
    @Nullable
    private final Boolean isOnFire;
    @Nullable
    private final Boolean isCrouching;
    @Nullable
    private final Boolean isSprinting;
    @Nullable
    private final Boolean isSwimming;
    @Nullable
    private final Boolean isBaby;

    public EntityFlagsPredicate(@Nullable Boolean isOnFire, @Nullable Boolean isSneaking, @Nullable Boolean isSprinting, @Nullable Boolean isSwimming, @Nullable Boolean isBaby) {
        this.isOnFire = isOnFire;
        this.isCrouching = isSneaking;
        this.isSprinting = isSprinting;
        this.isSwimming = isSwimming;
        this.isBaby = isBaby;
    }

    public boolean matches(Entity entity) {
        if (this.isOnFire != null && entity.isOnFire() != this.isOnFire) {
            return false;
        } else if (this.isCrouching != null && entity.isCrouching() != this.isCrouching) {
            return false;
        } else if (this.isSprinting != null && entity.isSprinting() != this.isSprinting) {
            return false;
        } else if (this.isSwimming != null && entity.isSwimming() != this.isSwimming) {
            return false;
        } else {
            return this.isBaby == null || !(entity instanceof LivingEntity) || ((LivingEntity)entity).isBaby() == this.isBaby;
        }
    }

    @Nullable
    private static Boolean getOptionalBoolean(JsonObject json, String key) {
        return json.has(key) ? GsonHelper.getAsBoolean(json, key) : null;
    }

    public static EntityFlagsPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "entity flags");
            Boolean boolean_ = getOptionalBoolean(jsonObject, "is_on_fire");
            Boolean boolean2 = getOptionalBoolean(jsonObject, "is_sneaking");
            Boolean boolean3 = getOptionalBoolean(jsonObject, "is_sprinting");
            Boolean boolean4 = getOptionalBoolean(jsonObject, "is_swimming");
            Boolean boolean5 = getOptionalBoolean(jsonObject, "is_baby");
            return new EntityFlagsPredicate(boolean_, boolean2, boolean3, boolean4, boolean5);
        } else {
            return ANY;
        }
    }

    private void addOptionalBoolean(JsonObject json, String key, @Nullable Boolean value) {
        if (value != null) {
            json.addProperty(key, value);
        }

    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            this.addOptionalBoolean(jsonObject, "is_on_fire", this.isOnFire);
            this.addOptionalBoolean(jsonObject, "is_sneaking", this.isCrouching);
            this.addOptionalBoolean(jsonObject, "is_sprinting", this.isSprinting);
            this.addOptionalBoolean(jsonObject, "is_swimming", this.isSwimming);
            this.addOptionalBoolean(jsonObject, "is_baby", this.isBaby);
            return jsonObject;
        }
    }

    public static class Builder {
        @Nullable
        private Boolean isOnFire;
        @Nullable
        private Boolean isCrouching;
        @Nullable
        private Boolean isSprinting;
        @Nullable
        private Boolean isSwimming;
        @Nullable
        private Boolean isBaby;

        public static EntityFlagsPredicate.Builder flags() {
            return new EntityFlagsPredicate.Builder();
        }

        public EntityFlagsPredicate.Builder setOnFire(@Nullable Boolean onFire) {
            this.isOnFire = onFire;
            return this;
        }

        public EntityFlagsPredicate.Builder setCrouching(@Nullable Boolean sneaking) {
            this.isCrouching = sneaking;
            return this;
        }

        public EntityFlagsPredicate.Builder setSprinting(@Nullable Boolean sprinting) {
            this.isSprinting = sprinting;
            return this;
        }

        public EntityFlagsPredicate.Builder setSwimming(@Nullable Boolean swimming) {
            this.isSwimming = swimming;
            return this;
        }

        public EntityFlagsPredicate.Builder setIsBaby(@Nullable Boolean isBaby) {
            this.isBaby = isBaby;
            return this;
        }

        public EntityFlagsPredicate build() {
            return new EntityFlagsPredicate(this.isOnFire, this.isCrouching, this.isSprinting, this.isSwimming, this.isBaby);
        }
    }
}
