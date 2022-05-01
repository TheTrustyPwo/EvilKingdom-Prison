package net.minecraft.advancements.critereon;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PlayerPredicate {
    public static final PlayerPredicate ANY = (new PlayerPredicate.Builder()).build();
    public static final int LOOKING_AT_RANGE = 100;
    private final MinMaxBounds.Ints level;
    @Nullable
    private final GameType gameType;
    private final Map<Stat<?>, MinMaxBounds.Ints> stats;
    private final Object2BooleanMap<ResourceLocation> recipes;
    private final Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements;
    private final EntityPredicate lookingAt;

    private static PlayerPredicate.AdvancementPredicate advancementPredicateFromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            boolean bl = json.getAsBoolean();
            return new PlayerPredicate.AdvancementDonePredicate(bl);
        } else {
            Object2BooleanMap<String> object2BooleanMap = new Object2BooleanOpenHashMap<>();
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "criterion data");
            jsonObject.entrySet().forEach((entry) -> {
                boolean bl = GsonHelper.convertToBoolean(entry.getValue(), "criterion test");
                object2BooleanMap.put(entry.getKey(), bl);
            });
            return new PlayerPredicate.AdvancementCriterionsPredicate(object2BooleanMap);
        }
    }

    PlayerPredicate(MinMaxBounds.Ints experienceLevel, @Nullable GameType gameMode, Map<Stat<?>, MinMaxBounds.Ints> stats, Object2BooleanMap<ResourceLocation> recipes, Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements, EntityPredicate lookingAt) {
        this.level = experienceLevel;
        this.gameType = gameMode;
        this.stats = stats;
        this.recipes = recipes;
        this.advancements = advancements;
        this.lookingAt = lookingAt;
    }

    public boolean matches(Entity entity) {
        if (this == ANY) {
            return true;
        } else if (!(entity instanceof ServerPlayer)) {
            return false;
        } else {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            if (!this.level.matches(serverPlayer.experienceLevel)) {
                return false;
            } else if (this.gameType != null && this.gameType != serverPlayer.gameMode.getGameModeForPlayer()) {
                return false;
            } else {
                StatsCounter statsCounter = serverPlayer.getStats();

                for(Entry<Stat<?>, MinMaxBounds.Ints> entry : this.stats.entrySet()) {
                    int i = statsCounter.getValue(entry.getKey());
                    if (!entry.getValue().matches(i)) {
                        return false;
                    }
                }

                RecipeBook recipeBook = serverPlayer.getRecipeBook();

                for(it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry<ResourceLocation> entry2 : this.recipes.object2BooleanEntrySet()) {
                    if (recipeBook.contains(entry2.getKey()) != entry2.getBooleanValue()) {
                        return false;
                    }
                }

                if (!this.advancements.isEmpty()) {
                    PlayerAdvancements playerAdvancements = serverPlayer.getAdvancements();
                    ServerAdvancementManager serverAdvancementManager = serverPlayer.getServer().getAdvancements();

                    for(Entry<ResourceLocation, PlayerPredicate.AdvancementPredicate> entry3 : this.advancements.entrySet()) {
                        Advancement advancement = serverAdvancementManager.getAdvancement(entry3.getKey());
                        if (advancement == null || !entry3.getValue().test(playerAdvancements.getOrStartProgress(advancement))) {
                            return false;
                        }
                    }
                }

                if (this.lookingAt != EntityPredicate.ANY) {
                    Vec3 vec3 = serverPlayer.getEyePosition();
                    Vec3 vec32 = serverPlayer.getViewVector(1.0F);
                    Vec3 vec33 = vec3.add(vec32.x * 100.0D, vec32.y * 100.0D, vec32.z * 100.0D);
                    EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(serverPlayer.level, serverPlayer, vec3, vec33, (new AABB(vec3, vec33)).inflate(1.0D), (entityx) -> {
                        return !entityx.isSpectator();
                    }, 0.0F);
                    if (entityHitResult == null || entityHitResult.getType() != HitResult.Type.ENTITY) {
                        return false;
                    }

                    Entity entity2 = entityHitResult.getEntity();
                    if (!this.lookingAt.matches(serverPlayer, entity2) || !serverPlayer.hasLineOfSight(entity2)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public static PlayerPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "player");
            MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("level"));
            String string = GsonHelper.getAsString(jsonObject, "gamemode", "");
            GameType gameType = GameType.byName(string, (GameType)null);
            Map<Stat<?>, MinMaxBounds.Ints> map = Maps.newHashMap();
            JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "stats", (JsonArray)null);
            if (jsonArray != null) {
                for(JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject2 = GsonHelper.convertToJsonObject(jsonElement, "stats entry");
                    ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject2, "type"));
                    StatType<?> statType = Registry.STAT_TYPE.get(resourceLocation);
                    if (statType == null) {
                        throw new JsonParseException("Invalid stat type: " + resourceLocation);
                    }

                    ResourceLocation resourceLocation2 = new ResourceLocation(GsonHelper.getAsString(jsonObject2, "stat"));
                    Stat<?> stat = getStat(statType, resourceLocation2);
                    MinMaxBounds.Ints ints2 = MinMaxBounds.Ints.fromJson(jsonObject2.get("value"));
                    map.put(stat, ints2);
                }
            }

            Object2BooleanMap<ResourceLocation> object2BooleanMap = new Object2BooleanOpenHashMap<>();
            JsonObject jsonObject3 = GsonHelper.getAsJsonObject(jsonObject, "recipes", new JsonObject());

            for(Entry<String, JsonElement> entry : jsonObject3.entrySet()) {
                ResourceLocation resourceLocation3 = new ResourceLocation(entry.getKey());
                boolean bl = GsonHelper.convertToBoolean(entry.getValue(), "recipe present");
                object2BooleanMap.put(resourceLocation3, bl);
            }

            Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> map2 = Maps.newHashMap();
            JsonObject jsonObject4 = GsonHelper.getAsJsonObject(jsonObject, "advancements", new JsonObject());

            for(Entry<String, JsonElement> entry2 : jsonObject4.entrySet()) {
                ResourceLocation resourceLocation4 = new ResourceLocation(entry2.getKey());
                PlayerPredicate.AdvancementPredicate advancementPredicate = advancementPredicateFromJson(entry2.getValue());
                map2.put(resourceLocation4, advancementPredicate);
            }

            EntityPredicate entityPredicate = EntityPredicate.fromJson(jsonObject.get("looking_at"));
            return new PlayerPredicate(ints, gameType, map, object2BooleanMap, map2, entityPredicate);
        } else {
            return ANY;
        }
    }

    private static <T> Stat<T> getStat(StatType<T> type, ResourceLocation id) {
        Registry<T> registry = type.getRegistry();
        T object = registry.get(id);
        if (object == null) {
            throw new JsonParseException("Unknown object " + id + " for stat type " + Registry.STAT_TYPE.getKey(type));
        } else {
            return type.get(object);
        }
    }

    private static <T> ResourceLocation getStatValueId(Stat<T> stat) {
        return stat.getType().getRegistry().getKey(stat.getValue());
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("level", this.level.serializeToJson());
            if (this.gameType != null) {
                jsonObject.addProperty("gamemode", this.gameType.getName());
            }

            if (!this.stats.isEmpty()) {
                JsonArray jsonArray = new JsonArray();
                this.stats.forEach((stat, ints) -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("type", Registry.STAT_TYPE.getKey(stat.getType()).toString());
                    jsonObject.addProperty("stat", getStatValueId(stat).toString());
                    jsonObject.add("value", ints.serializeToJson());
                    jsonArray.add(jsonObject);
                });
                jsonObject.add("stats", jsonArray);
            }

            if (!this.recipes.isEmpty()) {
                JsonObject jsonObject2 = new JsonObject();
                this.recipes.forEach((id, boolean_) -> {
                    jsonObject2.addProperty(id.toString(), boolean_);
                });
                jsonObject.add("recipes", jsonObject2);
            }

            if (!this.advancements.isEmpty()) {
                JsonObject jsonObject3 = new JsonObject();
                this.advancements.forEach((id, advancementPredicate) -> {
                    jsonObject3.add(id.toString(), advancementPredicate.toJson());
                });
                jsonObject.add("advancements", jsonObject3);
            }

            jsonObject.add("looking_at", this.lookingAt.serializeToJson());
            return jsonObject;
        }
    }

    static class AdvancementCriterionsPredicate implements PlayerPredicate.AdvancementPredicate {
        private final Object2BooleanMap<String> criterions;

        public AdvancementCriterionsPredicate(Object2BooleanMap<String> criteria) {
            this.criterions = criteria;
        }

        @Override
        public JsonElement toJson() {
            JsonObject jsonObject = new JsonObject();
            this.criterions.forEach(jsonObject::addProperty);
            return jsonObject;
        }

        @Override
        public boolean test(AdvancementProgress advancementProgress) {
            for(it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry<String> entry : this.criterions.object2BooleanEntrySet()) {
                CriterionProgress criterionProgress = advancementProgress.getCriterion(entry.getKey());
                if (criterionProgress == null || criterionProgress.isDone() != entry.getBooleanValue()) {
                    return false;
                }
            }

            return true;
        }
    }

    static class AdvancementDonePredicate implements PlayerPredicate.AdvancementPredicate {
        private final boolean state;

        public AdvancementDonePredicate(boolean done) {
            this.state = done;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(this.state);
        }

        @Override
        public boolean test(AdvancementProgress advancementProgress) {
            return advancementProgress.isDone() == this.state;
        }
    }

    interface AdvancementPredicate extends Predicate<AdvancementProgress> {
        JsonElement toJson();
    }

    public static class Builder {
        private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
        @Nullable
        private GameType gameType;
        private final Map<Stat<?>, MinMaxBounds.Ints> stats = Maps.newHashMap();
        private final Object2BooleanMap<ResourceLocation> recipes = new Object2BooleanOpenHashMap<>();
        private final Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements = Maps.newHashMap();
        private EntityPredicate lookingAt = EntityPredicate.ANY;

        public static PlayerPredicate.Builder player() {
            return new PlayerPredicate.Builder();
        }

        public PlayerPredicate.Builder setLevel(MinMaxBounds.Ints experienceLevel) {
            this.level = experienceLevel;
            return this;
        }

        public PlayerPredicate.Builder addStat(Stat<?> stat, MinMaxBounds.Ints value) {
            this.stats.put(stat, value);
            return this;
        }

        public PlayerPredicate.Builder addRecipe(ResourceLocation id, boolean unlocked) {
            this.recipes.put(id, unlocked);
            return this;
        }

        public PlayerPredicate.Builder setGameType(GameType gameMode) {
            this.gameType = gameMode;
            return this;
        }

        public PlayerPredicate.Builder setLookingAt(EntityPredicate lookingAt) {
            this.lookingAt = lookingAt;
            return this;
        }

        public PlayerPredicate.Builder checkAdvancementDone(ResourceLocation id, boolean done) {
            this.advancements.put(id, new PlayerPredicate.AdvancementDonePredicate(done));
            return this;
        }

        public PlayerPredicate.Builder checkAdvancementCriterions(ResourceLocation id, Map<String, Boolean> criteria) {
            this.advancements.put(id, new PlayerPredicate.AdvancementCriterionsPredicate(new Object2BooleanOpenHashMap<>(criteria)));
            return this;
        }

        public PlayerPredicate build() {
            return new PlayerPredicate(this.level, this.gameType, this.stats, this.recipes, this.advancements, this.lookingAt);
        }
    }
}
