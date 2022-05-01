package net.minecraft.network.syncher;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EntityDataSerializers {
    private static final CrudeIncrementalIntIdentityHashBiMap<EntityDataSerializer<?>> SERIALIZERS = CrudeIncrementalIntIdentityHashBiMap.create(16);
    public static final EntityDataSerializer<Byte> BYTE = new EntityDataSerializer<Byte>() {
        @Override
        public void write(FriendlyByteBuf buf, Byte value) {
            buf.writeByte(value);
        }

        @Override
        public Byte read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readByte();
        }

        @Override
        public Byte copy(Byte value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Integer> INT = new EntityDataSerializer<Integer>() {
        @Override
        public void write(FriendlyByteBuf buf, Integer value) {
            buf.writeVarInt(value);
        }

        @Override
        public Integer read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readVarInt();
        }

        @Override
        public Integer copy(Integer value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Float> FLOAT = new EntityDataSerializer<Float>() {
        @Override
        public void write(FriendlyByteBuf buf, Float value) {
            buf.writeFloat(value);
        }

        @Override
        public Float read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readFloat();
        }

        @Override
        public Float copy(Float value) {
            return value;
        }
    };
    public static final EntityDataSerializer<String> STRING = new EntityDataSerializer<String>() {
        @Override
        public void write(FriendlyByteBuf buf, String value) {
            buf.writeUtf(value);
        }

        @Override
        public String read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readUtf();
        }

        @Override
        public String copy(String value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Component> COMPONENT = new EntityDataSerializer<Component>() {
        @Override
        public void write(FriendlyByteBuf buf, Component value) {
            buf.writeComponent(value);
        }

        @Override
        public Component read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readComponent();
        }

        @Override
        public Component copy(Component value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Optional<Component>> OPTIONAL_COMPONENT = new EntityDataSerializer<Optional<Component>>() {
        @Override
        public void write(FriendlyByteBuf buf, Optional<Component> value) {
            if (value.isPresent()) {
                buf.writeBoolean(true);
                buf.writeComponent(value.get());
            } else {
                buf.writeBoolean(false);
            }

        }

        @Override
        public Optional<Component> read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readBoolean() ? Optional.of(friendlyByteBuf.readComponent()) : Optional.empty();
        }

        @Override
        public Optional<Component> copy(Optional<Component> value) {
            return value;
        }
    };
    public static final EntityDataSerializer<ItemStack> ITEM_STACK = new EntityDataSerializer<ItemStack>() {
        @Override
        public void write(FriendlyByteBuf buf, ItemStack value) {
            buf.writeItem(net.minecraft.world.entity.LivingEntity.sanitizeItemStack(value, false)); // Paper - prevent oversized data
        }

        @Override
        public ItemStack read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readItem();
        }

        @Override
        public ItemStack copy(ItemStack value) {
            return value.copy();
        }
    };
    public static final EntityDataSerializer<Optional<BlockState>> BLOCK_STATE = new EntityDataSerializer<Optional<BlockState>>() {
        @Override
        public void write(FriendlyByteBuf buf, Optional<BlockState> value) {
            if (value.isPresent()) {
                buf.writeVarInt(Block.getId(value.get()));
            } else {
                buf.writeVarInt(0);
            }

        }

        @Override
        public Optional<BlockState> read(FriendlyByteBuf friendlyByteBuf) {
            int i = friendlyByteBuf.readVarInt();
            return i == 0 ? Optional.empty() : Optional.of(Block.stateById(i));
        }

        @Override
        public Optional<BlockState> copy(Optional<BlockState> value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Boolean> BOOLEAN = new EntityDataSerializer<Boolean>() {
        @Override
        public void write(FriendlyByteBuf buf, Boolean value) {
            buf.writeBoolean(value);
        }

        @Override
        public Boolean read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readBoolean();
        }

        @Override
        public Boolean copy(Boolean value) {
            return value;
        }
    };
    public static final EntityDataSerializer<ParticleOptions> PARTICLE = new EntityDataSerializer<ParticleOptions>() {
        @Override
        public void write(FriendlyByteBuf buf, ParticleOptions value) {
            buf.writeVarInt(Registry.PARTICLE_TYPE.getId(value.getType()));
            value.writeToNetwork(buf);
        }

        @Override
        public ParticleOptions read(FriendlyByteBuf friendlyByteBuf) {
            return this.readParticle(friendlyByteBuf, Registry.PARTICLE_TYPE.byId(friendlyByteBuf.readVarInt()));
        }

        private <T extends ParticleOptions> T readParticle(FriendlyByteBuf buf, ParticleType<T> type) {
            return type.getDeserializer().fromNetwork(type, buf);
        }

        @Override
        public ParticleOptions copy(ParticleOptions value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Rotations> ROTATIONS = new EntityDataSerializer<Rotations>() {
        @Override
        public void write(FriendlyByteBuf buf, Rotations value) {
            buf.writeFloat(value.getX());
            buf.writeFloat(value.getY());
            buf.writeFloat(value.getZ());
        }

        @Override
        public Rotations read(FriendlyByteBuf friendlyByteBuf) {
            return new Rotations(friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat());
        }

        @Override
        public Rotations copy(Rotations value) {
            return value;
        }
    };
    public static final EntityDataSerializer<BlockPos> BLOCK_POS = new EntityDataSerializer<BlockPos>() {
        @Override
        public void write(FriendlyByteBuf buf, BlockPos value) {
            buf.writeBlockPos(value);
        }

        @Override
        public BlockPos read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readBlockPos();
        }

        @Override
        public BlockPos copy(BlockPos value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Optional<BlockPos>> OPTIONAL_BLOCK_POS = new EntityDataSerializer<Optional<BlockPos>>() {
        @Override
        public void write(FriendlyByteBuf buf, Optional<BlockPos> value) {
            buf.writeBoolean(value.isPresent());
            if (value.isPresent()) {
                buf.writeBlockPos(value.get());
            }

        }

        @Override
        public Optional<BlockPos> read(FriendlyByteBuf friendlyByteBuf) {
            return !friendlyByteBuf.readBoolean() ? Optional.empty() : Optional.of(friendlyByteBuf.readBlockPos());
        }

        @Override
        public Optional<BlockPos> copy(Optional<BlockPos> value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Direction> DIRECTION = new EntityDataSerializer<Direction>() {
        @Override
        public void write(FriendlyByteBuf buf, Direction value) {
            buf.writeEnum(value);
        }

        @Override
        public Direction read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readEnum(Direction.class);
        }

        @Override
        public Direction copy(Direction value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Optional<UUID>> OPTIONAL_UUID = new EntityDataSerializer<Optional<UUID>>() {
        @Override
        public void write(FriendlyByteBuf buf, Optional<UUID> value) {
            buf.writeBoolean(value.isPresent());
            if (value.isPresent()) {
                buf.writeUUID(value.get());
            }

        }

        @Override
        public Optional<UUID> read(FriendlyByteBuf friendlyByteBuf) {
            return !friendlyByteBuf.readBoolean() ? Optional.empty() : Optional.of(friendlyByteBuf.readUUID());
        }

        @Override
        public Optional<UUID> copy(Optional<UUID> value) {
            return value;
        }
    };
    public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG = new EntityDataSerializer<CompoundTag>() {
        @Override
        public void write(FriendlyByteBuf buf, CompoundTag value) {
            buf.writeNbt(value);
        }

        @Override
        public CompoundTag read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readNbt();
        }

        @Override
        public CompoundTag copy(CompoundTag value) {
            return value.copy();
        }
    };
    public static final EntityDataSerializer<VillagerData> VILLAGER_DATA = new EntityDataSerializer<VillagerData>() {
        @Override
        public void write(FriendlyByteBuf buf, VillagerData value) {
            buf.writeVarInt(Registry.VILLAGER_TYPE.getId(value.getType()));
            buf.writeVarInt(Registry.VILLAGER_PROFESSION.getId(value.getProfession()));
            buf.writeVarInt(value.getLevel());
        }

        @Override
        public VillagerData read(FriendlyByteBuf friendlyByteBuf) {
            return new VillagerData(Registry.VILLAGER_TYPE.byId(friendlyByteBuf.readVarInt()), Registry.VILLAGER_PROFESSION.byId(friendlyByteBuf.readVarInt()), friendlyByteBuf.readVarInt());
        }

        @Override
        public VillagerData copy(VillagerData value) {
            return value;
        }
    };
    public static final EntityDataSerializer<OptionalInt> OPTIONAL_UNSIGNED_INT = new EntityDataSerializer<OptionalInt>() {
        @Override
        public void write(FriendlyByteBuf buf, OptionalInt value) {
            buf.writeVarInt(value.orElse(-1) + 1);
        }

        @Override
        public OptionalInt read(FriendlyByteBuf friendlyByteBuf) {
            int i = friendlyByteBuf.readVarInt();
            return i == 0 ? OptionalInt.empty() : OptionalInt.of(i - 1);
        }

        @Override
        public OptionalInt copy(OptionalInt value) {
            return value;
        }
    };
    public static final EntityDataSerializer<Pose> POSE = new EntityDataSerializer<Pose>() {
        @Override
        public void write(FriendlyByteBuf buf, Pose value) {
            buf.writeEnum(value);
        }

        @Override
        public Pose read(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readEnum(Pose.class);
        }

        @Override
        public Pose copy(Pose value) {
            return value;
        }
    };

    public static void registerSerializer(EntityDataSerializer<?> handler) {
        SERIALIZERS.add(handler);
    }

    @Nullable
    public static EntityDataSerializer<?> getSerializer(int id) {
        return SERIALIZERS.byId(id);
    }

    public static int getSerializedId(EntityDataSerializer<?> handler) {
        return SERIALIZERS.getId(handler);
    }

    private EntityDataSerializers() {
    }

    static {
        registerSerializer(BYTE);
        registerSerializer(INT);
        registerSerializer(FLOAT);
        registerSerializer(STRING);
        registerSerializer(COMPONENT);
        registerSerializer(OPTIONAL_COMPONENT);
        registerSerializer(ITEM_STACK);
        registerSerializer(BOOLEAN);
        registerSerializer(ROTATIONS);
        registerSerializer(BLOCK_POS);
        registerSerializer(OPTIONAL_BLOCK_POS);
        registerSerializer(DIRECTION);
        registerSerializer(OPTIONAL_UUID);
        registerSerializer(BLOCK_STATE);
        registerSerializer(COMPOUND_TAG);
        registerSerializer(PARTICLE);
        registerSerializer(VILLAGER_DATA);
        registerSerializer(OPTIONAL_UNSIGNED_INT);
        registerSerializer(POSE);
    }
}
