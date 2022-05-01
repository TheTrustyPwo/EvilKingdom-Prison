package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class GameTestBatchRunner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockPos firstTestNorthWestCorner;
    final ServerLevel level;
    private final GameTestTicker testTicker;
    private final int testsPerRow;
    private final List<GameTestInfo> allTestInfos;
    private final List<Pair<GameTestBatch, Collection<GameTestInfo>>> batches;
    private final BlockPos.MutableBlockPos nextTestNorthWestCorner;

    public GameTestBatchRunner(Collection<GameTestBatch> batches, BlockPos pos, Rotation rotation, ServerLevel world, GameTestTicker testManager, int sizeZ) {
        this.nextTestNorthWestCorner = pos.mutable();
        this.firstTestNorthWestCorner = pos;
        this.level = world;
        this.testTicker = testManager;
        this.testsPerRow = sizeZ;
        this.batches = batches.stream().map((batch) -> {
            Collection<GameTestInfo> collection = batch.getTestFunctions().stream().map((testFunction) -> {
                return new GameTestInfo(testFunction, rotation, world);
            }).collect(ImmutableList.toImmutableList());
            return Pair.of(batch, collection);
        }).collect(ImmutableList.toImmutableList());
        this.allTestInfos = this.batches.stream().flatMap((batch) -> {
            return batch.getSecond().stream();
        }).collect(ImmutableList.toImmutableList());
    }

    public List<GameTestInfo> getTestInfos() {
        return this.allTestInfos;
    }

    public void start() {
        this.runBatch(0);
    }

    void runBatch(int index) {
        if (index < this.batches.size()) {
            Pair<GameTestBatch, Collection<GameTestInfo>> pair = this.batches.get(index);
            final GameTestBatch gameTestBatch = pair.getFirst();
            Collection<GameTestInfo> collection = pair.getSecond();
            Map<GameTestInfo, BlockPos> map = this.createStructuresForBatch(collection);
            String string = gameTestBatch.getName();
            LOGGER.info("Running test batch '{}' ({} tests)...", string, collection.size());
            gameTestBatch.runBeforeBatchFunction(this.level);
            final MultipleTestTracker multipleTestTracker = new MultipleTestTracker();
            collection.forEach(multipleTestTracker::addTestToTrack);
            multipleTestTracker.addListener(new GameTestListener() {
                private void testCompleted() {
                    if (multipleTestTracker.isDone()) {
                        gameTestBatch.runAfterBatchFunction(GameTestBatchRunner.this.level);
                        GameTestBatchRunner.this.runBatch(index + 1);
                    }

                }

                @Override
                public void testStructureLoaded(GameTestInfo test) {
                }

                @Override
                public void testPassed(GameTestInfo test) {
                    this.testCompleted();
                }

                @Override
                public void testFailed(GameTestInfo test) {
                    this.testCompleted();
                }
            });
            collection.forEach((gameTest) -> {
                BlockPos blockPos = map.get(gameTest);
                GameTestRunner.runTest(gameTest, blockPos, this.testTicker);
            });
        }
    }

    private Map<GameTestInfo, BlockPos> createStructuresForBatch(Collection<GameTestInfo> gameTests) {
        Map<GameTestInfo, BlockPos> map = Maps.newHashMap();
        int i = 0;
        AABB aABB = new AABB(this.nextTestNorthWestCorner);

        for(GameTestInfo gameTestInfo : gameTests) {
            BlockPos blockPos = new BlockPos(this.nextTestNorthWestCorner);
            StructureBlockEntity structureBlockEntity = StructureUtils.spawnStructure(gameTestInfo.getStructureName(), blockPos, gameTestInfo.getRotation(), 2, this.level, true);
            AABB aABB2 = StructureUtils.getStructureBounds(structureBlockEntity);
            gameTestInfo.setStructureBlockPos(structureBlockEntity.getBlockPos());
            map.put(gameTestInfo, new BlockPos(this.nextTestNorthWestCorner));
            aABB = aABB.minmax(aABB2);
            this.nextTestNorthWestCorner.move((int)aABB2.getXsize() + 5, 0, 0);
            if (i++ % this.testsPerRow == this.testsPerRow - 1) {
                this.nextTestNorthWestCorner.move(0, 0, (int)aABB.getZsize() + 6);
                this.nextTestNorthWestCorner.setX(this.firstTestNorthWestCorner.getX());
                aABB = new AABB(this.nextTestNorthWestCorner);
            }
        }

        return map;
    }
}
