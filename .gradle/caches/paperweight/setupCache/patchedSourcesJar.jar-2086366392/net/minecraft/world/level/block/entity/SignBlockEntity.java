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
import net.minecraft.network.chat.MutableComponent;
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

public class SignBlockEntity extends BlockEntity implements CommandSource { // CraftBukkit - implements
    private static final boolean CONVERT_LEGACY_SIGNS = Boolean.getBoolean("convertLegacySigns"); // Paper

    public static final int LINES = 4;
    private static final String[] RAW_TEXT_FIELD_NAMES = new String[]{"Text1", "Text2", "Text3", "Text4"};
    private static final String[] FILTERED_TEXT_FIELD_NAMES = new String[]{"FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4"};
    public final Component[] messages;
    private final Component[] filteredMessages;
    public boolean isEditable;
    @Nullable
    private UUID playerWhoMayEdit;
    @Nullable
    private FormattedCharSequence[] renderMessages;
    private boolean renderMessagedFiltered;
    private DyeColor color;
    private boolean hasGlowingText;
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    public SignBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.SIGN, pos, state);
        this.messages = new Component[]{TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY};
        this.filteredMessages = new Component[]{TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY};
        this.isEditable = true;
        this.color = DyeColor.BLACK;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);

        for (int i = 0; i < 4; ++i) {
            Component ichatbasecomponent = this.messages[i];
            String s = Component.Serializer.toJson(ichatbasecomponent);

            nbt.putString(SignBlockEntity.RAW_TEXT_FIELD_NAMES[i], s);
            Component ichatbasecomponent1 = this.filteredMessages[i];

            if (!ichatbasecomponent1.equals(ichatbasecomponent)) {
                nbt.putString(SignBlockEntity.FILTERED_TEXT_FIELD_NAMES[i], Component.Serializer.toJson(ichatbasecomponent1));
            }
        }

        // CraftBukkit start
        if (CONVERT_LEGACY_SIGNS) { // Paper
            nbt.putBoolean("Bukkit.isConverted", true);
        }
        // CraftBukkit end

        nbt.putString("Color", this.color.getName());
        nbt.putBoolean("GlowingText", this.hasGlowingText);
    }

    @Override
    public void load(CompoundTag nbt) {
        this.isEditable = false;
        super.load(nbt);
        this.color = DyeColor.byName(nbt.getString("Color"), DyeColor.BLACK);

        // CraftBukkit start - Add an option to convert signs correctly
        // This is done with a flag instead of all the time because
        // we have no way to tell whether a sign is from 1.7.10 or 1.8
        boolean oldSign = Boolean.getBoolean("convertLegacySigns") && !nbt.getBoolean("Bukkit.isConverted");
        // CraftBukkit end

        for (int i = 0; i < 4; ++i) {
            String s = nbt.getString(SignBlockEntity.RAW_TEXT_FIELD_NAMES[i]);
            // CraftBukkit start
            if (s != null && s.length() > 2048) {
                s = "\"\"";
            }

            if (oldSign && !this.isLoadingStructure) { // Paper - saved structures will be in the new format, but will not have isConverted
                this.messages[i] = org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage.fromString(s)[0];
                continue;
            }
            // CraftBukkit end
            Component ichatbasecomponent = this.loadLine(s);

            this.messages[i] = ichatbasecomponent;
            String s1 = SignBlockEntity.FILTERED_TEXT_FIELD_NAMES[i];

            if (nbt.contains(s1, 8)) {
                this.filteredMessages[i] = this.loadLine(nbt.getString(s1));
            } else {
                this.filteredMessages[i] = ichatbasecomponent;
            }
        }

        this.renderMessages = null;
        this.hasGlowingText = nbt.getBoolean("GlowingText");
    }

    private Component loadLine(String json) {
        Component ichatbasecomponent = this.deserializeTextSafe(json);

        if (this.level instanceof ServerLevel) {
            try {
                return ComponentUtils.updateForEntity(this.createCommandSourceStack((ServerPlayer) null), ichatbasecomponent, (Entity) null, 0);
            } catch (CommandSyntaxException commandsyntaxexception) {
                ;
            }
        }

        return ichatbasecomponent;
    }

    private Component deserializeTextSafe(String json) {
        try {
            MutableComponent ichatmutablecomponent = Component.Serializer.fromJson(json);

            if (ichatmutablecomponent != null) {
                return ichatmutablecomponent;
            }
            // CraftBukkit start
        } catch (com.google.gson.JsonParseException jsonparseexception) {
            return new TextComponent(json);
            // CraftBukkit end
        } catch (Exception exception) {
            ;
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

            for (int i = 0; i < 4; ++i) {
                this.renderMessages[i] = (FormattedCharSequence) textOrderingFunction.apply(this.getMessage(i, filterText));
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
        Component[] aichatbasecomponent = this.getMessages(player.isTextFilteringEnabled());
        int i = aichatbasecomponent.length;

        for (int j = 0; j < i; ++j) {
            Component ichatbasecomponent = aichatbasecomponent[j];
            Style chatmodifier = ichatbasecomponent.getStyle();
            ClickEvent chatclickable = chatmodifier.getClickEvent();

            if (chatclickable != null && chatclickable.getAction() == ClickEvent.Action.RUN_COMMAND) {
                // Paper start
                String command = chatclickable.getValue().startsWith("/") ? chatclickable.getValue() : "/" + chatclickable.getValue();
                if (org.spigotmc.SpigotConfig.logCommands)  {
                    LOGGER.info("{} issued server command: {}", player.getScoreboardName(), command);
                }
                io.papermc.paper.event.player.PlayerSignCommandPreprocessEvent event = new io.papermc.paper.event.player.PlayerSignCommandPreprocessEvent(player.getBukkitEntity(), command, new org.bukkit.craftbukkit.v1_18_R2.util.LazyPlayerSet(player.getServer()), (org.bukkit.block.Sign) net.minecraft.server.MCUtil.toBukkitBlock(this.level, this.worldPosition).getState());
                if (!event.callEvent()) {
                    return false;
                }
                player.getServer().getCommands().performCommand(this.createCommandSourceStack(((org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer) event.getPlayer()).getHandle()), event.getMessage());
                // Paper end
            }
        }

        return true;
    }

    // CraftBukkit start
    @Override
    public void sendMessage(Component message, java.util.UUID sender) {}

    @Override
    public org.bukkit.command.CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return wrapper.getEntity() != null ? wrapper.getEntity().getBukkitSender(wrapper) : new org.bukkit.craftbukkit.v1_18_R2.command.CraftBlockCommandSender(wrapper, this);
    }

    @Override
    public boolean acceptsSuccess() {
        return false;
    }

    @Override
    public boolean acceptsFailure() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }
    // CraftBukkit end

    public CommandSourceStack createCommandSourceStack(@Nullable ServerPlayer player) {
        String s = player == null ? "Sign" : player.getName().getString();
        Object object = player == null ? new TextComponent("Sign") : player.getDisplayName();

        // Paper start - send messages back to the player
        CommandSource commandSource = this.level.paperConfig.showSignClickCommandFailureMessagesToPlayer ? new io.papermc.paper.commands.DelegatingCommandSource(this) {
            @Override
            public void sendMessage(Component message, UUID sender) {
                player.sendMessage(message, sender);
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }
        } : this;
        // Paper end
        // CraftBukkit - this
        return new CommandSourceStack(commandSource, Vec3.atCenterOf(this.worldPosition), Vec2.ZERO, (ServerLevel) this.level, 2, s, (Component) object, this.level.getServer(), player); // Paper
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
        if (this.level != null) this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3); // CraftBukkit - skip notify if world is null (SPIGOT-5122)
    }
}
