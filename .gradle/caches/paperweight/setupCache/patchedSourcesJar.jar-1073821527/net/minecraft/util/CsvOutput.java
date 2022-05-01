package net.minecraft.util;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringEscapeUtils;

public class CsvOutput {
    private static final String LINE_SEPARATOR = "\r\n";
    private static final String FIELD_SEPARATOR = ",";
    private final Writer output;
    private final int columnCount;

    CsvOutput(Writer writer, List<String> columns) throws IOException {
        this.output = writer;
        this.columnCount = columns.size();
        this.writeLine(columns.stream());
    }

    public static CsvOutput.Builder builder() {
        return new CsvOutput.Builder();
    }

    public void writeRow(Object... columns) throws IOException {
        if (columns.length != this.columnCount) {
            throw new IllegalArgumentException("Invalid number of columns, expected " + this.columnCount + ", but got " + columns.length);
        } else {
            this.writeLine(Stream.of(columns));
        }
    }

    private void writeLine(Stream<?> columns) throws IOException {
        this.output.write((String)columns.map(CsvOutput::getStringValue).collect(Collectors.joining(",")) + "\r\n");
    }

    private static String getStringValue(@Nullable Object o) {
        return StringEscapeUtils.escapeCsv(o != null ? o.toString() : "[null]");
    }

    public static class Builder {
        private final List<String> headers = Lists.newArrayList();

        public CsvOutput.Builder addColumn(String name) {
            this.headers.add(name);
            return this;
        }

        public CsvOutput build(Writer writer) throws IOException {
            return new CsvOutput(writer, this.headers);
        }
    }
}
