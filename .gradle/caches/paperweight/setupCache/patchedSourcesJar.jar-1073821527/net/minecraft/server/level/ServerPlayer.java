package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class ServerPlayer extends Player {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
    public ServerGamePacketListenerImpl connection;
    public final MinecraftServer server;
    public final ServerPlayerGameMode gameMode;
    private final PlayerAdvancements advancements;
    private final ServerStatsCounter stats;
    private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
    private int lastRecordedFoodLevel = Integer.MIN_VALUE;
    private int lastRecordedAirLevel = Integer.MIN_VALUE;
    private int lastRecordedArmor = Integer.MIN_VALUE;
    private int lastRecordedLevel = Integer.MIN_VALUE;
    private int lastRecordedExperience = Integer.MIN_VALUE;
    private float lastSentHealth = -1.0E8F;
    private int lastSentFood = -99999999;
    private boolean lastFoodSaturationZero = true;
    public int lastSentExp = -99999999;
    public int spawnInvulnerableTime = 60;
    private ChatVisiblity chatVisibility = ChatVisiblity.FULL;
    private boolean canChatColor = true;
    private long lastActionTime = Util.getMillis();
    @Nullable
    private Entity camera;
    public boolean isChangingDimension;
    private boolean seenCredits;
    private final ServerRecipeBook recipeBook = new ServerRecipeBook();
    @Nullable
    private Vec3 levitationStartPos;
    private int levitationStartTime;
    private boolean disconnected;
    @Nullable
    private Vec3 startingToFallPosition;
    @Nullable
    private Vec3 enteredNetherPosition;
    @Nullable
    private Vec3 enteredLavaOnVehiclePosition;
    private SectionPos lastSectionPos = SectionPos.of(0, 0, 0);
    private ResourceKey<Level> respawnDimension = Level.OVERWORLD;
    @Nullable
    private BlockPos respawnPosition;
    private boolean respawnForced;
    private float respawnAngle;
    private final TextFilter textFilter;
    private boolean textFilteringEnabled;
    private boolean allowsListing = true;
    private final ContainerSynchronizer containerSynchronizer = new ContainerSynchronizer() {
        @Override
        public void sendInitialData(AbstractContainerMenu handler, NonNullList<ItemStack> stacks, ItemStack cursorStack, int[] properties) {
            ServerPlayer.this.connection.send(new ClientboundContainerSetContentPacket(handler.containerId, handler.incrementStateId(), stacks, cursorStack));

            for(int i = 0; i < properties.length; ++i) {
                this.broadcastDataValue(handler, i, properties[i]);
            }

        }

        @Override
        public void sendSlotChange(AbstractContainerMenu handler, int slot, ItemStack stack) {
            ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(handler.containerId, handler.incrementStateId(), slot, stack));
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu handler, ItemStack stack) {
            ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(-1, handler.incrementStateId(), -1, stack));
        }

        @Override
        public void sendDataChange(AbstractContainerMenu handler, int property, int value) {
            this.broadcastDataValue(handler, property, value);
        }

        private void broadcastDataValue(AbstractContainerMenu handler, int property, int value) {
            ServerPlayer.this.connection.send(new ClientboundContainerSetDataPacket(handler.containerId, property, value));
        }
    };
    private final ContainerListener containerListener = new ContainerListener() {
        @Override
        public void slotChanged(AbstractContainerMenu handler, int slotId, ItemStack stack) {
            Slot slot = handler.getSlot(slotId);
            if (!(slot instanceof ResultSlot)) {
                if (slot.container == ServerPlayer.this.getInventory()) {
                    CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), stack);
                }

            }
        }

        @Override
        public void dataChanged(AbstractContainerMenu handler, int property, int value) {
        }
    };
    private int containerCounter;
    public int latency;
    public boolean wonGame;

    public ServerPlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(world, world.getSharedSpawnPos(), world.getSharedSpawnAngle(), profile);
        this.textFilter = server.createTextFilterForPlayer(this);
        this.gameMode = server.createGameModeForPlayer(this);
        this.server = server;
        this.stats = server.getPlayerList().getPlayerStats(this);
        this.advancements = server.getPlayerList().getPlayerAdvancements(this);
        this.maxUpStep = 1.0F;
        this.fudgeSpawnLocation(world);
    }

    public void fudgeSpawnLocation(ServerLevel world) {
        BlockPos blockPos = world.getSharedSpawnPos();
        if (world.dimensionType().hasSkyLight() && world.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
            int i = Math.max(0, this.server.getSpawnRadius(world));
            int j = Mth.floor(world.getWorldBorder().getDistanceToBorder((double)blockPos.getX(), (double)blockPos.getZ()));
            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long l = (long)(i * 2 + 1);
            long m = l * l;
            int k = m > 2147483647L ? Integer.MAX_VALUE : (int)m;
            int n = this.getCoprime(k);
            int o = (new Random()).nextInt(k);

            for(int p = 0; p < k; ++p) {
                int q = (o + n * p) % k;
                int r = q % (i * 2 + 1);
                int s = q / (i * 2 + 1);
                BlockPos blockPos2 = PlayerRespawnLogic.getOverworldRespawnPos(world, blockPos.getX() + r - i, blockPos.getZ() + s - i);
                if (blockPos2 != null) {
                    this.moveTo(blockPos2, 0.0F, 0.0F);
                    if (world.noCollision(this)) {
                        break;
                    }
                }
            }
        } else {
            this.moveTo(blockPos, 0.0F, 0.0F);

            while(!world.noCollision(this) && this.getY() < (double)(world.getMaxBuildHeight() - 1)) {
                this.setPos(this.getX(), this.getY() + 1.0D, this.getZ());
            }
        }

    }

    private int getCoprime(int horizontalSpawnArea) {
        return horizontalSpawnArea <= 16 ? horizontalSpawnArea - 1 : 17;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("enteredNetherPosition", 10)) {
            CompoundTag compoundTag = nbt.getCompound("enteredNetherPosition");
            this.enteredNetherPosition = new Vec3(compoundTag.getDouble("x"), compoundTag.getDouble("y"), compoundTag.getDouble("z"));
        }

        this.seenCredits = nbt.getBoolean("seenCredits");
        if (nbt.contains("recipeBook", 10)) {
            this.recipeBook.fromNbt(nbt.getCompound("recipeBook"), this.server.getRecipeManager());
        }

        if (this.isSleeping()) {
            this.stopSleeping();
        }

        if (nbt.contains("SpawnX", 99) && nbt.contains("SpawnY", 99) && nbt.contains("SpawnZ", 99)) {
            this.respawnPosition = new BlockPos(nbt.getInt("SpawnX"), nbt.getInt("SpawnY"), nbt.getInt("SpawnZ"));
            this.respawnForced = nbt.getBoolean("SpawnForced");
            this.respawnAngle = nbt.getFloat("SpawnAngle");
            if (nbt.contains("SpawnDimension")) {
                this.respawnDimension = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, nbt.get("SpawnDimension")).resultOrPartial(LOGGER::error).orElse(Level.OVERWORLD);
            }
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        this.storeGameTypes(nbt);
        nbt.putBoolean("seenCredits", this.seenCredits);
        if (this.enteredNetherPosition != null) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putDouble("x", this.enteredNetherPosition.x);
            compoundTag.putDouble("y", this.enteredNetherPosition.y);
            compoundTag.putDouble("z", this.enteredNetherPosition.z);
            nbt.put("enteredNetherPosition", compoundTag);
        }

        Entity entity = this.getRootVehicle();
        Entity entity2 = this.getVehicle();
        if (entity2 != null && entity != this && entity.hasExactlyOnePlayerPassenger()) {
            CompoundTag compoundTag2 = new CompoundTag();
            CompoundTag compoundTag3 = new CompoundTag();
            entity.save(compoundTag3);
            compoundTag2.putUUID("Attach", entity2.getUUID());
            compoundTag2.put("Entity", compoundTag3);
            nbt.put("RootVehicle", compoundTag2);
        }

        nbt.put("recipeBook", this.recipeBook.toNbt());
        nbt.putString("Dimension", this.level.dimension().location().toString());
        if (this.respawnPosition != null) {
            nbt.putInt("SpawnX", this.respawnPosition.getX());
            nbt.putInt("SpawnY", this.respawnPosition.getY());
            nbt.putInt("SpawnZ", this.respawnPosition.getZ());
            nbt.putBoolean("SpawnForced", this.respawnForced);
            nbt.putFloat("SpawnAngle", this.respawnAngle);
            ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, this.respawnDimension.location()).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
                nbt.put("SpawnDimension", tag);
            });
        }

    }

    public void setExperiencePoints(int points) {
        float f = (float)this.getXpNeededForNextLevel();
        float g = (f - 1.0F) / f;
        this.experienceProgress = Mth.clamp((float)points / f, 0.0F, g);
        this.lastSentExp = -1;
    }

    public void setExperienceLevels(int level) {
        this.experienceLevel = level;
        this.lastSentExp = -1;
    }

    @Override
    public void giveExperienceLevels(int levels) {
        super.giveExperienceLevels(levels);
        this.lastSentExp = -1;
    }

    @Override
    public void onEnchantmentPerformed(ItemStack enchantedItem, int experienceLevels) {
        super.onEnchantmentPerformed(enchantedItem, experienceLevels);
        this.lastSentExp = -1;
    }

    public void initMenu(AbstractContainerMenu screenHandler) {
        screenHandler.addSlotListener(this.containerListener);
        screenHandler.setSynchronizer(this.containerSynchronizer);
    }

    public void initInventoryMenu() {
        this.initMenu(this.inventoryMenu);
    }

    @Override
    public void onEnterCombat() {
        super.onEnterCombat();
        this.connection.send(new ClientboundPlayerCombatEnterPacket());
    }

    @Override
    public void onLeaveCombat() {
        super.onLeaveCombat();
        this.connection.send(new ClientboundPlayerCombatEndPacket(this.getCombatTracker()));
    }

    @Override
    protected void onInsideBlock(BlockState state) {
        CriteriaTriggers.ENTER_BLOCK.trigger(this, state);
    }

    @Override
    protected ItemCooldowns createItemCooldowns() {
        return new ServerItemCooldowns(this);
    }

    @Override
    public void tick() {
        this.gameMode.tick();
        --this.spawnInvulnerableTime;
        if (this.invulnerableTime > 0) {
            --this.invulnerableTime;
        }

        this.containerMenu.broadcastChanges();
        if (!this.level.isClientSide && !this.containerMenu.stillValid(this)) {
            this.closeContainer();
            this.containerMenu = this.inventoryMenu;
        }

        Entity entity = this.getCamera();
        if (entity != this) {
            if (entity.isAlive()) {
                this.absMoveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                this.getLevel().getChunkSource().move(this);
                if (this.wantsToStopRiding()) {
                    this.setCamera(this);
                }
            } else {
                this.setCamera(this);
            }
        }

        CriteriaTriggers.TICK.trigger(this);
        if (this.levitationStartPos != null) {
            CriteriaTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
        }

        this.trackStartFallingPosition();
        this.trackEnteredOrExitedLavaOnVehicle();
        this.advancements.flushDirty(this);
    }

    public void doTick() {
        try {
            if (!this.isSpectator() || !this.touchingUnloadedChunk()) {
                super.tick();
            }

            for(int i = 0; i < this.getInventory().getContainerSize(); ++i) {
                ItemStack itemStack = this.getInventory().getItem(i);
                if (itemStack.getItem().isComplex()) {
                    Packet<?> packet = ((ComplexItem)itemStack.getItem()).getUpdatePacket(itemStack, this.level, this);
                    if (packet != null) {
                        this.connection.send(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastSentHealth || this.lastSentFood != this.foodData.getFoodLevel() || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
                this.connection.send(new ClientboundSetHealthPacket(this.getHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel()));
                this.lastSentHealth = this.getHealth();
                this.lastSentFood = this.foodData.getFoodLevel();
                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption) {
                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
                this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
            }

            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
                this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil((float)this.lastRecordedFoodLevel));
            }

            if (this.getAirSupply() != this.lastRecordedAirLevel) {
                this.lastRecordedAirLevel = this.getAirSupply();
                this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil((float)this.lastRecordedAirLevel));
            }

            if (this.getArmorValue() != this.lastRecordedArmor) {
                this.lastRecordedArmor = this.getArmorValue();
                this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil((float)this.lastRecordedArmor));
            }

            if (this.totalExperience != this.lastRecordedExperience) {
                this.lastRecordedExperience = this.totalExperience;
                this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil((float)this.lastRecordedExperience));
            }

            if (this.experienceLevel != this.lastRecordedLevel) {
                this.lastRecordedLevel = this.experienceLevel;
                this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil((float)this.lastRecordedLevel));
            }

            if (this.totalExperience != this.lastSentExp) {
                this.lastSentExp = this.totalExperience;
                this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
            }

            if (this.tickCount % 20 == 0) {
                CriteriaTriggers.LOCATION.trigger(this);
            }

        } catch (Throwable var4) {
            CrashReport crashReport = CrashReport.forThrowable(var4, "Ticking player");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Player being ticked");
            this.fillCrashReportCategory(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public void resetFallDistance() {
        if (this.getHealth() > 0.0F && this.startingToFallPosition != null) {
            CriteriaTriggers.FALL_FROM_HEIGHT.trigger(this, this.startingToFallPosition);
        }

        this.startingToFallPosition = null;
        super.resetFallDistance();
    }

    public void trackStartFallingPosition() {
        if (this.fallDistance > 0.0F && this.startingToFallPosition == null) {
            this.startingToFallPosition = this.position();
        }

    }

    public void trackEnteredOrExitedLavaOnVehicle() {
        if (this.getVehicle() != null && this.getVehicle().isInLava()) {
            if (this.enteredLavaOnVehiclePosition == null) {
                this.enteredLavaOnVehiclePosition = this.position();
            } else {
                CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.trigger(this, this.enteredLavaOnVehiclePosition);
            }
        }

        if (this.enteredLavaOnVehiclePosition != null && (this.getVehicle() == null || !this.getVehicle().isInLava())) {
            this.enteredLavaOnVehiclePosition = null;
        }

    }

    private void updateScoreForCriteria(ObjectiveCriteria criterion, int score) {
        this.getScoreboard().forAllObjectives(criterion, this.getScoreboardName(), (scorex) -> {
            scorex.setScore(score);
        });
    }

    @Override
    public void die(DamageSource source) {
        boolean bl = this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        if (bl) {
            Component component = this.getCombatTracker().getDeathMessage();
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), component), (future) -> {
                if (!future.isSuccess()) {
                    int i = 256;
                    String string = component.getString(256);
                    Component component2 = new TranslatableComponent("death.attack.message_too_long", (new TextComponent(string)).withStyle(ChatFormatting.YELLOW));
                    Component component3 = (new TranslatableComponent("death.attack.even_more_magic", this.getDisplayName())).withStyle((style) -> {
                        return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component2));
                    });
                    this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), component3));
                }

            });
            Team team = this.getTeam();
            if (team != null && team.getDeathMessageVisibility() != Team.Visibility.ALWAYS) {
                if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().broadcastToTeam(this, component);
                } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().broadcastToAllExceptTeam(this, component);
                }
            } else {
                this.server.getPlayerList().broadcastMessage(component, ChatType.SYSTEM, Util.NIL_UUID);
            }
        } else {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), TextComponent.EMPTY));
        }

        this.removeEntitiesOnShoulder();
        if (this.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }

        if (!this.isSpectator()) {
            this.dropAllDeathLoot(source);
        }

        this.getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, this.getScoreboardName(), Score::increment);
        LivingEntity livingEntity = this.getKillCredit();
        if (livingEntity != null) {
            this.awardStat(Stats.ENTITY_KILLED_BY.get(livingEntity.getType()));
            livingEntity.awardKillScore(this, this.deathScore, source);
            this.createWitherRose(livingEntity);
        }

        this.level.broadcastEntityEvent(this, (byte)3);
        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);
        this.getCombatTracker().recheckStatus();
    }

    private void tellNeutralMobsThatIDied() {
        AABB aABB = (new AABB(this.blockPosition())).inflate(32.0D, 10.0D, 32.0D);
        this.level.getEntitiesOfClass(Mob.class, aABB, EntitySelector.NO_SPECTATORS).stream().filter((entity) -> {
            return entity instanceof NeutralMob;
        }).forEach((entity) -> {
            ((NeutralMob)entity).playerDied(this);
        });
    }

    @Override
    public void awardKillScore(Entity entityKilled, int score, DamageSource damageSource) {
        if (entityKilled != this) {
            super.awardKillScore(entityKilled, score, damageSource);
            this.increaseScore(score);
            String string = this.getScoreboardName();
            String string2 = entityKilled.getScoreboardName();
            this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_ALL, string, Score::increment);
            if (entityKilled instanceof Player) {
                this.awardStat(Stats.PLAYER_KILLS);
                this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_PLAYERS, string, Score::increment);
            } else {
                this.awardStat(Stats.MOB_KILLS);
            }

            this.handleTeamKill(string, string2, ObjectiveCriteria.TEAM_KILL);
            this.handleTeamKill(string2, string, ObjectiveCriteria.KILLED_BY_TEAM);
            CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(this, entityKilled, damageSource);
        }
    }

    private void handleTeamKill(String playerName, String team, ObjectiveCriteria[] criterions) {
        PlayerTeam playerTeam = this.getScoreboard().getPlayersTeam(team);
        if (playerTeam != null) {
            int i = playerTeam.getColor().getId();
            if (i >= 0 && i < criterions.length) {
                this.getScoreboard().forAllObjectives(criterions[i], playerName, Score::increment);
            }
        }

    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            boolean bl = this.server.isDedicatedServer() && this.isPvpAllowed() && "fall".equals(source.msgId);
            if (!bl && this.spawnInvulnerableTime > 0 && source != DamageSource.OUT_OF_WORLD) {
                return false;
            } else {
                if (source instanceof EntityDamageSource) {
                    Entity entity = source.getEntity();
                    if (entity instanceof Player && !this.canHarmPlayer((Player)entity)) {
                        return false;
                    }

                    if (entity instanceof AbstractArrow) {
                        AbstractArrow abstractArrow = (AbstractArrow)entity;
                        Entity entity2 = abstractArrow.getOwner();
                        if (entity2 instanceof Player && !this.canHarmPlayer((Player)entity2)) {
                            return false;
                        }
                    }
                }

                return super.hurt(source, amount);
            }
        }
    }

    @Override
    public boolean canHarmPlayer(Player player) {
        return !this.isPvpAllowed() ? false : super.canHarmPlayer(player);
    }

    private boolean isPvpAllowed() {
        return this.server.isPvpAllowed();
    }

    @Nullable
    @Override
    protected PortalInfo findDimensionEntryPoint(ServerLevel destination) {
        PortalInfo portalInfo = super.findDimensionEntryPoint(destination);
        if (portalInfo != null && this.level.dimension() == Level.OVERWORLD && destination.dimension() == Level.END) {
            Vec3 vec3 = portalInfo.pos.add(0.0D, -1.0D, 0.0D);
            return new PortalInfo(vec3, Vec3.ZERO, 90.0F, 0.0F);
        } else {
            return portalInfo;
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(ServerLevel destination) {
        this.isChangingDimension = true;
        ServerLevel serverLevel = this.getLevel();
        ResourceKey<Level> resourceKey = serverLevel.dimension();
        if (resourceKey == Level.END && destination.dimension() == Level.OVERWORLD) {
            this.unRide();
            this.getLevel().removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            if (!this.wonGame) {
                this.wonGame = true;
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return this;
        } else {
            LevelData levelData = destination.getLevelData();
            this.connection.send(new ClientboundRespawnPacket(destination.dimensionTypeRegistration(), destination.dimension(), BiomeManager.obfuscateSeed(destination.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), destination.isDebug(), destination.isFlat(), true));
            this.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
            PlayerList playerList = this.server.getPlayerList();
            playerList.sendPlayerPermissionLevel(this);
            serverLevel.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            PortalInfo portalInfo = this.findDimensionEntryPoint(destination);
            if (portalInfo != null) {
                serverLevel.getProfiler().push("moving");
                if (resourceKey == Level.OVERWORLD && destination.dimension() == Level.NETHER) {
                    this.enteredNetherPosition = this.position();
                } else if (destination.dimension() == Level.END) {
                    this.createEndPlatform(destination, new BlockPos(portalInfo.pos));
                }

                serverLevel.getProfiler().pop();
                serverLevel.getProfiler().push("placing");
                this.setLevel(destination);
                destination.addDuringPortalTeleport(this);
                this.setRot(portalInfo.yRot, portalInfo.xRot);
                this.moveTo(portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z);
                serverLevel.getProfiler().pop();
                this.triggerDimensionChangeTriggers(serverLevel);
                this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                playerList.sendLevelInfo(this, destination);
                playerList.sendAllPlayerInfo(this);

                for(MobEffectInstance mobEffectInstance : this.getActiveEffects()) {
                    this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), mobEffectInstance));
                }

                this.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;
            }

            return this;
        }
    }

    private void createEndPlatform(ServerLevel world, BlockPos centerPos) {
        BlockPos.MutableBlockPos mutableBlockPos = centerPos.mutable();

        for(int i = -2; i <= 2; ++i) {
            for(int j = -2; j <= 2; ++j) {
                for(int k = -1; k < 3; ++k) {
                    BlockState blockState = k == -1 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    world.setBlockAndUpdate(mutableBlockPos.set(centerPos).move(j, k, i), blockState);
                }
            }
        }

    }

    @Override
    protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel destWorld, BlockPos destPos, boolean destIsNether, WorldBorder worldBorder) {
        Optional<BlockUtil.FoundRectangle> optional = super.getExitPortal(destWorld, destPos, destIsNether, worldBorder);
        if (optional.isPresent()) {
            return optional;
        } else {
            Direction.Axis axis = this.level.getBlockState(this.portalEntrancePos).getOptionalValue(NetherPortalBlock.AXIS).orElse(Direction.Axis.X);
            Optional<BlockUtil.FoundRectangle> optional2 = destWorld.getPortalForcer().createPortal(destPos, axis);
            if (!optional2.isPresent()) {
                LOGGER.error("Unable to create a portal, likely target out of worldborder");
            }

            return optional2;
        }
    }

    public void triggerDimensionChangeTriggers(ServerLevel origin) {
        ResourceKey<Level> resourceKey = origin.dimension();
        ResourceKey<Level> resourceKey2 = this.level.dimension();
        CriteriaTriggers.CHANGED_DIMENSION.trigger(this, resourceKey, resourceKey2);
        if (resourceKey == Level.NETHER && resourceKey2 == Level.OVERWORLD && this.enteredNetherPosition != null) {
            CriteriaTriggers.NETHER_TRAVEL.trigger(this, this.enteredNetherPosition);
        }

        if (resourceKey2 != Level.NETHER) {
            this.enteredNetherPosition = null;
        }

    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer spectator) {
        if (spectator.isSpectator()) {
            return this.getCamera() == this;
        } else {
            return this.isSpectator() ? false : super.broadcastToPlayer(spectator);
        }
    }

    @Override
    public void take(Entity item, int count) {
        super.take(item, count);
        this.containerMenu.broadcastChanges();
    }

    @Override
    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos pos) {
        Direction direction = this.level.getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING);
        if (!this.isSleeping() && this.isAlive()) {
            if (!this.level.dimensionType().natural()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
            } else if (!this.bedInRange(pos, direction)) {
                return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
            } else if (this.bedBlocked(pos, direction)) {
                return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
            } else {
                this.setRespawnPosition(this.level.dimension(), pos, this.getYRot(), false, true);
                if (this.level.isDay()) {
                    return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
                } else {
                    if (!this.isCreative()) {
                        double d = 8.0D;
                        double e = 5.0D;
                        Vec3 vec3 = Vec3.atBottomCenterOf(pos);
                        List<Monster> list = this.level.getEntitiesOfClass(Monster.class, new AABB(vec3.x() - 8.0D, vec3.y() - 5.0D, vec3.z() - 8.0D, vec3.x() + 8.0D, vec3.y() + 5.0D, vec3.z() + 8.0D), (monster) -> {
                            return monster.isPreventingPlayerRest(this);
                        });
                        if (!list.isEmpty()) {
                            return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                        }
                    }

                    Either<Player.BedSleepingProblem, Unit> either = super.startSleepInBed(pos).ifRight((unit) -> {
                        this.awardStat(Stats.SLEEP_IN_BED);
                        CriteriaTriggers.SLEPT_IN_BED.trigger(this);
                    });
                    if (!this.getLevel().canSleepThroughNights()) {
                        this.displayClientMessage(new TranslatableComponent("sleep.not_possible"), true);
                    }

                    ((ServerLevel)this.level).updateSleepingPlayerList();
                    return either;
                }
            }
        } else {
            return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @Override
    public void startSleeping(BlockPos pos) {
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        super.startSleeping(pos);
    }

    private boolean bedInRange(BlockPos pos, Direction direction) {
        return this.isReachableBedBlock(pos) || this.isReachableBedBlock(pos.relative(direction.getOpposite()));
    }

    private boolean isReachableBedBlock(BlockPos pos) {
        Vec3 vec3 = Vec3.atBottomCenterOf(pos);
        return Math.abs(this.getX() - vec3.x()) <= 3.0D && Math.abs(this.getY() - vec3.y()) <= 2.0D && Math.abs(this.getZ() - vec3.z()) <= 3.0D;
    }

    private boolean bedBlocked(BlockPos pos, Direction direction) {
        BlockPos blockPos = pos.above();
        return !this.freeAt(blockPos) || !this.freeAt(blockPos.relative(direction.getOpposite()));
    }

    @Override
    public void stopSleepInBed(boolean skipSleepTimer, boolean updateSleepingPlayers) {
        if (this.isSleeping()) {
            this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(this, 2));
        }

        super.stopSleepInBed(skipSleepTimer, updateSleepingPlayers);
        if (this.connection != null) {
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }

    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        Entity entity2 = this.getVehicle();
        if (!super.startRiding(entity, force)) {
            return false;
        } else {
            Entity entity3 = this.getVehicle();
            if (entity3 != entity2 && this.connection != null) {
                this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            }

            return true;
        }
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        super.stopRiding();
        Entity entity2 = this.getVehicle();
        if (entity2 != entity && this.connection != null) {
            this.connection.dismount(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }

    }

    @Override
    public void dismountTo(double destX, double destY, double destZ) {
        this.removeVehicle();
        if (this.connection != null) {
            this.connection.dismount(destX, destY, destZ, this.getYRot(), this.getXRot());
        }

    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return super.isInvulnerableTo(damageSource) || this.isChangingDimension() || this.getAbilities().invulnerable && damageSource == DamageSource.WITHER;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
    }

    @Override
    protected void onChangedBlock(BlockPos pos) {
        if (!this.isSpectator()) {
            super.onChangedBlock(pos);
        }

    }

    public void doCheckFallDamage(double heightDifference, boolean onGround) {
        if (!this.touchingUnloadedChunk()) {
            BlockPos blockPos = this.getOnPos();
            super.checkFallDamage(heightDifference, onGround, this.level.getBlockState(blockPos), blockPos);
        }
    }

    @Override
    public void openTextEdit(SignBlockEntity sign) {
        sign.setAllowedPlayerEditor(this.getUUID());
        this.connection.send(new ClientboundBlockUpdatePacket(this.level, sign.getBlockPos()));
        this.connection.send(new ClientboundOpenSignEditorPacket(sign.getBlockPos()));
    }

    public void nextContainerCounter() {
        this.containerCounter = this.containerCounter % 100 + 1;
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider factory) {
        if (factory == null) {
            return OptionalInt.empty();
        } else {
            if (this.containerMenu != this.inventoryMenu) {
                this.closeContainer();
            }

            this.nextContainerCounter();
            AbstractContainerMenu abstractContainerMenu = factory.createMenu(this.containerCounter, this.getInventory(), this);
            if (abstractContainerMenu == null) {
                if (this.isSpectator()) {
                    this.displayClientMessage((new TranslatableComponent("container.spectatorCantOpen")).withStyle(ChatFormatting.RED), true);
                }

                return OptionalInt.empty();
            } else {
                this.connection.send(new ClientboundOpenScreenPacket(abstractContainerMenu.containerId, abstractContainerMenu.getType(), factory.getDisplayName()));
                this.initMenu(abstractContainerMenu);
                this.containerMenu = abstractContainerMenu;
                return OptionalInt.of(this.containerCounter);
            }
        }
    }

    @Override
    public void sendMerchantOffers(int syncId, MerchantOffers offers, int levelProgress, int experience, boolean leveled, boolean refreshable) {
        this.connection.send(new ClientboundMerchantOffersPacket(syncId, offers, levelProgress, experience, leveled, refreshable));
    }

    @Override
    public void openHorseInventory(AbstractHorse horse, Container inventory) {
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer();
        }

        this.nextContainerCounter();
        this.connection.send(new ClientboundHorseScreenOpenPacket(this.containerCounter, inventory.getContainerSize(), horse.getId()));
        this.containerMenu = new HorseInventoryMenu(this.containerCounter, this.getInventory(), inventory, horse);
        this.initMenu(this.containerMenu);
    }

    @Override
    public void openItemGui(ItemStack book, InteractionHand hand) {
        if (book.is(Items.WRITTEN_BOOK)) {
            if (WrittenBookItem.resolveBookComponents(book, this.createCommandSourceStack(), this)) {
                this.containerMenu.broadcastChanges();
            }

            this.connection.send(new ClientboundOpenBookPacket(hand));
        }

    }

    @Override
    public void openCommandBlock(CommandBlockEntity commandBlock) {
        this.connection.send(ClientboundBlockEntityDataPacket.create(commandBlock, BlockEntity::saveWithoutMetadata));
    }

    @Override
    public void closeContainer() {
        this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
        this.doCloseContainer();
    }

    public void doCloseContainer() {
        this.containerMenu.removed(this);
        this.inventoryMenu.transferState(this.containerMenu);
        this.containerMenu = this.inventoryMenu;
    }

    public void setPlayerInput(float sidewaysSpeed, float forwardSpeed, boolean jumping, boolean sneaking) {
        if (this.isPassenger()) {
            if (sidewaysSpeed >= -1.0F && sidewaysSpeed <= 1.0F) {
                this.xxa = sidewaysSpeed;
            }

            if (forwardSpeed >= -1.0F && forwardSpeed <= 1.0F) {
                this.zza = forwardSpeed;
            }

            this.jumping = jumping;
            this.setShiftKeyDown(sneaking);
        }

    }

    @Override
    public void awardStat(Stat<?> stat, int amount) {
        this.stats.increment(this, stat, amount);
        this.getScoreboard().forAllObjectives(stat, this.getScoreboardName(), (score) -> {
            score.add(amount);
        });
    }

    @Override
    public void resetStat(Stat<?> stat) {
        this.stats.setValue(this, stat, 0);
        this.getScoreboard().forAllObjectives(stat, this.getScoreboardName(), Score::reset);
    }

    @Override
    public int awardRecipes(Collection<Recipe<?>> recipes) {
        return this.recipeBook.addRecipes(recipes, this);
    }

    @Override
    public void awardRecipesByKey(ResourceLocation[] ids) {
        List<Recipe<?>> list = Lists.newArrayList();

        for(ResourceLocation resourceLocation : ids) {
            this.server.getRecipeManager().byKey(resourceLocation).ifPresent(list::add);
        }

        this.awardRecipes(list);
    }

    @Override
    public int resetRecipes(Collection<Recipe<?>> recipes) {
        return this.recipeBook.removeRecipes(recipes, this);
    }

    @Override
    public void giveExperiencePoints(int experience) {
        super.giveExperiencePoints(experience);
        this.lastSentExp = -1;
    }

    public void disconnect() {
        this.disconnected = true;
        this.ejectPassengers();
        if (this.isSleeping()) {
            this.stopSleepInBed(true, false);
        }

    }

    public boolean hasDisconnected() {
        return this.disconnected;
    }

    public void resetSentInfo() {
        this.lastSentHealth = -1.0E8F;
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
        this.sendMessage(message, actionBar ? ChatType.GAME_INFO : ChatType.CHAT, Util.NIL_UUID);
    }

    @Override
    protected void completeUsingItem() {
        if (!this.useItem.isEmpty() && this.isUsingItem()) {
            this.connection.send(new ClientboundEntityEventPacket(this, (byte)9));
            super.completeUsingItem();
        }

    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor anchorPoint, Vec3 target) {
        super.lookAt(anchorPoint, target);
        this.connection.send(new ClientboundPlayerLookAtPacket(anchorPoint, target.x, target.y, target.z));
    }

    public void lookAt(EntityAnchorArgument.Anchor anchorPoint, Entity targetEntity, EntityAnchorArgument.Anchor targetAnchor) {
        Vec3 vec3 = targetAnchor.apply(targetEntity);
        super.lookAt(anchorPoint, vec3);
        this.connection.send(new ClientboundPlayerLookAtPacket(anchorPoint, targetEntity, targetAnchor));
    }

    public void restoreFrom(ServerPlayer oldPlayer, boolean alive) {
        this.textFilteringEnabled = oldPlayer.textFilteringEnabled;
        this.gameMode.setGameModeForPlayer(oldPlayer.gameMode.getGameModeForPlayer(), oldPlayer.gameMode.getPreviousGameModeForPlayer());
        if (alive) {
            this.getInventory().replaceWith(oldPlayer.getInventory());
            this.setHealth(oldPlayer.getHealth());
            this.foodData = oldPlayer.foodData;
            this.experienceLevel = oldPlayer.experienceLevel;
            this.totalExperience = oldPlayer.totalExperience;
            this.experienceProgress = oldPlayer.experienceProgress;
            this.setScore(oldPlayer.getScore());
            this.portalEntrancePos = oldPlayer.portalEntrancePos;
        } else if (this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || oldPlayer.isSpectator()) {
            this.getInventory().replaceWith(oldPlayer.getInventory());
            this.experienceLevel = oldPlayer.experienceLevel;
            this.totalExperience = oldPlayer.totalExperience;
            this.experienceProgress = oldPlayer.experienceProgress;
            this.setScore(oldPlayer.getScore());
        }

        this.enchantmentSeed = oldPlayer.enchantmentSeed;
        this.enderChestInventory = oldPlayer.enderChestInventory;
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, oldPlayer.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        this.lastSentExp = -1;
        this.lastSentHealth = -1.0F;
        this.lastSentFood = -1;
        this.recipeBook.copyOverData(oldPlayer.recipeBook);
        this.seenCredits = oldPlayer.seenCredits;
        this.enteredNetherPosition = oldPlayer.enteredNetherPosition;
        this.setShoulderEntityLeft(oldPlayer.getShoulderEntityLeft());
        this.setShoulderEntityRight(oldPlayer.getShoulderEntityRight());
    }

    @Override
    protected void onEffectAdded(MobEffectInstance effect, @Nullable Entity source) {
        super.onEffectAdded(effect, source);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effect));
        if (effect.getEffect() == MobEffects.LEVITATION) {
            this.levitationStartTime = this.tickCount;
            this.levitationStartPos = this.position();
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, source);
    }

    @Override
    protected void onEffectUpdated(MobEffectInstance effect, boolean reapplyEffect, @Nullable Entity source) {
        super.onEffectUpdated(effect, reapplyEffect, source);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effect));
        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, source);
    }

    @Override
    protected void onEffectRemoved(MobEffectInstance effect) {
        super.onEffectRemoved(effect);
        this.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), effect.getEffect()));
        if (effect.getEffect() == MobEffects.LEVITATION) {
            this.levitationStartPos = null;
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, (Entity)null);
    }

    @Override
    public void teleportTo(double destX, double destY, double destZ) {
        this.connection.teleport(destX, destY, destZ, this.getYRot(), this.getXRot());
    }

    @Override
    public void moveTo(double x, double y, double z) {
        this.teleportTo(x, y, z);
        this.connection.resetPosition();
    }

    @Override
    public void crit(Entity target) {
        this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(target, 4));
    }

    @Override
    public void magicCrit(Entity target) {
        this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(target, 5));
    }

    @Override
    public void onUpdateAbilities() {
        if (this.connection != null) {
            this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
            this.updateInvisibilityStatus();
        }
    }

    @Override
    public ServerLevel getLevel() {
        return (ServerLevel)this.level;
    }

    public boolean setGameMode(GameType gameMode) {
        if (!this.gameMode.changeGameModeForPlayer(gameMode)) {
            return false;
        } else {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float)gameMode.getId()));
            if (gameMode == GameType.SPECTATOR) {
                this.removeEntitiesOnShoulder();
                this.stopRiding();
            } else {
                this.setCamera(this);
            }

            this.onUpdateAbilities();
            this.updateEffectVisibility();
            return true;
        }
    }

    @Override
    public boolean isSpectator() {
        return this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        return this.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
    }

    @Override
    public void sendMessage(Component message, UUID sender) {
        this.sendMessage(message, ChatType.SYSTEM, sender);
    }

    public void sendMessage(Component message, ChatType type, UUID sender) {
        if (this.acceptsChat(type)) {
            this.connection.send(new ClientboundChatPacket(message, type, sender), (future) -> {
                if (!future.isSuccess() && (type == ChatType.GAME_INFO || type == ChatType.SYSTEM) && this.acceptsChat(ChatType.SYSTEM)) {
                    int i = 256;
                    String string = message.getString(256);
                    Component component2 = (new TextComponent(string)).withStyle(ChatFormatting.YELLOW);
                    this.connection.send(new ClientboundChatPacket((new TranslatableComponent("multiplayer.message_not_delivered", component2)).withStyle(ChatFormatting.RED), ChatType.SYSTEM, sender));
                }

            });
        }
    }

    public String getIpAddress() {
        String string = this.connection.connection.getRemoteAddress().toString();
        string = string.substring(string.indexOf("/") + 1);
        return string.substring(0, string.indexOf(":"));
    }

    public void updateOptions(ServerboundClientInformationPacket packet) {
        this.chatVisibility = packet.chatVisibility();
        this.canChatColor = packet.chatColors();
        this.textFilteringEnabled = packet.textFilteringEnabled();
        this.allowsListing = packet.allowsListing();
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte)packet.modelCustomisation());
        this.getEntityData().set(DATA_PLAYER_MAIN_HAND, (byte)(packet.mainHand() == HumanoidArm.LEFT ? 0 : 1));
    }

    public boolean canChatInColor() {
        return this.canChatColor;
    }

    public ChatVisiblity getChatVisibility() {
        return this.chatVisibility;
    }

    private boolean acceptsChat(ChatType type) {
        switch(this.chatVisibility) {
        case HIDDEN:
            return type == ChatType.GAME_INFO;
        case SYSTEM:
            return type == ChatType.SYSTEM || type == ChatType.GAME_INFO;
        case FULL:
        default:
            return true;
        }
    }

    public void sendTexturePack(String url, String hash, boolean required, @Nullable Component resourcePackPrompt) {
        this.connection.send(new ClientboundResourcePackPacket(url, hash, required, resourcePackPrompt));
    }

    @Override
    protected int getPermissionLevel() {
        return this.server.getProfilePermissions(this.getGameProfile());
    }

    public void resetLastActionTime() {
        this.lastActionTime = Util.getMillis();
    }

    public ServerStatsCounter getStats() {
        return this.stats;
    }

    public ServerRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    @Override
    protected void updateInvisibilityStatus() {
        if (this.isSpectator()) {
            this.removeEffectParticles();
            this.setInvisible(true);
        } else {
            super.updateInvisibilityStatus();
        }

    }

    public Entity getCamera() {
        return (Entity)(this.camera == null ? this : this.camera);
    }

    public void setCamera(@Nullable Entity entity) {
        Entity entity2 = this.getCamera();
        this.camera = (Entity)(entity == null ? this : entity);
        if (entity2 != this.camera) {
            this.connection.send(new ClientboundSetCameraPacket(this.camera));
            this.teleportTo(this.camera.getX(), this.camera.getY(), this.camera.getZ());
        }

    }

    @Override
    protected void processPortalCooldown() {
        if (!this.isChangingDimension) {
            super.processPortalCooldown();
        }

    }

    @Override
    public void attack(Entity target) {
        if (this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            this.setCamera(target);
        } else {
            super.attack(target);
        }

    }

    public long getLastActionTime() {
        return this.lastActionTime;
    }

    @Nullable
    public Component getTabListDisplayName() {
        return null;
    }

    @Override
    public void swing(InteractionHand hand) {
        super.swing(hand);
        this.resetAttackStrengthTicker();
    }

    public boolean isChangingDimension() {
        return this.isChangingDimension;
    }

    public void hasChangedDimension() {
        this.isChangingDimension = false;
    }

    public PlayerAdvancements getAdvancements() {
        return this.advancements;
    }

    public void teleportTo(ServerLevel targetWorld, double x, double y, double z, float yaw, float pitch) {
        this.setCamera(this);
        this.stopRiding();
        if (targetWorld == this.level) {
            this.connection.teleport(x, y, z, yaw, pitch);
        } else {
            ServerLevel serverLevel = this.getLevel();
            LevelData levelData = targetWorld.getLevelData();
            this.connection.send(new ClientboundRespawnPacket(targetWorld.dimensionTypeRegistration(), targetWorld.dimension(), BiomeManager.obfuscateSeed(targetWorld.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), targetWorld.isDebug(), targetWorld.isFlat(), true));
            this.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
            this.server.getPlayerList().sendPlayerPermissionLevel(this);
            serverLevel.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            this.moveTo(x, y, z, yaw, pitch);
            this.setLevel(targetWorld);
            targetWorld.addDuringCommandTeleport(this);
            this.triggerDimensionChangeTriggers(serverLevel);
            this.connection.teleport(x, y, z, yaw, pitch);
            this.server.getPlayerList().sendLevelInfo(this, targetWorld);
            this.server.getPlayerList().sendAllPlayerInfo(this);
        }

    }

    @Nullable
    public BlockPos getRespawnPosition() {
        return this.respawnPosition;
    }

    public float getRespawnAngle() {
        return this.respawnAngle;
    }

    public ResourceKey<Level> getRespawnDimension() {
        return this.respawnDimension;
    }

    public boolean isRespawnForced() {
        return this.respawnForced;
    }

    public void setRespawnPosition(ResourceKey<Level> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage) {
        if (pos != null) {
            boolean bl = pos.equals(this.respawnPosition) && dimension.equals(this.respawnDimension);
            if (sendMessage && !bl) {
                this.sendMessage(new TranslatableComponent("block.minecraft.set_spawn"), Util.NIL_UUID);
            }

            this.respawnPosition = pos;
            this.respawnDimension = dimension;
            this.respawnAngle = angle;
            this.respawnForced = forced;
        } else {
            this.respawnPosition = null;
            this.respawnDimension = Level.OVERWORLD;
            this.respawnAngle = 0.0F;
            this.respawnForced = false;
        }

    }

    public void trackChunk(ChunkPos chunkPos, Packet<?> chunkDataPacket) {
        this.connection.send(chunkDataPacket);
    }

    public void untrackChunk(ChunkPos chunkPos) {
        if (this.isAlive()) {
            this.connection.send(new ClientboundForgetLevelChunkPacket(chunkPos.x, chunkPos.z));
        }

    }

    public SectionPos getLastSectionPos() {
        return this.lastSectionPos;
    }

    public void setLastSectionPos(SectionPos section) {
        this.lastSectionPos = section;
    }

    @Override
    public void playNotifySound(SoundEvent event, SoundSource category, float volume, float pitch) {
        this.connection.send(new ClientboundSoundPacket(event, category, this.getX(), this.getY(), this.getZ(), volume, pitch));
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddPlayerPacket(this);
    }

    @Override
    public ItemEntity drop(ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        ItemEntity itemEntity = super.drop(stack, throwRandomly, retainOwnership);
        if (itemEntity == null) {
            return null;
        } else {
            this.level.addFreshEntity(itemEntity);
            ItemStack itemStack = itemEntity.getItem();
            if (retainOwnership) {
                if (!itemStack.isEmpty()) {
                    this.awardStat(Stats.ITEM_DROPPED.get(itemStack.getItem()), stack.getCount());
                }

                this.awardStat(Stats.DROP);
            }

            return itemEntity;
        }
    }

    public TextFilter getTextFilter() {
        return this.textFilter;
    }

    public void setLevel(ServerLevel world) {
        this.level = world;
        this.gameMode.setLevel(world);
    }

    @Nullable
    private static GameType readPlayerMode(@Nullable CompoundTag nbt, String key) {
        return nbt != null && nbt.contains(key, 99) ? GameType.byId(nbt.getInt(key)) : null;
    }

    private GameType calculateGameModeForNewPlayer(@Nullable GameType backupGameMode) {
        GameType gameType = this.server.getForcedGameType();
        if (gameType != null) {
            return gameType;
        } else {
            return backupGameMode != null ? backupGameMode : this.server.getDefaultGameType();
        }
    }

    public void loadGameTypes(@Nullable CompoundTag nbt) {
        this.gameMode.setGameModeForPlayer(this.calculateGameModeForNewPlayer(readPlayerMode(nbt, "playerGameType")), readPlayerMode(nbt, "previousPlayerGameType"));
    }

    private void storeGameTypes(CompoundTag nbt) {
        nbt.putInt("playerGameType", this.gameMode.getGameModeForPlayer().getId());
        GameType gameType = this.gameMode.getPreviousGameModeForPlayer();
        if (gameType != null) {
            nbt.putInt("previousPlayerGameType", gameType.getId());
        }

    }

    public boolean isTextFilteringEnabled() {
        return this.textFilteringEnabled;
    }

    public boolean shouldFilterMessageTo(ServerPlayer player) {
        if (player == this) {
            return false;
        } else {
            return this.textFilteringEnabled || player.textFilteringEnabled;
        }
    }

    @Override
    public boolean mayInteract(Level world, BlockPos pos) {
        return super.mayInteract(world, pos) && world.mayInteract(this, pos);
    }

    @Override
    protected void updateUsingItem(ItemStack stack) {
        CriteriaTriggers.USING_ITEM.trigger(this, stack);
        super.updateUsingItem(stack);
    }

    public boolean drop(boolean entireStack) {
        Inventory inventory = this.getInventory();
        ItemStack itemStack = inventory.removeFromSelected(entireStack);
        this.containerMenu.findSlot(inventory, inventory.selected).ifPresent((i) -> {
            this.containerMenu.setRemoteSlot(i, inventory.getSelected());
        });
        return this.drop(itemStack, false, true) != null;
    }

    public boolean allowsListing() {
        return this.allowsListing;
    }
}
