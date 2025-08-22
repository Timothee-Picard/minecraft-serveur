package dev.timothee.skyraces.service.time;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

public class MultiWorldTimeService {

    private static final int DAY_TICKS = 24000;

    // Config runtime
    @Getter private final int nightStart;      // ex: 13000
    @Getter private final int nightEnd;        // ex: 23000
    @Getter private final int extraBefore;     // padding avant (ticks)
    @Getter private final int extraAfter;      // padding après (ticks)

    @Getter private final List<World> worlds;

    /**
     * Comportement par défaut : nuit vanilla stricte (13k → 23k), sans padding.
     * Gardé pour compatibilité.
     */
    public MultiWorldTimeService(List<String> configuredWorlds) {
        this(configuredWorlds, 13000, 23000, 0, 0);
    }

    /**
     * Constructeur paramétrable (recommandé).
     */
    public MultiWorldTimeService(List<String> configuredWorlds, int nightStart, int nightEnd,
                                 int extraBeforeTicks, int extraAfterTicks) {
        this.nightStart = clampTick(nightStart);
        this.nightEnd   = clampTick(nightEnd);
        this.extraBefore = Math.max(0, extraBeforeTicks);
        this.extraAfter  = Math.max(0, extraAfterTicks);

        if (configuredWorlds == null || configuredWorlds.isEmpty()) {
            this.worlds = new ArrayList<>(Bukkit.getWorlds());
        } else {
            this.worlds = configuredWorlds.stream()
                    .map(Bukkit::getWorld)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    /** Fenêtre Ombres (nuit élargie selon padding). */
    public boolean isShadowWindow(World world) {
        long t = world.getTime() % DAY_TICKS;
        int start = mod(nightStart - extraBefore, DAY_TICKS);
        int end   = mod(nightEnd   + extraAfter,  DAY_TICKS);
        return inWrappedInterval((int) t, start, end);
    }

    /** Fenêtre Solaires = complémentaire d’Ombres. */
    public boolean isSolarWindow(World world) {
        return !isShadowWindow(world);
    }

    // ===== Helpers & debug =====

    public int getEffectiveShadowStart() {
        return mod(nightStart - extraBefore, DAY_TICKS);
    }

    public int getEffectiveShadowEnd() {
        return mod(nightEnd + extraAfter, DAY_TICKS);
    }

    private static int clampTick(int x) { return mod(x, DAY_TICKS); }
    private static int mod(int x, int m) { int r = x % m; return r < 0 ? r + m : r; }

    /** Intervalle circulaire sur 0..m-1 */
    private static boolean inWrappedInterval(int t, int start, int end) {
        if (start <= end) return t >= start && t <= end;
        // Fenêtre qui enveloppe 0 (ex: 23000 → 2000)
        return t >= start || t <= end;
    }
}
