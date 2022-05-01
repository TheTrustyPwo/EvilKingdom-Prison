package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class JigsawBlockEntity extends BlockEntity {
    public static final String TARGET = "target";
    public static final String POOL = "pool";
    public static final String JOINT = "joint";
    public static final String NAME = "name";
    public static final String FINAL_STATE = "final_state";
    private ResourceLocation name = new ResourceLocation("empty");
    private ResourceLocation target = new ResourceLocation("empty");
    private ResourceLocation pool = new ResourceLocation("empty");
    private JigsawBlockEntity.JointType joint = JigsawBlockEntity.JointType.ROLLABLE;
    private String finalState = "minecraft:air";

    public JigsawBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.JIGSAW, pos, state);
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public ResourceLocation getTarget() {
        return this.target;
    }

    public ResourceLocation getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public JigsawBlockEntity.JointType getJoint() {
        return this.joint;
    }

    public void setName(ResourceLocation name) {
        this.name = name;
    }

    public void setTarget(ResourceLocation target) {
        this.target = target;
    }

    public void setPool(ResourceLocation pool) {
        this.pool = pool;
    }

    public void setFinalState(String finalState) {
        this.finalState = finalState;
    }

    public void setJoint(JigsawBlockEntity.JointType joint) {
        this.joint = joint;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putString("name", this.name.toString());
        nbt.putString("target", this.target.toString());
        nbt.putString("pool", this.pool.toString());
        nbt.putString("final_state", this.finalState);
        nbt.putString("joint", this.joint.getSerializedName());
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.name = new ResourceLocation(nbt.getString("name"));
        this.target = new ResourceLocation(nbt.getString("target"));
        this.pool = new ResourceLocation(nbt.getString("pool"));
        this.finalState = nbt.getString("final_state");
        this.joint = JigsawBlockEntity.JointType.byName(nbt.getString("joint")).orElseGet(() -> {
            return JigsawBlock.getFrontFacing(this.getBlockState()).getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE;
        });
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public void generate(ServerLevel world, int maxDepth, boolean keepJigsaws) {
        ChunkGenerator chunkGenerator = world.getChunkSource().getGenerator();
        StructureManager structureManager = world.getStructureManager();
        StructureFeatureManager structureFeatureManager = world.structureFeatureManager();
        Random random = world.getRandom();
        BlockPos blockPos = this.getBlockPos();
        List<PoolElementStructurePiece> list = Lists.newArrayList();
        StructureTemplate structureTemplate = new StructureTemplate();
        structureTemplate.fillFromWorld(world, blockPos, new Vec3i(1, 1, 1), false, (Block)null);
        StructurePoolElement structurePoolElement = new SinglePoolElement(structureTemplate);
        PoolElementStructurePiece poolElementStructurePiece = new PoolElementStructurePiece(structureManager, structurePoolElement, blockPos, 1, Rotation.NONE, new BoundingBox(blockPos));
        JigsawPlacement.addPieces(world.registryAccess(), poolElementStructurePiece, maxDepth, PoolElementStructurePiece::new, chunkGenerator, structureManager, list, random, world);

        for(PoolElementStructurePiece poolElementStructurePiece2 : list) {
            poolElementStructurePiece2.place(world, structureFeatureManager, chunkGenerator, random, BoundingBox.infinite(), blockPos, keepJigsaws);
        }

    }

    public static enum JointType implements StringRepresentable {
        ROLLABLE("rollable"),
        ALIGNED("aligned");

        private final String name;

        private JointType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static Optional<JigsawBlockEntity.JointType> byName(String name) {
            return Arrays.stream(values()).filter((joint) -> {
                return joint.getSerializedName().equals(name);
            }).findFirst();
        }

        public Component getTranslatedName() {
            return new TranslatableComponent("jigsaw_block.joint." + this.name);
        }
    }
}
