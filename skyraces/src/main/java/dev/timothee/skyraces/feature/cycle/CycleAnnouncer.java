package dev.timothee.skyraces.feature.cycle;

import dev.timothee.skyraces.core.SkyRacesPlugin;
import dev.timothee.skyraces.model.Race;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.service.time.MultiWorldTimeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CycleAnnouncer implements Listener {

    private final SkyRacesPlugin plugin;
    private final MultiWorldTimeService time;
    private final RaceManager races;
    private final MessageProvider messages;

    // Dernier état nuit/jour connu par monde
    private final Map<UUID, Boolean> lastShadowByWorld = new HashMap<>();

    // Config
    private final int pollTicks;
    private final boolean announceOnStart;

    public CycleAnnouncer(SkyRacesPlugin plugin,
                          MultiWorldTimeService time,
                          RaceManager races,
                          MessageProvider messages,
                          int pollTicks,
                          boolean announceOnStart) {
        this.plugin = plugin;
        this.time = time;
        this.races = races;
        this.messages = messages;
        this.pollTicks = Math.max(1, pollTicks);
        this.announceOnStart = announceOnStart;
    }

    /** Démarre le listener + la détection périodique. */
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Init état par monde + annonce immédiate si demandé
        for (World w : time.getWorlds()) {
            boolean nowShadow = time.isShadowWindow(w);
            lastShadowByWorld.put(w.getUID(), nowShadow);
            if (announceOnStart) {
                announceWorldCycle(w, nowShadow);
            }
        }

        // Poll pour détecter les bascules jour/nuit
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, pollTicks);
    }

    private void tick() {
        for (World w : time.getWorlds()) {
            boolean nowShadow = time.isShadowWindow(w);
            UUID id = w.getUID();
            Boolean prev = lastShadowByWorld.get(id);
            if (prev == null || !prev.equals(nowShadow)) {
                announceWorldCycle(w, nowShadow);
                lastShadowByWorld.put(id, nowShadow);
            }
        }
    }

    /**
     * Envoie DEUX messages aux joueurs du monde w :
     * 1) un message général (jour/nuit)
     * 2) un message d'effet selon la race (renforcé/affaibli)
     */
    private void announceWorldCycle(World w, boolean nowShadow) {
        Component worldPrefix = Component.text("[" + w.getName() + "] ", NamedTextColor.DARK_GRAY);
        Component general = nowShadow ? messages.shadowGeneral() : messages.solarGeneral();

        for (Player p : w.getPlayers()) {
            // 1) Général (chat joueur)
            p.sendMessage(worldPrefix.append(general));

            // 2) Effet selon race (chat joueur)
            Race r = races.getRace(p);
            if (r == null) continue;

            if (nowShadow) {
                if (r == Race.OMBRES) {
                    p.sendMessage(worldPrefix.append(messages.shadowOmbEff()));
                } else if (r == Race.SOLAIRES) {
                    p.sendMessage(worldPrefix.append(messages.shadowSolEff()));
                }
            } else {
                if (r == Race.SOLAIRES) {
                    p.sendMessage(worldPrefix.append(messages.solarSolEff()));
                } else if (r == Race.OMBRES) {
                    p.sendMessage(worldPrefix.append(messages.solarOmbEff()));
                }
            }
        }
    }

    /**
     * À chaque changement de monde, si l’effet change (renforcé/affaibli/neutre),
     * on envoie SEULEMENT l’effet.
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        World from = e.getFrom();
        World to = p.getWorld();

        Race r = races.getRace(p);
        if (r == null) return;

        Effect oldEff = effectInWorld(r, from);
        Effect newEff = effectInWorld(r, to);

        if (oldEff != newEff) {
            Component prefix = Component.text("[" + to.getName() + "] ", NamedTextColor.DARK_GRAY);
            boolean shadow = time.isShadowWindow(to);
            if (shadow) {
                if (r == Race.OMBRES) p.sendMessage(prefix.append(messages.shadowOmbEff()));
                else if (r == Race.SOLAIRES) p.sendMessage(prefix.append(messages.shadowSolEff()));
            } else {
                if (r == Race.SOLAIRES) p.sendMessage(prefix.append(messages.solarSolEff()));
                else if (r == Race.OMBRES) p.sendMessage(prefix.append(messages.solarOmbEff()));
            }
        }
    }

    // ===== Helpers =====

    private enum Effect { REINFORCED, WEAKENED, NEUTRAL }

    private Effect effectInWorld(Race r, World w) {
        boolean shadow = time.isShadowWindow(w);
        if (r == Race.SOLAIRES) return shadow ? Effect.WEAKENED : Effect.REINFORCED;
        if (r == Race.OMBRES)   return shadow ? Effect.REINFORCED : Effect.WEAKENED;
        return Effect.NEUTRAL;
    }
}
