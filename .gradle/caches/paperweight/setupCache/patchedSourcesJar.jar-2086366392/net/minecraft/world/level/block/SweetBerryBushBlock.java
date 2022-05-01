package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
// CraftBukkit start
import java.util.Collections;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
// CraftBukkit end

public class SweetBerryBushBlock extends BushBlock implements BonemealableBlock {

    private static final float HURT_SPEED_THRESHOLD = 0.003F;
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape SAPLING_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
    private static final VoxelShape MID_GROWTH_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public SweetBerryBushBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState) ((BlockState) this.stateDefinition.any()).setValue(SweetBerryBushBlock.AGE, 0));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return new ItemStack(Items.SWEET_BERRIES);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return (Integer) state.getValue(SweetBerryBushBlock.AGE) == 0 ? SweetBerryBushBlock.SAPLING_SHAPE : ((Integer) state.getValue(SweetBerryBushBlock.AGE) < 3 ? SweetBerryBushBlock.MID_GROWTH_SHAPE : super.getShape(state, world, pos, context));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return (Integer) state.getValue(SweetBerryBushBlock.AGE) < 3;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        int i = (Integer) state.getValue(SweetBerryBushBlock.AGE);

        if (i < 3 && random.nextInt(Math.max(1, (int) (100.0F / world.spigotConfig.sweetBerryModifier) * 5)) == 0 && world.getRawBrightness(pos.above(), 0) >= 9) { // Spigot
            CraftEventFactory.handleBlockGrowEvent(world, pos, (BlockState) state.setValue(SweetBerryBushBlock.AGE, i + 1), 2); // CraftBukkit
        }

    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!new io.papermc.paper.event.entity.EntityInsideBlockEvent(entity.getBukkitEntity(), org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock.at(world, pos)).callEvent()) { return; } // Paper
        if (entity instanceof LivingEntity && entity.getType() != EntityType.FOX && entity.getType() != EntityType.BEE) {
            entity.makeStuckInBlock(state, new Vec3(0.800000011920929D, 0.75D, 0.800000011920929D));
            if (!world.isClientSide && (Integer) state.getValue(SweetBerryBushBlock.AGE) > 0 && (entity.xOld != entity.getX() || entity.zOld != entity.getZ())) {
                double d0 = Math.abs(entity.getX() - entity.xOld);
                double d1 = Math.abs(entity.getZ() - entity.zOld);

                if (d0 >= 0.003000000026077032D || d1 >= 0.003000000026077032D) {
                    CraftEventFactory.blockDamage = CraftBlock.at(world, pos); // CraftBukkit
                    entity.hurt(DamageSource.SWEET_BERRY_BUSH, 1.0F);
                    CraftEventFactory.blockDamage = null; // CraftBukkit
                }
            }

        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int i = (Integer) state.getValue(SweetBerryBushBlock.AGE);
        boolean flag = i == 3;

        if (!flag && player.getItemInHand(hand).is(Items.BONE_MEAL)) {
            return InteractionResult.PASS;
        } else if (i > 1) {
            int j = 1 + world.random.nextInt(2);

            // CraftBukkit start
            PlayerHarvestBlockEvent event = CraftEventFactory.callPlayerHarvestBlockEvent(world, pos, player, Collections.singletonList(new ItemStack(Items.SWEET_BERRIES, j + (flag ? 1 : 0))));
            if (event.isCancelled()) {
                return InteractionResult.SUCCESS; // We need to return a success either way, because making it PASS or FAIL will result in a bug where cancelling while harvesting w/ block in hand places block
            }
            for (org.bukkit.inventory.ItemStack itemStack : event.getItemsHarvested()) {
                popResource(world, pos, CraftItemStack.asNMSCopy(itemStack));
            }
            // CraftBukkit end
            world.playSound((Player) null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
            world.setBlock(pos, (BlockState) state.setValue(SweetBerryBushBlock.AGE, 1), 2);
            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return super.use(state, world, pos, player, hand, hit);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SweetBerryBushBlock.AGE);
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean isClient) {
        return (Integer) state.getValue(SweetBerryBushBlock.AGE) < 3;
    }

    @Override
    public boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state) {
        int i = Math.min(3, (Integer) state.getValue(SweetBerryBushBlock.AGE) + 1);

        world.setBlock(pos, (BlockState) state.setValue(SweetBerryBushBlock.AGE, i), 2);
    }
}
