package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class SimpleCriterionTrigger<T extends AbstractCriterionTriggerInstance> implements CriterionTrigger<T> {
    private final Map<PlayerAdvancements, Set<CriterionTrigger.Listener<T>>> players = Maps.newIdentityHashMap();

    @Override
    public final void addPlayerListener(PlayerAdvancements manager, CriterionTrigger.Listener<T> conditions) {
        this.players.computeIfAbsent(manager, (managerx) -> {
            return Sets.newHashSet();
        }).add(conditions);
    }

    @Override
    public final void removePlayerListener(PlayerAdvancements manager, CriterionTrigger.Listener<T> conditions) {
        Set<CriterionTrigger.Listener<T>> set = this.players.get(manager);
        if (set != null) {
            set.remove(conditions);
            if (set.isEmpty()) {
                this.players.remove(manager);
            }
        }

    }

    @Override
    public final void removePlayerListeners(PlayerAdvancements tracker) {
        this.players.remove(tracker);
    }

    protected abstract T createInstance(JsonObject obj, EntityPredicate.Composite playerPredicate, DeserializationContext predicateDeserializer);

    @Override
    public final T createInstance(JsonObject jsonObject, DeserializationContext deserializationContext) {
        EntityPredicate.Composite composite = EntityPredicate.Composite.fromJson(jsonObject, "player", deserializationContext);
        return this.createInstance(jsonObject, composite, deserializationContext);
    }

    protected void trigger(ServerPlayer player, Predicate<T> predicate) {
        PlayerAdvancements playerAdvancements = player.getAdvancements();
        Set<CriterionTrigger.Listener<T>> set = this.players.get(playerAdvancements);
        if (set != null && !set.isEmpty()) {
            LootContext lootContext = EntityPredicate.createContext(player, player);
            List<CriterionTrigger.Listener<T>> list = null;

            for(CriterionTrigger.Listener<T> listener : set) {
                T abstractCriterionTriggerInstance = listener.getTriggerInstance();
                if (predicate.test(abstractCriterionTriggerInstance) && abstractCriterionTriggerInstance.getPlayerPredicate().matches(lootContext)) {
                    if (list == null) {
                        list = Lists.newArrayList();
                    }

                    list.add(listener);
                }
            }

            if (list != null) {
                for(CriterionTrigger.Listener<T> listener2 : list) {
                    listener2.run(playerAdvancements);
                }
            }

        }
    }
}
