package net.minecraft.network.chat;

public class TranslatableFormatException extends IllegalArgumentException {
    public TranslatableFormatException(TranslatableComponent text, String message) {
        super(String.format("Error parsing: %s: %s", text, message));
    }

    public TranslatableFormatException(TranslatableComponent text, int index) {
        super(String.format("Invalid index %d requested for %s", index, text));
    }

    public TranslatableFormatException(TranslatableComponent text, Throwable cause) {
        super(String.format("Error while parsing: %s", text), cause);
    }
}
