package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketListener;

public interface ClientGamePacketListener extends PacketListener {
    void handleAddEntity(ClientboundAddEntityPacket packet);

    void handleAddExperienceOrb(ClientboundAddExperienceOrbPacket packet);

    void handleAddVibrationSignal(ClientboundAddVibrationSignalPacket packet);

    void handleAddMob(ClientboundAddMobPacket packet);

    void handleAddObjective(ClientboundSetObjectivePacket packet);

    void handleAddPainting(ClientboundAddPaintingPacket packet);

    void handleAddPlayer(ClientboundAddPlayerPacket packet);

    void handleAnimate(ClientboundAnimatePacket packet);

    void handleAwardStats(ClientboundAwardStatsPacket packet);

    void handleAddOrRemoveRecipes(ClientboundRecipePacket packet);

    void handleBlockDestruction(ClientboundBlockDestructionPacket packet);

    void handleOpenSignEditor(ClientboundOpenSignEditorPacket packet);

    void handleBlockEntityData(ClientboundBlockEntityDataPacket packet);

    void handleBlockEvent(ClientboundBlockEventPacket packet);

    void handleBlockUpdate(ClientboundBlockUpdatePacket packet);

    void handleChat(ClientboundChatPacket packet);

    void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet);

    void handleMapItemData(ClientboundMapItemDataPacket packet);

    void handleContainerClose(ClientboundContainerClosePacket packet);

    void handleContainerContent(ClientboundContainerSetContentPacket packet);

    void handleHorseScreenOpen(ClientboundHorseScreenOpenPacket packet);

    void handleContainerSetData(ClientboundContainerSetDataPacket packet);

    void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet);

    void handleCustomPayload(ClientboundCustomPayloadPacket packet);

    void handleDisconnect(ClientboundDisconnectPacket packet);

    void handleEntityEvent(ClientboundEntityEventPacket packet);

    void handleEntityLinkPacket(ClientboundSetEntityLinkPacket packet);

    void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket packet);

    void handleExplosion(ClientboundExplodePacket packet);

    void handleGameEvent(ClientboundGameEventPacket packet);

    void handleKeepAlive(ClientboundKeepAlivePacket packet);

    void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet);

    void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket packet);

    void handleLevelEvent(ClientboundLevelEventPacket packet);

    void handleLogin(ClientboundLoginPacket packet);

    void handleMoveEntity(ClientboundMoveEntityPacket packet);

    void handleMovePlayer(ClientboundPlayerPositionPacket packet);

    void handleParticleEvent(ClientboundLevelParticlesPacket packet);

    void handlePing(ClientboundPingPacket packet);

    void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket packet);

    void handlePlayerInfo(ClientboundPlayerInfoPacket packet);

    void handleRemoveEntities(ClientboundRemoveEntitiesPacket packet);

    void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket packet);

    void handleRespawn(ClientboundRespawnPacket packet);

    void handleRotateMob(ClientboundRotateHeadPacket packet);

    void handleSetCarriedItem(ClientboundSetCarriedItemPacket packet);

    void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket packet);

    void handleSetEntityData(ClientboundSetEntityDataPacket packet);

    void handleSetEntityMotion(ClientboundSetEntityMotionPacket packet);

    void handleSetEquipment(ClientboundSetEquipmentPacket packet);

    void handleSetExperience(ClientboundSetExperiencePacket packet);

    void handleSetHealth(ClientboundSetHealthPacket packet);

    void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket packet);

    void handleSetScore(ClientboundSetScorePacket packet);

    void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket packet);

    void handleSetTime(ClientboundSetTimePacket packet);

    void handleSoundEvent(ClientboundSoundPacket packet);

    void handleSoundEntityEvent(ClientboundSoundEntityPacket packet);

    void handleCustomSoundEvent(ClientboundCustomSoundPacket packet);

    void handleTakeItemEntity(ClientboundTakeItemEntityPacket packet);

    void handleTeleportEntity(ClientboundTeleportEntityPacket packet);

    void handleUpdateAttributes(ClientboundUpdateAttributesPacket packet);

    void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket packet);

    void handleUpdateTags(ClientboundUpdateTagsPacket packet);

    void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket packet);

    void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket packet);

    void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket packet);

    void handleChangeDifficulty(ClientboundChangeDifficultyPacket packet);

    void handleSetCamera(ClientboundSetCameraPacket packet);

    void handleInitializeBorder(ClientboundInitializeBorderPacket packet);

    void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket packet);

    void handleSetBorderSize(ClientboundSetBorderSizePacket packet);

    void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket packet);

    void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket packet);

    void handleSetBorderCenter(ClientboundSetBorderCenterPacket packet);

    void handleTabListCustomisation(ClientboundTabListPacket packet);

    void handleResourcePack(ClientboundResourcePackPacket packet);

    void handleBossUpdate(ClientboundBossEventPacket packet);

    void handleItemCooldown(ClientboundCooldownPacket packet);

    void handleMoveVehicle(ClientboundMoveVehiclePacket packet);

    void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket packet);

    void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket packet);

    void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket packet);

    void handleCommands(ClientboundCommandsPacket packet);

    void handleStopSoundEvent(ClientboundStopSoundPacket packet);

    void handleCommandSuggestions(ClientboundCommandSuggestionsPacket packet);

    void handleUpdateRecipes(ClientboundUpdateRecipesPacket packet);

    void handleLookAt(ClientboundPlayerLookAtPacket packet);

    void handleTagQueryPacket(ClientboundTagQueryPacket packet);

    void handleLightUpdatePacket(ClientboundLightUpdatePacket packet);

    void handleOpenBook(ClientboundOpenBookPacket packet);

    void handleOpenScreen(ClientboundOpenScreenPacket packet);

    void handleMerchantOffers(ClientboundMerchantOffersPacket packet);

    void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket packet);

    void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket packet);

    void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet);

    void handleBlockBreakAck(ClientboundBlockBreakAckPacket packet);

    void setActionBarText(ClientboundSetActionBarTextPacket packet);

    void setSubtitleText(ClientboundSetSubtitleTextPacket packet);

    void setTitleText(ClientboundSetTitleTextPacket packet);

    void setTitlesAnimation(ClientboundSetTitlesAnimationPacket packet);

    void handleTitlesClear(ClientboundClearTitlesPacket packet);
}
