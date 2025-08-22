package dev.timothee.skyraces.feature.cycle;

import dev.timothee.skyraces.service.core.ConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Consumer;

/**
 * Fournit tous les messages "jour/nuit" déjà colorisés (Adventure),
 * avec fallback si la config est incomplète + warning console.
 */
public class MessageProvider {

    private final Component solarGeneral;
    private final Component solarSolEff;
    private final Component solarOmbEff;

    private final Component shadowGeneral;
    private final Component shadowSolEff;
    private final Component shadowOmbEff;

    public MessageProvider(ConfigService cfg, Consumer<String> warn) {
        ConfigurationSection solar  = cfg.solarSec();
        ConfigurationSection shadow = cfg.shadowSec();

        this.solarGeneral = msg(solar,  "general",
                "&e☀ Le jour se lève.", "messages.solar_start.general", warn);
        this.solarSolEff  = msg(solar,  "solaires",
                "&6Vos forces grandissent sous le soleil.", "messages.solar_start.solaires", warn);
        this.solarOmbEff  = msg(solar,  "ombres",
                "&7La lumière émousse vos pouvoirs.", "messages.solar_start.ombres", warn);

        this.shadowGeneral = msg(shadow, "general",
                "&9🌙 La nuit tombe.", "messages.shadow_start.general", warn);
        this.shadowSolEff  = msg(shadow, "solaires",
                "&7Vos forces faiblissent dans l'ombre.", "messages.shadow_start.solaires", warn);
        this.shadowOmbEff  = msg(shadow, "ombres",
                "&5Les ténèbres vous renforcent.", "messages.shadow_start.ombres", warn);
    }

    private static Component msg(ConfigurationSection sec,
                                 String key,
                                 String def,
                                 String fullPath,
                                 Consumer<String> warn) {
        String raw = (sec == null) ? null : sec.getString(key);
        if (raw == null) {
            warn.accept("Config: clé manquante '" + fullPath + "' – fallback: " + def);
            raw = def;
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public Component solarGeneral() { return solarGeneral; }
    public Component solarSolEff()  { return solarSolEff; }
    public Component solarOmbEff()  { return solarOmbEff; }

    public Component shadowGeneral(){ return shadowGeneral; }
    public Component shadowSolEff() { return shadowSolEff; }
    public Component shadowOmbEff() { return shadowOmbEff; }
}
