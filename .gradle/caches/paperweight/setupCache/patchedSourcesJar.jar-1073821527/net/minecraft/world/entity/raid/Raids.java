package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class Raids extends SavedData {
    private static final String RAID_FILE_ID = "raids";
    public final Map<Integer, Raid> raidMap = Maps.newHashMap();
    private final ServerLevel level;
    private int nextAvailableID;
    private int tick;

    public Raids(ServerLevel world) {
        this.level = world;
        this.nextAvailableID = 1;
        this.setDirty();
    }

    public Raid get(int id) {
        return this.raidMap.get(id);
    }

    public void tick() {
        ++this.tick;
        Iterator<Raid> iterator = this.raidMap.values().iterator();

        while(iterator.hasNext()) {
            Raid raid = iterator.next();
            if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
                raid.stop();
            }

            if (raid.isStopped()) {
                iterator.remove();
                this.setDirty();
            } else {
                raid.tick();
            }
        }

        if (this.tick % 200 == 0) {
            this.setDirty();
        }

        DebugPackets.sendRaids(this.level, this.raidMap.values());
    }

    public static boolean canJoinRaid(Raider raider, Raid raid) {
        if (raider != null && raid != null && raid.getLevel() != null) {
            return raider.isAlive() && raider.canJoinRaid() && raider.getNoActionTime() <= 2400 && raider.level.dimensionType() == raid.getLevel().dimensionType();
        } else {
            return false;
        }
    }

    @Nullable
    public Raid createOrExtendRaid(ServerPlayer player) {
        if (player.isSpectator()) {
            return null;
        } else if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
            return null;
        } else {
            DimensionType dimensionType = player.level.dimensionType();
            if (!dimensionType.hasRaids()) {
                return null;
            } else {
                BlockPos blockPos = player.blockPosition();
                List<PoiRecord> list = this.level.getPoiManager().getInRange(PoiType.ALL, blockPos, 64, PoiManager.Occupancy.IS_OCCUPIED).collect(Collectors.toList());
                int i = 0;
                Vec3 vec3 = Vec3.ZERO;

                for(PoiRecord poiRecord : list) {
                    BlockPos blockPos2 = poiRecord.getPos();
                    vec3 = vec3.add((double)blockPos2.getX(), (double)blockPos2.getY(), (double)blockPos2.getZ());
                    ++i;
                }

                BlockPos blockPos3;
                if (i > 0) {
                    vec3 = vec3.scale(1.0D / (double)i);
                    blockPos3 = new BlockPos(vec3);
                } else {
                    blockPos3 = blockPos;
                }

                Raid raid = this.getOrCreateRaid(player.getLevel(), blockPos3);
                boolean bl = false;
                if (!raid.isStarted()) {
                    if (!this.raidMap.containsKey(raid.getId())) {
                        this.raidMap.put(raid.getId(), raid);
                    }

                    bl = true;
                } else if (raid.getBadOmenLevel() < raid.getMaxBadOmenLevel()) {
                    bl = true;
                } else {
                    player.removeEffect(MobEffects.BAD_OMEN);
                    player.connection.send(new ClientboundEntityEventPacket(player, (byte)43));
                }

                if (bl) {
                    raid.absorbBadOmen(player);
                    player.connection.send(new ClientboundEntityEventPacket(player, (byte)43));
                    if (!raid.hasFirstWaveSpawned()) {
                        player.awardStat(Stats.RAID_TRIGGER);
                        CriteriaTriggers.BAD_OMEN.trigger(player);
                    }
                }

                this.setDirty();
                return raid;
            }
        }
    }

    private Raid getOrCreateRaid(ServerLevel world, BlockPos pos) {
        Raid raid = world.getRaidAt(pos);
        return raid != null ? raid : new Raid(this.getUniqueId(), world, pos);
    }

    public static Raids load(ServerLevel world, CompoundTag nbt) {
        Raids raids = new Raids(world);
        raids.nextAvailableID = nbt.getInt("NextAvailableID");
        raids.tick = nbt.getInt("Tick");
        ListTag listTag = nbt.getList("Raids", 10);

        for(int i = 0; i < listTag.size(); ++i) {
            CompoundTag compoundTag = listTag.getCompound(i);
            Raid raid = new Raid(world, compoundTag);
            raids.raidMap.put(raid.getId(), raid);
        }

        return raids;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("NextAvailableID", this.nextAvailableID);
        nbt.putInt("Tick", this.tick);
        ListTag listTag = new ListTag();

        for(Raid raid : this.raidMap.values()) {
            CompoundTag compoundTag = new CompoundTag();
            raid.save(compoundTag);
            listTag.add(compoundTag);
        }

        nbt.put("Raids", listTag);
        return nbt;
    }

    public static String getFileId(Holder<DimensionType> dimensionTypeEntry) {
        return dimensionTypeEntry.is(DimensionType.END_LOCATION) ? "raids_end" : "raids";
    }

    private int getUniqueId() {
        return ++this.nextAvailableID;
    }

    @Nullable
    public Raid getNearbyRaid(BlockPos pos, int searchDistance) {
        Raid raid = null;
        double d = (double)searchDistance;

        for(Raid raid2 : this.raidMap.values()) {
            double e = raid2.getCenter().distSqr(pos);
            if (raid2.isActive() && e < d) {
                raid = raid2;
                d = e;
            }
        }

        return raid;
    }
}
