package net.minecraft.world.level.levelgen.structure;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class StructureStart {
    public static final String INVALID_START_ID = "INVALID";
    public static final StructureStart INVALID_START = new StructureStart((ConfiguredStructureFeature<?, ?>)null, new ChunkPos(0, 0), 0, new PiecesContainer(List.of()));
    private final ConfiguredStructureFeature<?, ?> feature;
    private final PiecesContainer pieceContainer;
    private final ChunkPos chunkPos;
    private int references;
    @Nullable
    private volatile BoundingBox cachedBoundingBox;

    public StructureStart(ConfiguredStructureFeature<?, ?> configuredStructureFeature, ChunkPos pos, int references, PiecesContainer children) {
        this.feature = configuredStructureFeature;
        this.chunkPos = pos;
        this.references = references;
        this.pieceContainer = children;
    }

    public BoundingBox getBoundingBox() {
        BoundingBox boundingBox = this.cachedBoundingBox;
        if (boundingBox == null) {
            boundingBox = this.feature.adjustBoundingBox(this.pieceContainer.calculateBoundingBox());
            this.cachedBoundingBox = boundingBox;
        }

        return boundingBox;
    }

    public void placeInChunk(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos) {
        List<StructurePiece> list = this.pieceContainer.pieces();
        if (!list.isEmpty()) {
            BoundingBox boundingBox = (list.get(0)).boundingBox;
            BlockPos blockPos = boundingBox.getCenter();
            BlockPos blockPos2 = new BlockPos(blockPos.getX(), boundingBox.minY(), blockPos.getZ());

            for(StructurePiece structurePiece : list) {
                if (structurePiece.getBoundingBox().intersects(chunkBox)) {
                    structurePiece.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, blockPos2);
                }
            }

            this.feature.feature.getPostPlacementProcessor().afterPlace(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, this.pieceContainer);
        }
    }

    public CompoundTag createTag(StructurePieceSerializationContext context, ChunkPos chunkPos) {
        CompoundTag compoundTag = new CompoundTag();
        if (this.isValid()) {
            compoundTag.putString("id", context.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).getKey(this.feature).toString());
            compoundTag.putInt("ChunkX", chunkPos.x);
            compoundTag.putInt("ChunkZ", chunkPos.z);
            compoundTag.putInt("references", this.references);
            compoundTag.put("Children", this.pieceContainer.save(context));
            return compoundTag;
        } else {
            compoundTag.putString("id", "INVALID");
            return compoundTag;
        }
    }

    public boolean isValid() {
        return !this.pieceContainer.isEmpty();
    }

    public ChunkPos getChunkPos() {
        return this.chunkPos;
    }

    public boolean canBeReferenced() {
        return this.references < this.getMaxReferences();
    }

    public void addReference() {
        ++this.references;
    }

    public int getReferences() {
        return this.references;
    }

    protected int getMaxReferences() {
        return 1;
    }

    public ConfiguredStructureFeature<?, ?> getFeature() {
        return this.feature;
    }

    public List<StructurePiece> getPieces() {
        return this.pieceContainer.pieces();
    }
}
