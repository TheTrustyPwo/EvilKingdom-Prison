package net.minecraft.world.level.storage.loot;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ValidationContext {
    private final Multimap<String, String> problems;
    private final Supplier<String> context;
    private final LootContextParamSet params;
    private final Function<ResourceLocation, LootItemCondition> conditionResolver;
    private final Set<ResourceLocation> visitedConditions;
    private final Function<ResourceLocation, LootTable> tableResolver;
    private final Set<ResourceLocation> visitedTables;
    private String contextCache;

    public ValidationContext(LootContextParamSet contextType, Function<ResourceLocation, LootItemCondition> conditionGetter, Function<ResourceLocation, LootTable> tableFactory) {
        this(HashMultimap.create(), () -> {
            return "";
        }, contextType, conditionGetter, ImmutableSet.of(), tableFactory, ImmutableSet.of());
    }

    public ValidationContext(Multimap<String, String> messages, Supplier<String> nameFactory, LootContextParamSet contextType, Function<ResourceLocation, LootItemCondition> conditionGetter, Set<ResourceLocation> conditions, Function<ResourceLocation, LootTable> tableGetter, Set<ResourceLocation> tables) {
        this.problems = messages;
        this.context = nameFactory;
        this.params = contextType;
        this.conditionResolver = conditionGetter;
        this.visitedConditions = conditions;
        this.tableResolver = tableGetter;
        this.visitedTables = tables;
    }

    private String getContext() {
        if (this.contextCache == null) {
            this.contextCache = this.context.get();
        }

        return this.contextCache;
    }

    public void reportProblem(String message) {
        this.problems.put(this.getContext(), message);
    }

    public ValidationContext forChild(String name) {
        return new ValidationContext(this.problems, () -> {
            return this.getContext() + name;
        }, this.params, this.conditionResolver, this.visitedConditions, this.tableResolver, this.visitedTables);
    }

    public ValidationContext enterTable(String name, ResourceLocation id) {
        ImmutableSet<ResourceLocation> immutableSet = ImmutableSet.<ResourceLocation>builder().addAll(this.visitedTables).add(id).build();
        return new ValidationContext(this.problems, () -> {
            return this.getContext() + name;
        }, this.params, this.conditionResolver, this.visitedConditions, this.tableResolver, immutableSet);
    }

    public ValidationContext enterCondition(String name, ResourceLocation id) {
        ImmutableSet<ResourceLocation> immutableSet = ImmutableSet.<ResourceLocation>builder().addAll(this.visitedConditions).add(id).build();
        return new ValidationContext(this.problems, () -> {
            return this.getContext() + name;
        }, this.params, this.conditionResolver, immutableSet, this.tableResolver, this.visitedTables);
    }

    public boolean hasVisitedTable(ResourceLocation id) {
        return this.visitedTables.contains(id);
    }

    public boolean hasVisitedCondition(ResourceLocation id) {
        return this.visitedConditions.contains(id);
    }

    public Multimap<String, String> getProblems() {
        return ImmutableMultimap.copyOf(this.problems);
    }

    public void validateUser(LootContextUser contextAware) {
        this.params.validateUser(this, contextAware);
    }

    @Nullable
    public LootTable resolveLootTable(ResourceLocation id) {
        return this.tableResolver.apply(id);
    }

    @Nullable
    public LootItemCondition resolveCondition(ResourceLocation id) {
        return this.conditionResolver.apply(id);
    }

    public ValidationContext setParams(LootContextParamSet contextType) {
        return new ValidationContext(this.problems, this.context, contextType, this.conditionResolver, this.visitedConditions, this.tableResolver, this.visitedTables);
    }
}
