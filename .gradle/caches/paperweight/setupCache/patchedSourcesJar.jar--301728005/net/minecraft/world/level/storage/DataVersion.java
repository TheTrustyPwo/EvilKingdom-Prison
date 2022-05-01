package net.minecraft.world.level.storage;

public class DataVersion {
    private final int version;
    private final String series;
    public static String MAIN_SERIES = "main";

    public DataVersion(int id) {
        this(id, MAIN_SERIES);
    }

    public DataVersion(int id, String series) {
        this.version = id;
        this.series = series;
    }

    public boolean isSideSeries() {
        return !this.series.equals(MAIN_SERIES);
    }

    public String getSeries() {
        return this.series;
    }

    public int getVersion() {
        return this.version;
    }

    public boolean isCompatible(DataVersion other) {
        return this.getSeries().equals(other.getSeries());
    }
}
