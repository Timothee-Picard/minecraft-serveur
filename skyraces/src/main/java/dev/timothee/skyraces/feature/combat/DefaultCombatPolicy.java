package dev.timothee.skyraces.feature.combat;

import dev.timothee.skyraces.model.Race;

/**
 * Politique par défaut:
 * - Nuit (Ombres): Ombres +20% ATTAQUE, Solaires -10% ATTAQUE
 * - Jour (Solaires): Solaires -10% DÉGÂTS REÇUS, Ombres +10% DÉGÂTS REÇUS
 */
public class DefaultCombatPolicy implements CombatPolicy {

    private static final double ATTACK_OMBRES_IN_SHADOW = 1.20;
    private static final double ATTACK_SOLAIRE_IN_SHADOW = 0.90;
    private static final double DEFENSE_SOLAIRE_IN_SOLAR = 0.90;
    private static final double DEFENSE_OMBRE_IN_SOLAR   = 1.10;

    @Override
    public double attackMultiplier(Race attackerRace, boolean isShadowWindow) {
        if (attackerRace == null) return 1.0;
        if (isShadowWindow) {
            if (attackerRace == Race.OMBRES)   return ATTACK_OMBRES_IN_SHADOW;
            if (attackerRace == Race.SOLAIRES) return ATTACK_SOLAIRE_IN_SHADOW;
        }
        return 1.0;
    }

    @Override
    public double defenseMultiplier(Race defenderRace, boolean isSolarWindow) {
        if (defenderRace == null) return 1.0;
        if (isSolarWindow) {
            if (defenderRace == Race.SOLAIRES) return DEFENSE_SOLAIRE_IN_SOLAR;
            if (defenderRace == Race.OMBRES)   return DEFENSE_OMBRE_IN_SOLAR;
        }
        return 1.0;
    }
}
