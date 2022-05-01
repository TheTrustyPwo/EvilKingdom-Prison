package net.minecraft.core.dispenser;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public interface DispenseItemBehavior {
    Logger LOGGER = LogUtils.getLogger();
    DispenseItemBehavior NOOP = (pointer, stack) -> {
        return stack;
    };

    ItemStack dispense(BlockSource pointer, ItemStack stack);

    static void bootStrap() {
        DispenserBlock.registerBehavior(Items.ARROW, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level world, Position position, ItemStack stack) {
                Arrow arrow = new Arrow(world, position.x(), position.y(), position.z());
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return arrow;
            }
        });
        DispenserBlock.registerBehavior(Items.TIPPED_ARROW, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level world, Position position, ItemStack stack) {
                Arrow arrow = new Arrow(world, position.x(), position.y(), position.z());
                arrow.setEffectsFromItem(stack);
                arrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return arrow;
            }
        });
        DispenserBlock.registerBehavior(Items.SPECTRAL_ARROW, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level world, Position position, ItemStack stack) {
                AbstractArrow abstractArrow = new SpectralArrow(world, position.x(), position.y(), position.z());
                abstractArrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return abstractArrow;
            }
        });
        DispenserBlock.registerBehavior(Items.EGG, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level world, Position position, ItemStack stack) {
                return Util.make(new ThrownEgg(world, position.x(), position.y(), position.z()), (entity) -> {
                    entity.setItem(stack);
                });
            }
        });
        DispenserBlock.registerBehavior(Items.SNOWBALL, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level world, Position position, ItemStack stack) {
                return Util.make(new Snowball(world, position.x(), position.y(), position.z()), (entity) -> {
                    entity.setItem(stack);
                });
            }
        });
        DispenserBlock.registerBehavior(Items.EXPERIENCE_BOTTLE, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level world, Position position, ItemStack stack) {
                return Util.make(new ThrownExperienceBottle(world, position.x(), position.y(), position.z()), (entity) -> {
                    entity.setItem(stack);
                });
            }

            @Override
            protected float getUncertainty() {
                return super.getUncertainty() * 0.5F;
            }

            @Override
            protected float getPower() {
                return super.getPower() * 1.25F;
            }
        });
        DispenserBlock.registerBehavior(Items.SPLASH_POTION, new DispenseItemBehavior() {
            @Override
            public ItemStack dispense(BlockSource pointer, ItemStack stack) {
                return (new AbstractProjectileDispenseBehavior() {
                    @Override
                    protected Projectile getProjectile(Level world, Position position, ItemStack stack) {
                        return Util.make(new ThrownPotion(world, position.x(), position.y(), position.z()), (entity) -> {
                            entity.setItem(stack);
                        });
                    }

                    @Override
                    protected float getUncertainty() {
                        return super.getUncertainty() * 0.5F;
                    }

                    @Override
                    protected float getPower() {
                        return super.getPower() * 1.25F;
                    }
                }).dispense(pointer, stack);
            }
        });
        DispenserBlock.registerBehavior(Items.LINGERING_POTION, new DispenseItemBehavior() {
            @Override
            public ItemStack dispense(BlockSource pointer, ItemStack stack) {
                return (new AbstractProjectileDispenseBehavior() {
                    @Override
                    protected Projectile getProjectile(Level world, Position position, ItemStack stack) {
                        return Util.make(new ThrownPotion(world, position.x(), position.y(), position.z()), (entity) -> {
                            entity.setItem(stack);
                        });
                    }

                    @Override
                    protected float getUncertainty() {
                        return super.getUncertainty() * 0.5F;
                    }

                    @Override
                    protected float getPower() {
                        return super.getPower() * 1.25F;
                    }
                }).dispense(pointer, stack);
            }
        });
        DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
                EntityType<?> entityType = ((SpawnEggItem)stack.getItem()).getType(stack.getTag());

                try {
                    entityType.spawn(pointer.getLevel(), stack, (Player)null, pointer.getPos().relative(direction), MobSpawnType.DISPENSER, direction != Direction.UP, false);
                } catch (Exception var6) {
                    LOGGER.error("Error while dispensing spawn egg from dispenser at {}", pointer.getPos(), var6);
                    return ItemStack.EMPTY;
                }

                stack.shrink(1);
                pointer.getLevel().gameEvent(GameEvent.ENTITY_PLACE, pointer.getPos());
                return stack;
            }
        };

        for(SpawnEggItem spawnEggItem : SpawnEggItem.eggs()) {
            DispenserBlock.registerBehavior(spawnEggItem, defaultDispenseItemBehavior);
        }

        DispenserBlock.registerBehavior(Items.ARMOR_STAND, new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos blockPos = pointer.getPos().relative(direction);
                Level level = pointer.getLevel();
                ArmorStand armorStand = new ArmorStand(level, (double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D);
                EntityType.updateCustomEntityTag(level, (Player)null, armorStand, stack.getTag());
                armorStand.setYRot(direction.toYRot());
                level.addFreshEntity(armorStand);
                stack.shrink(1);
                return stack;
            }
        });
        DispenserBlock.registerBehavior(Items.SADDLE, new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
                List<LivingEntity> list = pointer.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(blockPos), (entity) -> {
                    if (!(entity instanceof Saddleable)) {
                        return false;
                    } else {
                        Saddleable saddleable = (Saddleable)entity;
                        return !saddleable.isSaddled() && saddleable.isSaddleable();
                    }
                });
                if (!list.isEmpty()) {
                    ((Saddleable)list.get(0)).equipSaddle(SoundSource.BLOCKS);
                    stack.shrink(1);
                    this.setSuccess(true);
                    return stack;
                } else {
                    return super.execute(pointer, stack);
                }
            }
        });
        DefaultDispenseItemBehavior defaultDispenseItemBehavior2 = new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));

                for(AbstractHorse abstractHorse : pointer.getLevel().getEntitiesOfClass(AbstractHorse.class, new AABB(blockPos), (entity) -> {
                    return entity.isAlive() && entity.canWearArmor();
                })) {
                    if (abstractHorse.isArmor(stack) && !abstractHorse.isWearingArmor() && abstractHorse.isTamed()) {
                        abstractHorse.getSlot(401).set(stack.split(1));
                        this.setSuccess(true);
                        return stack;
                    }
                }

                return super.execute(pointer, stack);
            }
        };
        DispenserBlock.registerBehavior(Items.LEATHER_HORSE_ARMOR, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.IRON_HORSE_ARMOR, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.GOLDEN_HORSE_ARMOR, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.DIAMOND_HORSE_ARMOR, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.WHITE_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.ORANGE_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.CYAN_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.BLUE_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.BROWN_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.BLACK_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.GRAY_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.GREEN_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.LIGHT_BLUE_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.LIGHT_GRAY_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.LIME_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.MAGENTA_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.PINK_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.PURPLE_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.RED_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.YELLOW_CARPET, defaultDispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.CHEST, new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));

                for(AbstractChestedHorse abstractChestedHorse : pointer.getLevel().getEntitiesOfClass(AbstractChestedHorse.class, new AABB(blockPos), (entity) -> {
                    return entity.isAlive() && !entity.hasChest();
                })) {
                    if (abstractChestedHorse.isTamed() && abstractChestedHorse.getSlot(499).set(stack)) {
                        stack.shrink(1);
                        this.setSuccess(true);
                        return stack;
                    }
                }

                return super.execute(pointer, stack);
            }
        });
        DispenserBlock.registerBehavior(Items.FIREWORK_ROCKET, new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
                FireworkRocketEntity fireworkRocketEntity = new FireworkRocketEntity(pointer.getLevel(), stack, pointer.x(), pointer.y(), pointer.x(), true);
                DispenseItemBehavior.setEntityPokingOutOfBlock(pointer, fireworkRocketEntity, direction);
                fireworkRocketEntity.shoot((double)direction.getStepX(), (double)direction.getStepY(), (double)direction.getStepZ(), 0.5F, 1.0F);
                pointer.getLevel().addFreshEntity(fireworkRocketEntity);
                stack.shrink(1);
                return stack;
            }

            @Override
            protected void playSound(BlockSource pointer) {
                pointer.getLevel().levelEvent(1004, pointer.getPos(), 0);
            }
        });
        DispenserBlock.registerBehavior(Items.FIRE_CHARGE, new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
                Position position = DispenserBlock.getDispensePosition(pointer);
                double d = position.x() + (double)((float)direction.getStepX() * 0.3F);
                double e = position.y() + (double)((float)direction.getStepY() * 0.3F);
                double f = position.z() + (double)((float)direction.getStepZ() * 0.3F);
                Level level = pointer.getLevel();
                Random random = level.random;
                double g = random.nextGaussian() * 0.05D + (double)direction.getStepX();
                double h = random.nextGaussian() * 0.05D + (double)direction.getStepY();
                double i = random.nextGaussian() * 0.05D + (double)direction.getStepZ();
                SmallFireball smallFireball = new SmallFireball(level, d, e, f, g, h, i);
                level.addFreshEntity(Util.make(smallFireball, (entity) -> {
                    entity.setItem(stack);
                }));
                stack.shrink(1);
                return stack;
            }

            @Override
            protected void playSound(BlockSource pointer) {
                pointer.getLevel().levelEvent(1018, pointer.getPos(), 0);
            }
        });
        DispenserBlock.registerBehavior(Items.OAK_BOAT, new BoatDispenseItemBehavior(Boat.Type.OAK));
        DispenserBlock.registerBehavior(Items.SPRUCE_BOAT, new BoatDispenseItemBehavior(Boat.Type.SPRUCE));
        DispenserBlock.registerBehavior(Items.BIRCH_BOAT, new BoatDispenseItemBehavior(Boat.Type.BIRCH));
        DispenserBlock.registerBehavior(Items.JUNGLE_BOAT, new BoatDispenseItemBehavior(Boat.Type.JUNGLE));
        DispenserBlock.registerBehavior(Items.DARK_OAK_BOAT, new BoatDispenseItemBehavior(Boat.Type.DARK_OAK));
        DispenserBlock.registerBehavior(Items.ACACIA_BOAT, new BoatDispenseItemBehavior(Boat.Type.ACACIA));
        DispenseItemBehavior dispenseItemBehavior = new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                DispensibleContainerItem dispensibleContainerItem = (DispensibleContainerItem)stack.getItem();
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
                Level level = pointer.getLevel();
                if (dispensibleContainerItem.emptyContents((Player)null, level, blockPos, (BlockHitResult)null)) {
                    dispensibleContainerItem.checkExtraContent((Player)null, level, stack, blockPos);
                    return new ItemStack(Items.BUCKET);
                } else {
                    return this.defaultDispenseItemBehavior.dispense(pointer, stack);
                }
            }
        };
        DispenserBlock.registerBehavior(Items.LAVA_BUCKET, dispenseItemBehavior);
        DispenserBlock.registerBehavior(Items.WATER_BUCKET, dispenseItemBehavior);
        DispenserBlock.registerBehavior(Items.POWDER_SNOW_BUCKET, dispenseItemBehavior);
        DispenserBlock.registerBehavior(Items.SALMON_BUCKET, dispenseItemBehavior);
        DispenserBlock.registerBehavior(Items.COD_BUCKET, dispenseItemBehavior);
        DispenserBlock.registerBehavior(Items.PUFFERFISH_BUCKET, dispenseItemBehavior);
        DispenserBlock.registerBehavior(Items.TROPICAL_FISH_BUCKET, dispenseItemBehavior);
        DispenserBlock.registerBehavior(Items.AXOLOTL_BUCKET, dispenseItemBehavior);
        DispenserBlock.registerBehavior(Items.BUCKET, new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                LevelAccessor levelAccessor = pointer.getLevel();
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
                BlockState blockState = levelAccessor.getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (block instanceof BucketPickup) {
                    ItemStack itemStack = ((BucketPickup)block).pickupBlock(levelAccessor, blockPos, blockState);
                    if (itemStack.isEmpty()) {
                        return super.execute(pointer, stack);
                    } else {
                        levelAccessor.gameEvent((Entity)null, GameEvent.FLUID_PICKUP, blockPos);
                        Item item = itemStack.getItem();
                        stack.shrink(1);
                        if (stack.isEmpty()) {
                            return new ItemStack(item);
                        } else {
                            if (pointer.<DispenserBlockEntity>getEntity().addItem(new ItemStack(item)) < 0) {
                                this.defaultDispenseItemBehavior.dispense(pointer, new ItemStack(item));
                            }

                            return stack;
                        }
                    }
                } else {
                    return super.execute(pointer, stack);
                }
            }
        });
        DispenserBlock.registerBehavior(Items.FLINT_AND_STEEL, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                Level level = pointer.getLevel();
                this.setSuccess(true);
                Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos blockPos = pointer.getPos().relative(direction);
                BlockState blockState = level.getBlockState(blockPos);
                if (BaseFireBlock.canBePlacedAt(level, blockPos, direction)) {
                    level.setBlockAndUpdate(blockPos, BaseFireBlock.getState(level, blockPos));
                    level.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, blockPos);
                } else if (!CampfireBlock.canLight(blockState) && !CandleBlock.canLight(blockState) && !CandleCakeBlock.canLight(blockState)) {
                    if (blockState.getBlock() instanceof TntBlock) {
                        TntBlock.explode(level, blockPos);
                        level.removeBlock(blockPos, false);
                    } else {
                        this.setSuccess(false);
                    }
                } else {
                    level.setBlockAndUpdate(blockPos, blockState.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)));
                    level.gameEvent((Entity)null, GameEvent.BLOCK_CHANGE, blockPos);
                }

                if (this.isSuccess() && stack.hurt(1, level.random, (ServerPlayer)null)) {
                    stack.setCount(0);
                }

                return stack;
            }
        });
        DispenserBlock.registerBehavior(Items.BONE_MEAL, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                this.setSuccess(true);
                Level level = pointer.getLevel();
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
                if (!BoneMealItem.growCrop(stack, level, blockPos) && !BoneMealItem.growWaterPlant(stack, level, blockPos, (Direction)null)) {
                    this.setSuccess(false);
                } else if (!level.isClientSide) {
                    level.levelEvent(1505, blockPos, 0);
                }

                return stack;
            }
        });
        DispenserBlock.registerBehavior(Blocks.TNT, new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                Level level = pointer.getLevel();
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
                PrimedTnt primedTnt = new PrimedTnt(level, (double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, (LivingEntity)null);
                level.addFreshEntity(primedTnt);
                level.playSound((Player)null, primedTnt.getX(), primedTnt.getY(), primedTnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent((Entity)null, GameEvent.ENTITY_PLACE, blockPos);
                stack.shrink(1);
                return stack;
            }
        });
        DispenseItemBehavior dispenseItemBehavior2 = new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                this.setSuccess(ArmorItem.dispenseArmor(pointer, stack));
                return stack;
            }
        };
        DispenserBlock.registerBehavior(Items.CREEPER_HEAD, dispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.ZOMBIE_HEAD, dispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.DRAGON_HEAD, dispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.SKELETON_SKULL, dispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.PLAYER_HEAD, dispenseItemBehavior2);
        DispenserBlock.registerBehavior(Items.WITHER_SKELETON_SKULL, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                Level level = pointer.getLevel();
                Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos blockPos = pointer.getPos().relative(direction);
                if (level.isEmptyBlock(blockPos) && WitherSkullBlock.canSpawnMob(level, blockPos, stack)) {
                    level.setBlock(blockPos, Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, Integer.valueOf(direction.getAxis() == Direction.Axis.Y ? 0 : direction.getOpposite().get2DDataValue() * 4)), 3);
                    level.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, blockPos);
                    BlockEntity blockEntity = level.getBlockEntity(blockPos);
                    if (blockEntity instanceof SkullBlockEntity) {
                        WitherSkullBlock.checkSpawn(level, blockPos, (SkullBlockEntity)blockEntity);
                    }

                    stack.shrink(1);
                    this.setSuccess(true);
                } else {
                    this.setSuccess(ArmorItem.dispenseArmor(pointer, stack));
                }

                return stack;
            }
        });
        DispenserBlock.registerBehavior(Blocks.CARVED_PUMPKIN, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                Level level = pointer.getLevel();
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
                CarvedPumpkinBlock carvedPumpkinBlock = (CarvedPumpkinBlock)Blocks.CARVED_PUMPKIN;
                if (level.isEmptyBlock(blockPos) && carvedPumpkinBlock.canSpawnGolem(level, blockPos)) {
                    if (!level.isClientSide) {
                        level.setBlock(blockPos, carvedPumpkinBlock.defaultBlockState(), 3);
                        level.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, blockPos);
                    }

                    stack.shrink(1);
                    this.setSuccess(true);
                } else {
                    this.setSuccess(ArmorItem.dispenseArmor(pointer, stack));
                }

                return stack;
            }
        });
        DispenserBlock.registerBehavior(Blocks.SHULKER_BOX.asItem(), new ShulkerBoxDispenseBehavior());

        for(DyeColor dyeColor : DyeColor.values()) {
            DispenserBlock.registerBehavior(ShulkerBoxBlock.getBlockByColor(dyeColor).asItem(), new ShulkerBoxDispenseBehavior());
        }

        DispenserBlock.registerBehavior(Items.GLASS_BOTTLE.asItem(), new OptionalDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

            private ItemStack takeLiquid(BlockSource pointer, ItemStack emptyBottleStack, ItemStack filledBottleStack) {
                emptyBottleStack.shrink(1);
                if (emptyBottleStack.isEmpty()) {
                    pointer.getLevel().gameEvent((Entity)null, GameEvent.FLUID_PICKUP, pointer.getPos());
                    return filledBottleStack.copy();
                } else {
                    if (pointer.<DispenserBlockEntity>getEntity().addItem(filledBottleStack.copy()) < 0) {
                        this.defaultDispenseItemBehavior.dispense(pointer, filledBottleStack.copy());
                    }

                    return emptyBottleStack;
                }
            }

            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                this.setSuccess(false);
                ServerLevel serverLevel = pointer.getLevel();
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
                BlockState blockState = serverLevel.getBlockState(blockPos);
                if (blockState.is(BlockTags.BEEHIVES, (state) -> {
                    return state.hasProperty(BeehiveBlock.HONEY_LEVEL) && state.getBlock() instanceof BeehiveBlock;
                }) && blockState.getValue(BeehiveBlock.HONEY_LEVEL) >= 5) {
                    ((BeehiveBlock)blockState.getBlock()).releaseBeesAndResetHoneyLevel(serverLevel, blockState, blockPos, (Player)null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
                    this.setSuccess(true);
                    return this.takeLiquid(pointer, stack, new ItemStack(Items.HONEY_BOTTLE));
                } else if (serverLevel.getFluidState(blockPos).is(FluidTags.WATER)) {
                    this.setSuccess(true);
                    return this.takeLiquid(pointer, stack, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
                } else {
                    return super.execute(pointer, stack);
                }
            }
        });
        DispenserBlock.registerBehavior(Items.GLOWSTONE, new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                Direction direction = pointer.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos blockPos = pointer.getPos().relative(direction);
                Level level = pointer.getLevel();
                BlockState blockState = level.getBlockState(blockPos);
                this.setSuccess(true);
                if (blockState.is(Blocks.RESPAWN_ANCHOR)) {
                    if (blockState.getValue(RespawnAnchorBlock.CHARGE) != 4) {
                        RespawnAnchorBlock.charge(level, blockPos, blockState);
                        stack.shrink(1);
                    } else {
                        this.setSuccess(false);
                    }

                    return stack;
                } else {
                    return super.execute(pointer, stack);
                }
            }
        });
        DispenserBlock.registerBehavior(Items.SHEARS.asItem(), new ShearsDispenseItemBehavior());
        DispenserBlock.registerBehavior(Items.HONEYCOMB, new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
                Level level = pointer.getLevel();
                BlockState blockState = level.getBlockState(blockPos);
                Optional<BlockState> optional = HoneycombItem.getWaxed(blockState);
                if (optional.isPresent()) {
                    level.setBlockAndUpdate(blockPos, optional.get());
                    level.levelEvent(3003, blockPos, 0);
                    stack.shrink(1);
                    this.setSuccess(true);
                    return stack;
                } else {
                    return super.execute(pointer, stack);
                }
            }
        });
    }

    static void setEntityPokingOutOfBlock(BlockSource pointer, Entity entity, Direction direction) {
        entity.setPos(pointer.x() + (double)direction.getStepX() * (0.5000099999997474D - (double)entity.getBbWidth() / 2.0D), pointer.y() + (double)direction.getStepY() * (0.5000099999997474D - (double)entity.getBbHeight() / 2.0D) - (double)entity.getBbHeight() / 2.0D, pointer.z() + (double)direction.getStepZ() * (0.5000099999997474D - (double)entity.getBbWidth() / 2.0D));
    }
}
