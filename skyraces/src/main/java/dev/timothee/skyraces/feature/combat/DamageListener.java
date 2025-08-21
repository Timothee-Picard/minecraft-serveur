package dev.timothee.skyraces.feature.combat;

import dev.timothee.skyraces.model.Race;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.service.time.MultiWorldTimeService; // Gardé pour compat, non utilisé pour le calcul combat
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.EnumSet;
import java.util.Locale;

/**
 * Applique les modificateurs de dégâts selon la race et la fenêtre (jour/nuit).
 * IMPORTANT : la fenêtre est calculée à la volée via l’horloge du monde (stateless),
 * afin d’être correcte immédiatement même après /time set day|night.
 */
public class DamageListener implements Listener {
    private final RaceManager races;
    @SuppressWarnings("unused")
    private final MultiWorldTimeService time; // utile ailleurs, pas pour le calcul de combat

    public DamageListener(RaceManager races, MultiWorldTimeService time) {
        this.races = races;
        this.time = time;
    }

    // Causes comptées comme "dégâts de mobs" (pas les chutes/lave, etc.)
    private static final EnumSet<EntityDamageEvent.DamageCause> MOB_CAUSES = EnumSet.of(
            EntityDamageEvent.DamageCause.ENTITY_ATTACK,
            EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,
            EntityDamageEvent.DamageCause.PROJECTILE,
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION // creepers
    );

    /* =========================
       Helpers debug & affichage
       ========================= */

    // Affiche en cœurs (1 cœur = 2 HP), format FR (virgule)
    private static String hearts(double damage) {
        return String.format(Locale.FRANCE, "%.1f❤", damage / 2.0);
    }

    private static void debug(Player p, String msg) {
        if (p != null && p.hasPermission("skyraces.debug")) {
            p.sendMessage("§8[§7SkyRaces§8] §7" + msg);
        }
    }

    private static void clog(String msg) {
        Bukkit.getLogger().info("[SkyRaces] " + msg);
    }

    /* =========================
       Fenêtres Jour/Nuit (stateless)
       ========================= */

    // Vanilla : cycle 24000 ticks ; nuit ≈ 13000–23000
    private boolean isShadowNowByClock(World world) {
        long t = world.getTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    private boolean isSolarNowByClock(World world) {
        return !isShadowNowByClock(world);
    }

    /* =========================
       Listeners dégâts
       ========================= */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHit(EntityDamageByEntityEvent e) {
        Player attacker = null;
        Player victim = (e.getEntity() instanceof Player pv) ? pv : null;

        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p; // arcs, arbalètes, tridents tirés par un joueur
        }

        if (attacker == null) return;

        double base = e.getDamage(); // avant nos modifs
        double after = base;
        double mult = 1.0;

        Race r = races.getRace(attacker);
        if (r == null) {
            debug(attacker, "Aucune race détectée → pas de modification de dégâts.");
            return;
        }

        boolean shadow = isShadowNowByClock(attacker.getWorld());

        if (r == Race.OMBRES && shadow) {
            mult = 1.20; // +20% la nuit pour Ombres
        } else if (r == Race.SOLAIRES && shadow) {
            mult = 0.90; // -10% la nuit pour Solaires
        }

        if (mult != 1.0) {
            after = base * mult;
            e.setDamage(after);
        }

        // e.getFinalDamage() inclut d’autres modifs plus tard (armure/potions)
        double finalAfter = e.getFinalDamage();

        // Debug joueurs (nécessite la permission skyraces.debug)
        debug(attacker, String.format(
                Locale.FRANCE,
                "ATTAQUE (%s, nuit=%s) → base=%s, x%.2f → mod=%s (final=%s)%s",
                r.name(), shadow ? "oui" : "non",
                hearts(base), mult,
                hearts(after), hearts(finalAfter),
                victim != null ? (" sur " + victim.getName()) : ""
        ));
        if (victim != null) {
            debug(victim, String.format(
                    Locale.FRANCE,
                    "REÇU (par %s) → base=%s, après mod attaquant=%s (final=%s)",
                    attacker.getName(),
                    hearts(base), hearts(after), hearts(finalAfter)
            ));
        }

        // Log console concis
        clog(String.format(
                Locale.US, // logs machine-friendly (point décimal)
                "Hit %s -> %s | race=%s night=%s base=%.2f after=%.2f final=%.2f mult=%.2f",
                attacker.getName(),
                (victim != null ? victim.getName() : e.getEntity().getType().name()),
                r.name(), shadow, base, after, finalAfter, mult
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!MOB_CAUSES.contains(e.getCause())) return; // ignore chutes, feu, etc.

        double base = e.getDamage();
        double after = base;
        double mult = 1.0;

        Race r = races.getRace(p);
        if (r == null) {
            debug(p, "Aucune race détectée → pas de modification de dégâts reçus.");
            return;
        }

        boolean solar = isSolarNowByClock(p.getWorld());

        if (solar && r == Race.SOLAIRES) {
            mult = 0.90; // -10% le jour pour Solaires
        } else if (solar && r == Race.OMBRES) {
            mult = 1.10; // +10% le jour pour Ombres
        }

        if (mult != 1.0) {
            after = base * mult;
            e.setDamage(after);
        }

        double finalAfter = e.getFinalDamage();

        debug(p, String.format(
                Locale.FRANCE,
                "SUBI (%s, jour=%s) → base=%s, x%.2f → mod=%s (final=%s) cause=%s",
                r.name(), solar ? "oui" : "non",
                hearts(base), mult, hearts(after), hearts(finalAfter),
                e.getCause().name()
        ));

        clog(String.format(
                Locale.US, // logs machine-friendly
                "Damaged %s | race=%s day=%s cause=%s base=%.2f after=%.2f final=%.2f mult=%.2f",
                p.getName(), r.name(), solar, e.getCause().name(), base, after, finalAfter, mult
        ));
    }
}
