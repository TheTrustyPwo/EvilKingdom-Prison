// mc-dev import
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
import net.minecraft.world.item.context.UseOnContext;
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
    public void updateIndirectNeighbourShapes(BlockState state, LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {}

    /** @deprecated */
    @Deprecated
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        switch (type) {
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

    // Paper start - add ItemActionContext param
    @Deprecated
    public void onPlace(BlockState iblockdata, Level world, BlockPos blockposition, BlockState iblockdata1, boolean flag, UseOnContext itemActionContext) {
        this.onPlace(iblockdata, world, blockposition, iblockdata1, flag);
    }
    // Paper end
    /** @deprecated */
    @Deprecated
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        org.spigotmc.AsyncCatcher.catchOp("block onPlace"); // Spigot
    }

    /** @deprecated */
    @Deprecated
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        org.spigotmc.AsyncCatcher.catchOp("block remove"); // Spigot
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
        return this.material.isReplaceable() && (context.getItemInHand().isEmpty() || !context.getItemInHand().is(this.asItem())) && (state.isDestroyable() || (context.getPlayer() != null && context.getPlayer().getAbilities().instabuild)); // Paper
    }

    /** @deprecated */
    @Deprecated
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        return this.material.isReplaceable() || !this.material.isSolid();
    }

    /** @deprecated */
    @Deprecated
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        ResourceLocation minecraftkey = this.getLootTable();

        if (minecraftkey == BuiltInLootTables.EMPTY) {
            return Collections.emptyList();
        } else {
            LootContext loottableinfo = builder.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
            ServerLevel worldserver = loottableinfo.getLevel();
            LootTable loottable = worldserver.getServer().getLootTables().get(minecraftkey);

            return loottable.getRandomItems(loottableinfo);
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
        return state.isSolidRender(world, pos) ? world.getMaxLightLevel() : (state.propagatesSkylightDown(world, pos) ? 0 : 1);
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
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {}

    /** @deprecated */
    @Deprecated
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        float f = state.getDestroySpeed(world, pos);

        if (f == -1.0F) {
            return 0.0F;
        } else {
            int i = player.hasCorrectToolForDrops(state) ? 30 : 100;

            return player.getDestroySpeed(state) / f / (float) i;
        }
    }

    /** @deprecated */
    @Deprecated
    public void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack stack) {}

    /** @deprecated */
    @Deprecated
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {}

    /** @deprecated */
    @Deprecated
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return 0;
    }

    /** @deprecated */
    @Deprecated
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {}

    /** @deprecated */
    @Deprecated
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return 0;
    }

    public final ResourceLocation getLootTable() {
        if (this.drops == null) {
            ResourceLocation minecraftkey = Registry.BLOCK.getKey(this.asBlock());

            this.drops = new ResourceLocation(minecraftkey.getNamespace(), "blocks/" + minecraftkey.getPath());
        }

        return this.drops;
    }

    /** @deprecated */
    @Deprecated
    public void onProjectileHit(Level world, BlockState state, BlockHitResult hit, Projectile projectile) {}

    public abstract Item asItem();

    protected abstract Block asBlock();

    public MaterialColor defaultMaterialColor() {
        return (MaterialColor) this.properties.materialColor.apply(this.asBlock().defaultBlockState());
    }

    public float defaultDestroyTime() {
        return this.properties.destroyTime;
    }

    public static class Properties {

        Material material;
        Function<BlockState, MaterialColor> materialColor;
        boolean hasCollision;
        SoundType soundType;
        ToIntFunction<BlockState> lightEmission;
        float explosionResistance;
        float destroyTime;
        boolean requiresCorrectToolForDrops;
        boolean isRandomlyTicking;
        float friction;
        float speedFactor;
        float jumpFactor;
        ResourceLocation drops;
        boolean canOcclude;
        boolean isAir;
        BlockBehaviour.StateArgumentPredicate<EntityType<?>> isValidSpawn;
        BlockBehaviour.StatePredicate isRedstoneConductor;
        BlockBehaviour.StatePredicate isSuffocating;
        BlockBehaviour.StatePredicate isViewBlocking;
        BlockBehaviour.StatePredicate hasPostProcess;
        BlockBehaviour.StatePredicate emissiveRendering;
        boolean dynamicShape;

        private Properties(Material material, MaterialColor mapColorProvider) {
            this(material, (iblockdata) -> {
                return mapColorProvider;
            });
        }

        private Properties(Material material, Function<BlockState, MaterialColor> mapColorProvider) {
            this.hasCollision = true;
            this.soundType = SoundType.STONE;
            this.lightEmission = (iblockdata) -> {
                return 0;
            };
            this.friction = 0.6F;
            this.speedFactor = 1.0F;
            this.jumpFactor = 1.0F;
            this.canOcclude = true;
            this.isValidSpawn = (iblockdata, iblockaccess, blockposition, entitytypes) -> {
                return iblockdata.isFaceSturdy(iblockaccess, blockposition, Direction.UP) && iblockdata.getLightEmission() < 14;
            };
            this.isRedstoneConductor = (iblockdata, iblockaccess, blockposition) -> {
                return iblockdata.getMaterial().isSolidBlocking() && iblockdata.isCollisionShapeFullBlock(iblockaccess, blockposition);
            };
            this.isSuffocating = (iblockdata, iblockaccess, blockposition) -> {
                return this.material.blocksMotion() && iblockdata.isCollisionShapeFullBlock(iblockaccess, blockposition);
            };
            this.isViewBlocking = this.isSuffocating;
            this.hasPostProcess = (iblockdata, iblockaccess, blockposition) -> {
                return false;
            };
            this.emissiveRendering = (iblockdata, iblockaccess, blockposition) -> {
                return false;
            };
            this.material = material;
            this.materialColor = mapColorProvider;
        }

        public static BlockBehaviour.Properties of(Material material) {
            return Properties.of(material, material.getColor());
        }

        public static BlockBehaviour.Properties of(Material material, DyeColor color) {
            return Properties.of(material, color.getMaterialColor());
        }

        public static BlockBehaviour.Properties of(Material material, MaterialColor color) {
            return new BlockBehaviour.Properties(material, color);
        }

        public static BlockBehaviour.Properties of(Material material, Function<BlockState, MaterialColor> mapColor) {
            return new BlockBehaviour.Properties(material, mapColor);
        }

        public static BlockBehaviour.Properties copy(BlockBehaviour block) {
            BlockBehaviour.Properties blockbase_info = new BlockBehaviour.Properties(block.material, block.properties.materialColor);

            blockbase_info.material = block.properties.material;
            blockbase_info.destroyTime = block.properties.destroyTime;
            blockbase_info.explosionResistance = block.properties.explosionResistance;
            blockbase_info.hasCollision = block.properties.hasCollision;
            blockbase_info.isRandomlyTicking = block.properties.isRandomlyTicking;
            blockbase_info.lightEmission = block.properties.lightEmission;
            blockbase_info.materialColor = block.properties.materialColor;
            blockbase_info.soundType = block.properties.soundType;
            blockbase_info.friction = block.properties.friction;
            blockbase_info.speedFactor = block.properties.speedFactor;
            blockbase_info.dynamicShape = block.properties.dynamicShape;
            blockbase_info.canOcclude = block.properties.canOcclude;
            blockbase_info.isAir = block.properties.isAir;
            blockbase_info.requiresCorrectToolForDrops = block.properties.requiresCorrectToolForDrops;
            return blockbase_info;
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
            this.materialColor = (iblockdata) -> {
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

    public static enum OffsetType {

        NONE, XZ, XYZ;

        private OffsetType() {}
    }

    public interface StateArgumentPredicate<A> {

        boolean test(BlockState state, BlockGetter world, BlockPos pos, A type);
    }

    public interface StatePredicate {

        boolean test(BlockState state, BlockGetter world, BlockPos pos);
    }

    public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {

        private final int lightEmission; public final int getEmittedLight() { return this.lightEmission; } // Paper - OBFHELPER
        private final boolean useShapeForLightOcclusion; public final boolean isTransparentOnSomeFaces() { return this.useShapeForLightOcclusion; } // Paper - OBFHELPER
        private final boolean isAir;
        private final Material material;
        private final MaterialColor materialColor;
        public final float destroySpeed;
        private final boolean requiresCorrectToolForDrops;
        private final boolean canOcclude; public final boolean isOpaque() { return this.canOcclude; } // Paper - OBFHELPER
        private final BlockBehaviour.StatePredicate isRedstoneConductor;
        private final BlockBehaviour.StatePredicate isSuffocating;
        private final BlockBehaviour.StatePredicate isViewBlocking;
        private final BlockBehaviour.StatePredicate hasPostProcess;
        private final BlockBehaviour.StatePredicate emissiveRendering;
        @Nullable
        protected BlockBehaviour.BlockStateBase.Cache cache;

        protected BlockStateBase(Block block, ImmutableMap<Property<?>, Comparable<?>> propertyMap, MapCodec<BlockState> codec) {
            super(block, propertyMap, codec);
            BlockBehaviour.Properties blockbase_info = block.properties;

            this.lightEmission = blockbase_info.lightEmission.applyAsInt(this.asState());
            this.useShapeForLightOcclusion = block.useShapeForLightOcclusion(this.asState());
            this.isAir = blockbase_info.isAir;
            this.material = blockbase_info.material;
            this.materialColor = (MaterialColor) blockbase_info.materialColor.apply(this.asState());
            this.destroySpeed = blockbase_info.destroyTime;
            this.requiresCorrectToolForDrops = blockbase_info.requiresCorrectToolForDrops;
            this.canOcclude = blockbase_info.canOcclude;
            this.isRedstoneConductor = blockbase_info.isRedstoneConductor;
            this.isSuffocating = blockbase_info.isSuffocating;
            this.isViewBlocking = blockbase_info.isViewBlocking;
            this.hasPostProcess = blockbase_info.hasPostProcess;
            this.emissiveRendering = blockbase_info.emissiveRendering;
            this.conditionallyFullOpaque = this.isOpaque() & this.isTransparentOnSomeFaces(); // Paper
        }
        // Paper start - impl cached craft block data, lazy load to fix issue with loading at the wrong time
        private org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData cachedCraftBlockData;

        public org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData createCraftBlockData() {
            if (cachedCraftBlockData == null) cachedCraftBlockData = org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData.createData(asState());
            return (org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData) cachedCraftBlockData.clone();
        }
        // Paper end

        // Paper start
        protected boolean shapeExceedsCube = true;
        public final boolean shapeExceedsCube() {
            return this.shapeExceedsCube;
        }
        // Paper end
        // Paper start
        protected boolean isTicking;
        protected FluidState fluid;
        // Paper end
        // Paper start
        protected int opacityIfCached = -1;
        // ret -1 if opacity is dynamic, or -1 if the block is conditionally full opaque, else return opacity in [0, 15]
        public final int getOpacityIfCached() {
            return this.opacityIfCached;
        }

        protected final boolean conditionallyFullOpaque;
        public final boolean isConditionallyFullOpaque() {
            return this.conditionallyFullOpaque;
        }
        // Paper end
        // Paper start
        private long blockCollisionBehavior = io.papermc.paper.util.CollisionUtil.KNOWN_SPECIAL_BLOCK;

        public final long getBlockCollisionBehavior() {
            return this.blockCollisionBehavior;
        }
        // Paper end

        public void initCache() {
            this.fluid = this.getBlock().getFluidState(this.asState()); // Paper - moved from getFluid()
            this.isTicking = this.getBlock().isRandomlyTicking(this.asState()); // Paper - moved from isTicking()
            if (!this.getBlock().hasDynamicShape()) {
                this.cache = new BlockBehaviour.BlockStateBase.Cache(this.asState());
            }
            this.shapeExceedsCube = this.cache == null || this.cache.largeCollisionShape; // Paper - moved from actual method to here
            this.opacityIfCached = this.cache == null || this.isConditionallyFullOpaque() ? -1 : this.cache.lightBlock; // Paper - cache opacity for light
            // Paper start
            if (io.papermc.paper.util.CollisionUtil.isSpecialCollidingBlock(this)) {
                this.blockCollisionBehavior = io.papermc.paper.util.CollisionUtil.KNOWN_SPECIAL_BLOCK;
            } else {
                try {
                    // There is NOTHING HACKY ABOUT THIS AT ALLLLLLLLLLLLLLL
                    VoxelShape constantShape = this.getCollisionShape(null, null, null);
                    if (constantShape == null) {
                        this.blockCollisionBehavior = io.papermc.paper.util.CollisionUtil.KNOWN_UNKNOWN_BLOCK;
                    } else {
                        constantShape = constantShape.optimize();
                        if (constantShape.isEmpty()) {
                            this.blockCollisionBehavior = io.papermc.paper.util.CollisionUtil.KNOWN_EMPTY_BLOCK;
                        } else {
                            final List<net.minecraft.world.phys.AABB> boxes = constantShape.toAabbs();
                            if (constantShape == net.minecraft.world.phys.shapes.Shapes.getFullUnoptimisedCube() || (boxes.size() == 1 && boxes.get(0).equals(net.minecraft.world.phys.shapes.Shapes.BLOCK_OPTIMISED.aabb))) {
                                this.blockCollisionBehavior = io.papermc.paper.util.CollisionUtil.KNOWN_FULL_BLOCK;
                            } else {
                                this.blockCollisionBehavior = io.papermc.paper.util.CollisionUtil.KNOWN_UNKNOWN_BLOCK;
                            }
                        }
                    }
                } catch (final Error error) {
                    throw error;
                } catch (final Throwable throwable) {
                    this.blockCollisionBehavior = io.papermc.paper.util.CollisionUtil.KNOWN_UNKNOWN_BLOCK;
                }
            }
            // Paper end
        }

        public Block getBlock() {
            return (Block) this.owner;
        }
        // Paper start
        public final boolean isDestroyable() {
            return getBlock().isDestroyable();
        }
        // Paper end
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

        public final boolean hasLargeCollisionShape() { // Paper
            return this.shapeExceedsCube; // Paper - moved into shape cache init
        }

        public final boolean useShapeForLightOcclusion() { // Paper
            return this.useShapeForLightOcclusion;
        }

        public final int getLightEmission() { // Paper
            return this.lightEmission;
        }

        public final boolean isAir() { // Paper
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
            return !isDestroyable() ? PushReaction.BLOCK : this.getBlock().getPistonPushReaction(this.asState()); // Paper
        }

        public boolean isSolidRender(BlockGetter world, BlockPos pos) {
            if (this.cache != null) {
                return this.cache.solidRender;
            } else {
                BlockState iblockdata = this.asState();

                return iblockdata.canOcclude() ? Block.isShapeFullBlock(iblockdata.getOcclusionShape(world, pos)) : false;
            }
        }

        public final boolean canOcclude() { // Paper
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
            BlockBehaviour.OffsetType blockbase_enumrandomoffset = block.getOffsetType();

            if (blockbase_enumrandomoffset == BlockBehaviour.OffsetType.NONE) {
                return Vec3.ZERO;
            } else {
                long i = Mth.getSeed(pos.getX(), 0, pos.getZ());
                float f = block.getMaxHorizontalOffset();
                double d0 = Mth.clamp(((double) ((float) (i & 15L) / 15.0F) - 0.5D) * 0.5D, (double) (-f), (double) f);
                double d1 = blockbase_enumrandomoffset == BlockBehaviour.OffsetType.XYZ ? ((double) ((float) (i >> 4 & 15L) / 15.0F) - 1.0D) * (double) block.getMaxVerticalOffset() : 0.0D;
                double d2 = Mth.clamp(((double) ((float) (i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D, (double) (-f), (double) f);

                return new Vec3(d0, d1, d2);
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
            BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
            Direction[] aenumdirection = BlockBehaviour.UPDATE_SHAPE_ORDER;
            int k = aenumdirection.length;

            for (int l = 0; l < k; ++l) {
                Direction enumdirection = aenumdirection[l];

                blockposition_mutableblockposition.setWithOffset(pos, enumdirection);
                BlockState iblockdata = world.getBlockState(blockposition_mutableblockposition);
                BlockState iblockdata1 = iblockdata.updateShape(enumdirection.getOpposite(), this.asState(), world, blockposition_mutableblockposition, pos);

                Block.updateOrDestroy(iblockdata, iblockdata1, world, blockposition_mutableblockposition, flags, maxUpdateDepth);
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
            return this.getBlock() instanceof EntityBlock ? ((EntityBlock) this.getBlock()).getTicker(world, this.asState(), blockEntityType) : null;
        }

        public boolean is(Block block) {
            return this.getBlock() == block;
        }

        public final FluidState getFluidState() { // Paper
            return this.fluid; // Paper - moved into init
        }

        public final boolean isRandomlyTicking() { // Paper
            return this.isTicking; // Paper - moved into init
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

        private static final class Cache {

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
                int i;

                if (!state.canOcclude()) {
                    this.occlusionShapes = null;
                } else {
                    this.occlusionShapes = new VoxelShape[BlockBehaviour.BlockStateBase.Cache.DIRECTIONS.length];
                    VoxelShape voxelshape = block.getOcclusionShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                    Direction[] aenumdirection = BlockBehaviour.BlockStateBase.Cache.DIRECTIONS;

                    i = aenumdirection.length;

                    for (int j = 0; j < i; ++j) {
                        Direction enumdirection = aenumdirection[j];

                        this.occlusionShapes[enumdirection.ordinal()] = Shapes.getFaceShape(voxelshape, enumdirection);
                    }
                }

                this.collisionShape = block.getCollisionShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
                if (!this.collisionShape.isEmpty() && block.getOffsetType() != BlockBehaviour.OffsetType.NONE) {
                    throw new IllegalStateException(String.format("%s has a collision shape and an offset type, but is not marked as dynamicShape in its properties.", Registry.BLOCK.getKey(block)));
                } else {
                    this.largeCollisionShape = Arrays.stream(Direction.Axis.values()).anyMatch((enumdirection_enumaxis) -> {
                        return this.collisionShape.min(enumdirection_enumaxis) < 0.0D || this.collisionShape.max(enumdirection_enumaxis) > 1.0D;
                    });
                    this.faceSturdy = new boolean[BlockBehaviour.BlockStateBase.Cache.DIRECTIONS.length * BlockBehaviour.BlockStateBase.Cache.SUPPORT_TYPE_COUNT];
                    Direction[] aenumdirection1 = BlockBehaviour.BlockStateBase.Cache.DIRECTIONS;
                    int k = aenumdirection1.length;

                    for (i = 0; i < k; ++i) {
                        Direction enumdirection1 = aenumdirection1[i];
                        SupportType[] aenumblocksupport = SupportType.values();
                        int l = aenumblocksupport.length;

                        for (int i1 = 0; i1 < l; ++i1) {
                            SupportType enumblocksupport = aenumblocksupport[i1];

                            this.faceSturdy[Cache.getFaceSupportIndex(enumdirection1, enumblocksupport)] = enumblocksupport.isSupporting(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, enumdirection1);
                        }
                    }

                    this.isCollisionShapeFullBlock = Block.isShapeFullBlock(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
                }
            }

            public boolean isFaceSturdy(Direction direction, SupportType shapeType) {
                return this.faceSturdy[Cache.getFaceSupportIndex(direction, shapeType)];
            }

            private static int getFaceSupportIndex(Direction direction, SupportType shapeType) {
                return direction.ordinal() * BlockBehaviour.BlockStateBase.Cache.SUPPORT_TYPE_COUNT + shapeType.ordinal();
            }
        }
    }
}
