package dev.timothee.skyraces.feature.combat;

import dev.timothee.skyraces.model.Race;

/**
 * Stratégie de calcul des multiplicateurs de dégâts.
 * 1. Attaque: multiplicateur appliqué quand le joueur inflige des dégâts.
 * 2. Défense: multiplicateur appliqué quand le joueur reçoit des dégâts.
 *
 * Les booléens indiquent la fenêtre temporelle du monde:
 * - isShadowWindow: vrai si on est dans la fenêtre "Ombres" (nuit étendue.)
 * - isSolarWindow: vrai si on est dans la fenêtre "Solaires" (complément d'Ombres.)
 */
public interface CombatPolicy {
    double attackMultiplier(Race attackerRace, boolean isShadowWindow);
    double defenseMultiplier(Race defenderRace, boolean isSolarWindow);
}
