package dev.timothee.skyraces.feature.cycle;

import dev.timothee.skyraces.service.time.MultiWorldTimeService;
import dev.timothee.skyraces.model.Race;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.core.SkyRacesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor; // 👈 ajout
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class CycleAnnouncer {
    private final SkyRacesPlugin plugin;
    private final MultiWorldTimeService time;
    private final RaceManager races;

    private final Map<UUID, Boolean> lastShadowByWorld = new HashMap<>();
    private final int pollTicks;

    // Messages Adventure
    private final Component solarDefault, solarSol, solarOmb;
    private final Component shadowDefault, shadowSol, shadowOmb;

    // Templates de broadcast (optionnels)
    private final String solarBroadcastTpl, shadowBroadcastTpl;

    public CycleAnnouncer(SkyRacesPlugin plugin, MultiWorldTimeService time, RaceManager races) {
        this.plugin = plugin;
        this.time = time;
        this.races = races;

        this.pollTicks = Math.max(1, plugin.getConfig().getInt("messages.poll_ticks", 40));

        this.solarDefault = cc(plugin.getConfig().getString("messages.solar_start.default", "&e☀ Le jour se lève."));
        this.solarSol     = cc(plugin.getConfig().getString("messages.solar_start.solaires", "&6☀ Le jour se lève ! Vos forces grandissent sous le soleil."));
        this.solarOmb     = cc(plugin.getConfig().getString("messages.solar_start.ombres", "&7☀ Le jour se lève. La lumière émousse vos pouvoirs."));

        this.shadowDefault = cc(plugin.getConfig().getString("messages.shadow_start.default", "&9🌙 La nuit tombe."));
        this.shadowSol     = cc(plugin.getConfig().getString("messages.shadow_start.solaires", "&7🌙 La nuit tombe. Vos forces faiblissent dans l'ombre."));
        this.shadowOmb     = cc(plugin.getConfig().getString("messages.shadow_start.ombres", "&5🌙 La nuit tombe ! Les ténèbres vous renforcent."));

        this.solarBroadcastTpl  = plugin.getConfig().getString("messages.solar_start.broadcast", null);
        this.shadowBroadcastTpl = plugin.getConfig().getString("messages.shadow_start.broadcast", null);
    }

    public void start() {
        for (World w : time.getWorlds()) {
            lastShadowByWorld.put(w.getUID(), time.isShadowWindow(w));
        }

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World w : time.getWorlds()) {
                boolean nowShadow = time.isShadowWindow(w);
                UUID id = w.getUID();
                Boolean prev = lastShadowByWorld.get(id);
                if (prev == null || prev != nowShadow) {
                    if (nowShadow) announceShadowStart(w);
                    else announceSolarStart(w);
                    lastShadowByWorld.put(id, nowShadow);
                }
            }
        }, pollTicks, pollTicks);
    }

    private void announceSolarStart(World w) {
        for (Player p : w.getPlayers()) {
            Race r = races.getRace(p);
            if (r == Race.SOLAIRES) p.sendMessage(solarSol);
            else if (r == Race.OMBRES) p.sendMessage(solarOmb);
            else p.sendMessage(solarDefault);
        }
        Component global = buildBroadcast(solarBroadcastTpl, solarDefault, w.getName());
        broadcastGlobal(global);
    }

    private void announceShadowStart(World w) {
        for (Player p : w.getPlayers()) {
            Race r = races.getRace(p);
            if (r == Race.SOLAIRES) p.sendMessage(shadowSol);
            else if (r == Race.OMBRES) p.sendMessage(shadowOmb);
            else p.sendMessage(shadowDefault);
        }
        Component global = buildBroadcast(shadowBroadcastTpl, shadowDefault, w.getName());
        broadcastGlobal(global);
    }

    // 👇 Préfixe toujours le message global avec [world]
    private Component buildBroadcast(String template, Component fallback, String worldName) {
        Component prefix = Component.text("[" + worldName + "] ", NamedTextColor.DARK_GRAY);
        if (template != null && !template.isBlank()) {
            String raw = template.replace("%world%", worldName);
            return prefix.append(cc(raw));
        }
        return prefix.append(fallback);
    }

    private void broadcastGlobal(Component message) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private static Component cc(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s == null ? "" : s);
    }
}
