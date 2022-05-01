package net.minecraft.util;

import net.minecraft.network.chat.Component;

public interface ProgressListener {
    void progressStartNoAbort(Component title);

    void progressStart(Component title);

    void progressStage(Component task);

    void progressStagePercentage(int percentage);

    void stop();
}
