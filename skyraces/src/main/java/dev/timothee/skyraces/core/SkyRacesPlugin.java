package dev.timothee.skyraces.core;

import dev.timothee.skyraces.command.RaceCommand;
import dev.timothee.skyraces.feature.combat.DamageListener;
import dev.timothee.skyraces.feature.cycle.CycleAnnouncer;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.service.time.MultiWorldTimeService;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SkyRacesPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Crée config.yml depuis resources si absent
        saveDefaultConfig();

        // === Lecture config ===
        List<String> worlds = getConfig().getStringList("worlds"); // vide = tous les mondes
        ConfigurationSection timeSec = getConfig().getConfigurationSection("time");
        int nightStart   = timeSec != null ? timeSec.getInt("night_start", 13000) : 13000;
        int nightEnd     = timeSec != null ? timeSec.getInt("night_end",   23000) : 23000;
        int extraBefore  = timeSec != null ? timeSec.getInt("extra_before_ticks", 0) : 0;
        int extraAfter   = timeSec != null ? timeSec.getInt("extra_after_ticks",  0) : 0;

        // Services
        MultiWorldTimeService time = new MultiWorldTimeService(worlds, nightStart, nightEnd, extraBefore, extraAfter);
        RaceManager races = new RaceManager();

        // Logs démarrage utiles
        getLogger().info(String.format(
                "Fenêtre Ombres: start=%d end=%d (effectif: %d→%d) | extras: before=%d after=%d",
                nightStart, nightEnd, time.getEffectiveShadowStart(), time.getEffectiveShadowEnd(), extraBefore, extraAfter
        ));
        for (World w : time.getWorlds()) {
            boolean shadowNow = time.isShadowWindow(w);
            long t = w.getTime() % 24000L;
            getLogger().info(String.format("World=%s time=%d → shadow=%s", w.getName(), t, shadowNow));
        }

        // Commande /race
        PluginCommand raceCmd = getCommand("race");
        if (raceCmd != null) {
            raceCmd.setExecutor(new RaceCommand(races));
        } else {
            getLogger().warning("La commande 'race' n'est pas définie dans plugin.yml");
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new DamageListener(races, time), this);

        // Annonces
        new CycleAnnouncer(this, time, races).start();

        getLogger().info("[SkyRaces] Plugin activé !");
    }

    @Override
    public void onDisable() {
        getLogger().info("[SkyRaces] Plugin désactivé.");
    }
}
