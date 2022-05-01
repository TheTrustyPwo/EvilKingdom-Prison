package net.minecraft.world;

public class InteractionResultHolder<T> {
    private final InteractionResult result;
    private final T object;

    public InteractionResultHolder(InteractionResult result, T value) {
        this.result = result;
        this.object = value;
    }

    public InteractionResult getResult() {
        return this.result;
    }

    public T getObject() {
        return this.object;
    }

    public static <T> InteractionResultHolder<T> success(T data) {
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, data);
    }

    public static <T> InteractionResultHolder<T> consume(T data) {
        return new InteractionResultHolder<>(InteractionResult.CONSUME, data);
    }

    public static <T> InteractionResultHolder<T> pass(T data) {
        return new InteractionResultHolder<>(InteractionResult.PASS, data);
    }

    public static <T> InteractionResultHolder<T> fail(T data) {
        return new InteractionResultHolder<>(InteractionResult.FAIL, data);
    }

    public static <T> InteractionResultHolder<T> sidedSuccess(T data, boolean swingHand) {
        return swingHand ? success(data) : consume(data);
    }
}
