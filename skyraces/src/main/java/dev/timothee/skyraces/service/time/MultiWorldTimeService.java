package dev.timothee.skyraces.service.time;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

public class MultiWorldTimeService {

    private static final int DAY_TICKS = 24000;
    private static final int NIGHT_START = 13000; // vanilla approx
    private static final int NIGHT_END   = 23000;
    // 1.5 min -> 1800 ticks (10 min total nuit élargie)
    private static final int EXTRA_BEFORE_TICKS = (int) Math.round(1.5 * 1200);
    private static final int EXTRA_AFTER_TICKS  = (int) Math.round(1.5 * 1200);

    private final List<World> worlds;

    public MultiWorldTimeService(List<String> configuredWorlds) {
        if (configuredWorlds == null || configuredWorlds.isEmpty()) {
            this.worlds = new ArrayList<>(Bukkit.getWorlds());
        } else {
            this.worlds = configuredWorlds.stream()
                    .map(Bukkit::getWorld)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    public List<World> getWorlds() {
        return worlds;
    }

    public boolean isShadowWindow(World world) {
        long t = world.getTime() % DAY_TICKS;
        int start = mod(NIGHT_START - EXTRA_BEFORE_TICKS, DAY_TICKS);
        int end   = mod(NIGHT_END   + EXTRA_AFTER_TICKS,  DAY_TICKS);
        return inWrappedInterval((int) t, start, end);
    }

    public boolean isSolarWindow(World world) {
        return !isShadowWindow(world);
    }

    private static int mod(int x, int m) { int r = x % m; return r < 0 ? r + m : r; }
    private static boolean inWrappedInterval(int t, int start, int end) {
        if (start <= end) return t >= start && t <= end;
        return t >= start || t <= end;
    }
}
