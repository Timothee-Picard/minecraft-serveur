package dev.timothee.skyraces.service.core;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ConfigService {
    private final @NonNull Configuration cfg;
    private final @NonNull Consumer<String> warn;

    public int pollTicks()              { return intMin1("messages.poll_ticks", 40); }
    public boolean announceOnStart()    { return bool("messages.announce_on_start", true); }
    public ConfigurationSection solarSec()  { return section("messages.solar_start"); }
    public ConfigurationSection shadowSec() { return section("messages.shadow_start"); }

    public List<String> worlds() { return cfg.getStringList("worlds"); }

    public int nightStart() { return intTick("time.night_start", 13000); }
    public int nightEnd()   { return intTick("time.night_end",   23000); }
    public int extraBefore(){ return nonNeg("time.extra_before_ticks", 0); }
    public int extraAfter() { return nonNeg("time.extra_after_ticks",  0); }

    private int intMin1(String path, int def) {
        if (!cfg.contains(path)) warn.accept(miss(path, def));
        int v = cfg.getInt(path, def);
        if (v < 1) {
            warn.accept("Config: valeur invalide '" + path + "' (" + v + ") – forcé à 1.");
            return 1;
        }
        return v;
    }

    private boolean bool(String path, boolean def) {
        if (!cfg.contains(path)) warn.accept(miss(path, def));
        return cfg.getBoolean(path, def);
    }

    private int intTick(String path, int def) {
        if (!cfg.contains(path)) warn.accept(miss(path, def));
        int x = cfg.getInt(path, def) % 24000;
        return x < 0 ? x + 24000 : x;
    }

    private int nonNeg(String path, int def) {
        if (!cfg.contains(path)) warn.accept(miss(path, def));
        int x = cfg.getInt(path, def);
        return Math.max(0, x);
    }

    private ConfigurationSection section(String path) {
        ConfigurationSection s = cfg.getConfigurationSection(path);
        if (s == null) warn.accept("Config: section manquante '" + path + "' – valeurs par défaut utilisées.");
        return s;
    }

    private String miss(String path, Object def) {
        return "Config: clé manquante '" + path + "' – fallback " + def + ".";
    }
}
