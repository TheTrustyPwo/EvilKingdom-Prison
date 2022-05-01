package net.minecraft.util.profiling;

public final class ResultField implements Comparable<ResultField> {
    public final double percentage;
    public final double globalPercentage;
    public final long count;
    public final String name;

    public ResultField(String name, double parentUsagePercentage, double totalUsagePercentage, long visitCount) {
        this.name = name;
        this.percentage = parentUsagePercentage;
        this.globalPercentage = totalUsagePercentage;
        this.count = visitCount;
    }

    @Override
    public int compareTo(ResultField resultField) {
        if (resultField.percentage < this.percentage) {
            return -1;
        } else {
            return resultField.percentage > this.percentage ? 1 : resultField.name.compareTo(this.name);
        }
    }

    public int getColor() {
        return (this.name.hashCode() & 11184810) + 4473924;
    }
}
