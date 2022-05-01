package net.minecraft.network.chat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class Style {
    public static final Style EMPTY = new Style((TextColor)null, (Boolean)null, (Boolean)null, (Boolean)null, (Boolean)null, (Boolean)null, (ClickEvent)null, (HoverEvent)null, (String)null, (ResourceLocation)null);
    public static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");
    @Nullable
    final TextColor color;
    @Nullable
    final Boolean bold;
    @Nullable
    final Boolean italic;
    @Nullable
    final Boolean underlined;
    @Nullable
    final Boolean strikethrough;
    @Nullable
    final Boolean obfuscated;
    @Nullable
    final ClickEvent clickEvent;
    @Nullable
    final HoverEvent hoverEvent;
    @Nullable
    final String insertion;
    @Nullable
    final ResourceLocation font;

    Style(@Nullable TextColor color, @Nullable Boolean bold, @Nullable Boolean italic, @Nullable Boolean underlined, @Nullable Boolean strikethrough, @Nullable Boolean obfuscated, @Nullable ClickEvent clickEvent, @Nullable HoverEvent hoverEvent, @Nullable String insertion, @Nullable ResourceLocation font) {
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
        this.insertion = insertion;
        this.font = font;
    }

    @Nullable
    public TextColor getColor() {
        return this.color;
    }

    public boolean isBold() {
        return this.bold == Boolean.TRUE;
    }

    public boolean isItalic() {
        return this.italic == Boolean.TRUE;
    }

    public boolean isStrikethrough() {
        return this.strikethrough == Boolean.TRUE;
    }

    public boolean isUnderlined() {
        return this.underlined == Boolean.TRUE;
    }

    public boolean isObfuscated() {
        return this.obfuscated == Boolean.TRUE;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    @Nullable
    public ClickEvent getClickEvent() {
        return this.clickEvent;
    }

    @Nullable
    public HoverEvent getHoverEvent() {
        return this.hoverEvent;
    }

    @Nullable
    public String getInsertion() {
        return this.insertion;
    }

    public ResourceLocation getFont() {
        return this.font != null ? this.font : DEFAULT_FONT;
    }

    public Style withColor(@Nullable TextColor color) {
        return new Style(color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style withColor(@Nullable ChatFormatting color) {
        return this.withColor(color != null ? TextColor.fromLegacyFormat(color) : null);
    }

    public Style withColor(int rgbColor) {
        return this.withColor(TextColor.fromRgb(rgbColor));
    }

    public Style withBold(@Nullable Boolean bold) {
        return new Style(this.color, bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style withItalic(@Nullable Boolean italic) {
        return new Style(this.color, this.bold, italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style withUnderlined(@Nullable Boolean underline) {
        return new Style(this.color, this.bold, this.italic, underline, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style withStrikethrough(@Nullable Boolean strikethrough) {
        return new Style(this.color, this.bold, this.italic, this.underlined, strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style withObfuscated(@Nullable Boolean obfuscated) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style withClickEvent(@Nullable ClickEvent clickEvent) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style withHoverEvent(@Nullable HoverEvent hoverEvent) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, hoverEvent, this.insertion, this.font);
    }

    public Style withInsertion(@Nullable String insertion) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, insertion, this.font);
    }

    public Style withFont(@Nullable ResourceLocation font) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, font);
    }

    public Style applyFormat(ChatFormatting formatting) {
        TextColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;
        switch(formatting) {
        case OBFUSCATED:
            boolean5 = true;
            break;
        case BOLD:
            boolean_ = true;
            break;
        case STRIKETHROUGH:
            boolean3 = true;
            break;
        case UNDERLINE:
            boolean4 = true;
            break;
        case ITALIC:
            boolean2 = true;
            break;
        case RESET:
            return EMPTY;
        default:
            textColor = TextColor.fromLegacyFormat(formatting);
        }

        return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style applyLegacyFormat(ChatFormatting formatting) {
        TextColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;
        switch(formatting) {
        case OBFUSCATED:
            boolean5 = true;
            break;
        case BOLD:
            boolean_ = true;
            break;
        case STRIKETHROUGH:
            boolean3 = true;
            break;
        case UNDERLINE:
            boolean4 = true;
            break;
        case ITALIC:
            boolean2 = true;
            break;
        case RESET:
            return EMPTY;
        default:
            boolean5 = false;
            boolean_ = false;
            boolean3 = false;
            boolean4 = false;
            boolean2 = false;
            textColor = TextColor.fromLegacyFormat(formatting);
        }

        return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style applyFormats(ChatFormatting... formattings) {
        TextColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;

        for(ChatFormatting chatFormatting : formattings) {
            switch(chatFormatting) {
            case OBFUSCATED:
                boolean5 = true;
                break;
            case BOLD:
                boolean_ = true;
                break;
            case STRIKETHROUGH:
                boolean3 = true;
                break;
            case UNDERLINE:
                boolean4 = true;
                break;
            case ITALIC:
                boolean2 = true;
                break;
            case RESET:
                return EMPTY;
            default:
                textColor = TextColor.fromLegacyFormat(chatFormatting);
            }
        }

        return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style applyTo(Style parent) {
        if (this == EMPTY) {
            return parent;
        } else {
            return parent == EMPTY ? this : new Style(this.color != null ? this.color : parent.color, this.bold != null ? this.bold : parent.bold, this.italic != null ? this.italic : parent.italic, this.underlined != null ? this.underlined : parent.underlined, this.strikethrough != null ? this.strikethrough : parent.strikethrough, this.obfuscated != null ? this.obfuscated : parent.obfuscated, this.clickEvent != null ? this.clickEvent : parent.clickEvent, this.hoverEvent != null ? this.hoverEvent : parent.hoverEvent, this.insertion != null ? this.insertion : parent.insertion, this.font != null ? this.font : parent.font);
        }
    }

    @Override
    public String toString() {
        return "Style{ color=" + this.color + ", bold=" + this.bold + ", italic=" + this.italic + ", underlined=" + this.underlined + ", strikethrough=" + this.strikethrough + ", obfuscated=" + this.obfuscated + ", clickEvent=" + this.getClickEvent() + ", hoverEvent=" + this.getHoverEvent() + ", insertion=" + this.getInsertion() + ", font=" + this.getFont() + "}";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Style)) {
            return false;
        } else {
            Style style = (Style)object;
            return this.isBold() == style.isBold() && Objects.equals(this.getColor(), style.getColor()) && this.isItalic() == style.isItalic() && this.isObfuscated() == style.isObfuscated() && this.isStrikethrough() == style.isStrikethrough() && this.isUnderlined() == style.isUnderlined() && Objects.equals(this.getClickEvent(), style.getClickEvent()) && Objects.equals(this.getHoverEvent(), style.getHoverEvent()) && Objects.equals(this.getInsertion(), style.getInsertion()) && Objects.equals(this.getFont(), style.getFont());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion);
    }

    public static class Serializer implements JsonDeserializer<Style>, JsonSerializer<Style> {
        @Nullable
        @Override
        public Style deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject == null) {
                    return null;
                } else {
                    Boolean boolean_ = getOptionalFlag(jsonObject, "bold");
                    Boolean boolean2 = getOptionalFlag(jsonObject, "italic");
                    Boolean boolean3 = getOptionalFlag(jsonObject, "underlined");
                    Boolean boolean4 = getOptionalFlag(jsonObject, "strikethrough");
                    Boolean boolean5 = getOptionalFlag(jsonObject, "obfuscated");
                    TextColor textColor = getTextColor(jsonObject);
                    String string = getInsertion(jsonObject);
                    ClickEvent clickEvent = getClickEvent(jsonObject);
                    HoverEvent hoverEvent = getHoverEvent(jsonObject);
                    ResourceLocation resourceLocation = getFont(jsonObject);
                    return new Style(textColor, boolean_, boolean2, boolean3, boolean4, boolean5, clickEvent, hoverEvent, string, resourceLocation);
                }
            } else {
                return null;
            }
        }

        @Nullable
        private static ResourceLocation getFont(JsonObject root) {
            if (root.has("font")) {
                String string = GsonHelper.getAsString(root, "font");

                try {
                    return new ResourceLocation(string);
                } catch (ResourceLocationException var3) {
                    throw new JsonSyntaxException("Invalid font name: " + string);
                }
            } else {
                return null;
            }
        }

        @Nullable
        private static HoverEvent getHoverEvent(JsonObject root) {
            if (root.has("hoverEvent")) {
                JsonObject jsonObject = GsonHelper.getAsJsonObject(root, "hoverEvent");
                HoverEvent hoverEvent = HoverEvent.deserialize(jsonObject);
                if (hoverEvent != null && hoverEvent.getAction().isAllowedFromServer()) {
                    return hoverEvent;
                }
            }

            return null;
        }

        @Nullable
        private static ClickEvent getClickEvent(JsonObject root) {
            if (root.has("clickEvent")) {
                JsonObject jsonObject = GsonHelper.getAsJsonObject(root, "clickEvent");
                String string = GsonHelper.getAsString(jsonObject, "action", (String)null);
                ClickEvent.Action action = string == null ? null : ClickEvent.Action.getByName(string);
                String string2 = GsonHelper.getAsString(jsonObject, "value", (String)null);
                if (action != null && string2 != null && action.isAllowedFromServer()) {
                    return new ClickEvent(action, string2);
                }
            }

            return null;
        }

        @Nullable
        private static String getInsertion(JsonObject root) {
            return GsonHelper.getAsString(root, "insertion", (String)null);
        }

        @Nullable
        private static TextColor getTextColor(JsonObject root) {
            if (root.has("color")) {
                String string = GsonHelper.getAsString(root, "color");
                return TextColor.parseColor(string);
            } else {
                return null;
            }
        }

        @Nullable
        private static Boolean getOptionalFlag(JsonObject root, String key) {
            return root.has(key) ? root.get(key).getAsBoolean() : null;
        }

        @Nullable
        @Override
        public JsonElement serialize(Style style, Type type, JsonSerializationContext jsonSerializationContext) {
            if (style.isEmpty()) {
                return null;
            } else {
                JsonObject jsonObject = new JsonObject();
                if (style.bold != null) {
                    jsonObject.addProperty("bold", style.bold);
                }

                if (style.italic != null) {
                    jsonObject.addProperty("italic", style.italic);
                }

                if (style.underlined != null) {
                    jsonObject.addProperty("underlined", style.underlined);
                }

                if (style.strikethrough != null) {
                    jsonObject.addProperty("strikethrough", style.strikethrough);
                }

                if (style.obfuscated != null) {
                    jsonObject.addProperty("obfuscated", style.obfuscated);
                }

                if (style.color != null) {
                    jsonObject.addProperty("color", style.color.serialize());
                }

                if (style.insertion != null) {
                    jsonObject.add("insertion", jsonSerializationContext.serialize(style.insertion));
                }

                if (style.clickEvent != null) {
                    JsonObject jsonObject2 = new JsonObject();
                    jsonObject2.addProperty("action", style.clickEvent.getAction().getName());
                    jsonObject2.addProperty("value", style.clickEvent.getValue());
                    jsonObject.add("clickEvent", jsonObject2);
                }

                if (style.hoverEvent != null) {
                    jsonObject.add("hoverEvent", style.hoverEvent.serialize());
                }

                if (style.font != null) {
                    jsonObject.addProperty("font", style.font.toString());
                }

                return jsonObject;
            }
        }
    }
}
