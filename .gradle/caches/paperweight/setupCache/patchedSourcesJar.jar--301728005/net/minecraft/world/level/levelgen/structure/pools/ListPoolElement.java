package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class ListPoolElement extends StructurePoolElement {
    public static final Codec<ListPoolElement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(StructurePoolElement.CODEC.listOf().fieldOf("elements").forGetter((listPoolElement) -> {
            return listPoolElement.elements;
        }), projectionCodec()).apply(instance, ListPoolElement::new);
    });
    private final List<StructurePoolElement> elements;

    public ListPoolElement(List<StructurePoolElement> elements, StructureTemplatePool.Projection projection) {
        super(projection);
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("Elements are empty");
        } else {
            this.elements = elements;
            this.setProjectionOnEachElement(projection);
        }
    }

    @Override
    public Vec3i getSize(StructureManager structureManager, Rotation rotation) {
        int i = 0;
        int j = 0;
        int k = 0;

        for(StructurePoolElement structurePoolElement : this.elements) {
            Vec3i vec3i = structurePoolElement.getSize(structureManager, rotation);
            i = Math.max(i, vec3i.getX());
            j = Math.max(j, vec3i.getY());
            k = Math.max(k, vec3i.getZ());
        }

        return new Vec3i(i, j, k);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureManager structureManager, BlockPos pos, Rotation rotation, Random random) {
        return this.elements.get(0).getShuffledJigsawBlocks(structureManager, pos, rotation, random);
    }

    @Override
    public BoundingBox getBoundingBox(StructureManager structureManager, BlockPos pos, Rotation rotation) {
        Stream<BoundingBox> stream = this.elements.stream().filter((element) -> {
            return element != EmptyPoolElement.INSTANCE;
        }).map((element) -> {
            return element.getBoundingBox(structureManager, pos, rotation);
        });
        return BoundingBox.encapsulatingBoxes(stream::iterator).orElseThrow(() -> {
            return new IllegalStateException("Unable to calculate boundingbox for ListPoolElement");
        });
    }

    @Override
    public boolean place(StructureManager structureManager, WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, BlockPos pos, BlockPos blockPos, Rotation rotation, BoundingBox box, Random random, boolean keepJigsaws) {
        for(StructurePoolElement structurePoolElement : this.elements) {
            if (!structurePoolElement.place(structureManager, world, structureAccessor, chunkGenerator, pos, blockPos, rotation, box, random, keepJigsaws)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return StructurePoolElementType.LIST;
    }

    @Override
    public StructurePoolElement setProjection(StructureTemplatePool.Projection projection) {
        super.setProjection(projection);
        this.setProjectionOnEachElement(projection);
        return this;
    }

    @Override
    public String toString() {
        return "List[" + (String)this.elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }

    private void setProjectionOnEachElement(StructureTemplatePool.Projection projection) {
        this.elements.forEach((element) -> {
            element.setProjection(projection);
        });
    }
}
