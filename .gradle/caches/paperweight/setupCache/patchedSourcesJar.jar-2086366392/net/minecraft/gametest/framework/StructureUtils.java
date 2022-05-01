package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class StructureUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String DEFAULT_TEST_STRUCTURES_DIR = "gameteststructures";
    public static String testStructuresDir = "gameteststructures";
    private static final int HOW_MANY_CHUNKS_TO_LOAD_IN_EACH_DIRECTION_OF_STRUCTURE = 4;

    public static Rotation getRotationForRotationSteps(int steps) {
        switch(steps) {
        case 0:
            return Rotation.NONE;
        case 1:
            return Rotation.CLOCKWISE_90;
        case 2:
            return Rotation.CLOCKWISE_180;
        case 3:
            return Rotation.COUNTERCLOCKWISE_90;
        default:
            throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + steps);
        }
    }

    public static int getRotationStepsForRotation(Rotation rotation) {
        switch(rotation) {
        case NONE:
            return 0;
        case CLOCKWISE_90:
            return 1;
        case CLOCKWISE_180:
            return 2;
        case COUNTERCLOCKWISE_90:
            return 3;
        default:
            throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + rotation);
        }
    }

    public static void main(String[] args) throws IOException {
        Bootstrap.bootStrap();
        Files.walk(Paths.get(testStructuresDir)).filter((path) -> {
            return path.toString().endsWith(".snbt");
        }).forEach((path) -> {
            try {
                String string = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                CompoundTag compoundTag = NbtUtils.snbtToStructure(string);
                CompoundTag compoundTag2 = StructureUpdater.update(path.toString(), compoundTag);
                NbtToSnbt.writeSnbt(path, NbtUtils.structureToSnbt(compoundTag2));
            } catch (IOException | CommandSyntaxException var4) {
                LOGGER.error("Something went wrong upgrading: {}", path, var4);
            }

        });
    }

    public static AABB getStructureBounds(StructureBlockEntity structureBlockEntity) {
        BlockPos blockPos = structureBlockEntity.getBlockPos();
        BlockPos blockPos2 = blockPos.offset(structureBlockEntity.getStructureSize().offset(-1, -1, -1));
        BlockPos blockPos3 = StructureTemplate.transform(blockPos2, Mirror.NONE, structureBlockEntity.getRotation(), blockPos);
        return new AABB(blockPos, blockPos3);
    }

    public static BoundingBox getStructureBoundingBox(StructureBlockEntity structureBlockEntity) {
        BlockPos blockPos = structureBlockEntity.getBlockPos();
        BlockPos blockPos2 = blockPos.offset(structureBlockEntity.getStructureSize().offset(-1, -1, -1));
        BlockPos blockPos3 = StructureTemplate.transform(blockPos2, Mirror.NONE, structureBlockEntity.getRotation(), blockPos);
        return BoundingBox.fromCorners(blockPos, blockPos3);
    }

    public static void addCommandBlockAndButtonToStartTest(BlockPos pos, BlockPos relativePos, Rotation rotation, ServerLevel world) {
        BlockPos blockPos = StructureTemplate.transform(pos.offset(relativePos), Mirror.NONE, rotation, pos);
        world.setBlockAndUpdate(blockPos, Blocks.COMMAND_BLOCK.defaultBlockState());
        CommandBlockEntity commandBlockEntity = (CommandBlockEntity)world.getBlockEntity(blockPos);
        commandBlockEntity.getCommandBlock().setCommand("test runthis");
        BlockPos blockPos2 = StructureTemplate.transform(blockPos.offset(0, 0, -1), Mirror.NONE, rotation, blockPos);
        world.setBlockAndUpdate(blockPos2, Blocks.STONE_BUTTON.defaultBlockState().rotate(rotation));
    }

    public static void createNewEmptyStructureBlock(String structure, BlockPos pos, Vec3i relativePos, Rotation rotation, ServerLevel world) {
        BoundingBox boundingBox = getStructureBoundingBox(pos, relativePos, rotation);
        clearSpaceForStructure(boundingBox, pos.getY(), world);
        world.setBlockAndUpdate(pos, Blocks.STRUCTURE_BLOCK.defaultBlockState());
        StructureBlockEntity structureBlockEntity = (StructureBlockEntity)world.getBlockEntity(pos);
        structureBlockEntity.setIgnoreEntities(false);
        structureBlockEntity.setStructureName(new ResourceLocation(structure));
        structureBlockEntity.setStructureSize(relativePos);
        structureBlockEntity.setMode(StructureMode.SAVE);
        structureBlockEntity.setShowBoundingBox(true);
    }

    public static StructureBlockEntity spawnStructure(String structureName, BlockPos pos, Rotation rotation, int i, ServerLevel world, boolean bl) {
        Vec3i vec3i = getStructureTemplate(structureName, world).getSize();
        BoundingBox boundingBox = getStructureBoundingBox(pos, vec3i, rotation);
        BlockPos blockPos;
        if (rotation == Rotation.NONE) {
            blockPos = pos;
        } else if (rotation == Rotation.CLOCKWISE_90) {
            blockPos = pos.offset(vec3i.getZ() - 1, 0, 0);
        } else if (rotation == Rotation.CLOCKWISE_180) {
            blockPos = pos.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);
        } else {
            if (rotation != Rotation.COUNTERCLOCKWISE_90) {
                throw new IllegalArgumentException("Invalid rotation: " + rotation);
            }

            blockPos = pos.offset(0, 0, vec3i.getX() - 1);
        }

        forceLoadChunks(pos, world);
        clearSpaceForStructure(boundingBox, pos.getY(), world);
        StructureBlockEntity structureBlockEntity = createStructureBlock(structureName, blockPos, rotation, world, bl);
        world.getBlockTicks().clearArea(boundingBox);
        world.clearBlockEvents(boundingBox);
        return structureBlockEntity;
    }

    private static void forceLoadChunks(BlockPos pos, ServerLevel world) {
        ChunkPos chunkPos = new ChunkPos(pos);

        for(int i = -1; i < 4; ++i) {
            for(int j = -1; j < 4; ++j) {
                int k = chunkPos.x + i;
                int l = chunkPos.z + j;
                world.setChunkForced(k, l, true);
            }
        }

    }

    public static void clearSpaceForStructure(BoundingBox area, int altitude, ServerLevel world) {
        BoundingBox boundingBox = new BoundingBox(area.minX() - 2, area.minY() - 3, area.minZ() - 3, area.maxX() + 3, area.maxY() + 20, area.maxZ() + 3);
        BlockPos.betweenClosedStream(boundingBox).forEach((pos) -> {
            clearBlock(altitude, pos, world);
        });
        world.getBlockTicks().clearArea(boundingBox);
        world.clearBlockEvents(boundingBox);
        AABB aABB = new AABB((double)boundingBox.minX(), (double)boundingBox.minY(), (double)boundingBox.minZ(), (double)boundingBox.maxX(), (double)boundingBox.maxY(), (double)boundingBox.maxZ());
        List<Entity> list = world.getEntitiesOfClass(Entity.class, aABB, (entity) -> {
            return !(entity instanceof Player);
        });
        list.forEach(Entity::discard);
    }

    public static BoundingBox getStructureBoundingBox(BlockPos pos, Vec3i relativePos, Rotation rotation) {
        BlockPos blockPos = pos.offset(relativePos).offset(-1, -1, -1);
        BlockPos blockPos2 = StructureTemplate.transform(blockPos, Mirror.NONE, rotation, pos);
        BoundingBox boundingBox = BoundingBox.fromCorners(pos, blockPos2);
        int i = Math.min(boundingBox.minX(), boundingBox.maxX());
        int j = Math.min(boundingBox.minZ(), boundingBox.maxZ());
        return boundingBox.move(pos.getX() - i, 0, pos.getZ() - j);
    }

    public static Optional<BlockPos> findStructureBlockContainingPos(BlockPos pos, int radius, ServerLevel world) {
        return findStructureBlocks(pos, radius, world).stream().filter((structureBlockPos) -> {
            return doesStructureContain(structureBlockPos, pos, world);
        }).findFirst();
    }

    @Nullable
    public static BlockPos findNearestStructureBlock(BlockPos pos, int radius, ServerLevel world) {
        Comparator<BlockPos> comparator = Comparator.comparingInt((posx) -> {
            return posx.distManhattan(pos);
        });
        Collection<BlockPos> collection = findStructureBlocks(pos, radius, world);
        Optional<BlockPos> optional = collection.stream().min(comparator);
        return optional.orElse((BlockPos)null);
    }

    public static Collection<BlockPos> findStructureBlocks(BlockPos pos, int radius, ServerLevel world) {
        Collection<BlockPos> collection = Lists.newArrayList();
        AABB aABB = new AABB(pos);
        aABB = aABB.inflate((double)radius);

        for(int i = (int)aABB.minX; i <= (int)aABB.maxX; ++i) {
            for(int j = (int)aABB.minY; j <= (int)aABB.maxY; ++j) {
                for(int k = (int)aABB.minZ; k <= (int)aABB.maxZ; ++k) {
                    BlockPos blockPos = new BlockPos(i, j, k);
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.is(Blocks.STRUCTURE_BLOCK)) {
                        collection.add(blockPos);
                    }
                }
            }
        }

        return collection;
    }

    private static StructureTemplate getStructureTemplate(String structureId, ServerLevel world) {
        StructureManager structureManager = world.getStructureManager();
        Optional<StructureTemplate> optional = structureManager.get(new ResourceLocation(structureId));
        if (optional.isPresent()) {
            return optional.get();
        } else {
            String string = structureId + ".snbt";
            Path path = Paths.get(testStructuresDir, string);
            CompoundTag compoundTag = tryLoadStructure(path);
            if (compoundTag == null) {
                throw new RuntimeException("Could not find structure file " + path + ", and the structure is not available in the world structures either.");
            } else {
                return structureManager.readStructure(compoundTag);
            }
        }
    }

    private static StructureBlockEntity createStructureBlock(String name, BlockPos pos, Rotation rotation, ServerLevel world, boolean bl) {
        world.setBlockAndUpdate(pos, Blocks.STRUCTURE_BLOCK.defaultBlockState());
        StructureBlockEntity structureBlockEntity = (StructureBlockEntity)world.getBlockEntity(pos);
        structureBlockEntity.setMode(StructureMode.LOAD);
        structureBlockEntity.setRotation(rotation);
        structureBlockEntity.setIgnoreEntities(false);
        structureBlockEntity.setStructureName(new ResourceLocation(name));
        structureBlockEntity.loadStructure(world, bl);
        if (structureBlockEntity.getStructureSize() != Vec3i.ZERO) {
            return structureBlockEntity;
        } else {
            StructureTemplate structureTemplate = getStructureTemplate(name, world);
            structureBlockEntity.loadStructure(world, bl, structureTemplate);
            if (structureBlockEntity.getStructureSize() == Vec3i.ZERO) {
                throw new RuntimeException("Failed to load structure " + name);
            } else {
                return structureBlockEntity;
            }
        }
    }

    @Nullable
    private static CompoundTag tryLoadStructure(Path path) {
        try {
            BufferedReader bufferedReader = Files.newBufferedReader(path);
            String string = IOUtils.toString((Reader)bufferedReader);
            return NbtUtils.snbtToStructure(string);
        } catch (IOException var3) {
            return null;
        } catch (CommandSyntaxException var4) {
            throw new RuntimeException("Error while trying to load structure " + path, var4);
        }
    }

    private static void clearBlock(int altitude, BlockPos pos, ServerLevel world) {
        BlockState blockState = null;
        FlatLevelGeneratorSettings flatLevelGeneratorSettings = FlatLevelGeneratorSettings.getDefault(world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), world.registryAccess().registryOrThrow(Registry.STRUCTURE_SET_REGISTRY));
        List<BlockState> list = flatLevelGeneratorSettings.getLayers();
        int i = pos.getY() - world.getMinBuildHeight();
        if (pos.getY() < altitude && i > 0 && i <= list.size()) {
            blockState = list.get(i - 1);
        }

        if (blockState == null) {
            blockState = Blocks.AIR.defaultBlockState();
        }

        BlockInput blockInput = new BlockInput(blockState, Collections.emptySet(), (CompoundTag)null);
        blockInput.place(world, pos, 2);
        world.blockUpdated(pos, blockState.getBlock());
    }

    private static boolean doesStructureContain(BlockPos structureBlockPos, BlockPos pos, ServerLevel world) {
        StructureBlockEntity structureBlockEntity = (StructureBlockEntity)world.getBlockEntity(structureBlockPos);
        AABB aABB = getStructureBounds(structureBlockEntity).inflate(1.0D);
        return aABB.contains(Vec3.atCenterOf(pos));
    }
}
