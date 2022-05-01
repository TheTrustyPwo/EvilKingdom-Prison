package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class CompositeEntryBase extends LootPoolEntryContainer {
    protected final LootPoolEntryContainer[] children;
    private final ComposableEntryContainer composedChildren;

    protected CompositeEntryBase(LootPoolEntryContainer[] children, LootItemCondition[] conditions) {
        super(conditions);
        this.children = children;
        this.composedChildren = this.compose(children);
    }

    @Override
    public void validate(ValidationContext reporter) {
        super.validate(reporter);
        if (this.children.length == 0) {
            reporter.reportProblem("Empty children list");
        }

        for(int i = 0; i < this.children.length; ++i) {
            this.children[i].validate(reporter.forChild(".entry[" + i + "]"));
        }

    }

    protected abstract ComposableEntryContainer compose(ComposableEntryContainer[] children);

    @Override
    public final boolean expand(LootContext context, Consumer<LootPoolEntry> choiceConsumer) {
        return !this.canRun(context) ? false : this.composedChildren.expand(context, choiceConsumer);
    }

    public static <T extends CompositeEntryBase> LootPoolEntryContainer.Serializer<T> createSerializer(CompositeEntryBase.CompositeEntryConstructor<T> factory) {
        return new LootPoolEntryContainer.Serializer<T>() {
            @Override
            public void serializeCustom(JsonObject json, T entry, JsonSerializationContext context) {
                json.add("children", context.serialize(entry.children));
            }

            @Override
            public final T deserializeCustom(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
                LootPoolEntryContainer[] lootPoolEntryContainers = GsonHelper.getAsObject(jsonObject, "children", jsonDeserializationContext, LootPoolEntryContainer[].class);
                return factory.create(lootPoolEntryContainers, lootItemConditions);
            }
        };
    }

    @FunctionalInterface
    public interface CompositeEntryConstructor<T extends CompositeEntryBase> {
        T create(LootPoolEntryContainer[] children, LootItemCondition[] conditions);
    }
}
