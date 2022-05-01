package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import org.slf4j.Logger;

public class DebugPackets {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void sendGameTestAddMarker(ServerLevel world, BlockPos pos, String message, int color, int duration) {
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        friendlyByteBuf.writeBlockPos(pos);
        friendlyByteBuf.writeInt(color);
        friendlyByteBuf.writeUtf(message);
        friendlyByteBuf.writeInt(duration);
        sendPacketToAllPlayers(world, friendlyByteBuf, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER);
    }

    public static void sendGameTestClearPacket(ServerLevel world) {
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        sendPacketToAllPlayers(world, friendlyByteBuf, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_CLEAR);
    }

    public static void sendPoiPacketsForChunk(ServerLevel world, ChunkPos pos) {
    }

    public static void sendPoiAddedPacket(ServerLevel world, BlockPos pos) {
        sendVillageSectionsPacket(world, pos);
    }

    public static void sendPoiRemovedPacket(ServerLevel world, BlockPos pos) {
        sendVillageSectionsPacket(world, pos);
    }

    public static void sendPoiTicketCountPacket(ServerLevel world, BlockPos pos) {
        sendVillageSectionsPacket(world, pos);
    }

    private static void sendVillageSectionsPacket(ServerLevel world, BlockPos pos) {
    }

    public static void sendPathFindingPacket(Level world, Mob mob, @Nullable Path path, float nodeReachProximity) {
    }

    public static void sendNeighborsUpdatePacket(Level world, BlockPos pos) {
    }

    public static void sendStructurePacket(WorldGenLevel world, StructureStart structureStart) {
    }

    public static void sendGoalSelector(Level world, Mob mob, GoalSelector goalSelector) {
        if (world instanceof ServerLevel) {
            ;
        }
    }

    public static void sendRaids(ServerLevel server, Collection<Raid> raids) {
    }

    public static void sendEntityBrain(LivingEntity living) {
    }

    public static void sendBeeInfo(Bee bee) {
    }

    public static void sendGameEventInfo(Level world, GameEvent event, BlockPos pos) {
    }

    public static void sendGameEventListenerInfo(Level world, GameEventListener eventListener) {
    }

    public static void sendHiveInfo(Level world, BlockPos pos, BlockState state, BeehiveBlockEntity blockEntity) {
    }

    private static void writeBrain(LivingEntity entity, FriendlyByteBuf buf) {
        Brain<?> brain = entity.getBrain();
        long l = entity.level.getGameTime();
        if (entity instanceof InventoryCarrier) {
            Container container = ((InventoryCarrier)entity).getInventory();
            buf.writeUtf(container.isEmpty() ? "" : container.toString());
        } else {
            buf.writeUtf("");
        }

        if (brain.hasMemoryValue(MemoryModuleType.PATH)) {
            buf.writeBoolean(true);
            Path path = brain.getMemory(MemoryModuleType.PATH).get();
            path.writeToStream(buf);
        } else {
            buf.writeBoolean(false);
        }

        if (entity instanceof Villager) {
            Villager villager = (Villager)entity;
            boolean bl = villager.wantsToSpawnGolem(l);
            buf.writeBoolean(bl);
        } else {
            buf.writeBoolean(false);
        }

        buf.writeCollection(brain.getActiveActivities(), (bufx, activity) -> {
            bufx.writeUtf(activity.getName());
        });
        Set<String> set = brain.getRunningBehaviors().stream().map(Behavior::toString).collect(Collectors.toSet());
        buf.writeCollection(set, FriendlyByteBuf::writeUtf);
        buf.writeCollection(getMemoryDescriptions(entity, l), (bufx, memory) -> {
            String string = StringUtil.truncateStringIfNecessary(memory, 255, true);
            bufx.writeUtf(string);
        });
        if (entity instanceof Villager) {
            Set<BlockPos> set2 = Stream.of(MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT).map(brain::getMemory).flatMap(Optional::stream).map(GlobalPos::pos).collect(Collectors.toSet());
            buf.writeCollection(set2, FriendlyByteBuf::writeBlockPos);
        } else {
            buf.writeVarInt(0);
        }

        if (entity instanceof Villager) {
            Set<BlockPos> set3 = Stream.of(MemoryModuleType.POTENTIAL_JOB_SITE).map(brain::getMemory).flatMap(Optional::stream).map(GlobalPos::pos).collect(Collectors.toSet());
            buf.writeCollection(set3, FriendlyByteBuf::writeBlockPos);
        } else {
            buf.writeVarInt(0);
        }

        if (entity instanceof Villager) {
            Map<UUID, Object2IntMap<GossipType>> map = ((Villager)entity).getGossips().getGossipEntries();
            List<String> list = Lists.newArrayList();
            map.forEach((uuid, gossips) -> {
                String string = DebugEntityNameGenerator.getEntityName(uuid);
                gossips.forEach((type, value) -> {
                    list.add(string + ": " + type + ": " + value);
                });
            });
            buf.writeCollection(list, FriendlyByteBuf::writeUtf);
        } else {
            buf.writeVarInt(0);
        }

    }

    private static List<String> getMemoryDescriptions(LivingEntity entity, long currentTime) {
        Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> map = entity.getBrain().getMemories();
        List<String> list = Lists.newArrayList();

        for(Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : map.entrySet()) {
            MemoryModuleType<?> memoryModuleType = entry.getKey();
            Optional<? extends ExpirableValue<?>> optional = entry.getValue();
            String string;
            if (optional.isPresent()) {
                ExpirableValue<?> expirableValue = optional.get();
                Object object = expirableValue.getValue();
                if (memoryModuleType == MemoryModuleType.HEARD_BELL_TIME) {
                    long l = currentTime - (Long)object;
                    string = l + " ticks ago";
                } else if (expirableValue.canExpire()) {
                    string = getShortDescription((ServerLevel)entity.level, object) + " (ttl: " + expirableValue.getTimeToLive() + ")";
                } else {
                    string = getShortDescription((ServerLevel)entity.level, object);
                }
            } else {
                string = "-";
            }

            list.add(Registry.MEMORY_MODULE_TYPE.getKey(memoryModuleType).getPath() + ": " + string);
        }

        list.sort(String::compareTo);
        return list;
    }

    private static String getShortDescription(ServerLevel world, @Nullable Object object) {
        if (object == null) {
            return "-";
        } else if (object instanceof UUID) {
            return getShortDescription(world, world.getEntity((UUID)object));
        } else if (object instanceof LivingEntity) {
            Entity entity = (Entity)object;
            return DebugEntityNameGenerator.getEntityName(entity);
        } else if (object instanceof Nameable) {
            return ((Nameable)object).getName().getString();
        } else if (object instanceof WalkTarget) {
            return getShortDescription(world, ((WalkTarget)object).getTarget());
        } else if (object instanceof EntityTracker) {
            return getShortDescription(world, ((EntityTracker)object).getEntity());
        } else if (object instanceof GlobalPos) {
            return getShortDescription(world, ((GlobalPos)object).pos());
        } else if (object instanceof BlockPosTracker) {
            return getShortDescription(world, ((BlockPosTracker)object).currentBlockPosition());
        } else if (object instanceof EntityDamageSource) {
            Entity entity2 = ((EntityDamageSource)object).getEntity();
            return entity2 == null ? object.toString() : getShortDescription(world, entity2);
        } else if (!(object instanceof Collection)) {
            return object.toString();
        } else {
            List<String> list = Lists.newArrayList();

            for(Object object2 : (Iterable)object) {
                list.add(getShortDescription(world, object2));
            }

            return list.toString();
        }
    }

    private static void sendPacketToAllPlayers(ServerLevel world, FriendlyByteBuf buf, ResourceLocation channel) {
        Packet<?> packet = new ClientboundCustomPayloadPacket(channel, buf);

        for(Player player : world.getLevel().players()) {
            ((ServerPlayer)player).connection.send(packet);
        }

    }
}
