package net.minecraft.world.level.storage.loot.parameters;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.minecraft.world.level.storage.loot.ValidationContext;

public class LootContextParamSet {
    private final Set<LootContextParam<?>> required;
    private final Set<LootContextParam<?>> all;

    LootContextParamSet(Set<LootContextParam<?>> required, Set<LootContextParam<?>> allowed) {
        this.required = ImmutableSet.copyOf(required);
        this.all = ImmutableSet.copyOf(Sets.union(required, allowed));
    }

    public boolean isAllowed(LootContextParam<?> parameter) {
        return this.all.contains(parameter);
    }

    public Set<LootContextParam<?>> getRequired() {
        return this.required;
    }

    public Set<LootContextParam<?>> getAllowed() {
        return this.all;
    }

    @Override
    public String toString() {
        return "[" + Joiner.on(", ").join(this.all.stream().map((parameter) -> {
            return (this.required.contains(parameter) ? "!" : "") + parameter.getName();
        }).iterator()) + "]";
    }

    public void validateUser(ValidationContext reporter, LootContextUser parameterConsumer) {
        Set<LootContextParam<?>> set = parameterConsumer.getReferencedContextParams();
        Set<LootContextParam<?>> set2 = Sets.difference(set, this.all);
        if (!set2.isEmpty()) {
            reporter.reportProblem("Parameters " + set2 + " are not provided in this context");
        }

    }

    public static LootContextParamSet.Builder builder() {
        return new LootContextParamSet.Builder();
    }

    public static class Builder {
        private final Set<LootContextParam<?>> required = Sets.newIdentityHashSet();
        private final Set<LootContextParam<?>> optional = Sets.newIdentityHashSet();

        public LootContextParamSet.Builder required(LootContextParam<?> parameter) {
            if (this.optional.contains(parameter)) {
                throw new IllegalArgumentException("Parameter " + parameter.getName() + " is already optional");
            } else {
                this.required.add(parameter);
                return this;
            }
        }

        public LootContextParamSet.Builder optional(LootContextParam<?> parameter) {
            if (this.required.contains(parameter)) {
                throw new IllegalArgumentException("Parameter " + parameter.getName() + " is already required");
            } else {
                this.optional.add(parameter);
                return this;
            }
        }

        public LootContextParamSet build() {
            return new LootContextParamSet(this.required, this.optional);
        }
    }
}
