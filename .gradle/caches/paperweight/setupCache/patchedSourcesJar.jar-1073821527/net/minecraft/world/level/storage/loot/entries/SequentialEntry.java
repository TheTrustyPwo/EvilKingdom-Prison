package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SequentialEntry extends CompositeEntryBase {
    SequentialEntry(LootPoolEntryContainer[] children, LootItemCondition[] conditions) {
        super(children, conditions);
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.SEQUENCE;
    }

    @Override
    protected ComposableEntryContainer compose(ComposableEntryContainer[] children) {
        switch(children.length) {
        case 0:
            return ALWAYS_TRUE;
        case 1:
            return children[0];
        case 2:
            return children[0].and(children[1]);
        default:
            return (context, lootChoiceExpander) -> {
                for(ComposableEntryContainer composableEntryContainer : children) {
                    if (!composableEntryContainer.expand(context, lootChoiceExpander)) {
                        return false;
                    }
                }

                return true;
            };
        }
    }

    public static SequentialEntry.Builder sequential(LootPoolEntryContainer.Builder<?>... entries) {
        return new SequentialEntry.Builder(entries);
    }

    public static class Builder extends LootPoolEntryContainer.Builder<SequentialEntry.Builder> {
        private final List<LootPoolEntryContainer> entries = Lists.newArrayList();

        public Builder(LootPoolEntryContainer.Builder<?>... entries) {
            for(LootPoolEntryContainer.Builder<?> builder : entries) {
                this.entries.add(builder.build());
            }

        }

        @Override
        protected SequentialEntry.Builder getThis() {
            return this;
        }

        @Override
        public SequentialEntry.Builder then(LootPoolEntryContainer.Builder<?> entry) {
            this.entries.add(entry.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new SequentialEntry(this.entries.toArray(new LootPoolEntryContainer[0]), this.getConditions());
        }
    }
}
