package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableInt;

public class GameTestRunner {
    private static final int MAX_TESTS_PER_BATCH = 100;
    public static final int PADDING_AROUND_EACH_STRUCTURE = 2;
    public static final int SPACE_BETWEEN_COLUMNS = 5;
    public static final int SPACE_BETWEEN_ROWS = 6;
    public static final int DEFAULT_TESTS_PER_ROW = 8;

    public static void runTest(GameTestInfo test, BlockPos pos, GameTestTicker testManager) {
        test.startExecution();
        testManager.add(test);
        test.addListener(new ReportGameListener(test, testManager, pos));
        test.spawnStructure(pos, 2);
    }

    public static Collection<GameTestInfo> runTestBatches(Collection<GameTestBatch> batches, BlockPos pos, Rotation rotation, ServerLevel world, GameTestTicker testManager, int sizeZ) {
        GameTestBatchRunner gameTestBatchRunner = new GameTestBatchRunner(batches, pos, rotation, world, testManager, sizeZ);
        gameTestBatchRunner.start();
        return gameTestBatchRunner.getTestInfos();
    }

    public static Collection<GameTestInfo> runTests(Collection<TestFunction> testFunctions, BlockPos pos, Rotation rotation, ServerLevel world, GameTestTicker testManager, int sizeZ) {
        return runTestBatches(groupTestsIntoBatches(testFunctions), pos, rotation, world, testManager, sizeZ);
    }

    public static Collection<GameTestBatch> groupTestsIntoBatches(Collection<TestFunction> testFunctions) {
        Map<String, List<TestFunction>> map = testFunctions.stream().collect(Collectors.groupingBy(TestFunction::getBatchName));
        return map.entrySet().stream().flatMap((entry) -> {
            String string = entry.getKey();
            Consumer<ServerLevel> consumer = GameTestRegistry.getBeforeBatchFunction(string);
            Consumer<ServerLevel> consumer2 = GameTestRegistry.getAfterBatchFunction(string);
            MutableInt mutableInt = new MutableInt();
            Collection<TestFunction> collection = entry.getValue();
            return Streams.stream(Iterables.partition(collection, 100)).map((testFunctions) -> {
                return new GameTestBatch(string + ":" + mutableInt.incrementAndGet(), ImmutableList.copyOf(testFunctions), consumer, consumer2);
            });
        }).collect(ImmutableList.toImmutableList());
    }

    public static void clearAllTests(ServerLevel world, BlockPos pos, GameTestTicker testManager, int radius) {
        testManager.clear();
        BlockPos blockPos = pos.offset(-radius, 0, -radius);
        BlockPos blockPos2 = pos.offset(radius, 0, radius);
        BlockPos.betweenClosedStream(blockPos, blockPos2).filter((posx) -> {
            return world.getBlockState(posx).is(Blocks.STRUCTURE_BLOCK);
        }).forEach((posx) -> {
            StructureBlockEntity structureBlockEntity = (StructureBlockEntity)world.getBlockEntity(posx);
            BlockPos blockPos = structureBlockEntity.getBlockPos();
            BoundingBox boundingBox = StructureUtils.getStructureBoundingBox(structureBlockEntity);
            StructureUtils.clearSpaceForStructure(boundingBox, blockPos.getY(), world);
        });
    }

    public static void clearMarkers(ServerLevel world) {
        DebugPackets.sendGameTestClearPacket(world);
    }
}
