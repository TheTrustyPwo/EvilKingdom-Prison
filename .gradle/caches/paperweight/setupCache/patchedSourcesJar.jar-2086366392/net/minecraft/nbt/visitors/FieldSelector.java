package net.minecraft.nbt.visitors;

import java.util.List;
import net.minecraft.nbt.TagType;

public record FieldSelector(List<String> path, TagType<?> type, String name) {
    public FieldSelector(TagType<?> type, String key) {
        this(List.of(), type, key);
    }

    public FieldSelector(String path, TagType<?> type, String key) {
        this(List.of(path), type, key);
    }

    public FieldSelector(String path1, String path2, TagType<?> type, String key) {
        this(List.of(path1, path2), type, key);
    }
}
