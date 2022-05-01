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
        return (Raid) this.raidMap.get(id);
    }

    public void tick() {
        ++this.tick;
        Iterator iterator = this.raidMap.values().iterator();

        while (iterator.hasNext()) {
            Raid raid = (Raid) iterator.next();

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
        return raider != null && raid != null && raid.getLevel() != null ? raider.isAlive() && raider.canJoinRaid() && raider.getNoActionTime() <= 2400 && raider.level.dimensionType() == raid.getLevel().dimensionType() : false;
    }

    @Nullable
    public Raid createOrExtendRaid(ServerPlayer player) {
        if (player.isSpectator()) {
            return null;
        } else if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
            return null;
        } else {
            DimensionType dimensionmanager = player.level.dimensionType();

            if (!dimensionmanager.hasRaids()) {
                return null;
            } else {
                BlockPos blockposition = player.blockPosition();
                List<PoiRecord> list = (List) this.level.getPoiManager().getInRange(PoiType.ALL, blockposition, 64, PoiManager.Occupancy.IS_OCCUPIED).collect(Collectors.toList());
                int i = 0;
                Vec3 vec3d = Vec3.ZERO;

                for (Iterator iterator = list.iterator(); iterator.hasNext(); ++i) {
                    PoiRecord villageplacerecord = (PoiRecord) iterator.next();
                    BlockPos blockposition1 = villageplacerecord.getPos();

                    vec3d = vec3d.add((double) blockposition1.getX(), (double) blockposition1.getY(), (double) blockposition1.getZ());
                }

                BlockPos blockposition2;

                if (i > 0) {
                    vec3d = vec3d.scale(1.0D / (double) i);
                    blockposition2 = new BlockPos(vec3d);
                } else {
                    blockposition2 = blockposition;
                }

                Raid raid = this.getOrCreateRaid(player.getLevel(), blockposition2);
                boolean flag = false;

                if (!raid.isStarted()) {
                    /* CraftBukkit - moved down
                    if (!this.raidMap.containsKey(raid.getId())) {
                        this.raidMap.put(raid.getId(), raid);
                    }
                    */

                    flag = true;
                    // CraftBukkit start - fixed a bug with raid: players could add up Bad Omen level even when the raid had finished
                } else if (raid.isInProgress() && raid.getBadOmenLevel() < raid.getMaxBadOmenLevel()) {
                    flag = true;
                    // CraftBukkit end
                } else {
                    player.removeEffect(MobEffects.BAD_OMEN);
                    player.connection.send(new ClientboundEntityEventPacket(player, (byte) 43));
                }

                if (flag) {
                    // CraftBukkit start
                    if (!org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callRaidTriggerEvent(raid, player)) {
                        player.removeEffect(MobEffects.BAD_OMEN);
                        return null;
                    }

                    if (!this.raidMap.containsKey(raid.getId())) {
                        this.raidMap.put(raid.getId(), raid);
                    }
                    // CraftBukkit end
                    raid.absorbBadOmen(player);
                    player.connection.send(new ClientboundEntityEventPacket(player, (byte) 43));
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
        Raids persistentraid = new Raids(world);

        persistentraid.nextAvailableID = nbt.getInt("NextAvailableID");
        persistentraid.tick = nbt.getInt("Tick");
        ListTag nbttaglist = nbt.getList("Raids", 10);

        for (int i = 0; i < nbttaglist.size(); ++i) {
            CompoundTag nbttagcompound1 = nbttaglist.getCompound(i);
            Raid raid = new Raid(world, nbttagcompound1);

            persistentraid.raidMap.put(raid.getId(), raid);
        }

        return persistentraid;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("NextAvailableID", this.nextAvailableID);
        nbt.putInt("Tick", this.tick);
        ListTag nbttaglist = new ListTag();
        Iterator iterator = this.raidMap.values().iterator();

        while (iterator.hasNext()) {
            Raid raid = (Raid) iterator.next();
            CompoundTag nbttagcompound1 = new CompoundTag();

            raid.save(nbttagcompound1);
            nbttaglist.add(nbttagcompound1);
        }

        nbt.put("Raids", nbttaglist);
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
        double d0 = (double) searchDistance;
        Iterator iterator = this.raidMap.values().iterator();

        while (iterator.hasNext()) {
            Raid raid1 = (Raid) iterator.next();
            double d1 = raid1.getCenter().distSqr(pos);

            if (raid1.isActive() && d1 < d0) {
                raid = raid1;
                d0 = d1;
            }
        }

        return raid;
    }
}
