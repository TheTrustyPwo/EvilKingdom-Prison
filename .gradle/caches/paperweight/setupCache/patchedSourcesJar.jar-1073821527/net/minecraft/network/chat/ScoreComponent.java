package net.minecraft.network.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class ScoreComponent extends BaseComponent implements ContextAwareComponent {
    private static final String SCORER_PLACEHOLDER = "*";
    private final String name;
    @Nullable
    private final EntitySelector selector;
    private final String objective;

    @Nullable
    private static EntitySelector parseSelector(String name) {
        try {
            return (new EntitySelectorParser(new StringReader(name))).parse();
        } catch (CommandSyntaxException var2) {
            return null;
        }
    }

    public ScoreComponent(String name, String objective) {
        this(name, parseSelector(name), objective);
    }

    private ScoreComponent(String name, @Nullable EntitySelector selector, String objective) {
        this.name = name;
        this.selector = selector;
        this.objective = objective;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public EntitySelector getSelector() {
        return this.selector;
    }

    public String getObjective() {
        return this.objective;
    }

    private String findTargetName(CommandSourceStack source) throws CommandSyntaxException {
        if (this.selector != null) {
            List<? extends Entity> list = this.selector.findEntities(source);
            if (!list.isEmpty()) {
                if (list.size() != 1) {
                    throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
                }

                return list.get(0).getScoreboardName();
            }
        }

        return this.name;
    }

    private String getScore(String playerName, CommandSourceStack source) {
        MinecraftServer minecraftServer = source.getServer();
        if (minecraftServer != null) {
            Scoreboard scoreboard = minecraftServer.getScoreboard();
            Objective objective = scoreboard.getObjective(this.objective);
            if (scoreboard.hasPlayerScore(playerName, objective)) {
                Score score = scoreboard.getOrCreatePlayerScore(playerName, objective);
                return Integer.toString(score.getScore());
            }
        }

        return "";
    }

    @Override
    public ScoreComponent plainCopy() {
        return new ScoreComponent(this.name, this.selector, this.objective);
    }

    @Override
    public MutableComponent resolve(@Nullable CommandSourceStack source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        if (source == null) {
            return new TextComponent("");
        } else {
            String string = this.findTargetName(source);
            String string2 = sender != null && string.equals("*") ? sender.getScoreboardName() : string;
            return new TextComponent(this.getScore(string2, source));
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ScoreComponent)) {
            return false;
        } else {
            ScoreComponent scoreComponent = (ScoreComponent)object;
            return this.name.equals(scoreComponent.name) && this.objective.equals(scoreComponent.objective) && super.equals(object);
        }
    }

    @Override
    public String toString() {
        return "ScoreComponent{name='" + this.name + "'objective='" + this.objective + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
    }
}
