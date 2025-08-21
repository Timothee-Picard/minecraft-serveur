package dev.timothee.skyraces.feature.combat;

import dev.timothee.skyraces.model.Race;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.service.time.MultiWorldTimeService;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.EnumSet;

public class DamageListener implements Listener {
    private final RaceManager races;
    private final MultiWorldTimeService time;

    public DamageListener(RaceManager races, MultiWorldTimeService time) {
        this.races = races;
        this.time  = time;
    }

    private static final EnumSet<EntityDamageEvent.DamageCause> MOB_CAUSES = EnumSet.of(
            EntityDamageEvent.DamageCause.ENTITY_ATTACK,
            EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,
            EntityDamageEvent.DamageCause.PROJECTILE,
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHit(EntityDamageByEntityEvent e) {
        Player attacker = null;

        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p; // arcs, arbalètes, tridents tirés par un joueur
        }

        if (attacker == null) return;

        double base = e.getDamage();
        double mult = 1.0;

        Race r = races.getRace(attacker);
        if (r == null) return;

        boolean shadow = time.isShadowWindow(attacker.getWorld());

        if (r == Race.OMBRES && shadow) {
            mult = 1.20;
        } else if (r == Race.SOLAIRES && shadow) {
            mult = 0.90;
        }

        if (mult != 1.0) {
            e.setDamage(base * mult);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!MOB_CAUSES.contains(e.getCause())) return;

        double base = e.getDamage();
        double mult = 1.0;

        Race r = races.getRace(p);
        if (r == null) return;

        boolean solar = time.isSolarWindow(p.getWorld());

        if (solar && r == Race.SOLAIRES) {
            mult = 0.90;
        } else if (solar && r == Race.OMBRES) {
            mult = 1.10;
        }

        if (mult != 1.0) {
            e.setDamage(base * mult);
        }
    }
}
