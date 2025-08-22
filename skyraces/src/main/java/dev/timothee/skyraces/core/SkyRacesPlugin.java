package dev.timothee.skyraces.core;

import dev.timothee.skyraces.command.RaceCommand;
import dev.timothee.skyraces.feature.combat.CombatPolicy;
import dev.timothee.skyraces.feature.combat.DamageListener;
import dev.timothee.skyraces.feature.combat.DefaultCombatPolicy;
import dev.timothee.skyraces.feature.cycle.CycleAnnouncer;
import dev.timothee.skyraces.feature.cycle.MessageProvider;
import dev.timothee.skyraces.service.core.ConfigService;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.service.time.MultiWorldTimeService;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SkyRacesPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Crée config.yml depuis resources si absent
        saveDefaultConfig();

        // === Config centralisée ===
        ConfigService cfg = new ConfigService(getConfig(), msg -> getLogger().warning(msg));

        // === Time service à partir de la config ===
        List<String> worlds = cfg.worlds(); // vide = tous les mondes
        int nightStart      = cfg.nightStart();
        int nightEnd        = cfg.nightEnd();
        int extraBefore     = cfg.extraBefore();
        int extraAfter      = cfg.extraAfter();

        MultiWorldTimeService time = new MultiWorldTimeService(worlds, nightStart, nightEnd, extraBefore, extraAfter);

        // === Services métier ===
        RaceManager races = new RaceManager();
        CombatPolicy policy = new DefaultCombatPolicy();

        // === Provider des messages (fallback + warnings via cfg) ===
        MessageProvider messages = new MessageProvider(cfg, msg -> getLogger().warning(msg));

        // === Logs démarrage utiles ===
        getLogger().info(String.format(
                "Fenêtre Ombres: start=%d end=%d (effectif: %d→%d) | extras: before=%d after=%d",
                nightStart, nightEnd, time.getEffectiveShadowStart(), time.getEffectiveShadowEnd(), extraBefore, extraAfter
        ));
        for (World w : time.getWorlds()) {
            boolean shadowNow = time.isShadowWindow(w);
            long t = w.getTime() % 24000L;
            getLogger().info(String.format("World=%s time=%d → shadow=%s", w.getName(), t, shadowNow));
        }

        // === Commande /race ===
        PluginCommand raceCmd = getCommand("race");
        if (raceCmd != null) {
            raceCmd.setExecutor(new RaceCommand(races));
        } else {
            getLogger().warning("La commande 'race' n'est pas définie dans plugin.yml");
        }

        // === Listeners ===
        getServer().getPluginManager().registerEvents(new DamageListener(races, time, policy), this);

        // === Annonces (Cycle) ===
        new CycleAnnouncer(
                this,
                time,
                races,
                messages,
                cfg.pollTicks(),
                cfg.announceOnStart()
        ).start();

        getLogger().info("[SkyRaces] Plugin activé !");
    }

    @Override
    public void onDisable() {
        getLogger().info("[SkyRaces] Plugin désactivé.");
    }
}
