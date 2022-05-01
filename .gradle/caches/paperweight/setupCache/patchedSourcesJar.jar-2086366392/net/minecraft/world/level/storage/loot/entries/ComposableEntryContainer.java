package net.minecraft.world.level.storage.loot.entries;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.level.storage.loot.LootContext;

@FunctionalInterface
interface ComposableEntryContainer {
    ComposableEntryContainer ALWAYS_FALSE = (lootContext, consumer) -> {
        return false;
    };
    ComposableEntryContainer ALWAYS_TRUE = (lootContext, consumer) -> {
        return true;
    };

    boolean expand(LootContext context, Consumer<LootPoolEntry> choiceConsumer);

    default ComposableEntryContainer and(ComposableEntryContainer other) {
        Objects.requireNonNull(other);
        return (context, lootChoiceExpander) -> {
            return this.expand(context, lootChoiceExpander) && other.expand(context, lootChoiceExpander);
        };
    }

    default ComposableEntryContainer or(ComposableEntryContainer other) {
        Objects.requireNonNull(other);
        return (context, lootChoiceExpander) -> {
            return this.expand(context, lootChoiceExpander) || other.expand(context, lootChoiceExpander);
        };
    }
}
