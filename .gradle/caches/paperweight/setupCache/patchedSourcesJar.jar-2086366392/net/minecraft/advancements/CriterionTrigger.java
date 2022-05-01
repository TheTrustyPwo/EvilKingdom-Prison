package net.minecraft.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;

public interface CriterionTrigger<T extends CriterionTriggerInstance> {
    ResourceLocation getId();

    void addPlayerListener(PlayerAdvancements manager, CriterionTrigger.Listener<T> conditions);

    void removePlayerListener(PlayerAdvancements manager, CriterionTrigger.Listener<T> conditions);

    void removePlayerListeners(PlayerAdvancements tracker);

    T createInstance(JsonObject obj, DeserializationContext predicateDeserializer);

    public static class Listener<T extends CriterionTriggerInstance> {
        private final T trigger;
        private final Advancement advancement;
        private final String criterion;

        public Listener(T conditions, Advancement advancement, String id) {
            this.trigger = conditions;
            this.advancement = advancement;
            this.criterion = id;
        }

        public T getTriggerInstance() {
            return this.trigger;
        }

        public void run(PlayerAdvancements tracker) {
            tracker.award(this.advancement, this.criterion);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                CriterionTrigger.Listener<?> listener = (CriterionTrigger.Listener)object;
                if (!this.trigger.equals(listener.trigger)) {
                    return false;
                } else {
                    return !this.advancement.equals(listener.advancement) ? false : this.criterion.equals(listener.criterion);
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.trigger.hashCode();
            i = 31 * i + this.advancement.hashCode();
            return 31 * i + this.criterion.hashCode();
        }
    }
}
