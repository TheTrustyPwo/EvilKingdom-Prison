package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestHelper {
    private final GameTestInfo testInfo;
    private boolean finalCheckAdded;

    public GameTestHelper(GameTestInfo test) {
        this.testInfo = test;
    }

    public ServerLevel getLevel() {
        return this.testInfo.getLevel();
    }

    public BlockState getBlockState(BlockPos pos) {
        return this.getLevel().getBlockState(this.absolutePos(pos));
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getLevel().getBlockEntity(this.absolutePos(pos));
    }

    public void killAllEntities() {
        AABB aABB = this.getBounds();
        List<Entity> list = this.getLevel().getEntitiesOfClass(Entity.class, aABB.inflate(1.0D), (entity) -> {
            return !(entity instanceof Player);
        });
        list.forEach(Entity::kill);
    }

    public ItemEntity spawnItem(Item item, float x, float y, float z) {
        ServerLevel serverLevel = this.getLevel();
        Vec3 vec3 = this.absoluteVec(new Vec3((double)x, (double)y, (double)z));
        ItemEntity itemEntity = new ItemEntity(serverLevel, vec3.x, vec3.y, vec3.z, new ItemStack(item, 1));
        itemEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        serverLevel.addFreshEntity(itemEntity);
        return itemEntity;
    }

    public <E extends Entity> E spawn(EntityType<E> type, BlockPos pos) {
        return this.spawn(type, Vec3.atBottomCenterOf(pos));
    }

    public <E extends Entity> E spawn(EntityType<E> type, Vec3 pos) {
        ServerLevel serverLevel = this.getLevel();
        E entity = type.create(serverLevel);
        if (entity instanceof Mob) {
            ((Mob)entity).setPersistenceRequired();
        }

        Vec3 vec3 = this.absoluteVec(pos);
        entity.moveTo(vec3.x, vec3.y, vec3.z, entity.getYRot(), entity.getXRot());
        serverLevel.addFreshEntity(entity);
        return entity;
    }

    public <E extends Entity> E spawn(EntityType<E> type, int x, int y, int z) {
        return this.spawn(type, new BlockPos(x, y, z));
    }

    public <E extends Entity> E spawn(EntityType<E> type, float x, float y, float z) {
        return this.spawn(type, new Vec3((double)x, (double)y, (double)z));
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, BlockPos pos) {
        E mob = this.spawn(type, pos);
        mob.removeFreeWill();
        return mob;
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, int x, int y, int z) {
        return this.spawnWithNoFreeWill(type, new BlockPos(x, y, z));
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, Vec3 pos) {
        E mob = this.spawn(type, pos);
        mob.removeFreeWill();
        return mob;
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, float x, float y, float z) {
        return this.spawnWithNoFreeWill(type, new Vec3((double)x, (double)y, (double)z));
    }

    public GameTestSequence walkTo(Mob entity, BlockPos pos, float speed) {
        return this.startSequence().thenExecuteAfter(2, () -> {
            Path path = entity.getNavigation().createPath(this.absolutePos(pos), 0);
            entity.getNavigation().moveTo(path, (double)speed);
        });
    }

    public void pressButton(int x, int y, int z) {
        this.pressButton(new BlockPos(x, y, z));
    }

    public void pressButton(BlockPos pos) {
        this.assertBlockState(pos, (state) -> {
            return state.is(BlockTags.BUTTONS);
        }, () -> {
            return "Expected button";
        });
        BlockPos blockPos = this.absolutePos(pos);
        BlockState blockState = this.getLevel().getBlockState(blockPos);
        ButtonBlock buttonBlock = (ButtonBlock)blockState.getBlock();
        buttonBlock.press(blockState, this.getLevel(), blockPos);
    }

    public void useBlock(BlockPos pos) {
        BlockPos blockPos = this.absolutePos(pos);
        BlockState blockState = this.getLevel().getBlockState(blockPos);
        blockState.use(this.getLevel(), this.makeMockPlayer(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(blockPos), Direction.NORTH, blockPos, true));
    }

    public LivingEntity makeAboutToDrown(LivingEntity entity) {
        entity.setAirSupply(0);
        entity.setHealth(0.25F);
        return entity;
    }

    public Player makeMockPlayer() {
        return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
    }

    public void pullLever(int x, int y, int z) {
        this.pullLever(new BlockPos(x, y, z));
    }

    public void pullLever(BlockPos pos) {
        this.assertBlockPresent(Blocks.LEVER, pos);
        BlockPos blockPos = this.absolutePos(pos);
        BlockState blockState = this.getLevel().getBlockState(blockPos);
        LeverBlock leverBlock = (LeverBlock)blockState.getBlock();
        leverBlock.pull(blockState, this.getLevel(), blockPos);
    }

    public void pulseRedstone(BlockPos pos, long delay) {
        this.setBlock(pos, Blocks.REDSTONE_BLOCK);
        this.runAfterDelay(delay, () -> {
            this.setBlock(pos, Blocks.AIR);
        });
    }

    public void destroyBlock(BlockPos pos) {
        this.getLevel().destroyBlock(this.absolutePos(pos), false, (Entity)null);
    }

    public void setBlock(int x, int y, int z, Block block) {
        this.setBlock(new BlockPos(x, y, z), block);
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        this.setBlock(new BlockPos(x, y, z), state);
    }

    public void setBlock(BlockPos pos, Block block) {
        this.setBlock(pos, block.defaultBlockState());
    }

    public void setBlock(BlockPos pos, BlockState state) {
        this.getLevel().setBlock(this.absolutePos(pos), state, 3);
    }

    public void setNight() {
        this.setDayTime(13000);
    }

    public void setDayTime(int timeOfDay) {
        this.getLevel().setDayTime((long)timeOfDay);
    }

    public void assertBlockPresent(Block block, int x, int y, int z) {
        this.assertBlockPresent(block, new BlockPos(x, y, z));
    }

    public void assertBlockPresent(Block block, BlockPos pos) {
        BlockState blockState = this.getBlockState(pos);
        this.assertBlock(pos, (block1) -> {
            return blockState.is(block);
        }, "Expected " + block.getName().getString() + ", got " + blockState.getBlock().getName().getString());
    }

    public void assertBlockNotPresent(Block block, int x, int y, int z) {
        this.assertBlockNotPresent(block, new BlockPos(x, y, z));
    }

    public void assertBlockNotPresent(Block block, BlockPos pos) {
        this.assertBlock(pos, (block1) -> {
            return !this.getBlockState(pos).is(block);
        }, "Did not expect " + block.getName().getString());
    }

    public void succeedWhenBlockPresent(Block block, int x, int y, int z) {
        this.succeedWhenBlockPresent(block, new BlockPos(x, y, z));
    }

    public void succeedWhenBlockPresent(Block block, BlockPos pos) {
        this.succeedWhen(() -> {
            this.assertBlockPresent(block, pos);
        });
    }

    public void assertBlock(BlockPos pos, Predicate<Block> predicate, String errorMessage) {
        this.assertBlock(pos, predicate, () -> {
            return errorMessage;
        });
    }

    public void assertBlock(BlockPos pos, Predicate<Block> predicate, Supplier<String> errorMessageSupplier) {
        this.assertBlockState(pos, (state) -> {
            return predicate.test(state.getBlock());
        }, errorMessageSupplier);
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, T value) {
        this.assertBlockState(pos, (state) -> {
            return state.hasProperty(property) && state.<T>getValue(property).equals(value);
        }, () -> {
            return "Expected property " + property.getName() + " to be " + value;
        });
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, Predicate<T> predicate, String errorMessage) {
        this.assertBlockState(pos, (state) -> {
            return predicate.test(state.getValue(property));
        }, () -> {
            return errorMessage;
        });
    }

    public void assertBlockState(BlockPos pos, Predicate<BlockState> predicate, Supplier<String> errorMessageSupplier) {
        BlockState blockState = this.getBlockState(pos);
        if (!predicate.test(blockState)) {
            throw new GameTestAssertPosException(errorMessageSupplier.get(), this.absolutePos(pos), pos, this.testInfo.getTick());
        }
    }

    public void assertEntityPresent(EntityType<?> type) {
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertException("Expected " + type.toShortString() + " to exist");
        }
    }

    public void assertEntityPresent(EntityType<?> type, int x, int y, int z) {
        this.assertEntityPresent(type, new BlockPos(x, y, z));
    }

    public void assertEntityPresent(EntityType<?> type, BlockPos pos) {
        BlockPos blockPos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(type, new AABB(blockPos), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertPosException("Expected " + type.toShortString(), blockPos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityPresent(EntityType<?> type, BlockPos pos, double radius) {
        BlockPos blockPos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(type, (new AABB(blockPos)).inflate(radius), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertPosException("Expected " + type.toShortString(), blockPos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityInstancePresent(Entity entity, int x, int y, int z) {
        this.assertEntityInstancePresent(entity, new BlockPos(x, y, z));
    }

    public void assertEntityInstancePresent(Entity entity, BlockPos pos) {
        BlockPos blockPos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(entity.getType(), new AABB(blockPos), Entity::isAlive);
        list.stream().filter((e) -> {
            return e == entity;
        }).findFirst().orElseThrow(() -> {
            return new GameTestAssertPosException("Expected " + entity.getType().toShortString(), blockPos, pos, this.testInfo.getTick());
        });
    }

    public void assertItemEntityCountIs(Item item, BlockPos pos, double radius, int amount) {
        BlockPos blockPos = this.absolutePos(pos);
        List<ItemEntity> list = this.getLevel().getEntities(EntityType.ITEM, (new AABB(blockPos)).inflate(radius), Entity::isAlive);
        int i = 0;

        for(Entity entity : list) {
            ItemEntity itemEntity = (ItemEntity)entity;
            if (itemEntity.getItem().getItem().equals(item)) {
                i += itemEntity.getItem().getCount();
            }
        }

        if (i != amount) {
            throw new GameTestAssertPosException("Expected " + amount + " " + item.getDescription().getString() + " items to exist (found " + i + ")", blockPos, pos, this.testInfo.getTick());
        }
    }

    public void assertItemEntityPresent(Item item, BlockPos pos, double radius) {
        BlockPos blockPos = this.absolutePos(pos);

        for(Entity entity : this.getLevel().getEntities(EntityType.ITEM, (new AABB(blockPos)).inflate(radius), Entity::isAlive)) {
            ItemEntity itemEntity = (ItemEntity)entity;
            if (itemEntity.getItem().getItem().equals(item)) {
                return;
            }
        }

        throw new GameTestAssertPosException("Expected " + item.getDescription().getString() + " item", blockPos, pos, this.testInfo.getTick());
    }

    public void assertEntityNotPresent(EntityType<?> type) {
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), Entity::isAlive);
        if (!list.isEmpty()) {
            throw new GameTestAssertException("Did not expect " + type.toShortString() + " to exist");
        }
    }

    public void assertEntityNotPresent(EntityType<?> type, int x, int y, int z) {
        this.assertEntityNotPresent(type, new BlockPos(x, y, z));
    }

    public void assertEntityNotPresent(EntityType<?> type, BlockPos pos) {
        BlockPos blockPos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(type, new AABB(blockPos), Entity::isAlive);
        if (!list.isEmpty()) {
            throw new GameTestAssertPosException("Did not expect " + type.toShortString(), blockPos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityTouching(EntityType<?> type, double x, double y, double z) {
        Vec3 vec3 = new Vec3(x, y, z);
        Vec3 vec32 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = (entity) -> {
            return entity.getBoundingBox().intersects(vec32, vec32);
        };
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), predicate);
        if (list.isEmpty()) {
            throw new GameTestAssertException("Expected " + type.toShortString() + " to touch " + vec32 + " (relative " + vec3 + ")");
        }
    }

    public void assertEntityNotTouching(EntityType<?> type, double x, double y, double z) {
        Vec3 vec3 = new Vec3(x, y, z);
        Vec3 vec32 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = (entity) -> {
            return !entity.getBoundingBox().intersects(vec32, vec32);
        };
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), predicate);
        if (list.isEmpty()) {
            throw new GameTestAssertException("Did not expect " + type.toShortString() + " to touch " + vec32 + " (relative " + vec3 + ")");
        }
    }

    public <E extends Entity, T> void assertEntityData(BlockPos pos, EntityType<E> type, Function<? super E, T> entityDataGetter, @Nullable T data) {
        BlockPos blockPos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(type, new AABB(blockPos), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertPosException("Expected " + type.toShortString(), blockPos, pos, this.testInfo.getTick());
        } else {
            for(E entity : list) {
                T object = entityDataGetter.apply(entity);
                if (object == null) {
                    if (data != null) {
                        throw new GameTestAssertException("Expected entity data to be: " + data + ", but was: " + object);
                    }
                } else if (!object.equals(data)) {
                    throw new GameTestAssertException("Expected entity data to be: " + data + ", but was: " + object);
                }
            }

        }
    }

    public void assertContainerEmpty(BlockPos pos) {
        BlockPos blockPos = this.absolutePos(pos);
        BlockEntity blockEntity = this.getLevel().getBlockEntity(blockPos);
        if (blockEntity instanceof BaseContainerBlockEntity && !((BaseContainerBlockEntity)blockEntity).isEmpty()) {
            throw new GameTestAssertException("Container should be empty");
        }
    }

    public void assertContainerContains(BlockPos pos, Item item) {
        BlockPos blockPos = this.absolutePos(pos);
        BlockEntity blockEntity = this.getLevel().getBlockEntity(blockPos);
        if (blockEntity instanceof BaseContainerBlockEntity && ((BaseContainerBlockEntity)blockEntity).countItem(item) != 1) {
            throw new GameTestAssertException("Container should contain: " + item);
        }
    }

    public void assertSameBlockStates(BoundingBox checkedBlockBox, BlockPos correctStatePos) {
        BlockPos.betweenClosedStream(checkedBlockBox).forEach((checkedPos) -> {
            BlockPos blockPos2 = correctStatePos.offset(checkedPos.getX() - checkedBlockBox.minX(), checkedPos.getY() - checkedBlockBox.minY(), checkedPos.getZ() - checkedBlockBox.minZ());
            this.assertSameBlockState(checkedPos, blockPos2);
        });
    }

    public void assertSameBlockState(BlockPos checkedPos, BlockPos correctStatePos) {
        BlockState blockState = this.getBlockState(checkedPos);
        BlockState blockState2 = this.getBlockState(correctStatePos);
        if (blockState != blockState2) {
            this.fail("Incorrect state. Expected " + blockState2 + ", got " + blockState, checkedPos);
        }

    }

    public void assertAtTickTimeContainerContains(long delay, BlockPos pos, Item item) {
        this.runAtTickTime(delay, () -> {
            this.assertContainerContains(pos, item);
        });
    }

    public void assertAtTickTimeContainerEmpty(long delay, BlockPos pos) {
        this.runAtTickTime(delay, () -> {
            this.assertContainerEmpty(pos);
        });
    }

    public <E extends Entity, T> void succeedWhenEntityData(BlockPos pos, EntityType<E> type, Function<E, T> entityDataGetter, T data) {
        this.succeedWhen(() -> {
            this.assertEntityData(pos, type, entityDataGetter, data);
        });
    }

    public <E extends Entity> void assertEntityProperty(E entity, Predicate<E> predicate, String testName) {
        if (!predicate.test(entity)) {
            throw new GameTestAssertException("Entity " + entity + " failed " + testName + " test");
        }
    }

    public <E extends Entity, T> void assertEntityProperty(E entity, Function<E, T> propertyGetter, String propertyName, T expectedValue) {
        T object = propertyGetter.apply(entity);
        if (!object.equals(expectedValue)) {
            throw new GameTestAssertException("Entity " + entity + " value " + propertyName + "=" + object + " is not equal to expected " + expectedValue);
        }
    }

    public void succeedWhenEntityPresent(EntityType<?> type, int x, int y, int z) {
        this.succeedWhenEntityPresent(type, new BlockPos(x, y, z));
    }

    public void succeedWhenEntityPresent(EntityType<?> type, BlockPos pos) {
        this.succeedWhen(() -> {
            this.assertEntityPresent(type, pos);
        });
    }

    public void succeedWhenEntityNotPresent(EntityType<?> type, int x, int y, int z) {
        this.succeedWhenEntityNotPresent(type, new BlockPos(x, y, z));
    }

    public void succeedWhenEntityNotPresent(EntityType<?> type, BlockPos pos) {
        this.succeedWhen(() -> {
            this.assertEntityNotPresent(type, pos);
        });
    }

    public void succeed() {
        this.testInfo.succeed();
    }

    private void ensureSingleFinalCheck() {
        if (this.finalCheckAdded) {
            throw new IllegalStateException("This test already has final clause");
        } else {
            this.finalCheckAdded = true;
        }
    }

    public void succeedIf(Runnable runnable) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(0L, runnable).thenSucceed();
    }

    public void succeedWhen(Runnable runnable) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(runnable).thenSucceed();
    }

    public void succeedOnTickWhen(int duration, Runnable runnable) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil((long)duration, runnable).thenSucceed();
    }

    public void runAtTickTime(long tick, Runnable runnable) {
        this.testInfo.setRunAtTickTime(tick, runnable);
    }

    public void runAfterDelay(long ticks, Runnable runnable) {
        this.runAtTickTime(this.testInfo.getTick() + ticks, runnable);
    }

    public void randomTick(BlockPos pos) {
        BlockPos blockPos = this.absolutePos(pos);
        ServerLevel serverLevel = this.getLevel();
        serverLevel.getBlockState(blockPos).randomTick(serverLevel, blockPos, serverLevel.random);
    }

    public void fail(String message, BlockPos pos) {
        throw new GameTestAssertPosException(message, this.absolutePos(pos), pos, this.getTick());
    }

    public void fail(String message, Entity entity) {
        throw new GameTestAssertPosException(message, entity.blockPosition(), this.relativePos(entity.blockPosition()), this.getTick());
    }

    public void fail(String message) {
        throw new GameTestAssertException(message);
    }

    public void failIf(Runnable task) {
        this.testInfo.createSequence().thenWaitUntil(task).thenFail(() -> {
            return new GameTestAssertException("Fail conditions met");
        });
    }

    public void failIfEver(Runnable task) {
        LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks()).forEach((tick) -> {
            this.testInfo.setRunAtTickTime(tick, task::run);
        });
    }

    public GameTestSequence startSequence() {
        return this.testInfo.createSequence();
    }

    public BlockPos absolutePos(BlockPos pos) {
        BlockPos blockPos = this.testInfo.getStructureBlockPos();
        BlockPos blockPos2 = blockPos.offset(pos);
        return StructureTemplate.transform(blockPos2, Mirror.NONE, this.testInfo.getRotation(), blockPos);
    }

    public BlockPos relativePos(BlockPos pos) {
        BlockPos blockPos = this.testInfo.getStructureBlockPos();
        Rotation rotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
        BlockPos blockPos2 = StructureTemplate.transform(pos, Mirror.NONE, rotation, blockPos);
        return blockPos2.subtract(blockPos);
    }

    public Vec3 absoluteVec(Vec3 pos) {
        Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
        return StructureTemplate.transform(vec3.add(pos), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
    }

    public long getTick() {
        return this.testInfo.getTick();
    }

    private AABB getBounds() {
        return this.testInfo.getStructureBounds();
    }

    private AABB getRelativeBounds() {
        AABB aABB = this.testInfo.getStructureBounds();
        return aABB.move(BlockPos.ZERO.subtract(this.absolutePos(BlockPos.ZERO)));
    }

    public void forEveryBlockInStructure(Consumer<BlockPos> posConsumer) {
        AABB aABB = this.getRelativeBounds();
        BlockPos.MutableBlockPos.betweenClosedStream(aABB.move(0.0D, 1.0D, 0.0D)).forEach(posConsumer);
    }

    public void onEachTick(Runnable runnable) {
        LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks()).forEach((tick) -> {
            this.testInfo.setRunAtTickTime(tick, runnable::run);
        });
    }
}
