package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPredicate {
    public static final BlockPredicate ANY = new BlockPredicate((TagKey<Block>)null, (Set<Block>)null, StatePropertiesPredicate.ANY, NbtPredicate.ANY);
    @Nullable
    private final TagKey<Block> tag;
    @Nullable
    private final Set<Block> blocks;
    private final StatePropertiesPredicate properties;
    private final NbtPredicate nbt;

    public BlockPredicate(@Nullable TagKey<Block> tag, @Nullable Set<Block> blocks, StatePropertiesPredicate state, NbtPredicate nbt) {
        this.tag = tag;
        this.blocks = blocks;
        this.properties = state;
        this.nbt = nbt;
    }

    public boolean matches(ServerLevel world, BlockPos pos) {
        if (this == ANY) {
            return true;
        } else if (!world.isLoaded(pos)) {
            return false;
        } else {
            BlockState blockState = world.getBlockState(pos);
            if (this.tag != null && !blockState.is(this.tag)) {
                return false;
            } else if (this.blocks != null && !this.blocks.contains(blockState.getBlock())) {
                return false;
            } else if (!this.properties.matches(blockState)) {
                return false;
            } else {
                if (this.nbt != NbtPredicate.ANY) {
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity == null || !this.nbt.matches(blockEntity.saveWithFullMetadata())) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public static BlockPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "block");
            NbtPredicate nbtPredicate = NbtPredicate.fromJson(jsonObject.get("nbt"));
            Set<Block> set = null;
            JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "blocks", (JsonArray)null);
            if (jsonArray != null) {
                ImmutableSet.Builder<Block> builder = ImmutableSet.builder();

                for(JsonElement jsonElement : jsonArray) {
                    ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.convertToString(jsonElement, "block"));
                    builder.add(Registry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown block id '" + resourceLocation + "'");
                    }));
                }

                set = builder.build();
            }

            TagKey<Block> tagKey = null;
            if (jsonObject.has("tag")) {
                ResourceLocation resourceLocation2 = new ResourceLocation(GsonHelper.getAsString(jsonObject, "tag"));
                tagKey = TagKey.create(Registry.BLOCK_REGISTRY, resourceLocation2);
            }

            StatePropertiesPredicate statePropertiesPredicate = StatePropertiesPredicate.fromJson(jsonObject.get("state"));
            return new BlockPredicate(tagKey, set, statePropertiesPredicate, nbtPredicate);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.blocks != null) {
                JsonArray jsonArray = new JsonArray();

                for(Block block : this.blocks) {
                    jsonArray.add(Registry.BLOCK.getKey(block).toString());
                }

                jsonObject.add("blocks", jsonArray);
            }

            if (this.tag != null) {
                jsonObject.addProperty("tag", this.tag.location().toString());
            }

            jsonObject.add("nbt", this.nbt.serializeToJson());
            jsonObject.add("state", this.properties.serializeToJson());
            return jsonObject;
        }
    }

    public static class Builder {
        @Nullable
        private Set<Block> blocks;
        @Nullable
        private TagKey<Block> tag;
        private StatePropertiesPredicate properties = StatePropertiesPredicate.ANY;
        private NbtPredicate nbt = NbtPredicate.ANY;

        private Builder() {
        }

        public static BlockPredicate.Builder block() {
            return new BlockPredicate.Builder();
        }

        public BlockPredicate.Builder of(Block... blocks) {
            this.blocks = ImmutableSet.copyOf(blocks);
            return this;
        }

        public BlockPredicate.Builder of(Iterable<Block> blocks) {
            this.blocks = ImmutableSet.copyOf(blocks);
            return this;
        }

        public BlockPredicate.Builder of(TagKey<Block> tag) {
            this.tag = tag;
            return this;
        }

        public BlockPredicate.Builder hasNbt(CompoundTag nbt) {
            this.nbt = new NbtPredicate(nbt);
            return this;
        }

        public BlockPredicate.Builder setProperties(StatePropertiesPredicate state) {
            this.properties = state;
            return this;
        }

        public BlockPredicate build() {
            return new BlockPredicate(this.tag, this.blocks, this.properties, this.nbt);
        }
    }
}
