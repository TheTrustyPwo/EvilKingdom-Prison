package net.minecraft.gametest.framework;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import org.slf4j.Logger;

public class TeamcityTestReporter implements TestReporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Escaper ESCAPER = Escapers.builder().addEscape('\'', "|'").addEscape('\n', "|n").addEscape('\r', "|r").addEscape('|', "||").addEscape('[', "|[").addEscape(']', "|]").build();

    @Override
    public void onTestFailed(GameTestInfo test) {
        String string = ESCAPER.escape(test.getTestName());
        String string2 = ESCAPER.escape(test.getError().getMessage());
        String string3 = ESCAPER.escape(Util.describeError(test.getError()));
        LOGGER.info("##teamcity[testStarted name='{}']", (Object)string);
        if (test.isRequired()) {
            LOGGER.info("##teamcity[testFailed name='{}' message='{}' details='{}']", string, string2, string3);
        } else {
            LOGGER.info("##teamcity[testIgnored name='{}' message='{}' details='{}']", string, string2, string3);
        }

        LOGGER.info("##teamcity[testFinished name='{}' duration='{}']", string, test.getRunTime());
    }

    @Override
    public void onTestSuccess(GameTestInfo test) {
        String string = ESCAPER.escape(test.getTestName());
        LOGGER.info("##teamcity[testStarted name='{}']", (Object)string);
        LOGGER.info("##teamcity[testFinished name='{}' duration='{}']", string, test.getRunTime());
    }
}
