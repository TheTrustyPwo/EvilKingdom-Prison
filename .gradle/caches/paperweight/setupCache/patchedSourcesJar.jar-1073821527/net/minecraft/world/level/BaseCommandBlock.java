package net.minecraft.world.level;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public abstract class BaseCommandBlock implements CommandSource {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final Component DEFAULT_NAME = new TextComponent("@");
    private long lastExecution = -1L;
    private boolean updateLastExecution = true;
    private int successCount;
    private boolean trackOutput = true;
    @Nullable
    private Component lastOutput;
    private String command = "";
    private Component name = DEFAULT_NAME;

    public int getSuccessCount() {
        return this.successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public Component getLastOutput() {
        return this.lastOutput == null ? TextComponent.EMPTY : this.lastOutput;
    }

    public CompoundTag save(CompoundTag nbt) {
        nbt.putString("Command", this.command);
        nbt.putInt("SuccessCount", this.successCount);
        nbt.putString("CustomName", Component.Serializer.toJson(this.name));
        nbt.putBoolean("TrackOutput", this.trackOutput);
        if (this.lastOutput != null && this.trackOutput) {
            nbt.putString("LastOutput", Component.Serializer.toJson(this.lastOutput));
        }

        nbt.putBoolean("UpdateLastExecution", this.updateLastExecution);
        if (this.updateLastExecution && this.lastExecution > 0L) {
            nbt.putLong("LastExecution", this.lastExecution);
        }

        return nbt;
    }

    public void load(CompoundTag nbt) {
        this.command = nbt.getString("Command");
        this.successCount = nbt.getInt("SuccessCount");
        if (nbt.contains("CustomName", 8)) {
            this.setName(Component.Serializer.fromJson(nbt.getString("CustomName")));
        }

        if (nbt.contains("TrackOutput", 1)) {
            this.trackOutput = nbt.getBoolean("TrackOutput");
        }

        if (nbt.contains("LastOutput", 8) && this.trackOutput) {
            try {
                this.lastOutput = Component.Serializer.fromJson(nbt.getString("LastOutput"));
            } catch (Throwable var3) {
                this.lastOutput = new TextComponent(var3.getMessage());
            }
        } else {
            this.lastOutput = null;
        }

        if (nbt.contains("UpdateLastExecution")) {
            this.updateLastExecution = nbt.getBoolean("UpdateLastExecution");
        }

        if (this.updateLastExecution && nbt.contains("LastExecution")) {
            this.lastExecution = nbt.getLong("LastExecution");
        } else {
            this.lastExecution = -1L;
        }

    }

    public void setCommand(String command) {
        this.command = command;
        this.successCount = 0;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean performCommand(Level world) {
        if (!world.isClientSide && world.getGameTime() != this.lastExecution) {
            if ("Searge".equalsIgnoreCase(this.command)) {
                this.lastOutput = new TextComponent("#itzlipofutzli");
                this.successCount = 1;
                return true;
            } else {
                this.successCount = 0;
                MinecraftServer minecraftServer = this.getLevel().getServer();
                if (minecraftServer.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(this.command)) {
                    try {
                        this.lastOutput = null;
                        CommandSourceStack commandSourceStack = this.createCommandSourceStack().withCallback((commandContext, bl, i) -> {
                            if (bl) {
                                ++this.successCount;
                            }

                        });
                        minecraftServer.getCommands().performCommand(commandSourceStack, this.command);
                    } catch (Throwable var6) {
                        CrashReport crashReport = CrashReport.forThrowable(var6, "Executing command block");
                        CrashReportCategory crashReportCategory = crashReport.addCategory("Command to be executed");
                        crashReportCategory.setDetail("Command", this::getCommand);
                        crashReportCategory.setDetail("Name", () -> {
                            return this.getName().getString();
                        });
                        throw new ReportedException(crashReport);
                    }
                }

                if (this.updateLastExecution) {
                    this.lastExecution = world.getGameTime();
                } else {
                    this.lastExecution = -1L;
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public Component getName() {
        return this.name;
    }

    public void setName(@Nullable Component name) {
        if (name != null) {
            this.name = name;
        } else {
            this.name = DEFAULT_NAME;
        }

    }

    @Override
    public void sendMessage(Component message, UUID sender) {
        if (this.trackOutput) {
            this.lastOutput = (new TextComponent("[" + TIME_FORMAT.format(new Date()) + "] ")).append(message);
            this.onUpdated();
        }

    }

    public abstract ServerLevel getLevel();

    public abstract void onUpdated();

    public void setLastOutput(@Nullable Component lastOutput) {
        this.lastOutput = lastOutput;
    }

    public void setTrackOutput(boolean trackOutput) {
        this.trackOutput = trackOutput;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }

    public InteractionResult usedBy(Player player) {
        if (!player.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
        } else {
            if (player.getCommandSenderWorld().isClientSide) {
                player.openMinecartCommandBlock(this);
            }

            return InteractionResult.sidedSuccess(player.level.isClientSide);
        }
    }

    public abstract Vec3 getPosition();

    public abstract CommandSourceStack createCommandSourceStack();

    @Override
    public boolean acceptsSuccess() {
        return this.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK) && this.trackOutput;
    }

    @Override
    public boolean acceptsFailure() {
        return this.trackOutput;
    }

    @Override
    public boolean shouldInformAdmins() {
        return this.getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
    }
}
