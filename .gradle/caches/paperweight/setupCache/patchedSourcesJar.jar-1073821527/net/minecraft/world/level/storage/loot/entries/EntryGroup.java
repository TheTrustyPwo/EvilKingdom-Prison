package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class EntryGroup extends CompositeEntryBase {
    EntryGroup(LootPoolEntryContainer[] children, LootItemCondition[] conditions) {
        super(children, conditions);
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.GROUP;
    }

    @Override
    protected ComposableEntryContainer compose(ComposableEntryContainer[] children) {
        switch(children.length) {
        case 0:
            return ALWAYS_TRUE;
        case 1:
            return children[0];
        case 2:
            ComposableEntryContainer composableEntryContainer = children[0];
            ComposableEntryContainer composableEntryContainer2 = children[1];
            return (context, consumer) -> {
                composableEntryContainer.expand(context, consumer);
                composableEntryContainer2.expand(context, consumer);
                return true;
            };
        default:
            return (context, lootChoiceExpander) -> {
                for(ComposableEntryContainer composableEntryContainer : children) {
                    composableEntryContainer.expand(context, lootChoiceExpander);
                }

                return true;
            };
        }
    }

    public static EntryGroup.Builder list(LootPoolEntryContainer.Builder<?>... entries) {
        return new EntryGroup.Builder(entries);
    }

    public static class Builder extends LootPoolEntryContainer.Builder<EntryGroup.Builder> {
        private final List<LootPoolEntryContainer> entries = Lists.newArrayList();

        public Builder(LootPoolEntryContainer.Builder<?>... entries) {
            for(LootPoolEntryContainer.Builder<?> builder : entries) {
                this.entries.add(builder.build());
            }

        }

        @Override
        protected EntryGroup.Builder getThis() {
            return this;
        }

        @Override
        public EntryGroup.Builder append(LootPoolEntryContainer.Builder<?> entry) {
            this.entries.add(entry.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new EntryGroup(this.entries.toArray(new LootPoolEntryContainer[0]), this.getConditions());
        }
    }
}
