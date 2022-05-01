package net.minecraft.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class MultiPartGenerator implements BlockStateGenerator {
    private final Block block;
    private final List<MultiPartGenerator.Entry> parts = Lists.newArrayList();

    private MultiPartGenerator(Block block) {
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    public static MultiPartGenerator multiPart(Block block) {
        return new MultiPartGenerator(block);
    }

    public MultiPartGenerator with(List<Variant> variants) {
        this.parts.add(new MultiPartGenerator.Entry(variants));
        return this;
    }

    public MultiPartGenerator with(Variant variant) {
        return this.with(ImmutableList.of(variant));
    }

    public MultiPartGenerator with(Condition condition, List<Variant> variants) {
        this.parts.add(new MultiPartGenerator.ConditionalEntry(condition, variants));
        return this;
    }

    public MultiPartGenerator with(Condition condition, Variant... variants) {
        return this.with(condition, ImmutableList.copyOf(variants));
    }

    public MultiPartGenerator with(Condition condition, Variant variant) {
        return this.with(condition, ImmutableList.of(variant));
    }

    @Override
    public JsonElement get() {
        StateDefinition<Block, BlockState> stateDefinition = this.block.getStateDefinition();
        this.parts.forEach((entry) -> {
            entry.validate(stateDefinition);
        });
        JsonArray jsonArray = new JsonArray();
        this.parts.stream().map(MultiPartGenerator.Entry::get).forEach(jsonArray::add);
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("multipart", jsonArray);
        return jsonObject;
    }

    static class ConditionalEntry extends MultiPartGenerator.Entry {
        private final Condition condition;

        ConditionalEntry(Condition when, List<Variant> variants) {
            super(variants);
            this.condition = when;
        }

        @Override
        public void validate(StateDefinition<?, ?> stateManager) {
            this.condition.validate(stateManager);
        }

        @Override
        public void decorate(JsonObject json) {
            json.add("when", this.condition.get());
        }
    }

    static class Entry implements Supplier<JsonElement> {
        private final List<Variant> variants;

        Entry(List<Variant> variants) {
            this.variants = variants;
        }

        public void validate(StateDefinition<?, ?> stateManager) {
        }

        public void decorate(JsonObject json) {
        }

        @Override
        public JsonElement get() {
            JsonObject jsonObject = new JsonObject();
            this.decorate(jsonObject);
            jsonObject.add("apply", Variant.convertList(this.variants));
            return jsonObject;
        }
    }
}
