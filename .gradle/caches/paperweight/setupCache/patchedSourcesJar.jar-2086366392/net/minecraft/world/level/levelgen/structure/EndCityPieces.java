package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class EndCityPieces {
    private static final int MAX_GEN_DEPTH = 8;
    static final EndCityPieces.SectionGenerator HOUSE_TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
        @Override
        public void init() {
        }

        @Override
        public boolean generate(StructureManager manager, int depth, EndCityPieces.EndCityPiece root, BlockPos pos, List<StructurePiece> pieces, Random random) {
            if (depth > 8) {
                return false;
            } else {
                Rotation rotation = root.placeSettings.getRotation();
                EndCityPieces.EndCityPiece endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, root, pos, "base_floor", rotation, true));
                int i = random.nextInt(3);
                if (i == 0) {
                    EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-1, 4, -1), "base_roof", rotation, true));
                } else if (i == 1) {
                    endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-1, 0, -1), "second_floor_2", rotation, false));
                    endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-1, 8, -1), "second_roof", rotation, false));
                    EndCityPieces.recursiveChildren(manager, EndCityPieces.TOWER_GENERATOR, depth + 1, endCityPiece, (BlockPos)null, pieces, random);
                } else if (i == 2) {
                    endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-1, 0, -1), "second_floor_2", rotation, false));
                    endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-1, 4, -1), "third_floor_2", rotation, false));
                    endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-1, 8, -1), "third_roof", rotation, true));
                    EndCityPieces.recursiveChildren(manager, EndCityPieces.TOWER_GENERATOR, depth + 1, endCityPiece, (BlockPos)null, pieces, random);
                }

                return true;
            }
        }
    };
    static final List<Tuple<Rotation, BlockPos>> TOWER_BRIDGES = Lists.newArrayList(new Tuple<>(Rotation.NONE, new BlockPos(1, -1, 0)), new Tuple<>(Rotation.CLOCKWISE_90, new BlockPos(6, -1, 1)), new Tuple<>(Rotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 5)), new Tuple<>(Rotation.CLOCKWISE_180, new BlockPos(5, -1, 6)));
    static final EndCityPieces.SectionGenerator TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
        @Override
        public void init() {
        }

        @Override
        public boolean generate(StructureManager manager, int depth, EndCityPieces.EndCityPiece root, BlockPos pos, List<StructurePiece> pieces, Random random) {
            Rotation rotation = root.placeSettings.getRotation();
            EndCityPieces.EndCityPiece endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, root, new BlockPos(3 + random.nextInt(2), -3, 3 + random.nextInt(2)), "tower_base", rotation, true));
            endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(0, 7, 0), "tower_piece", rotation, true));
            EndCityPieces.EndCityPiece endCityPiece2 = random.nextInt(3) == 0 ? endCityPiece : null;
            int i = 1 + random.nextInt(3);

            for(int j = 0; j < i; ++j) {
                endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(0, 4, 0), "tower_piece", rotation, true));
                if (j < i - 1 && random.nextBoolean()) {
                    endCityPiece2 = endCityPiece;
                }
            }

            if (endCityPiece2 != null) {
                for(Tuple<Rotation, BlockPos> tuple : EndCityPieces.TOWER_BRIDGES) {
                    if (random.nextBoolean()) {
                        EndCityPieces.EndCityPiece endCityPiece3 = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece2, tuple.getB(), "bridge_end", rotation.getRotated(tuple.getA()), true));
                        EndCityPieces.recursiveChildren(manager, EndCityPieces.TOWER_BRIDGE_GENERATOR, depth + 1, endCityPiece3, (BlockPos)null, pieces, random);
                    }
                }

                EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-1, 4, -1), "tower_top", rotation, true));
            } else {
                if (depth != 7) {
                    return EndCityPieces.recursiveChildren(manager, EndCityPieces.FAT_TOWER_GENERATOR, depth + 1, endCityPiece, (BlockPos)null, pieces, random);
                }

                EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-1, 4, -1), "tower_top", rotation, true));
            }

            return true;
        }
    };
    static final EndCityPieces.SectionGenerator TOWER_BRIDGE_GENERATOR = new EndCityPieces.SectionGenerator() {
        public boolean shipCreated;

        @Override
        public void init() {
            this.shipCreated = false;
        }

        @Override
        public boolean generate(StructureManager manager, int depth, EndCityPieces.EndCityPiece root, BlockPos pos, List<StructurePiece> pieces, Random random) {
            Rotation rotation = root.placeSettings.getRotation();
            int i = random.nextInt(4) + 1;
            EndCityPieces.EndCityPiece endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, root, new BlockPos(0, 0, -4), "bridge_piece", rotation, true));
            endCityPiece.genDepth = -1;
            int j = 0;

            for(int k = 0; k < i; ++k) {
                if (random.nextBoolean()) {
                    endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(0, j, -4), "bridge_piece", rotation, true));
                    j = 0;
                } else {
                    if (random.nextBoolean()) {
                        endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(0, j, -4), "bridge_steep_stairs", rotation, true));
                    } else {
                        endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(0, j, -8), "bridge_gentle_stairs", rotation, true));
                    }

                    j = 4;
                }
            }

            if (!this.shipCreated && random.nextInt(10 - depth) == 0) {
                EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-8 + random.nextInt(8), j, -70 + random.nextInt(10)), "ship", rotation, true));
                this.shipCreated = true;
            } else if (!EndCityPieces.recursiveChildren(manager, EndCityPieces.HOUSE_TOWER_GENERATOR, depth + 1, endCityPiece, new BlockPos(-3, j + 1, -11), pieces, random)) {
                return false;
            }

            endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(4, j, 0), "bridge_end", rotation.getRotated(Rotation.CLOCKWISE_180), true));
            endCityPiece.genDepth = -1;
            return true;
        }
    };
    static final List<Tuple<Rotation, BlockPos>> FAT_TOWER_BRIDGES = Lists.newArrayList(new Tuple<>(Rotation.NONE, new BlockPos(4, -1, 0)), new Tuple<>(Rotation.CLOCKWISE_90, new BlockPos(12, -1, 4)), new Tuple<>(Rotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 8)), new Tuple<>(Rotation.CLOCKWISE_180, new BlockPos(8, -1, 12)));
    static final EndCityPieces.SectionGenerator FAT_TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
        @Override
        public void init() {
        }

        @Override
        public boolean generate(StructureManager manager, int depth, EndCityPieces.EndCityPiece root, BlockPos pos, List<StructurePiece> pieces, Random random) {
            Rotation rotation = root.placeSettings.getRotation();
            EndCityPieces.EndCityPiece endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, root, new BlockPos(-3, 4, -3), "fat_tower_base", rotation, true));
            endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(0, 4, 0), "fat_tower_middle", rotation, true));

            for(int i = 0; i < 2 && random.nextInt(3) != 0; ++i) {
                endCityPiece = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(0, 8, 0), "fat_tower_middle", rotation, true));

                for(Tuple<Rotation, BlockPos> tuple : EndCityPieces.FAT_TOWER_BRIDGES) {
                    if (random.nextBoolean()) {
                        EndCityPieces.EndCityPiece endCityPiece2 = EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, tuple.getB(), "bridge_end", rotation.getRotated(tuple.getA()), true));
                        EndCityPieces.recursiveChildren(manager, EndCityPieces.TOWER_BRIDGE_GENERATOR, depth + 1, endCityPiece2, (BlockPos)null, pieces, random);
                    }
                }
            }

            EndCityPieces.addHelper(pieces, EndCityPieces.addPiece(manager, endCityPiece, new BlockPos(-2, 8, -2), "fat_tower_top", rotation, true));
            return true;
        }
    };

    static EndCityPieces.EndCityPiece addPiece(StructureManager structureManager, EndCityPieces.EndCityPiece lastPiece, BlockPos relativePosition, String template, Rotation rotation, boolean ignoreAir) {
        EndCityPieces.EndCityPiece endCityPiece = new EndCityPieces.EndCityPiece(structureManager, template, lastPiece.templatePosition, rotation, ignoreAir);
        BlockPos blockPos = lastPiece.template.calculateConnectedPosition(lastPiece.placeSettings, relativePosition, endCityPiece.placeSettings, BlockPos.ZERO);
        endCityPiece.move(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        return endCityPiece;
    }

    public static void startHouseTower(StructureManager structureManager, BlockPos pos, Rotation rotation, List<StructurePiece> pieces, Random random) {
        FAT_TOWER_GENERATOR.init();
        HOUSE_TOWER_GENERATOR.init();
        TOWER_BRIDGE_GENERATOR.init();
        TOWER_GENERATOR.init();
        EndCityPieces.EndCityPiece endCityPiece = addHelper(pieces, new EndCityPieces.EndCityPiece(structureManager, "base_floor", pos, rotation, true));
        endCityPiece = addHelper(pieces, addPiece(structureManager, endCityPiece, new BlockPos(-1, 0, -1), "second_floor_1", rotation, false));
        endCityPiece = addHelper(pieces, addPiece(structureManager, endCityPiece, new BlockPos(-1, 4, -1), "third_floor_1", rotation, false));
        endCityPiece = addHelper(pieces, addPiece(structureManager, endCityPiece, new BlockPos(-1, 8, -1), "third_roof", rotation, true));
        recursiveChildren(structureManager, TOWER_GENERATOR, 1, endCityPiece, (BlockPos)null, pieces, random);
    }

    static EndCityPieces.EndCityPiece addHelper(List<StructurePiece> pieces, EndCityPieces.EndCityPiece piece) {
        pieces.add(piece);
        return piece;
    }

    static boolean recursiveChildren(StructureManager manager, EndCityPieces.SectionGenerator piece, int depth, EndCityPieces.EndCityPiece parent, BlockPos pos, List<StructurePiece> pieces, Random random) {
        if (depth > 8) {
            return false;
        } else {
            List<StructurePiece> list = Lists.newArrayList();
            if (piece.generate(manager, depth, parent, pos, list, random)) {
                boolean bl = false;
                int i = random.nextInt();

                for(StructurePiece structurePiece : list) {
                    structurePiece.genDepth = i;
                    StructurePiece structurePiece2 = StructurePiece.findCollisionPiece(pieces, structurePiece.getBoundingBox());
                    if (structurePiece2 != null && structurePiece2.genDepth != parent.genDepth) {
                        bl = true;
                        break;
                    }
                }

                if (!bl) {
                    pieces.addAll(list);
                    return true;
                }
            }

            return false;
        }
    }

    public static class EndCityPiece extends TemplateStructurePiece {
        public EndCityPiece(StructureManager manager, String template, BlockPos pos, Rotation rotation, boolean includeAir) {
            super(StructurePieceType.END_CITY_PIECE, 0, manager, makeResourceLocation(template), template, makeSettings(includeAir, rotation), pos);
        }

        public EndCityPiece(StructureManager manager, CompoundTag nbt) {
            super(StructurePieceType.END_CITY_PIECE, nbt, manager, (resourceLocation) -> {
                return makeSettings(nbt.getBoolean("OW"), Rotation.valueOf(nbt.getString("Rot")));
            });
        }

        private static StructurePlaceSettings makeSettings(boolean includeAir, Rotation rotation) {
            BlockIgnoreProcessor blockIgnoreProcessor = includeAir ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR;
            return (new StructurePlaceSettings()).setIgnoreEntities(true).addProcessor(blockIgnoreProcessor).setRotation(rotation);
        }

        @Override
        protected ResourceLocation makeTemplateLocation() {
            return makeResourceLocation(this.templateName);
        }

        private static ResourceLocation makeResourceLocation(String template) {
            return new ResourceLocation("end_city/" + template);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putString("Rot", this.placeSettings.getRotation().name());
            nbt.putBoolean("OW", this.placeSettings.getProcessors().get(0) == BlockIgnoreProcessor.STRUCTURE_BLOCK);
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPos pos, ServerLevelAccessor world, Random random, BoundingBox boundingBox) {
            if (metadata.startsWith("Chest")) {
                BlockPos blockPos = pos.below();
                if (boundingBox.isInside(blockPos)) {
                    RandomizableContainerBlockEntity.setLootTable(world, random, blockPos, BuiltInLootTables.END_CITY_TREASURE);
                }
            } else if (boundingBox.isInside(pos) && Level.isInSpawnableBounds(pos)) {
                if (metadata.startsWith("Sentry")) {
                    Shulker shulker = EntityType.SHULKER.create(world.getLevel());
                    shulker.setPos((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D);
                    world.addFreshEntity(shulker);
                } else if (metadata.startsWith("Elytra")) {
                    ItemFrame itemFrame = new ItemFrame(world.getLevel(), pos, this.placeSettings.getRotation().rotate(Direction.SOUTH));
                    itemFrame.setItem(new ItemStack(Items.ELYTRA), false);
                    world.addFreshEntity(itemFrame);
                }
            }

        }
    }

    interface SectionGenerator {
        void init();

        boolean generate(StructureManager manager, int depth, EndCityPieces.EndCityPiece root, BlockPos pos, List<StructurePiece> pieces, Random random);
    }
}
