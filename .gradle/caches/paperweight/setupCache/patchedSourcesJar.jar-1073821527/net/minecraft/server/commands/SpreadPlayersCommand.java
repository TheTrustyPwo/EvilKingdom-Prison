package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.Team;

public class SpreadPlayersCommand {
    private static final int MAX_ITERATION_COUNT = 10000;
    private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_TEAMS = new Dynamic4CommandExceptionType((pilesCount, x, z, maxSpreadDistance) -> {
        return new TranslatableComponent("commands.spreadplayers.failed.teams", pilesCount, x, z, maxSpreadDistance);
    });
    private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_ENTITIES = new Dynamic4CommandExceptionType((pilesCount, x, z, maxSpreadDistance) -> {
        return new TranslatableComponent("commands.spreadplayers.failed.entities", pilesCount, x, z, maxSpreadDistance);
    });
    private static final Dynamic2CommandExceptionType ERROR_INVALID_MAX_HEIGHT = new Dynamic2CommandExceptionType((maxY, worldBottomY) -> {
        return new TranslatableComponent("commands.spreadplayers.failed.invalid.height", maxY, worldBottomY);
    });

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spreadplayers").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("center", Vec2Argument.vec2()).then(Commands.argument("spreadDistance", FloatArgumentType.floatArg(0.0F)).then(Commands.argument("maxRange", FloatArgumentType.floatArg(1.0F)).then(Commands.argument("respectTeams", BoolArgumentType.bool()).then(Commands.argument("targets", EntityArgument.entities()).executes((context) -> {
            return spreadPlayers(context.getSource(), Vec2Argument.getVec2(context, "center"), FloatArgumentType.getFloat(context, "spreadDistance"), FloatArgumentType.getFloat(context, "maxRange"), context.getSource().getLevel().getMaxBuildHeight(), BoolArgumentType.getBool(context, "respectTeams"), EntityArgument.getEntities(context, "targets"));
        }))).then(Commands.literal("under").then(Commands.argument("maxHeight", IntegerArgumentType.integer()).then(Commands.argument("respectTeams", BoolArgumentType.bool()).then(Commands.argument("targets", EntityArgument.entities()).executes((context) -> {
            return spreadPlayers(context.getSource(), Vec2Argument.getVec2(context, "center"), FloatArgumentType.getFloat(context, "spreadDistance"), FloatArgumentType.getFloat(context, "maxRange"), IntegerArgumentType.getInteger(context, "maxHeight"), BoolArgumentType.getBool(context, "respectTeams"), EntityArgument.getEntities(context, "targets"));
        })))))))));
    }

    private static int spreadPlayers(CommandSourceStack source, Vec2 center, float spreadDistance, float maxRange, int maxY, boolean respectTeams, Collection<? extends Entity> players) throws CommandSyntaxException {
        ServerLevel serverLevel = source.getLevel();
        int i = serverLevel.getMinBuildHeight();
        if (maxY < i) {
            throw ERROR_INVALID_MAX_HEIGHT.create(maxY, i);
        } else {
            Random random = new Random();
            double d = (double)(center.x - maxRange);
            double e = (double)(center.y - maxRange);
            double f = (double)(center.x + maxRange);
            double g = (double)(center.y + maxRange);
            SpreadPlayersCommand.Position[] positions = createInitialPositions(random, respectTeams ? getNumberOfTeams(players) : players.size(), d, e, f, g);
            spreadPositions(center, (double)spreadDistance, serverLevel, random, d, e, f, g, maxY, positions, respectTeams);
            double h = setPlayerPositions(players, serverLevel, positions, maxY, respectTeams);
            source.sendSuccess(new TranslatableComponent("commands.spreadplayers.success." + (respectTeams ? "teams" : "entities"), positions.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", h)), true);
            return positions.length;
        }
    }

    private static int getNumberOfTeams(Collection<? extends Entity> entities) {
        Set<Team> set = Sets.newHashSet();

        for(Entity entity : entities) {
            if (entity instanceof Player) {
                set.add(entity.getTeam());
            } else {
                set.add((Team)null);
            }
        }

        return set.size();
    }

    private static void spreadPositions(Vec2 center, double spreadDistance, ServerLevel world, Random random, double minX, double minZ, double maxX, double maxZ, int maxY, SpreadPlayersCommand.Position[] piles, boolean respectTeams) throws CommandSyntaxException {
        boolean bl = true;
        double d = (double)Float.MAX_VALUE;

        int i;
        for(i = 0; i < 10000 && bl; ++i) {
            bl = false;
            d = (double)Float.MAX_VALUE;

            for(int j = 0; j < piles.length; ++j) {
                SpreadPlayersCommand.Position position = piles[j];
                int k = 0;
                SpreadPlayersCommand.Position position2 = new SpreadPlayersCommand.Position();

                for(int l = 0; l < piles.length; ++l) {
                    if (j != l) {
                        SpreadPlayersCommand.Position position3 = piles[l];
                        double e = position.dist(position3);
                        d = Math.min(e, d);
                        if (e < spreadDistance) {
                            ++k;
                            position2.x += position3.x - position.x;
                            position2.z += position3.z - position.z;
                        }
                    }
                }

                if (k > 0) {
                    position2.x /= (double)k;
                    position2.z /= (double)k;
                    double f = position2.getLength();
                    if (f > 0.0D) {
                        position2.normalize();
                        position.moveAway(position2);
                    } else {
                        position.randomize(random, minX, minZ, maxX, maxZ);
                    }

                    bl = true;
                }

                if (position.clamp(minX, minZ, maxX, maxZ)) {
                    bl = true;
                }
            }

            if (!bl) {
                for(SpreadPlayersCommand.Position position4 : piles) {
                    if (!position4.isSafe(world, maxY)) {
                        position4.randomize(random, minX, minZ, maxX, maxZ);
                        bl = true;
                    }
                }
            }
        }

        if (d == (double)Float.MAX_VALUE) {
            d = 0.0D;
        }

        if (i >= 10000) {
            if (respectTeams) {
                throw ERROR_FAILED_TO_SPREAD_TEAMS.create(piles.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", d));
            } else {
                throw ERROR_FAILED_TO_SPREAD_ENTITIES.create(piles.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", d));
            }
        }
    }

    private static double setPlayerPositions(Collection<? extends Entity> entities, ServerLevel world, SpreadPlayersCommand.Position[] piles, int maxY, boolean respectTeams) {
        double d = 0.0D;
        int i = 0;
        Map<Team, SpreadPlayersCommand.Position> map = Maps.newHashMap();

        for(Entity entity : entities) {
            SpreadPlayersCommand.Position position;
            if (respectTeams) {
                Team team = entity instanceof Player ? entity.getTeam() : null;
                if (!map.containsKey(team)) {
                    map.put(team, piles[i++]);
                }

                position = map.get(team);
            } else {
                position = piles[i++];
            }

            entity.teleportToWithTicket((double)Mth.floor(position.x) + 0.5D, (double)position.getSpawnY(world, maxY), (double)Mth.floor(position.z) + 0.5D);
            double e = Double.MAX_VALUE;

            for(SpreadPlayersCommand.Position position3 : piles) {
                if (position != position3) {
                    double f = position.dist(position3);
                    e = Math.min(f, e);
                }
            }

            d += e;
        }

        return entities.size() < 2 ? 0.0D : d / (double)entities.size();
    }

    private static SpreadPlayersCommand.Position[] createInitialPositions(Random random, int count, double minX, double minZ, double maxX, double maxZ) {
        SpreadPlayersCommand.Position[] positions = new SpreadPlayersCommand.Position[count];

        for(int i = 0; i < positions.length; ++i) {
            SpreadPlayersCommand.Position position = new SpreadPlayersCommand.Position();
            position.randomize(random, minX, minZ, maxX, maxZ);
            positions[i] = position;
        }

        return positions;
    }

    static class Position {
        double x;
        double z;

        double dist(SpreadPlayersCommand.Position other) {
            double d = this.x - other.x;
            double e = this.z - other.z;
            return Math.sqrt(d * d + e * e);
        }

        void normalize() {
            double d = this.getLength();
            this.x /= d;
            this.z /= d;
        }

        double getLength() {
            return Math.sqrt(this.x * this.x + this.z * this.z);
        }

        public void moveAway(SpreadPlayersCommand.Position other) {
            this.x -= other.x;
            this.z -= other.z;
        }

        public boolean clamp(double minX, double minZ, double maxX, double maxZ) {
            boolean bl = false;
            if (this.x < minX) {
                this.x = minX;
                bl = true;
            } else if (this.x > maxX) {
                this.x = maxX;
                bl = true;
            }

            if (this.z < minZ) {
                this.z = minZ;
                bl = true;
            } else if (this.z > maxZ) {
                this.z = maxZ;
                bl = true;
            }

            return bl;
        }

        public int getSpawnY(BlockGetter blockView, int maxY) {
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(this.x, (double)(maxY + 1), this.z);
            boolean bl = blockView.getBlockState(mutableBlockPos).isAir();
            mutableBlockPos.move(Direction.DOWN);

            boolean bl3;
            for(boolean bl2 = blockView.getBlockState(mutableBlockPos).isAir(); mutableBlockPos.getY() > blockView.getMinBuildHeight(); bl2 = bl3) {
                mutableBlockPos.move(Direction.DOWN);
                bl3 = blockView.getBlockState(mutableBlockPos).isAir();
                if (!bl3 && bl2 && bl) {
                    return mutableBlockPos.getY() + 1;
                }

                bl = bl2;
            }

            return maxY + 1;
        }

        public boolean isSafe(BlockGetter world, int maxY) {
            BlockPos blockPos = new BlockPos(this.x, (double)(this.getSpawnY(world, maxY) - 1), this.z);
            BlockState blockState = world.getBlockState(blockPos);
            Material material = blockState.getMaterial();
            return blockPos.getY() < maxY && !material.isLiquid() && material != Material.FIRE;
        }

        public void randomize(Random random, double minX, double minZ, double maxX, double maxZ) {
            this.x = Mth.nextDouble(random, minX, maxX);
            this.z = Mth.nextDouble(random, minZ, maxZ);
        }
    }
}
