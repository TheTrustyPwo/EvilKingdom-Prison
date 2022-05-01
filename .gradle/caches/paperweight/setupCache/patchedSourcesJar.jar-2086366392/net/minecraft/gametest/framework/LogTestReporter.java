package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import org.slf4j.Logger;

public class LogTestReporter implements TestReporter {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onTestFailed(GameTestInfo test) {
        if (test.isRequired()) {
            LOGGER.error("{} failed! {}", test.getTestName(), Util.describeError(test.getError()));
        } else {
            LOGGER.warn("(optional) {} failed. {}", test.getTestName(), Util.describeError(test.getError()));
        }

    }

    @Override
    public void onTestSuccess(GameTestInfo test) {
    }
}
