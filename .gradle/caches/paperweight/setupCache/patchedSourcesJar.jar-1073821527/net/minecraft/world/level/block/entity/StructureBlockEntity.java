package net.minecraft.world.level.block.entity;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class StructureBlockEntity extends BlockEntity {
    private static final int SCAN_CORNER_BLOCKS_RANGE = 5;
    public static final int MAX_OFFSET_PER_AXIS = 48;
    public static final int MAX_SIZE_PER_AXIS = 48;
    public static final String AUTHOR_TAG = "author";
    private ResourceLocation structureName;
    public String author = "";
    public String metaData = "";
    public BlockPos structurePos = new BlockPos(0, 1, 0);
    public Vec3i structureSize = Vec3i.ZERO;
    public Mirror mirror = Mirror.NONE;
    public Rotation rotation = Rotation.NONE;
    public StructureMode mode;
    public boolean ignoreEntities = true;
    private boolean powered;
    public boolean showAir;
    public boolean showBoundingBox = true;
    public float integrity = 1.0F;
    public long seed;

    public StructureBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.STRUCTURE_BLOCK, pos, state);
        this.mode = state.getValue(StructureBlock.MODE);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putString("name", this.getStructureName());
        nbt.putString("author", this.author);
        nbt.putString("metadata", this.metaData);
        nbt.putInt("posX", this.structurePos.getX());
        nbt.putInt("posY", this.structurePos.getY());
        nbt.putInt("posZ", this.structurePos.getZ());
        nbt.putInt("sizeX", this.structureSize.getX());
        nbt.putInt("sizeY", this.structureSize.getY());
        nbt.putInt("sizeZ", this.structureSize.getZ());
        nbt.putString("rotation", this.rotation.toString());
        nbt.putString("mirror", this.mirror.toString());
        nbt.putString("mode", this.mode.toString());
        nbt.putBoolean("ignoreEntities", this.ignoreEntities);
        nbt.putBoolean("powered", this.powered);
        nbt.putBoolean("showair", this.showAir);
        nbt.putBoolean("showboundingbox", this.showBoundingBox);
        nbt.putFloat("integrity", this.integrity);
        nbt.putLong("seed", this.seed);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.setStructureName(nbt.getString("name"));
        this.author = nbt.getString("author");
        this.metaData = nbt.getString("metadata");
        int i = Mth.clamp(nbt.getInt("posX"), -48, 48);
        int j = Mth.clamp(nbt.getInt("posY"), -48, 48);
        int k = Mth.clamp(nbt.getInt("posZ"), -48, 48);
        this.structurePos = new BlockPos(i, j, k);
        int l = Mth.clamp(nbt.getInt("sizeX"), 0, 48);
        int m = Mth.clamp(nbt.getInt("sizeY"), 0, 48);
        int n = Mth.clamp(nbt.getInt("sizeZ"), 0, 48);
        this.structureSize = new Vec3i(l, m, n);

        try {
            this.rotation = Rotation.valueOf(nbt.getString("rotation"));
        } catch (IllegalArgumentException var11) {
            this.rotation = Rotation.NONE;
        }

        try {
            this.mirror = Mirror.valueOf(nbt.getString("mirror"));
        } catch (IllegalArgumentException var10) {
            this.mirror = Mirror.NONE;
        }

        try {
            this.mode = StructureMode.valueOf(nbt.getString("mode"));
        } catch (IllegalArgumentException var9) {
            this.mode = StructureMode.DATA;
        }

        this.ignoreEntities = nbt.getBoolean("ignoreEntities");
        this.powered = nbt.getBoolean("powered");
        this.showAir = nbt.getBoolean("showair");
        this.showBoundingBox = nbt.getBoolean("showboundingbox");
        if (nbt.contains("integrity")) {
            this.integrity = nbt.getFloat("integrity");
        } else {
            this.integrity = 1.0F;
        }

        this.seed = nbt.getLong("seed");
        this.updateBlockState();
    }

    private void updateBlockState() {
        if (this.level != null) {
            BlockPos blockPos = this.getBlockPos();
            BlockState blockState = this.level.getBlockState(blockPos);
            if (blockState.is(Blocks.STRUCTURE_BLOCK)) {
                this.level.setBlock(blockPos, blockState.setValue(StructureBlock.MODE, this.mode), 2);
            }

        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public boolean usedBy(Player player) {
        if (!player.canUseGameMasterBlocks()) {
            return false;
        } else {
            if (player.getCommandSenderWorld().isClientSide) {
                player.openStructureBlock(this);
            }

            return true;
        }
    }

    public String getStructureName() {
        return this.structureName == null ? "" : this.structureName.toString();
    }

    public String getStructurePath() {
        return this.structureName == null ? "" : this.structureName.getPath();
    }

    public boolean hasStructureName() {
        return this.structureName != null;
    }

    public void setStructureName(@Nullable String name) {
        this.setStructureName(StringUtil.isNullOrEmpty(name) ? null : ResourceLocation.tryParse(name));
    }

    public void setStructureName(@Nullable ResourceLocation structureName) {
        this.structureName = structureName;
    }

    public void createdBy(LivingEntity entity) {
        this.author = entity.getName().getString();
    }

    public BlockPos getStructurePos() {
        return this.structurePos;
    }

    public void setStructurePos(BlockPos offset) {
        this.structurePos = offset;
    }

    public Vec3i getStructureSize() {
        return this.structureSize;
    }

    public void setStructureSize(Vec3i size) {
        this.structureSize = size;
    }

    public Mirror getMirror() {
        return this.mirror;
    }

    public void setMirror(Mirror mirror) {
        this.mirror = mirror;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public String getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String metadata) {
        this.metaData = metadata;
    }

    public StructureMode getMode() {
        return this.mode;
    }

    public void setMode(StructureMode mode) {
        this.mode = mode;
        BlockState blockState = this.level.getBlockState(this.getBlockPos());
        if (blockState.is(Blocks.STRUCTURE_BLOCK)) {
            this.level.setBlock(this.getBlockPos(), blockState.setValue(StructureBlock.MODE, mode), 2);
        }

    }

    public boolean isIgnoreEntities() {
        return this.ignoreEntities;
    }

    public void setIgnoreEntities(boolean ignoreEntities) {
        this.ignoreEntities = ignoreEntities;
    }

    public float getIntegrity() {
        return this.integrity;
    }

    public void setIntegrity(float integrity) {
        this.integrity = integrity;
    }

    public long getSeed() {
        return this.seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public boolean detectSize() {
        if (this.mode != StructureMode.SAVE) {
            return false;
        } else {
            BlockPos blockPos = this.getBlockPos();
            int i = 80;
            BlockPos blockPos2 = new BlockPos(blockPos.getX() - 80, this.level.getMinBuildHeight(), blockPos.getZ() - 80);
            BlockPos blockPos3 = new BlockPos(blockPos.getX() + 80, this.level.getMaxBuildHeight() - 1, blockPos.getZ() + 80);
            Stream<BlockPos> stream = this.getRelatedCorners(blockPos2, blockPos3);
            return calculateEnclosingBoundingBox(blockPos, stream).filter((box) -> {
                int i = box.maxX() - box.minX();
                int j = box.maxY() - box.minY();
                int k = box.maxZ() - box.minZ();
                if (i > 1 && j > 1 && k > 1) {
                    this.structurePos = new BlockPos(box.minX() - blockPos.getX() + 1, box.minY() - blockPos.getY() + 1, box.minZ() - blockPos.getZ() + 1);
                    this.structureSize = new Vec3i(i - 1, j - 1, k - 1);
                    this.setChanged();
                    BlockState blockState = this.level.getBlockState(blockPos);
                    this.level.sendBlockUpdated(blockPos, blockState, blockState, 3);
                    return true;
                } else {
                    return false;
                }
            }).isPresent();
        }
    }

    private Stream<BlockPos> getRelatedCorners(BlockPos start, BlockPos end) {
        return BlockPos.betweenClosedStream(start, end).filter((pos) -> {
            return this.level.getBlockState(pos).is(Blocks.STRUCTURE_BLOCK);
        }).map(this.level::getBlockEntity).filter((blockEntity) -> {
            return blockEntity instanceof StructureBlockEntity;
        }).map((blockEntity) -> {
            return (StructureBlockEntity)blockEntity;
        }).filter((blockEntity) -> {
            return blockEntity.mode == StructureMode.CORNER && Objects.equals(this.structureName, blockEntity.structureName);
        }).map(BlockEntity::getBlockPos);
    }

    private static Optional<BoundingBox> calculateEnclosingBoundingBox(BlockPos pos, Stream<BlockPos> corners) {
        Iterator<BlockPos> iterator = corners.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            BlockPos blockPos = iterator.next();
            BoundingBox boundingBox = new BoundingBox(blockPos);
            if (iterator.hasNext()) {
                iterator.forEachRemaining(boundingBox::encapsulate);
            } else {
                boundingBox.encapsulate(pos);
            }

            return Optional.of(boundingBox);
        }
    }

    public boolean saveStructure() {
        return this.saveStructure(true);
    }

    public boolean saveStructure(boolean bl) {
        if (this.mode == StructureMode.SAVE && !this.level.isClientSide && this.structureName != null) {
            BlockPos blockPos = this.getBlockPos().offset(this.structurePos);
            ServerLevel serverLevel = (ServerLevel)this.level;
            StructureManager structureManager = serverLevel.getStructureManager();

            StructureTemplate structureTemplate;
            try {
                structureTemplate = structureManager.getOrCreate(this.structureName);
            } catch (ResourceLocationException var8) {
                return false;
            }

            structureTemplate.fillFromWorld(this.level, blockPos, this.structureSize, !this.ignoreEntities, Blocks.STRUCTURE_VOID);
            structureTemplate.setAuthor(this.author);
            if (bl) {
                try {
                    return structureManager.save(this.structureName);
                } catch (ResourceLocationException var7) {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean loadStructure(ServerLevel world) {
        return this.loadStructure(world, true);
    }

    private static Random createRandom(long seed) {
        return seed == 0L ? new Random(Util.getMillis()) : new Random(seed);
    }

    public boolean loadStructure(ServerLevel world, boolean bl) {
        if (this.mode == StructureMode.LOAD && this.structureName != null) {
            StructureManager structureManager = world.getStructureManager();

            Optional<StructureTemplate> optional;
            try {
                optional = structureManager.get(this.structureName);
            } catch (ResourceLocationException var6) {
                return false;
            }

            return !optional.isPresent() ? false : this.loadStructure(world, bl, optional.get());
        } else {
            return false;
        }
    }

    public boolean loadStructure(ServerLevel world, boolean bl, StructureTemplate structure) {
        BlockPos blockPos = this.getBlockPos();
        if (!StringUtil.isNullOrEmpty(structure.getAuthor())) {
            this.author = structure.getAuthor();
        }

        Vec3i vec3i = structure.getSize();
        boolean bl2 = this.structureSize.equals(vec3i);
        if (!bl2) {
            this.structureSize = vec3i;
            this.setChanged();
            BlockState blockState = world.getBlockState(blockPos);
            world.sendBlockUpdated(blockPos, blockState, blockState, 3);
        }

        if (bl && !bl2) {
            return false;
        } else {
            StructurePlaceSettings structurePlaceSettings = (new StructurePlaceSettings()).setMirror(this.mirror).setRotation(this.rotation).setIgnoreEntities(this.ignoreEntities);
            if (this.integrity < 1.0F) {
                structurePlaceSettings.clearProcessors().addProcessor(new BlockRotProcessor(Mth.clamp(this.integrity, 0.0F, 1.0F))).setRandom(createRandom(this.seed));
            }

            BlockPos blockPos2 = blockPos.offset(this.structurePos);
            structure.placeInWorld(world, blockPos2, blockPos2, structurePlaceSettings, createRandom(this.seed), 2);
            return true;
        }
    }

    public void unloadStructure() {
        if (this.structureName != null) {
            ServerLevel serverLevel = (ServerLevel)this.level;
            StructureManager structureManager = serverLevel.getStructureManager();
            structureManager.remove(this.structureName);
        }
    }

    public boolean isStructureLoadable() {
        if (this.mode == StructureMode.LOAD && !this.level.isClientSide && this.structureName != null) {
            ServerLevel serverLevel = (ServerLevel)this.level;
            StructureManager structureManager = serverLevel.getStructureManager();

            try {
                return structureManager.get(this.structureName).isPresent();
            } catch (ResourceLocationException var4) {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public boolean getShowAir() {
        return this.showAir;
    }

    public void setShowAir(boolean showAir) {
        this.showAir = showAir;
    }

    public boolean getShowBoundingBox() {
        return this.showBoundingBox;
    }

    public void setShowBoundingBox(boolean showBoundingBox) {
        this.showBoundingBox = showBoundingBox;
    }

    public static enum UpdateType {
        UPDATE_DATA,
        SAVE_AREA,
        LOAD_AREA,
        SCAN_AREA;
    }
}
