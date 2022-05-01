package net.minecraft.advancements;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class AdvancementRewards {
    public static final AdvancementRewards EMPTY = new AdvancementRewards(0, new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE);
    private final int experience;
    private final ResourceLocation[] loot;
    private final ResourceLocation[] recipes;
    private final CommandFunction.CacheableFunction function;

    public AdvancementRewards(int experience, ResourceLocation[] loot, ResourceLocation[] recipes, CommandFunction.CacheableFunction function) {
        this.experience = experience;
        this.loot = loot;
        this.recipes = recipes;
        this.function = function;
    }

    public ResourceLocation[] getRecipes() {
        return this.recipes;
    }

    public void grant(ServerPlayer player) {
        player.giveExperiencePoints(this.experience);
        LootContext lootContext = (new LootContext.Builder(player.getLevel())).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.ORIGIN, player.position()).withRandom(player.getRandom()).create(LootContextParamSets.ADVANCEMENT_REWARD);
        boolean bl = false;

        for(ResourceLocation resourceLocation : this.loot) {
            for(ItemStack itemStack : player.server.getLootTables().get(resourceLocation).getRandomItems(lootContext)) {
                if (player.addItem(itemStack)) {
                    player.level.playSound((Player)null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    bl = true;
                } else {
                    ItemEntity itemEntity = player.drop(itemStack, false);
                    if (itemEntity != null) {
                        itemEntity.setNoPickUpDelay();
                        itemEntity.setOwner(player.getUUID());
                    }
                }
            }
        }

        if (bl) {
            player.containerMenu.broadcastChanges();
        }

        if (this.recipes.length > 0) {
            player.awardRecipesByKey(this.recipes);
        }

        MinecraftServer minecraftServer = player.server;
        this.function.get(minecraftServer.getFunctions()).ifPresent((commandFunction) -> {
            minecraftServer.getFunctions().execute(commandFunction, player.createCommandSourceStack().withSuppressedOutput().withPermission(2));
        });
    }

    @Override
    public String toString() {
        return "AdvancementRewards{experience=" + this.experience + ", loot=" + Arrays.toString((Object[])this.loot) + ", recipes=" + Arrays.toString((Object[])this.recipes) + ", function=" + this.function + "}";
    }

    public JsonElement serializeToJson() {
        if (this == EMPTY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.experience != 0) {
                jsonObject.addProperty("experience", this.experience);
            }

            if (this.loot.length > 0) {
                JsonArray jsonArray = new JsonArray();

                for(ResourceLocation resourceLocation : this.loot) {
                    jsonArray.add(resourceLocation.toString());
                }

                jsonObject.add("loot", jsonArray);
            }

            if (this.recipes.length > 0) {
                JsonArray jsonArray2 = new JsonArray();

                for(ResourceLocation resourceLocation2 : this.recipes) {
                    jsonArray2.add(resourceLocation2.toString());
                }

                jsonObject.add("recipes", jsonArray2);
            }

            if (this.function.getId() != null) {
                jsonObject.addProperty("function", this.function.getId().toString());
            }

            return jsonObject;
        }
    }

    public static AdvancementRewards deserialize(JsonObject json) throws JsonParseException {
        int i = GsonHelper.getAsInt(json, "experience", 0);
        JsonArray jsonArray = GsonHelper.getAsJsonArray(json, "loot", new JsonArray());
        ResourceLocation[] resourceLocations = new ResourceLocation[jsonArray.size()];

        for(int j = 0; j < resourceLocations.length; ++j) {
            resourceLocations[j] = new ResourceLocation(GsonHelper.convertToString(jsonArray.get(j), "loot[" + j + "]"));
        }

        JsonArray jsonArray2 = GsonHelper.getAsJsonArray(json, "recipes", new JsonArray());
        ResourceLocation[] resourceLocations2 = new ResourceLocation[jsonArray2.size()];

        for(int k = 0; k < resourceLocations2.length; ++k) {
            resourceLocations2[k] = new ResourceLocation(GsonHelper.convertToString(jsonArray2.get(k), "recipes[" + k + "]"));
        }

        CommandFunction.CacheableFunction cacheableFunction;
        if (json.has("function")) {
            cacheableFunction = new CommandFunction.CacheableFunction(new ResourceLocation(GsonHelper.getAsString(json, "function")));
        } else {
            cacheableFunction = CommandFunction.CacheableFunction.NONE;
        }

        return new AdvancementRewards(i, resourceLocations, resourceLocations2, cacheableFunction);
    }

    public static class Builder {
        private int experience;
        private final List<ResourceLocation> loot = Lists.newArrayList();
        private final List<ResourceLocation> recipes = Lists.newArrayList();
        @Nullable
        private ResourceLocation function;

        public static AdvancementRewards.Builder experience(int experience) {
            return (new AdvancementRewards.Builder()).addExperience(experience);
        }

        public AdvancementRewards.Builder addExperience(int experience) {
            this.experience += experience;
            return this;
        }

        public static AdvancementRewards.Builder loot(ResourceLocation loot) {
            return (new AdvancementRewards.Builder()).addLootTable(loot);
        }

        public AdvancementRewards.Builder addLootTable(ResourceLocation loot) {
            this.loot.add(loot);
            return this;
        }

        public static AdvancementRewards.Builder recipe(ResourceLocation recipe) {
            return (new AdvancementRewards.Builder()).addRecipe(recipe);
        }

        public AdvancementRewards.Builder addRecipe(ResourceLocation recipe) {
            this.recipes.add(recipe);
            return this;
        }

        public static AdvancementRewards.Builder function(ResourceLocation function) {
            return (new AdvancementRewards.Builder()).runs(function);
        }

        public AdvancementRewards.Builder runs(ResourceLocation function) {
            this.function = function;
            return this;
        }

        public AdvancementRewards build() {
            return new AdvancementRewards(this.experience, this.loot.toArray(new ResourceLocation[0]), this.recipes.toArray(new ResourceLocation[0]), this.function == null ? CommandFunction.CacheableFunction.NONE : new CommandFunction.CacheableFunction(this.function));
        }
    }
}
