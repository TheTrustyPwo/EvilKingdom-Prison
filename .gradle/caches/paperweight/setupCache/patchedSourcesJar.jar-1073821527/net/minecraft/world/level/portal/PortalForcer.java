package net.minecraft.world.level.portal;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public class PortalForcer {
    private static final int TICKET_RADIUS = 3;
    private static final int SEARCH_RADIUS = 128;
    private static final int CREATE_RADIUS = 16;
    private static final int FRAME_HEIGHT = 5;
    private static final int FRAME_WIDTH = 4;
    private static final int FRAME_BOX = 3;
    private static final int FRAME_HEIGHT_START = -1;
    private static final int FRAME_HEIGHT_END = 4;
    private static final int FRAME_WIDTH_START = -1;
    private static final int FRAME_WIDTH_END = 3;
    private static final int FRAME_BOX_START = -1;
    private static final int FRAME_BOX_END = 2;
    private static final int NOTHING_FOUND = -1;
    private final ServerLevel level;

    public PortalForcer(ServerLevel world) {
        this.level = world;
    }

    public Optional<BlockUtil.FoundRectangle> findPortalAround(BlockPos pos, boolean destIsNether, WorldBorder worldBorder) {
        PoiManager poiManager = this.level.getPoiManager();
        int i = destIsNether ? 16 : 128;
        poiManager.ensureLoadedAndValid(this.level, pos, i);
        Optional<PoiRecord> optional = poiManager.getInSquare((poiType) -> {
            return poiType == PoiType.NETHER_PORTAL;
        }, pos, i, PoiManager.Occupancy.ANY).filter((poi) -> {
            return worldBorder.isWithinBounds(poi.getPos());
        }).sorted(Comparator.comparingDouble((poi) -> {
            return poi.getPos().distSqr(pos);
        }).thenComparingInt((poi) -> {
            return poi.getPos().getY();
        })).filter((poi) -> {
            return this.level.getBlockState(poi.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS);
        }).findFirst();
        return optional.map((poi) -> {
            BlockPos blockPos = poi.getPos();
            this.level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(blockPos), 3, blockPos);
            BlockState blockState = this.level.getBlockState(blockPos);
            return BlockUtil.getLargestRectangleAround(blockPos, blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, (pos) -> {
                return this.level.getBlockState(pos) == blockState;
            });
        });
    }

    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        double d = -1.0D;
        BlockPos blockPos = null;
        double e = -1.0D;
        BlockPos blockPos2 = null;
        WorldBorder worldBorder = this.level.getWorldBorder();
        int i = Math.min(this.level.getMaxBuildHeight(), this.level.getMinBuildHeight() + this.level.getLogicalHeight()) - 1;
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

        for(BlockPos.MutableBlockPos mutableBlockPos2 : BlockPos.spiralAround(pos, 16, Direction.EAST, Direction.SOUTH)) {
            int j = Math.min(i, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, mutableBlockPos2.getX(), mutableBlockPos2.getZ()));
            int k = 1;
            if (worldBorder.isWithinBounds(mutableBlockPos2) && worldBorder.isWithinBounds(mutableBlockPos2.move(direction, 1))) {
                mutableBlockPos2.move(direction.getOpposite(), 1);

                for(int l = j; l >= this.level.getMinBuildHeight(); --l) {
                    mutableBlockPos2.setY(l);
                    if (this.level.isEmptyBlock(mutableBlockPos2)) {
                        int m;
                        for(m = l; l > this.level.getMinBuildHeight() && this.level.isEmptyBlock(mutableBlockPos2.move(Direction.DOWN)); --l) {
                        }

                        if (l + 4 <= i) {
                            int n = m - l;
                            if (n <= 0 || n >= 3) {
                                mutableBlockPos2.setY(l);
                                if (this.canHostFrame(mutableBlockPos2, mutableBlockPos, direction, 0)) {
                                    double f = pos.distSqr(mutableBlockPos2);
                                    if (this.canHostFrame(mutableBlockPos2, mutableBlockPos, direction, -1) && this.canHostFrame(mutableBlockPos2, mutableBlockPos, direction, 1) && (d == -1.0D || d > f)) {
                                        d = f;
                                        blockPos = mutableBlockPos2.immutable();
                                    }

                                    if (d == -1.0D && (e == -1.0D || e > f)) {
                                        e = f;
                                        blockPos2 = mutableBlockPos2.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (d == -1.0D && e != -1.0D) {
            blockPos = blockPos2;
            d = e;
        }

        if (d == -1.0D) {
            int o = Math.max(this.level.getMinBuildHeight() - -1, 70);
            int p = i - 9;
            if (p < o) {
                return Optional.empty();
            }

            blockPos = (new BlockPos(pos.getX(), Mth.clamp(pos.getY(), o, p), pos.getZ())).immutable();
            Direction direction2 = direction.getClockWise();
            if (!worldBorder.isWithinBounds(blockPos)) {
                return Optional.empty();
            }

            for(int q = -1; q < 2; ++q) {
                for(int r = 0; r < 2; ++r) {
                    for(int s = -1; s < 3; ++s) {
                        BlockState blockState = s < 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        mutableBlockPos.setWithOffset(blockPos, r * direction.getStepX() + q * direction2.getStepX(), s, r * direction.getStepZ() + q * direction2.getStepZ());
                        this.level.setBlockAndUpdate(mutableBlockPos, blockState);
                    }
                }
            }
        }

        for(int t = -1; t < 3; ++t) {
            for(int u = -1; u < 4; ++u) {
                if (t == -1 || t == 2 || u == -1 || u == 3) {
                    mutableBlockPos.setWithOffset(blockPos, t * direction.getStepX(), u, t * direction.getStepZ());
                    this.level.setBlock(mutableBlockPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                }
            }
        }

        BlockState blockState2 = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);

        for(int v = 0; v < 2; ++v) {
            for(int w = 0; w < 3; ++w) {
                mutableBlockPos.setWithOffset(blockPos, v * direction.getStepX(), w, v * direction.getStepZ());
                this.level.setBlock(mutableBlockPos, blockState2, 18);
            }
        }

        return Optional.of(new BlockUtil.FoundRectangle(blockPos.immutable(), 2, 3));
    }

    private boolean canHostFrame(BlockPos pos, BlockPos.MutableBlockPos temp, Direction portalDirection, int distanceOrthogonalToPortal) {
        Direction direction = portalDirection.getClockWise();

        for(int i = -1; i < 3; ++i) {
            for(int j = -1; j < 4; ++j) {
                temp.setWithOffset(pos, portalDirection.getStepX() * i + direction.getStepX() * distanceOrthogonalToPortal, j, portalDirection.getStepZ() * i + direction.getStepZ() * distanceOrthogonalToPortal);
                if (j < 0 && !this.level.getBlockState(temp).getMaterial().isSolid()) {
                    return false;
                }

                if (j >= 0 && !this.level.isEmptyBlock(temp)) {
                    return false;
                }
            }
        }

        return true;
    }
}
