package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SinglePoolElement extends StructurePoolElement {
    private static final Codec<Either<ResourceLocation, StructureTemplate>> TEMPLATE_CODEC = Codec.of(SinglePoolElement::encodeTemplate, ResourceLocation.CODEC.map(Either::left));
    public static final Codec<SinglePoolElement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(templateCodec(), processorsCodec(), projectionCodec()).apply(instance, SinglePoolElement::new);
    });
    protected final Either<ResourceLocation, StructureTemplate> template;
    protected final Holder<StructureProcessorList> processors;

    private static <T> DataResult<T> encodeTemplate(Either<ResourceLocation, StructureTemplate> either, DynamicOps<T> dynamicOps, T object) {
        Optional<ResourceLocation> optional = either.left();
        return !optional.isPresent() ? DataResult.error("Can not serialize a runtime pool element") : ResourceLocation.CODEC.encode(optional.get(), dynamicOps, object);
    }

    protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> processorsCodec() {
        return StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter((singlePoolElement) -> {
            return singlePoolElement.processors;
        });
    }

    protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Either<ResourceLocation, StructureTemplate>> templateCodec() {
        return TEMPLATE_CODEC.fieldOf("location").forGetter((singlePoolElement) -> {
            return singlePoolElement.template;
        });
    }

    protected SinglePoolElement(Either<ResourceLocation, StructureTemplate> location, Holder<StructureProcessorList> holder, StructureTemplatePool.Projection projection) {
        super(projection);
        this.template = location;
        this.processors = holder;
    }

    public SinglePoolElement(StructureTemplate structure) {
        this(Either.right(structure), ProcessorLists.EMPTY, StructureTemplatePool.Projection.RIGID);
    }

    @Override
    public Vec3i getSize(StructureManager structureManager, Rotation rotation) {
        StructureTemplate structureTemplate = this.getTemplate(structureManager);
        return structureTemplate.getSize(rotation);
    }

    private StructureTemplate getTemplate(StructureManager structureManager) {
        return this.template.map(structureManager::getOrCreate, Function.identity());
    }

    public List<StructureTemplate.StructureBlockInfo> getDataMarkers(StructureManager structureManager, BlockPos pos, Rotation rotation, boolean mirroredAndRotated) {
        StructureTemplate structureTemplate = this.getTemplate(structureManager);
        List<StructureTemplate.StructureBlockInfo> list = structureTemplate.filterBlocks(pos, (new StructurePlaceSettings()).setRotation(rotation), Blocks.STRUCTURE_BLOCK, mirroredAndRotated);
        List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();

        for(StructureTemplate.StructureBlockInfo structureBlockInfo : list) {
            if (structureBlockInfo.nbt != null) {
                StructureMode structureMode = StructureMode.valueOf(structureBlockInfo.nbt.getString("mode"));
                if (structureMode == StructureMode.DATA) {
                    list2.add(structureBlockInfo);
                }
            }
        }

        return list2;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureManager structureManager, BlockPos pos, Rotation rotation, Random random) {
        StructureTemplate structureTemplate = this.getTemplate(structureManager);
        List<StructureTemplate.StructureBlockInfo> list = structureTemplate.filterBlocks(pos, (new StructurePlaceSettings()).setRotation(rotation), Blocks.JIGSAW, true);
        Collections.shuffle(list, random);
        return list;
    }

    @Override
    public BoundingBox getBoundingBox(StructureManager structureManager, BlockPos pos, Rotation rotation) {
        StructureTemplate structureTemplate = this.getTemplate(structureManager);
        return structureTemplate.getBoundingBox((new StructurePlaceSettings()).setRotation(rotation), pos);
    }

    @Override
    public boolean place(StructureManager structureManager, WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, BlockPos pos, BlockPos blockPos, Rotation rotation, BoundingBox box, Random random, boolean keepJigsaws) {
        StructureTemplate structureTemplate = this.getTemplate(structureManager);
        StructurePlaceSettings structurePlaceSettings = this.getSettings(rotation, box, keepJigsaws);
        if (!structureTemplate.placeInWorld(world, pos, blockPos, structurePlaceSettings, random, 18)) {
            return false;
        } else {
            for(StructureTemplate.StructureBlockInfo structureBlockInfo : StructureTemplate.processBlockInfos(world, pos, blockPos, structurePlaceSettings, this.getDataMarkers(structureManager, pos, rotation, false))) {
                this.handleDataMarker(world, structureBlockInfo, pos, rotation, random, box);
            }

            return true;
        }
    }

    protected StructurePlaceSettings getSettings(Rotation rotation, BoundingBox box, boolean keepJigsaws) {
        StructurePlaceSettings structurePlaceSettings = new StructurePlaceSettings();
        structurePlaceSettings.setBoundingBox(box);
        structurePlaceSettings.setRotation(rotation);
        structurePlaceSettings.setKnownShape(true);
        structurePlaceSettings.setIgnoreEntities(false);
        structurePlaceSettings.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        structurePlaceSettings.setFinalizeEntities(true);
        if (!keepJigsaws) {
            structurePlaceSettings.addProcessor(JigsawReplacementProcessor.INSTANCE);
        }

        this.processors.value().list().forEach(structurePlaceSettings::addProcessor);
        this.getProjection().getProcessors().forEach(structurePlaceSettings::addProcessor);
        return structurePlaceSettings;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return StructurePoolElementType.SINGLE;
    }

    @Override
    public String toString() {
        return "Single[" + this.template + "]";
    }
}
