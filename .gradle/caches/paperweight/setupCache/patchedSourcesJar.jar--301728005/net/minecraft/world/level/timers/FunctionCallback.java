package net.minecraft.world.level.timers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;

public class FunctionCallback implements TimerCallback<MinecraftServer> {
    final ResourceLocation functionId;

    public FunctionCallback(ResourceLocation name) {
        this.functionId = name;
    }

    @Override
    public void handle(MinecraftServer minecraftServer, TimerQueue<MinecraftServer> timerQueue, long l) {
        ServerFunctionManager serverFunctionManager = minecraftServer.getFunctions();
        serverFunctionManager.get(this.functionId).ifPresent((function) -> {
            serverFunctionManager.execute(function, serverFunctionManager.getGameLoopSender());
        });
    }

    public static class Serializer extends TimerCallback.Serializer<MinecraftServer, FunctionCallback> {
        public Serializer() {
            super(new ResourceLocation("function"), FunctionCallback.class);
        }

        @Override
        public void serialize(CompoundTag nbt, FunctionCallback callback) {
            nbt.putString("Name", callback.functionId.toString());
        }

        @Override
        public FunctionCallback deserialize(CompoundTag compoundTag) {
            ResourceLocation resourceLocation = new ResourceLocation(compoundTag.getString("Name"));
            return new FunctionCallback(resourceLocation);
        }
    }
}
