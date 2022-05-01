package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.StringUtils;

public class ItemWrittenBookPagesStrictJsonFix extends DataFix {
    public ItemWrittenBookPagesStrictJsonFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        return dynamic.update("pages", (dynamic2) -> {
            return DataFixUtils.orElse(dynamic2.asStreamOpt().map((stream) -> {
                return stream.map((dynamic) -> {
                    if (!dynamic.asString().result().isPresent()) {
                        return dynamic;
                    } else {
                        String string = dynamic.asString("");
                        Component component = null;
                        if (!"null".equals(string) && !StringUtils.isEmpty(string)) {
                            if (string.charAt(0) == '"' && string.charAt(string.length() - 1) == '"' || string.charAt(0) == '{' && string.charAt(string.length() - 1) == '}') {
                                try {
                                    component = GsonHelper.fromJson(BlockEntitySignTextStrictJsonFix.GSON, string, Component.class, true);
                                    if (component == null) {
                                        component = TextComponent.EMPTY;
                                    }
                                } catch (Exception var6) {
                                }

                                if (component == null) {
                                    try {
                                        component = Component.Serializer.fromJson(string);
                                    } catch (Exception var5) {
                                    }
                                }

                                if (component == null) {
                                    try {
                                        component = Component.Serializer.fromJsonLenient(string);
                                    } catch (Exception var4) {
                                    }
                                }

                                if (component == null) {
                                    component = new TextComponent(string);
                                }
                            } else {
                                component = new TextComponent(string);
                            }
                        } else {
                            component = TextComponent.EMPTY;
                        }

                        return dynamic.createString(Component.Serializer.toJson(component));
                    }
                });
            }).map(dynamic::createList).result(), dynamic.emptyList());
        });
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<?> opticFinder = type.findField("tag");
        return this.fixTypeEverywhereTyped("ItemWrittenBookPagesStrictJsonFix", type, (typed) -> {
            return typed.updateTyped(opticFinder, (typedx) -> {
                return typedx.update(DSL.remainderFinder(), this::fixTag);
            });
        });
    }
}
