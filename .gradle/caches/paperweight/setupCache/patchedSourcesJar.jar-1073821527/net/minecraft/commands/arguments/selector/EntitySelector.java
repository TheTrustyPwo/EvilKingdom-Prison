package net.minecraft.commands.arguments.selector;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntitySelector {
    public static final int INFINITE = Integer.MAX_VALUE;
    private static final EntityTypeTest<Entity, ?> ANY_TYPE = new EntityTypeTest<Entity, Entity>() {
        @Override
        public Entity tryCast(Entity obj) {
            return obj;
        }

        @Override
        public Class<? extends Entity> getBaseClass() {
            return Entity.class;
        }
    };
    private final int maxResults;
    private final boolean includesEntities;
    private final boolean worldLimited;
    private final Predicate<Entity> predicate;
    private final MinMaxBounds.Doubles range;
    private final Function<Vec3, Vec3> position;
    @Nullable
    private final AABB aabb;
    private final BiConsumer<Vec3, List<? extends Entity>> order;
    private final boolean currentEntity;
    @Nullable
    private final String playerName;
    @Nullable
    private final UUID entityUUID;
    private EntityTypeTest<Entity, ?> type;
    private final boolean usesSelector;

    public EntitySelector(int count, boolean includesNonPlayers, boolean localWorldOnly, Predicate<Entity> basePredicate, MinMaxBounds.Doubles distance, Function<Vec3, Vec3> positionOffset, @Nullable AABB box, BiConsumer<Vec3, List<? extends Entity>> sorter, boolean senderOnly, @Nullable String playerName, @Nullable UUID uuid, @Nullable EntityType<?> type, boolean usesAt) {
        this.maxResults = count;
        this.includesEntities = includesNonPlayers;
        this.worldLimited = localWorldOnly;
        this.predicate = basePredicate;
        this.range = distance;
        this.position = positionOffset;
        this.aabb = box;
        this.order = sorter;
        this.currentEntity = senderOnly;
        this.playerName = playerName;
        this.entityUUID = uuid;
        this.type = (EntityTypeTest<Entity, ?>)(type == null ? ANY_TYPE : type);
        this.usesSelector = usesAt;
    }

    public int getMaxResults() {
        return this.maxResults;
    }

    public boolean includesEntities() {
        return this.includesEntities;
    }

    public boolean isSelfSelector() {
        return this.currentEntity;
    }

    public boolean isWorldLimited() {
        return this.worldLimited;
    }

    public boolean usesSelector() {
        return this.usesSelector;
    }

    private void checkPermissions(CommandSourceStack source) throws CommandSyntaxException {
        if (this.usesSelector && !source.hasPermission(2)) {
            throw EntityArgument.ERROR_SELECTORS_NOT_ALLOWED.create();
        }
    }

    public Entity findSingleEntity(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermissions(source);
        List<? extends Entity> list = this.findEntities(source);
        if (list.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        } else if (list.size() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
        } else {
            return list.get(0);
        }
    }

    public List<? extends Entity> findEntities(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermissions(source);
        if (!this.includesEntities) {
            return this.findPlayers(source);
        } else if (this.playerName != null) {
            ServerPlayer serverPlayer = source.getServer().getPlayerList().getPlayerByName(this.playerName);
            return (List<? extends Entity>)(serverPlayer == null ? Collections.emptyList() : Lists.newArrayList(serverPlayer));
        } else if (this.entityUUID != null) {
            for(ServerLevel serverLevel : source.getServer().getAllLevels()) {
                Entity entity = serverLevel.getEntity(this.entityUUID);
                if (entity != null) {
                    return Lists.newArrayList(entity);
                }
            }

            return Collections.emptyList();
        } else {
            Vec3 vec3 = this.position.apply(source.getPosition());
            Predicate<Entity> predicate = this.getPredicate(vec3);
            if (this.currentEntity) {
                return (List<? extends Entity>)(source.getEntity() != null && predicate.test(source.getEntity()) ? Lists.newArrayList(source.getEntity()) : Collections.emptyList());
            } else {
                List<Entity> list = Lists.newArrayList();
                if (this.isWorldLimited()) {
                    this.addEntities(list, source.getLevel(), vec3, predicate);
                } else {
                    for(ServerLevel serverLevel2 : source.getServer().getAllLevels()) {
                        this.addEntities(list, serverLevel2, vec3, predicate);
                    }
                }

                return this.sortAndLimit(vec3, list);
            }
        }
    }

    private void addEntities(List<Entity> result, ServerLevel world, Vec3 pos, Predicate<Entity> predicate) {
        if (this.aabb != null) {
            result.addAll(world.getEntities(this.type, this.aabb.move(pos), predicate));
        } else {
            result.addAll(world.getEntities(this.type, predicate));
        }

    }

    public ServerPlayer findSinglePlayer(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermissions(source);
        List<ServerPlayer> list = this.findPlayers(source);
        if (list.size() != 1) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        } else {
            return list.get(0);
        }
    }

    public List<ServerPlayer> findPlayers(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermissions(source);
        if (this.playerName != null) {
            ServerPlayer serverPlayer = source.getServer().getPlayerList().getPlayerByName(this.playerName);
            return (List<ServerPlayer>)(serverPlayer == null ? Collections.emptyList() : Lists.newArrayList(serverPlayer));
        } else if (this.entityUUID != null) {
            ServerPlayer serverPlayer2 = source.getServer().getPlayerList().getPlayer(this.entityUUID);
            return (List<ServerPlayer>)(serverPlayer2 == null ? Collections.emptyList() : Lists.newArrayList(serverPlayer2));
        } else {
            Vec3 vec3 = this.position.apply(source.getPosition());
            Predicate<Entity> predicate = this.getPredicate(vec3);
            if (this.currentEntity) {
                if (source.getEntity() instanceof ServerPlayer) {
                    ServerPlayer serverPlayer3 = (ServerPlayer)source.getEntity();
                    if (predicate.test(serverPlayer3)) {
                        return Lists.newArrayList(serverPlayer3);
                    }
                }

                return Collections.emptyList();
            } else {
                List<ServerPlayer> list;
                if (this.isWorldLimited()) {
                    list = source.getLevel().getPlayers(predicate);
                } else {
                    list = Lists.newArrayList();

                    for(ServerPlayer serverPlayer4 : source.getServer().getPlayerList().getPlayers()) {
                        if (predicate.test(serverPlayer4)) {
                            list.add(serverPlayer4);
                        }
                    }
                }

                return this.sortAndLimit(vec3, list);
            }
        }
    }

    private Predicate<Entity> getPredicate(Vec3 pos) {
        Predicate<Entity> predicate = this.predicate;
        if (this.aabb != null) {
            AABB aABB = this.aabb.move(pos);
            predicate = predicate.and((entity) -> {
                return aABB.intersects(entity.getBoundingBox());
            });
        }

        if (!this.range.isAny()) {
            predicate = predicate.and((entity) -> {
                return this.range.matchesSqr(entity.distanceToSqr(pos));
            });
        }

        return predicate;
    }

    private <T extends Entity> List<T> sortAndLimit(Vec3 pos, List<T> entities) {
        if (entities.size() > 1) {
            this.order.accept(pos, entities);
        }

        return entities.subList(0, Math.min(this.maxResults, entities.size()));
    }

    public static Component joinNames(List<? extends Entity> entities) {
        return ComponentUtils.formatList(entities, Entity::getDisplayName);
    }
}
