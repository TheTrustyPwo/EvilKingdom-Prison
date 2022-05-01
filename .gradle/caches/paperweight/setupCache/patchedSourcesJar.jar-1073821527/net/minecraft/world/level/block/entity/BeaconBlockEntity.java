package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public class BeaconBlockEntity extends BlockEntity implements MenuProvider {
    private static final int MAX_LEVELS = 4;
    public static final MobEffect[][] BEACON_EFFECTS = new MobEffect[][]{{MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED}, {MobEffects.DAMAGE_RESISTANCE, MobEffects.JUMP}, {MobEffects.DAMAGE_BOOST}, {MobEffects.REGENERATION}};
    private static final Set<MobEffect> VALID_EFFECTS = Arrays.stream(BEACON_EFFECTS).flatMap(Arrays::stream).collect(Collectors.toSet());
    public static final int DATA_LEVELS = 0;
    public static final int DATA_PRIMARY = 1;
    public static final int DATA_SECONDARY = 2;
    public static final int NUM_DATA_VALUES = 3;
    private static final int BLOCKS_CHECK_PER_TICK = 10;
    List<BeaconBlockEntity.BeaconBeamSection> beamSections = Lists.newArrayList();
    private List<BeaconBlockEntity.BeaconBeamSection> checkingBeamSections = Lists.newArrayList();
    public int levels;
    private int lastCheckY;
    @Nullable
    public MobEffect primaryPower;
    @Nullable
    public MobEffect secondaryPower;
    @Nullable
    public Component name;
    public LockCode lockKey = LockCode.NO_LOCK;
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            switch(index) {
            case 0:
                return BeaconBlockEntity.this.levels;
            case 1:
                return MobEffect.getId(BeaconBlockEntity.this.primaryPower);
            case 2:
                return MobEffect.getId(BeaconBlockEntity.this.secondaryPower);
            default:
                return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch(index) {
            case 0:
                BeaconBlockEntity.this.levels = value;
                break;
            case 1:
                if (!BeaconBlockEntity.this.level.isClientSide && !BeaconBlockEntity.this.beamSections.isEmpty()) {
                    BeaconBlockEntity.playSound(BeaconBlockEntity.this.level, BeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
                }

                BeaconBlockEntity.this.primaryPower = BeaconBlockEntity.getValidEffectById(value);
                break;
            case 2:
                BeaconBlockEntity.this.secondaryPower = BeaconBlockEntity.getValidEffectById(value);
            }

        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public BeaconBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.BEACON, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        BlockPos blockPos;
        if (blockEntity.lastCheckY < j) {
            blockPos = pos;
            blockEntity.checkingBeamSections = Lists.newArrayList();
            blockEntity.lastCheckY = pos.getY() - 1;
        } else {
            blockPos = new BlockPos(i, blockEntity.lastCheckY + 1, k);
        }

        BeaconBlockEntity.BeaconBeamSection beaconBeamSection = blockEntity.checkingBeamSections.isEmpty() ? null : blockEntity.checkingBeamSections.get(blockEntity.checkingBeamSections.size() - 1);
        int l = world.getHeight(Heightmap.Types.WORLD_SURFACE, i, k);

        for(int m = 0; m < 10 && blockPos.getY() <= l; ++m) {
            BlockState blockState = world.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if (block instanceof BeaconBeamBlock) {
                float[] fs = ((BeaconBeamBlock)block).getColor().getTextureDiffuseColors();
                if (blockEntity.checkingBeamSections.size() <= 1) {
                    beaconBeamSection = new BeaconBlockEntity.BeaconBeamSection(fs);
                    blockEntity.checkingBeamSections.add(beaconBeamSection);
                } else if (beaconBeamSection != null) {
                    if (Arrays.equals(fs, beaconBeamSection.color)) {
                        beaconBeamSection.increaseHeight();
                    } else {
                        beaconBeamSection = new BeaconBlockEntity.BeaconBeamSection(new float[]{(beaconBeamSection.color[0] + fs[0]) / 2.0F, (beaconBeamSection.color[1] + fs[1]) / 2.0F, (beaconBeamSection.color[2] + fs[2]) / 2.0F});
                        blockEntity.checkingBeamSections.add(beaconBeamSection);
                    }
                }
            } else {
                if (beaconBeamSection == null || blockState.getLightBlock(world, blockPos) >= 15 && !blockState.is(Blocks.BEDROCK)) {
                    blockEntity.checkingBeamSections.clear();
                    blockEntity.lastCheckY = l;
                    break;
                }

                beaconBeamSection.increaseHeight();
            }

            blockPos = blockPos.above();
            ++blockEntity.lastCheckY;
        }

        int n = blockEntity.levels;
        if (world.getGameTime() % 80L == 0L) {
            if (!blockEntity.beamSections.isEmpty()) {
                blockEntity.levels = updateBase(world, i, j, k);
            }

            if (blockEntity.levels > 0 && !blockEntity.beamSections.isEmpty()) {
                applyEffects(world, pos, blockEntity.levels, blockEntity.primaryPower, blockEntity.secondaryPower);
                playSound(world, pos, SoundEvents.BEACON_AMBIENT);
            }
        }

        if (blockEntity.lastCheckY >= l) {
            blockEntity.lastCheckY = world.getMinBuildHeight() - 1;
            boolean bl = n > 0;
            blockEntity.beamSections = blockEntity.checkingBeamSections;
            if (!world.isClientSide) {
                boolean bl2 = blockEntity.levels > 0;
                if (!bl && bl2) {
                    playSound(world, pos, SoundEvents.BEACON_ACTIVATE);

                    for(ServerPlayer serverPlayer : world.getEntitiesOfClass(ServerPlayer.class, (new AABB((double)i, (double)j, (double)k, (double)i, (double)(j - 4), (double)k)).inflate(10.0D, 5.0D, 10.0D))) {
                        CriteriaTriggers.CONSTRUCT_BEACON.trigger(serverPlayer, blockEntity.levels);
                    }
                } else if (bl && !bl2) {
                    playSound(world, pos, SoundEvents.BEACON_DEACTIVATE);
                }
            }
        }

    }

    private static int updateBase(Level world, int x, int y, int z) {
        int i = 0;

        for(int j = 1; j <= 4; i = j++) {
            int k = y - j;
            if (k < world.getMinBuildHeight()) {
                break;
            }

            boolean bl = true;

            for(int l = x - j; l <= x + j && bl; ++l) {
                for(int m = z - j; m <= z + j; ++m) {
                    if (!world.getBlockState(new BlockPos(l, k, m)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                        bl = false;
                        break;
                    }
                }
            }

            if (!bl) {
                break;
            }
        }

        return i;
    }

    @Override
    public void setRemoved() {
        playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    private static void applyEffects(Level world, BlockPos pos, int beaconLevel, @Nullable MobEffect primaryEffect, @Nullable MobEffect secondaryEffect) {
        if (!world.isClientSide && primaryEffect != null) {
            double d = (double)(beaconLevel * 10 + 10);
            int i = 0;
            if (beaconLevel >= 4 && primaryEffect == secondaryEffect) {
                i = 1;
            }

            int j = (9 + beaconLevel * 2) * 20;
            AABB aABB = (new AABB(pos)).inflate(d).expandTowards(0.0D, (double)world.getHeight(), 0.0D);
            List<Player> list = world.getEntitiesOfClass(Player.class, aABB);

            for(Player player : list) {
                player.addEffect(new MobEffectInstance(primaryEffect, j, i, true, true));
            }

            if (beaconLevel >= 4 && primaryEffect != secondaryEffect && secondaryEffect != null) {
                for(Player player2 : list) {
                    player2.addEffect(new MobEffectInstance(secondaryEffect, j, 0, true, true));
                }
            }

        }
    }

    public static void playSound(Level world, BlockPos pos, SoundEvent sound) {
        world.playSound((Player)null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public List<BeaconBlockEntity.BeaconBeamSection> getBeamSections() {
        return (List<BeaconBlockEntity.BeaconBeamSection>)(this.levels == 0 ? ImmutableList.of() : this.beamSections);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    static MobEffect getValidEffectById(int id) {
        MobEffect mobEffect = MobEffect.byId(id);
        return VALID_EFFECTS.contains(mobEffect) ? mobEffect : null;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.primaryPower = getValidEffectById(nbt.getInt("Primary"));
        this.secondaryPower = getValidEffectById(nbt.getInt("Secondary"));
        if (nbt.contains("CustomName", 8)) {
            this.name = Component.Serializer.fromJson(nbt.getString("CustomName"));
        }

        this.lockKey = LockCode.fromTag(nbt);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("Primary", MobEffect.getId(this.primaryPower));
        nbt.putInt("Secondary", MobEffect.getId(this.secondaryPower));
        nbt.putInt("Levels", this.levels);
        if (this.name != null) {
            nbt.putString("CustomName", Component.Serializer.toJson(this.name));
        }

        this.lockKey.addToTag(nbt);
    }

    public void setCustomName(@Nullable Component customName) {
        this.name = customName;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return BaseContainerBlockEntity.canUnlock(player, this.lockKey, this.getDisplayName()) ? new BeaconMenu(syncId, inv, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos())) : null;
    }

    @Override
    public Component getDisplayName() {
        return (Component)(this.name != null ? this.name : new TranslatableComponent("container.beacon"));
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
        this.lastCheckY = world.getMinBuildHeight() - 1;
    }

    public static class BeaconBeamSection {
        final float[] color;
        private int height;

        public BeaconBeamSection(float[] color) {
            this.color = color;
            this.height = 1;
        }

        protected void increaseHeight() {
            ++this.height;
        }

        public float[] getColor() {
            return this.color;
        }

        public int getHeight() {
            return this.height;
        }
    }
}
