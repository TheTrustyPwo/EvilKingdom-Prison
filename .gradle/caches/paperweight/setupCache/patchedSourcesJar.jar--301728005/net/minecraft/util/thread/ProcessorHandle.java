package net.minecraft.util.thread;

import com.mojang.datafixers.util.Either;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ProcessorHandle<Msg> extends AutoCloseable {
    String name();

    void tell(Msg message);

    @Override
    default void close() {
    }

    default <Source> CompletableFuture<Source> ask(Function<? super ProcessorHandle<Source>, ? extends Msg> messageProvider) {
        CompletableFuture<Source> completableFuture = new CompletableFuture<>();
        Msg object = messageProvider.apply(of("ask future procesor handle", completableFuture::complete));
        this.tell(object);
        return completableFuture;
    }

    default <Source> CompletableFuture<Source> askEither(Function<? super ProcessorHandle<Either<Source, Exception>>, ? extends Msg> messageProvider) {
        CompletableFuture<Source> completableFuture = new CompletableFuture<>();
        Msg object = messageProvider.apply(of("ask future procesor handle", (either) -> {
            either.ifLeft(completableFuture::complete);
            either.ifRight(completableFuture::completeExceptionally);
        }));
        this.tell(object);
        return completableFuture;
    }

    static <Msg> ProcessorHandle<Msg> of(String name, Consumer<Msg> action) {
        return new ProcessorHandle<Msg>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void tell(Msg message) {
                action.accept(message);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
