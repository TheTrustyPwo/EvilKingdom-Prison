package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundEditBookPacket implements Packet<ServerGamePacketListener> {
    public static final int MAX_BYTES_PER_CHAR = 4;
    private static final int TITLE_MAX_CHARS = 128;
    private static final int PAGE_MAX_CHARS = 8192;
    private static final int MAX_PAGES_COUNT = 200;
    private final int slot;
    private final List<String> pages;
    private final Optional<String> title;

    public ServerboundEditBookPacket(int slot, List<String> pages, Optional<String> title) {
        this.slot = slot;
        this.pages = ImmutableList.copyOf(pages);
        this.title = title;
    }

    public ServerboundEditBookPacket(FriendlyByteBuf buf) {
        this.slot = buf.readVarInt();
        this.pages = buf.readCollection(FriendlyByteBuf.limitValue(Lists::newArrayListWithCapacity, 200), (bufx) -> {
            return bufx.readUtf(8192);
        });
        this.title = buf.readOptional((bufx) -> {
            return bufx.readUtf(128);
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.slot);
        buf.writeCollection(this.pages, (bufx, page) -> {
            bufx.writeUtf(page, 8192);
        });
        buf.writeOptional(this.title, (bufx, title) -> {
            bufx.writeUtf(title, 128);
        });
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleEditBook(this);
    }

    public List<String> getPages() {
        return this.pages;
    }

    public Optional<String> getTitle() {
        return this.title;
    }

    public int getSlot() {
        return this.slot;
    }
}
