package dev.timothee.skyraces.core;

import dev.timothee.skyraces.command.RaceCommand;
import dev.timothee.skyraces.feature.combat.DamageListener;
import dev.timothee.skyraces.feature.cycle.CycleAnnouncer;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.service.time.MultiWorldTimeService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SkyRacesPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Crée config.yml depuis resources si absent
        saveDefaultConfig();

        // Services
        List<String> worlds = getConfig().getStringList("worlds"); // vide = tous les mondes
        MultiWorldTimeService time = new MultiWorldTimeService(worlds);
        RaceManager races = new RaceManager();

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
