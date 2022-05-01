package com.destroystokyo.paper.console;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.console.HexFormattingConverter;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v1_18_R2.command.CraftConsoleCommandSender;

public class TerminalConsoleCommandSender extends CraftConsoleCommandSender {

    private static final Logger LOGGER = LogManager.getRootLogger();

    @Override
    public void sendRawMessage(String message) {
        final Component msg = PaperAdventure.LEGACY_SECTION_UXRC.deserialize(message);
        this.sendMessage(Identity.nil(), msg, MessageType.SYSTEM);
    }

    @Override
    public void sendMessage(Identity identity, Component message, MessageType type) {
        LOGGER.info(HexFormattingConverter.SERIALIZER.serialize(message));
    }

}
