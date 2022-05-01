package net.minecraft.world.entity.ai.village.poi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.VisibleForDebug;
import org.slf4j.Logger;

public class PoiSection {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Short2ObjectMap<PoiRecord> records = new Short2ObjectOpenHashMap<>();
    private final Map<PoiType, Set<PoiRecord>> byType = Maps.newHashMap();
    private final Runnable setDirty;
    private boolean isValid;

    public static Codec<PoiSection> codec(Runnable updateListener) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(RecordCodecBuilder.point(updateListener), Codec.BOOL.optionalFieldOf("Valid", Boolean.valueOf(false)).forGetter((poiSet) -> {
                return poiSet.isValid;
            }), PoiRecord.codec(updateListener).listOf().fieldOf("Records").forGetter((poiSet) -> {
                return ImmutableList.copyOf(poiSet.records.values());
            })).apply(instance, PoiSection::new);
        }).orElseGet(Util.prefix("Failed to read POI section: ", LOGGER::error), () -> {
            return new PoiSection(updateListener, false, ImmutableList.of());
        });
    }

    public PoiSection(Runnable updateListener) {
        this(updateListener, true, ImmutableList.of());
    }

    private PoiSection(Runnable updateListener, boolean valid, List<PoiRecord> pois) {
        this.setDirty = updateListener;
        this.isValid = valid;
        pois.forEach(this::add);
    }

    public Stream<PoiRecord> getRecords(Predicate<PoiType> predicate, PoiManager.Occupancy occupationStatus) {
        return this.byType.entrySet().stream().filter((entry) -> {
            return predicate.test(entry.getKey());
        }).flatMap((entry) -> {
            return entry.getValue().stream();
        }).filter(occupationStatus.getTest());
    }

    public void add(BlockPos pos, PoiType type) {
        if (this.add(new PoiRecord(pos, type, this.setDirty))) {
            LOGGER.debug("Added POI of type {} @ {}", type, pos);
            this.setDirty.run();
        }

    }

    private boolean add(PoiRecord poi) {
        BlockPos blockPos = poi.getPos();
        PoiType poiType = poi.getPoiType();
        short s = SectionPos.sectionRelativePos(blockPos);
        PoiRecord poiRecord = this.records.get(s);
        if (poiRecord != null) {
            if (poiType.equals(poiRecord.getPoiType())) {
                return false;
            }

            Util.logAndPauseIfInIde("POI data mismatch: already registered at " + blockPos);
        }

        this.records.put(s, poi);
        this.byType.computeIfAbsent(poiType, (poiTypex) -> {
            return Sets.newHashSet();
        }).add(poi);
        return true;
    }

    public void remove(BlockPos pos) {
        PoiRecord poiRecord = this.records.remove(SectionPos.sectionRelativePos(pos));
        if (poiRecord == null) {
            LOGGER.error("POI data mismatch: never registered at {}", (Object)pos);
        } else {
            this.byType.get(poiRecord.getPoiType()).remove(poiRecord);
            LOGGER.debug("Removed POI of type {} @ {}", LogUtils.defer(poiRecord::getPoiType), LogUtils.defer(poiRecord::getPos));
            this.setDirty.run();
        }
    }

    /** @deprecated */
    @Deprecated
    @VisibleForDebug
    public int getFreeTickets(BlockPos pos) {
        return this.getPoiRecord(pos).map(PoiRecord::getFreeTickets).orElse(0);
    }

    public boolean release(BlockPos pos) {
        PoiRecord poiRecord = this.records.get(SectionPos.sectionRelativePos(pos));
        if (poiRecord == null) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("POI never registered at " + pos));
        } else {
            boolean bl = poiRecord.releaseTicket();
            this.setDirty.run();
            return bl;
        }
    }

    public boolean exists(BlockPos pos, Predicate<PoiType> predicate) {
        return this.getType(pos).filter(predicate).isPresent();
    }

    public Optional<PoiType> getType(BlockPos pos) {
        return this.getPoiRecord(pos).map(PoiRecord::getPoiType);
    }

    private Optional<PoiRecord> getPoiRecord(BlockPos pos) {
        return Optional.ofNullable(this.records.get(SectionPos.sectionRelativePos(pos)));
    }

    public void refresh(Consumer<BiConsumer<BlockPos, PoiType>> consumer) {
        if (!this.isValid) {
            Short2ObjectMap<PoiRecord> short2ObjectMap = new Short2ObjectOpenHashMap<>(this.records);
            this.clear();
            consumer.accept((pos, poiType) -> {
                short s = SectionPos.sectionRelativePos(pos);
                PoiRecord poiRecord = short2ObjectMap.computeIfAbsent(s, (sx) -> {
                    return new PoiRecord(pos, poiType, this.setDirty);
                });
                this.add(poiRecord);
            });
            this.isValid = true;
            this.setDirty.run();
        }

    }

    private void clear() {
        this.records.clear();
        this.byType.clear();
    }

    boolean isValid() {
        return this.isValid;
    }
}
