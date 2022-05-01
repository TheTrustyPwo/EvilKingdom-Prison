package net.minecraft.world.level.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class Block extends BlockBehaviour implements ItemLike {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Holder.Reference<Block> builtInRegistryHolder = Registry.BLOCK.createIntrusiveHolder(this);
    public static final IdMapper<BlockState> BLOCK_STATE_REGISTRY = new IdMapper<>();
    private static final LoadingCache<VoxelShape, Boolean> SHAPE_FULL_BLOCK_CACHE = CacheBuilder.newBuilder().maximumSize(512L).weakKeys().build(new CacheLoader<VoxelShape, Boolean>() {
        @Override
        public Boolean load(VoxelShape voxelShape) {
            return !Shapes.joinIsNotEmpty(Shapes.block(), voxelShape, BooleanOp.NOT_SAME);
        }
    });
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_INVISIBLE = 4;
    public static final int UPDATE_IMMEDIATE = 8;
    public static final int UPDATE_KNOWN_SHAPE = 16;
    public static final int UPDATE_SUPPRESS_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int UPDATE_SUPPRESS_LIGHT = 128;
    public static final int UPDATE_NONE = 4;
    public static final int UPDATE_ALL = 3;
    public static final int UPDATE_ALL_IMMEDIATE = 11;
    public static final float INDESTRUCTIBLE = -1.0F;
    public static final float INSTANT = 0.0F;
    public static final int UPDATE_LIMIT = 512;
    protected final StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultBlockState;
    @Nullable
    private String descriptionId;
    @Nullable
    private Item item;
    private static final int CACHE_SIZE = 2048;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2ByteLinkedOpenHashMap = new Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>(2048, 0.25F) {
            protected void rehash(int i) {
            }
        };
        object2ByteLinkedOpenHashMap.defaultReturnValue((byte)127);
        return object2ByteLinkedOpenHashMap;
    });

    public static int getId(@Nullable BlockState state) {
        if (state == null) {
            return 0;
        } else {
            int i = BLOCK_STATE_REGISTRY.getId(state);
            return i == -1 ? 0 : i;
        }
    }

    public static BlockState stateById(int stateId) {
        BlockState blockState = BLOCK_STATE_REGISTRY.byId(stateId);
        return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
    }

    public static Block byItem(@Nullable Item item) {
        return item instanceof BlockItem ? ((BlockItem)item).getBlock() : Blocks.AIR;
    }

    public static BlockState pushEntitiesUp(BlockState from, BlockState to, Level world, BlockPos pos) {
        VoxelShape voxelShape = Shapes.joinUnoptimized(from.getCollisionShape(world, pos), to.getCollisionShape(world, pos), BooleanOp.ONLY_SECOND).move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        if (voxelShape.isEmpty()) {
            return to;
        } else {
            for(Entity entity : world.getEntities((Entity)null, voxelShape.bounds())) {
                double d = Shapes.collide(Direction.Axis.Y, entity.getBoundingBox().move(0.0D, 1.0D, 0.0D), List.of(voxelShape), -1.0D);
                entity.teleportTo(entity.getX(), entity.getY() + 1.0D + d, entity.getZ());
            }

            return to;
        }
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return Shapes.box(minX / 16.0D, minY / 16.0D, minZ / 16.0D, maxX / 16.0D, maxY / 16.0D, maxZ / 16.0D);
    }

    public static BlockState updateFromNeighbourShapes(BlockState state, LevelAccessor world, BlockPos pos) {
        BlockState blockState = state;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(Direction direction : UPDATE_SHAPE_ORDER) {
            mutableBlockPos.setWithOffset(pos, direction);
            blockState = blockState.updateShape(direction, world.getBlockState(mutableBlockPos), world, pos, mutableBlockPos);
        }

        return blockState;
    }

    public static void updateOrDestroy(BlockState state, BlockState newState, LevelAccessor world, BlockPos pos, int flags) {
        updateOrDestroy(state, newState, world, pos, flags, 512);
    }

    public static void updateOrDestroy(BlockState state, BlockState newState, LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {
        if (newState != state) {
            if (newState.isAir()) {
                if (!world.isClientSide()) {
                    world.destroyBlock(pos, (flags & 32) == 0, (Entity)null, maxUpdateDepth);
                }
            } else {
                world.setBlock(pos, newState, flags & -33, maxUpdateDepth);
            }
        }

    }

    public Block(BlockBehaviour.Properties settings) {
        super(settings);
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(this);
        this.createBlockStateDefinition(builder);
        this.stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
        this.registerDefaultState(this.stateDefinition.any());
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            String string = this.getClass().getSimpleName();
            if (!string.endsWith("Block")) {
                LOGGER.error("Block classes should end with Block and {} doesn't.", (Object)string);
            }
        }

    }

    public static boolean isExceptionForConnection(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || state.is(Blocks.BARRIER) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN) || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || state.is(BlockTags.SHULKER_BOXES);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return this.isRandomlyTicking;
    }

    public static boolean shouldRenderFace(BlockState state, BlockGetter world, BlockPos pos, Direction side, BlockPos otherPos) {
        BlockState blockState = world.getBlockState(otherPos);
        if (state.skipRendering(blockState, side)) {
            return false;
        } else if (blockState.canOcclude()) {
            Block.BlockStatePairKey blockStatePairKey = new Block.BlockStatePairKey(state, blockState, side);
            Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2ByteLinkedOpenHashMap = OCCLUSION_CACHE.get();
            byte b = object2ByteLinkedOpenHashMap.getAndMoveToFirst(blockStatePairKey);
            if (b != 127) {
                return b != 0;
            } else {
                VoxelShape voxelShape = state.getFaceOcclusionShape(world, pos, side);
                if (voxelShape.isEmpty()) {
                    return true;
                } else {
                    VoxelShape voxelShape2 = blockState.getFaceOcclusionShape(world, otherPos, side.getOpposite());
                    boolean bl = Shapes.joinIsNotEmpty(voxelShape, voxelShape2, BooleanOp.ONLY_FIRST);
                    if (object2ByteLinkedOpenHashMap.size() == 2048) {
                        object2ByteLinkedOpenHashMap.removeLastByte();
                    }

                    object2ByteLinkedOpenHashMap.putAndMoveToFirst(blockStatePairKey, (byte)(bl ? 1 : 0));
                    return bl;
                }
            }
        } else {
            return true;
        }
    }

    public static boolean canSupportRigidBlock(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos).isFaceSturdy(world, pos, Direction.UP, SupportType.RIGID);
    }

    public static boolean canSupportCenter(LevelReader world, BlockPos pos, Direction side) {
        BlockState blockState = world.getBlockState(pos);
        return side == Direction.DOWN && blockState.is(BlockTags.UNSTABLE_BOTTOM_CENTER) ? false : blockState.isFaceSturdy(world, pos, side, SupportType.CENTER);
    }

    public static boolean isFaceFull(VoxelShape shape, Direction side) {
        VoxelShape voxelShape = shape.getFaceShape(side);
        return isShapeFullBlock(voxelShape);
    }

    public static boolean isShapeFullBlock(VoxelShape shape) {
        return SHAPE_FULL_BLOCK_CACHE.getUnchecked(shape);
    }

    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return !isShapeFullBlock(state.getShape(world, pos)) && state.getFluidState().isEmpty();
    }

    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
    }

    public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
    }

    public static List<ItemStack> getDrops(BlockState state, ServerLevel world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        LootContext.Builder builder = (new LootContext.Builder(world)).withRandom(world.random).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        return state.getDrops(builder);
    }

    public static List<ItemStack> getDrops(BlockState state, ServerLevel world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack stack) {
        LootContext.Builder builder = (new LootContext.Builder(world)).withRandom(world.random).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, stack).withOptionalParameter(LootContextParams.THIS_ENTITY, entity).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        return state.getDrops(builder);
    }

    public static void dropResources(BlockState state, LootContext.Builder lootContext) {
        ServerLevel serverLevel = lootContext.getLevel();
        BlockPos blockPos = new BlockPos(lootContext.getParameter(LootContextParams.ORIGIN));
        state.getDrops(lootContext).forEach((stack) -> {
            popResource(serverLevel, blockPos, stack);
        });
        state.spawnAfterBreak(serverLevel, blockPos, ItemStack.EMPTY);
    }

    public static void dropResources(BlockState state, Level world, BlockPos pos) {
        if (world instanceof ServerLevel) {
            getDrops(state, (ServerLevel)world, pos, (BlockEntity)null).forEach((stack) -> {
                popResource(world, pos, stack);
            });
            state.spawnAfterBreak((ServerLevel)world, pos, ItemStack.EMPTY);
        }

    }

    public static void dropResources(BlockState state, LevelAccessor world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (world instanceof ServerLevel) {
            getDrops(state, (ServerLevel)world, pos, blockEntity).forEach((stack) -> {
                popResource((ServerLevel)world, pos, stack);
            });
            state.spawnAfterBreak((ServerLevel)world, pos, ItemStack.EMPTY);
        }

    }

    public static void dropResources(BlockState state, Level world, BlockPos pos, @Nullable BlockEntity blockEntity, Entity entity, ItemStack stack) {
        if (world instanceof ServerLevel) {
            getDrops(state, (ServerLevel)world, pos, blockEntity, entity, stack).forEach((stackx) -> {
                popResource(world, pos, stackx);
            });
            state.spawnAfterBreak((ServerLevel)world, pos, stack);
        }

    }

    public static void popResource(Level world, BlockPos pos, ItemStack stack) {
        float f = EntityType.ITEM.getHeight() / 2.0F;
        double d = (double)((float)pos.getX() + 0.5F) + Mth.nextDouble(world.random, -0.25D, 0.25D);
        double e = (double)((float)pos.getY() + 0.5F) + Mth.nextDouble(world.random, -0.25D, 0.25D) - (double)f;
        double g = (double)((float)pos.getZ() + 0.5F) + Mth.nextDouble(world.random, -0.25D, 0.25D);
        popResource(world, () -> {
            return new ItemEntity(world, d, e, g, stack);
        }, stack);
    }

    public static void popResourceFromFace(Level world, BlockPos pos, Direction direction, ItemStack stack) {
        int i = direction.getStepX();
        int j = direction.getStepY();
        int k = direction.getStepZ();
        float f = EntityType.ITEM.getWidth() / 2.0F;
        float g = EntityType.ITEM.getHeight() / 2.0F;
        double d = (double)((float)pos.getX() + 0.5F) + (i == 0 ? Mth.nextDouble(world.random, -0.25D, 0.25D) : (double)((float)i * (0.5F + f)));
        double e = (double)((float)pos.getY() + 0.5F) + (j == 0 ? Mth.nextDouble(world.random, -0.25D, 0.25D) : (double)((float)j * (0.5F + g))) - (double)g;
        double h = (double)((float)pos.getZ() + 0.5F) + (k == 0 ? Mth.nextDouble(world.random, -0.25D, 0.25D) : (double)((float)k * (0.5F + f)));
        double l = i == 0 ? Mth.nextDouble(world.random, -0.1D, 0.1D) : (double)i * 0.1D;
        double m = j == 0 ? Mth.nextDouble(world.random, 0.0D, 0.1D) : (double)j * 0.1D + 0.1D;
        double n = k == 0 ? Mth.nextDouble(world.random, -0.1D, 0.1D) : (double)k * 0.1D;
        popResource(world, () -> {
            return new ItemEntity(world, d, e, h, stack, l, m, n);
        }, stack);
    }

    private static void popResource(Level world, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack) {
        if (!world.isClientSide && !stack.isEmpty() && world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ItemEntity itemEntity = itemEntitySupplier.get();
            itemEntity.setDefaultPickUpDelay();
            world.addFreshEntity(itemEntity);
        }
    }

    public void popExperience(ServerLevel world, BlockPos pos, int size) {
        if (world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ExperienceOrb.award(world, Vec3.atCenterOf(pos), size);
        }

    }

    public float getExplosionResistance() {
        return this.explosionResistance;
    }

    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
    }

    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState();
    }

    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        dropResources(state, world, pos, blockEntity, player, stack);
    }

    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
    }

    public boolean isPossibleToRespawnInThis() {
        return !this.material.isSolid() && !this.material.isLiquid();
    }

    public MutableComponent getName() {
        return new TranslatableComponent(this.getDescriptionId());
    }

    public String getDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("block", Registry.BLOCK.getKey(this));
        }

        return this.descriptionId;
    }

    public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, DamageSource.FALL);
    }

    public void updateEntityAfterFallOn(BlockGetter world, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
    }

    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        stacks.add(new ItemStack(this));
    }

    public float getFriction() {
        return this.friction;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public float getJumpFactor() {
        return this.jumpFactor;
    }

    protected void spawnDestroyParticles(Level world, Player player, BlockPos pos, BlockState state) {
        world.levelEvent(player, 2001, pos, getId(state));
    }

    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        this.spawnDestroyParticles(world, player, pos, state);
        if (state.is(BlockTags.GUARDED_BY_PIGLINS)) {
            PiglinAi.angerNearbyPiglins(player, false);
        }

        world.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
    }

    public void handlePrecipitation(BlockState state, Level world, BlockPos pos, Biome.Precipitation precipitation) {
    }

    public boolean dropFromExplosion(Explosion explosion) {
        return true;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    }

    public StateDefinition<Block, BlockState> getStateDefinition() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(BlockState state) {
        this.defaultBlockState = state;
    }

    public final BlockState defaultBlockState() {
        return this.defaultBlockState;
    }

    public final BlockState withPropertiesOf(BlockState state) {
        BlockState blockState = this.defaultBlockState();

        for(Property<?> property : state.getBlock().getStateDefinition().getProperties()) {
            if (blockState.hasProperty(property)) {
                blockState = copyProperty(state, blockState, property);
            }
        }

        return blockState;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState source, BlockState target, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }

    public SoundType getSoundType(BlockState state) {
        return this.soundType;
    }

    @Override
    public Item asItem() {
        if (this.item == null) {
            this.item = Item.byBlock(this);
        }

        return this.item;
    }

    public boolean hasDynamicShape() {
        return this.dynamicShape;
    }

    @Override
    public String toString() {
        return "Block{" + Registry.BLOCK.getKey(this) + "}";
    }

    public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
    }

    @Override
    protected Block asBlock() {
        return this;
    }

    protected ImmutableMap<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> stateToShape) {
        return this.stateDefinition.getPossibleStates().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), stateToShape));
    }

    /** @deprecated */
    @Deprecated
    public Holder.Reference<Block> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    public static final class BlockStatePairKey {
        private final BlockState first;
        private final BlockState second;
        private final Direction direction;

        public BlockStatePairKey(BlockState self, BlockState other, Direction facing) {
            this.first = self;
            this.second = other;
            this.direction = facing;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof Block.BlockStatePairKey)) {
                return false;
            } else {
                Block.BlockStatePairKey blockStatePairKey = (Block.BlockStatePairKey)object;
                return this.first == blockStatePairKey.first && this.second == blockStatePairKey.second && this.direction == blockStatePairKey.direction;
            }
        }

        @Override
        public int hashCode() {
            int i = this.first.hashCode();
            i = 31 * i + this.second.hashCode();
            return 31 * i + this.direction.hashCode();
        }
    }
}
