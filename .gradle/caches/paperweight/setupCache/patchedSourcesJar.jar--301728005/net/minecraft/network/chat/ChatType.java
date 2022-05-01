package net.minecraft.network.chat;

public enum ChatType {
    CHAT((byte)0, false),
    SYSTEM((byte)1, true),
    GAME_INFO((byte)2, true);

    private final byte index;
    private final boolean interrupt;

    private ChatType(byte id, boolean interruptsNarration) {
        this.index = id;
        this.interrupt = interruptsNarration;
    }

    public byte getIndex() {
        return this.index;
    }

    public static ChatType getForIndex(byte id) {
        for(ChatType chatType : values()) {
            if (id == chatType.index) {
                return chatType;
            }
        }

        return CHAT;
    }

    public boolean shouldInterrupt() {
        return this.interrupt;
    }
}
