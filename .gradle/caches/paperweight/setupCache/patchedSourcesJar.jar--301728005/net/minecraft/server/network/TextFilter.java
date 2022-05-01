package net.minecraft.server.network;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TextFilter {
    TextFilter DUMMY = new TextFilter() {
        @Override
        public void join() {
        }

        @Override
        public void leave() {
        }

        @Override
        public CompletableFuture<TextFilter.FilteredText> processStreamMessage(String text) {
            return CompletableFuture.completedFuture(TextFilter.FilteredText.passThrough(text));
        }

        @Override
        public CompletableFuture<List<TextFilter.FilteredText>> processMessageBundle(List<String> texts) {
            return CompletableFuture.completedFuture(texts.stream().map(TextFilter.FilteredText::passThrough).collect(ImmutableList.toImmutableList()));
        }
    };

    void join();

    void leave();

    CompletableFuture<TextFilter.FilteredText> processStreamMessage(String text);

    CompletableFuture<List<TextFilter.FilteredText>> processMessageBundle(List<String> texts);

    public static class FilteredText {
        public static final TextFilter.FilteredText EMPTY = new TextFilter.FilteredText("", "");
        private final String raw;
        private final String filtered;

        public FilteredText(String raw, String filtered) {
            this.raw = raw;
            this.filtered = filtered;
        }

        public String getRaw() {
            return this.raw;
        }

        public String getFiltered() {
            return this.filtered;
        }

        public static TextFilter.FilteredText passThrough(String text) {
            return new TextFilter.FilteredText(text, text);
        }

        public static TextFilter.FilteredText fullyFiltered(String raw) {
            return new TextFilter.FilteredText(raw, "");
        }
    }
}
