package dev.timothee.skyraces.feature.combat;

import dev.timothee.skyraces.model.Race;
import dev.timothee.skyraces.service.race.RaceManager;
import dev.timothee.skyraces.service.time.MultiWorldTimeService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.EnumSet;

@RequiredArgsConstructor
public class DamageListener implements Listener {
    private final RaceManager races;
    private final MultiWorldTimeService time;
    private final CombatPolicy policy;

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

        Race r = races.getRace(attacker);
        if (r == null) return;

        boolean shadow = time.isShadowWindow(attacker.getWorld());
        double mult = policy.attackMultiplier(r, shadow);

        if (mult != 1.0) {
            e.setDamage(e.getDamage() * mult);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!MOB_CAUSES.contains(e.getCause())) return;

        Race r = races.getRace(p);
        if (r == null) return;

        boolean solar = time.isSolarWindow(p.getWorld());
        double mult = policy.defenseMultiplier(r, solar);

        if (mult != 1.0) {
            e.setDamage(e.getDamage() * mult);
        }
    }
}
