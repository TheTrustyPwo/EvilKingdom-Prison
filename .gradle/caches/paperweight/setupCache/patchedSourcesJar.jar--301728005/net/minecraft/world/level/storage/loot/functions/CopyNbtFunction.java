package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;

public class CopyNbtFunction extends LootItemConditionalFunction {
    final NbtProvider source;
    final List<CopyNbtFunction.CopyOperation> operations;

    CopyNbtFunction(LootItemCondition[] conditions, NbtProvider source, List<CopyNbtFunction.CopyOperation> operations) {
        super(conditions);
        this.source = source;
        this.operations = ImmutableList.copyOf(operations);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.COPY_NBT;
    }

    static NbtPathArgument.NbtPath compileNbtPath(String nbtPath) {
        try {
            return (new NbtPathArgument()).parse(new StringReader(nbtPath));
        } catch (CommandSyntaxException var2) {
            throw new IllegalArgumentException("Failed to parse path " + nbtPath, var2);
        }
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.source.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Tag tag = this.source.get(context);
        if (tag != null) {
            this.operations.forEach((operation) -> {
                operation.apply(stack::getOrCreateTag, tag);
            });
        }

        return stack;
    }

    public static CopyNbtFunction.Builder copyData(NbtProvider source) {
        return new CopyNbtFunction.Builder(source);
    }

    public static CopyNbtFunction.Builder copyData(LootContext.EntityTarget target) {
        return new CopyNbtFunction.Builder(ContextNbtProvider.forContextEntity(target));
    }

    public static class Builder extends LootItemConditionalFunction.Builder<CopyNbtFunction.Builder> {
        private final NbtProvider source;
        private final List<CopyNbtFunction.CopyOperation> ops = Lists.newArrayList();

        Builder(NbtProvider source) {
            this.source = source;
        }

        public CopyNbtFunction.Builder copy(String source, String target, CopyNbtFunction.MergeStrategy operator) {
            this.ops.add(new CopyNbtFunction.CopyOperation(source, target, operator));
            return this;
        }

        public CopyNbtFunction.Builder copy(String source, String target) {
            return this.copy(source, target, CopyNbtFunction.MergeStrategy.REPLACE);
        }

        @Override
        protected CopyNbtFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyNbtFunction(this.getConditions(), this.source, this.ops);
        }
    }

    static class CopyOperation {
        private final String sourcePathText;
        private final NbtPathArgument.NbtPath sourcePath;
        private final String targetPathText;
        private final NbtPathArgument.NbtPath targetPath;
        private final CopyNbtFunction.MergeStrategy op;

        CopyOperation(String sourcePath, String targetPath, CopyNbtFunction.MergeStrategy operator) {
            this.sourcePathText = sourcePath;
            this.sourcePath = CopyNbtFunction.compileNbtPath(sourcePath);
            this.targetPathText = targetPath;
            this.targetPath = CopyNbtFunction.compileNbtPath(targetPath);
            this.op = operator;
        }

        public void apply(Supplier<Tag> itemNbtGetter, Tag sourceEntityNbt) {
            try {
                List<Tag> list = this.sourcePath.get(sourceEntityNbt);
                if (!list.isEmpty()) {
                    this.op.merge(itemNbtGetter.get(), this.targetPath, list);
                }
            } catch (CommandSyntaxException var4) {
            }

        }

        public JsonObject toJson() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("source", this.sourcePathText);
            jsonObject.addProperty("target", this.targetPathText);
            jsonObject.addProperty("op", this.op.name);
            return jsonObject;
        }

        public static CopyNbtFunction.CopyOperation fromJson(JsonObject json) {
            String string = GsonHelper.getAsString(json, "source");
            String string2 = GsonHelper.getAsString(json, "target");
            CopyNbtFunction.MergeStrategy mergeStrategy = CopyNbtFunction.MergeStrategy.getByName(GsonHelper.getAsString(json, "op"));
            return new CopyNbtFunction.CopyOperation(string, string2, mergeStrategy);
        }
    }

    public static enum MergeStrategy {
        REPLACE("replace") {
            @Override
            public void merge(Tag itemNbt, NbtPathArgument.NbtPath targetPath, List<Tag> sourceNbts) throws CommandSyntaxException {
                targetPath.set(itemNbt, Iterables.getLast(sourceNbts)::copy);
            }
        },
        APPEND("append") {
            @Override
            public void merge(Tag itemNbt, NbtPathArgument.NbtPath targetPath, List<Tag> sourceNbts) throws CommandSyntaxException {
                List<Tag> list = targetPath.getOrCreate(itemNbt, ListTag::new);
                list.forEach((foundNbt) -> {
                    if (foundNbt instanceof ListTag) {
                        sourceNbts.forEach((sourceNbt) -> {
                            ((ListTag)foundNbt).add(sourceNbt.copy());
                        });
                    }

                });
            }
        },
        MERGE("merge") {
            @Override
            public void merge(Tag itemNbt, NbtPathArgument.NbtPath targetPath, List<Tag> sourceNbts) throws CommandSyntaxException {
                List<Tag> list = targetPath.getOrCreate(itemNbt, CompoundTag::new);
                list.forEach((foundNbt) -> {
                    if (foundNbt instanceof CompoundTag) {
                        sourceNbts.forEach((sourceNbt) -> {
                            if (sourceNbt instanceof CompoundTag) {
                                ((CompoundTag)foundNbt).merge((CompoundTag)sourceNbt);
                            }

                        });
                    }

                });
            }
        };

        final String name;

        public abstract void merge(Tag itemNbt, NbtPathArgument.NbtPath targetPath, List<Tag> sourceNbts) throws CommandSyntaxException;

        MergeStrategy(String name) {
            this.name = name;
        }

        public static CopyNbtFunction.MergeStrategy getByName(String name) {
            for(CopyNbtFunction.MergeStrategy mergeStrategy : values()) {
                if (mergeStrategy.name.equals(name)) {
                    return mergeStrategy;
                }
            }

            throw new IllegalArgumentException("Invalid merge strategy" + name);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<CopyNbtFunction> {
        @Override
        public void serialize(JsonObject json, CopyNbtFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("source", context.serialize(object.source));
            JsonArray jsonArray = new JsonArray();
            object.operations.stream().map(CopyNbtFunction.CopyOperation::toJson).forEach(jsonArray::add);
            json.add("ops", jsonArray);
        }

        @Override
        public CopyNbtFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            NbtProvider nbtProvider = GsonHelper.getAsObject(jsonObject, "source", jsonDeserializationContext, NbtProvider.class);
            List<CopyNbtFunction.CopyOperation> list = Lists.newArrayList();

            for(JsonElement jsonElement : GsonHelper.getAsJsonArray(jsonObject, "ops")) {
                JsonObject jsonObject2 = GsonHelper.convertToJsonObject(jsonElement, "op");
                list.add(CopyNbtFunction.CopyOperation.fromJson(jsonObject2));
            }

            return new CopyNbtFunction(lootItemConditions, nbtProvider, list);
        }
    }
}
