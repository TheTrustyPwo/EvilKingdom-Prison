package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.FeatureAccess;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class StructureFeatureManager {
    private final LevelAccessor level;
    private final WorldGenSettings worldGenSettings;
    private final StructureCheck structureCheck;

    public StructureFeatureManager(LevelAccessor world, WorldGenSettings options, StructureCheck locator) {
        this.level = world;
        this.worldGenSettings = options;
        this.structureCheck = locator;
    }

    public StructureFeatureManager forWorldGenRegion(WorldGenRegion region) {
        if (region.getLevel() != this.level) {
            throw new IllegalStateException("Using invalid feature manager (source level: " + region.getLevel() + ", region: " + region);
        } else {
            return new StructureFeatureManager(region, this.worldGenSettings, this.structureCheck);
        }
    }

    public List<StructureStart> startsForFeature(SectionPos sectionPos, Predicate<ConfiguredStructureFeature<?, ?>> predicate) {
        Map<ConfiguredStructureFeature<?, ?>, LongSet> map = this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
        Builder<StructureStart> builder = ImmutableList.builder();

        for(Entry<ConfiguredStructureFeature<?, ?>, LongSet> entry : map.entrySet()) {
            ConfiguredStructureFeature<?, ?> configuredStructureFeature = entry.getKey();
            if (predicate.test(configuredStructureFeature)) {
                this.fillStartsForFeature(configuredStructureFeature, entry.getValue(), builder::add);
            }
        }

        return builder.build();
    }

    public List<StructureStart> startsForFeature(SectionPos sectionPos, ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        LongSet longSet = this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForFeature(configuredStructureFeature);
        Builder<StructureStart> builder = ImmutableList.builder();
        this.fillStartsForFeature(configuredStructureFeature, longSet, builder::add);
        return builder.build();
    }

    public void fillStartsForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature, LongSet longSet, Consumer<StructureStart> consumer) {
        for(long l : longSet) {
            SectionPos sectionPos = SectionPos.of(new ChunkPos(l), this.level.getMinSection());
            StructureStart structureStart = this.getStartForFeature(sectionPos, configuredStructureFeature, this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_STARTS));
            if (structureStart != null && structureStart.isValid()) {
                consumer.accept(structureStart);
            }
        }

    }

    @Nullable
    public StructureStart getStartForFeature(SectionPos pos, ConfiguredStructureFeature<?, ?> configuredStructureFeature, FeatureAccess holder) {
        return holder.getStartForFeature(configuredStructureFeature);
    }

    public void setStartForFeature(SectionPos pos, ConfiguredStructureFeature<?, ?> configuredStructureFeature, StructureStart structureStart, FeatureAccess holder) {
        holder.setStartForFeature(configuredStructureFeature, structureStart);
    }

    public void addReferenceForFeature(SectionPos pos, ConfiguredStructureFeature<?, ?> configuredStructureFeature, long reference, FeatureAccess holder) {
        holder.addReferenceForFeature(configuredStructureFeature, reference);
    }

    public boolean shouldGenerateFeatures() {
        return this.worldGenSettings.generateFeatures();
    }

    public StructureStart getStructureAt(BlockPos pos, ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        for(StructureStart structureStart : this.startsForFeature(SectionPos.of(pos), configuredStructureFeature)) {
            if (structureStart.getBoundingBox().isInside(pos)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureWithPieceAt(BlockPos blockPos, ResourceKey<ConfiguredStructureFeature<?, ?>> resourceKey) {
        ConfiguredStructureFeature<?, ?> configuredStructureFeature = this.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(resourceKey);
        return configuredStructureFeature == null ? StructureStart.INVALID_START : this.getStructureWithPieceAt(blockPos, configuredStructureFeature);
    }

    public StructureStart getStructureWithPieceAt(BlockPos pos, ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        for(StructureStart structureStart : this.startsForFeature(SectionPos.of(pos), configuredStructureFeature)) {
            if (this.structureHasPieceAt(pos, structureStart)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public boolean structureHasPieceAt(BlockPos blockPos, StructureStart structureStart) {
        for(StructurePiece structurePiece : structureStart.getPieces()) {
            if (structurePiece.getBoundingBox().isInside(blockPos)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAnyStructureAt(BlockPos pos) {
        SectionPos sectionPos = SectionPos.of(pos);
        return this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
    }

    public Map<ConfiguredStructureFeature<?, ?>, LongSet> getAllStructuresAt(BlockPos blockPos) {
        SectionPos sectionPos = SectionPos.of(blockPos);
        return this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
    }

    public StructureCheckResult checkStructurePresence(ChunkPos chunkPos, ConfiguredStructureFeature<?, ?> configuredStructureFeature, boolean skipExistingChunk) {
        return this.structureCheck.checkStart(chunkPos, configuredStructureFeature, skipExistingChunk);
    }

    public void addReference(StructureStart structureStart) {
        structureStart.addReference();
        this.structureCheck.incrementReference(structureStart.getChunkPos(), structureStart.getFeature());
    }

    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }
}
