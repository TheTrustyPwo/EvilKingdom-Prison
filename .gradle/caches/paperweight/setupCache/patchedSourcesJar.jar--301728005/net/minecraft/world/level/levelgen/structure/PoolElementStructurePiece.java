package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.slf4j.Logger;

public class PoolElementStructurePiece extends StructurePiece {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final StructurePoolElement element;
    protected BlockPos position;
    private final int groundLevelDelta;
    protected final Rotation rotation;
    private final List<JigsawJunction> junctions = Lists.newArrayList();
    private final StructureManager structureManager;

    public PoolElementStructurePiece(StructureManager structureManager, StructurePoolElement poolElement, BlockPos pos, int groundLevelDelta, Rotation rotation, BoundingBox boundingBox) {
        super(StructurePieceType.JIGSAW, 0, boundingBox);
        this.structureManager = structureManager;
        this.element = poolElement;
        this.position = pos;
        this.groundLevelDelta = groundLevelDelta;
        this.rotation = rotation;
    }

    public PoolElementStructurePiece(StructurePieceSerializationContext context, CompoundTag nbt) {
        super(StructurePieceType.JIGSAW, nbt);
        this.structureManager = context.structureManager();
        this.position = new BlockPos(nbt.getInt("PosX"), nbt.getInt("PosY"), nbt.getInt("PosZ"));
        this.groundLevelDelta = nbt.getInt("ground_level_delta");
        DynamicOps<Tag> dynamicOps = RegistryOps.create(NbtOps.INSTANCE, context.registryAccess());
        this.element = StructurePoolElement.CODEC.parse(dynamicOps, nbt.getCompound("pool_element")).resultOrPartial(LOGGER::error).orElseThrow(() -> {
            return new IllegalStateException("Invalid pool element found");
        });
        this.rotation = Rotation.valueOf(nbt.getString("rotation"));
        this.boundingBox = this.element.getBoundingBox(this.structureManager, this.position, this.rotation);
        ListTag listTag = nbt.getList("junctions", 10);
        this.junctions.clear();
        listTag.forEach((junctionTag) -> {
            this.junctions.add(JigsawJunction.deserialize(new Dynamic<>(dynamicOps, junctionTag)));
        });
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        nbt.putInt("PosX", this.position.getX());
        nbt.putInt("PosY", this.position.getY());
        nbt.putInt("PosZ", this.position.getZ());
        nbt.putInt("ground_level_delta", this.groundLevelDelta);
        DynamicOps<Tag> dynamicOps = RegistryOps.create(NbtOps.INSTANCE, context.registryAccess());
        StructurePoolElement.CODEC.encodeStart(dynamicOps, this.element).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.put("pool_element", tag);
        });
        nbt.putString("rotation", this.rotation.name());
        ListTag listTag = new ListTag();

        for(JigsawJunction jigsawJunction : this.junctions) {
            listTag.add(jigsawJunction.serialize(dynamicOps).getValue());
        }

        nbt.put("junctions", listTag);
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        this.place(world, structureAccessor, chunkGenerator, random, chunkBox, pos, false);
    }

    public void place(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox boundingBox, BlockPos pos, boolean keepJigsaws) {
        this.element.place(this.structureManager, world, structureAccessor, chunkGenerator, this.position, pos, this.rotation, boundingBox, random, keepJigsaws);
    }

    @Override
    public void move(int x, int y, int z) {
        super.move(x, y, z);
        this.position = this.position.offset(x, y, z);
    }

    @Override
    public Rotation getRotation() {
        return this.rotation;
    }

    @Override
    public String toString() {
        return String.format("<%s | %s | %s | %s>", this.getClass().getSimpleName(), this.position, this.rotation, this.element);
    }

    public StructurePoolElement getElement() {
        return this.element;
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public int getGroundLevelDelta() {
        return this.groundLevelDelta;
    }

    public void addJunction(JigsawJunction junction) {
        this.junctions.add(junction);
    }

    public List<JigsawJunction> getJunctions() {
        return this.junctions;
    }
}
