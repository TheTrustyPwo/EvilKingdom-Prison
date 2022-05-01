package net.minecraft.nbt.visitors;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.TagType;

public record FieldTree(int depth, Map<String, TagType<?>> selectedFields, Map<String, FieldTree> fieldsToRecurse) {
    private FieldTree(int depth) {
        this(depth, new HashMap<>(), new HashMap<>());
    }

    public static FieldTree createRoot() {
        return new FieldTree(1);
    }

    public void addEntry(FieldSelector query) {
        if (this.depth <= query.path().size()) {
            this.fieldsToRecurse.computeIfAbsent(query.path().get(this.depth - 1), (path) -> {
                return new FieldTree(this.depth + 1);
            }).addEntry(query);
        } else {
            this.selectedFields.put(query.name(), query.type());
        }

    }

    public boolean isSelected(TagType<?> type, String key) {
        return type.equals(this.selectedFields().get(key));
    }
}
