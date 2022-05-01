package net.minecraft.world.level.block.state;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockBehaviour {
    protected static final Direction[] UPDATE_SHAPE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP};
    protected final Material material;
    public final boolean hasCollision;
    protected final float explosionResistance;
    protected final boolean isRandomlyTicking;
    protected final SoundType soundType;
    protected final float friction;
    protected final float speedFactor;
    protected final float jumpFactor;
    protected final boolean dynamicShape;
    protected final BlockBehaviour.Properties properties;
    @Nullable
    protected ResourceLocation drops;

    public BlockBehaviour(BlockBehaviour.Properties settings) {
        this.material = settings.material;
        this.hasCollision = settings.hasCollision;
        this.drops = settings.drops;
        this.explosionResistance = settings.explosionResistance;
        this.isRandomlyTicking = settings.isRandomlyTicking;
        this.soundType = settings.soundType;
        this.friction = settings.friction;
        this.speedFactor = settings.speedFactor;
        this.jumpFactor = settings.jumpFactor;
        this.dynamicShape = settings.dynamicShape;
        this.properties = settings;
    }

    /** @deprecated */
    @Deprecated
    public void updateIndirectNeighbourShapes(BlockState state, LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {
    }

    /** @deprecated */
    @Deprecated
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        switch(type) {
        case LAND:
            return !state.isCollisionShapeFullBlock(world, pos);
        case WATER:
            return world.getFluidState(pos).is(FluidTags.WATER);
        case AIR:
            return !state.isCollisionShapeFullBlock(world, pos);
        default:
            return false;
        }
    }

    /** @deprecated */
    @Deprecated
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    /** @deprecated */
    @Deprecated
    public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
        return false;
    }

    /** @deprecated */
    @Deprecated
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        DebugPackets.sendNeighborsUpdatePacket(world, pos);
    }

    /** @deprecated */
    @Deprecated
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
    }

    /** @deprecated */
    @Deprecated
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            world.removeBlockEntity(pos);
        }

    }

    /** @deprecated */
    @Deprecated
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    /** @deprecated */
    @Deprecated
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        return false;
    }

    /** @deprecated */
    @Deprecated
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** @deprecated */
    @Deprecated
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    /** @deprecated */
    @Deprecated
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    /** @deprecated */
    @Deprecated
    public PushReaction getPistonPushReaction(BlockState state) {
        return this.material.getPushReaction();
    }

    /** @deprecated */
    @Deprecated
    public FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }

    /** @deprecated */
    @Deprecated
    public boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    public BlockBehaviour.OffsetType getOffsetType() {
        return BlockBehaviour.OffsetType.NONE;
    }

    public float getMaxHorizontalOffset() {
        return 0.25F;
    }

    public float getMaxVerticalOffset() {
        return 0.2F;
    }

    /** @deprecated */
    @Deprecated
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    /** @deprecated */
    @Deprecated
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    /** @deprecated */
    @Deprecated
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return this.material.isReplaceable() && (context.getItemInHand().isEmpty() || !context.getItemInHand().is(this.asItem()));
    }

    /** @deprecated */
    @Deprecated
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        return this.material.isReplaceable() || !this.material.isSolid();
    }

    /** @deprecated */
    @Deprecated
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        ResourceLocation resourceLocation = this.getLootTable();
        if (resourceLocation == BuiltInLootTables.EMPTY) {
            return Collections.emptyList();
        } else {
            LootContext lootContext = builder.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
            ServerLevel serverLevel = lootContext.getLevel();
            LootTable lootTable = serverLevel.getServer().getLootTables().get(resourceLocation);
            return lootTable.getRandomItems(lootContext);
        }
    }

    /** @deprecated */
    @Deprecated
    public long getSeed(BlockState state, BlockPos pos) {
        return Mth.getSeed(pos);
    }

    /** @deprecated */
    @Deprecated
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return state.getShape(world, pos);
    }

    /** @deprecated */
    @Deprecated
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
        return this.getCollisionShape(state, world, pos, CollisionContext.empty());
    }

    /** @deprecated */
    @Deprecated
    public VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return Shapes.empty();
    }

    /** @deprecated */
    @Deprecated
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        if (state.isSolidRender(world, pos)) {
            return world.getMaxLightLevel();
        } else {
            return state.propagatesSkylightDown(world, pos) ? 0 : 1;
        }
    }

    /** @deprecated */
    @Nullable
    @Deprecated
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return null;
    }

    /** @deprecated */
    @Deprecated
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return true;
    }

    /** @deprecated */
    @Deprecated
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return state.isCollisionShapeFullBlock(world, pos) ? 0.2F : 1.0F;
    }

    /** @deprecated */
    @Deprecated
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return 0;
    }

    /** @deprecated */
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** @deprecated */
    @Deprecated
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.hasCollision ? state.getShape(world, pos) : Shapes.empty();
    }

    /** @deprecated */
    @Deprecated
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return Block.isShapeFullBlock(state.getCollisionShape(world, pos));
    }

    /** @deprecated */
    @Deprecated
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.getCollisionShape(state, world, pos, context);
    }

    /** @deprecated */
    @Deprecated
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        this.tick(state, world, pos, random);
    }

    /** @deprecated */
    @Deprecated
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
    }

    /** @deprecated */
    @Deprecated
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        float f = state.getDestroySpeed(world, pos);
        if (f == -1.0F) {
            return 0.0F;
        } else {
            int i = player.hasCorrectToolForDrops(state) ? 30 : 100;
            return player.getDestroySpeed(state) / f / (float)i;
        }
    }

    /** @deprecated */
    @Deprecated
    public void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack stack) {
    }

    /** @deprecated */
    @Deprecated
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {
    }

    /** @deprecated */
    @Deprecated
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return 0;
    }

    /** @deprecated */
    @Deprecated
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
    }

    /** @deprecated */
    @Deprecated
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return 0;
    }

    public final ResourceLocation getLootTable() {
        if (this.drops == null) {
            ResourceLocation resourceLocation = Registry.BLOCK.getKey(this.asBlock());
            this.drops = new ResourceLocation(resourceLocation.getNamespace(), "blocks/" + resourceLocation.getPath());
        }

        return this.drops;
    }

    /** @deprecated */
    @Deprecated
    public void onProjectileHit(Level world, BlockState state, BlockHitResult hit, Projectile projectile) {
    }

    public abstract Item asItem();

    protected abstract Block asBlock();

    public MaterialColor defaultMaterialColor() {
        return this.properties.materialColor.apply(this.asBlock().defaultBlockState());
    }

    public float defaultDestroyTime() {
        return this.properties.destroyTime;
    }

    public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {
        private final int lightEmission;
        private final boolean useShapeForLightOcclusion;
        private final boolean isAir;
        private final Material material;
        private final MaterialColor materialColor;
        public final float destroySpeed;
        private final boolean requiresCorrectToolForDrops;
        private final boolean canOcclude;
        private final BlockBehaviour.StatePredicate isRedstoneConductor;
        private final BlockBehaviour.StatePredicate isSuffocating;
        private final BlockBehaviour.StatePredicate isViewBlocking;
        private final BlockBehaviour.StatePredicate hasPostProcess;
        private final BlockBehaviour.StatePredicate emissiveRendering;
        @Nullable
        protected BlockBehaviour.BlockStateBase.Cache cache;

        protected BlockStateBase(Block block, ImmutableMap<Property<?>, Comparable<?>> propertyMap, MapCodec<BlockState> codec) {
            super(block, propertyMap, codec);
            BlockBehaviour.Properties properties = block.properties;
            this.lightEmission = properties.lightEmission.applyAsInt(this.asState());
            this.useShapeForLightOcclusion = block.useShapeForLightOcclusion(this.asState());
            this.isAir = properties.isAir;
            this.material = properties.material;
            this.materialColor = properties.materialColor.apply(this.asState());
            this.destroySpeed = properties.destroyTime;
            this.requiresCorrectToolForDrops = properties.requiresCorrectToolForDrops;
            this.canOcclude = properties.canOcclude;
            this.isRedstoneConductor = properties.isRedstoneConductor;
            this.isSuffocating = properties.isSuffocating;
            this.isViewBlocking = properties.isViewBlocking;
            this.hasPostProcess = properties.hasPostProcess;
            this.emissiveRendering = properties.emissiveRendering;
        }

        public void initCache() {
            if (!this.getBlock().hasDynamicShape()) {
                this.cache = new BlockBehaviour.BlockStateBase.Cache(this.asState());
            }

        }

        public Block getBlock() {
            return this.owner;
        }

        public Material getMaterial() {
            return this.material;
        }

        public boolean isValidSpawn(BlockGetter world, BlockPos pos, EntityType<?> type) {
            return this.getBlock().properties.isValidSpawn.test(this.asState(), world, pos, type);
        }

        public boolean propagatesSkylightDown(BlockGetter world, BlockPos pos) {
            return this.cache != null ? this.cache.propagatesSkylightDown : this.getBlock().propagatesSkylightDown(this.asState(), world, pos);
        }

        public int getLightBlock(BlockGetter world, BlockPos pos) {
            return this.cache != null ? this.cache.lightBlock : this.getBlock().getLightBlock(this.asState(), world, pos);
        }

        public VoxelShape getFaceOcclusionShape(BlockGetter world, BlockPos pos, Direction direction) {
            return this.cache != null && this.cache.occlusionShapes != null ? this.cache.occlusionShapes[direction.ordinal()] : Shapes.getFaceShape(this.getOcclusionShape(world, pos), direction);
        }

        public VoxelShape getOcclusionShape(BlockGetter world, BlockPos pos) {
            return this.getBlock().getOcclusionShape(this.asState(), world, pos);
        }

        public boolean hasLargeCollisionShape() {
            return this.cache == null || this.cache.largeCollisionShape;
        }

        public boolean useShapeForLightOcclusion() {
            return this.useShapeForLightOcclusion;
        }

        public int getLightEmission() {
            return this.lightEmission;
        }

        public boolean isAir() {
            return this.isAir;
        }

        public MaterialColor getMapColor(BlockGetter world, BlockPos pos) {
            return this.materialColor;
        }

        public BlockState rotate(Rotation rotation) {
            return this.getBlock().rotate(this.asState(), rotation);
        }

        public BlockState mirror(Mirror mirror) {
            return this.getBlock().mirror(this.asState(), mirror);
        }

        public RenderShape getRenderShape() {
            return this.getBlock().getRenderShape(this.asState());
        }

        public boolean emissiveRendering(BlockGetter world, BlockPos pos) {
            return this.emissiveRendering.test(this.asState(), world, pos);
        }

        public float getShadeBrightness(BlockGetter world, BlockPos pos) {
            return this.getBlock().getShadeBrightness(this.asState(), world, pos);
        }

        public boolean isRedstoneConductor(BlockGetter world, BlockPos pos) {
            return this.isRedstoneConductor.test(this.asState(), world, pos);
        }

        public boolean isSignalSource() {
            return this.getBlock().isSignalSource(this.asState());
        }

        public int getSignal(BlockGetter world, BlockPos pos, Direction direction) {
            return this.getBlock().getSignal(this.asState(), world, pos, direction);
        }

        public boolean hasAnalogOutputSignal() {
            return this.getBlock().hasAnalogOutputSignal(this.asState());
        }

        public int getAnalogOutputSignal(Level world, BlockPos pos) {
            return this.getBlock().getAnalogOutputSignal(this.asState(), world, pos);
        }

        public float getDestroySpeed(BlockGetter world, BlockPos pos) {
            return this.destroySpeed;
        }

        public float getDestroyProgress(Player player, BlockGetter world, BlockPos pos) {
            return this.getBlock().getDestroyProgress(this.asState(), player, world, pos);
        }

        public int getDirectSignal(BlockGetter world, BlockPos pos, Direction direction) {
            return this.getBlock().getDirectSignal(this.asState(), world, pos, direction);
        }

        public PushReaction getPistonPushReaction() {
            return this.getBlock().getPistonPushReaction(this.asState());
        }

        public boolean isSolidRender(BlockGetter world, BlockPos pos) {
            if (this.cache != null) {
                return this.cache.solidRender;
            } else {
                BlockState blockState = this.asState();
                return blockState.canOcclude() ? Block.isShapeFullBlock(blockState.getOcclusionShape(world, pos)) : false;
            }
        }

        public boolean canOcclude() {
            return this.canOcclude;
        }

        public boolean skipRendering(BlockState state, Direction direction) {
            return this.getBlock().skipRendering(this.asState(), state, direction);
        }

        public VoxelShape getShape(BlockGetter world, BlockPos pos) {
            return this.getShape(world, pos, CollisionContext.empty());
        }

        public VoxelShape getShape(BlockGetter world, BlockPos pos, CollisionContext context) {
            return this.getBlock().getShape(this.asState(), world, pos, context);
        }

        public VoxelShape getCollisionShape(BlockGetter world, BlockPos pos) {
            return this.cache != null ? this.cache.collisionShape : this.getCollisionShape(world, pos, CollisionContext.empty());
        }

        public VoxelShape getCollisionShape(BlockGetter world, BlockPos pos, CollisionContext context) {
            return this.getBlock().getCollisionShape(this.asState(), world, pos, context);
        }

        public VoxelShape getBlockSupportShape(BlockGetter world, BlockPos pos) {
            return this.getBlock().getBlockSupportShape(this.asState(), world, pos);
        }

        public VoxelShape getVisualShape(BlockGetter world, BlockPos pos, CollisionContext context) {
            return this.getBlock().getVisualShape(this.asState(), world, pos, context);
        }

        public VoxelShape getInteractionShape(BlockGetter world, BlockPos pos) {
            return this.getBlock().getInteractionShape(this.asState(), world, pos);
        }

        public final boolean entityCanStandOn(BlockGetter world, BlockPos pos, Entity entity) {
            return this.entityCanStandOnFace(world, pos, entity, Direction.UP);
        }

        public final boolean entityCanStandOnFace(BlockGetter world, BlockPos pos, Entity entity, Direction direction) {
            return Block.isFaceFull(this.getCollisionShape(world, pos, CollisionContext.of(entity)), direction);
        }

        public Vec3 getOffset(BlockGetter world, BlockPos pos) {
            Block block = this.getBlock();
            BlockBehaviour.OffsetType offsetType = block.getOffsetType();
            if (offsetType == BlockBehaviour.OffsetType.NONE) {
                return Vec3.ZERO;
            } else {
                long l = Mth.getSeed(pos.getX(), 0, pos.getZ());
                float f = block.getMaxHorizontalOffset();
                double d = Mth.clamp(((double)((float)(l & 15L) / 15.0F) - 0.5D) * 0.5D, (double)(-f), (double)f);
                double e = offsetType == BlockBehaviour.OffsetType.XYZ ? ((double)((float)(l >> 4 & 15L) / 15.0F) - 1.0D) * (double)block.getMaxVerticalOffset() : 0.0D;
                double g = Mth.clamp(((double)((float)(l >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D, (double)(-f), (double)f);
                return new Vec3(d, e, g);
            }
        }

        public boolean triggerEvent(Level world, BlockPos pos, int type, int data) {
            return this.getBlock().triggerEvent(this.asState(), world, pos, type, data);
        }

        public void neighborChanged(Level world, BlockPos pos, Block block, BlockPos posFrom, boolean notify) {
            this.getBlock().neighborChanged(this.asState(), world, pos, block, posFrom, notify);
        }

        public final void updateNeighbourShapes(LevelAccessor world, BlockPos pos, int flags) {
            this.updateNeighbourShapes(world, pos, flags, 512);
        }

        public final void updateNeighbourShapes(LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {
            this.getBlock();
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(Direction direction : BlockBehaviour.UPDATE_SHAPE_ORDER) {
                mutableBlockPos.setWithOffset(pos, direction);
                BlockState blockState = world.getBlockState(mutableBlockPos);
                BlockState blockState2 = blockState.updateShape(direction.getOpposite(), this.asState(), world, mutableBlockPos, pos);
                Block.updateOrDestroy(blockState, blockState2, world, mutableBlockPos, flags, maxUpdateDepth);
            }

        }

        public final void updateIndirectNeighbourShapes(LevelAccessor world, BlockPos pos, int flags) {
            this.updateIndirectNeighbourShapes(world, pos, flags, 512);
        }

        public void updateIndirectNeighbourShapes(LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {
            this.getBlock().updateIndirectNeighbourShapes(this.asState(), world, pos, flags, maxUpdateDepth);
        }

        public void onPlace(Level world, BlockPos pos, BlockState state, boolean notify) {
            this.getBlock().onPlace(this.asState(), world, pos, state, notify);
        }

        public void onRemove(Level world, BlockPos pos, BlockState state, boolean moved) {
            this.getBlock().onRemove(this.asState(), world, pos, state, moved);
        }

        public void tick(ServerLevel world, BlockPos pos, Random random) {
            this.getBlock().tick(this.asState(), world, pos, random);
        }

        public void randomTick(ServerLevel world, BlockPos pos, Random random) {
            this.getBlock().randomTick(this.asState(), world, pos, random);
        }

        public void entityInside(Level world, BlockPos pos, Entity entity) {
            this.getBlock().entityInside(this.asState(), world, pos, entity);
        }

        public void spawnAfterBreak(ServerLevel world, BlockPos pos, ItemStack stack) {
            this.getBlock().spawnAfterBreak(this.asState(), world, pos, stack);
        }

        public List<ItemStack> getDrops(LootContext.Builder builder) {
            return this.getBlock().getDrops(this.asState(), builder);
        }

        public InteractionResult use(Level world, Player player, InteractionHand hand, BlockHitResult hit) {
            return this.getBlock().use(this.asState(), world, hit.getBlockPos(), player, hand, hit);
        }

        public void attack(Level world, BlockPos pos, Player player) {
            this.getBlock().attack(this.asState(), world, pos, player);
        }

        public boolean isSuffocating(BlockGetter world, BlockPos pos) {
            return this.isSuffocating.test(this.asState(), world, pos);
        }

        public boolean isViewBlocking(BlockGetter world, BlockPos pos) {
            return this.isViewBlocking.test(this.asState(), world, pos);
        }

        public BlockState updateShape(Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
            return this.getBlock().updateShape(this.asState(), direction, neighborState, world, pos, neighborPos);
        }

        public boolean isPathfindable(BlockGetter world, BlockPos pos, PathComputationType type) {
            return this.getBlock().isPathfindable(this.asState(), world, pos, type);
        }

        public boolean canBeReplaced(BlockPlaceContext context) {
            return this.getBlock().canBeReplaced(this.asState(), context);
        }

        public boolean canBeReplaced(Fluid fluid) {
            return this.getBlock().canBeReplaced(this.asState(), fluid);
        }

        public boolean canSurvive(LevelReader world, BlockPos pos) {
            return this.getBlock().canSurvive(this.asState(), world, pos);
        }

        public boolean hasPostProcess(BlockGetter world, BlockPos pos) {
            return this.hasPostProcess.test(this.asState(), world, pos);
        }

        @Nullable
        public MenuProvider getMenuProvider(Level world, BlockPos pos) {
            return this.getBlock().getMenuProvider(this.asState(), world, pos);
        }

        public boolean is(TagKey<Block> tag) {
            return this.getBlock().builtInRegistryHolder().is(tag);
        }

        public boolean is(TagKey<Block> tag, Predicate<BlockBehaviour.BlockStateBase> predicate) {
            return this.is(tag) && predicate.test(this);
        }

        public boolean is(HolderSet<Block> blocks) {
            return blocks.contains(this.getBlock().builtInRegistryHolder());
        }

        public Stream<TagKey<Block>> getTags() {
            return this.getBlock().builtInRegistryHolder().tags();
        }

        public boolean hasBlockEntity() {
            return this.getBlock() instanceof EntityBlock;
        }

        @Nullable
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockEntityType<T> blockEntityType) {
            return this.getBlock() instanceof EntityBlock ? ((EntityBlock)this.getBlock()).getTicker(world, this.asState(), blockEntityType) : null;
        }

        public boolean is(Block block) {
            return this.getBlock() == block;
        }

        public FluidState getFluidState() {
            return this.getBlock().getFluidState(this.asState());
        }

        public boolean isRandomlyTicking() {
            return this.getBlock().isRandomlyTicking(this.asState());
        }

        public long getSeed(BlockPos pos) {
            return this.getBlock().getSeed(this.asState(), pos);
        }

        public SoundType getSoundType() {
            return this.getBlock().getSoundType(this.asState());
        }

        public void onProjectileHit(Level world, BlockState state, BlockHitResult hit, Projectile projectile) {
            this.getBlock().onProjectileHit(world, state, hit, projectile);
        }

        public boolean isFaceSturdy(BlockGetter world, BlockPos pos, Direction direction) {
            return this.isFaceSturdy(world, pos, direction, SupportType.FULL);
        }

        public boolean isFaceSturdy(BlockGetter world, BlockPos pos, Direction direction, SupportType shapeType) {
            return this.cache != null ? this.cache.isFaceSturdy(direction, shapeType) : shapeType.isSupporting(this.asState(), world, pos, direction);
        }

        public boolean isCollisionShapeFullBlock(BlockGetter world, BlockPos pos) {
            return this.cache != null ? this.cache.isCollisionShapeFullBlock : this.getBlock().isCollisionShapeFullBlock(this.asState(), world, pos);
        }

        protected abstract BlockState asState();

        public boolean requiresCorrectToolForDrops() {
            return this.requiresCorrectToolForDrops;
        }

        static final class Cache {
            private static final Direction[] DIRECTIONS = Direction.values();
            private static final int SUPPORT_TYPE_COUNT = SupportType.values().length;
            protected final boolean solidRender;
            final boolean propagatesSkylightDown;
            final int lightBlock;
            @Nullable
            final VoxelShape[] occlusionShapes;
            protected final VoxelShape collisionShape;
            protected final boolean largeCollisionShape;
            private final boolean[] faceSturdy;
            protected final boolean isCollisionShapeFullBlock;

            Cache(BlockState state) {
                Block block = state.getBlock();
                this.solidRender = state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                this.propagatesSkylightDown = block.propagatesSkylightDown(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                this.lightBlock = block.getLightBlock(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                if (!state.canOcclude()) {
                    this.occlusionShapes = null;
                } else {
                    this.occlusionShapes = new VoxelShape[DIRECTIONS.length];
                    VoxelShape voxelShape = block.getOcclusionShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

                    for(Direction direction : DIRECTIONS) {
                        this.occlusionShapes[direction.ordinal()] = Shapes.getFaceShape(voxelShape, direction);
                    }
                }

                this.collisionShape = block.getCollisionShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
                if (!this.collisionShape.isEmpty() && block.getOffsetType() != BlockBehaviour.OffsetType.NONE) {
                    throw new IllegalStateException(String.format("%s has a collision shape and an offset type, but is not marked as dynamicShape in its properties.", Registry.BLOCK.getKey(block)));
                } else {
                    this.largeCollisionShape = Arrays.stream(Direction.Axis.values()).anyMatch((axis) -> {
                        return this.collisionShape.min(axis) < 0.0D || this.collisionShape.max(axis) > 1.0D;
                    });
                    this.faceSturdy = new boolean[DIRECTIONS.length * SUPPORT_TYPE_COUNT];

                    for(Direction direction2 : DIRECTIONS) {
                        for(SupportType supportType : SupportType.values()) {
                            this.faceSturdy[getFaceSupportIndex(direction2, supportType)] = supportType.isSupporting(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction2);
                        }
                    }

                    this.isCollisionShapeFullBlock = Block.isShapeFullBlock(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
                }
            }

            public boolean isFaceSturdy(Direction direction, SupportType shapeType) {
                return this.faceSturdy[getFaceSupportIndex(direction, shapeType)];
            }

            private static int getFaceSupportIndex(Direction direction, SupportType shapeType) {
                return direction.ordinal() * SUPPORT_TYPE_COUNT + shapeType.ordinal();
            }
        }
    }

    public static enum OffsetType {
        NONE,
        XZ,
        XYZ;
    }

    public static class Properties {
        Material material;
        Function<BlockState, MaterialColor> materialColor;
        boolean hasCollision = true;
        SoundType soundType = SoundType.STONE;
        ToIntFunction<BlockState> lightEmission = (state) -> {
            return 0;
        };
        float explosionResistance;
        float destroyTime;
        boolean requiresCorrectToolForDrops;
        boolean isRandomlyTicking;
        float friction = 0.6F;
        float speedFactor = 1.0F;
        float jumpFactor = 1.0F;
        ResourceLocation drops;
        boolean canOcclude = true;
        boolean isAir;
        BlockBehaviour.StateArgumentPredicate<EntityType<?>> isValidSpawn = (state, world, pos, type) -> {
            return state.isFaceSturdy(world, pos, Direction.UP) && state.getLightEmission() < 14;
        };
        BlockBehaviour.StatePredicate isRedstoneConductor = (state, world, pos) -> {
            return state.getMaterial().isSolidBlocking() && state.isCollisionShapeFullBlock(world, pos);
        };
        BlockBehaviour.StatePredicate isSuffocating = (state, world, pos) -> {
            return this.material.blocksMotion() && state.isCollisionShapeFullBlock(world, pos);
        };
        BlockBehaviour.StatePredicate isViewBlocking = this.isSuffocating;
        BlockBehaviour.StatePredicate hasPostProcess = (state, world, pos) -> {
            return false;
        };
        BlockBehaviour.StatePredicate emissiveRendering = (state, world, pos) -> {
            return false;
        };
        boolean dynamicShape;

        private Properties(Material material, MaterialColor mapColorProvider) {
            this(material, (state) -> {
                return mapColorProvider;
            });
        }

        private Properties(Material material, Function<BlockState, MaterialColor> mapColorProvider) {
            this.material = material;
            this.materialColor = mapColorProvider;
        }

        public static BlockBehaviour.Properties of(Material material) {
            return of(material, material.getColor());
        }

        public static BlockBehaviour.Properties of(Material material, DyeColor color) {
            return of(material, color.getMaterialColor());
        }

        public static BlockBehaviour.Properties of(Material material, MaterialColor color) {
            return new BlockBehaviour.Properties(material, color);
        }

        public static BlockBehaviour.Properties of(Material material, Function<BlockState, MaterialColor> mapColor) {
            return new BlockBehaviour.Properties(material, mapColor);
        }

        public static BlockBehaviour.Properties copy(BlockBehaviour block) {
            BlockBehaviour.Properties properties = new BlockBehaviour.Properties(block.material, block.properties.materialColor);
            properties.material = block.properties.material;
            properties.destroyTime = block.properties.destroyTime;
            properties.explosionResistance = block.properties.explosionResistance;
            properties.hasCollision = block.properties.hasCollision;
            properties.isRandomlyTicking = block.properties.isRandomlyTicking;
            properties.lightEmission = block.properties.lightEmission;
            properties.materialColor = block.properties.materialColor;
            properties.soundType = block.properties.soundType;
            properties.friction = block.properties.friction;
            properties.speedFactor = block.properties.speedFactor;
            properties.dynamicShape = block.properties.dynamicShape;
            properties.canOcclude = block.properties.canOcclude;
            properties.isAir = block.properties.isAir;
            properties.requiresCorrectToolForDrops = block.properties.requiresCorrectToolForDrops;
            return properties;
        }

        public BlockBehaviour.Properties noCollission() {
            this.hasCollision = false;
            this.canOcclude = false;
            return this;
        }

        public BlockBehaviour.Properties noOcclusion() {
            this.canOcclude = false;
            return this;
        }

        public BlockBehaviour.Properties friction(float slipperiness) {
            this.friction = slipperiness;
            return this;
        }

        public BlockBehaviour.Properties speedFactor(float velocityMultiplier) {
            this.speedFactor = velocityMultiplier;
            return this;
        }

        public BlockBehaviour.Properties jumpFactor(float jumpVelocityMultiplier) {
            this.jumpFactor = jumpVelocityMultiplier;
            return this;
        }

        public BlockBehaviour.Properties sound(SoundType soundGroup) {
            this.soundType = soundGroup;
            return this;
        }

        public BlockBehaviour.Properties lightLevel(ToIntFunction<BlockState> luminance) {
            this.lightEmission = luminance;
            return this;
        }

        public BlockBehaviour.Properties strength(float hardness, float resistance) {
            return this.destroyTime(hardness).explosionResistance(resistance);
        }

        public BlockBehaviour.Properties instabreak() {
            return this.strength(0.0F);
        }

        public BlockBehaviour.Properties strength(float strength) {
            this.strength(strength, strength);
            return this;
        }

        public BlockBehaviour.Properties randomTicks() {
            this.isRandomlyTicking = true;
            return this;
        }

        public BlockBehaviour.Properties dynamicShape() {
            this.dynamicShape = true;
            return this;
        }

        public BlockBehaviour.Properties noDrops() {
            this.drops = BuiltInLootTables.EMPTY;
            return this;
        }

        public BlockBehaviour.Properties dropsLike(Block source) {
            this.drops = source.getLootTable();
            return this;
        }

        public BlockBehaviour.Properties air() {
            this.isAir = true;
            return this;
        }

        public BlockBehaviour.Properties isValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> predicate) {
            this.isValidSpawn = predicate;
            return this;
        }

        public BlockBehaviour.Properties isRedstoneConductor(BlockBehaviour.StatePredicate predicate) {
            this.isRedstoneConductor = predicate;
            return this;
        }

        public BlockBehaviour.Properties isSuffocating(BlockBehaviour.StatePredicate predicate) {
            this.isSuffocating = predicate;
            return this;
        }

        public BlockBehaviour.Properties isViewBlocking(BlockBehaviour.StatePredicate predicate) {
            this.isViewBlocking = predicate;
            return this;
        }

        public BlockBehaviour.Properties hasPostProcess(BlockBehaviour.StatePredicate predicate) {
            this.hasPostProcess = predicate;
            return this;
        }

        public BlockBehaviour.Properties emissiveRendering(BlockBehaviour.StatePredicate predicate) {
            this.emissiveRendering = predicate;
            return this;
        }

        public BlockBehaviour.Properties requiresCorrectToolForDrops() {
            this.requiresCorrectToolForDrops = true;
            return this;
        }

        public BlockBehaviour.Properties color(MaterialColor color) {
            this.materialColor = (state) -> {
                return color;
            };
            return this;
        }

        public BlockBehaviour.Properties destroyTime(float hardness) {
            this.destroyTime = hardness;
            return this;
        }

        public BlockBehaviour.Properties explosionResistance(float resistance) {
            this.explosionResistance = Math.max(0.0F, resistance);
            return this;
        }
    }

    public interface StateArgumentPredicate<A> {
        boolean test(BlockState state, BlockGetter world, BlockPos pos, A type);
    }

    public interface StatePredicate {
        boolean test(BlockState state, BlockGetter world, BlockPos pos);
    }
}
