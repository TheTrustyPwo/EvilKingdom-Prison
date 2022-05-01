package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType.StringType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;

public class StringArgumentSerializer implements ArgumentSerializer<StringArgumentType> {
    @Override
    public void serializeToNetwork(StringArgumentType type, FriendlyByteBuf buf) {
        buf.writeEnum(type.getType());
    }

    @Override
    public StringArgumentType deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
        StringType stringType = friendlyByteBuf.readEnum(StringType.class);
        switch(stringType) {
        case SINGLE_WORD:
            return StringArgumentType.word();
        case QUOTABLE_PHRASE:
            return StringArgumentType.string();
        case GREEDY_PHRASE:
        default:
            return StringArgumentType.greedyString();
        }
    }

    @Override
    public void serializeToJson(StringArgumentType type, JsonObject json) {
        switch(type.getType()) {
        case SINGLE_WORD:
            json.addProperty("type", "word");
            break;
        case QUOTABLE_PHRASE:
            json.addProperty("type", "phrase");
            break;
        case GREEDY_PHRASE:
        default:
            json.addProperty("type", "greedy");
        }

    }
}
