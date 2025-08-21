package dev.timothee.skyraces.feature.cycle;

import dev.timothee.skyraces.core.SkyRacesPlugin;
import dev.timothee.skyraces.model.Race;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.service.time.MultiWorldTimeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.*;

public class CycleAnnouncer implements Listener {
    private final SkyRacesPlugin plugin;
    private final MultiWorldTimeService time;
    private final RaceManager races;

    private final Map<UUID, Boolean> lastShadowByWorld = new HashMap<>();
    private final int pollTicks;
    private final boolean announceOnStart;

    // Messages (Adventure)
    private final Component solarGeneral, solarSolEff, solarOmbEff;
    private final Component shadowGeneral, shadowSolEff, shadowOmbEff;

    public CycleAnnouncer(SkyRacesPlugin plugin, MultiWorldTimeService time, RaceManager races) {
        this.plugin = plugin;
        this.time = time;
        this.races = races;

        this.pollTicks = Math.max(1, plugin.getConfig().getInt("messages.poll_ticks", 40));
        this.announceOnStart = plugin.getConfig().getBoolean("messages.announce_on_start", true);

        var solarSec  = plugin.getConfig().getConfigurationSection("messages.solar_start");
        var shadowSec = plugin.getConfig().getConfigurationSection("messages.shadow_start");

        this.solarGeneral = cc(solarSec != null ? solarSec.getString("general", "&e☀ Le jour se lève.") : "&e☀ Le jour se lève.");
        this.solarSolEff  = cc(solarSec != null ? solarSec.getString("solaires", "&6Vos forces grandissent sous le soleil.") : "&6Vos forces grandissent sous le soleil.");
        this.solarOmbEff  = cc(solarSec != null ? solarSec.getString("ombres", "&7La lumière émousse vos pouvoirs.") : "&7La lumière émousse vos pouvoirs.");

        this.shadowGeneral = cc(shadowSec != null ? shadowSec.getString("general", "&9🌙 La nuit tombe.") : "&9🌙 La nuit tombe.");
        this.shadowSolEff  = cc(shadowSec != null ? shadowSec.getString("solaires", "&7Vos forces faiblissent dans l'ombre.") : "&7Vos forces faiblissent dans l'ombre.");
        this.shadowOmbEff  = cc(shadowSec != null ? shadowSec.getString("ombres", "&5Les ténèbres vous renforcent.") : "&5Les ténèbres vous renforcent.");
    }

    public void start() {
        // Enregistre le listener pour PlayerChangedWorldEvent
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Init état par monde + annonce immédiate si demandé
        for (World w : time.getWorlds()) {
            boolean nowShadow = time.isShadowWindow(w);
            lastShadowByWorld.put(w.getUID(), nowShadow);
            if (announceOnStart) {
                announceWorldCycle(w, nowShadow);
            }
        }

        // Poll périodique pour détecter les bascules jour/nuit
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, pollTicks);
    }

    private void tick() {
        for (World w : time.getWorlds()) {
            boolean nowShadow = time.isShadowWindow(w);
            UUID id = w.getUID();
            Boolean prev = lastShadowByWorld.get(id);
            if (prev == null || prev != nowShadow) {
                // Bascule détectée → messages perso aux joueurs de CE monde
                announceWorldCycle(w, nowShadow);
                lastShadowByWorld.put(id, nowShadow);
            }
        }
    }

    /**
     * Envoie 2 messages perso à chaque joueur dans le monde w :
     *  - 1 général (jour/nuit)
     *  - 1 d’effet selon sa race (renforcé / affaibli) sans répéter le général.
     */
    private void announceWorldCycle(World w, boolean nowShadow) {
        Component worldPrefix = Component.text("[" + w.getName() + "] ", NamedTextColor.DARK_GRAY);
        Component general = nowShadow ? shadowGeneral : solarGeneral;

        for (Player p : w.getPlayers()) {
            // 1) Général
            p.sendMessage(worldPrefix.append(general));

            // 2) Effet selon race
            Race r = races.getRace(p);
            if (r == null) continue;

            if (nowShadow) {
                // Nuit: Ombres renforcées / Solaires affaiblis
                if (r == Race.OMBRES) p.sendMessage(worldPrefix.append(shadowOmbEff));
                else if (r == Race.SOLAIRES) p.sendMessage(worldPrefix.append(shadowSolEff));
            } else {
                // Jour: Solaires renforcés / Ombres affaiblies
                if (r == Race.SOLAIRES) p.sendMessage(worldPrefix.append(solarSolEff));
                else if (r == Race.OMBRES) p.sendMessage(worldPrefix.append(solarOmbEff));
            }
        }
    }

    /**
     * À chaque changement de monde, si l’"effet" (renforcé/affaibli/neutral) change entre
     * l'ancien monde et le nouveau, on envoie SEULEMENT le message d'effet (pas le général).
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
            // Envoie uniquement l’effet
            if (shadow) {
                if (r == Race.OMBRES) p.sendMessage(prefix.append(shadowOmbEff));
                else if (r == Race.SOLAIRES) p.sendMessage(prefix.append(shadowSolEff));
            } else {
                if (r == Race.SOLAIRES) p.sendMessage(prefix.append(solarSolEff));
                else if (r == Race.OMBRES) p.sendMessage(prefix.append(solarOmbEff));
            }
        }
    }

    // ===== Helpers =====

    private enum Effect { REINFORCED, WEAKENED, NEUTRAL }

    private Effect effectInWorld(Race r, World w) {
        boolean shadow = time.isShadowWindow(w);
        if (r == Race.SOLAIRES) {
            return shadow ? Effect.WEAKENED : Effect.REINFORCED;
        } else if (r == Race.OMBRES) {
            return shadow ? Effect.REINFORCED : Effect.WEAKENED;
        }
        return Effect.NEUTRAL;
    }

    private static Component cc(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s == null ? "" : s);
    }
}
