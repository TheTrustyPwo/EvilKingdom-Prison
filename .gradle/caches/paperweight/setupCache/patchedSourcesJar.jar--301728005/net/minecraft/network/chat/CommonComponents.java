package net.minecraft.network.chat;

import java.util.Arrays;
import java.util.Collection;

public class CommonComponents {
    public static final Component OPTION_ON = new TranslatableComponent("options.on");
    public static final Component OPTION_OFF = new TranslatableComponent("options.off");
    public static final Component GUI_DONE = new TranslatableComponent("gui.done");
    public static final Component GUI_CANCEL = new TranslatableComponent("gui.cancel");
    public static final Component GUI_YES = new TranslatableComponent("gui.yes");
    public static final Component GUI_NO = new TranslatableComponent("gui.no");
    public static final Component GUI_PROCEED = new TranslatableComponent("gui.proceed");
    public static final Component GUI_BACK = new TranslatableComponent("gui.back");
    public static final Component CONNECT_FAILED = new TranslatableComponent("connect.failed");
    public static final Component NEW_LINE = new TextComponent("\n");
    public static final Component NARRATION_SEPARATOR = new TextComponent(". ");

    public static Component optionStatus(boolean on) {
        return on ? OPTION_ON : OPTION_OFF;
    }

    public static MutableComponent optionStatus(Component text, boolean value) {
        return new TranslatableComponent(value ? "options.on.composed" : "options.off.composed", text);
    }

    public static MutableComponent optionNameValue(Component text, Component value) {
        return new TranslatableComponent("options.generic_value", text, value);
    }

    public static MutableComponent joinForNarration(Component first, Component second) {
        return (new TextComponent("")).append(first).append(NARRATION_SEPARATOR).append(second);
    }

    public static Component joinLines(Component... texts) {
        return joinLines(Arrays.asList(texts));
    }

    public static Component joinLines(Collection<? extends Component> texts) {
        return ComponentUtils.formatList(texts, NEW_LINE);
    }
}
