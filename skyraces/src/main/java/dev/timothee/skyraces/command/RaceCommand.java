package dev.timothee.skyraces.command;

import dev.timothee.skyraces.model.Race;
import dev.timothee.skyraces.service.race.RaceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RaceCommand implements CommandExecutor {
    private final RaceManager races;

    public RaceCommand(RaceManager races) { this.races = races; }

    private static Component msg(String s, NamedTextColor c) { return Component.text(s, c); }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             @NotNull String[] args) {

        // /race help
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            if (sender.hasPermission("skyraces.admin")) sendAdminHelp(sender);
            else sendUserHelp(sender);
            return true;
        }

        // /race info [player]
        if (args.length >= 1 && args[0].equalsIgnoreCase("info") || args.length == 0) {
            // cible : soi-même par défaut si joueur, sinon require <player> depuis console
            if (args.length >= 2) {
                // admin/console : inspection d'un autre joueur
                if (!sender.hasPermission("skyraces.admin") && !(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(msg("Tu n'as pas la permission pour inspecter d'autres joueurs.", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(msg("Joueur introuvable: " + args[1], NamedTextColor.RED));
                    return true;
                }
                sendRaceInfo(sender, target);
                return true;
            } else {
                // pas de cible fournie
                if (sender instanceof Player p) {
                    sendRaceInfo(p, p);
                } else {
                    sender.sendMessage(msg("Depuis la console : /race info <joueur>", NamedTextColor.YELLOW));
                }
                return true;
            }
        }

        // Arguments inconnus -> aide
        if (sender.hasPermission("skyraces.admin")) sendAdminHelp(sender);
        else sendUserHelp(sender);
        return true;
    }

    private void sendRaceInfo(CommandSender viewer, Player target) {
        Race r = races.getRace(target);
        if (viewer.equals(target)) {
            // vue joueur sur soi-même
            if (r == null) viewer.sendMessage(msg("Race : aucune.", NamedTextColor.GRAY));
            else if (r == Race.OMBRES) viewer.sendMessage(msg("Race : Ombres", NamedTextColor.LIGHT_PURPLE));
            else viewer.sendMessage(msg("Race : Solaires", NamedTextColor.GOLD));
        } else {
            // vue admin/console sur un autre joueur
            if (r == null) viewer.sendMessage(msg("Race de " + target.getName() + " : aucune.", NamedTextColor.GRAY));
            else if (r == Race.OMBRES) viewer.sendMessage(msg("Race de " + target.getName() + " : Ombres", NamedTextColor.LIGHT_PURPLE));
            else viewer.sendMessage(msg("Race de " + target.getName() + " : Solaires", NamedTextColor.GOLD));
        }
    }

    private void sendUserHelp(CommandSender sender) {
        sender.sendMessage(msg("Usage :", NamedTextColor.YELLOW));
        sender.sendMessage(msg("• /race            → affiche ta race", NamedTextColor.GRAY));
        sender.sendMessage(msg("• /race info       → affiche ta race", NamedTextColor.GRAY));
        sender.sendMessage(msg("• /race help       → aide", NamedTextColor.GRAY));
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(msg("SkyRaces — Aide admin", NamedTextColor.GOLD));
        sender.sendMessage(msg("• /race info [joueur] : affiche la race (toi ou un autre)", NamedTextColor.GRAY));
        sender.sendMessage(msg("• Attribution des races via permissions :", NamedTextColor.GRAY));
        sender.sendMessage(msg("    - skyrace.race.ombre  → Ombres", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(msg("    - skyrace.race.solar  → Solaires", NamedTextColor.GOLD));
        sender.sendMessage(msg("  Si les deux ou aucune sont présentes → aucune race appliquée.", NamedTextColor.DARK_GRAY));
    }
}
