package net.minecraft.world.inventory;

public abstract class DataSlot {
    private int prevValue;

    public static DataSlot forContainer(ContainerData delegate, int index) {
        return new DataSlot() {
            @Override
            public int get() {
                return delegate.get(index);
            }

            @Override
            public void set(int value) {
                delegate.set(index, value);
            }
        };
    }

    public static DataSlot shared(int[] array, int index) {
        return new DataSlot() {
            @Override
            public int get() {
                return array[index];
            }

            @Override
            public void set(int value) {
                array[index] = value;
            }
        };
    }

    public static DataSlot standalone() {
        return new DataSlot() {
            private int value;

            @Override
            public int get() {
                return this.value;
            }

            @Override
            public void set(int value) {
                this.value = value;
            }
        };
    }

    public abstract int get();

    public abstract void set(int value);

    public boolean checkAndClearUpdateFlag() {
        int i = this.get();
        boolean bl = i != this.prevValue;
        this.prevValue = i;
        return bl;
    }
}
