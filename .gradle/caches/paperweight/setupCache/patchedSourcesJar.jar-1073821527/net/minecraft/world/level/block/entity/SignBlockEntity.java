package net.minecraft.world.level.block.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SignBlockEntity extends BlockEntity {
    public static final int LINES = 4;
    private static final String[] RAW_TEXT_FIELD_NAMES = new String[]{"Text1", "Text2", "Text3", "Text4"};
    private static final String[] FILTERED_TEXT_FIELD_NAMES = new String[]{"FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4"};
    public final Component[] messages = new Component[]{TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY};
    private final Component[] filteredMessages = new Component[]{TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY};
    public boolean isEditable = true;
    @Nullable
    private UUID playerWhoMayEdit;
    @Nullable
    private FormattedCharSequence[] renderMessages;
    private boolean renderMessagedFiltered;
    private DyeColor color = DyeColor.BLACK;
    private boolean hasGlowingText;

    public SignBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.SIGN, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);

        for(int i = 0; i < 4; ++i) {
            Component component = this.messages[i];
            String string = Component.Serializer.toJson(component);
            nbt.putString(RAW_TEXT_FIELD_NAMES[i], string);
            Component component2 = this.filteredMessages[i];
            if (!component2.equals(component)) {
                nbt.putString(FILTERED_TEXT_FIELD_NAMES[i], Component.Serializer.toJson(component2));
            }
        }

        nbt.putString("Color", this.color.getName());
        nbt.putBoolean("GlowingText", this.hasGlowingText);
    }

    @Override
    public void load(CompoundTag nbt) {
        this.isEditable = false;
        super.load(nbt);
        this.color = DyeColor.byName(nbt.getString("Color"), DyeColor.BLACK);

        for(int i = 0; i < 4; ++i) {
            String string = nbt.getString(RAW_TEXT_FIELD_NAMES[i]);
            Component component = this.loadLine(string);
            this.messages[i] = component;
            String string2 = FILTERED_TEXT_FIELD_NAMES[i];
            if (nbt.contains(string2, 8)) {
                this.filteredMessages[i] = this.loadLine(nbt.getString(string2));
            } else {
                this.filteredMessages[i] = component;
            }
        }

        this.renderMessages = null;
        this.hasGlowingText = nbt.getBoolean("GlowingText");
    }

    private Component loadLine(String json) {
        Component component = this.deserializeTextSafe(json);
        if (this.level instanceof ServerLevel) {
            try {
                return ComponentUtils.updateForEntity(this.createCommandSourceStack((ServerPlayer)null), component, (Entity)null, 0);
            } catch (CommandSyntaxException var4) {
            }
        }

        return component;
    }

    private Component deserializeTextSafe(String json) {
        try {
            Component component = Component.Serializer.fromJson(json);
            if (component != null) {
                return component;
            }
        } catch (Exception var3) {
        }

        return TextComponent.EMPTY;
    }

    public Component getMessage(int row, boolean filtered) {
        return this.getMessages(filtered)[row];
    }

    public void setMessage(int row, Component text) {
        this.setMessage(row, text, text);
    }

    public void setMessage(int row, Component text, Component filteredText) {
        this.messages[row] = text;
        this.filteredMessages[row] = filteredText;
        this.renderMessages = null;
    }

    public FormattedCharSequence[] getRenderMessages(boolean filterText, Function<Component, FormattedCharSequence> textOrderingFunction) {
        if (this.renderMessages == null || this.renderMessagedFiltered != filterText) {
            this.renderMessagedFiltered = filterText;
            this.renderMessages = new FormattedCharSequence[4];

            for(int i = 0; i < 4; ++i) {
                this.renderMessages[i] = textOrderingFunction.apply(this.getMessage(i, filterText));
            }
        }

        return this.renderMessages;
    }

    private Component[] getMessages(boolean filtered) {
        return filtered ? this.filteredMessages : this.messages;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }

    public boolean isEditable() {
        return this.isEditable;
    }

    public void setEditable(boolean editable) {
        this.isEditable = editable;
        if (!editable) {
            this.playerWhoMayEdit = null;
        }

    }

    public void setAllowedPlayerEditor(UUID editor) {
        this.playerWhoMayEdit = editor;
    }

    @Nullable
    public UUID getPlayerWhoMayEdit() {
        return this.playerWhoMayEdit;
    }

    public boolean executeClickCommands(ServerPlayer player) {
        for(Component component : this.getMessages(player.isTextFilteringEnabled())) {
            Style style = component.getStyle();
            ClickEvent clickEvent = style.getClickEvent();
            if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                player.getServer().getCommands().performCommand(this.createCommandSourceStack(player), clickEvent.getValue());
            }
        }

        return true;
    }

    public CommandSourceStack createCommandSourceStack(@Nullable ServerPlayer player) {
        String string = player == null ? "Sign" : player.getName().getString();
        Component component = (Component)(player == null ? new TextComponent("Sign") : player.getDisplayName());
        return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(this.worldPosition), Vec2.ZERO, (ServerLevel)this.level, 2, string, component, this.level.getServer(), player);
    }

    public DyeColor getColor() {
        return this.color;
    }

    public boolean setColor(DyeColor value) {
        if (value != this.getColor()) {
            this.color = value;
            this.markUpdated();
            return true;
        } else {
            return false;
        }
    }

    public boolean hasGlowingText() {
        return this.hasGlowingText;
    }

    public boolean setHasGlowingText(boolean glowingText) {
        if (this.hasGlowingText != glowingText) {
            this.hasGlowingText = glowingText;
            this.markUpdated();
            return true;
        } else {
            return false;
        }
    }

    private void markUpdated() {
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }
}
