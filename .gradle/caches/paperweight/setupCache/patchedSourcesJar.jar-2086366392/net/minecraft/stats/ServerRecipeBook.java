package net.minecraft.stats;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ResourceLocationException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory; // CraftBukkit

public class ServerRecipeBook extends RecipeBook {

    public static final String RECIPE_BOOK_TAG = "recipeBook";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ServerRecipeBook() {}

    public int addRecipes(Collection<Recipe<?>> recipes, ServerPlayer player) {
        List<ResourceLocation> list = Lists.newArrayList();
        int i = 0;
        Iterator iterator = recipes.iterator();

        while (iterator.hasNext()) {
            Recipe<?> irecipe = (Recipe) iterator.next();
            ResourceLocation minecraftkey = irecipe.getId();

            if (!this.known.contains(minecraftkey) && !irecipe.isSpecial() && CraftEventFactory.handlePlayerRecipeListUpdateEvent(player, minecraftkey)) { // CraftBukkit
                this.add(minecraftkey);
                this.addHighlight(minecraftkey);
                list.add(minecraftkey);
                CriteriaTriggers.RECIPE_UNLOCKED.trigger(player, irecipe);
                ++i;
            }
        }

        this.sendRecipes(ClientboundRecipePacket.State.ADD, player, list);
        return i;
    }

    public int removeRecipes(Collection<Recipe<?>> recipes, ServerPlayer player) {
        List<ResourceLocation> list = Lists.newArrayList();
        int i = 0;
        Iterator iterator = recipes.iterator();

        while (iterator.hasNext()) {
            Recipe<?> irecipe = (Recipe) iterator.next();
            ResourceLocation minecraftkey = irecipe.getId();

            if (this.known.contains(minecraftkey)) {
                this.remove(minecraftkey);
                list.add(minecraftkey);
                ++i;
            }
        }

        this.sendRecipes(ClientboundRecipePacket.State.REMOVE, player, list);
        return i;
    }

    private void sendRecipes(ClientboundRecipePacket.State action, ServerPlayer player, List<ResourceLocation> recipeIds) {
        if (player.connection == null) return; // SPIGOT-4478 during PlayerLoginEvent
        player.connection.send(new ClientboundRecipePacket(action, recipeIds, Collections.emptyList(), this.getBookSettings()));
    }

    public CompoundTag toNbt() {
        CompoundTag nbttagcompound = new CompoundTag();

        this.getBookSettings().write(nbttagcompound);
        ListTag nbttaglist = new ListTag();
        Iterator iterator = this.known.iterator();

        while (iterator.hasNext()) {
            ResourceLocation minecraftkey = (ResourceLocation) iterator.next();

            nbttaglist.add(StringTag.valueOf(minecraftkey.toString()));
        }

        nbttagcompound.put("recipes", nbttaglist);
        ListTag nbttaglist1 = new ListTag();
        Iterator iterator1 = this.highlight.iterator();

        while (iterator1.hasNext()) {
            ResourceLocation minecraftkey1 = (ResourceLocation) iterator1.next();

            nbttaglist1.add(StringTag.valueOf(minecraftkey1.toString()));
        }

        nbttagcompound.put("toBeDisplayed", nbttaglist1);
        return nbttagcompound;
    }

    public void fromNbt(CompoundTag nbt, RecipeManager recipeManager) {
        this.setBookSettings(RecipeBookSettings.read(nbt));
        ListTag nbttaglist = nbt.getList("recipes", 8);

        this.loadRecipes(nbttaglist, this::add, recipeManager);
        ListTag nbttaglist1 = nbt.getList("toBeDisplayed", 8);

        this.loadRecipes(nbttaglist1, this::addHighlight, recipeManager);
    }

    private void loadRecipes(ListTag list, Consumer<Recipe<?>> handler, RecipeManager recipeManager) {
        for (int i = 0; i < list.size(); ++i) {
            String s = list.getString(i);

            try {
                ResourceLocation minecraftkey = new ResourceLocation(s);
                Optional<? extends Recipe<?>> optional = recipeManager.byKey(minecraftkey);

                if (!optional.isPresent()) {
                    ServerRecipeBook.LOGGER.error("Tried to load unrecognized recipe: {} removed now.", minecraftkey);
                } else {
                    handler.accept((Recipe) optional.get());
                }
            } catch (ResourceLocationException resourcekeyinvalidexception) {
                ServerRecipeBook.LOGGER.error("Tried to load improperly formatted recipe: {} removed now.", s);
            }
        }

    }

    public void sendInitialRecipeBook(ServerPlayer player) {
        player.connection.send(new ClientboundRecipePacket(ClientboundRecipePacket.State.INIT, this.known, this.highlight, this.getBookSettings()));
    }
}
