package net.minecraft.commands.arguments.selector.options;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.WrappedMinMaxBounds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class EntitySelectorOptions {
    private static final Map<String, EntitySelectorOptions.Option> OPTIONS = Maps.newHashMap();
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType((option) -> {
        return new TranslatableComponent("argument.entity.options.unknown", option);
    });
    public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType((option) -> {
        return new TranslatableComponent("argument.entity.options.inapplicable", option);
    });
    public static final SimpleCommandExceptionType ERROR_RANGE_NEGATIVE = new SimpleCommandExceptionType(new TranslatableComponent("argument.entity.options.distance.negative"));
    public static final SimpleCommandExceptionType ERROR_LEVEL_NEGATIVE = new SimpleCommandExceptionType(new TranslatableComponent("argument.entity.options.level.negative"));
    public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL = new SimpleCommandExceptionType(new TranslatableComponent("argument.entity.options.limit.toosmall"));
    public static final DynamicCommandExceptionType ERROR_SORT_UNKNOWN = new DynamicCommandExceptionType((sortType) -> {
        return new TranslatableComponent("argument.entity.options.sort.irreversible", sortType);
    });
    public static final DynamicCommandExceptionType ERROR_GAME_MODE_INVALID = new DynamicCommandExceptionType((gameMode) -> {
        return new TranslatableComponent("argument.entity.options.mode.invalid", gameMode);
    });
    public static final DynamicCommandExceptionType ERROR_ENTITY_TYPE_INVALID = new DynamicCommandExceptionType((entity) -> {
        return new TranslatableComponent("argument.entity.options.type.invalid", entity);
    });
    // Paper start
    public static final DynamicCommandExceptionType ERROR_ENTITY_TAG_INVALID = new DynamicCommandExceptionType((object) -> {
        return io.papermc.paper.adventure.PaperAdventure
            .asVanilla(net.kyori.adventure.text.Component
                .text("Invalid or unknown entity type tag '" + object + "'")
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                    .showText(net.kyori.adventure.text.Component
                        .text("You can disable this error in 'paper.yml'")
                    )
                )
            );
    });
    // Paper end

    private static void register(String id, EntitySelectorOptions.Modifier handler, Predicate<EntitySelectorParser> condition, Component description) {
        OPTIONS.put(id, new EntitySelectorOptions.Option(handler, condition, description));
    }

    public static void bootStrap() {
        if (OPTIONS.isEmpty()) {
            register("name", (reader) -> {
                int i = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readString();
                if (reader.hasNameNotEquals() && !bl) {
                    reader.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader.getReader(), "name");
                } else {
                    if (bl) {
                        reader.setHasNameNotEquals(true);
                    } else {
                        reader.setHasNameEquals(true);
                    }

                    reader.addPredicate((readerx) -> {
                        return readerx.getName().getString().equals(string) != bl;
                    });
                }
            }, (reader) -> {
                return !reader.hasNameEquals();
            }, new TranslatableComponent("argument.entity.options.name.description"));
            register("distance", (reader) -> {
                int i = reader.getReader().getCursor();
                MinMaxBounds.Doubles doubles = MinMaxBounds.Doubles.fromReader(reader.getReader());
                if ((doubles.getMin() == null || !(doubles.getMin() < 0.0D)) && (doubles.getMax() == null || !(doubles.getMax() < 0.0D))) {
                    reader.setDistance(doubles);
                    reader.setWorldLimited();
                } else {
                    reader.getReader().setCursor(i);
                    throw ERROR_RANGE_NEGATIVE.createWithContext(reader.getReader());
                }
            }, (reader) -> {
                return reader.getDistance().isAny();
            }, new TranslatableComponent("argument.entity.options.distance.description"));
            register("level", (reader) -> {
                int i = reader.getReader().getCursor();
                MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromReader(reader.getReader());
                if ((ints.getMin() == null || ints.getMin() >= 0) && (ints.getMax() == null || ints.getMax() >= 0)) {
                    reader.setLevel(ints);
                    reader.setIncludesEntities(false);
                } else {
                    reader.getReader().setCursor(i);
                    throw ERROR_LEVEL_NEGATIVE.createWithContext(reader.getReader());
                }
            }, (reader) -> {
                return reader.getLevel().isAny();
            }, new TranslatableComponent("argument.entity.options.level.description"));
            register("x", (reader) -> {
                reader.setWorldLimited();
                reader.setX(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getX() == null;
            }, new TranslatableComponent("argument.entity.options.x.description"));
            register("y", (reader) -> {
                reader.setWorldLimited();
                reader.setY(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getY() == null;
            }, new TranslatableComponent("argument.entity.options.y.description"));
            register("z", (reader) -> {
                reader.setWorldLimited();
                reader.setZ(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getZ() == null;
            }, new TranslatableComponent("argument.entity.options.z.description"));
            register("dx", (reader) -> {
                reader.setWorldLimited();
                reader.setDeltaX(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getDeltaX() == null;
            }, new TranslatableComponent("argument.entity.options.dx.description"));
            register("dy", (reader) -> {
                reader.setWorldLimited();
                reader.setDeltaY(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getDeltaY() == null;
            }, new TranslatableComponent("argument.entity.options.dy.description"));
            register("dz", (reader) -> {
                reader.setWorldLimited();
                reader.setDeltaZ(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getDeltaZ() == null;
            }, new TranslatableComponent("argument.entity.options.dz.description"));
            register("x_rotation", (reader) -> {
                reader.setRotX(WrappedMinMaxBounds.fromReader(reader.getReader(), true, Mth::wrapDegrees));
            }, (reader) -> {
                return reader.getRotX() == WrappedMinMaxBounds.ANY;
            }, new TranslatableComponent("argument.entity.options.x_rotation.description"));
            register("y_rotation", (reader) -> {
                reader.setRotY(WrappedMinMaxBounds.fromReader(reader.getReader(), true, Mth::wrapDegrees));
            }, (reader) -> {
                return reader.getRotY() == WrappedMinMaxBounds.ANY;
            }, new TranslatableComponent("argument.entity.options.y_rotation.description"));
            register("limit", (reader) -> {
                int i = reader.getReader().getCursor();
                int j = reader.getReader().readInt();
                if (j < 1) {
                    reader.getReader().setCursor(i);
                    throw ERROR_LIMIT_TOO_SMALL.createWithContext(reader.getReader());
                } else {
                    reader.setMaxResults(j);
                    reader.setLimited(true);
                }
            }, (reader) -> {
                return !reader.isCurrentEntity() && !reader.isLimited();
            }, new TranslatableComponent("argument.entity.options.limit.description"));
            register("sort", (reader) -> {
                int i = reader.getReader().getCursor();
                String string = reader.getReader().readUnquotedString();
                reader.setSuggestions((builder, consumer) -> {
                    return SharedSuggestionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), builder);
                });
                BiConsumer<Vec3, List<? extends Entity>> biConsumer;
                switch(string) {
                case "nearest":
                    biConsumer = EntitySelectorParser.ORDER_NEAREST;
                    break;
                case "furthest":
                    biConsumer = EntitySelectorParser.ORDER_FURTHEST;
                    break;
                case "random":
                    biConsumer = EntitySelectorParser.ORDER_RANDOM;
                    break;
                case "arbitrary":
                    biConsumer = EntitySelectorParser.ORDER_ARBITRARY;
                    break;
                default:
                    reader.getReader().setCursor(i);
                    throw ERROR_SORT_UNKNOWN.createWithContext(reader.getReader(), string);
                }

                reader.setOrder(biConsumer);
                reader.setSorted(true);
            }, (reader) -> {
                return !reader.isCurrentEntity() && !reader.isSorted();
            }, new TranslatableComponent("argument.entity.options.sort.description"));
            register("gamemode", (reader) -> {
                reader.setSuggestions((builder, consumer) -> {
                    String string = builder.getRemaining().toLowerCase(Locale.ROOT);
                    boolean bl = !reader.hasGamemodeNotEquals();
                    boolean bl2 = true;
                    if (!string.isEmpty()) {
                        if (string.charAt(0) == '!') {
                            bl = false;
                            string = string.substring(1);
                        } else {
                            bl2 = false;
                        }
                    }

                    for(GameType gameType : GameType.values()) {
                        if (gameType.getName().toLowerCase(Locale.ROOT).startsWith(string)) {
                            if (bl2) {
                                builder.suggest("!" + gameType.getName());
                            }

                            if (bl) {
                                builder.suggest(gameType.getName());
                            }
                        }
                    }

                    return builder.buildFuture();
                });
                int i = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                if (reader.hasGamemodeNotEquals() && !bl) {
                    reader.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader.getReader(), "gamemode");
                } else {
                    String string = reader.getReader().readUnquotedString();
                    GameType gameType = GameType.byName(string, (GameType)null);
                    if (gameType == null) {
                        reader.getReader().setCursor(i);
                        throw ERROR_GAME_MODE_INVALID.createWithContext(reader.getReader(), string);
                    } else {
                        reader.setIncludesEntities(false);
                        reader.addPredicate((entity) -> {
                            if (!(entity instanceof ServerPlayer)) {
                                return false;
                            } else {
                                GameType gameType2 = ((ServerPlayer)entity).gameMode.getGameModeForPlayer();
                                return bl ? gameType2 != gameType : gameType2 == gameType;
                            }
                        });
                        if (bl) {
                            reader.setHasGamemodeNotEquals(true);
                        } else {
                            reader.setHasGamemodeEquals(true);
                        }

                    }
                }
            }, (reader) -> {
                return !reader.hasGamemodeEquals();
            }, new TranslatableComponent("argument.entity.options.gamemode.description"));
            register("team", (reader) -> {
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readUnquotedString();
                reader.addPredicate((entity) -> {
                    if (!(entity instanceof LivingEntity)) {
                        return false;
                    } else {
                        Team team = entity.getTeam();
                        String string2 = team == null ? "" : team.getName();
                        return string2.equals(string) != bl;
                    }
                });
                if (bl) {
                    reader.setHasTeamNotEquals(true);
                } else {
                    reader.setHasTeamEquals(true);
                }

            }, (reader) -> {
                return !reader.hasTeamEquals();
            }, new TranslatableComponent("argument.entity.options.team.description"));
            register("type", (reader) -> {
                reader.setSuggestions((builder, consumer) -> {
                    SharedSuggestionProvider.suggestResource(Registry.ENTITY_TYPE.keySet(), builder, String.valueOf('!'));
                    SharedSuggestionProvider.suggestResource(Registry.ENTITY_TYPE.getTagNames().map(TagKey::location), builder, "!#");
                    if (!reader.isTypeLimitedInversely()) {
                        SharedSuggestionProvider.suggestResource(Registry.ENTITY_TYPE.keySet(), builder);
                        SharedSuggestionProvider.suggestResource(Registry.ENTITY_TYPE.getTagNames().map(TagKey::location), builder, String.valueOf('#'));
                    }

                    return builder.buildFuture();
                });
                int i = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                if (reader.isTypeLimitedInversely() && !bl) {
                    reader.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader.getReader(), "type");
                } else {
                    if (bl) {
                        reader.setTypeLimitedInversely();
                    }

                    if (reader.isTag()) {
                        TagKey<EntityType<?>> tagKey = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, ResourceLocation.read(reader.getReader()));
                        // Paper start - throw error if invalid entity tag (only on suggestions to keep cmd success behavior)
                        if (com.destroystokyo.paper.PaperConfig.fixTargetSelectorTagCompletion && reader.parsingEntityArgumentSuggestions && !Registry.ENTITY_TYPE.isKnownTagName(tagKey)) {
                            reader.getReader().setCursor(i);
                            throw ERROR_ENTITY_TAG_INVALID.createWithContext(reader.getReader(), tagKey);
                        }
                        // Paper end
                        reader.addPredicate((entity) -> {
                            return entity.getType().is(tagKey) != bl;
                        });
                    } else {
                        ResourceLocation resourceLocation = ResourceLocation.read(reader.getReader());
                        EntityType<?> entityType = Registry.ENTITY_TYPE.getOptional(resourceLocation).orElseThrow(() -> {
                            reader.getReader().setCursor(i);
                            return ERROR_ENTITY_TYPE_INVALID.createWithContext(reader.getReader(), resourceLocation.toString());
                        });
                        if (Objects.equals(EntityType.PLAYER, entityType) && !bl) {
                            reader.setIncludesEntities(false);
                        }

                        reader.addPredicate((entity) -> {
                            return Objects.equals(entityType, entity.getType()) != bl;
                        });
                        if (!bl) {
                            reader.limitToType(entityType);
                        }
                    }

                }
            }, (reader) -> {
                return !reader.isTypeLimited();
            }, new TranslatableComponent("argument.entity.options.type.description"));
            register("tag", (reader) -> {
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readUnquotedString();
                reader.addPredicate((entity) -> {
                    if ("".equals(string)) {
                        return entity.getTags().isEmpty() != bl;
                    } else {
                        return entity.getTags().contains(string) != bl;
                    }
                });
            }, (reader) -> {
                return true;
            }, new TranslatableComponent("argument.entity.options.tag.description"));
            register("nbt", (reader) -> {
                boolean bl = reader.shouldInvertValue();
                CompoundTag compoundTag = (new TagParser(reader.getReader())).readStruct();
                reader.addPredicate((entity) -> {
                    CompoundTag compoundTag2 = entity.saveWithoutId(new CompoundTag());
                    if (entity instanceof ServerPlayer) {
                        ItemStack itemStack = ((ServerPlayer)entity).getInventory().getSelected();
                        if (!itemStack.isEmpty()) {
                            compoundTag2.put("SelectedItem", itemStack.save(new CompoundTag()));
                        }
                    }

                    return NbtUtils.compareNbt(compoundTag, compoundTag2, true) != bl;
                });
            }, (reader) -> {
                return true;
            }, new TranslatableComponent("argument.entity.options.nbt.description"));
            register("scores", (reader) -> {
                StringReader stringReader = reader.getReader();
                Map<String, MinMaxBounds.Ints> map = Maps.newHashMap();
                stringReader.expect('{');
                stringReader.skipWhitespace();

                while(stringReader.canRead() && stringReader.peek() != '}') {
                    stringReader.skipWhitespace();
                    String string = stringReader.readUnquotedString();
                    stringReader.skipWhitespace();
                    stringReader.expect('=');
                    stringReader.skipWhitespace();
                    MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromReader(stringReader);
                    map.put(string, ints);
                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == ',') {
                        stringReader.skip();
                    }
                }

                stringReader.expect('}');
                if (!map.isEmpty()) {
                    reader.addPredicate((entity) -> {
                        Scoreboard scoreboard = entity.getServer().getScoreboard();
                        String string = entity.getScoreboardName();

                        for(Entry<String, MinMaxBounds.Ints> entry : map.entrySet()) {
                            Objective objective = scoreboard.getObjective(entry.getKey());
                            if (objective == null) {
                                return false;
                            }

                            if (!scoreboard.hasPlayerScore(string, objective)) {
                                return false;
                            }

                            Score score = scoreboard.getOrCreatePlayerScore(string, objective);
                            int i = score.getScore();
                            if (!entry.getValue().matches(i)) {
                                return false;
                            }
                        }

                        return true;
                    });
                }

                reader.setHasScores(true);
            }, (reader) -> {
                return !reader.hasScores();
            }, new TranslatableComponent("argument.entity.options.scores.description"));
            register("advancements", (reader) -> {
                StringReader stringReader = reader.getReader();
                Map<ResourceLocation, Predicate<AdvancementProgress>> map = Maps.newHashMap();
                stringReader.expect('{');
                stringReader.skipWhitespace();

                while(stringReader.canRead() && stringReader.peek() != '}') {
                    stringReader.skipWhitespace();
                    ResourceLocation resourceLocation = ResourceLocation.read(stringReader);
                    stringReader.skipWhitespace();
                    stringReader.expect('=');
                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == '{') {
                        Map<String, Predicate<CriterionProgress>> map2 = Maps.newHashMap();
                        stringReader.skipWhitespace();
                        stringReader.expect('{');
                        stringReader.skipWhitespace();

                        while(stringReader.canRead() && stringReader.peek() != '}') {
                            stringReader.skipWhitespace();
                            String string = stringReader.readUnquotedString();
                            stringReader.skipWhitespace();
                            stringReader.expect('=');
                            stringReader.skipWhitespace();
                            boolean bl = stringReader.readBoolean();
                            map2.put(string, (criterionProgress) -> {
                                return criterionProgress.isDone() == bl;
                            });
                            stringReader.skipWhitespace();
                            if (stringReader.canRead() && stringReader.peek() == ',') {
                                stringReader.skip();
                            }
                        }

                        stringReader.skipWhitespace();
                        stringReader.expect('}');
                        stringReader.skipWhitespace();
                        map.put(resourceLocation, (advancementProgress) -> {
                            for(Entry<String, Predicate<CriterionProgress>> entry : map2.entrySet()) {
                                CriterionProgress criterionProgress = advancementProgress.getCriterion(entry.getKey());
                                if (criterionProgress == null || !entry.getValue().test(criterionProgress)) {
                                    return false;
                                }
                            }

                            return true;
                        });
                    } else {
                        boolean bl2 = stringReader.readBoolean();
                        map.put(resourceLocation, (advancementProgress) -> {
                            return advancementProgress.isDone() == bl2;
                        });
                    }

                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == ',') {
                        stringReader.skip();
                    }
                }

                stringReader.expect('}');
                if (!map.isEmpty()) {
                    reader.addPredicate((entity) -> {
                        if (!(entity instanceof ServerPlayer)) {
                            return false;
                        } else {
                            ServerPlayer serverPlayer = (ServerPlayer)entity;
                            PlayerAdvancements playerAdvancements = serverPlayer.getAdvancements();
                            ServerAdvancementManager serverAdvancementManager = serverPlayer.getServer().getAdvancements();

                            for(Entry<ResourceLocation, Predicate<AdvancementProgress>> entry : map.entrySet()) {
                                Advancement advancement = serverAdvancementManager.getAdvancement(entry.getKey());
                                if (advancement == null || !entry.getValue().test(playerAdvancements.getOrStartProgress(advancement))) {
                                    return false;
                                }
                            }

                            return true;
                        }
                    });
                    reader.setIncludesEntities(false);
                }

                reader.setHasAdvancements(true);
            }, (reader) -> {
                return !reader.hasAdvancements();
            }, new TranslatableComponent("argument.entity.options.advancements.description"));
            register("predicate", (reader) -> {
                boolean bl = reader.shouldInvertValue();
                ResourceLocation resourceLocation = ResourceLocation.read(reader.getReader());
                reader.addPredicate((entity) -> {
                    if (!(entity.level instanceof ServerLevel)) {
                        return false;
                    } else {
                        ServerLevel serverLevel = (ServerLevel)entity.level;
                        LootItemCondition lootItemCondition = serverLevel.getServer().getPredicateManager().get(resourceLocation);
                        if (lootItemCondition == null) {
                            return false;
                        } else {
                            LootContext lootContext = (new LootContext.Builder(serverLevel)).withParameter(LootContextParams.THIS_ENTITY, entity).withParameter(LootContextParams.ORIGIN, entity.position()).create(LootContextParamSets.SELECTOR);
                            return bl ^ lootItemCondition.test(lootContext);
                        }
                    }
                });
            }, (reader) -> {
                return true;
            }, new TranslatableComponent("argument.entity.options.predicate.description"));
        }
    }

    public static EntitySelectorOptions.Modifier get(EntitySelectorParser reader, String option, int restoreCursor) throws CommandSyntaxException {
        EntitySelectorOptions.Option option2 = OPTIONS.get(option);
        if (option2 != null) {
            if (option2.predicate.test(reader)) {
                return option2.modifier;
            } else {
                throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader.getReader(), option);
            }
        } else {
            reader.getReader().setCursor(restoreCursor);
            throw ERROR_UNKNOWN_OPTION.createWithContext(reader.getReader(), option);
        }
    }

    public static void suggestNames(EntitySelectorParser reader, SuggestionsBuilder suggestionBuilder) {
        String string = suggestionBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for(Entry<String, EntitySelectorOptions.Option> entry : OPTIONS.entrySet()) {
            if ((entry.getValue()).predicate.test(reader) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(string)) {
                suggestionBuilder.suggest((String)entry.getKey() + "=", (entry.getValue()).description);
            }
        }

    }

    public interface Modifier {
        void handle(EntitySelectorParser reader) throws CommandSyntaxException;
    }

    static class Option {
        public final EntitySelectorOptions.Modifier modifier;
        public final Predicate<EntitySelectorParser> predicate;
        public final Component description;

        Option(EntitySelectorOptions.Modifier handler, Predicate<EntitySelectorParser> condition, Component description) {
            this.modifier = handler;
            this.predicate = condition;
            this.description = description;
        }
    }
}
