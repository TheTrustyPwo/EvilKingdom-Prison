package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
    private final GameTestInfo originalTestInfo;
    private final GameTestTicker testTicker;
    private final BlockPos structurePos;
    int attempts;
    int successes;

    public ReportGameListener(GameTestInfo test, GameTestTicker testManager, BlockPos pos) {
        this.originalTestInfo = test;
        this.testTicker = testManager;
        this.structurePos = pos;
        this.attempts = 0;
        this.successes = 0;
    }

    @Override
    public void testStructureLoaded(GameTestInfo test) {
        spawnBeacon(this.originalTestInfo, Blocks.LIGHT_GRAY_STAINED_GLASS);
        ++this.attempts;
    }

    @Override
    public void testPassed(GameTestInfo test) {
        ++this.successes;
        if (!test.isFlaky()) {
            reportPassed(test, test.getTestName() + " passed!");
        } else {
            if (this.successes >= test.requiredSuccesses()) {
                reportPassed(test, test + " passed " + this.successes + " times of " + this.attempts + " attempts.");
            } else {
                say(this.originalTestInfo.getLevel(), ChatFormatting.GREEN, "Flaky test " + this.originalTestInfo + " succeeded, attempt: " + this.attempts + " successes: " + this.successes);
                this.rerunTest();
            }

        }
    }

    @Override
    public void testFailed(GameTestInfo test) {
        if (!test.isFlaky()) {
            reportFailure(test, test.getError());
        } else {
            TestFunction testFunction = this.originalTestInfo.getTestFunction();
            String string = "Flaky test " + this.originalTestInfo + " failed, attempt: " + this.attempts + "/" + testFunction.getMaxAttempts();
            if (testFunction.getRequiredSuccesses() > 1) {
                string = string + ", successes: " + this.successes + " (" + testFunction.getRequiredSuccesses() + " required)";
            }

            say(this.originalTestInfo.getLevel(), ChatFormatting.YELLOW, string);
            if (test.maxAttempts() - this.attempts + this.successes >= test.requiredSuccesses()) {
                this.rerunTest();
            } else {
                reportFailure(test, new ExhaustedAttemptsException(this.attempts, this.successes, test));
            }

        }
    }

    public static void reportPassed(GameTestInfo test, String output) {
        spawnBeacon(test, Blocks.LIME_STAINED_GLASS);
        visualizePassedTest(test, output);
    }

    private static void visualizePassedTest(GameTestInfo test, String output) {
        say(test.getLevel(), ChatFormatting.GREEN, output);
        GlobalTestReporter.onTestSuccess(test);
    }

    protected static void reportFailure(GameTestInfo test, Throwable output) {
        spawnBeacon(test, test.isRequired() ? Blocks.RED_STAINED_GLASS : Blocks.ORANGE_STAINED_GLASS);
        spawnLectern(test, Util.describeError(output));
        visualizeFailedTest(test, output);
    }

    protected static void visualizeFailedTest(GameTestInfo test, Throwable output) {
        String string = output.getMessage() + (output.getCause() == null ? "" : " cause: " + Util.describeError(output.getCause()));
        String string2 = (test.isRequired() ? "" : "(optional) ") + test.getTestName() + " failed! " + string;
        say(test.getLevel(), test.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, string2);
        Throwable throwable = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(output), output);
        if (throwable instanceof GameTestAssertPosException) {
            GameTestAssertPosException gameTestAssertPosException = (GameTestAssertPosException)throwable;
            showRedBox(test.getLevel(), gameTestAssertPosException.getAbsolutePos(), gameTestAssertPosException.getMessageToShowAtBlock());
        }

        GlobalTestReporter.onTestFailed(test);
    }

    private void rerunTest() {
        this.originalTestInfo.clearStructure();
        GameTestInfo gameTestInfo = new GameTestInfo(this.originalTestInfo.getTestFunction(), this.originalTestInfo.getRotation(), this.originalTestInfo.getLevel());
        gameTestInfo.startExecution();
        this.testTicker.add(gameTestInfo);
        gameTestInfo.addListener(this);
        gameTestInfo.spawnStructure(this.structurePos, 2);
    }

    protected static void spawnBeacon(GameTestInfo test, Block block) {
        ServerLevel serverLevel = test.getLevel();
        BlockPos blockPos = test.getStructureBlockPos();
        BlockPos blockPos2 = new BlockPos(-1, -1, -1);
        BlockPos blockPos3 = StructureTemplate.transform(blockPos.offset(blockPos2), Mirror.NONE, test.getRotation(), blockPos);
        serverLevel.setBlockAndUpdate(blockPos3, Blocks.BEACON.defaultBlockState().rotate(test.getRotation()));
        BlockPos blockPos4 = blockPos3.offset(0, 1, 0);
        serverLevel.setBlockAndUpdate(blockPos4, block.defaultBlockState());

        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                BlockPos blockPos5 = blockPos3.offset(i, -1, j);
                serverLevel.setBlockAndUpdate(blockPos5, Blocks.IRON_BLOCK.defaultBlockState());
            }
        }

    }

    private static void spawnLectern(GameTestInfo test, String output) {
        ServerLevel serverLevel = test.getLevel();
        BlockPos blockPos = test.getStructureBlockPos();
        BlockPos blockPos2 = new BlockPos(-1, 1, -1);
        BlockPos blockPos3 = StructureTemplate.transform(blockPos.offset(blockPos2), Mirror.NONE, test.getRotation(), blockPos);
        serverLevel.setBlockAndUpdate(blockPos3, Blocks.LECTERN.defaultBlockState().rotate(test.getRotation()));
        BlockState blockState = serverLevel.getBlockState(blockPos3);
        ItemStack itemStack = createBook(test.getTestName(), test.isRequired(), output);
        LecternBlock.tryPlaceBook((Player)null, serverLevel, blockPos3, blockState, itemStack);
    }

    private static ItemStack createBook(String text, boolean required, String output) {
        ItemStack itemStack = new ItemStack(Items.WRITABLE_BOOK);
        ListTag listTag = new ListTag();
        StringBuffer stringBuffer = new StringBuffer();
        Arrays.stream(text.split("\\.")).forEach((line) -> {
            stringBuffer.append(line).append('\n');
        });
        if (!required) {
            stringBuffer.append("(optional)\n");
        }

        stringBuffer.append("-------------------\n");
        listTag.add(StringTag.valueOf(stringBuffer + output));
        itemStack.addTagElement("pages", listTag);
        return itemStack;
    }

    protected static void say(ServerLevel world, ChatFormatting formatting, String message) {
        world.getPlayers((player) -> {
            return true;
        }).forEach((player) -> {
            player.sendMessage((new TextComponent(message)).withStyle(formatting), Util.NIL_UUID);
        });
    }

    private static void showRedBox(ServerLevel world, BlockPos pos, String message) {
        DebugPackets.sendGameTestAddMarker(world, pos, message, -2130771968, Integer.MAX_VALUE);
    }
}
