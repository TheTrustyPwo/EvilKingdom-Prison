package net.minecraft.network.chat;

import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;

public interface MutableComponent extends Component {
    MutableComponent setStyle(Style style);

    default MutableComponent append(String text) {
        return this.append(new TextComponent(text));
    }

    MutableComponent append(Component text);

    default MutableComponent withStyle(UnaryOperator<Style> styleUpdater) {
        this.setStyle(styleUpdater.apply(this.getStyle()));
        return this;
    }

    default MutableComponent withStyle(Style styleOverride) {
        this.setStyle(styleOverride.applyTo(this.getStyle()));
        return this;
    }

    default MutableComponent withStyle(ChatFormatting... formattings) {
        this.setStyle(this.getStyle().applyFormats(formattings));
        return this;
    }

    default MutableComponent withStyle(ChatFormatting formatting) {
        this.setStyle(this.getStyle().applyFormat(formatting));
        return this;
    }
}
