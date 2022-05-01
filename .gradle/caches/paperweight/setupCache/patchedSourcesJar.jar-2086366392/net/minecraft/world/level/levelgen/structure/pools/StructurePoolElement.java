package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public abstract class StructurePoolElement {
    public static final Codec<StructurePoolElement> CODEC = Registry.STRUCTURE_POOL_ELEMENT.byNameCodec().dispatch("element_type", StructurePoolElement::getType, StructurePoolElementType::codec);
    @Nullable
    private volatile StructureTemplatePool.Projection projection;

    protected static <E extends StructurePoolElement> RecordCodecBuilder<E, StructureTemplatePool.Projection> projectionCodec() {
        return StructureTemplatePool.Projection.CODEC.fieldOf("projection").forGetter(StructurePoolElement::getProjection);
    }

    protected StructurePoolElement(StructureTemplatePool.Projection projection) {
        this.projection = projection;
    }

    public abstract Vec3i getSize(StructureManager structureManager, Rotation rotation);

    public abstract List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureManager structureManager, BlockPos pos, Rotation rotation, Random random);

    public abstract BoundingBox getBoundingBox(StructureManager structureManager, BlockPos pos, Rotation rotation);

    public abstract boolean place(StructureManager structureManager, WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, BlockPos pos, BlockPos blockPos, Rotation rotation, BoundingBox box, Random random, boolean keepJigsaws);

    public abstract StructurePoolElementType<?> getType();

    public void handleDataMarker(LevelAccessor world, StructureTemplate.StructureBlockInfo structureBlockInfo, BlockPos pos, Rotation rotation, Random random, BoundingBox box) {
    }

    public StructurePoolElement setProjection(StructureTemplatePool.Projection projection) {
        this.projection = projection;
        return this;
    }

    public StructureTemplatePool.Projection getProjection() {
        StructureTemplatePool.Projection projection = this.projection;
        if (projection == null) {
            throw new IllegalStateException();
        } else {
            return projection;
        }
    }

    public int getGroundLevelDelta() {
        return 1;
    }

    public static Function<StructureTemplatePool.Projection, EmptyPoolElement> empty() {
        return (projection) -> {
            return EmptyPoolElement.INSTANCE;
        };
    }

    public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String id) {
        return (projection) -> {
            return new LegacySinglePoolElement(Either.left(new ResourceLocation(id)), ProcessorLists.EMPTY, projection);
        };
    }

    public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String id, Holder<StructureProcessorList> holder) {
        return (projection) -> {
            return new LegacySinglePoolElement(Either.left(new ResourceLocation(id)), holder, projection);
        };
    }

    public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String id) {
        return (projection) -> {
            return new SinglePoolElement(Either.left(new ResourceLocation(id)), ProcessorLists.EMPTY, projection);
        };
    }

    public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String id, Holder<StructureProcessorList> holder) {
        return (projection) -> {
            return new SinglePoolElement(Either.left(new ResourceLocation(id)), holder, projection);
        };
    }

    public static Function<StructureTemplatePool.Projection, FeaturePoolElement> feature(Holder<PlacedFeature> holder) {
        return (projection) -> {
            return new FeaturePoolElement(holder, projection);
        };
    }

    public static Function<StructureTemplatePool.Projection, ListPoolElement> list(List<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>> list) {
        return (projection) -> {
            return new ListPoolElement(list.stream().map((function) -> {
                return function.apply(projection);
            }).collect(Collectors.toList()), projection);
        };
    }
}
