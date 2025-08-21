package dev.timothee.skyraces.service.race;

import dev.timothee.skyraces.model.Race;
import org.bukkit.entity.Player;

public class RaceManager {
    public static final String PERM_OMBRE  = "skyrace.race.ombre";
    public static final String PERM_SOLAIR = "skyrace.race.solar";

    public Race getRace(Player p) {
        boolean ombre  = p.hasPermission(PERM_OMBRE);
        boolean solaire = p.hasPermission(PERM_SOLAIR);
        if (ombre == solaire) return null; // les deux true OU les deux false → indéterminé
        return ombre ? Race.OMBRES : Race.SOLAIRES;
    }
}
