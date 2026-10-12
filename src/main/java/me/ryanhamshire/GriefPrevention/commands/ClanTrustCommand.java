package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.clans.ClanTrustLevel;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClanTrustCommand implements CommandExecutor, TabCompleter {

    private final GriefPrevention plugin;

    public ClanTrustCommand(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);

        if (claim == null) {
            player.sendMessage(ChatColor.RED + "Debes estar dentro de la protección para modificar la confianza del clan.");
            return true;
        }

        boolean isOwner = player.getUniqueId().equals(claim.getOwnerID());
        boolean isAdmin = player.hasPermission("griefprevention.admin");

        if (!isOwner && !isAdmin) {
            player.sendMessage(ChatColor.RED + "Solo el dueño de la protección puede configurar la confianza del clan.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Nivel actual de Clan Trust: " + ChatColor.YELLOW + claim.getClanTrust().name());
            player.sendMessage(ChatColor.YELLOW + "Uso: /clantrust <build|container|access|off>");
            player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.WHITE + "build: " + ChatColor.GRAY + "Permiso total de construcción.");
            player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.WHITE + "container: " + ChatColor.GRAY + "Acceso a cofres, hornos y cultivos.");
            player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.WHITE + "access: " + ChatColor.GRAY + "Uso de puertas, botones y camas.");
            player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.WHITE + "off: " + ChatColor.GRAY + "Sin permisos especiales para el clan.");
            return true;
        }

        ClanTrustLevel level = ClanTrustLevel.fromString(args[0]);
        claim.setClanTrust(level);
        plugin.dataStore.saveClaim(claim);

        ChatColor color = (level == ClanTrustLevel.OFF) ? ChatColor.RED : ChatColor.GREEN;
        player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.GREEN + "Confianza de clan para esta protección actualizada a: " + color + level.name());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> levels = Arrays.asList("build", "container", "access", "off");
            return levels.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
