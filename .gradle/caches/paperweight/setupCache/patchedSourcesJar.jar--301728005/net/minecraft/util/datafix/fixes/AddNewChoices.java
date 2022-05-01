package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;

public class AddNewChoices extends DataFix {
    private final String name;
    private final TypeReference type;

    public AddNewChoices(Schema outputSchema, String name, TypeReference types) {
        super(outputSchema, true);
        this.name = name;
        this.type = types;
    }

    public TypeRewriteRule makeRule() {
        TaggedChoiceType<?> taggedChoiceType = this.getInputSchema().findChoiceType(this.type);
        TaggedChoiceType<?> taggedChoiceType2 = this.getOutputSchema().findChoiceType(this.type);
        return this.cap(this.name, taggedChoiceType, taggedChoiceType2);
    }

    protected final <K> TypeRewriteRule cap(String name, TaggedChoiceType<K> inputChoiceType, TaggedChoiceType<?> outputChoiceType) {
        if (inputChoiceType.getKeyType() != outputChoiceType.getKeyType()) {
            throw new IllegalStateException("Could not inject: key type is not the same");
        } else {
            return this.fixTypeEverywhere(name, inputChoiceType, outputChoiceType, (dynamicOps) -> {
                return (pair) -> {
                    if (!outputChoiceType.hasType(pair.getFirst())) {
                        throw new IllegalArgumentException(String.format("Unknown type %s in %s ", pair.getFirst(), this.type));
                    } else {
                        return pair;
                    }
                };
            });
        }
    }
}
