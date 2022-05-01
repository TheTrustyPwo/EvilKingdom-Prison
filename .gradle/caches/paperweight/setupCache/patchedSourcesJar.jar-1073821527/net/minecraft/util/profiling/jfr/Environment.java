package net.minecraft.util.profiling.jfr;

import net.minecraft.server.MinecraftServer;

public enum Environment {
    CLIENT("client"),
    SERVER("server");

    private final String description;

    private Environment(String name) {
        this.description = name;
    }

    public static Environment from(MinecraftServer server) {
        return server.isDedicatedServer() ? SERVER : CLIENT;
    }

    public String getDescription() {
        return this.description;
    }
}
