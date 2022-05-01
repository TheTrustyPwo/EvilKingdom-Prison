package net.minecraft.world.level;

import com.mojang.serialization.Dynamic;
import net.minecraft.world.Difficulty;

public final class LevelSettings {
    public String levelName;
    private final GameType gameType;
    public boolean hardcore;
    private final Difficulty difficulty;
    private final boolean allowCommands;
    private final GameRules gameRules;
    private final DataPackConfig dataPackConfig;

    public LevelSettings(String name, GameType gameMode, boolean hardcore, Difficulty difficulty, boolean allowCommands, GameRules gameRules, DataPackConfig dataPackSettings) {
        this.levelName = name;
        this.gameType = gameMode;
        this.hardcore = hardcore;
        this.difficulty = difficulty;
        this.allowCommands = allowCommands;
        this.gameRules = gameRules;
        this.dataPackConfig = dataPackSettings;
    }

    public static LevelSettings parse(Dynamic<?> dynamic, DataPackConfig dataPackSettings) {
        GameType gameType = GameType.byId(dynamic.get("GameType").asInt(0));
        return new LevelSettings(dynamic.get("LevelName").asString(""), gameType, dynamic.get("hardcore").asBoolean(false), dynamic.get("Difficulty").asNumber().map((number) -> {
            return Difficulty.byId(number.byteValue());
        }).result().orElse(Difficulty.NORMAL), dynamic.get("allowCommands").asBoolean(gameType == GameType.CREATIVE), new GameRules(dynamic.get("GameRules")), dataPackSettings);
    }

    public String levelName() {
        return this.levelName;
    }

    public GameType gameType() {
        return this.gameType;
    }

    public boolean hardcore() {
        return this.hardcore;
    }

    public Difficulty difficulty() {
        return this.difficulty;
    }

    public boolean allowCommands() {
        return this.allowCommands;
    }

    public GameRules gameRules() {
        return this.gameRules;
    }

    public DataPackConfig getDataPackConfig() {
        return this.dataPackConfig;
    }

    public LevelSettings withGameType(GameType mode) {
        return new LevelSettings(this.levelName, mode, this.hardcore, this.difficulty, this.allowCommands, this.gameRules, this.dataPackConfig);
    }

    public LevelSettings withDifficulty(Difficulty difficulty) {
        return new LevelSettings(this.levelName, this.gameType, this.hardcore, difficulty, this.allowCommands, this.gameRules, this.dataPackConfig);
    }

    public LevelSettings withDataPackConfig(DataPackConfig dataPackSettings) {
        return new LevelSettings(this.levelName, this.gameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules, dataPackSettings);
    }

    public LevelSettings copy() {
        return new LevelSettings(this.levelName, this.gameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules.copy(), this.dataPackConfig);
    }
}
