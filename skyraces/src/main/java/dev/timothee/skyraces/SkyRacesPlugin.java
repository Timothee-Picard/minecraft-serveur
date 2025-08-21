package dev.timothee.skyraces;

import org.bukkit.plugin.java.JavaPlugin;

public class SkyRacesPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("[SkyRaces] Plugin activé !");
    }

    @Override
    public void onDisable() {
        getLogger().info("[SkyRaces] Plugin désactivé.");
    }
}
