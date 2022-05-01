package net.minecraft.world.level.timers;

import net.minecraft.commands.CommandFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.tags.Tag;

public class FunctionTagCallback implements TimerCallback<MinecraftServer> {
    final ResourceLocation tagId;

    public FunctionTagCallback(ResourceLocation name) {
        this.tagId = name;
    }

    @Override
    public void handle(MinecraftServer minecraftServer, TimerQueue<MinecraftServer> timerQueue, long l) {
        ServerFunctionManager serverFunctionManager = minecraftServer.getFunctions();
        Tag<CommandFunction> tag = serverFunctionManager.getTag(this.tagId);

        for(CommandFunction commandFunction : tag.getValues()) {
            serverFunctionManager.execute(commandFunction, serverFunctionManager.getGameLoopSender());
        }

    }

    public static class Serializer extends TimerCallback.Serializer<MinecraftServer, FunctionTagCallback> {
        public Serializer() {
            super(new ResourceLocation("function_tag"), FunctionTagCallback.class);
        }

        @Override
        public void serialize(CompoundTag nbt, FunctionTagCallback callback) {
            nbt.putString("Name", callback.tagId.toString());
        }

        @Override
        public FunctionTagCallback deserialize(CompoundTag compoundTag) {
            ResourceLocation resourceLocation = new ResourceLocation(compoundTag.getString("Name"));
            return new FunctionTagCallback(resourceLocation);
        }
    }
}
