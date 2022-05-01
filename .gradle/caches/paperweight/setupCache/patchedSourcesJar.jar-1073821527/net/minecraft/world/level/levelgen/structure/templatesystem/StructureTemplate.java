package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class StructureTemplate {
    public static final String PALETTE_TAG = "palette";
    public static final String PALETTE_LIST_TAG = "palettes";
    public static final String ENTITIES_TAG = "entities";
    public static final String BLOCKS_TAG = "blocks";
    public static final String BLOCK_TAG_POS = "pos";
    public static final String BLOCK_TAG_STATE = "state";
    public static final String BLOCK_TAG_NBT = "nbt";
    public static final String ENTITY_TAG_POS = "pos";
    public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
    public static final String ENTITY_TAG_NBT = "nbt";
    public static final String SIZE_TAG = "size";
    static final int CHUNK_SIZE = 16;
    public final List<StructureTemplate.Palette> palettes = Lists.newArrayList();
    public final List<StructureTemplate.StructureEntityInfo> entityInfoList = Lists.newArrayList();
    private Vec3i size = Vec3i.ZERO;
    private String author = "?";

    public Vec3i getSize() {
        return this.size;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return this.author;
    }

    public void fillFromWorld(Level world, BlockPos start, Vec3i dimensions, boolean includeEntities, @Nullable Block ignoredBlock) {
        if (dimensions.getX() >= 1 && dimensions.getY() >= 1 && dimensions.getZ() >= 1) {
            BlockPos blockPos = start.offset(dimensions).offset(-1, -1, -1);
            List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();
            BlockPos blockPos2 = new BlockPos(Math.min(start.getX(), blockPos.getX()), Math.min(start.getY(), blockPos.getY()), Math.min(start.getZ(), blockPos.getZ()));
            BlockPos blockPos3 = new BlockPos(Math.max(start.getX(), blockPos.getX()), Math.max(start.getY(), blockPos.getY()), Math.max(start.getZ(), blockPos.getZ()));
            this.size = dimensions;

            for(BlockPos blockPos4 : BlockPos.betweenClosed(blockPos2, blockPos3)) {
                BlockPos blockPos5 = blockPos4.subtract(blockPos2);
                BlockState blockState = world.getBlockState(blockPos4);
                if (ignoredBlock == null || !blockState.is(ignoredBlock)) {
                    BlockEntity blockEntity = world.getBlockEntity(blockPos4);
                    StructureTemplate.StructureBlockInfo structureBlockInfo;
                    if (blockEntity != null) {
                        structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos5, blockState, blockEntity.saveWithId());
                    } else {
                        structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos5, blockState, (CompoundTag)null);
                    }

                    addToLists(structureBlockInfo, list, list2, list3);
                }
            }

            List<StructureTemplate.StructureBlockInfo> list4 = buildInfoList(list, list2, list3);
            this.palettes.clear();
            this.palettes.add(new StructureTemplate.Palette(list4));
            if (includeEntities) {
                this.fillEntityList(world, blockPos2, blockPos3.offset(1, 1, 1));
            } else {
                this.entityInfoList.clear();
            }

        }
    }

    private static void addToLists(StructureTemplate.StructureBlockInfo blockInfo, List<StructureTemplate.StructureBlockInfo> fullBlocks, List<StructureTemplate.StructureBlockInfo> blocksWithNbt, List<StructureTemplate.StructureBlockInfo> otherBlocks) {
        if (blockInfo.nbt != null) {
            blocksWithNbt.add(blockInfo);
        } else if (!blockInfo.state.getBlock().hasDynamicShape() && blockInfo.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            fullBlocks.add(blockInfo);
        } else {
            otherBlocks.add(blockInfo);
        }

    }

    private static List<StructureTemplate.StructureBlockInfo> buildInfoList(List<StructureTemplate.StructureBlockInfo> fullBlocks, List<StructureTemplate.StructureBlockInfo> blocksWithNbt, List<StructureTemplate.StructureBlockInfo> otherBlocks) {
        Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.comparingInt((blockInfo) -> {
            return blockInfo.pos.getY();
        }).thenComparingInt((blockInfo) -> {
            return blockInfo.pos.getX();
        }).thenComparingInt((blockInfo) -> {
            return blockInfo.pos.getZ();
        });
        fullBlocks.sort(comparator);
        otherBlocks.sort(comparator);
        blocksWithNbt.sort(comparator);
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        list.addAll(fullBlocks);
        list.addAll(otherBlocks);
        list.addAll(blocksWithNbt);
        return list;
    }

    private void fillEntityList(Level world, BlockPos firstCorner, BlockPos secondCorner) {
        List<Entity> list = world.getEntitiesOfClass(Entity.class, new AABB(firstCorner, secondCorner), (entityx) -> {
            return !(entityx instanceof Player);
        });
        this.entityInfoList.clear();

        for(Entity entity : list) {
            Vec3 vec3 = new Vec3(entity.getX() - (double)firstCorner.getX(), entity.getY() - (double)firstCorner.getY(), entity.getZ() - (double)firstCorner.getZ());
            CompoundTag compoundTag = new CompoundTag();
            entity.save(compoundTag);
            BlockPos blockPos;
            if (entity instanceof Painting) {
                blockPos = ((Painting)entity).getPos().subtract(firstCorner);
            } else {
                blockPos = new BlockPos(vec3);
            }

            this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockPos, compoundTag.copy()));
        }

    }

    public List<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos pos, StructurePlaceSettings placementData, Block block) {
        return this.filterBlocks(pos, placementData, block, true);
    }

    public List<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos pos, StructurePlaceSettings placementData, Block block, boolean transformed) {
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        BoundingBox boundingBox = placementData.getBoundingBox();
        if (this.palettes.isEmpty()) {
            return Collections.emptyList();
        } else {
            for(StructureTemplate.StructureBlockInfo structureBlockInfo : placementData.getRandomPalette(this.palettes, pos).blocks(block)) {
                BlockPos blockPos = transformed ? calculateRelativePosition(placementData, structureBlockInfo.pos).offset(pos) : structureBlockInfo.pos;
                if (boundingBox == null || boundingBox.isInside(blockPos)) {
                    list.add(new StructureTemplate.StructureBlockInfo(blockPos, structureBlockInfo.state.rotate(placementData.getRotation()), structureBlockInfo.nbt));
                }
            }

            return list;
        }
    }

    public BlockPos calculateConnectedPosition(StructurePlaceSettings placementData1, BlockPos pos1, StructurePlaceSettings placementData2, BlockPos pos2) {
        BlockPos blockPos = calculateRelativePosition(placementData1, pos1);
        BlockPos blockPos2 = calculateRelativePosition(placementData2, pos2);
        return blockPos.subtract(blockPos2);
    }

    public static BlockPos calculateRelativePosition(StructurePlaceSettings placementData, BlockPos pos) {
        return transform(pos, placementData.getMirror(), placementData.getRotation(), placementData.getRotationPivot());
    }

    public boolean placeInWorld(ServerLevelAccessor world, BlockPos pos, BlockPos pivot, StructurePlaceSettings placementData, Random random, int flags) {
        if (this.palettes.isEmpty()) {
            return false;
        } else {
            List<StructureTemplate.StructureBlockInfo> list = placementData.getRandomPalette(this.palettes, pos).blocks();
            if ((!list.isEmpty() || !placementData.isIgnoreEntities() && !this.entityInfoList.isEmpty()) && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
                BoundingBox boundingBox = placementData.getBoundingBox();
                List<BlockPos> list2 = Lists.newArrayListWithCapacity(placementData.shouldKeepLiquids() ? list.size() : 0);
                List<BlockPos> list3 = Lists.newArrayListWithCapacity(placementData.shouldKeepLiquids() ? list.size() : 0);
                List<Pair<BlockPos, CompoundTag>> list4 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int m = Integer.MIN_VALUE;
                int n = Integer.MIN_VALUE;

                for(StructureTemplate.StructureBlockInfo structureBlockInfo : processBlockInfos(world, pos, pivot, placementData, list)) {
                    BlockPos blockPos = structureBlockInfo.pos;
                    if (boundingBox == null || boundingBox.isInside(blockPos)) {
                        FluidState fluidState = placementData.shouldKeepLiquids() ? world.getFluidState(blockPos) : null;
                        BlockState blockState = structureBlockInfo.state.mirror(placementData.getMirror()).rotate(placementData.getRotation());
                        if (structureBlockInfo.nbt != null) {
                            BlockEntity blockEntity = world.getBlockEntity(blockPos);
                            Clearable.tryClear(blockEntity);
                            world.setBlock(blockPos, Blocks.BARRIER.defaultBlockState(), 20);
                        }

                        if (world.setBlock(blockPos, blockState, flags)) {
                            i = Math.min(i, blockPos.getX());
                            j = Math.min(j, blockPos.getY());
                            k = Math.min(k, blockPos.getZ());
                            l = Math.max(l, blockPos.getX());
                            m = Math.max(m, blockPos.getY());
                            n = Math.max(n, blockPos.getZ());
                            list4.add(Pair.of(blockPos, structureBlockInfo.nbt));
                            if (structureBlockInfo.nbt != null) {
                                BlockEntity blockEntity2 = world.getBlockEntity(blockPos);
                                if (blockEntity2 != null) {
                                    if (blockEntity2 instanceof RandomizableContainerBlockEntity) {
                                        structureBlockInfo.nbt.putLong("LootTableSeed", random.nextLong());
                                    }

                                    blockEntity2.load(structureBlockInfo.nbt);
                                }
                            }

                            if (fluidState != null) {
                                if (blockState.getFluidState().isSource()) {
                                    list3.add(blockPos);
                                } else if (blockState.getBlock() instanceof LiquidBlockContainer) {
                                    ((LiquidBlockContainer)blockState.getBlock()).placeLiquid(world, blockPos, blockState, fluidState);
                                    if (!fluidState.isSource()) {
                                        list2.add(blockPos);
                                    }
                                }
                            }
                        }
                    }
                }

                boolean bl = true;
                Direction[] directions = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

                while(bl && !list2.isEmpty()) {
                    bl = false;
                    Iterator<BlockPos> iterator = list2.iterator();

                    while(iterator.hasNext()) {
                        BlockPos blockPos2 = iterator.next();
                        FluidState fluidState2 = world.getFluidState(blockPos2);

                        for(int o = 0; o < directions.length && !fluidState2.isSource(); ++o) {
                            BlockPos blockPos3 = blockPos2.relative(directions[o]);
                            FluidState fluidState3 = world.getFluidState(blockPos3);
                            if (fluidState3.isSource() && !list3.contains(blockPos3)) {
                                fluidState2 = fluidState3;
                            }
                        }

                        if (fluidState2.isSource()) {
                            BlockState blockState2 = world.getBlockState(blockPos2);
                            Block block = blockState2.getBlock();
                            if (block instanceof LiquidBlockContainer) {
                                ((LiquidBlockContainer)block).placeLiquid(world, blockPos2, blockState2, fluidState2);
                                bl = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (i <= l) {
                    if (!placementData.getKnownShape()) {
                        DiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(l - i + 1, m - j + 1, n - k + 1);
                        int p = i;
                        int q = j;
                        int r = k;

                        for(Pair<BlockPos, CompoundTag> pair : list4) {
                            BlockPos blockPos4 = pair.getFirst();
                            discreteVoxelShape.fill(blockPos4.getX() - p, blockPos4.getY() - q, blockPos4.getZ() - r);
                        }

                        updateShapeAtEdge(world, flags, discreteVoxelShape, p, q, r);
                    }

                    for(Pair<BlockPos, CompoundTag> pair2 : list4) {
                        BlockPos blockPos5 = pair2.getFirst();
                        if (!placementData.getKnownShape()) {
                            BlockState blockState3 = world.getBlockState(blockPos5);
                            BlockState blockState4 = Block.updateFromNeighbourShapes(blockState3, world, blockPos5);
                            if (blockState3 != blockState4) {
                                world.setBlock(blockPos5, blockState4, flags & -2 | 16);
                            }

                            world.blockUpdated(blockPos5, blockState4.getBlock());
                        }

                        if (pair2.getSecond() != null) {
                            BlockEntity blockEntity3 = world.getBlockEntity(blockPos5);
                            if (blockEntity3 != null) {
                                blockEntity3.setChanged();
                            }
                        }
                    }
                }

                if (!placementData.isIgnoreEntities()) {
                    this.placeEntities(world, pos, placementData.getMirror(), placementData.getRotation(), placementData.getRotationPivot(), boundingBox, placementData.shouldFinalizeEntities());
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public static void updateShapeAtEdge(LevelAccessor world, int flags, DiscreteVoxelShape discreteVoxelShape, int startX, int startY, int startZ) {
        discreteVoxelShape.forAllFaces((direction, m, n, o) -> {
            BlockPos blockPos = new BlockPos(startX + m, startY + n, startZ + o);
            BlockPos blockPos2 = blockPos.relative(direction);
            BlockState blockState = world.getBlockState(blockPos);
            BlockState blockState2 = world.getBlockState(blockPos2);
            BlockState blockState3 = blockState.updateShape(direction, blockState2, world, blockPos, blockPos2);
            if (blockState != blockState3) {
                world.setBlock(blockPos, blockState3, flags & -2);
            }

            BlockState blockState4 = blockState2.updateShape(direction.getOpposite(), blockState3, world, blockPos2, blockPos);
            if (blockState2 != blockState4) {
                world.setBlock(blockPos2, blockState4, flags & -2);
            }

        });
    }

    public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(LevelAccessor world, BlockPos pos, BlockPos pivot, StructurePlaceSettings placementData, List<StructureTemplate.StructureBlockInfo> list) {
        List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();

        for(StructureTemplate.StructureBlockInfo structureBlockInfo : list) {
            BlockPos blockPos = calculateRelativePosition(placementData, structureBlockInfo.pos).offset(pos);
            StructureTemplate.StructureBlockInfo structureBlockInfo2 = new StructureTemplate.StructureBlockInfo(blockPos, structureBlockInfo.state, structureBlockInfo.nbt != null ? structureBlockInfo.nbt.copy() : null);

            for(Iterator<StructureProcessor> iterator = placementData.getProcessors().iterator(); structureBlockInfo2 != null && iterator.hasNext(); structureBlockInfo2 = iterator.next().processBlock(world, pos, pivot, structureBlockInfo, structureBlockInfo2, placementData)) {
            }

            if (structureBlockInfo2 != null) {
                list2.add(structureBlockInfo2);
            }
        }

        return list2;
    }

    private void placeEntities(ServerLevelAccessor world, BlockPos pos, Mirror mirror, Rotation rotation, BlockPos pivot, @Nullable BoundingBox area, boolean initializeMobs) {
        for(StructureTemplate.StructureEntityInfo structureEntityInfo : this.entityInfoList) {
            BlockPos blockPos = transform(structureEntityInfo.blockPos, mirror, rotation, pivot).offset(pos);
            if (area == null || area.isInside(blockPos)) {
                CompoundTag compoundTag = structureEntityInfo.nbt.copy();
                Vec3 vec3 = transform(structureEntityInfo.pos, mirror, rotation, pivot);
                Vec3 vec32 = vec3.add((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                ListTag listTag = new ListTag();
                listTag.add(DoubleTag.valueOf(vec32.x));
                listTag.add(DoubleTag.valueOf(vec32.y));
                listTag.add(DoubleTag.valueOf(vec32.z));
                compoundTag.put("Pos", listTag);
                compoundTag.remove("UUID");
                createEntityIgnoreException(world, compoundTag).ifPresent((entity) -> {
                    float f = entity.rotate(rotation);
                    f += entity.mirror(mirror) - entity.getYRot();
                    entity.moveTo(vec32.x, vec32.y, vec32.z, f, entity.getXRot());
                    if (initializeMobs && entity instanceof Mob) {
                        ((Mob)entity).finalizeSpawn(world, world.getCurrentDifficultyAt(new BlockPos(vec32)), MobSpawnType.STRUCTURE, (SpawnGroupData)null, compoundTag);
                    }

                    world.addFreshEntityWithPassengers(entity);
                });
            }
        }

    }

    private static Optional<Entity> createEntityIgnoreException(ServerLevelAccessor world, CompoundTag nbt) {
        try {
            return EntityType.create(nbt, world.getLevel());
        } catch (Exception var3) {
            return Optional.empty();
        }
    }

    public Vec3i getSize(Rotation rotation) {
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
        case CLOCKWISE_90:
            return new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
        default:
            return this.size;
        }
    }

    public static BlockPos transform(BlockPos pos, Mirror mirror, Rotation rotation, BlockPos pivot) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        boolean bl = true;
        switch(mirror) {
        case LEFT_RIGHT:
            k = -k;
            break;
        case FRONT_BACK:
            i = -i;
            break;
        default:
            bl = false;
        }

        int l = pivot.getX();
        int m = pivot.getZ();
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
            return new BlockPos(l - m + k, j, l + m - i);
        case CLOCKWISE_90:
            return new BlockPos(l + m - k, j, m - l + i);
        case CLOCKWISE_180:
            return new BlockPos(l + l - i, j, m + m - k);
        default:
            return bl ? new BlockPos(i, j, k) : pos;
        }
    }

    public static Vec3 transform(Vec3 point, Mirror mirror, Rotation rotation, BlockPos pivot) {
        double d = point.x;
        double e = point.y;
        double f = point.z;
        boolean bl = true;
        switch(mirror) {
        case LEFT_RIGHT:
            f = 1.0D - f;
            break;
        case FRONT_BACK:
            d = 1.0D - d;
            break;
        default:
            bl = false;
        }

        int i = pivot.getX();
        int j = pivot.getZ();
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
            return new Vec3((double)(i - j) + f, e, (double)(i + j + 1) - d);
        case CLOCKWISE_90:
            return new Vec3((double)(i + j + 1) - f, e, (double)(j - i) + d);
        case CLOCKWISE_180:
            return new Vec3((double)(i + i + 1) - d, e, (double)(j + j + 1) - f);
        default:
            return bl ? new Vec3(d, e, f) : point;
        }
    }

    public BlockPos getZeroPositionWithTransform(BlockPos pos, Mirror mirror, Rotation rotation) {
        return getZeroPositionWithTransform(pos, mirror, rotation, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPos getZeroPositionWithTransform(BlockPos pos, Mirror mirror, Rotation rotation, int offsetX, int offsetZ) {
        --offsetX;
        --offsetZ;
        int i = mirror == Mirror.FRONT_BACK ? offsetX : 0;
        int j = mirror == Mirror.LEFT_RIGHT ? offsetZ : 0;
        BlockPos blockPos = pos;
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
            blockPos = pos.offset(j, 0, offsetX - i);
            break;
        case CLOCKWISE_90:
            blockPos = pos.offset(offsetZ - j, 0, i);
            break;
        case CLOCKWISE_180:
            blockPos = pos.offset(offsetX - i, 0, offsetZ - j);
            break;
        case NONE:
            blockPos = pos.offset(i, 0, j);
        }

        return blockPos;
    }

    public BoundingBox getBoundingBox(StructurePlaceSettings placementData, BlockPos pos) {
        return this.getBoundingBox(pos, placementData.getRotation(), placementData.getRotationPivot(), placementData.getMirror());
    }

    public BoundingBox getBoundingBox(BlockPos pos, Rotation rotation, BlockPos pivot, Mirror mirror) {
        return getBoundingBox(pos, rotation, pivot, mirror, this.size);
    }

    @VisibleForTesting
    protected static BoundingBox getBoundingBox(BlockPos pos, Rotation rotation, BlockPos pivot, Mirror mirror, Vec3i dimensions) {
        Vec3i vec3i = dimensions.offset(-1, -1, -1);
        BlockPos blockPos = transform(BlockPos.ZERO, mirror, rotation, pivot);
        BlockPos blockPos2 = transform(BlockPos.ZERO.offset(vec3i), mirror, rotation, pivot);
        return BoundingBox.fromCorners(blockPos, blockPos2).move(pos);
    }

    public CompoundTag save(CompoundTag nbt) {
        if (this.palettes.isEmpty()) {
            nbt.put("blocks", new ListTag());
            nbt.put("palette", new ListTag());
        } else {
            List<StructureTemplate.SimplePalette> list = Lists.newArrayList();
            StructureTemplate.SimplePalette simplePalette = new StructureTemplate.SimplePalette();
            list.add(simplePalette);

            for(int i = 1; i < this.palettes.size(); ++i) {
                list.add(new StructureTemplate.SimplePalette());
            }

            ListTag listTag = new ListTag();
            List<StructureTemplate.StructureBlockInfo> list2 = this.palettes.get(0).blocks();

            for(int j = 0; j < list2.size(); ++j) {
                StructureTemplate.StructureBlockInfo structureBlockInfo = list2.get(j);
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.put("pos", this.newIntegerList(structureBlockInfo.pos.getX(), structureBlockInfo.pos.getY(), structureBlockInfo.pos.getZ()));
                int k = simplePalette.idFor(structureBlockInfo.state);
                compoundTag.putInt("state", k);
                if (structureBlockInfo.nbt != null) {
                    compoundTag.put("nbt", structureBlockInfo.nbt);
                }

                listTag.add(compoundTag);

                for(int l = 1; l < this.palettes.size(); ++l) {
                    StructureTemplate.SimplePalette simplePalette2 = list.get(l);
                    simplePalette2.addMapping((this.palettes.get(l).blocks().get(j)).state, k);
                }
            }

            nbt.put("blocks", listTag);
            if (list.size() == 1) {
                ListTag listTag2 = new ListTag();

                for(BlockState blockState : simplePalette) {
                    listTag2.add(NbtUtils.writeBlockState(blockState));
                }

                nbt.put("palette", listTag2);
            } else {
                ListTag listTag3 = new ListTag();

                for(StructureTemplate.SimplePalette simplePalette3 : list) {
                    ListTag listTag4 = new ListTag();

                    for(BlockState blockState2 : simplePalette3) {
                        listTag4.add(NbtUtils.writeBlockState(blockState2));
                    }

                    listTag3.add(listTag4);
                }

                nbt.put("palettes", listTag3);
            }
        }

        ListTag listTag5 = new ListTag();

        for(StructureTemplate.StructureEntityInfo structureEntityInfo : this.entityInfoList) {
            CompoundTag compoundTag2 = new CompoundTag();
            compoundTag2.put("pos", this.newDoubleList(structureEntityInfo.pos.x, structureEntityInfo.pos.y, structureEntityInfo.pos.z));
            compoundTag2.put("blockPos", this.newIntegerList(structureEntityInfo.blockPos.getX(), structureEntityInfo.blockPos.getY(), structureEntityInfo.blockPos.getZ()));
            if (structureEntityInfo.nbt != null) {
                compoundTag2.put("nbt", structureEntityInfo.nbt);
            }

            listTag5.add(compoundTag2);
        }

        nbt.put("entities", listTag5);
        nbt.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
        nbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        return nbt;
    }

    public void load(CompoundTag nbt) {
        this.palettes.clear();
        this.entityInfoList.clear();
        ListTag listTag = nbt.getList("size", 3);
        this.size = new Vec3i(listTag.getInt(0), listTag.getInt(1), listTag.getInt(2));
        ListTag listTag2 = nbt.getList("blocks", 10);
        if (nbt.contains("palettes", 9)) {
            ListTag listTag3 = nbt.getList("palettes", 9);

            for(int i = 0; i < listTag3.size(); ++i) {
                this.loadPalette(listTag3.getList(i), listTag2);
            }
        } else {
            this.loadPalette(nbt.getList("palette", 10), listTag2);
        }

        ListTag listTag4 = nbt.getList("entities", 10);

        for(int j = 0; j < listTag4.size(); ++j) {
            CompoundTag compoundTag = listTag4.getCompound(j);
            ListTag listTag5 = compoundTag.getList("pos", 6);
            Vec3 vec3 = new Vec3(listTag5.getDouble(0), listTag5.getDouble(1), listTag5.getDouble(2));
            ListTag listTag6 = compoundTag.getList("blockPos", 3);
            BlockPos blockPos = new BlockPos(listTag6.getInt(0), listTag6.getInt(1), listTag6.getInt(2));
            if (compoundTag.contains("nbt")) {
                CompoundTag compoundTag2 = compoundTag.getCompound("nbt");
                this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockPos, compoundTag2));
            }
        }

    }

    private void loadPalette(ListTag paletteNbt, ListTag blocksNbt) {
        StructureTemplate.SimplePalette simplePalette = new StructureTemplate.SimplePalette();

        for(int i = 0; i < paletteNbt.size(); ++i) {
            simplePalette.addMapping(NbtUtils.readBlockState(paletteNbt.getCompound(i)), i);
        }

        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();

        for(int j = 0; j < blocksNbt.size(); ++j) {
            CompoundTag compoundTag = blocksNbt.getCompound(j);
            ListTag listTag = compoundTag.getList("pos", 3);
            BlockPos blockPos = new BlockPos(listTag.getInt(0), listTag.getInt(1), listTag.getInt(2));
            BlockState blockState = simplePalette.stateFor(compoundTag.getInt("state"));
            CompoundTag compoundTag2;
            if (compoundTag.contains("nbt")) {
                compoundTag2 = compoundTag.getCompound("nbt");
            } else {
                compoundTag2 = null;
            }

            StructureTemplate.StructureBlockInfo structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos, blockState, compoundTag2);
            addToLists(structureBlockInfo, list, list2, list3);
        }

        List<StructureTemplate.StructureBlockInfo> list4 = buildInfoList(list, list2, list3);
        this.palettes.add(new StructureTemplate.Palette(list4));
    }

    private ListTag newIntegerList(int... ints) {
        ListTag listTag = new ListTag();

        for(int i : ints) {
            listTag.add(IntTag.valueOf(i));
        }

        return listTag;
    }

    private ListTag newDoubleList(double... doubles) {
        ListTag listTag = new ListTag();

        for(double d : doubles) {
            listTag.add(DoubleTag.valueOf(d));
        }

        return listTag;
    }

    public static final class Palette {
        private final List<StructureTemplate.StructureBlockInfo> blocks;
        private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newHashMap();

        Palette(List<StructureTemplate.StructureBlockInfo> infos) {
            this.blocks = infos;
        }

        public List<StructureTemplate.StructureBlockInfo> blocks() {
            return this.blocks;
        }

        public List<StructureTemplate.StructureBlockInfo> blocks(Block block) {
            return this.cache.computeIfAbsent(block, (blockx) -> {
                return this.blocks.stream().filter((structureBlockInfo) -> {
                    return structureBlockInfo.state.is(blockx);
                }).collect(Collectors.toList());
            });
        }
    }

    static class SimplePalette implements Iterable<BlockState> {
        public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
        private final IdMapper<BlockState> ids = new IdMapper<>(16);
        private int lastId;

        public int idFor(BlockState state) {
            int i = this.ids.getId(state);
            if (i == -1) {
                i = this.lastId++;
                this.ids.addMapping(state, i);
            }

            return i;
        }

        @Nullable
        public BlockState stateFor(int id) {
            BlockState blockState = this.ids.byId(id);
            return blockState == null ? DEFAULT_BLOCK_STATE : blockState;
        }

        @Override
        public Iterator<BlockState> iterator() {
            return this.ids.iterator();
        }

        public void addMapping(BlockState state, int id) {
            this.ids.addMapping(state, id);
        }
    }

    public static class StructureBlockInfo {
        public final BlockPos pos;
        public final BlockState state;
        public final CompoundTag nbt;

        public StructureBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
            this.pos = pos;
            this.state = state;
            this.nbt = nbt;
        }

        @Override
        public String toString() {
            return String.format("<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
        }
    }

    public static class StructureEntityInfo {
        public final Vec3 pos;
        public final BlockPos blockPos;
        public final CompoundTag nbt;

        public StructureEntityInfo(Vec3 pos, BlockPos blockPos, CompoundTag nbt) {
            this.pos = pos;
            this.blockPos = blockPos;
            this.nbt = nbt;
        }
    }
}
