package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class OceanMonumentPieces {
    private OceanMonumentPieces() {
    }

    static class FitDoubleXRoom implements OceanMonumentPieces.MonumentRoomFitter {
        @Override
        public boolean fits(OceanMonumentPieces.RoomDefinition setting) {
            return setting.hasOpening[Direction.EAST.get3DDataValue()] && !setting.connections[Direction.EAST.get3DDataValue()].claimed;
        }

        @Override
        public OceanMonumentPieces.OceanMonumentPiece create(Direction direction, OceanMonumentPieces.RoomDefinition setting, Random random) {
            setting.claimed = true;
            setting.connections[Direction.EAST.get3DDataValue()].claimed = true;
            return new OceanMonumentPieces.OceanMonumentDoubleXRoom(direction, setting);
        }
    }

    static class FitDoubleXYRoom implements OceanMonumentPieces.MonumentRoomFitter {
        @Override
        public boolean fits(OceanMonumentPieces.RoomDefinition setting) {
            if (setting.hasOpening[Direction.EAST.get3DDataValue()] && !setting.connections[Direction.EAST.get3DDataValue()].claimed && setting.hasOpening[Direction.UP.get3DDataValue()] && !setting.connections[Direction.UP.get3DDataValue()].claimed) {
                OceanMonumentPieces.RoomDefinition roomDefinition = setting.connections[Direction.EAST.get3DDataValue()];
                return roomDefinition.hasOpening[Direction.UP.get3DDataValue()] && !roomDefinition.connections[Direction.UP.get3DDataValue()].claimed;
            } else {
                return false;
            }
        }

        @Override
        public OceanMonumentPieces.OceanMonumentPiece create(Direction direction, OceanMonumentPieces.RoomDefinition setting, Random random) {
            setting.claimed = true;
            setting.connections[Direction.EAST.get3DDataValue()].claimed = true;
            setting.connections[Direction.UP.get3DDataValue()].claimed = true;
            setting.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
            return new OceanMonumentPieces.OceanMonumentDoubleXYRoom(direction, setting);
        }
    }

    static class FitDoubleYRoom implements OceanMonumentPieces.MonumentRoomFitter {
        @Override
        public boolean fits(OceanMonumentPieces.RoomDefinition setting) {
            return setting.hasOpening[Direction.UP.get3DDataValue()] && !setting.connections[Direction.UP.get3DDataValue()].claimed;
        }

        @Override
        public OceanMonumentPieces.OceanMonumentPiece create(Direction direction, OceanMonumentPieces.RoomDefinition setting, Random random) {
            setting.claimed = true;
            setting.connections[Direction.UP.get3DDataValue()].claimed = true;
            return new OceanMonumentPieces.OceanMonumentDoubleYRoom(direction, setting);
        }
    }

    static class FitDoubleYZRoom implements OceanMonumentPieces.MonumentRoomFitter {
        @Override
        public boolean fits(OceanMonumentPieces.RoomDefinition setting) {
            if (setting.hasOpening[Direction.NORTH.get3DDataValue()] && !setting.connections[Direction.NORTH.get3DDataValue()].claimed && setting.hasOpening[Direction.UP.get3DDataValue()] && !setting.connections[Direction.UP.get3DDataValue()].claimed) {
                OceanMonumentPieces.RoomDefinition roomDefinition = setting.connections[Direction.NORTH.get3DDataValue()];
                return roomDefinition.hasOpening[Direction.UP.get3DDataValue()] && !roomDefinition.connections[Direction.UP.get3DDataValue()].claimed;
            } else {
                return false;
            }
        }

        @Override
        public OceanMonumentPieces.OceanMonumentPiece create(Direction direction, OceanMonumentPieces.RoomDefinition setting, Random random) {
            setting.claimed = true;
            setting.connections[Direction.NORTH.get3DDataValue()].claimed = true;
            setting.connections[Direction.UP.get3DDataValue()].claimed = true;
            setting.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
            return new OceanMonumentPieces.OceanMonumentDoubleYZRoom(direction, setting);
        }
    }

    static class FitDoubleZRoom implements OceanMonumentPieces.MonumentRoomFitter {
        @Override
        public boolean fits(OceanMonumentPieces.RoomDefinition setting) {
            return setting.hasOpening[Direction.NORTH.get3DDataValue()] && !setting.connections[Direction.NORTH.get3DDataValue()].claimed;
        }

        @Override
        public OceanMonumentPieces.OceanMonumentPiece create(Direction direction, OceanMonumentPieces.RoomDefinition setting, Random random) {
            OceanMonumentPieces.RoomDefinition roomDefinition = setting;
            if (!setting.hasOpening[Direction.NORTH.get3DDataValue()] || setting.connections[Direction.NORTH.get3DDataValue()].claimed) {
                roomDefinition = setting.connections[Direction.SOUTH.get3DDataValue()];
            }

            roomDefinition.claimed = true;
            roomDefinition.connections[Direction.NORTH.get3DDataValue()].claimed = true;
            return new OceanMonumentPieces.OceanMonumentDoubleZRoom(direction, roomDefinition);
        }
    }

    static class FitSimpleRoom implements OceanMonumentPieces.MonumentRoomFitter {
        @Override
        public boolean fits(OceanMonumentPieces.RoomDefinition setting) {
            return true;
        }

        @Override
        public OceanMonumentPieces.OceanMonumentPiece create(Direction direction, OceanMonumentPieces.RoomDefinition setting, Random random) {
            setting.claimed = true;
            return new OceanMonumentPieces.OceanMonumentSimpleRoom(direction, setting, random);
        }
    }

    static class FitSimpleTopRoom implements OceanMonumentPieces.MonumentRoomFitter {
        @Override
        public boolean fits(OceanMonumentPieces.RoomDefinition setting) {
            return !setting.hasOpening[Direction.WEST.get3DDataValue()] && !setting.hasOpening[Direction.EAST.get3DDataValue()] && !setting.hasOpening[Direction.NORTH.get3DDataValue()] && !setting.hasOpening[Direction.SOUTH.get3DDataValue()] && !setting.hasOpening[Direction.UP.get3DDataValue()];
        }

        @Override
        public OceanMonumentPieces.OceanMonumentPiece create(Direction direction, OceanMonumentPieces.RoomDefinition setting, Random random) {
            setting.claimed = true;
            return new OceanMonumentPieces.OceanMonumentSimpleTopRoom(direction, setting);
        }
    }

    public static class MonumentBuilding extends OceanMonumentPieces.OceanMonumentPiece {
        private static final int WIDTH = 58;
        private static final int HEIGHT = 22;
        private static final int DEPTH = 58;
        public static final int BIOME_RANGE_CHECK = 29;
        private static final int TOP_POSITION = 61;
        private OceanMonumentPieces.RoomDefinition sourceRoom;
        private OceanMonumentPieces.RoomDefinition coreRoom;
        private final List<OceanMonumentPieces.OceanMonumentPiece> childPieces = Lists.newArrayList();

        public MonumentBuilding(Random random, int x, int z, Direction orientation) {
            super(StructurePieceType.OCEAN_MONUMENT_BUILDING, orientation, 0, makeBoundingBox(x, 39, z, orientation, 58, 23, 58));
            this.setOrientation(orientation);
            List<OceanMonumentPieces.RoomDefinition> list = this.generateRoomGraph(random);
            this.sourceRoom.claimed = true;
            this.childPieces.add(new OceanMonumentPieces.OceanMonumentEntryRoom(orientation, this.sourceRoom));
            this.childPieces.add(new OceanMonumentPieces.OceanMonumentCoreRoom(orientation, this.coreRoom));
            List<OceanMonumentPieces.MonumentRoomFitter> list2 = Lists.newArrayList();
            list2.add(new OceanMonumentPieces.FitDoubleXYRoom());
            list2.add(new OceanMonumentPieces.FitDoubleYZRoom());
            list2.add(new OceanMonumentPieces.FitDoubleZRoom());
            list2.add(new OceanMonumentPieces.FitDoubleXRoom());
            list2.add(new OceanMonumentPieces.FitDoubleYRoom());
            list2.add(new OceanMonumentPieces.FitSimpleTopRoom());
            list2.add(new OceanMonumentPieces.FitSimpleRoom());

            for(OceanMonumentPieces.RoomDefinition roomDefinition : list) {
                if (!roomDefinition.claimed && !roomDefinition.isSpecial()) {
                    for(OceanMonumentPieces.MonumentRoomFitter monumentRoomFitter : list2) {
                        if (monumentRoomFitter.fits(roomDefinition)) {
                            this.childPieces.add(monumentRoomFitter.create(orientation, roomDefinition, random));
                            break;
                        }
                    }
                }
            }

            BlockPos blockPos = this.getWorldPos(9, 0, 22);

            for(OceanMonumentPieces.OceanMonumentPiece oceanMonumentPiece : this.childPieces) {
                oceanMonumentPiece.getBoundingBox().move(blockPos);
            }

            BoundingBox boundingBox = BoundingBox.fromCorners(this.getWorldPos(1, 1, 1), this.getWorldPos(23, 8, 21));
            BoundingBox boundingBox2 = BoundingBox.fromCorners(this.getWorldPos(34, 1, 1), this.getWorldPos(56, 8, 21));
            BoundingBox boundingBox3 = BoundingBox.fromCorners(this.getWorldPos(22, 13, 22), this.getWorldPos(35, 17, 35));
            int i = random.nextInt();
            this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(orientation, boundingBox, i++));
            this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(orientation, boundingBox2, i++));
            this.childPieces.add(new OceanMonumentPieces.OceanMonumentPenthouse(orientation, boundingBox3));
        }

        public MonumentBuilding(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_BUILDING, nbt);
        }

        private List<OceanMonumentPieces.RoomDefinition> generateRoomGraph(Random random) {
            OceanMonumentPieces.RoomDefinition[] roomDefinitions = new OceanMonumentPieces.RoomDefinition[75];

            for(int i = 0; i < 5; ++i) {
                for(int j = 0; j < 4; ++j) {
                    int k = 0;
                    int l = getRoomIndex(i, 0, j);
                    roomDefinitions[l] = new OceanMonumentPieces.RoomDefinition(l);
                }
            }

            for(int m = 0; m < 5; ++m) {
                for(int n = 0; n < 4; ++n) {
                    int o = 1;
                    int p = getRoomIndex(m, 1, n);
                    roomDefinitions[p] = new OceanMonumentPieces.RoomDefinition(p);
                }
            }

            for(int q = 1; q < 4; ++q) {
                for(int r = 0; r < 2; ++r) {
                    int s = 2;
                    int t = getRoomIndex(q, 2, r);
                    roomDefinitions[t] = new OceanMonumentPieces.RoomDefinition(t);
                }
            }

            this.sourceRoom = roomDefinitions[GRIDROOM_SOURCE_INDEX];

            for(int u = 0; u < 5; ++u) {
                for(int v = 0; v < 5; ++v) {
                    for(int w = 0; w < 3; ++w) {
                        int x = getRoomIndex(u, w, v);
                        if (roomDefinitions[x] != null) {
                            for(Direction direction : Direction.values()) {
                                int y = u + direction.getStepX();
                                int z = w + direction.getStepY();
                                int aa = v + direction.getStepZ();
                                if (y >= 0 && y < 5 && aa >= 0 && aa < 5 && z >= 0 && z < 3) {
                                    int ab = getRoomIndex(y, z, aa);
                                    if (roomDefinitions[ab] != null) {
                                        if (aa == v) {
                                            roomDefinitions[x].setConnection(direction, roomDefinitions[ab]);
                                        } else {
                                            roomDefinitions[x].setConnection(direction.getOpposite(), roomDefinitions[ab]);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OceanMonumentPieces.RoomDefinition roomDefinition = new OceanMonumentPieces.RoomDefinition(1003);
            OceanMonumentPieces.RoomDefinition roomDefinition2 = new OceanMonumentPieces.RoomDefinition(1001);
            OceanMonumentPieces.RoomDefinition roomDefinition3 = new OceanMonumentPieces.RoomDefinition(1002);
            roomDefinitions[GRIDROOM_TOP_CONNECT_INDEX].setConnection(Direction.UP, roomDefinition);
            roomDefinitions[GRIDROOM_LEFTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, roomDefinition2);
            roomDefinitions[GRIDROOM_RIGHTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, roomDefinition3);
            roomDefinition.claimed = true;
            roomDefinition2.claimed = true;
            roomDefinition3.claimed = true;
            this.sourceRoom.isSource = true;
            this.coreRoom = roomDefinitions[getRoomIndex(random.nextInt(4), 0, 2)];
            this.coreRoom.claimed = true;
            this.coreRoom.connections[Direction.EAST.get3DDataValue()].claimed = true;
            this.coreRoom.connections[Direction.NORTH.get3DDataValue()].claimed = true;
            this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].claimed = true;
            this.coreRoom.connections[Direction.UP.get3DDataValue()].claimed = true;
            this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
            this.coreRoom.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
            this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
            List<OceanMonumentPieces.RoomDefinition> list = Lists.newArrayList();

            for(OceanMonumentPieces.RoomDefinition roomDefinition4 : roomDefinitions) {
                if (roomDefinition4 != null) {
                    roomDefinition4.updateOpenings();
                    list.add(roomDefinition4);
                }
            }

            roomDefinition.updateOpenings();
            Collections.shuffle(list, random);
            int ac = 1;

            for(OceanMonumentPieces.RoomDefinition roomDefinition5 : list) {
                int ad = 0;
                int ae = 0;

                while(ad < 2 && ae < 5) {
                    ++ae;
                    int af = random.nextInt(6);
                    if (roomDefinition5.hasOpening[af]) {
                        int ag = Direction.from3DDataValue(af).getOpposite().get3DDataValue();
                        roomDefinition5.hasOpening[af] = false;
                        roomDefinition5.connections[af].hasOpening[ag] = false;
                        if (roomDefinition5.findSource(ac++) && roomDefinition5.connections[af].findSource(ac++)) {
                            ++ad;
                        } else {
                            roomDefinition5.hasOpening[af] = true;
                            roomDefinition5.connections[af].hasOpening[ag] = true;
                        }
                    }
                }
            }

            list.add(roomDefinition);
            list.add(roomDefinition2);
            list.add(roomDefinition3);
            return list;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            int i = Math.max(world.getSeaLevel(), 64) - this.boundingBox.minY();
            this.generateWaterBox(world, chunkBox, 0, 0, 0, 58, i, 58);
            this.generateWing(false, 0, world, random, chunkBox);
            this.generateWing(true, 33, world, random, chunkBox);
            this.generateEntranceArchs(world, random, chunkBox);
            this.generateEntranceWall(world, random, chunkBox);
            this.generateRoofPiece(world, random, chunkBox);
            this.generateLowerWall(world, random, chunkBox);
            this.generateMiddleWall(world, random, chunkBox);
            this.generateUpperWall(world, random, chunkBox);

            for(int j = 0; j < 7; ++j) {
                int k = 0;

                while(k < 7) {
                    if (k == 0 && j == 3) {
                        k = 6;
                    }

                    int l = j * 9;
                    int m = k * 9;

                    for(int n = 0; n < 4; ++n) {
                        for(int o = 0; o < 4; ++o) {
                            this.placeBlock(world, BASE_LIGHT, l + n, 0, m + o, chunkBox);
                            this.fillColumnDown(world, BASE_LIGHT, l + n, -1, m + o, chunkBox);
                        }
                    }

                    if (j != 0 && j != 6) {
                        k += 6;
                    } else {
                        ++k;
                    }
                }
            }

            for(int p = 0; p < 5; ++p) {
                this.generateWaterBox(world, chunkBox, -1 - p, 0 + p * 2, -1 - p, -1 - p, 23, 58 + p);
                this.generateWaterBox(world, chunkBox, 58 + p, 0 + p * 2, -1 - p, 58 + p, 23, 58 + p);
                this.generateWaterBox(world, chunkBox, 0 - p, 0 + p * 2, -1 - p, 57 + p, 23, -1 - p);
                this.generateWaterBox(world, chunkBox, 0 - p, 0 + p * 2, 58 + p, 57 + p, 23, 58 + p);
            }

            for(OceanMonumentPieces.OceanMonumentPiece oceanMonumentPiece : this.childPieces) {
                if (oceanMonumentPiece.getBoundingBox().intersects(chunkBox)) {
                    oceanMonumentPiece.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pos);
                }
            }

        }

        private void generateWing(boolean bl, int i, WorldGenLevel world, Random random, BoundingBox box) {
            int j = 24;
            if (this.chunkIntersects(box, i, 0, i + 23, 20)) {
                this.generateBox(world, box, i + 0, 0, 0, i + 24, 0, 20, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, i + 0, 1, 0, i + 24, 10, 20);

                for(int k = 0; k < 4; ++k) {
                    this.generateBox(world, box, i + k, k + 1, k, i + k, k + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, i + k + 7, k + 5, k + 7, i + k + 7, k + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, i + 17 - k, k + 5, k + 7, i + 17 - k, k + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, i + 24 - k, k + 1, k, i + 24 - k, k + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, i + k + 1, k + 1, k, i + 23 - k, k + 1, k, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, i + k + 8, k + 5, k + 7, i + 16 - k, k + 5, k + 7, BASE_LIGHT, BASE_LIGHT, false);
                }

                this.generateBox(world, box, i + 4, 4, 4, i + 6, 4, 20, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, i + 7, 4, 4, i + 17, 4, 6, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, i + 18, 4, 4, i + 20, 4, 20, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, i + 11, 8, 11, i + 13, 8, 20, BASE_GRAY, BASE_GRAY, false);
                this.placeBlock(world, DOT_DECO_DATA, i + 12, 9, 12, box);
                this.placeBlock(world, DOT_DECO_DATA, i + 12, 9, 15, box);
                this.placeBlock(world, DOT_DECO_DATA, i + 12, 9, 18, box);
                int l = i + (bl ? 19 : 5);
                int m = i + (bl ? 5 : 19);

                for(int n = 20; n >= 5; n -= 3) {
                    this.placeBlock(world, DOT_DECO_DATA, l, 5, n, box);
                }

                for(int o = 19; o >= 7; o -= 3) {
                    this.placeBlock(world, DOT_DECO_DATA, m, 5, o, box);
                }

                for(int p = 0; p < 4; ++p) {
                    int q = bl ? i + 24 - (17 - p * 3) : i + 17 - p * 3;
                    this.placeBlock(world, DOT_DECO_DATA, q, 5, 5, box);
                }

                this.placeBlock(world, DOT_DECO_DATA, m, 5, 5, box);
                this.generateBox(world, box, i + 11, 1, 12, i + 13, 7, 12, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, i + 12, 1, 11, i + 12, 7, 13, BASE_GRAY, BASE_GRAY, false);
            }

        }

        private void generateEntranceArchs(WorldGenLevel world, Random random, BoundingBox box) {
            if (this.chunkIntersects(box, 22, 5, 35, 17)) {
                this.generateWaterBox(world, box, 25, 0, 0, 32, 8, 20);

                for(int i = 0; i < 4; ++i) {
                    this.generateBox(world, box, 24, 2, 5 + i * 4, 24, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, 22, 4, 5 + i * 4, 23, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
                    this.placeBlock(world, BASE_LIGHT, 25, 5, 5 + i * 4, box);
                    this.placeBlock(world, BASE_LIGHT, 26, 6, 5 + i * 4, box);
                    this.placeBlock(world, LAMP_BLOCK, 26, 5, 5 + i * 4, box);
                    this.generateBox(world, box, 33, 2, 5 + i * 4, 33, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, 34, 4, 5 + i * 4, 35, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
                    this.placeBlock(world, BASE_LIGHT, 32, 5, 5 + i * 4, box);
                    this.placeBlock(world, BASE_LIGHT, 31, 6, 5 + i * 4, box);
                    this.placeBlock(world, LAMP_BLOCK, 31, 5, 5 + i * 4, box);
                    this.generateBox(world, box, 27, 6, 5 + i * 4, 30, 6, 5 + i * 4, BASE_GRAY, BASE_GRAY, false);
                }
            }

        }

        private void generateEntranceWall(WorldGenLevel world, Random random, BoundingBox box) {
            if (this.chunkIntersects(box, 15, 20, 42, 21)) {
                this.generateBox(world, box, 15, 0, 21, 42, 0, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 26, 1, 21, 31, 3, 21);
                this.generateBox(world, box, 21, 12, 21, 36, 12, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 17, 11, 21, 40, 11, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 16, 10, 21, 41, 10, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 15, 7, 21, 42, 9, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 16, 6, 21, 41, 6, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 17, 5, 21, 40, 5, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 21, 4, 21, 36, 4, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 22, 3, 21, 26, 3, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 31, 3, 21, 35, 3, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 23, 2, 21, 25, 2, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 32, 2, 21, 34, 2, 21, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 28, 4, 20, 29, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
                this.placeBlock(world, BASE_LIGHT, 27, 3, 21, box);
                this.placeBlock(world, BASE_LIGHT, 30, 3, 21, box);
                this.placeBlock(world, BASE_LIGHT, 26, 2, 21, box);
                this.placeBlock(world, BASE_LIGHT, 31, 2, 21, box);
                this.placeBlock(world, BASE_LIGHT, 25, 1, 21, box);
                this.placeBlock(world, BASE_LIGHT, 32, 1, 21, box);

                for(int i = 0; i < 7; ++i) {
                    this.placeBlock(world, BASE_BLACK, 28 - i, 6 + i, 21, box);
                    this.placeBlock(world, BASE_BLACK, 29 + i, 6 + i, 21, box);
                }

                for(int j = 0; j < 4; ++j) {
                    this.placeBlock(world, BASE_BLACK, 28 - j, 9 + j, 21, box);
                    this.placeBlock(world, BASE_BLACK, 29 + j, 9 + j, 21, box);
                }

                this.placeBlock(world, BASE_BLACK, 28, 12, 21, box);
                this.placeBlock(world, BASE_BLACK, 29, 12, 21, box);

                for(int k = 0; k < 3; ++k) {
                    this.placeBlock(world, BASE_BLACK, 22 - k * 2, 8, 21, box);
                    this.placeBlock(world, BASE_BLACK, 22 - k * 2, 9, 21, box);
                    this.placeBlock(world, BASE_BLACK, 35 + k * 2, 8, 21, box);
                    this.placeBlock(world, BASE_BLACK, 35 + k * 2, 9, 21, box);
                }

                this.generateWaterBox(world, box, 15, 13, 21, 42, 15, 21);
                this.generateWaterBox(world, box, 15, 1, 21, 15, 6, 21);
                this.generateWaterBox(world, box, 16, 1, 21, 16, 5, 21);
                this.generateWaterBox(world, box, 17, 1, 21, 20, 4, 21);
                this.generateWaterBox(world, box, 21, 1, 21, 21, 3, 21);
                this.generateWaterBox(world, box, 22, 1, 21, 22, 2, 21);
                this.generateWaterBox(world, box, 23, 1, 21, 24, 1, 21);
                this.generateWaterBox(world, box, 42, 1, 21, 42, 6, 21);
                this.generateWaterBox(world, box, 41, 1, 21, 41, 5, 21);
                this.generateWaterBox(world, box, 37, 1, 21, 40, 4, 21);
                this.generateWaterBox(world, box, 36, 1, 21, 36, 3, 21);
                this.generateWaterBox(world, box, 33, 1, 21, 34, 1, 21);
                this.generateWaterBox(world, box, 35, 1, 21, 35, 2, 21);
            }

        }

        private void generateRoofPiece(WorldGenLevel world, Random random, BoundingBox box) {
            if (this.chunkIntersects(box, 21, 21, 36, 36)) {
                this.generateBox(world, box, 21, 0, 22, 36, 0, 36, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 21, 1, 22, 36, 23, 36);

                for(int i = 0; i < 4; ++i) {
                    this.generateBox(world, box, 21 + i, 13 + i, 21 + i, 36 - i, 13 + i, 21 + i, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, 21 + i, 13 + i, 36 - i, 36 - i, 13 + i, 36 - i, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, 21 + i, 13 + i, 22 + i, 21 + i, 13 + i, 35 - i, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, box, 36 - i, 13 + i, 22 + i, 36 - i, 13 + i, 35 - i, BASE_LIGHT, BASE_LIGHT, false);
                }

                this.generateBox(world, box, 25, 16, 25, 32, 16, 32, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 25, 17, 25, 25, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, box, 32, 17, 25, 32, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, box, 25, 17, 32, 25, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, box, 32, 17, 32, 32, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
                this.placeBlock(world, BASE_LIGHT, 26, 20, 26, box);
                this.placeBlock(world, BASE_LIGHT, 27, 21, 27, box);
                this.placeBlock(world, LAMP_BLOCK, 27, 20, 27, box);
                this.placeBlock(world, BASE_LIGHT, 26, 20, 31, box);
                this.placeBlock(world, BASE_LIGHT, 27, 21, 30, box);
                this.placeBlock(world, LAMP_BLOCK, 27, 20, 30, box);
                this.placeBlock(world, BASE_LIGHT, 31, 20, 31, box);
                this.placeBlock(world, BASE_LIGHT, 30, 21, 30, box);
                this.placeBlock(world, LAMP_BLOCK, 30, 20, 30, box);
                this.placeBlock(world, BASE_LIGHT, 31, 20, 26, box);
                this.placeBlock(world, BASE_LIGHT, 30, 21, 27, box);
                this.placeBlock(world, LAMP_BLOCK, 30, 20, 27, box);
                this.generateBox(world, box, 28, 21, 27, 29, 21, 27, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 27, 21, 28, 27, 21, 29, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 28, 21, 30, 29, 21, 30, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 30, 21, 28, 30, 21, 29, BASE_GRAY, BASE_GRAY, false);
            }

        }

        private void generateLowerWall(WorldGenLevel world, Random random, BoundingBox box) {
            if (this.chunkIntersects(box, 0, 21, 6, 58)) {
                this.generateBox(world, box, 0, 0, 21, 6, 0, 57, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 0, 1, 21, 6, 7, 57);
                this.generateBox(world, box, 4, 4, 21, 6, 4, 53, BASE_GRAY, BASE_GRAY, false);

                for(int i = 0; i < 4; ++i) {
                    this.generateBox(world, box, i, i + 1, 21, i, i + 1, 57 - i, BASE_LIGHT, BASE_LIGHT, false);
                }

                for(int j = 23; j < 53; j += 3) {
                    this.placeBlock(world, DOT_DECO_DATA, 5, 5, j, box);
                }

                this.placeBlock(world, DOT_DECO_DATA, 5, 5, 52, box);

                for(int k = 0; k < 4; ++k) {
                    this.generateBox(world, box, k, k + 1, 21, k, k + 1, 57 - k, BASE_LIGHT, BASE_LIGHT, false);
                }

                this.generateBox(world, box, 4, 1, 52, 6, 3, 52, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 5, 1, 51, 5, 3, 53, BASE_GRAY, BASE_GRAY, false);
            }

            if (this.chunkIntersects(box, 51, 21, 58, 58)) {
                this.generateBox(world, box, 51, 0, 21, 57, 0, 57, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 51, 1, 21, 57, 7, 57);
                this.generateBox(world, box, 51, 4, 21, 53, 4, 53, BASE_GRAY, BASE_GRAY, false);

                for(int l = 0; l < 4; ++l) {
                    this.generateBox(world, box, 57 - l, l + 1, 21, 57 - l, l + 1, 57 - l, BASE_LIGHT, BASE_LIGHT, false);
                }

                for(int m = 23; m < 53; m += 3) {
                    this.placeBlock(world, DOT_DECO_DATA, 52, 5, m, box);
                }

                this.placeBlock(world, DOT_DECO_DATA, 52, 5, 52, box);
                this.generateBox(world, box, 51, 1, 52, 53, 3, 52, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 52, 1, 51, 52, 3, 53, BASE_GRAY, BASE_GRAY, false);
            }

            if (this.chunkIntersects(box, 0, 51, 57, 57)) {
                this.generateBox(world, box, 7, 0, 51, 50, 0, 57, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 7, 1, 51, 50, 10, 57);

                for(int n = 0; n < 4; ++n) {
                    this.generateBox(world, box, n + 1, n + 1, 57 - n, 56 - n, n + 1, 57 - n, BASE_LIGHT, BASE_LIGHT, false);
                }
            }

        }

        private void generateMiddleWall(WorldGenLevel world, Random random, BoundingBox box) {
            if (this.chunkIntersects(box, 7, 21, 13, 50)) {
                this.generateBox(world, box, 7, 0, 21, 13, 0, 50, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 7, 1, 21, 13, 10, 50);
                this.generateBox(world, box, 11, 8, 21, 13, 8, 53, BASE_GRAY, BASE_GRAY, false);

                for(int i = 0; i < 4; ++i) {
                    this.generateBox(world, box, i + 7, i + 5, 21, i + 7, i + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
                }

                for(int j = 21; j <= 45; j += 3) {
                    this.placeBlock(world, DOT_DECO_DATA, 12, 9, j, box);
                }
            }

            if (this.chunkIntersects(box, 44, 21, 50, 54)) {
                this.generateBox(world, box, 44, 0, 21, 50, 0, 50, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 44, 1, 21, 50, 10, 50);
                this.generateBox(world, box, 44, 8, 21, 46, 8, 53, BASE_GRAY, BASE_GRAY, false);

                for(int k = 0; k < 4; ++k) {
                    this.generateBox(world, box, 50 - k, k + 5, 21, 50 - k, k + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
                }

                for(int l = 21; l <= 45; l += 3) {
                    this.placeBlock(world, DOT_DECO_DATA, 45, 9, l, box);
                }
            }

            if (this.chunkIntersects(box, 8, 44, 49, 54)) {
                this.generateBox(world, box, 14, 0, 44, 43, 0, 50, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 14, 1, 44, 43, 10, 50);

                for(int m = 12; m <= 45; m += 3) {
                    this.placeBlock(world, DOT_DECO_DATA, m, 9, 45, box);
                    this.placeBlock(world, DOT_DECO_DATA, m, 9, 52, box);
                    if (m == 12 || m == 18 || m == 24 || m == 33 || m == 39 || m == 45) {
                        this.placeBlock(world, DOT_DECO_DATA, m, 9, 47, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 9, 50, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 10, 45, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 10, 46, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 10, 51, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 10, 52, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 11, 47, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 11, 50, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 12, 48, box);
                        this.placeBlock(world, DOT_DECO_DATA, m, 12, 49, box);
                    }
                }

                for(int n = 0; n < 3; ++n) {
                    this.generateBox(world, box, 8 + n, 5 + n, 54, 49 - n, 5 + n, 54, BASE_GRAY, BASE_GRAY, false);
                }

                this.generateBox(world, box, 11, 8, 54, 46, 8, 54, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, box, 14, 8, 44, 43, 8, 53, BASE_GRAY, BASE_GRAY, false);
            }

        }

        private void generateUpperWall(WorldGenLevel world, Random random, BoundingBox box) {
            if (this.chunkIntersects(box, 14, 21, 20, 43)) {
                this.generateBox(world, box, 14, 0, 21, 20, 0, 43, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 14, 1, 22, 20, 14, 43);
                this.generateBox(world, box, 18, 12, 22, 20, 12, 39, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 18, 12, 21, 20, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

                for(int i = 0; i < 4; ++i) {
                    this.generateBox(world, box, i + 14, i + 9, 21, i + 14, i + 9, 43 - i, BASE_LIGHT, BASE_LIGHT, false);
                }

                for(int j = 23; j <= 39; j += 3) {
                    this.placeBlock(world, DOT_DECO_DATA, 19, 13, j, box);
                }
            }

            if (this.chunkIntersects(box, 37, 21, 43, 43)) {
                this.generateBox(world, box, 37, 0, 21, 43, 0, 43, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 37, 1, 22, 43, 14, 43);
                this.generateBox(world, box, 37, 12, 22, 39, 12, 39, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, 37, 12, 21, 39, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

                for(int k = 0; k < 4; ++k) {
                    this.generateBox(world, box, 43 - k, k + 9, 21, 43 - k, k + 9, 43 - k, BASE_LIGHT, BASE_LIGHT, false);
                }

                for(int l = 23; l <= 39; l += 3) {
                    this.placeBlock(world, DOT_DECO_DATA, 38, 13, l, box);
                }
            }

            if (this.chunkIntersects(box, 15, 37, 42, 43)) {
                this.generateBox(world, box, 21, 0, 37, 36, 0, 43, BASE_GRAY, BASE_GRAY, false);
                this.generateWaterBox(world, box, 21, 1, 37, 36, 14, 43);
                this.generateBox(world, box, 21, 12, 37, 36, 12, 39, BASE_GRAY, BASE_GRAY, false);

                for(int m = 0; m < 4; ++m) {
                    this.generateBox(world, box, 15 + m, m + 9, 43 - m, 42 - m, m + 9, 43 - m, BASE_LIGHT, BASE_LIGHT, false);
                }

                for(int n = 21; n <= 36; n += 3) {
                    this.placeBlock(world, DOT_DECO_DATA, n, 13, 38, box);
                }
            }

        }
    }

    interface MonumentRoomFitter {
        boolean fits(OceanMonumentPieces.RoomDefinition setting);

        OceanMonumentPieces.OceanMonumentPiece create(Direction direction, OceanMonumentPieces.RoomDefinition setting, Random random);
    }

    public static class OceanMonumentCoreRoom extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentCoreRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, 1, orientation, setting, 2, 2, 2);
        }

        public OceanMonumentCoreRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBoxOnFillOnly(world, chunkBox, 1, 8, 0, 14, 8, 14, BASE_GRAY);
            int i = 7;
            BlockState blockState = BASE_LIGHT;
            this.generateBox(world, chunkBox, 0, 7, 0, 0, 7, 15, blockState, blockState, false);
            this.generateBox(world, chunkBox, 15, 7, 0, 15, 7, 15, blockState, blockState, false);
            this.generateBox(world, chunkBox, 1, 7, 0, 15, 7, 0, blockState, blockState, false);
            this.generateBox(world, chunkBox, 1, 7, 15, 14, 7, 15, blockState, blockState, false);

            for(int j = 1; j <= 6; ++j) {
                blockState = BASE_LIGHT;
                if (j == 2 || j == 6) {
                    blockState = BASE_GRAY;
                }

                for(int k = 0; k <= 15; k += 15) {
                    this.generateBox(world, chunkBox, k, j, 0, k, j, 1, blockState, blockState, false);
                    this.generateBox(world, chunkBox, k, j, 6, k, j, 9, blockState, blockState, false);
                    this.generateBox(world, chunkBox, k, j, 14, k, j, 15, blockState, blockState, false);
                }

                this.generateBox(world, chunkBox, 1, j, 0, 1, j, 0, blockState, blockState, false);
                this.generateBox(world, chunkBox, 6, j, 0, 9, j, 0, blockState, blockState, false);
                this.generateBox(world, chunkBox, 14, j, 0, 14, j, 0, blockState, blockState, false);
                this.generateBox(world, chunkBox, 1, j, 15, 14, j, 15, blockState, blockState, false);
            }

            this.generateBox(world, chunkBox, 6, 3, 6, 9, 6, 9, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 7, 4, 7, 8, 5, 8, Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(), false);

            for(int l = 3; l <= 6; l += 3) {
                for(int m = 6; m <= 9; m += 3) {
                    this.placeBlock(world, LAMP_BLOCK, m, l, 6, chunkBox);
                    this.placeBlock(world, LAMP_BLOCK, m, l, 9, chunkBox);
                }
            }

            this.generateBox(world, chunkBox, 5, 1, 6, 5, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 1, 9, 5, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 1, 6, 10, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 1, 9, 10, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 1, 5, 6, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 9, 1, 5, 9, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 1, 10, 6, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 9, 1, 10, 9, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 2, 5, 5, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 2, 10, 5, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 2, 5, 10, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 2, 10, 10, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 7, 1, 5, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 7, 1, 10, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 7, 9, 5, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 7, 9, 10, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 7, 5, 6, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 7, 10, 6, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 9, 7, 5, 14, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 9, 7, 10, 14, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 2, 1, 2, 2, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 3, 1, 2, 3, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 13, 1, 2, 13, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 12, 1, 2, 12, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 2, 1, 12, 2, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 3, 1, 13, 3, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 13, 1, 12, 13, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 12, 1, 13, 12, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
        }
    }

    public static class OceanMonumentDoubleXRoom extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentDoubleXRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, 1, orientation, setting, 2, 1, 1);
        }

        public OceanMonumentDoubleXRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            OceanMonumentPieces.RoomDefinition roomDefinition = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
            OceanMonumentPieces.RoomDefinition roomDefinition2 = this.roomDefinition;
            if (this.roomDefinition.index / 25 > 0) {
                this.generateDefaultFloor(world, chunkBox, 8, 0, roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
                this.generateDefaultFloor(world, chunkBox, 0, 0, roomDefinition2.hasOpening[Direction.DOWN.get3DDataValue()]);
            }

            if (roomDefinition2.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 4, 1, 7, 4, 6, BASE_GRAY);
            }

            if (roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 8, 4, 1, 14, 4, 6, BASE_GRAY);
            }

            this.generateBox(world, chunkBox, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 15, 3, 0, 15, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 3, 0, 15, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 3, 7, 14, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 0, 2, 0, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 15, 2, 0, 15, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 1, 2, 0, 15, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 1, 2, 7, 14, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 15, 1, 0, 15, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 0, 15, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 1, 0, 10, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 2, 0, 9, 2, 3, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 5, 3, 0, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(world, LAMP_BLOCK, 6, 2, 3, chunkBox);
            this.placeBlock(world, LAMP_BLOCK, 9, 2, 3, chunkBox);
            if (roomDefinition2.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 0, 4, 2, 0);
            }

            if (roomDefinition2.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 7, 4, 2, 7);
            }

            if (roomDefinition2.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 1, 3, 0, 2, 4);
            }

            if (roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 11, 1, 0, 12, 2, 0);
            }

            if (roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 11, 1, 7, 12, 2, 7);
            }

            if (roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 15, 1, 3, 15, 2, 4);
            }

        }
    }

    public static class OceanMonumentDoubleXYRoom extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentDoubleXYRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, 1, orientation, setting, 2, 2, 1);
        }

        public OceanMonumentDoubleXYRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            OceanMonumentPieces.RoomDefinition roomDefinition = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
            OceanMonumentPieces.RoomDefinition roomDefinition2 = this.roomDefinition;
            OceanMonumentPieces.RoomDefinition roomDefinition3 = roomDefinition2.connections[Direction.UP.get3DDataValue()];
            OceanMonumentPieces.RoomDefinition roomDefinition4 = roomDefinition.connections[Direction.UP.get3DDataValue()];
            if (this.roomDefinition.index / 25 > 0) {
                this.generateDefaultFloor(world, chunkBox, 8, 0, roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
                this.generateDefaultFloor(world, chunkBox, 0, 0, roomDefinition2.hasOpening[Direction.DOWN.get3DDataValue()]);
            }

            if (roomDefinition3.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 8, 1, 7, 8, 6, BASE_GRAY);
            }

            if (roomDefinition4.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 8, 8, 1, 14, 8, 6, BASE_GRAY);
            }

            for(int i = 1; i <= 7; ++i) {
                BlockState blockState = BASE_LIGHT;
                if (i == 2 || i == 6) {
                    blockState = BASE_GRAY;
                }

                this.generateBox(world, chunkBox, 0, i, 0, 0, i, 7, blockState, blockState, false);
                this.generateBox(world, chunkBox, 15, i, 0, 15, i, 7, blockState, blockState, false);
                this.generateBox(world, chunkBox, 1, i, 0, 15, i, 0, blockState, blockState, false);
                this.generateBox(world, chunkBox, 1, i, 7, 14, i, 7, blockState, blockState, false);
            }

            this.generateBox(world, chunkBox, 2, 1, 3, 2, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 3, 1, 2, 4, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 3, 1, 5, 4, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 13, 1, 3, 13, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 11, 1, 2, 12, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 11, 1, 5, 12, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 1, 3, 5, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 1, 3, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 7, 2, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 5, 2, 5, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 5, 2, 10, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 5, 5, 5, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 10, 5, 5, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(world, BASE_LIGHT, 6, 6, 2, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 9, 6, 2, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 6, 6, 5, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 9, 6, 5, chunkBox);
            this.generateBox(world, chunkBox, 5, 4, 3, 6, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 9, 4, 3, 10, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(world, LAMP_BLOCK, 5, 4, 2, chunkBox);
            this.placeBlock(world, LAMP_BLOCK, 5, 4, 5, chunkBox);
            this.placeBlock(world, LAMP_BLOCK, 10, 4, 2, chunkBox);
            this.placeBlock(world, LAMP_BLOCK, 10, 4, 5, chunkBox);
            if (roomDefinition2.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 0, 4, 2, 0);
            }

            if (roomDefinition2.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 7, 4, 2, 7);
            }

            if (roomDefinition2.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 1, 3, 0, 2, 4);
            }

            if (roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 11, 1, 0, 12, 2, 0);
            }

            if (roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 11, 1, 7, 12, 2, 7);
            }

            if (roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 15, 1, 3, 15, 2, 4);
            }

            if (roomDefinition3.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 5, 0, 4, 6, 0);
            }

            if (roomDefinition3.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 5, 7, 4, 6, 7);
            }

            if (roomDefinition3.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 5, 3, 0, 6, 4);
            }

            if (roomDefinition4.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 11, 5, 0, 12, 6, 0);
            }

            if (roomDefinition4.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 11, 5, 7, 12, 6, 7);
            }

            if (roomDefinition4.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 15, 5, 3, 15, 6, 4);
            }

        }
    }

    public static class OceanMonumentDoubleYRoom extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentDoubleYRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, 1, orientation, setting, 1, 2, 1);
        }

        public OceanMonumentDoubleYRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            if (this.roomDefinition.index / 25 > 0) {
                this.generateDefaultFloor(world, chunkBox, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
            }

            OceanMonumentPieces.RoomDefinition roomDefinition = this.roomDefinition.connections[Direction.UP.get3DDataValue()];
            if (roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 8, 1, 6, 8, 6, BASE_GRAY);
            }

            this.generateBox(world, chunkBox, 0, 4, 0, 0, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 7, 4, 0, 7, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 4, 0, 6, 4, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 4, 7, 6, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 2, 4, 1, 2, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 4, 2, 1, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 4, 1, 5, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 4, 2, 6, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 2, 4, 5, 2, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 4, 5, 1, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 4, 5, 5, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 4, 5, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            OceanMonumentPieces.RoomDefinition roomDefinition2 = this.roomDefinition;

            for(int i = 1; i <= 5; i += 4) {
                int j = 0;
                if (roomDefinition2.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 2, i, j, 2, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 5, i, j, 5, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 3, i + 2, j, 4, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
                } else {
                    this.generateBox(world, chunkBox, 0, i, j, 7, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 0, i + 1, j, 7, i + 1, j, BASE_GRAY, BASE_GRAY, false);
                }

                j = 7;
                if (roomDefinition2.hasOpening[Direction.NORTH.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 2, i, j, 2, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 5, i, j, 5, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 3, i + 2, j, 4, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
                } else {
                    this.generateBox(world, chunkBox, 0, i, j, 7, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 0, i + 1, j, 7, i + 1, j, BASE_GRAY, BASE_GRAY, false);
                }

                int k = 0;
                if (roomDefinition2.hasOpening[Direction.WEST.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, k, i, 2, k, i + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, k, i, 5, k, i + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, k, i + 2, 3, k, i + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
                } else {
                    this.generateBox(world, chunkBox, k, i, 0, k, i + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, k, i + 1, 0, k, i + 1, 7, BASE_GRAY, BASE_GRAY, false);
                }

                k = 7;
                if (roomDefinition2.hasOpening[Direction.EAST.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, k, i, 2, k, i + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, k, i, 5, k, i + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, k, i + 2, 3, k, i + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
                } else {
                    this.generateBox(world, chunkBox, k, i, 0, k, i + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, k, i + 1, 0, k, i + 1, 7, BASE_GRAY, BASE_GRAY, false);
                }

                roomDefinition2 = roomDefinition;
            }

        }
    }

    public static class OceanMonumentDoubleYZRoom extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentDoubleYZRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, 1, orientation, setting, 1, 2, 2);
        }

        public OceanMonumentDoubleYZRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            OceanMonumentPieces.RoomDefinition roomDefinition = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
            OceanMonumentPieces.RoomDefinition roomDefinition2 = this.roomDefinition;
            OceanMonumentPieces.RoomDefinition roomDefinition3 = roomDefinition.connections[Direction.UP.get3DDataValue()];
            OceanMonumentPieces.RoomDefinition roomDefinition4 = roomDefinition2.connections[Direction.UP.get3DDataValue()];
            if (this.roomDefinition.index / 25 > 0) {
                this.generateDefaultFloor(world, chunkBox, 0, 8, roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
                this.generateDefaultFloor(world, chunkBox, 0, 0, roomDefinition2.hasOpening[Direction.DOWN.get3DDataValue()]);
            }

            if (roomDefinition4.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 8, 1, 6, 8, 7, BASE_GRAY);
            }

            if (roomDefinition3.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 8, 8, 6, 8, 14, BASE_GRAY);
            }

            for(int i = 1; i <= 7; ++i) {
                BlockState blockState = BASE_LIGHT;
                if (i == 2 || i == 6) {
                    blockState = BASE_GRAY;
                }

                this.generateBox(world, chunkBox, 0, i, 0, 0, i, 15, blockState, blockState, false);
                this.generateBox(world, chunkBox, 7, i, 0, 7, i, 15, blockState, blockState, false);
                this.generateBox(world, chunkBox, 1, i, 0, 6, i, 0, blockState, blockState, false);
                this.generateBox(world, chunkBox, 1, i, 15, 6, i, 15, blockState, blockState, false);
            }

            for(int j = 1; j <= 7; ++j) {
                BlockState blockState2 = BASE_BLACK;
                if (j == 2 || j == 6) {
                    blockState2 = LAMP_BLOCK;
                }

                this.generateBox(world, chunkBox, 3, j, 7, 4, j, 8, blockState2, blockState2, false);
            }

            if (roomDefinition2.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 0, 4, 2, 0);
            }

            if (roomDefinition2.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 7, 1, 3, 7, 2, 4);
            }

            if (roomDefinition2.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 1, 3, 0, 2, 4);
            }

            if (roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 15, 4, 2, 15);
            }

            if (roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 1, 11, 0, 2, 12);
            }

            if (roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 7, 1, 11, 7, 2, 12);
            }

            if (roomDefinition4.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 5, 0, 4, 6, 0);
            }

            if (roomDefinition4.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 7, 5, 3, 7, 6, 4);
                this.generateBox(world, chunkBox, 5, 4, 2, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 6, 1, 2, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 6, 1, 5, 6, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (roomDefinition4.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 5, 3, 0, 6, 4);
                this.generateBox(world, chunkBox, 1, 4, 2, 2, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 1, 1, 2, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 1, 1, 5, 1, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (roomDefinition3.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 5, 15, 4, 6, 15);
            }

            if (roomDefinition3.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 5, 11, 0, 6, 12);
                this.generateBox(world, chunkBox, 1, 4, 10, 2, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 1, 1, 10, 1, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 1, 1, 13, 1, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (roomDefinition3.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 7, 5, 11, 7, 6, 12);
                this.generateBox(world, chunkBox, 5, 4, 10, 6, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 6, 1, 10, 6, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 6, 1, 13, 6, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
            }

        }
    }

    public static class OceanMonumentDoubleZRoom extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentDoubleZRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, 1, orientation, setting, 1, 1, 2);
        }

        public OceanMonumentDoubleZRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            OceanMonumentPieces.RoomDefinition roomDefinition = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
            OceanMonumentPieces.RoomDefinition roomDefinition2 = this.roomDefinition;
            if (this.roomDefinition.index / 25 > 0) {
                this.generateDefaultFloor(world, chunkBox, 0, 8, roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
                this.generateDefaultFloor(world, chunkBox, 0, 0, roomDefinition2.hasOpening[Direction.DOWN.get3DDataValue()]);
            }

            if (roomDefinition2.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 4, 1, 6, 4, 7, BASE_GRAY);
            }

            if (roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 4, 8, 6, 4, 14, BASE_GRAY);
            }

            this.generateBox(world, chunkBox, 0, 3, 0, 0, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 7, 3, 0, 7, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 3, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 3, 15, 6, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 0, 2, 0, 0, 2, 15, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 7, 2, 0, 7, 2, 15, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 1, 2, 0, 7, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 1, 2, 15, 6, 2, 15, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 0, 1, 0, 0, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 7, 1, 0, 7, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 0, 7, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 15, 6, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 1, 1, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 1, 1, 6, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 3, 1, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 3, 1, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 13, 1, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 1, 13, 6, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 3, 13, 1, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 3, 13, 6, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 2, 1, 6, 2, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 1, 6, 5, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 2, 1, 9, 2, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 1, 9, 5, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 3, 2, 6, 4, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 3, 2, 9, 4, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 2, 2, 7, 2, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 2, 7, 5, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(world, LAMP_BLOCK, 2, 2, 5, chunkBox);
            this.placeBlock(world, LAMP_BLOCK, 5, 2, 5, chunkBox);
            this.placeBlock(world, LAMP_BLOCK, 2, 2, 10, chunkBox);
            this.placeBlock(world, LAMP_BLOCK, 5, 2, 10, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 2, 3, 5, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 5, 3, 5, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 2, 3, 10, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 5, 3, 10, chunkBox);
            if (roomDefinition2.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 0, 4, 2, 0);
            }

            if (roomDefinition2.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 7, 1, 3, 7, 2, 4);
            }

            if (roomDefinition2.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 1, 3, 0, 2, 4);
            }

            if (roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 15, 4, 2, 15);
            }

            if (roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 1, 11, 0, 2, 12);
            }

            if (roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 7, 1, 11, 7, 2, 12);
            }

        }
    }

    public static class OceanMonumentEntryRoom extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentEntryRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, 1, orientation, setting, 1, 1, 1);
        }

        public OceanMonumentEntryRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 3, 0, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 0, 2, 0, 1, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, 2, 0, 7, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 0, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 0, 2, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 5, 1, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 7, 4, 2, 7);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 0, 1, 3, 1, 2, 4);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 6, 1, 3, 7, 2, 4);
            }

        }
    }

    public static class OceanMonumentPenthouse extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentPenthouse(Direction orientation, BoundingBox box) {
            super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, orientation, 1, box);
        }

        public OceanMonumentPenthouse(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 2, -1, 2, 11, -1, 11, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 0, -1, 0, 1, -1, 11, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 12, -1, 0, 13, -1, 11, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 2, -1, 0, 11, -1, 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 2, -1, 12, 11, -1, 13, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(world, chunkBox, 0, 0, 0, 0, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 13, 0, 0, 13, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 0, 0, 12, 0, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 0, 13, 12, 0, 13, BASE_LIGHT, BASE_LIGHT, false);

            for(int i = 2; i <= 11; i += 3) {
                this.placeBlock(world, LAMP_BLOCK, 0, 0, i, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 13, 0, i, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, i, 0, 0, chunkBox);
            }

            this.generateBox(world, chunkBox, 2, 0, 3, 4, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 9, 0, 3, 11, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 4, 0, 9, 9, 0, 11, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(world, BASE_LIGHT, 5, 0, 8, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 8, 0, 8, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 10, 0, 10, chunkBox);
            this.placeBlock(world, BASE_LIGHT, 3, 0, 10, chunkBox);
            this.generateBox(world, chunkBox, 3, 0, 3, 3, 0, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 10, 0, 3, 10, 0, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 6, 0, 10, 7, 0, 10, BASE_BLACK, BASE_BLACK, false);
            int j = 3;

            for(int k = 0; k < 2; ++k) {
                for(int l = 2; l <= 8; l += 3) {
                    this.generateBox(world, chunkBox, j, 0, l, j, 2, l, BASE_LIGHT, BASE_LIGHT, false);
                }

                j = 10;
            }

            this.generateBox(world, chunkBox, 5, 0, 10, 5, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 8, 0, 10, 8, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 6, -1, 7, 7, -1, 8, BASE_BLACK, BASE_BLACK, false);
            this.generateWaterBox(world, chunkBox, 6, -1, 3, 7, -1, 4);
            this.spawnElder(world, chunkBox, 6, 1, 6);
        }
    }

    protected abstract static class OceanMonumentPiece extends StructurePiece {
        protected static final BlockState BASE_GRAY = Blocks.PRISMARINE.defaultBlockState();
        protected static final BlockState BASE_LIGHT = Blocks.PRISMARINE_BRICKS.defaultBlockState();
        protected static final BlockState BASE_BLACK = Blocks.DARK_PRISMARINE.defaultBlockState();
        protected static final BlockState DOT_DECO_DATA = BASE_LIGHT;
        protected static final BlockState LAMP_BLOCK = Blocks.SEA_LANTERN.defaultBlockState();
        protected static final boolean DO_FILL = true;
        protected static final BlockState FILL_BLOCK = Blocks.WATER.defaultBlockState();
        protected static final Set<Block> FILL_KEEP = ImmutableSet.<Block>builder().add(Blocks.ICE).add(Blocks.PACKED_ICE).add(Blocks.BLUE_ICE).add(FILL_BLOCK.getBlock()).build();
        protected static final int GRIDROOM_WIDTH = 8;
        protected static final int GRIDROOM_DEPTH = 8;
        protected static final int GRIDROOM_HEIGHT = 4;
        protected static final int GRID_WIDTH = 5;
        protected static final int GRID_DEPTH = 5;
        protected static final int GRID_HEIGHT = 3;
        protected static final int GRID_FLOOR_COUNT = 25;
        protected static final int GRID_SIZE = 75;
        protected static final int GRIDROOM_SOURCE_INDEX = getRoomIndex(2, 0, 0);
        protected static final int GRIDROOM_TOP_CONNECT_INDEX = getRoomIndex(2, 2, 0);
        protected static final int GRIDROOM_LEFTWING_CONNECT_INDEX = getRoomIndex(0, 1, 0);
        protected static final int GRIDROOM_RIGHTWING_CONNECT_INDEX = getRoomIndex(4, 1, 0);
        protected static final int LEFTWING_INDEX = 1001;
        protected static final int RIGHTWING_INDEX = 1002;
        protected static final int PENTHOUSE_INDEX = 1003;
        protected OceanMonumentPieces.RoomDefinition roomDefinition;

        protected static int getRoomIndex(int x, int y, int z) {
            return y * 25 + z * 5 + x;
        }

        public OceanMonumentPiece(StructurePieceType type, Direction orientation, int length, BoundingBox box) {
            super(type, length, box);
            this.setOrientation(orientation);
        }

        protected OceanMonumentPiece(StructurePieceType type, int length, Direction orientation, OceanMonumentPieces.RoomDefinition setting, int i, int j, int k) {
            super(type, length, makeBoundingBox(orientation, setting, i, j, k));
            this.setOrientation(orientation);
            this.roomDefinition = setting;
        }

        private static BoundingBox makeBoundingBox(Direction orientation, OceanMonumentPieces.RoomDefinition setting, int i, int j, int k) {
            int l = setting.index;
            int m = l % 5;
            int n = l / 5 % 5;
            int o = l / 25;
            BoundingBox boundingBox = makeBoundingBox(0, 0, 0, orientation, i * 8, j * 4, k * 8);
            switch(orientation) {
            case NORTH:
                boundingBox.move(m * 8, o * 4, -(n + k) * 8 + 1);
                break;
            case SOUTH:
                boundingBox.move(m * 8, o * 4, n * 8);
                break;
            case WEST:
                boundingBox.move(-(n + k) * 8 + 1, o * 4, m * 8);
                break;
            case EAST:
            default:
                boundingBox.move(n * 8, o * 4, m * 8);
            }

            return boundingBox;
        }

        public OceanMonumentPiece(StructurePieceType type, CompoundTag nbt) {
            super(type, nbt);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        }

        protected void generateWaterBox(WorldGenLevel world, BoundingBox box, int x, int y, int z, int width, int height, int depth) {
            for(int i = y; i <= height; ++i) {
                for(int j = x; j <= width; ++j) {
                    for(int k = z; k <= depth; ++k) {
                        BlockState blockState = this.getBlock(world, j, i, k, box);
                        if (!FILL_KEEP.contains(blockState.getBlock())) {
                            if (this.getWorldY(i) >= world.getSeaLevel() && blockState != FILL_BLOCK) {
                                this.placeBlock(world, Blocks.AIR.defaultBlockState(), j, i, k, box);
                            } else {
                                this.placeBlock(world, FILL_BLOCK, j, i, k, box);
                            }
                        }
                    }
                }
            }

        }

        protected void generateDefaultFloor(WorldGenLevel world, BoundingBox box, int x, int z, boolean bl) {
            if (bl) {
                this.generateBox(world, box, x + 0, 0, z + 0, x + 2, 0, z + 8 - 1, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, x + 5, 0, z + 0, x + 8 - 1, 0, z + 8 - 1, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, x + 3, 0, z + 0, x + 4, 0, z + 2, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, x + 3, 0, z + 5, x + 4, 0, z + 8 - 1, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, box, x + 3, 0, z + 2, x + 4, 0, z + 2, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, box, x + 3, 0, z + 5, x + 4, 0, z + 5, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, box, x + 2, 0, z + 3, x + 2, 0, z + 4, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, box, x + 5, 0, z + 3, x + 5, 0, z + 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
                this.generateBox(world, box, x + 0, 0, z + 0, x + 8 - 1, 0, z + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            }

        }

        protected void generateBoxOnFillOnly(WorldGenLevel world, BoundingBox box, int x, int y, int z, int width, int height, int depth, BlockState state) {
            for(int i = y; i <= height; ++i) {
                for(int j = x; j <= width; ++j) {
                    for(int k = z; k <= depth; ++k) {
                        if (this.getBlock(world, j, i, k, box) == FILL_BLOCK) {
                            this.placeBlock(world, state, j, i, k, box);
                        }
                    }
                }
            }

        }

        protected boolean chunkIntersects(BoundingBox box, int x, int i, int z, int j) {
            int k = this.getWorldX(x, i);
            int l = this.getWorldZ(x, i);
            int m = this.getWorldX(z, j);
            int n = this.getWorldZ(z, j);
            return box.intersects(Math.min(k, m), Math.min(l, n), Math.max(k, m), Math.max(l, n));
        }

        protected boolean spawnElder(WorldGenLevel world, BoundingBox box, int x, int y, int z) {
            BlockPos blockPos = this.getWorldPos(x, y, z);
            if (box.isInside(blockPos)) {
                ElderGuardian elderGuardian = EntityType.ELDER_GUARDIAN.create(world.getLevel());
                elderGuardian.heal(elderGuardian.getMaxHealth());
                elderGuardian.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                elderGuardian.finalizeSpawn(world, world.getCurrentDifficultyAt(elderGuardian.blockPosition()), MobSpawnType.STRUCTURE, (SpawnGroupData)null, (CompoundTag)null);
                world.addFreshEntityWithPassengers(elderGuardian);
                return true;
            } else {
                return false;
            }
        }
    }

    public static class OceanMonumentSimpleRoom extends OceanMonumentPieces.OceanMonumentPiece {
        private int mainDesign;

        public OceanMonumentSimpleRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting, Random random) {
            super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, 1, orientation, setting, 1, 1, 1);
            this.mainDesign = random.nextInt(3);
        }

        public OceanMonumentSimpleRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            if (this.roomDefinition.index / 25 > 0) {
                this.generateDefaultFloor(world, chunkBox, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
            }

            if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 4, 1, 6, 4, 6, BASE_GRAY);
            }

            boolean bl = this.mainDesign != 0 && random.nextBoolean() && !this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()] && !this.roomDefinition.hasOpening[Direction.UP.get3DDataValue()] && this.roomDefinition.countOpenings() > 1;
            if (this.mainDesign == 0) {
                this.generateBox(world, chunkBox, 0, 1, 0, 2, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 3, 0, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 2, 0, 0, 2, 2, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, chunkBox, 1, 2, 0, 2, 2, 0, BASE_GRAY, BASE_GRAY, false);
                this.placeBlock(world, LAMP_BLOCK, 1, 2, 1, chunkBox);
                this.generateBox(world, chunkBox, 5, 1, 0, 7, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 5, 3, 0, 7, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 7, 2, 0, 7, 2, 2, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, chunkBox, 5, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
                this.placeBlock(world, LAMP_BLOCK, 6, 2, 1, chunkBox);
                this.generateBox(world, chunkBox, 0, 1, 5, 2, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 3, 5, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 2, 5, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, chunkBox, 1, 2, 7, 2, 2, 7, BASE_GRAY, BASE_GRAY, false);
                this.placeBlock(world, LAMP_BLOCK, 1, 2, 6, chunkBox);
                this.generateBox(world, chunkBox, 5, 1, 5, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 5, 3, 5, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 7, 2, 5, 7, 2, 7, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, chunkBox, 5, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
                this.placeBlock(world, LAMP_BLOCK, 6, 2, 6, chunkBox);
                if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 3, 3, 0, 4, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
                } else {
                    this.generateBox(world, chunkBox, 3, 3, 0, 4, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 3, 2, 0, 4, 2, 0, BASE_GRAY, BASE_GRAY, false);
                    this.generateBox(world, chunkBox, 3, 1, 0, 4, 1, 1, BASE_LIGHT, BASE_LIGHT, false);
                }

                if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 3, 3, 7, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                } else {
                    this.generateBox(world, chunkBox, 3, 3, 6, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 3, 2, 7, 4, 2, 7, BASE_GRAY, BASE_GRAY, false);
                    this.generateBox(world, chunkBox, 3, 1, 6, 4, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                }

                if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 0, 3, 3, 0, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
                } else {
                    this.generateBox(world, chunkBox, 0, 3, 3, 1, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 0, 2, 3, 0, 2, 4, BASE_GRAY, BASE_GRAY, false);
                    this.generateBox(world, chunkBox, 0, 1, 3, 1, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
                }

                if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 7, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
                } else {
                    this.generateBox(world, chunkBox, 6, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 7, 2, 3, 7, 2, 4, BASE_GRAY, BASE_GRAY, false);
                    this.generateBox(world, chunkBox, 6, 1, 3, 7, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
                }
            } else if (this.mainDesign == 1) {
                this.generateBox(world, chunkBox, 2, 1, 2, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 2, 1, 5, 2, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 5, 1, 5, 5, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 5, 1, 2, 5, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
                this.placeBlock(world, LAMP_BLOCK, 2, 2, 2, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 2, 2, 5, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 5, 2, 5, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 5, 2, 2, chunkBox);
                this.generateBox(world, chunkBox, 0, 1, 0, 1, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 1, 1, 0, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 1, 7, 1, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 1, 6, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 6, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 7, 1, 6, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 6, 1, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 7, 1, 1, 7, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
                this.placeBlock(world, BASE_GRAY, 1, 2, 0, chunkBox);
                this.placeBlock(world, BASE_GRAY, 0, 2, 1, chunkBox);
                this.placeBlock(world, BASE_GRAY, 1, 2, 7, chunkBox);
                this.placeBlock(world, BASE_GRAY, 0, 2, 6, chunkBox);
                this.placeBlock(world, BASE_GRAY, 6, 2, 7, chunkBox);
                this.placeBlock(world, BASE_GRAY, 7, 2, 6, chunkBox);
                this.placeBlock(world, BASE_GRAY, 6, 2, 0, chunkBox);
                this.placeBlock(world, BASE_GRAY, 7, 2, 1, chunkBox);
                if (!this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 1, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
                    this.generateBox(world, chunkBox, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
                }

                if (!this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 1, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
                    this.generateBox(world, chunkBox, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                }

                if (!this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 0, 3, 1, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 0, 2, 1, 0, 2, 6, BASE_GRAY, BASE_GRAY, false);
                    this.generateBox(world, chunkBox, 0, 1, 1, 0, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
                }

                if (!this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
                    this.generateBox(world, chunkBox, 7, 3, 1, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, 7, 2, 1, 7, 2, 6, BASE_GRAY, BASE_GRAY, false);
                    this.generateBox(world, chunkBox, 7, 1, 1, 7, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
                }
            } else if (this.mainDesign == 2) {
                this.generateBox(world, chunkBox, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
                if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                    this.generateWaterBox(world, chunkBox, 3, 1, 0, 4, 2, 0);
                }

                if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
                    this.generateWaterBox(world, chunkBox, 3, 1, 7, 4, 2, 7);
                }

                if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
                    this.generateWaterBox(world, chunkBox, 0, 1, 3, 0, 2, 4);
                }

                if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
                    this.generateWaterBox(world, chunkBox, 7, 1, 3, 7, 2, 4);
                }
            }

            if (bl) {
                this.generateBox(world, chunkBox, 3, 1, 3, 4, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 3, 2, 3, 4, 2, 4, BASE_GRAY, BASE_GRAY, false);
                this.generateBox(world, chunkBox, 3, 3, 3, 4, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            }

        }
    }

    public static class OceanMonumentSimpleTopRoom extends OceanMonumentPieces.OceanMonumentPiece {
        public OceanMonumentSimpleTopRoom(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, 1, orientation, setting, 1, 1, 1);
        }

        public OceanMonumentSimpleTopRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            if (this.roomDefinition.index / 25 > 0) {
                this.generateDefaultFloor(world, chunkBox, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
            }

            if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
                this.generateBoxOnFillOnly(world, chunkBox, 1, 4, 1, 6, 4, 6, BASE_GRAY);
            }

            for(int i = 1; i <= 6; ++i) {
                for(int j = 1; j <= 6; ++j) {
                    if (random.nextInt(3) != 0) {
                        int k = 2 + (random.nextInt(4) == 0 ? 0 : 1);
                        BlockState blockState = Blocks.WET_SPONGE.defaultBlockState();
                        this.generateBox(world, chunkBox, i, k, j, i, 3, j, blockState, blockState, false);
                    }
                }
            }

            this.generateBox(world, chunkBox, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(world, chunkBox, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(world, chunkBox, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
                this.generateWaterBox(world, chunkBox, 3, 1, 0, 4, 2, 0);
            }

        }
    }

    public static class OceanMonumentWingRoom extends OceanMonumentPieces.OceanMonumentPiece {
        private int mainDesign;

        public OceanMonumentWingRoom(Direction orientation, BoundingBox box, int i) {
            super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, orientation, 1, box);
            this.mainDesign = i & 1;
        }

        public OceanMonumentWingRoom(CompoundTag nbt) {
            super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, nbt);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            if (this.mainDesign == 0) {
                for(int i = 0; i < 4; ++i) {
                    this.generateBox(world, chunkBox, 10 - i, 3 - i, 20 - i, 12 + i, 3 - i, 20, BASE_LIGHT, BASE_LIGHT, false);
                }

                this.generateBox(world, chunkBox, 7, 0, 6, 15, 0, 16, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 6, 0, 6, 6, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 16, 0, 6, 16, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 7, 1, 7, 7, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 15, 1, 7, 15, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 7, 1, 6, 9, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 13, 1, 6, 15, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 8, 1, 7, 9, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 13, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 9, 0, 5, 13, 0, 5, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 10, 0, 7, 12, 0, 7, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 8, 0, 10, 8, 0, 12, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 14, 0, 10, 14, 0, 12, BASE_BLACK, BASE_BLACK, false);

                for(int j = 18; j >= 7; j -= 3) {
                    this.placeBlock(world, LAMP_BLOCK, 6, 3, j, chunkBox);
                    this.placeBlock(world, LAMP_BLOCK, 16, 3, j, chunkBox);
                }

                this.placeBlock(world, LAMP_BLOCK, 10, 0, 10, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 12, 0, 10, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 10, 0, 12, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 12, 0, 12, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 8, 3, 6, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 14, 3, 6, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 4, 2, 4, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 4, 1, 4, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 4, 0, 4, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 18, 2, 4, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 18, 1, 4, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 18, 0, 4, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 4, 2, 18, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 4, 1, 18, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 4, 0, 18, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 18, 2, 18, chunkBox);
                this.placeBlock(world, LAMP_BLOCK, 18, 1, 18, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 18, 0, 18, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 9, 7, 20, chunkBox);
                this.placeBlock(world, BASE_LIGHT, 13, 7, 20, chunkBox);
                this.generateBox(world, chunkBox, 6, 0, 21, 7, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 15, 0, 21, 16, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
                this.spawnElder(world, chunkBox, 11, 2, 16);
            } else if (this.mainDesign == 1) {
                this.generateBox(world, chunkBox, 9, 3, 18, 13, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 9, 0, 18, 9, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
                this.generateBox(world, chunkBox, 13, 0, 18, 13, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
                int k = 9;
                int l = 20;
                int m = 5;

                for(int n = 0; n < 2; ++n) {
                    this.placeBlock(world, BASE_LIGHT, k, 6, 20, chunkBox);
                    this.placeBlock(world, LAMP_BLOCK, k, 5, 20, chunkBox);
                    this.placeBlock(world, BASE_LIGHT, k, 4, 20, chunkBox);
                    k = 13;
                }

                this.generateBox(world, chunkBox, 7, 3, 7, 15, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
                k = 10;

                for(int o = 0; o < 2; ++o) {
                    this.generateBox(world, chunkBox, k, 0, 10, k, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, k, 0, 12, k, 6, 12, BASE_LIGHT, BASE_LIGHT, false);
                    this.placeBlock(world, LAMP_BLOCK, k, 0, 10, chunkBox);
                    this.placeBlock(world, LAMP_BLOCK, k, 0, 12, chunkBox);
                    this.placeBlock(world, LAMP_BLOCK, k, 4, 10, chunkBox);
                    this.placeBlock(world, LAMP_BLOCK, k, 4, 12, chunkBox);
                    k = 12;
                }

                k = 8;

                for(int p = 0; p < 2; ++p) {
                    this.generateBox(world, chunkBox, k, 0, 7, k, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
                    this.generateBox(world, chunkBox, k, 0, 14, k, 2, 14, BASE_LIGHT, BASE_LIGHT, false);
                    k = 14;
                }

                this.generateBox(world, chunkBox, 8, 3, 8, 8, 3, 13, BASE_BLACK, BASE_BLACK, false);
                this.generateBox(world, chunkBox, 14, 3, 8, 14, 3, 13, BASE_BLACK, BASE_BLACK, false);
                this.spawnElder(world, chunkBox, 11, 5, 13);
            }

        }
    }

    static class RoomDefinition {
        final int index;
        final OceanMonumentPieces.RoomDefinition[] connections = new OceanMonumentPieces.RoomDefinition[6];
        final boolean[] hasOpening = new boolean[6];
        boolean claimed;
        boolean isSource;
        private int scanIndex;

        public RoomDefinition(int index) {
            this.index = index;
        }

        public void setConnection(Direction orientation, OceanMonumentPieces.RoomDefinition setting) {
            this.connections[orientation.get3DDataValue()] = setting;
            setting.connections[orientation.getOpposite().get3DDataValue()] = this;
        }

        public void updateOpenings() {
            for(int i = 0; i < 6; ++i) {
                this.hasOpening[i] = this.connections[i] != null;
            }

        }

        public boolean findSource(int i) {
            if (this.isSource) {
                return true;
            } else {
                this.scanIndex = i;

                for(int j = 0; j < 6; ++j) {
                    if (this.connections[j] != null && this.hasOpening[j] && this.connections[j].scanIndex != i && this.connections[j].findSource(i)) {
                        return true;
                    }
                }

                return false;
            }
        }

        public boolean isSpecial() {
            return this.index >= 75;
        }

        public int countOpenings() {
            int i = 0;

            for(int j = 0; j < 6; ++j) {
                if (this.hasOpening[j]) {
                    ++i;
                }
            }

            return i;
        }
    }
}
