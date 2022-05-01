package net.minecraft.network.protocol.game;

public interface ServerGamePacketListener extends ServerPacketListener {
    void handleAnimate(ServerboundSwingPacket packet);

    void handleChat(ServerboundChatPacket packet);

    void handleClientCommand(ServerboundClientCommandPacket packet);

    void handleClientInformation(ServerboundClientInformationPacket packet);

    void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet);

    void handleContainerClick(ServerboundContainerClickPacket packet);

    void handlePlaceRecipe(ServerboundPlaceRecipePacket packet);

    void handleContainerClose(ServerboundContainerClosePacket packet);

    void handleCustomPayload(ServerboundCustomPayloadPacket packet);

    void handleInteract(ServerboundInteractPacket packet);

    void handleKeepAlive(ServerboundKeepAlivePacket packet);

    void handleMovePlayer(ServerboundMovePlayerPacket packet);

    void handlePong(ServerboundPongPacket packet);

    void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet);

    void handlePlayerAction(ServerboundPlayerActionPacket packet);

    void handlePlayerCommand(ServerboundPlayerCommandPacket packet);

    void handlePlayerInput(ServerboundPlayerInputPacket packet);

    void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet);

    void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet);

    void handleSignUpdate(ServerboundSignUpdatePacket packet);

    void handleUseItemOn(ServerboundUseItemOnPacket packet);

    void handleUseItem(ServerboundUseItemPacket packet);

    void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet);

    void handleResourcePackResponse(ServerboundResourcePackPacket packet);

    void handlePaddleBoat(ServerboundPaddleBoatPacket packet);

    void handleMoveVehicle(ServerboundMoveVehiclePacket packet);

    void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet);

    void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet);

    void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet);

    void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet);

    void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet);

    void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet);

    void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet);

    void handlePickItem(ServerboundPickItemPacket packet);

    void handleRenameItem(ServerboundRenameItemPacket packet);

    void handleSetBeaconPacket(ServerboundSetBeaconPacket packet);

    void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet);

    void handleSelectTrade(ServerboundSelectTradePacket packet);

    void handleEditBook(ServerboundEditBookPacket packet);

    void handleEntityTagQuery(ServerboundEntityTagQuery packet);

    void handleBlockEntityTagQuery(ServerboundBlockEntityTagQuery packet);

    void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet);

    void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet);

    void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet);

    void handleLockDifficulty(ServerboundLockDifficultyPacket packet);
}
